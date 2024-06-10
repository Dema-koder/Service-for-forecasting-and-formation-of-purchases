import asyncio
import logging
import uuid
from aiogram import Bot, Dispatcher, types
from aiogram.filters import CommandStart
from aiogram.fsm.context import FSMContext
from aiogram.fsm.state import StatesGroup, State
from aiogram.utils.keyboard import InlineKeyboardBuilder

# Set up logging
logging.basicConfig(level=logging.INFO)

AUTH_LINK = "http://127.0.0.1:8080/realms/bot_realm/protocol/openid-connect/auth?client_id=bot_client&response_type=code&scope=openid&redirect_uri=http://127.0.0.1:9111/authenticate&state="

# Initialize bot and dispatcher
bot = Bot(token="token")
dp = Dispatcher()


class Form(StatesGroup):
    checking_auth = State()


def keyboard_builder(button_texts):
    builder = InlineKeyboardBuilder()
    for text in button_texts:
        builder.button(text=text, callback_data=text)
    builder.adjust(1)
    return builder.as_markup()


@dp.message(CommandStart())
async def start(message: types.Message, state: FSMContext):
    auth_state = str(uuid.uuid4())
    await state.set_state(Form.checking_auth)
    await message.answer(f"Добро пожаловать в бота для автоматизации покупок!\n<a href='{AUTH_LINK + auth_state}'>"
                         f"Перейдите по ссылке для авторизации, для того чтобы продолжить работу в боте.</a>\n<b>"
                         f"После авторизации нажмите кнопку ниже.</b>", parse_mode="html",
                         reply_markup=keyboard_builder("Я авторизовался ✅"))


async def main():
    await dp.start_polling(bot, skip_updates=True)

if __name__ == '__main__':
    asyncio.run(main())