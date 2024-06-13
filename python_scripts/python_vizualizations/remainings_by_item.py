from datetime import datetime

import pandas as pd
from matplotlib import pyplot as plt
from pymongo import MongoClient
from ..python_common_scripts.name_normalizer import normalize_name

def get_mongo_collection():
    client = MongoClient('localhost', 27017)
    db = client['stock_remainings']
    return db['Оборотная ведомость']


def fetch_data(collection, name):
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
    aggregated_data = {}
    for document in data:
        key = (document["квартал"], document["год"])
        if key not in aggregated_data:
            aggregated_data[key] = 0
        aggregated_data[key] += 0 if pd.isna(document["единицы после"]) else document["единицы после"]
    return aggregated_data


def create_dataframe(aggregated_data):
    df = pd.DataFrame(
        [(value, make_datetime(key[0], key[1])) for key, value in aggregated_data.items()],
        columns=['остаток', 'дата']
    )
    return df


def add_missing_dates(df, dates):
    new_rows = []
    for date in dates:
        if date not in df['дата'].apply(lambda x: x.strftime('%Y-%m-%d')).values:
            new_rows.append({'дата': datetime.strptime(date, '%Y-%m-%d'), 'остаток': 0})
    if new_rows:
        new_rows_df = pd.DataFrame(new_rows)
        df = pd.concat([df, new_rows_df], ignore_index=True)
    return df


def make_datetime(quarter, year):
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
    collection = get_mongo_collection()
    data, dates = fetch_data(collection, name)
    aggregated_data = aggregate_data(data)
    df = create_dataframe(aggregated_data)

    if df.empty:
        return f"Извините, нет данных об остатках для {name}"

    df = add_missing_dates(df, dates)
    df.sort_values(by='дата', inplace=True)
    plt.figure(figsize=(10, 6))
    plt.bar(df['дата'].dt.strftime('%Y-%m'), df['остаток'], color='blue')
    plt.xlabel('Дата')
    plt.ylabel('Остатки')
    plt.title('Остатки за период в конце квартала')
    plt.show()
    return df.iloc[-1].to_dict()


if __name__ == '__main__':
    print(make_plot_of_remainings('Клей монтажный «Tytanprofessional» Classic Fix каучуковый,прозрачный 310 мл'))

