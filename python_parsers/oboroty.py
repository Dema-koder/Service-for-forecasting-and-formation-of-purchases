from pymongo import MongoClient
import re
import pandas as pd
from pathlib import Path

# Initialize MongoDB client and collection
client = MongoClient('localhost', 27017)
db = client['stock_remainings']
collection = db['Оборотная ведомость']

# Define the path to the directory containing the files using relative path
current_directory = Path(__file__).parent
directory_path = current_directory.parent / 'dataset' / 'Обороты по счету'


def extract_quarter_year(filename):
    """Extracts the quarter and year from the filename."""
    quarter = re.search(r'(\d+) кв\.', filename.split('\\')[-1]).group(1)
    year = re.search(r'кв\. (\d+)', filename.split('\\')[-1]).group(1)
    return quarter, year


def insert_data_to_db(data):
    """Inserts data into the MongoDB collection."""
    collection.insert_one(data)


def process_common_logic(df, quarter, year, group, row_index, name_index):
    """Processes common logic for processing rows in the dataframes."""
    subgroup = None
    index = row_index
    while index < len(df):
        if index + 1 >= len(df):
            break
        row = df.iloc[index]
        pattern = re.compile(rf'{group}\.\d+')
        if bool(pattern.match(str(row.iloc[0]))):
            subgroup = row.iloc[0]
        elif str(row.iloc[0]) == 'Итого':
            break
        elif subgroup is not None and pd.notna(row.iloc[0]):
            name = df.iloc[index, name_index]
            count_before_debet = df.iloc[index + 1, 10]
            price_before_debet = df.iloc[index, 10] if pd.isna(count_before_debet)\
                else df.iloc[index, 10] / count_before_debet
            price_in_debet = df.iloc[index, 12]
            count_in_debet = df.iloc[index + 1, 12]
            price_in_debet = price_in_debet if pd.isna(count_in_debet) else df.iloc[index, 12] / count_in_debet
            price_in_kredit = df.iloc[index, 13]
            count_in_kredit = df.iloc[index + 1, 13]
            price_in_kredit = price_in_kredit if pd.isna(count_in_kredit) else df.iloc[index, 13] / count_in_kredit
            price_after_debet = df.iloc[index, 14]
            count_after_debet = df.iloc[index + 1, 14]
            price_after_debet = price_after_debet if pd.isna(count_after_debet) \
                else df.iloc[index, 14] / count_after_debet

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
                'группа': group,
                'подгруппа': subgroup,
                'квартал': quarter,
                'год': year
            }
            insert_data_to_db(data)
            index += 3
        index += 1


def process_21(filename):
    df = pd.read_excel(filename)
    quarter, year = extract_quarter_year(filename)
    process_common_logic(df, quarter, year, 21, 9, 0)


def process_105(filename):
    df = pd.read_excel(filename)
    quarter, year = extract_quarter_year(filename)
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
            price_before_debet = price_before_debet if pd.isna(count_before_debet) \
                else price_before_debet / count_before_debet
            price_in_debet = float(row.iloc[8])
            count_in_debet = float(row.iloc[7])
            price_in_debet = price_in_debet if pd.isna(count_in_debet) else price_in_debet / count_in_debet
            price_in_kredit = float(row.iloc[10])
            count_in_kredit = float(row.iloc[9])
            price_in_kredit = price_in_kredit if pd.isna(count_in_kredit) else price_in_kredit / count_in_kredit
            price_after_debet = float(row.iloc[12])
            count_after_debet = float(row.iloc[11])
            price_after_debet = price_after_debet if pd.isna(count_after_debet) \
                else price_after_debet / count_after_debet

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
                'квартал': quarter,
                'год': year
            }
            insert_data_to_db(data)


def process_101(filename):
    df = pd.read_excel(filename)
    quarter, year = extract_quarter_year(filename)
    process_common_logic(df, quarter, year, 101, 9, 0)


def process_file(filepath):
    """Processes a file based on its type."""
    if "сч. 21" in filepath:
        process_21(filepath)
    elif "сч. 105" in filepath:
        process_105(filepath)
    elif "сч. 101" in filepath:
        process_101(filepath)
    else:
        print(f"Skipping {filepath}, no matching function found")


if __name__ == '__main__':
    # Iterate over the files in the directory and process each file
    for file_path in directory_path.iterdir():
        if file_path.is_file() and file_path.suffix == '.xlsx':
            process_file(str(file_path))
