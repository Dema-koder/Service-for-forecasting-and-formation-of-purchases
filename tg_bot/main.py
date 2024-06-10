import asyncio
import logging
import uuid
from aiogram import Bot, Dispatcher, types
from aiogram.filters import CommandStart
from aiogram.fsm.context import FSMContext
from aiogram.fsm.state import StatesGroup, State
from aiogram.utils.keyboard import InlineKeyboardBuilder

from server_methods import server_get_token, server_post_session, server_delete_token, server_token_expired, \
    server_refresh_expired, server_refresh_token

# Set up logging
logging.basicConfig(level=logging.INFO)

AUTH_LINK = "http://127.0.0.1:8080/realms/bot_realm/protocol/openid-connect/auth?client_id=bot_client&response_type=code&scope=openid&redirect_uri=http://127.0.0.1:9111/authenticate&state="

# Initialize bot and dispatcher
bot = Bot(token="token")
dp = Dispatcher()


class Form(StatesGroup):
    checking_auth = State()
    choosing_action = State()
    stock_remains_item = State()
    predict_item = State()


def keyboard_builder(button_texts):
    builder = InlineKeyboardBuilder()
    for text in button_texts:
        builder.button(text=text, callback_data=text)
    builder.adjust(1)
    return builder.as_markup()


@dp.message(CommandStart())
async def start(message: types.Message, state: FSMContext):
    if server_get_token(message.from_user.id):
        await state.set_state(Form.choosing_action)
        await message.answer(f"Добро пожаловать в бота для автоматизации покупок!", parse_mode="html")
        await message.answer("Пожалуйста, выберите одно из предложенных ниже действий или опишите, что Вы хотите "
                             "сделать сообщением.", reply_markup=keyboard_builder(["Узнать складские остатки",
                                                                                   "Сформировать прогноз"]))
    else:
        auth_state = str(uuid.uuid4())
        server_post_session(message.from_user.id, auth_state)
        await state.set_state(Form.checking_auth)
        await message.answer(f"Добро пожаловать в бота для автоматизации покупок!\n<a href='{AUTH_LINK + auth_state}'>"
                             f"Перейдите по ссылке для авторизации, для того чтобы продолжить работу в боте.</a>\n<b>"
                             f"После авторизации нажмите кнопку ниже.</b>", parse_mode="html",
                             reply_markup=keyboard_builder("Я авторизовался ✅"))


@dp.callback_query(Form.checking_auth)
async def check_authorization(query: types.CallbackQuery, state: FSMContext):
    if server_get_token(query.from_user.id):
        await query.message.delete()
        await state.set_state(Form.choosing_action)
        await query.message.answer("Вы успешно авторизовались в боте!✅")
        await query.message.answer("Пожалуйста, выберите одно из предложенных ниже действий или опишите, что Вы хотите "
                                   "сделать сообщением.", reply_markup=keyboard_builder(["Узнать складские остатки",
                                                                                         "Сформировать прогноз"]))
    else:
        await query.message.delete()
        auth_state = str(uuid.uuid4())
        server_post_session(query.from_user.id, auth_state)
        await query.message.answer(f"Вы не авторизованы ❌\n<a href='{AUTH_LINK + auth_state}'>Перейдите "
                                   f"по ссылке для авторизации, для того чтобы продолжить работу в боте.</a>\n<b>После "
                                   f"авторизации нажмите кнопку ниже.</b>", parse_mode="html",
                                   reply_markup=keyboard_builder("Я авторизовался ✅"))


@dp.callback_query(Form.choosing_action)
async def choose_action(query: types.CallbackQuery, state: FSMContext):
    if server_refresh_expired(query.from_user.id):
        server_delete_token(query.from_user.id)
        auth_state = str(uuid.uuid4())
        server_post_session(query.from_user.id, auth_state)
        await state.set_state(Form.checking_auth)
        await query.message.answer(f"Вы не авторизованы ❌\n<a href='{AUTH_LINK + auth_state}'>Перейдите "
                                   f"по ссылке для авторизации, для того чтобы продолжить работу в боте.</a>\n<b>После "
                                   f"авторизации нажмите кнопку ниже.</b>", parse_mode="html",
                                   reply_markup=keyboard_builder("Я авторизовался ✅"))
    else:
        if server_token_expired(query.from_user.id):
            server_refresh_token(query.from_user.id)
        if query.data == "Узнать складские остатки":
            await state.set_state(Form.stock_remains_item)
            await query.message.answer("Введите название товара, чтобы узнать его остаток на складе.")
        else:
            await state.set_state(Form.predict_item)
            await query.message.answer("Введите название товара, по которому необходимо сформировать запрос.")


@dp.message(Form.stock_remains_item)
async def stock_remains(message: types.Message, state: FSMContext):
    # API GET
    await message.answer("Осталось 9213 шариковых ручек")


@dp.message(Form.predict_item)
async def predict_choose_period(message: types.Message, state: FSMContext):
    # API GET
    await message.answer("На какой период вы хотите сформировать прогноз?")


async def main():
    await dp.start_polling(bot, skip_updates=True)

if __name__ == '__main__':
    asyncio.run(main())