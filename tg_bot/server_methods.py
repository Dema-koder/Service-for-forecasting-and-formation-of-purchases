import requests

AUTHORIZATION_TOKEN = "AUTHORIZATION_TOKEN"
MAIN_URI = "http://127.0.0.1:9111/"


def server_get_token(user_id):
    headers = {'Authorization': AUTHORIZATION_TOKEN}
    response = requests.get(MAIN_URI + "token/" + str(user_id), headers=headers)
    if response.status_code == 200:
        return response.json()
    return None


def server_post_session(user_id, state):
    headers = {'Authorization': AUTHORIZATION_TOKEN}
    response = requests.post(MAIN_URI + "store-session", headers=headers, json={'user_id': user_id, 'state': state})
    return response.text


def server_delete_token(user_id):
    headers = {'Authorization': AUTHORIZATION_TOKEN}
    requests.delete(MAIN_URI + "token/" + str(user_id), headers=headers)


def server_token_expired(user_id):
    headers = {'Authorization': AUTHORIZATION_TOKEN}
    response = requests.get(MAIN_URI + "token/" + str(user_id) + "/expired", headers=headers)
    if response.status_code == 200:
        return response.json()['expired']
    return None


def server_refresh_expired(user_id):
    headers = {'Authorization': AUTHORIZATION_TOKEN}
    response = requests.get(MAIN_URI + "token/" + str(user_id) + "/refresh-token-expired", headers=headers)
    if response.status_code == 200:
        return response.json()['expired']
    return None


def server_refresh_token(user_id):
    headers = {'Authorization': AUTHORIZATION_TOKEN}
    requests.post(MAIN_URI + "token/" + str(user_id) + "/refresh", headers=headers)
