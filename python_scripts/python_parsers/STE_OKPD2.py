import time
import pandas
import pandas as pd
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.keys import Keys
from selenium.common.exceptions import TimeoutException, NoSuchElementException

df = pd.read_json('stock_remainings.Справочники.json')
# print(df['Название СТЕ'])

df_for_answers = pd.DataFrame(columns=['Название СТЕ', 'ОКПД-2'])
df_for_answers['Название СТЕ'] = df['Название СТЕ']

# Setup WebDriver
for i, name in enumerate(df['Название СТЕ']):
    if i < 1801: continue
    driver = webdriver.Chrome()

    try:
        # Open the website
        driver.get('https://zakupki44fz.ru/app/okpd2')

        WebDriverWait(driver, 15).until(
            lambda driver: driver.execute_script('return document.readyState') == 'complete'
        )

        # Your further actions
        time.sleep(3)

        # Wait for the search input field to be present
        search_input = WebDriverWait(driver, 15).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, '#searchInput'))
        )
        # driver.implicitly_wait(50)

        # Interact with the search input
        product_name = name
        search_input.clear()
        search_input.send_keys(product_name)
        search_input.send_keys(Keys.ENTER)

        # Wait for and process search results
        results = WebDriverWait(driver, 15).until(
            EC.presence_of_all_elements_located((By.CSS_SELECTOR,
                                                 'body > div.wrapper > div > app-root > template-with-menu > div > div > div > div > ng-component > div.okpd2-page > okpd2-search-results-page > div > div > div > okpd2-search-results-view > div > div:nth-child(1) > div.okpd2-modal-search-result__item-body > div:nth-child(1) > div > div.classifier-code-wrapper > a'))
        )
        print(i)

        for result in results:
            print(result.text)
            df_for_answers.iloc[i, 1] = result.text
            break
        # print(df_for_answers.head())


    except TimeoutException:
        print("Element not found within the specified timeout.")
    except NoSuchElementException:
        print("Element does not exist.")
    finally:
        driver.quit()

    csv_data = df_for_answers.to_csv("df_saved.csv", encoding='utf-8')
