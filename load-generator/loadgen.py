import os
import time
import json
import urllib.request
import urllib.error
import threading
from datetime import datetime

# Configuration from Environment Variables
INVENTORY_URL = os.getenv("INVENTORY_URL", "http://inventory-service:8081/api/products")
ORDER_URL = os.getenv("ORDER_URL", "http://order-service:8082/api/orders")
BROWSE_INTERVAL = float(os.getenv("BROWSE_INTERVAL", "0.2"))
ORDER_INTERVAL = float(os.getenv("ORDER_INTERVAL", "2.0"))
TIMEOUT = 5.0

# Payload for placing orders
ORDER_PAYLOAD = {
    "customerId": "CUST-LOADGEN",
    "items": [{"productCode": "BK-LOADGEN", "quantity": 1}]
}

def log(message):
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] {message}")

def browse_catalog():
    log(f"Starting Browser Thread (Interval: {BROWSE_INTERVAL}s)")
    while True:
        try:
            req = urllib.request.Request(INVENTORY_URL, method="GET")
            with urllib.request.urlopen(req, timeout=TIMEOUT) as response:
                if response.status == 200:
                    # Successfully browsed
                    pass
        except Exception as e:
            log(f"BROWSE ERROR: {str(e)}")
        
        time.sleep(BROWSE_INTERVAL)

def place_order():
    log(f"Starting Buyer Thread (Interval: {ORDER_INTERVAL}s)")
    data = json.dumps(ORDER_PAYLOAD).encode("utf-8")
    
    while True:
        try:
            req = urllib.request.Request(ORDER_URL, data=data, method="POST")
            req.add_header("Content-Type", "application/json")
            with urllib.request.urlopen(req, timeout=TIMEOUT) as response:
                if response.status == 201:
                    # Order successful
                    pass
        except urllib.error.HTTPError as e:
            log(f"ORDER FAILED: HTTP {e.code} - {e.reason}")
        except Exception as e:
            log(f"ORDER ERROR: {str(e)}")
            
        time.sleep(ORDER_INTERVAL)

if __name__ == "__main__":
    log("Starting Dual-Mode Load Generator...")
    log(f"Inventory: {INVENTORY_URL}")
    log(f"Order: {ORDER_URL}")
    
    browser_thread = threading.Thread(target=browse_catalog, name="Browser")
    buyer_thread = threading.Thread(target=place_order, name="Buyer")
    
    browser_thread.daemon = True
    buyer_thread.daemon = True
    
    browser_thread.start()
    buyer_thread.start()
    
    # Keep main thread alive
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        log("Shutting down Load Generator...")
