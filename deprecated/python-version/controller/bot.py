import logging
import os
import re
import threading
import traceback
from datetime import datetime, timedelta, timezone

import ResourceBundle
# noinspection PyPackageRequirements
from googletrans import Translator
from typing import List, Union, Dict, Optional

# noinspection PyPackageRequirements
from telegram.error import BadRequest
# noinspection PyPackageRequirements
from telegram.ext import ContextTypes
# noinspection PyPackageRequirements
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup, ReplyKeyboardMarkup

import database.script
from dto.job import Job, CompletedJob
from dto.request import Request
from exceptions.exceptions import RequestError
from dto.response import Response
from dto.user import User, Superuser
import service
from service.excel_service import ExcelService
from service.job_service import JobService
from service.user_service import UserService
from service.media_service import MediaService


class Session:
    pointer = 0
    request_builder = None
    answer_builder = None
    jobs = None  # type: List[Job]
    index = 0
    __stage = None  # type: Optional[str]
    __gen_plan = None  # type: Optional[str]
    __master = None  # type: Optional[str]
    __title = None  # type: Optional[str]
    __locale = 'ru'
    __user_id: int
    __chat_id: int = None
    __bundle = None  # type: ResourceBundle
    last_message_id: int = None
    only_active: bool

    def start(self, chat_id: int = None, only_active: bool = True):
        if chat_id is not None:
            self.__chat_id = chat_id

        self.only_active = only_active
        self.reset()

        self.get_distinct_by(lambda job: job.stage, lambda _: True)

    def get_distinct_by(self, field, condition):
        self.jobs = sorted({field(job): job for job in
                            (job_service.get_all_active() if self.only_active else job_service.get_all())
                            if condition(job)}.values(),
                           key=lambda job: {0: job.stage, 1: job.master, 2: job.gen_plan, 3: job.title}[self.index])

    def user(self) -> Union[User, Superuser]:
        return user_service.get_by_id(self.__user_id)

    def chat_id(self):
        return self.__chat_id

    def set_user(self, user_id: int):
        self.__user_id = user_id

    def check_access(self):
        if isinstance(self.user(), Superuser) or \
                (self.user().admin_in is not None and len(self.user().admin_in) != 0):
            pass
        else:
            raise RequestError('not-allowed')

    def step(self):
        self.index += 1

    def backwards(self):
        self.index = max(self.index - 1, 0)

        if self.index == 0:
            self.__stage = None
        elif self.index == 1:
            self.__master = None
        elif self.index == 2:
            self.__gen_plan = None
        elif self.index == 3:
            self.__title = None

        self.update_job_list()

    def stage(self):
        return self.__stage

    def gen_plan(self):
        return self.__gen_plan

    def master(self):
        return self.__master

    def title(self):
        return self.__title

    def set_by_job(self, job_id: int, update_list: bool = True):
        job = job_service.get_by_id(job_id)

        if self.index == 0:
            self.__stage = job.stage
        elif self.index == 1:
            self.__master = job.master
        elif self.index == 2:
            self.__gen_plan = job.gen_plan
        elif self.index == 3:
            self.__title = job.title

        self.step()

        if update_list:
            self.update_job_list()

    def update_job_list(self):
        self.get_distinct_by(lambda job: {0: job.stage, 1: job.master, 2: job.gen_plan, 3: job.title}[self.index],
                             lambda job: (self.__stage is None or job.stage == self.__stage) and \
                                         (self.__master is None or job.master == self.__master) and \
                                         (self.__gen_plan is None or job.gen_plan == self.__gen_plan))
        self.pointer = 0

    def generate_request(self, count: float):
        return Request.generate(user_service, self.__user_id)\
            (job_service, self.stage(), self.gen_plan(), self.master(), self.title())(count)

    def move_left(self):
        self.pointer = max(self.pointer - LIST_SIZE, 0)

    def move_right(self):
        if self.pointer + LIST_SIZE < len(self.jobs):
            self.pointer += LIST_SIZE

    def hit_bounds(self):
        if self.pointer <= 0:
            if self.pointer + LIST_SIZE >= len(self.jobs):
                return None
            else:
                return -1
        elif self.pointer + LIST_SIZE >= len(self.jobs):
            return 1
        return 0

    def interval(self) -> List[Job]:
        return self.jobs[self.pointer: min(self.pointer + LIST_SIZE, len(self.jobs))]

    def change_lang(self, lang: str):
        self.__locale = lang

        self.__bundle = ResourceBundle.get_bundle('./resources/MessageBundle', self.__locale)

    def message(self, key: str) -> str:
        if self.__bundle is None:
            self.__bundle = ResourceBundle.get_bundle('./resources/MessageBundle', self.__locale)

        return self.__bundle.get(key)

    def reset(self):
        self.index = 0
        self.pointer = 0
        self.request_builder = None
        self.answer_builder = None
        self.__stage = None
        self.__gen_plan = None
        self.__master = None


LIST_SIZE = 8
all_sessions = {}  # type: Dict[int, Session]
translator = Translator()
languages = {'en', 'ru'}

user_service = UserService()
job_service = JobService()
excel_service = ExcelService()
media_service = MediaService()

# noinspection PyRedeclaration
all_sessions = {user.id: Session() for user in user_service.get_all()}
for i, s in all_sessions.items():
    s.set_user(i)


def get_session(message):
    return all_sessions[message.from_user.id]


def process(request: Request) -> int:
    return job_service.day_activity(request.get_job())


def answer(request: Request, start: datetime, as_file: bool = False, xlsx: bool = False) -> Response:
    csv = excel_service.export_csv(start)

    if as_file:
        file_name = excel_service.save(start, xlsx)

    if as_file:
        # noinspection PyUnboundLocalVariable
        return request.response(csv_path=file_name)
    else:
        return request.response(content=csv)


async def send_message(update: Update, context: ContextTypes.DEFAULT_TYPE, message: str,
                       session: Optional[Session]):
    await context.bot.send_message(chat_id=update.effective_chat.id, text=message,
                                   reply_markup=None if session is None else
                                   ReplyKeyboardMarkup([list(r.keys()) for r in buttons(session.user())],
                                                       one_time_keyboard=True))


async def start(update: Update, context):
    session = Session()
    all_sessions[update.message.from_user.id] = session

    telegram_user = update.message.from_user

    if user_service.get_by_id(telegram_user.id) is None:
        user_service.register(User(telegram_user.id, telegram_user.full_name,
                                   [update.message.chat_id], [], datetime.now()))

    session.set_user(telegram_user.id)

    await send_message(update, context, session.message('start'), None)

    await make_report(update, context)


# noinspection PyShadowingBuiltins
async def help(update: Update, context):
    session = get_session(update.message)

    commands = [f'/{cmd.__name__} - {session.message("description-" + cmd.__name__)}'
                for r in buttons(session.user()) for cmd in list(r.values())]

    await send_message(update, context,
                       session.message('description') + '\n\n' +\
                       '\n'.join(commands) + '\n\n' +\
                       session.message('help'), session)


async def reload(update, context):
    session = get_session(update.message)

    await send_message(update, context, session.message('accepted'), session)


async def make_report(update: Update, context):
    session = get_session(update.message)

    session.start(update.message.chat_id)

    job_list = {job.stage: f'st{job.id}' for job in session.interval()}

    await update.message.reply_text(show_job_list(session, job_list),
                                    reply_markup=show_job_list_navigation(session, job_list))


def period_list_navigation():
    button_list = [
        [InlineKeyboardButton(k, callback_data=v) for k, v in {
            'День (1 день)': 'day',
            'Неделя (7 дней)': 'week',
            'Месяц (31 день)': 'month',
        }.items()],
        [InlineKeyboardButton(k, callback_data=v) for k, v in {
            'Год (365 дней)': 'year',
            'Все время': 'total',
        }.items()],
    ]

    return InlineKeyboardMarkup(button_list)


async def export(update: Update, context, fn):
    session = get_session(update.message)

    session.check_access()

    request = Request(session.user(), Job.fake(), -1)

    session.answer_builder = fn(session, request)

    await context.bot.send_message(chat_id=update.message.chat_id,
                                   text=session.message('choose-period'),
                                   reply_markup=period_list_navigation())


async def export_csv(update, context):
    await export(update, context, lambda session, request: \
        lambda time: \
            context.bot.send_document(chat_id=update.effective_chat.id,
                                      document=open(answer(request, time, as_file=True).path(), 'rb'),
                                      reply_markup=ReplyKeyboardMarkup(
                                          [list(r.keys()) for r in buttons(session.user())],
                                          one_time_keyboard=True)))


async def export_xlsx(update: Update, context):
    await export(update, context, lambda session, request: \
        lambda time: \
            context.bot.send_document(chat_id=update.effective_chat.id,
                                      document=open(answer(request, time, as_file=True, xlsx=True).path(), 'rb'),
                                      reply_markup=ReplyKeyboardMarkup(
                                          [list(r.keys()) for r in buttons(session.user())],
                                          one_time_keyboard=True)))


async def export_text(update: Update, context):
    await export(update, context, lambda session, request: \
        lambda time: \
            send_message(update, context, answer(request, time).content(), session))


async def month_update(update: Update, context):
    session = get_session(update.message)

    session.check_access()

    await send_message(update, context, session.message('provide-the-file'), session)


async def accept_photo(update: Update, context: ContextTypes.DEFAULT_TYPE):
    session = get_session(update.message)

    async def load_photo(file):
        lmi = '?' if session.last_message_id is None else str(session.last_message_id)
        file_name = '0_' + session.user().name + lmi + '.' + file.file_path.split('.')[-1]
        while file_name in os.listdir(service.media_service.path_to_media):
            file_name = f'{str(int(file_name.split("_")[0]) + 1)}_' +\
                        session.user().name + lmi + '.' + file.file_path.split('.')[-1]
        await file.download_to_drive(service.media_service.path_to_media + file_name)

    document = update.message.document
    if document is None:
        for ph in update.message.photo:
            await load_photo(await ph.get_file())
        await send_message(update, context, session.message('accepted'), None)
        await make_report(update, context)
    else:
        extension = document.file_name[-3:]

        variants = ['png', 'jpg', 'gif', 'tif']

        if extension not in variants:
            await send_message(update, context, session.message('unsupported-file-extension')
                               .format(str(variants)), session)
        else:
            await load_photo(await context.bot.get_file(document))
            await send_message(update, context, session.message('accepted'), None)
            await make_report(update, context)


async def accept_xlsx_month_update(update: Update, context: ContextTypes.DEFAULT_TYPE):
    session = get_session(update.message)

    session.check_access()

    await send_message(update, context, session.message('processing'), session)

    document = update.message.document
    file_name = document.file_name

    file = await context.bot.get_file(document)

    await file.download_to_drive(service.excel_service.path_to_job_list + file_name)

    data = database.script.upload(service.excel_service.path_to_job_list + file_name)

    job_service.deactivate_all()
    excel_service.import_data(data)

    await send_message(update, context, session.message('accepted'), session)


async def get_media(update, context):
    session = get_session(update.message)

    session.check_access()

    await context.bot.send_document(chat_id=update.effective_chat.id,
                                    document=open(media_service.get_all(), 'rb'),
                                    reply_markup=ReplyKeyboardMarkup(
                                        [list(r.keys()) for r in buttons(session.user())],
                                        one_time_keyboard=True))

    media_service.delete_tmp()


def get_daily_report(session) -> str:
    cjobs = excel_service.from_today()
    masters = {cj.job.master for cj in cjobs}
    outer = {j.master for j in job_service.get_all_active() if j.master not in masters}

    return f'Сегодня ({datetime.now(tz=timezone.utc).astimezone().strftime("%Y-%m-%d (%H-%M)")})\n' + \
           'Внесли:\n' + \
           '\n'.join([f'\t- {e}' for e in masters]) + '\n\n' + \
           'Не внесли:\n\t' + \
           '\n'.join([f'\t- {e}' for e in outer])


async def ru(update, context):
    session = get_session(update.message)

    session.change_lang('ru')
    await send_message(update, context, session.message('to-ru'), session)


async def en(update, context):
    session = get_session(update.message)

    session.change_lang('en')
    await send_message(update, context, session.message('to-en'), session)


async def lang(update: Update, context):
    session = get_session(update.message)

    def from_unicode(text):
        return ''.join(list(text))

    def to_unicode(text):
        return ''.join(['\\u' + ('%04x' % ord(e)) for e in text])

    try:
        marker = update.message.text.split(' ')[1]
    except IndexError:
        raise RequestError('invalid-command-usage')

    global languages
    if marker not in languages:
        languages.add(marker)

        await send_message(update, context, session.message('new-lang').format(marker), None)

        with open('./resources/MessageBundle.properties', 'a') as f:
            glob = dict(ResourceBundle.get_bundle('./resources/MessageBundle'))

            sentence = from_unicode(glob['to-en'])
            translated = translator.translate(sentence, src='en', dest=marker)
            f.write(f'to-{marker}={to_unicode(translated.text)}\n')

        with open(f'./resources/MessageBundle_{marker}.properties', 'w') as f:
            content = dict(ResourceBundle.get_bundle('./resources/MessageBundle', 'en'))

            values = [translator.translate(sentence, src='en', dest=marker).text for sentence in content.values()]

            for k, v in zip(content.keys(), values):
                f.write(f'{k}=' + to_unicode(v) + '\n')

    session.change_lang(marker)
    await send_message(update, context, session.message(f'to-{marker}'), session)


async def promote(update, context):
    session = get_session(update.message)

    session.check_access()

    try:
        user_id = int(update.message.text.split(' ')[1])

        user_service.make_admin(session.user(), update.message.chat_id, user_service.get_by_id(user_id))

        await send_message(update, context, session.message('accepted'), session)
    except IndexError:
        raise RequestError('invalid-command-usage')
    except ValueError:
        raise RequestError('invalid-input-format')


async def list_users(update, context):
    get_session(update.message).check_access()

    await send_message(update, context,
                       '\n'.join([f'{user.name} - {user.id}' for user in user_service.get_all()]),
                       get_session(update.message))


def show_job_list_navigation(session, job_list):
    button_list = [
        [InlineKeyboardButton(str(session.pointer + i + 1), callback_data=v) for i, v in enumerate(job_list.values())],

        [] if session.hit_bounds() is None
        else [InlineKeyboardButton("←", callback_data='previous'), InlineKeyboardButton("→", callback_data='next')]
        if session.hit_bounds() == 0
        else [InlineKeyboardButton("←", callback_data='previous')] if session.hit_bounds() > 0
        else [InlineKeyboardButton("→", callback_data='next')],

        [InlineKeyboardButton("Все работы", callback_data='all')] if session.only_active else [],
        [InlineKeyboardButton("Назад", callback_data='backwards')] if session.index > 0 else []
    ]

    return InlineKeyboardMarkup(button_list)


def show_job_list(session, job_list):
    return '\n'.join([session.message('in-type')] + [f'{session.pointer + i + 1}) {job}'
                                                     for i, job in enumerate(job_list)])


async def nav_menu(update, context, on_pass, next_fun, prefix, my_job_list_show, next_job_list_show):
    query = update.callback_query

    session = get_session(query)

    data = query.data
    if on_pass is not None:
        await next_fun(update, context)
        return

    if data == 'next' or data == 'previous':
        mark = session.pointer

        if data == 'next':
            session.move_right()
        else:
            session.move_left()

        if mark == session.pointer:
            return

        job_list = {my_job_list_show(job)[0]: my_job_list_show(job)[1] for job in session.interval()}

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=show_job_list(session, job_list),
                                          reply_markup=show_job_list_navigation(session, job_list))
    else:
        if not data.startswith(prefix):
            raise RequestError('invalid-request-sequence')

        session.set_by_job(int(data[2:]))

        job_list = {next_job_list_show(job)[0]: next_job_list_show(job)[1] for job in session.interval()}

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=show_job_list(session, job_list),
                                          reply_markup=show_job_list_navigation(session, job_list))


async def navigation(update, context):
    query = update.callback_query

    session = get_session(query)

    data = query.data
    if data == 'yes':
        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=session.message('load-media'))
        return
    elif data == 'no':
        session.backwards()

        job_list = {f'{job.title} ({job.measurement})': f'ti{job.id}' for job in session.interval()}

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=show_job_list(session, job_list),
                                          reply_markup=show_job_list_navigation(session, job_list))
        return

    if data == 'all':
        session.start(False)

        job_list = {job.stage: f'st{job.id}' for job in session.interval()}

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=show_job_list(session, job_list),
                                          reply_markup=show_job_list_navigation(session, job_list))
        return

    if data == 'backwards':
        session.backwards()

        session.update_job_list()

        def nxt(job):
            return {0: job.stage, 1: job.master, 2: job.gen_plan, 3: f'{job.title} ({job.measurement})'}[session.index],\
                   {0: f'st{job.id}', 1: f'ms{job.id}', 2: f'gp{job.id}', 3: f'ti{job.id}'}[session.index]

        job_list = {nxt(job)[0]: nxt(job)[1] for job in session.interval()}

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=show_job_list(session, job_list),
                                          reply_markup=show_job_list_navigation(session, job_list))
        return

    if data not in ['next', 'previous'] and session.answer_builder is not None:
        def def_time(text: str):
            now = datetime.now()

            shift = {
                'day': timedelta(days=1),
                'week': timedelta(days=7),
                'month': timedelta(days=31),
                'year': timedelta(days=365),
                'total': timedelta(days=now.year * 365 + now.month * 30 + now.day - 2),
                # hack to not getting into BC
            }[text]

            return datetime(now.year, now.month, now.day) - shift

        await session.answer_builder(def_time(data))
        return

    await nav_menu(update, context, session.stage(), navigation_master, 'st',
                   lambda job: (job.stage, f'st{job.id}'),
                   lambda job: (job.master, f'ms{job.id}'))


async def navigation_master(update, context):
    query = update.callback_query

    session = get_session(query)

    await nav_menu(update, context, session.master(), navigation_gen_plan, 'ms',
                   lambda job: (job.master, f'ms{job.id}'),
                   lambda job: (job.gen_plan, f'gp{job.id}'))


async def navigation_gen_plan(update, context):
    query = update.callback_query

    session = get_session(query)

    await nav_menu(update, context, session.gen_plan(), navigation_title, 'gp',
                   lambda job: (job.gen_plan, f'gp{job.id}'),
                   lambda job: (f'{job.title} ({job.measurement})', f'ti{job.id}'))


async def navigation_title(update, context):
    query = update.callback_query

    session = get_session(query)

    data = query.data
    if data == 'next' or data == 'previous':
        mark = session.pointer

        if data == 'next':
            session.move_right()
        else:
            session.move_left()

        if mark == session.pointer:
            return

        job_list = {f'{job.title} ({job.measurement})': f'ti{job.id}' for job in session.interval()}

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=show_job_list(session, job_list),
                                          reply_markup=show_job_list_navigation(session, job_list))
    else:
        if not data.startswith('ti'):
            raise RequestError('invalid-request-sequence')
        job_id = int(data[2:])

        session.set_by_job(job_id, False)
        await select_number(update, context, job_service.get_by_id(job_id), query.message)


async def select_number(update, context, job, message):
    session = get_session(update.callback_query)

    text = session.message('work-type') + ':\n' + \
           f'{job.title}, {job.measurement}:\n' + \
           session.message('in-count') + ':'

    await context.bot.editMessageText(chat_id=message.chat_id,
                                      message_id=message.message_id,
                                      text=text)


async def accept_count(update, context):
    session = get_session(update.message)

    try:
        count = float(update.message.text.replace(',', '.'))

        request = session.generate_request(count)
        await run_request(update, context, request)
    except ValueError as e:
        raise RequestError('invalid-input-format')


headmaster = [{'Выбор подрядной организации': make_report}]
imports = [{'Экспортировать в csv': export_csv, 'Экспортировать в xlsx': export_xlsx,
            'Посмотреть загруженные фотографии': get_media},
           {'Загрузить новый список работ': month_update}]
upgrade = [{'Пользователи': list_users}, {'Повысить': promote}]
langs = [{'en': en, 'ru': ru}]  # , 'other': lang}]
helping = [{'Помощь': help}]


def buttons(user: Union[User, Superuser]) -> List[dict]:
    if isinstance(user, Superuser):
        return headmaster + imports + upgrade + helping
    elif user.admin_in is not None and len(user.admin_in) != 0:
        return imports + helping
    else:
        return headmaster + helping


async def buttons_text(update: Update, context):
    session = get_session(update.message)

    try:
        fun = {k: v for r in buttons(session.user()) for k, v in r.items()}[update.message.text]

        await fun(update, context)
    except KeyError:
        raise RequestError('invalid-command-usage')


async def full_request(update: Update, context):
    session = get_session(update.message)

    # 0 - full
    # 1 - section_number
    # 2 - `part before dot` section_number
    # 3 - title
    # 4 - count
    # 5 - measurement
    match = re.search('^\s*((\d*[.])?\d+),\s*([^,]+),\s*(\d+),\s*(\S+)\s*$', update.message.text)

    session.reset()
    session.apply(user_service, update.message.from_user.id)
    session.apply(job_service, float(match.group(1)), match.group(3), match.group(5))
    request = session.apply(int(match.group(4)))

    await run_request(update, context, request)


async def run_request(update, context, request):
    session = get_session(update.message)

    session.last_message_id = process(request)

    await send_message(update, context, session.message('accepted'), None)

    await update.message.reply_text(session.message('decide-load-media'),
                                    reply_markup=InlineKeyboardMarkup(
                                        [[InlineKeyboardButton(k, callback_data=v) for k, v in
                                          {'Да': 'yes', 'Нет': 'no'}.items()]]))

    # session.reset()
    #
    # await make_report(update, context)


async def error_handler(update: Update, context):
    if update is None:
        raise context.error
    elif update.callback_query is None:
        session = get_session(update.message)
    else:
        session = get_session(update.callback_query)

    err = context.error

    if isinstance(err, RequestError):
        await send_message(update, context,
                           session.message('error') + ': ' + session.message(err.bundle_key), session)
    elif isinstance(err, BadRequest):
        return
    else:
        await send_message(update, context, session.message('unknown-error-appeared'), session)

        report_admin(err)

    session.reset()


logs_path = '/bot/logs/'
os.makedirs(logs_path, exist_ok=True)


def report_admin(err):
    with open(logs_path + 'report.log', 'a') as f:
        # noinspection PyBroadException
        try:
            raise err
        except Exception:
            f.write(traceback.format_exc())


def log(message: str, info: bool = True):
    logging.info(message)

    with open(logs_path + 'log.log', 'a') as f:
        f.write(f'{"INFO" if info else " ERR"} - {message}\n')
