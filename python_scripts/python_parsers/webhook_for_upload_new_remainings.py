from flask import Flask, request, jsonify
import subprocess

app = Flask(__name__)

@app.route('/webhook', methods=['POST'])
def webhook():
    print("kaif")
    process_message()
    return jsonify({'status': 'Message received'}), 200

def process_message():
    print("Processing message")
    run_consumer()

def run_consumer():
    consumer_script = 'upload_new_remainings.py'
    print(f"Running consumer: {consumer_script}")
    result = subprocess.run(['python3', consumer_script], capture_output=True, text=True)

    print("stdout:")
    print(result.stdout)
    print("stderr:")
    print(result.stderr)

if __name__ == '__main__':
    app.run(port=5000)
