import os

import joblib
import pandas as pd
from pymongo import MongoClient
import python_modules.model_parameters as model_baseline


client = MongoClient('localhost', 27017)
db = client['stock_remainings']
collection = db['Справочники']


def is_numeric_code(code):
    return str(code).isnumeric()


def to_db(filepath):
    df = pd.read_excel(filepath)
    df.dropna(axis=0, subset=['Название СТЕ'], inplace=True)
    # Apply the function to filter out non-numeric rows
    df_cleaned = df[df['Реестровый номер в РК'].apply(is_numeric_code)].copy()
    loaded_classifier = joblib.load("pipeline.joblib")
    lst = list(df_cleaned.columns)
    lst[4] = 'Конечный код КПГЗ'
    df_cleaned.columns = lst
    df_cleaned['ресурсный'] = loaded_classifier.predict(df_cleaned[['Конечный код КПГЗ', 'Реестровый номер в РК']])
    data_dict = df_cleaned.to_dict('records')
    collection.insert_many(data_dict)


if __name__ == '__main__':
    filepath = "dataset/КПГЗ ,СПГЗ, СТЕ.xlsx"
    to_db(filepath)

