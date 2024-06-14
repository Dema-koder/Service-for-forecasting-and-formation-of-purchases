import pandas as pd

# List of Excel file paths
file_paths = ['/content/138_Применение ограничений допуска отдельных видов пищевых продуктов__     происходящих из иностранн....xls',
              '/content/139_Применение ограничений допуска отдельных видов медицинских изделий_ происходящих из иностранных ....xls',
              '/content/141_Применение запрета для товаров_ входящих в Перечень сельскохозяйственной продукции_ сырья и прод....xls',
              '/content/142_Применение запрета на допуск промышленных товаров_ происходящих из иностранных государств_ за ис....xls',
              '/content/143_Товары_ происходящие из иностранного государства или группы иностранных государств_ допускаемые ....xls',
              '/content/144_Применение ограничений допуска отдельных видов промышленных товаров_ происходящих из иностранных....xls',
              '/content/1_Преимущества организациям инвалидов (ст. 29 44-ФЗ_ РП РФ от 08.12.2021 N 3500-р).xls',
              '/content/2_Перечень аукционной продукции (Распоряжение Правительства РФ от 21.03.2016 г. N 471-р).xls',
              '/content/3_Преимущества учреждениям и предприятиям УИС (ст. 28 44-ФЗ_ РП РФ от 08.12.2021 № 3500-р).xls']
dfs = []
# Initialize an empty list to store the dataframes
for file in file_paths:
    df = pd.read_excel(file, sheet_name=None)
    dfs.extend([value for key, value in df.items() if value.shape[0] > 0])

full_df = pd.concat(dfs, ignore_index=True)
full_df.to_excel('Настроечные таблицы.xlsx', index=False)

def find_OKPD(name):
  df = pd.read_csv('СТЕ_ОКПД-2')
  # print(df.head())
  OKPD_number = 0
  for index, row in df.iterrows():
    if df.iloc[index, 1] == name:
      OKPD_number = df.iloc[index, 2]
  return OKPD_number

def find_in_rules(OKPD):
  flag = False
  # print(OKPD)
  for index, row in full_df.iterrows():
    if isinstance(full_df.iloc[index, 1], str) and str(full_df.iloc[index, 1]) in str(OKPD) and len(full_df.iloc[index, 1])>=5 :
      # print(len(OKPD))
      flag = True
  # if flag == True:
  #   print('found')
  # else:
  #   print('notfound')
  return flag

name = 'Папка-уголок'

OKPD = find_OKPD(name)
find_in_rules(OKPD)
