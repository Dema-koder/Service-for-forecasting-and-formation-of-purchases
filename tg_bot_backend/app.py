import time
from functools import wraps
import jwt
from bidict import bidict
from flask import Flask, render_template, request, jsonify
from keycloak import KeycloakOpenID

keycloak_openid = KeycloakOpenID(server_url="http://127.0.0.1:8080/",
                                 client_id="client_id",
                                 realm_name="realm_name",
                                 client_secret_key="client_secret_key")

app = Flask(__name__)

AUTHORIZATION_TOKEN = "AUTHORIZATION_TOKEN"

tokens = {}
sessions = bidict({})


def token_required(f):
    @wraps(f)
    def decorator(*args, **kwargs):
        token = request.headers.get('Authorization')
        if not token:
            return jsonify({"message": "Token is missing!"}), 401
        if token != AUTHORIZATION_TOKEN:
            return jsonify({"message": "Token is invalid!"}), 401
        return f(*args, **kwargs)
    return decorator


@app.route('/authenticate')
def authenticate():
    if "code" in request.args:
        code = request.args.get('code')
        current_state = request.args.get('state')
        access_token = keycloak_openid.token(
            grant_type='authorization_code',
            code=code,
            redirect_uri="http://127.0.0.1:9111/authenticate")
        user_id = sessions.inverse[current_state]
        tokens[user_id] = access_token
        del sessions.inverse[current_state]
        return render_template('success_page.html')
    return render_template('fail_page.html')


@app.route('/token/<int:user_id>',  methods=['GET'])
@token_required
def get_token(user_id):
    if user_id in tokens:
        return jsonify(tokens[user_id]), 200
    return jsonify({"message": "Token not found"}), 404


@app.route('/token/<int:user_id>', methods=['DELETE'])
@token_required
def delete_token(user_id):
    if user_id in tokens:
        del tokens[user_id]
        return '', 204
    return jsonify({"message": "Token not found"}), 404


@app.route('/token/<int:user_id>/expired', methods=['GET'])
@token_required
def check_token_expired(user_id):
    if user_id in tokens:
        access_token = tokens['user_id']['access_token']
        decoded_access_tkn = jwt.decode(access_token, options={"verify_signature": False})
        if decoded_access_tkn["exp"] - time.time() < 0:
            return jsonify({'expired': True}), 200
        else:
            return jsonify({'expired': False}), 200
    return jsonify({"message": "Token not found"}), 404


@app.route('/token/<int:user_id>/refresh-token-expired', methods=['GET'])
@token_required
def check_refresh_token_expired(user_id):
    if user_id in tokens:
        refresh_token = tokens['user_id']['refresh_token']
        decoded_refresh_tkn = jwt.decode(refresh_token, options={"verify_signature": False})
        if decoded_refresh_tkn["exp"] - time.time() < 0:
            return jsonify({'expired': True}), 200
        else:
            return jsonify({'expired': False}), 200
    return jsonify({"message": "Token not found"}), 404


@app.route('/token/<int:user_id>/refresh', methods=['POST'])
@token_required
def refresh_token(user_id):
    if user_id in tokens:
        tokens['user_id'] = keycloak_openid.refresh_token(tokens['user_id']['refresh_token'])
        return '', 204
    return jsonify({"message": "Token not found"}), 404


@app.route('/store-session', methods=['POST'])
@token_required
def store_session():
    try:
        data = request.get_json()
        sessions[data['user_id']] = str(data["state"])
        return '', 204
    except Exception as e:
        return jsonify({"message": str(e)}), 400


@app.route('/home')
def home():
    return "home page"


@app.route('/')
def index():
    return "Flask app for Telegram Bot Authentication"