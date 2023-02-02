import logging
import os
from typing import List

# noinspection PyPackageRequirements
from telegram.ext import CommandHandler, ContextTypes, \
    ApplicationBuilder, MessageHandler, CallbackQueryHandler
# noinspection PyPackageRequirements
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
# noinspection PyPackageRequirements
from telegram.ext.filters import Regex

from dto.job import Job
from dto.request import Request
from dto.response import Response
from service.excel_service import ExcelService
from service.job_service import JobService
from service.user_service import UserService

logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)

LIST_SIZE = 8
first_index_pointer = 0
request_builder = None

user_service = UserService()
job_service = JobService()
excel_service = ExcelService()


def process(request: Request):
    job_service.day_activity(request.get_job())


# todo checking buttons
def answer(request: Request, store: bool = False) -> Response:
    csv = excel_service.export_csv()

    if store:
        excel_service.save()

    # todo content | csv_path
    return request.response(content=csv)


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await context.bot.send_message(chat_id=update.effective_chat.id,
                                   text="Здравствуйте! Вы можете внести данные через меня")


async def make_report(update: Update, context):
    jobs = job_service.get_all()

    await update.message.reply_text(make_message(jobs), reply_markup=make_button_message(jobs))


async def export_csv(update: Update, context):
    request = Request(user_service.get_by_id(update.message.from_user.id), Job.fake(), -1)

    response = answer(request, False)

    response.to_csv("C:/Users/na3t1/Desktop/bot/report .csv")
    await context.bot.send_document(chat_id=update.effective_chat.id, document=
    open("C:/Users/na3t1/Desktop/bot/report.csv", 'rb'))


async def export_text(update: Update, context):
    request = Request(user_service.get_by_id(update.message.from_user.id), Job.fake(), -1)

    response = answer(request, False)
    await context.bot.send_message(chat_id=update.effective_chat.id, text=response.content())


def make_button_message(jobs):
    button_list = [
        [InlineKeyboardButton(str(job.section_number), callback_data=job.id)
         for job in jobs][first_index_pointer: min(first_index_pointer + LIST_SIZE, len(jobs))],

        [InlineKeyboardButton("Назад", callback_data='previous'), InlineKeyboardButton("Вперёд", callback_data='next')]
    ]
    reply_markup = InlineKeyboardMarkup(button_list)
    return reply_markup


def make_message(jobs: List[Job]):
    return '\n'.join(['Выберите вид работы:'] +
                     [str(job).replace(',', '\t') for job in jobs][
                     first_index_pointer: first_index_pointer + LIST_SIZE])


async def button(update, context):
    global first_index_pointer

    jobs = job_service.get_all()

    query = update.callback_query

    if query.data == 'next' or query.data == 'previous':
        first_index_pointer = \
            min(first_index_pointer + LIST_SIZE, len(jobs)) if query.data == 'next' \
                else max(first_index_pointer - LIST_SIZE, 0)

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=query.message.message_id,
                                          text=make_message(jobs), reply_markup=make_button_message(jobs))
    else:
        global request_builder

        request_builder = Request.generate(user_service, query.message.from_user.id)

        request_builder = await select_number(context, job_service.get_by_id(int(query.data)), query.message)


async def select_number(context, job, message):
    await context.bot.editMessageText(chat_id=message.chat_id,
                                      message_id=message.message_id,
                                      text=f"Вид работы: '{str(job).replace(',', '  ')}':\nВведите количество:")

    # noinspection PyCallingNonCallable
    return request_builder(job_service, job.section_number, job.title, job.measurement)


async def inline_query(update, context):
    global request_builder

    if request_builder is None:
        return
    if int(update.message.text) < 1:
        await context.bot.send_message(chat_id=update.effective_chat.id, text='Введите число больше 0')
        return
    else:
        # noinspection PyCallingNonCallable
        request = request_builder(int(update.message.text))

        process(request)

        await context.bot.send_message(chat_id=update.effective_chat.id, text='Ваши данные успешно добавлены')

        request_builder = None


if __name__ == '__main__':
    TOKEN = os.environ.get('TOKEN')
    application = ApplicationBuilder().token(TOKEN).build()

    start_handler = CommandHandler('start', start)
    make_report_handler = CommandHandler('make_report', make_report)
    application.add_handler(start_handler)
    application.add_handler(make_report_handler)
    application.add_handler(CommandHandler('export_text', export_text))
    application.add_handler(CommandHandler('export_csv', export_csv))
    application.add_handler(CallbackQueryHandler(button))
    application.add_handler(MessageHandler(Regex('\d+'), inline_query))

    application.run_polling()
