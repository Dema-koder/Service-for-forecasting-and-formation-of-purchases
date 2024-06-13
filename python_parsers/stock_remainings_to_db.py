import os
import pandas as pd
from pymongo import MongoClient
import math
import re

client = MongoClient('localhost', 27017)
db = client['stock_remainings']
collection = db['Складские остатки']

def add_into_db_21(filepath):
    str_document = filepath
    df = pd.read_excel(str_document)
    columns = ["Название", "Остаток", "подгруппа", "дата", "сч"]
    date = str_document.split('\\')[-1][22:32]
    subgroup = 0
    for index, row in df.iterrows():
        row.iloc[0], row.iloc[2], row.iloc[20] = str(row.iloc[0]), str(row.iloc[2]), str(row.iloc[20])
        if '21.' in row.iloc[0]:
            pattern = r'21.{3}'
            subgroup = re.findall(pattern, row.iloc[0])
        if row.iloc[2] != 'nan' and row.iloc[20] != 'nan':
            collection.insert_one(
                {'Название': row.iloc[2][:row.iloc[2].rfind(',')], 'Остаток': row.iloc[20], 'Подгруппа': subgroup[0], 'Дата': date, 'сч': 21})


def add_into_db_105(filepath):
    df = pd.read_excel(filepath)
    date = filepath.split('\\')[-1][22:32]
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
                    {'Название': data['МОЛ'][:data['МОЛ'].rfind(',')], 'Остаток': data['Количество'], 'Подгруппа': subgroup_id, 'Дата': date,
                     'сч': 105})  # Insert document into collection
            else:
                is_new_subgroup = False
                seen_one = False


def add_into_db_101(filepath):
    str_document = filepath
    df = pd.read_excel(str_document)
    columns = ["Название", "Остаток", "подгруппа", "дата", "сч"]
    date = str_document.split('\\')[-1][22:32]
    subgroup = 0
    for index, row in df.iterrows():
        row.iloc[0], row.iloc[2], row.iloc[20] = str(row.iloc[0]), str(row.iloc[2]), str(row.iloc[20])
        if '101.' in row.iloc[0]:
            pattern = r'101.{3}'
            subgroup = re.findall(pattern, row.iloc[0])
        if row.iloc[2] != 'nan' and row.iloc[20] != 'nan':
            collection.insert_one(
                {'Название': row.iloc[2][:row.iloc[2].rfind(',')], 'Остаток': row.iloc[20], 'Подгруппа': subgroup[0], 'Дата': date, 'сч': 101})


def migrate_to_db(directory):
    # Iterate over files in the directory
    for filename in os.listdir(directory):
        # Check file name types and perform actions accordingly
        if filename.endswith("(сч. 21).xlsx"):
            add_into_db_21(os.path.join(directory, filename))
        elif filename.endswith("(сч. 105).xlsx"):
            add_into_db_105(os.path.join(directory, filename))
        elif filename.endswith("(сч. 101).xlsx"):
            add_into_db_101(os.path.join(directory, filename))


if __name__ == '__main__':
    directory = "../dataset/Складские остатки"
    migrate_to_db(directory)