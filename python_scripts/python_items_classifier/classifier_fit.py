import os
import sys

import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score
from sklearn.model_selection import train_test_split
import joblib
package_path = r'C:\Users\danil\Desktop\hackaton\Service-for-forecasting-and-formation-of-purchases\python_scripts\python_items_classifier'

# Add the directory to sys.path
sys.path.append(package_path)

# Now you can import the module/package

from model_parameters import CodeSplitter


def is_numeric_code(code):
    return str(code).isnumeric()


def preprocess_data(filepath):
    df = pd.read_excel(filepath)
    df.drop(columns=['Unnamed: 0', 'index'], inplace=True)
    df['Ресурсный'] = df['Ресурсный'].map({'да': 1, 'нет': 0})
    df_cleaned = df.dropna(subset=['Ресурсный'])
    df_cleaned = df_cleaned[df_cleaned['Реестровый номер в РК'].apply(is_numeric_code)]
    return df_cleaned


def split_data(df, target_column):
    X = df.drop(target_column, axis=1)
    y = df[target_column].astype(int)
    return train_test_split(X, y, test_size=0.2, random_state=123)


def build_pipeline():
    preprocessor = ColumnTransformer(
        transformers=[
            ('code_splitter', CodeSplitter(column='Конечный код КПГЗ', max_depth=9),
             ['Конечный код КПГЗ']),
            ('rest', 'passthrough', ['Реестровый номер в РК'])
        ]
    )

    pipeline = Pipeline(steps=[
        ('preprocessor', preprocessor),
        ('classifier', RandomForestClassifier(random_state=123))
    ])

    return pipeline


def train_and_evaluate(pipeline, X_train, X_test, y_train, y_test):
    pipeline.fit(X_train, y_train)
    y_pred = pipeline.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    return accuracy


def main():
    # Filepath to the dataset
    filepath = 'C:\\Users\danil\Desktop\hackaton\Service-for-forecasting-and-formation-of-purchases\python_scripts\python_items_classifier\Dorazmetka.xlsx'

    # Preprocess the data
    df_cleaned = preprocess_data(filepath)

    # Split the data into training and testing sets
    X_train, X_test, y_train, y_test = split_data(df_cleaned, 'Ресурсный')

    # Build the pipeline
    pipeline = build_pipeline()

    # Train the model and evaluate accuracy
    accuracy = train_and_evaluate(pipeline, X_train, X_test, y_train, y_test)

    # Save the pipeline
    joblib.dump(pipeline, 'pipeline.joblib')


if __name__ == '__main__':
    main()
