from datetime import datetime
import matplotlib.pyplot as plt
import pandas as pd
import os
from pymongo import MongoClient

client = MongoClient('localhost', 27017)
db = client['stock_remainings']
collection = db['Складские остатки']

# Query MongoDB collection
cursor = collection.find({})  # Empty query to retrieve all documents

# Convert cursor to list of dictionaries
data = list(cursor)

# Create DataFrame from list of dictionaries
df = pd.DataFrame(data)

df['Остаток'] = df['Остаток'].astype(float)
df_recipe = df.groupby(['сч', 'Дата', 'Подгруппа'])['Остаток'].sum().reset_index()
for index, row in df_recipe.iterrows():
    df_recipe.loc[index, 'Дата'] = datetime.strptime(row['Дата'], "%d.%m.%Y")

df_recipe = df_recipe.sort_values(by=['сч', 'Дата']).reset_index(drop=True)

df_recipe['Дата'] = df_recipe['Дата'].astype(str)
df_recipe['Дата'] = df_recipe['Дата'].apply(lambda x: x[0:10])

fig, axs = plt.subplots(len(df_recipe['сч'].unique()), 1, figsize=(8, 6 * len(df_recipe['сч'].unique())))

for i, group in enumerate(df_recipe['сч'].unique()):
    df_recipe_group = df_recipe[df_recipe['сч'] == group]
    bottom = None
    for subgroup in df_recipe_group['Подгруппа'].unique():
        axs[i].bar(df_recipe_group.loc[df_recipe_group['Подгруппа'] == subgroup, 'Дата'],
                   df_recipe_group.loc[df_recipe_group['Подгруппа'] == subgroup, 'Остаток'], width=0.4, label=subgroup,
                   bottom=bottom)
        if bottom is None:
            bottom = df_recipe_group.loc[df_recipe_group['Подгруппа'] == subgroup, 'Остаток']
        else:
            bottom += df_recipe_group.loc[df_recipe_group['Подгруппа'] == subgroup, 'Остаток']

    axs[i].set_xlabel("Дата")
    axs[i].set_ylabel("Остаток")
    axs[i].legend()
    axs[i].set_title(f"Остатки подгрупп для сч. {group}")

plt.tight_layout()
# Указание пути к папке, в которой вы хотите сохранить график
save_path = 'saved_statistics/'
if not os.path.exists(save_path):
    os.makedirs(save_path)

# Сохранение графика в файле изображения в указанной папке
plt.savefig(save_path + 'detailed_statistics.png')

plt.show()
