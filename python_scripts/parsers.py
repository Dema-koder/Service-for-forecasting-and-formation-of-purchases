import math
import os
import sys
from abc import ABC, abstractmethod

import joblib
import numpy as np
from pymongo import MongoClient
import re
import pandas as pd
from pathlib import Path
from python_common_scripts.name_normalizer import normalize_name
from python_items_classifier.predictor import predict


class Parser(ABC):
    def __init__(self):
        # Initialize MongoDB client and collection
        self.client = MongoClient('localhost', 27017)
        self.db = self.client['stock_remainings']

    @abstractmethod
    def parse(self, data):
        # Abstract method that must be implemented by subclasses
        pass


class OborotyParser(Parser):
    def __init__(self):
        super().__init__()
        self.collection = self.db['Оборотная ведомость']

    def parse(self, directory_path):
        """Parse all files in the directory and process each file."""
        for file_path in Path(directory_path).iterdir():
            if file_path.is_file() and file_path.suffix == '.xlsx':
                self.process_file(str(file_path))

    def extract_quarter_year(self, filename):
        """Extracts the quarter and year from the filename."""
        quarter = re.search(r'(\d+) кв\.', filename.split('\\')[-1]).group(1)
        year = re.search(r'кв\. (\d+)', filename.split('\\')[-1]).group(1)
        return quarter, year

    def insert_data_to_db(self, data):
        """Inserts data into the MongoDB collection."""
        self.collection.insert_one(data)

    def process_common_logic(self, df, quarter, year, group, row_index, name_index):
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
                    'name': normalize_name(name),
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
                self.insert_data_to_db(data)
                index += 3
            index += 1

    def process_21(self, filename):
        df = pd.read_excel(filename)
        quarter, year = self.extract_quarter_year(filename)
        self.process_common_logic(df, quarter, year, 21, 9, 0)

    def process_105(self, filename):
        df = pd.read_excel(filename)
        quarter, year = self.extract_quarter_year(filename)
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
                    'name': normalize_name(name),
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
                self.insert_data_to_db(data)

    def process_101(self, filename):
        df = pd.read_excel(filename)
        quarter, year = self.extract_quarter_year(filename)
        self.process_common_logic(df, quarter, year, 101, 9, 0)

    def process_file(self, filepath):
        """Processes a file based on its type."""
        if "сч. 21" in filepath:
            self.process_21(filepath)
        elif "сч. 105" in filepath:
            self.process_105(filepath)
        elif "сч. 101" in filepath:
            self.process_101(filepath)
        else:
            print(f"Skipping {filepath}, no matching function found")


class ReferenceBookParser(Parser):
    def __init__(self):
        super().__init__()
        self.collection = self.db['Справочники']

    @staticmethod
    def is_numeric_code(code):
        return str(code).isnumeric()

    @staticmethod
    def split_and_aggregate(name_characteristics):
        name = name_characteristics.iloc[0]
        position = name.find(';')
        if position == -1:
            return name, ""
        else:
            simple_name = name[:name.find(';')]
            characteristics = name.split(';')[1:]
            return simple_name, ';'.join(characteristics)

    def parse(self, filepath):
        df = pd.read_excel(filepath)
        df.dropna(axis=0, subset=['Название СТЕ'], inplace=True)
        # Apply the function to filter out non-numeric rows
        df_cleaned = df[df['Реестровый номер в РК'].apply(self.is_numeric_code)].copy()
        lst = list(df_cleaned.columns)
        lst[4] = 'Конечный код КПГЗ'
        df_cleaned.columns = lst
        df_cleaned['характеристики'] = np.full(df_cleaned['наименование характеристик'].shape[0], None)
        df_cleaned[["Название СТЕ", "характеристики"]] = df_cleaned[["Название СТЕ", "наименование характеристик"]].apply(self.split_and_aggregate, axis=1, result_type='broadcast')

        df_cleaned['ресурсный'] = predict(df_cleaned[['Конечный код КПГЗ', 'Реестровый номер в РК']])
        data_dict = df_cleaned.to_dict('records')
        self.collection.insert_many(data_dict)


class StockBalanceParser(Parser):
    def __init__(self):
        super().__init__()
        self.collection = self.db['Складские остатки']

    def add_into_db_21(self, filepath):
        df = pd.read_excel(filepath)
        date = filepath.split('\\')[-1][22:32]
        subgroup = 0
        for index, row in df.iterrows():
            row.iloc[0], row.iloc[2], row.iloc[20] = str(row.iloc[0]), str(row.iloc[2]), str(row.iloc[20])
            if '21.' in row.iloc[0]:
                pattern = r'21.{3}'
                subgroup = re.findall(pattern, row.iloc[0])
            if row.iloc[2] != 'nan' and row.iloc[20] != 'nan':
                self.collection.insert_one({
                    'Название': normalize_name(row.iloc[2][:row.iloc[2].rfind(',')]),
                    'Остаток': row.iloc[20],
                    'Подгруппа': subgroup[0],
                    'Дата': date,
                    'сч': 21
                })

    def add_into_db_105(self, filepath):
        df = pd.read_excel(filepath)
        date = filepath.split('\\')[-1][22:32]
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
                    data = row.to_dict()
                    self.collection.insert_one({
                        'Название': normalize_name(data['МОЛ'][:data['МОЛ'].rfind(',')]),
                        'Остаток': data['Количество'],
                        'Подгруппа': subgroup_id,
                        'Дата': date,
                        'сч': 105
                    })
                else:
                    is_new_subgroup = False
                    seen_one = False

    def add_into_db_101(self, filepath):
        df = pd.read_excel(filepath)
        date = filepath.split('\\')[-1][22:32]
        subgroup = 0
        for index, row in df.iterrows():
            row.iloc[0], row.iloc[2], row.iloc[20] = str(row.iloc[0]), str(row.iloc[2]), str(row.iloc[20])
            if '101.' in row.iloc[0]:
                pattern = r'101.{3}'
                subgroup = re.findall(pattern, row.iloc[0])
            if row.iloc[2] != 'nan' and row.iloc[20] != 'nan':
                self.collection.insert_one({
                    'Название': normalize_name(row.iloc[2][:row.iloc[2].rfind(',')]),
                    'Остаток': row.iloc[20],
                    'Подгруппа': subgroup[0],
                    'Дата': date,
                    'сч': 101
                })

    def parse(self, directory):
        # Iterate over files in the directory and process each file
        for filename in os.listdir(directory):
            if filename.endswith("(сч. 21).xlsx"):
                self.add_into_db_21(os.path.join(directory, filename))
            elif filename.endswith("(сч. 105).xlsx"):
                self.add_into_db_105(os.path.join(directory, filename))
            elif filename.endswith("(сч. 101).xlsx"):
                self.add_into_db_101(os.path.join(directory, filename))


if __name__ == "__main__":
    stock_parser = StockBalanceParser()
    oboroty_parser = OborotyParser()
    reference_book_parser = ReferenceBookParser()
    stock_parser.parse('C:\\Users\danil\Desktop\hackaton\Service-for-forecasting-and-formation-of-purchases\dataset\Складские остатки')
    oboroty_parser.parse('C:\\Users\danil\Desktop\hackaton\Service-for-forecasting-and-formation-of-purchases\dataset\Обороты по счету')
    reference_book_parser.parse('C:\\Users\danil\Desktop\hackaton\Service-for-forecasting-and-formation-of-purchases\dataset\КПГЗ ,СПГЗ, СТЕ.xlsx')


