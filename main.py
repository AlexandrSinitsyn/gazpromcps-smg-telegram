import logging
import os

# noinspection PyPackageRequirements
from telegram.ext import CommandHandler, ApplicationBuilder, MessageHandler, CallbackQueryHandler, filters
# noinspection PyPackageRequirements
from telegram.ext.filters import Regex

from controller.bot import *
import dto
import database
from database.script import run
import repository
import service


logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)


if __name__ == '__main__':
    run()

    TOKEN = os.environ.get('TOKEN')
    application = ApplicationBuilder().token(TOKEN).build()

    languages = ['ru', 'en', 'lang']

    for name in ['start', 'help', 'make_report', 'export_text', 'export_csv', 'promote'] + languages:
        application.add_handler(CommandHandler(name, locals()[name]))

    application.add_handler(CallbackQueryHandler(navigation))
    application.add_handler(MessageHandler(filters.Text(), accept_count))
    application.add_handler(MessageHandler(filters.Text(), buttons_text))
    # <ws>
    #   section_number <comma> <ws>
    #   name <comma> <ws>
    #   count <comma> <ws>
    #   measurement
    # <ws>
    application.add_handler(MessageHandler(Regex('^\s*([0-9]*[.])?[0-9]+,\s*[^,]+,\s*\d+,\s*\S+\s*$'), full_request))

    application.add_error_handler(error_handler)

    application.run_polling()
