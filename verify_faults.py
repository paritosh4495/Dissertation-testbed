import os
import time
import json
import subprocess
import requests
import threading
from datetime import datetime
import signal

# Configuration
NAMESPACE = "bookstore-testbed"
SERVICES = {
    "inventory": {"svc": "inventory-service", "port": 8081},
    "order": {"svc": "order-service", "port": 8082},
    "payment": {"svc": "payment-service", "port": 8083}
}
RESULTS_DIR = "fault-verification-results"

def run_cmd(cmd):
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout.strip(), result.stderr.strip()

class PortForwarder:
    def __init__(self, service_name, local_port):
        self.service_name = service_name
        self.local_port = local_port
        self.remote_port = SERVICES[service_name]["port"]
        self.svc_name = SERVICES[service_name]["svc"]
        self.process = None

    def start(self):
        print(f"Starting port-forward for {self.service_name} ({self.local_port}:{self.remote_port})...")
        cmd = f"kubectl port-forward svc/{self.svc_name} -n {NAMESPACE} {self.local_port}:{self.remote_port}"
        self.process = subprocess.Popen(
            cmd, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            creationflags=subprocess.CREATE_NEW_PROCESS_GROUP
        )
        max_retries = 15
        for i in range(max_retries):
            time.sleep(1)
            try:
                requests.get(f"http://localhost:{self.local_port}/actuator/health/liveness", timeout=2)
                print(f"Port-forward established after {i+1}s")
                return
            except:
                continue
        raise Exception(f"Failed to establish port-forward for {self.service_name}")

    def stop(self):
        if self.process:
            try:
                os.kill(self.process.pid, signal.CTRL_BREAK_EVENT)
                self.process.wait(timeout=5)
            except:
                pass
            print(f"Stopped port-forward for {self.service_name}")

def save_artifact(fault_id, name, data):
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filepath = os.path.join(RESULTS_DIR, f"{fault_id}_{name}_{timestamp}.json")
    with open(filepath, "w") as f:
        if isinstance(data, (dict, list)):
            json.dump(data, f, indent=2)
        else:
            f.write(str(data))
    return filepath

def test_fault(fault_id, service_name, check_fn):
    print(f"\n--- Testing Fault: {fault_id.upper()} on {service_name} ---")
    local_port = 10000 + SERVICES[service_name]["port"]
    pf = PortForwarder(service_name, local_port)
    try:
        pf.start()
        base_url = f"http://localhost:{local_port}"
        fault_url = f"{base_url}/internal/fault"
        
        print(f"Activating {fault_id}...")
        resp = requests.post(f"{fault_url}/activate/{fault_id}", timeout=20)
        save_artifact(fault_id, "activation", resp.json())
        
        status = requests.get(fault_url).json()
        active_fault = next((f for f in status if f["faultId"] == fault_id), None)
        if not active_fault or not active_fault["active"]:
            print(f"FAILED: {fault_id} is not active according to registry")
            return False

        print("Running materialization check...")
        result = check_fn(base_url, fault_id)
        
        print(f"Deactivating {fault_id}...")
        resp = requests.post(f"{fault_url}/deactivate/{fault_id}", timeout=20)
        save_artifact(fault_id, "deactivation", resp.json())
        
        print("Waiting for service recovery...")
        time.sleep(5)
        try:
            r = requests.get(f"{base_url}/actuator/health/readiness", timeout=10)
            if r.status_code == 200:
                print("Service recovered successfully")
            else:
                print(f"Recovery Warning: Readiness returned {r.status_code}")
        except:
            print("Recovery Warning: Service still unreachable")

        return result
    except Exception as e:
        print(f"ERROR during {fault_id} test: {str(e)}")
        return False
    finally:
        pf.stop()

def check_f1(base_url, fault_id):
    try:
        resp = requests.get(f"{base_url}/api/products", timeout=10)
        save_artifact(fault_id, "api_response", {"status": resp.status_code, "body": resp.text})
        if resp.status_code >= 500:
            print("F1 confirmed: Service returned 500")
            return True
    except Exception as e:
        print(f"F1 confirmed: Connection failed/timed out: {str(e)}")
        return True
    return False

def check_f2(base_url, fault_id):
    latencies = []
    for _ in range(5):
        try:
            start = time.time()
            requests.get(f"{base_url}/api/products", timeout=15)
            latencies.append(time.time() - start)
        except:
            latencies.append(15.0)
    avg_latency = sum(latencies) / len(latencies)
    print(f"Average latency under F2: {avg_latency:.2f}s")
    save_artifact(fault_id, "api_response", {"latencies": latencies, "avg": avg_latency})
    return avg_latency > 0.5

def check_f3(base_url, fault_id):
    order_port = 19082
    opf = PortForwarder("order", order_port)
    try:
        opf.start()
        # Use BK-HP003 (Harry Potter) which has 100 stock to avoid INVENTORY_REJECTED
        payload = {"customerId": "CUST-VERIFY", "items": [{"productCode": "BK-HP003", "quantity": 1}]}
        resp = requests.post(f"http://localhost:{order_port}/api/orders", json=payload, timeout=15)
        save_artifact(fault_id, "api_response", {"status": resp.status_code, "body": resp.text})
        if resp.status_code == 500 or "status\":\"FAILURE" in resp.text:
            print("F3 confirmed: Order creation failed as expected")
            return True
        else:
            print(f"F3 check: Order creation returned {resp.status_code} and body: {resp.text}")
    finally:
        opf.stop()
    return False

def check_f4(base_url, fault_id):
    threads = []
    results = []
    def req():
        try:
            r = requests.get(f"{base_url}/api/products", timeout=10)
            results.append(r.status_code)
        except:
            results.append("timeout")
    for _ in range(5):
        t = threading.Thread(target=req)
        t.start()
        threads.append(t)
    for t in threads:
        t.join()
    save_artifact(fault_id, "api_response", {"results": results})
    if "timeout" in results:
        print("F4 confirmed: Requests timed out")
        return True
    return False

def check_f5(base_url, fault_id):
    out, _ = run_cmd(f"kubectl get pods -n {NAMESPACE} -l app=inventory-service -o jsonpath='{{.items[0].metadata.name}}'")
    pod_name = out
    samples = []
    print(f"Monitoring memory for {pod_name}...")
    for i in range(15):
        out, _ = run_cmd(f"kubectl top pod {pod_name} -n {NAMESPACE} --no-headers")
        if out:
            try:
                parts = out.split()
                mem_str = next(p for p in parts if "Mi" in p)
                mem = int(mem_str.replace("Mi", ""))
                samples.append(mem)
                print(f"Sample {i+1}: {mem}Mi")
            except Exception as e:
                print(f"Sample {i+1}: Parsing error {str(e)}")
        time.sleep(4)
    save_artifact(fault_id, "memory_samples", samples)
    if len(samples) > 5:
        start_avg = sum(samples[:3]) / 3
        end_avg = sum(samples[-3:]) / 3
        print(f"F5 Trend: {start_avg:.1f}Mi -> {end_avg:.1f}Mi")
        return (end_avg - start_avg) > 5 
    return False

if __name__ == "__main__":
    summary = {}
    faults_to_test = [
        ("f1", "inventory", check_f1),
        ("f2", "inventory", check_f2),
        ("f4", "inventory", check_f4),
        ("f5", "inventory", check_f5),
        ("f3", "payment", check_f3)
    ]
    for fid, svc, fn in faults_to_test:
        success = test_fault(fid, svc, fn)
        summary[fid] = "PASSED" if success else "FAILED"
        time.sleep(5)
    print("\n=== VERIFICATION SUMMARY ===")
    print(json.dumps(summary, indent=2))
    with open(os.path.join(RESULTS_DIR, "summary.json"), "w") as f:
        json.dump(summary, f, indent=2)
