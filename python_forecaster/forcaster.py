import pandas as pd
from pymongo import MongoClient
import math
from datetime import datetime
from matplotlib import pyplot as plt
from pmdarima import auto_arima


def get_mongo_collection():
    client = MongoClient('localhost', 27017)
    db = client['stock_remainings']
    return db['Оборотная ведомость']


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


def purchase(months, forecast):
    sum_of_purchase = 0
    for quarter in range(months // 3):
        sum_of_purchase += forecast.iloc[quarter]
    if months > (months // 3) * 3:
        sum_of_purchase += forecast.iloc[months // 3] * (months - (months // 3) * 3) / 3
    return sum_of_purchase


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
    data = collection.find({"name": name})
    return data, dates


def aggregate_data(data):
    aggregated_data = {}
    for document in data:
        key = (document["квартал"], document["год"])
        if key not in aggregated_data:
            aggregated_data[key] = 0
        aggregated_data[key] += 0 if pd.isna(document["единицы во кред"]) else document["единицы во кред"]
    return aggregated_data


def create_dataframe(aggregated_data):
    df = pd.DataFrame(
        [(value, make_datetime(key[0], key[1])) for key, value in aggregated_data.items()],
        columns=['Kredit', 'дата']
    )
    return df


def add_missing_dates(df, dates):
    new_rows = []
    for date in dates:
        if date not in df['дата'].apply(lambda x: x.strftime('%Y-%m-%d')).values:
            new_rows.append({'дата': datetime.strptime(date, '%Y-%m-%d'), 'Kredit': 0})
    if new_rows:
        new_rows_df = pd.DataFrame(new_rows)
        df = pd.concat([df, new_rows_df], ignore_index=True)
    return df


def plot_forecast(df_forecast):
    plt.figure(figsize=(10, 6))
    plt.bar(df_forecast['дата'].dt.strftime('%Y-%m'), df_forecast['прогноз'], color='blue')
    plt.xlabel('Дата')
    plt.ylabel('Прогноз потребления')
    plt.title('Прогноз потребления на конец каждого квартала')
    plt.show()


def make_forecast(name, months):
    collection = get_mongo_collection()
    data, dates = fetch_data(collection, name)
    aggregated_data = aggregate_data(data)
    df = create_dataframe(aggregated_data)

    if df.empty:
        return f"Извините, нет данных оборотной ведомости для {name}"

    df = add_missing_dates(df, dates)
    df.sort_values(by='дата', inplace=True)
    plt.figure(figsize=(10, 6))
    plt.bar(df['дата'].dt.strftime('%Y-%m'), df['Kredit'], color='blue')
    plt.xlabel('Дата')
    plt.ylabel('Потребление')
    plt.title('Известное потребление за период в конце квартала')
    plt.show()
    if pd.notna(df['Kredit']).sum() == 0 or df['Kredit'].max() == 0:
        return f"Извините, кажется, {name} не тратился в течение всего времени"
    df.set_index('дата', inplace=True)
    model = auto_arima(df, seasonal=True, stepwise=True, trace=False, m=4, D=0)
    forecast = model.predict(n_periods=math.ceil(months / 3))

    if forecast.max() == 0:
        return f"Кажется {name} редко используется, невозможно предсказать потребление"

    df_forecast = pd.DataFrame(
        {'дата': pd.date_range(start=df.index[-1], periods=len(forecast), freq='QE'), 'прогноз': forecast})

    if months % 3 == 0:
        plot_forecast(df_forecast)
    else:
        monthly_consuming = pd.DataFrame(columns=['дата', 'прогноз'])
        for index, row in df_forecast.iterrows():
            avg_consuming = row['прогноз'] / 3
            until_month = 0
            if index == len(df_forecast) - 1:
                until_month = 3 - months % 3
            for month in range(2, until_month - 1, -1):
                monthly_consuming.loc[len(monthly_consuming)] = [row['дата'] - pd.offsets.MonthEnd(month),
                                                                 avg_consuming]

        plt.figure(figsize=(10, 6))
        plt.bar(monthly_consuming['дата'].dt.strftime('%Y-%m'), monthly_consuming['прогноз'], color='blue')
        plt.xlabel('Дата')
        plt.ylabel('Прогноз потребления')
        plt.title('Прогноз потребления по месяцам')
        plt.show()

    client = MongoClient('localhost', 27017)
    db = client['stock_remainings']
    collection_ost = db['Складские остатки']
    data = collection_ost.find({"Название": name})
    df = pd.DataFrame(columns=['дата', 'остатки'])
    for document in data:
        df.loc[len(df)] = [document['Дата'], document['Остаток']]

    df['дата'] = pd.to_datetime(df['дата'], dayfirst=True)
    df.sort_values(by='дата', inplace=True)
    df.reset_index(drop=True, inplace=True)

    if len(df) == 0 or df.iloc[-1]['дата'] < datetime(2022, 12, 31):
        rem = 0
    else:
        rem = df.iloc[-1]['остатки']

    sum_of_purchase = purchase(months, forecast)

    if sum_of_purchase < rem:
        return "Остатков хватит на этот срок"

    return sum_of_purchase


if __name__ == '__main__':
    print(make_forecast("Бумага для офисной техники IQ Allround А3, класса В+, 80 г/м2, 500л.", 12))
