import os
import pandas as pd
from pymongo import MongoClient
import math
import re
from datetime import datetime
import matplotlib.ticker as ticker
import matplotlib.pyplot as plt

client = MongoClient('localhost', 27017)
db = client['stock_remainings']
collection = db['Складские остатки']
cursor = collection.find({})
data = list(cursor)
df = pd.DataFrame(data)


list_of_recipes = df['сч'].unique()




df['Остаток'] = df['Остаток'].astype(float)
df_recipe = df.groupby(['сч', 'Дата'])['Остаток'].sum().reset_index()


df_recipe['Дата'] = df_recipe.apply(lambda row: datetime.strptime(row['Дата'], "%d.%m.%Y"), axis=1)


df_recipe = df_recipe.sort_values(by=['сч','Дата']).reset_index(drop= True)

df_recipe['Дата'] = df_recipe['Дата'].astype(str)
df_recipe['Дата'] = df_recipe['Дата'].apply(lambda x: x[0:10])



fig, axs = plt.subplots(len(df_recipe['сч'].unique()), 1, figsize=(8, 6*len(df_recipe['сч'].unique())))

for i, rec in enumerate(df_recipe['сч'].unique()):
    df_recipe_group = df_recipe[df_recipe['сч'] == rec]
    axs[i].bar(df_recipe_group['Дата'], df_recipe_group['Остаток'], color='maroon', width=0.4)
    axs[i].set_xlabel("Дата")
    axs[i].set_ylabel("Остаток")
    axs[i].set_title(f"Визуализация для сч {rec}")

plt.tight_layout()


# Указание пути к папке, в которой вы хотите сохранить график
save_path = 'saved_statistics/'
if not os.path.exists(save_path):
    os.makedirs(save_path)

# Сохранение графика в файле изображения в указанной папке
plt.savefig(save_path + 'statistics.png')

plt.show()
