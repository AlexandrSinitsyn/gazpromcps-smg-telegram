from controller.bot import *
import dto
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

    for name in ['start', 'make_report', 'export_text', 'export_csv']:
        application.add_handler(CommandHandler(name, locals()[name]))

    application.add_handler(CallbackQueryHandler(button))
    application.add_handler(MessageHandler(Regex('^\d+$'), inline_query))

    application.run_polling()
