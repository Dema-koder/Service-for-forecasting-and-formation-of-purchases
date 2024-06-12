import math
import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

# Load the dataset
df = pd.read_excel('Выгрузка контрактов по Заказчику.xlsx')

df.dropna(subset=['Наименование СПГЗ'], inplace=True)

# Group by ID СПГЗ and Конечный код КПГЗ, and count the number of rows
df_for_mark = df.groupby(['ID СПГЗ', 'Конечный код КПГЗ']).size().reset_index(name='count')

# Calculate the breakpoint (25th percentile) and separate temporary and permanent purchases
breakpoint = np.percentile(df_for_mark['count'].unique(), 25)
temporary_purchases = df_for_mark['count'].unique()[df_for_mark['count'].unique() < breakpoint]
permanent_purchases = df_for_mark['count'].unique()[df_for_mark['count'].unique() >= breakpoint]


print(breakpoint)

breakpoint = 3
# Create a new column 'marker' in df_for_mark
df_for_mark['marker'] = np.where(df_for_mark['count'] > breakpoint, 'регулярная', 'нерегулярная')

# Merge df_with_marker with df using ID СПГЗ and Конечный код КПГЗ
df = pd.merge(df, df_for_mark[['ID СПГЗ', 'Конечный код КПГЗ', 'marker']], on=['ID СПГЗ', 'Конечный код КПГЗ'], how='left')

# Select the desired columns
df_marked = df[['ID СПГЗ', 'Наименование СПГЗ', 'Конечный код КПГЗ', 'marker']]

print(df_marked[['Наименование СПГЗ','marker']].head(50))
