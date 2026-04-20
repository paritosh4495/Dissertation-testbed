import os
import time
import json
import urllib.request
import urllib.error

# Configuration from Environment Variables
TARGET_URL = os.getenv("TARGET_URL", "http://order-service:8080/api/orders")
INTERVAL = float(os.getenv("INTERVAL_SECONDS", "1.0"))
TIMEOUT = 5.0

# Simple hardcoded payload for the bookstore testbed
PAYLOAD = {
    "customerId": "CUST-LOADGEN",
    "items": [{"productCode": "BK-EJ002", "quantity": 1}]
}

def send_request():
    data = json.dumps(PAYLOAD).encode("utf-8")
    req = urllib.request.Request(TARGET_URL, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    
    try:
        # Added 5-second timeout as requested
        with urllib.request.urlopen(req, timeout=TIMEOUT) as response:
            print(f"[{time.ctime()}] SUCCESS: Status {response.status}")
    except urllib.error.HTTPError as e:
        print(f"[{time.ctime()}] FAILED: HTTP {e.code} - {e.reason}")
    except urllib.error.URLError as e:
        print(f"[{time.ctime()}] ERROR: Connection failed - {e.reason}")
    except Exception as e:
        print(f"[{time.ctime()}] ERROR: {str(e)}")

if __name__ == "__main__":
    print(f"Starting Load Generator...")
    print(f"Target: {TARGET_URL} | Interval: {INTERVAL}s | Timeout: {TIMEOUT}s")
    while True:
        send_request()
        time.sleep(INTERVAL)
