import logging
import os

from telegram.ext import CommandHandler, CallbackQueryHandler, ContextTypes, \
    ApplicationBuilder
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup

logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)

works = ['work1', 'work2', 'work3', 'work4', 'work5', 'work6', 'work7', 'work8', 'work9', 'work10', 'work11', 'work12',
         'work13']

first_element = 8


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await context.bot.send_message(chat_id=update.effective_chat.id, text="Здравствуйте! Внесите данные через меня")


async def make_report(update: Update):
    await update.message.reply_text(make_message(first_element), reply_markup=make_button_message(first_element))


def make_button_message(first):
    last_element = first + 8
    if last_element > len(works):
        last_element = len(works)
    button_list = [
        [InlineKeyboardButton(str(i + 1), callback_data=i) for i in range(first, last_element)],

        [InlineKeyboardButton("Назад", callback_data='previous'), InlineKeyboardButton("Вперёд", callback_data='next')]
    ]
    reply_markup = InlineKeyboardMarkup(button_list)
    return reply_markup


def make_message(first):
    message = 'Выберите вид работы:'
    last_element = first + 8
    if last_element > len(works):
        last_element = len(works)
    for i in range(first, last_element):
        message += f'\n{i + 1}    {works[i]}'
    return message


async def button(update, context):
    query = update.callback_query
    new_first = first_element
    if query.data == 'next':
        if new_first + 8 < len(works):
            new_first += 8
        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=update.callback_query.message.message_id,
                                          text=make_message(new_first), reply_markup=make_button_message(new_first))
    elif query.data == 'previous':
        if new_first - 8 < 0:
            new_first = 0
        else:
            new_first -= 8
        await context.bot.editMessageText(chat_id=query.message.chat_id,
                                          message_id=update.callback_query.message.message_id,
                                          text=make_message(new_first), reply_markup=make_button_message(new_first))
    else:
        await select_number(query, update, context)


async def select_number(query, update, context):
    message = f'Вид работы: "{works[int(query.data)]}"\nВведите количество:'
    await context.bot.editMessageText(chat_id=query.message.chat_id,
                                      message_id=update.callback_query.message.message_id,
                                      text=message)


if __name__ == '__main__':
    TOKEN = os.environ.get('TOKEN')
    application = ApplicationBuilder().token(TOKEN).build()

    start_handler = CommandHandler('start', start)
    make_report_handler = CommandHandler('make_report', make_report)
    application.add_handler(start_handler)
    application.add_handler(make_report_handler)
    application.add_handler(CallbackQueryHandler(button))

    application.run_polling()
