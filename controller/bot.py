import re
import ResourceBundle
from typing import List

# noinspection PyPackageRequirements
from telegram.ext import ContextTypes
# noinspection PyPackageRequirements
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup, ReplyKeyboardMarkup, KeyboardButton

from dto.job import Job
from dto.request import Request, RequestError
from dto.response import Response
from service.excel_service import ExcelService
from service.job_service import JobService
from service.user_service import UserService


class Session:
    pointer = 0
    request_builder = None
    jobs = None
    locale = 'ru'
    __bundle = None  # type: ResourceBundle

    def start(self):
        self.reset()

        self.jobs = job_service.get_all()

    def apply(self, *args):
        if self.request_builder is None:
            self.request_builder = Request.generate(*args)
        else:
            self.request_builder = self.request_builder(*args)

        return self.request_builder

    def move_left(self):
        self.pointer = max(self.pointer - LIST_SIZE, 0)

    def move_right(self):
        if self.pointer + LIST_SIZE <= len(self.jobs):
            self.pointer += LIST_SIZE

    def interval(self) -> List[Job]:
        return self.jobs[self.pointer: min(self.pointer + LIST_SIZE, len(self.jobs))]

    def change_lang(self, lang: str):
        self.locale = lang

        self.__bundle = ResourceBundle.get_bundle('./resources/MessageBundle', self.locale)

    def message(self, key: str) -> str:
        if self.__bundle is None:
            self.__bundle = ResourceBundle.get_bundle('./resources/MessageBundle', self.locale)

        return self.__bundle.get(key)

    def reset(self):
        self.pointer = 0
        self.request_builder = None


LIST_SIZE = 8
session = Session()

user_service = UserService()
job_service = JobService()
excel_service = ExcelService()


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
    await context.bot.send_message(chat_id=update.effective_chat.id, text=message, reply_markup=reply_markup)


async def start(update: Update, context):
    await send_message(update, context, session.message('start'), reply_markup=ReplyKeyboardMarkup(
        [['Создать новый отчет'], ['Импортировать текстом'], ['Импортировать в csv'], ['EN', 'RUS', 'OTHER']],
        one_time_keyboard=True))


# noinspection PyShadowingBuiltins
async def help(update: Update, context):
    await send_message(update, context,
                       session.message('description') + '\n\n' + session.message('help'), None)


async def make_report(update: Update, context):
    session.start()

    await update.message.reply_text(show_job_list(), reply_markup=show_job_list_navigation())


async def export_csv(update: Update, context):
    request = Request(user_service.get_by_id(update.message.from_user.id), Job.fake(), -1)

    await context.bot.send_document(chat_id=update.effective_chat.id,
                                    document=open(answer(request, as_file=True).path(), 'rb'))


async def export_text(update: Update, context):
    request = Request(user_service.get_by_id(update.message.from_user.id), Job.fake(), -1)

    await send_message(update, context, answer(request).content(), None)


async def ru(update, context):
    session.change_lang('ru')
    await send_message(update, context, session.message('to-ru'), None)


async def en(update, context):
    session.change_lang('en')
    await send_message(update, context, session.message('to-en'), None)


def show_job_list_navigation():
    button_list = [
        [InlineKeyboardButton(str(job.section_number), callback_data=job.id) for job in session.interval()],

        [InlineKeyboardButton("←", callback_data='previous'), InlineKeyboardButton("→", callback_data='next')]
    ]

    return InlineKeyboardMarkup(button_list)


def show_job_list():
    return '\n'.join([session.message('in-type')] +
                     [str(job).replace(',', '\t') for job in session.interval()])


async def navigation(update, context):
    query = update.callback_query

    if query.data == 'next' or query.data == 'previous':
        mark = session.pointer

        if query.data == 'next':
            session.move_right()
        else:
            session.move_left()

        if mark == session.pointer:
            return

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=show_job_list(), reply_markup=show_job_list_navigation())
    else:
        session.apply(user_service, query.message.from_user.id)

        await select_number(context, job_service.get_by_id(int(query.data)), query.message)


async def select_number(context, job, message):
    text = session.message('work-type') + ': ' +\
           str(job).replace(',', '\t') + ':\n' +\
           session.message('in-count') + ':'

    await context.bot.editMessageText(chat_id=message.chat_id,
                                      message_id=message.message_id,
                                      text=text)

    session.apply(job_service, job.section_number, job.title, job.measurement)


async def accept_count(update, context):
    if session.request_builder is None:
        return
    try:
        int(update.message.text)
        request = session.apply(int(update.message.text))
        await run_request(update, context, request)
    except ValueError:
        await send_message(update, context, 'Возникла ошибка: Некорректное количество. Введите число', None)


async def buttons_text(update, context):
    if update.message.text == 'Создать новый отчет':
        await make_report(update, context)
    elif update.message.text == 'Импортировать текстом':
        await export_text(update, context)
    elif update.message.text == 'Импортировать в csv':
        await export_csv(update, context)
    elif update.message.text == 'EN':
        await en(update, context)
    elif update.message.text == 'RUS':
        await ru(update, context)
    # elif update.message.text == 'OTHER':
    #     await en(update, context)


async def full_request(update: Update, context):
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
    try:
        process(request)

        await send_message(update, context, session.message('accepted'), None)
    except RequestError as e:
        await send_message(update, context,
                           session.message('error') + ': ' + session.message(e.bundle_key), None)

    session.reset()
