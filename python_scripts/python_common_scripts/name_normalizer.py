import re
from pymongo import MongoClient

# Подключение к MongoDB
client = MongoClient('localhost', 27017)
db = client['stock_remainings']
collection = db['Нормализированные имена']


# Функция для вставки данных, если записи еще не существует
def insert_data_if_not_exists(name, normalized_name):
    existing_document = collection.find_one({'name': name})
    if not existing_document:
        data = {
            'name': name,
            'normalized_name': normalized_name,
        }
        collection.insert_one(data)


def normalize_name(name):
    # Приводим к нижнему регистру и удаляем пробелы
    normalized_name = re.sub(r'\s+', '', name).strip().lower()
    insert_data_if_not_exists(name, normalized_name)
    return normalized_name
