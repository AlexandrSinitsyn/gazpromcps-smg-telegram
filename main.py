# noinspection PyPackageRequirements
from telegram.ext import CommandHandler, ApplicationBuilder, MessageHandler, CallbackQueryHandler, filters
# noinspection PyPackageRequirements
from telegram.ext.filters import Regex

from controller.bot import *
import dto
import exceptions
import database
import repository
import service


logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)


if __name__ == '__main__':
    TOKEN = os.environ.get('TOKEN')
    application = ApplicationBuilder().token(TOKEN).build()

    languages = ['ru', 'en']  # , 'lang']
    stuff = ['reload', 'promote', 'list_users']
    data_managing = ['export_csv', 'export_xlsx', 'export_text', 'get_media', 'month_update']

    for name in ['start',  'help', 'make_report'] + data_managing + stuff + languages:
        application.add_handler(CommandHandler(name, locals()[name]))

    application.add_handler(CallbackQueryHandler(navigation))
    application.add_handler(MessageHandler(Regex('^\s*(-?[1-9]\d*|0)([.,]\d*)?\s*$'), accept_count))
    application.add_handler(MessageHandler(filters.Text(), buttons_text))
    application.add_handler(MessageHandler(filters.Document.FileExtension('xlsx'), accept_xlsx_month_update))
    application.add_handler(MessageHandler(filters.Document.ALL, accept_photo))
    application.add_handler(MessageHandler(filters.PHOTO, accept_photo))
    # <ws>
    #   section_number <comma> <ws>
    #   name <comma> <ws>
    #   count <comma> <ws>
    #   measurement
    # <ws>
    application.add_handler(MessageHandler(Regex('^\s*([0-9]*[.])?[0-9]+,\s*[^,]+,\s*\d+,\s*\S+\s*$'), full_request))

    application.add_error_handler(error_handler)

    application.run_polling()
