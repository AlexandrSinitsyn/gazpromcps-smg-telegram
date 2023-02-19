import logging
import os
import re
import traceback
from datetime import datetime

import ResourceBundle
from googletrans import Translator
from typing import List, Union, Dict, Optional

# noinspection PyPackageRequirements
from telegram.ext import ContextTypes
# noinspection PyPackageRequirements
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup, ReplyKeyboardMarkup

from dto.job import Job
from dto.request import Request
from exceptions.exceptions import RequestError
from dto.response import Response
from dto.user import User, Superuser
from service.excel_service import ExcelService
from service.job_service import JobService
from service.user_service import UserService


class Session:
    pointer = 0
    request_builder = None
    jobs = None  # type: List[Job]
    __master = None  # type: Optional[str]
    __locale = 'ru'
    __user_id: int
    __bundle = None  # type: ResourceBundle

    def start(self):
        self.reset()

        self.jobs = list({job.master: job for job in job_service.get_all()}.values())

    def user(self) -> Union[User, Superuser]:
        return user_service.get_by_id(self.__user_id)

    def set_user(self, user_id: int):
        self.__user_id = user_id

    def check_access(self):
        if isinstance(self.user(), Superuser) or\
                (self.user().admin_in is not None and len(self.user().admin_in) != 0):
            pass
        else:
            raise RequestError('not-allowed')

    def master(self):
        return self.__master

    def set_master_by_job(self, job_id: int):
        self.__master = job_service.get_by_id(job_id).master
        self.jobs = job_service.get_by_master(self.__master)
        self.pointer = 0

    def apply(self, *args):
        try:
            if self.request_builder is None:
                self.request_builder = Request.generate(*args)
            else:
                self.request_builder = self.request_builder(*args)

            return self.request_builder
        except TypeError:
            raise RequestError('invalid-request-sequence')

    def move_left(self):
        self.pointer = max(self.pointer - LIST_SIZE, 0)

    def move_right(self):
        if self.pointer + LIST_SIZE <= len(self.jobs):
            self.pointer += LIST_SIZE

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
        self.pointer = 0
        self.request_builder = None
        self.__master = None


LIST_SIZE = 4
all_sessions = {}  # type: Dict[int, Session]
translator = Translator()

user_service = UserService()
job_service = JobService()
excel_service = ExcelService()


def get_session(user_id):
    return all_sessions[user_id]


def process(request: Request):
    job_service.day_activity(request.get_job())


def answer(request: Request, as_file: bool = False, store: bool = False) -> Response:
    csv = excel_service.export_csv()

    if as_file or store:
        file_name = excel_service.save()

    if as_file:
        # noinspection PyUnboundLocalVariable
        return request.response(csv_path=file_name)
    else:
        return request.response(content=csv)


async def send_message(update: Update, context: ContextTypes.DEFAULT_TYPE, message: str, reply_markup):
    await context.bot.send_message(chat_id=update.effective_chat.id, text=message,
                                   reply_markup=reply_markup)


async def start(update: Update, context):
    session = Session()
    all_sessions[update.message.from_user.id] = session

    telegram_user = update.message.from_user

    if user_service.get_by_id(telegram_user.id) is None:
        user_service.register(User(telegram_user.id, telegram_user.full_name,
                                   [update.message.chat_id], [], datetime.now()))

    session.set_user(telegram_user.id)

    await send_message(update, context, session.message('start'), reply_markup=ReplyKeyboardMarkup(
        [list(r.keys()) for r in buttons(session.user())], one_time_keyboard=True))


# noinspection PyShadowingBuiltins
async def help(update: Update, context):
    session = get_session(update.message.from_user.id)

    await send_message(update, context,
                       session.message('description') + '\n\n' + session.message('help'), None)


async def reload(update, context):
    session = get_session(update.message.from_user.id)

    await send_message(update, context, 'ok', reply_markup=ReplyKeyboardMarkup(
        [list(r.keys()) for r in buttons(session.user())], one_time_keyboard=True))


async def make_report(update: Update, context):
    session = get_session(update.message.from_user.id)

    session.start()

    job_list = {job.master: job.id for job in session.interval()}

    await update.message.reply_text(show_job_list(session, job_list), reply_markup=show_job_list_navigation(job_list))


async def export_csv(update: Update, context):
    session = get_session(update.message.from_user.id)

    session.check_access()

    request = Request(session.user(), Job.fake(), -1)

    await context.bot.send_document(chat_id=update.effective_chat.id,
                                    document=open(answer(request, as_file=True).path(), 'rb'))


async def export_text(update: Update, context):
    session = get_session(update.message.from_user.id)

    session.check_access()

    request = Request(session.user(), Job.fake(), -1)

    await send_message(update, context, answer(request).content(), None)


async def ru(update, context):
    session = get_session(update.message.from_user.id)

    session.change_lang('ru')
    await send_message(update, context, session.message('to-ru'), None)


async def en(update, context):
    session = get_session(update.message.from_user.id)

    session.change_lang('en')
    await send_message(update, context, session.message('to-en'), None)


async def lang(update: Update, context):
    session = get_session(update.message.from_user.id)

    def from_unicode(text):
        return ''.join(list(text))

    def to_unicode(text):
        return ''.join(['\\u' + ('%04x' % ord(e)) for e in text])

    try:
        marker = update.message.text.split(' ')[1]
    except IndexError:
        raise RequestError('invalid-command-usage')

    await send_message(update, context, session.message('new-lang').format(marker), None)

    with open('./resources/MessageBundle.properties', 'a') as f:
        glob = dict(ResourceBundle.get_bundle('./resources/MessageBundle'))

        sentence = from_unicode(glob['to-en'])
        translated = translator.translate(sentence, src='en', dest=marker)
        f.write(f'to-{marker}=' + to_unicode(translated.text))

    with open(f'./resources/MessageBundle_{marker}.properties', 'w') as f:
        content = dict(ResourceBundle.get_bundle('./resources/MessageBundle', 'en'))

        values = [translator.translate(sentence, src='en', dest=marker).text for sentence in content.values()]

        for k, v in zip(content.keys(), values):
            f.write(f'{k}=' + to_unicode(v) + '\n')

    session.change_lang(marker)
    await send_message(update, context, session.message(f'to-{marker}'), None)


async def promote(update, context):
    session = get_session(update.message.from_user.id)

    session.check_access()

    try:
        user_id = int(update.message.text.split(' ')[1])

        user_service.make_admin(session.user(), update.message.chat_id, user_service.get_by_id(user_id))

        await send_message(update, context, 'ok', None)
    except IndexError:
        raise RequestError('invalid-command-usage')
    except ValueError:
        raise RequestError('invalid-number-format')


def show_job_list_navigation(job_list):
    button_list = [
        [InlineKeyboardButton(k, callback_data=v) for k, v in job_list.items()],

        [InlineKeyboardButton("←", callback_data='previous'), InlineKeyboardButton("→", callback_data='next')]
    ]

    return InlineKeyboardMarkup(button_list)


def show_job_list(session, job_list):
    return '\n'.join([session.message('in-type')] + [job for job in job_list])


async def navigation(update, context):
    query = update.callback_query

    session = get_session(query.from_user.id)

    if session.master() is not None:
        await navigation_title(update, context)
        return

    if query.data == 'next' or query.data == 'previous':
        mark = session.pointer

        if query.data == 'next':
            session.move_right()
        else:
            session.move_left()

        if mark == session.pointer:
            return

        job_list = {job.master: job.id for job in session.interval()}

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=show_job_list(session, job_list),
                                          reply_markup=show_job_list_navigation(job_list))
    else:
        session.apply(user_service, query.message.from_user.id)

        session.set_master_by_job(query.data)

        job_list = {job.title: job.id for job in job_service.get_by_master(session.master())}

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=show_job_list(session, job_list),
                                          reply_markup=show_job_list_navigation(job_list))


async def navigation_title(update, context):
    query = update.callback_query

    session = get_session(query.from_user.id)

    if query.data == 'next' or query.data == 'previous':
        mark = session.pointer

        if query.data == 'next':
            session.move_right()
        else:
            session.move_left()

        if mark == session.pointer:
            return

        job_list = {job.title: job.id for job in job_service.get_by_master(session.master())}

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=show_job_list(session, job_list),
                                          reply_markup=show_job_list_navigation(job_list))
    else:
        await select_number(update, context, job_service.get_by_id(int(query.data)), query.message)


async def select_number(update, context, job, message):
    session = get_session(update.callback_query.from_user.id)

    text = session.message('work-type') + ': ' +\
           str(job).replace(',', '\t') + ':\n' +\
           session.message('in-count') + ':'

    await context.bot.editMessageText(chat_id=message.chat_id,
                                      message_id=message.message_id,
                                      text=text)

    session.apply(job_service, job.master, job.title)


async def accept_count(update, context):
    session = get_session(update.message.from_user.id)

    try:
        count = int(update.message.text)

        request = session.apply(count)
        await run_request(update, context, request)
    except ValueError:
        raise RequestError('invalid-number-format')

headmaster = [{'Подрядчик': make_report}]
imports = [
    {'Импортировать текстом': export_text, 'Импортировать в csv': export_csv},
    {'Promote': promote},
]
langs = [{'EN': en, 'RU': ru, 'OTHER': lang}]


def buttons(user: Union[User, Superuser]) -> List[dict]:
    if isinstance(user, Superuser):
        return headmaster + imports + langs
    elif user.admin_in is not None and len(user.admin_in) != 0:
        return imports + langs
    else:
        return headmaster + langs


async def buttons_text(update: Update, context):
    session = get_session(update.message.from_user.id)

    try:
        fun = {k: v for r in buttons(session.user()) for k, v in r.items()}[update.message.text]

        await fun(update, context)
    except KeyError:
        raise RequestError('invalid-command-usage')


async def full_request(update: Update, context):
    session = get_session(update.message.from_user.id)

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
    session = get_session(update.message.from_user.id)

    process(request)

    await send_message(update, context, session.message('accepted'), None)

    session.reset()


async def error_handler(update: Update, context):
    if update.callback_query is None:
        session = get_session(update.message.from_user.id)
    else:
        session = get_session(update.callback_query.from_user.id)

    err = context.error

    if isinstance(err, RequestError):
        await send_message(update, context,
                           session.message('error') + ': ' + session.message(err.bundle_key), None)
    else:
        await send_message(update, context, session.message('unknown-error-appeared'), None)

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
