import pandas as pd
from pymongo import MongoClient
from openpyxl import Workbook
from confluent_kafka import Consumer, KafkaError
import math
import re
import json
from io import BytesIO
from io import StringIO

client = MongoClient('localhost', 27017)
db = client['stock_remainings']
collection = db['Складские остатки']


def add_into_db_21(df, filename):
    date = filename.split('\\')[-1][22:32]
    subgroup = 0
    for index, row in df.iterrows():
        row.iloc[0], row.iloc[2], row.iloc[20] = str(row.iloc[0]), str(row.iloc[2]), str(row.iloc[20])
        if '21.' in row.iloc[0]:
            pattern = r'21.{3}'
            subgroup = re.findall(pattern, row.iloc[0])
        if row.iloc[2] != 'nan' and row.iloc[20] != 'nan':
            collection.insert_one(
                {'Название': row.iloc[2], 'Остаток': row.iloc[20], 'Подгруппа': subgroup[0], 'Дата': date, 'сч': 21})


def add_into_db_105(df, filename):
    date = filename.split('\\')[-1][22:32]
    # Convert DataFrame rows to dictionaries and insert into MongoDB

    is_new_subgroup = False
    seen_one = False
    subgroup_id = None
    for _, row in df.iterrows():
        try:
            subgroup = float(row['МОЛ'])
            if not math.isnan(subgroup) and not is_new_subgroup:
                is_new_subgroup = True
                subgroup_id = subgroup
            else:
                if not math.isnan(subgroup) and subgroup == 1.0:
                    seen_one = True
                elif is_new_subgroup and seen_one and not math.isnan(subgroup) and subgroup != 1.0:
                    subgroup_id = subgroup
                else:
                    is_new_subgroup = False
                    seen_one = False
        except ValueError:
            if is_new_subgroup and seen_one:
                data = row.to_dict()  # Convert row to dictionary
                collection.insert_one(
                    {'Название': data['МОЛ'], 'Остаток': data['Количество'], 'Подгруппа': subgroup_id, 'Дата': date,
                     'сч': 105})  # Insert document into collection
            else:
                is_new_subgroup = False
                seen_one = False


def add_into_db_101(df, filename):
    date = filename.split('\\')[-1][22:32]
    subgroup = 0
    for index, row in df.iterrows():
        row.iloc[0], row.iloc[2], row.iloc[20] = str(row.iloc[0]), str(row.iloc[2]), str(row.iloc[20])
        if '101.' in row.iloc[0]:
            pattern = r'101.{3}'
            subgroup = re.findall(pattern, row.iloc[0])
        if row.iloc[2] != 'nan' and row.iloc[20] != 'nan':
            collection.insert_one(
                {'Название': row.iloc[2], 'Остаток': row.iloc[20], 'Подгруппа': subgroup[0], 'Дата': date, 'сч': 101})


def process_file(file_content, filename):
    data = BytesIO(file_content)
    df = pd.read_excel(data)
    if filename.endswith("(сч. 21).xlsx"):
        add_into_db_21(df, filename)
    elif filename.endswith("(сч. 105).xlsx"):
        add_into_db_105(df, filename)
    elif filename.endswith("(сч. 101).xlsx"):
        add_into_db_101(df, filename)


def kafka_consumer():
    consumer = Consumer({
        'bootstrap.servers': 'localhost:9092',
        'group.id': 'my_group',
        'auto.offset.reset': 'earliest'
    })

    consumer.subscribe(['file_upload_topic'])

    while True:
        msg = consumer.poll(1.0)

        if msg is None:
            continue

        if msg.error():
            if msg.error().code() == KafkaError._PARTITION_EOF:
                continue
            else:
                print(msg.error())
                break

        filename = msg.key().decode('utf-8')
        file_content = msg.value()

        if file_content is None:
            print("Message value is None")
            continue

        print(filename)

        process_file(file_content, filename)

    consumer.close()


if __name__ == "__main__":
    kafka_consumer()
