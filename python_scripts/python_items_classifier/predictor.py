import sys

import pandas as pd
import joblib

package_path = r'C:\Users\danil\Desktop\hackaton\Service-for-forecasting-and-formation-of-purchases\python_scripts\python_items_classifier'

# Add the directory to sys.path
sys.path.append(package_path)

# Now you can import the module/package
from model_parameters import CodeSplitter


def predict(data):
    model = joblib.load('C:\\Users\danil\Desktop\hackaton\Service-for-forecasting-and-formation-of-purchases\python_scripts\python_items_classifier\pipeline.joblib')
    prediction = model.predict(data)
    return prediction