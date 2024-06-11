from pymongo import MongoClient
import re
import os
import pandas as pd
from pathlib import Path

client = MongoClient('localhost', 27017)
db = client['stock_remainings']
collection = db['Оборотная ведомость']

# Define the path to the directory containing the files
directory_path = Path(
    r'C:\Users\danil\Desktop\hackaton\Service-for-forecasting-and-formation-of-purchases\dataset\Обороты по счету')


# Define the functions to process the files
def process_21(filename):
    df = pd.read_excel(filename)
    kv = re.search(r'(\d+) кв\.', filename.split('\\')[-1]).group(1)
    subgroup = None
    index = 9
    new_df = pd.DataFrame(
        columns=['name', 'цена до', 'единицы до', 'цена во деб', 'единицы во деб', 'цена во кред', 'единицы во кред',
                 'цена после', 'единицы после', 'группа', 'подгруппа', 'квартал'])
    while index < len(df):
        if index + 1 >= len(df):
            break
        row = df.iloc[index]
        pattern = re.compile(r'21\.\d+')
        if bool(pattern.match(str(row.iloc[0]))):
            subgroup = row.iloc[0]
        elif str(row.iloc[0]) == 'Итого':
            break
        elif subgroup is not None and pd.notna(row.iloc[0]):
            name = df.iloc[index, 0]
            count_before_debet = df.iloc[index + 1, 10]
            price_before_debet = df.iloc[index, 10]
            if not pd.isna(count_before_debet):
                price_before_debet = df.iloc[index, 10] / count_before_debet
            price_in_debet = df.iloc[index, 12]
            count_in_debet = df.iloc[index + 1, 12]
            if not pd.isna(count_in_debet):
                price_in_debet = df.iloc[index, 12] / count_in_debet
            price_in_kredit = df.iloc[index, 13]
            count_in_kredit = df.iloc[index + 1, 13]
            if not pd.isna(count_in_kredit):
                price_in_kredit = df.iloc[index, 13] / count_in_kredit
            price_after_debet = df.iloc[index, 14]
            count_after_debet = df.iloc[index + 1, 14]
            if not pd.isna(count_after_debet):
                price_after_debet = df.iloc[index, 14] / count_after_debet
            data = {
                'name': name,
                'цена до': price_before_debet,
                'единицы до': count_before_debet,
                'цена во деб': price_in_debet,
                'единицы во деб': count_in_debet,
                'цена во кред': price_in_kredit,
                'единицы во кред': count_in_kredit,
                'цена после': price_after_debet,
                'единицы после': count_after_debet,
                'группа': 21,
                'подгруппа': subgroup,
                'квартал': kv
            }
            collection.insert_one(data)
            index += 3
        index += 1


def process_105(filename):
    df = pd.read_excel(filename)
    kv = re.search(r'(\d+) кв\.', filename.split('\\')[-1]).group(1)

    subgroup = None
    for index, row in df.iterrows():
        if pd.isna(row.iloc[0]):
            subgroup = str(row.iloc[1]).split(' ')[0]
        elif row.iloc[3] == 'Итого':
            break
        else:
            name = row.iloc[3]
            count_before_debet = float(row.iloc[5])
            price_before_debet = float(row.iloc[6])
            if not pd.isna(count_before_debet):
                price_before_debet = price_before_debet / count_before_debet
            price_in_debet = float(row.iloc[8])
            count_in_debet = float(row.iloc[7])
            if not pd.isna(count_in_debet):
                price_in_debet = price_in_debet / count_in_debet
            price_in_kredit = float(row.iloc[10])
            count_in_kredit = float(row.iloc[9])
            if not pd.isna(count_in_kredit):
                price_in_kredit = price_in_kredit / count_in_kredit
            price_after_debet = float(row.iloc[12])
            count_after_debet = float(row.iloc[11])
            if not pd.isna(count_after_debet):
                price_after_debet = price_after_debet / count_after_debet
            data = {
                'name': name,
                'цена до': price_before_debet,
                'единицы до': count_before_debet,
                'цена во деб': price_in_debet,
                'единицы во деб': count_in_debet,
                'цена во кред': price_in_kredit,
                'единицы во кред': count_in_kredit,
                'цена после': price_after_debet,
                'единицы после': count_after_debet,
                'группа': 105,
                'подгруппа': subgroup,
                'квартал': kv
            }
            collection.insert_one(data)


def process_101(filename):
    df = pd.read_excel(filename)
    kv = re.search(r'(\d+) кв\.', filename.split('\\')[-1]).group(1)
    subgroup = None
    index = 9
    new_df = pd.DataFrame(
        columns=['name', 'цена до', 'единицы до', 'цена во деб', 'единицы во деб', 'цена во кред', 'единицы во кред',
                 'цена после', 'единицы после', 'группа', 'подгруппа', 'квартал'])
    while index < len(df):
        if index + 1 >= len(df):
            break
        row = df.iloc[index]
        pattern = re.compile(r'101\.\d+')
        if bool(pattern.match(str(row.iloc[0]))):
            subgroup = row.iloc[0]
        elif str(row.iloc[0]) == 'Итого':
            break
        elif subgroup is not None and pd.notna(row.iloc[0]):
            name = df.iloc[index, 0]
            count_before_debet = df.iloc[index + 1, 10]
            price_before_debet = df.iloc[index, 10]
            if not pd.isna(count_before_debet):
                price_before_debet = df.iloc[index, 10] / count_before_debet
            price_in_debet = df.iloc[index, 12]
            count_in_debet = df.iloc[index + 1, 12]
            if not pd.isna(count_in_debet):
                price_in_debet = df.iloc[index, 12] / count_in_debet
            price_in_kredit = df.iloc[index, 13]
            count_in_kredit = df.iloc[index + 1, 13]
            if not pd.isna(count_in_kredit):
                price_in_kredit = df.iloc[index, 13] / count_in_kredit
            price_after_debet = df.iloc[index, 14]
            count_after_debet = df.iloc[index + 1, 14]
            if not pd.isna(count_after_debet):
                price_after_debet = df.iloc[index, 14] / count_after_debet
            data = {
                'name': name,
                'цена до': price_before_debet,
                'единицы до': count_before_debet,
                'цена во деб': price_in_debet,
                'единицы во деб': count_in_debet,
                'цена во кред': price_in_kredit,
                'единицы во кред': count_in_kredit,
                'цена после': price_after_debet,
                'единицы после': count_after_debet,
                'группа': 101,
                'подгруппа': subgroup,
                'квартал': kv
            }
            collection.insert_one(data)
            index += 3
        index += 1


# Define the function to process the file
def process_file(file_path):
    filename = file_path

    if "сч. 21" in filename:
        process_21(filename)
    elif "сч. 105" in filename:
        process_105(filename)
    elif "сч. 101" in filename:
        process_101(filename)
    else:
        print(f"Skipping {filename}, no matching function found")


if __name__ == '__main__':
    # Iterate over the files in the directory
    for file_path in directory_path.iterdir():
        if file_path.is_file() and file_path.suffix == '.xlsx':
            process_file(os.path.join(directory_path, file_path))