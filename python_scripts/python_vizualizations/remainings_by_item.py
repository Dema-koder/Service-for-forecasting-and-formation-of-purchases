import os
import sys
from datetime import datetime
import pandas as pd
from matplotlib import pyplot as plt
from pymongo import MongoClient
from dotenv import load_dotenv

# Получаем текущий каталог и корень проекта для установки правильного пути системы.
current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.abspath(os.path.join(current_dir, '..'))
dotenv_path = os.path.join(project_root, 'variables.env')
sys.path.append(project_root)

load_dotenv(dotenv_path)
# Используем абсолютные импорты
from python_common_scripts.name_normalizer import normalize_name


def get_mongo_collection():
    """
    Устанавливает соединение с экземпляром MongoDB и получает коллекцию
    'Оборотная ведомость' из базы данных 'stock_remainings'.

    Возвращает:
        pymongo.collection.Collection: Экземпляр коллекции MongoDB.
    """
    mongo_uri = os.getenv('MONGO_URI')
    db_name = os.getenv('DB_NAME')
    client = MongoClient(mongo_uri)
    db = client[db_name]
    return db['Оборотная ведомость']


def fetch_data(collection, name):
    """
    Извлекает данные и соответствующие даты из коллекции MongoDB для заданного названия продукта.

    Аргументы:
        collection (pymongo.collection.Collection): Экземпляр коллекции MongoDB.
        name (str): Название продукта, для которого необходимо извлечь данные.

    Возвращает:
        tuple: Содержит курсор данных и список соответствующих дат.
    """
    pipeline = [
        {
            "$project": {
                "год": {"$toString": "$год"},
                "квартал": "$квартал",
                "start_date": {
                    "$switch": {
                        "branches": [
                            {"case": {"$eq": ["$квартал", "1"]}, "then": {"$concat": ["$год", "-03-31"]}},
                            {"case": {"$eq": ["$квартал", "2"]}, "then": {"$concat": ["$год", "-06-30"]}},
                            {"case": {"$eq": ["$квартал", "3"]}, "then": {"$concat": ["$год", "-09-30"]}},
                            {"case": {"$eq": ["$квартал", "4"]}, "then": {"$concat": ["$год", "-12-31"]}}
                        ],
                        "default": "unknown"
                    }
                }
            }
        },
        {
            "$group": {
                "_id": "$start_date"
            }
        },
        {
            "$project": {
                "date": "$_id"
            }
        }
    ]
    result = list(collection.aggregate(pipeline))
    dates = [doc['date'] for doc in result]
    data = collection.find({"name": normalize_name(name)})
    return data, dates


def aggregate_data(data):
    """
    Аггрегирует данные, извлеченные из коллекции MongoDB, суммируя 'единицы после'
    для каждого квартала и года.

    Аргументы:
        data (pymongo.cursor.Cursor): Курсор, содержащий извлеченные данные.

    Возвращает:
        dict: Аггрегированные данные с ключами в виде (квартал, год) и значениями, представляющими сумму 'единицы после'.
    """
    aggregated_data = {}
    for document in data:
        key = (document["квартал"], document["год"])
        if key not in aggregated_data:
            aggregated_data[key] = 0
        aggregated_data[key] += 0 if pd.isna(document["единицы после"]) else document["единицы после"]
    return aggregated_data


def create_dataframe(aggregated_data):
    """
    Создает DataFrame из аггрегированных данных.

    Аргументы:
        aggregated_data (dict): Аггрегированные данные с ключами в виде (квартал, год) и значениями, представляющими сумму 'единицы после'.

    Возвращает:
        pd.DataFrame: DataFrame, содержащий аггрегированные данные с колонками 'остаток' и 'дата'.
    """
    df = pd.DataFrame(
        [(value, make_datetime(key[0], key[1])) for key, value in aggregated_data.items()],
        columns=['остаток', 'дата']
    )
    return df


def add_missing_dates(df, dates):
    """
    Добавляет строки с нулевым 'остаток' для дат, которые присутствуют в списке дат, но отсутствуют в DataFrame.

    Аргументы:
        df (pd.DataFrame): DataFrame, содержащий аггрегированные данные.
        dates (list): Список дат, которые должны быть включены в DataFrame.

    Возвращает:
        pd.DataFrame: DataFrame с добавленными отсутствующими датами.
    """
    new_rows = []
    for date in dates:
        if date not in df['дата'].apply(lambda x: x.strftime('%Y-%m-%d')).values:
            new_rows.append({'дата': datetime.strptime(date, '%Y-%m-%d'), 'остаток': 0})
    if new_rows:
        new_rows_df = pd.DataFrame(new_rows)
        df = pd.concat([df, new_rows_df], ignore_index=True)
    return df


def make_datetime(quarter, year):
    """
    Создает объект datetime, соответствующий дате окончания заданного квартала в заданном году.

    Аргументы:
        quarter (int): Номер квартала (1, 2, 3 или 4).
        year (int): Номер года.

    Возвращает:
        datetime.datetime: Объект datetime, представляющий дату окончания указанного квартала.
    """
    quarter = int(quarter)
    year = int(year)
    date_list = [
        datetime(year, 3, 31),
        datetime(year, 6, 30),
        datetime(year, 9, 30),
        datetime(year, 12, 31)
    ]
    return date_list[quarter - 1]


def make_plot_of_remainings(name):
    """
    Генерирует график остатков товара по времени для заданного названия продукта и отображает его.

    Аргументы:
        name (str): Название продукта, для которого необходимо сгенерировать график.

    Возвращает:
        dict: Содержит последнюю дату и соответствующий 'остаток'.
    """
    collection = get_mongo_collection()
    data, dates = fetch_data(collection, name)
    aggregated_data = aggregate_data(data)
    df = create_dataframe(aggregated_data)

    if df.empty:
        return f"Извините, нет данных об остатках для {name}"

    plt.rcParams.update({
        'font.size': 14,
        'font.family': 'serif',
        'axes.titlesize': 20,
        'axes.labelsize': 16,
        'xtick.labelsize': 12,
        'ytick.labelsize': 12,
        'figure.titlesize': 22
    })

    df = add_missing_dates(df, dates)
    df.sort_values(by='дата', inplace=True)

    plt.figure(figsize=(8, 7))
    plt.bar(df['дата'].dt.strftime('%Y-%m'), df['остаток'], color='blue')
    plt.xlabel('Дата')
    plt.ylabel('Остатки')
    plt.title('Остатки за период в конце квартала')
    plt.xticks(rotation=45, ha='right')
    plt.show()

    return df.iloc[-1].to_dict()


if __name__ == '__main__':
    print(make_plot_of_remainings('Клей монтажный «Tytanprofessional» Classic Fix каучуковый,прозрачный 310 мл'))
