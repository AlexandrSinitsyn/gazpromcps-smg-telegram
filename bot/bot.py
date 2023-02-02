import logging
import os

# noinspection PyPackageRequirements
from telegram.ext import CommandHandler, ContextTypes, \
    ApplicationBuilder, MessageHandler, CallbackQueryHandler
# noinspection PyPackageRequirements
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
# noinspection PyPackageRequirements
from telegram.ext.filters import Regex

logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)

works = ['Первая супер важная и особенная, а еще интересная, необходимая работа'] + ['work' + str(i) for i in range(12)]

LIST_SIZE = 8
first_index_pointer = 0


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await context.bot.send_message(chat_id=update.effective_chat.id, text="Здравствуйте! Внесите данные через меня")


async def make_report(update: Update, _):
    await update.message.reply_text(make_message(first_index_pointer), reply_markup=make_button_message(first_index_pointer))


def make_button_message(first):
    button_list = [
        [InlineKeyboardButton(str(i + 1), callback_data=i) for i in range(first, min(first + LIST_SIZE, len(works)))],

        [InlineKeyboardButton("Назад", callback_data='previous'), InlineKeyboardButton("Вперёд", callback_data='next')]
    ]
    reply_markup = InlineKeyboardMarkup(button_list)
    return reply_markup


def make_message(first):
    return '\n'.join(['Выберите вид работы:'] + [f'{i + 1}\t{e}' for i, e in enumerate(works)][first : first + LIST_SIZE])


async def button(update, context):
    global first_index_pointer

    query = update.callback_query

    if query.data == 'next' or query.data == 'previous':
        first_index_pointer =\
            min(first_index_pointer + LIST_SIZE, len(works)) if query.data == 'next'\
                else max(first_index_pointer - LIST_SIZE, 0)

        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=update.callback_query.message.message_id,
                                          text=make_message(first_index_pointer), reply_markup=make_button_message(first_index_pointer))
    else:
        await select_number(query, update, context)


async def select_number(query, update, context):
    message = f'Вид работы: "{works[int(query.data)]}":'
    await context.bot.editMessageText(chat_id=query.message.chat_id,
                                      message_id=update.callback_query.message.message_id,
                                      text=message)


async def inline_query(update, context):
    await context.bot.send_message(chat_id=update.effective_chat.id, text=update.message.text)


if __name__ == '__main__':
    TOKEN = os.environ.get('TOKEN')
    application = ApplicationBuilder().token(TOKEN).build()

    start_handler = CommandHandler('start', start)
    make_report_handler = CommandHandler('make_report', make_report)
    application.add_handler(start_handler)
    application.add_handler(make_report_handler)
    application.add_handler(CallbackQueryHandler(button))
    application.add_handler(MessageHandler(Regex('\d+'), inline_query))

    application.run_polling()
