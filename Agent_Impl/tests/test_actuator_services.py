# run as: python -c "..."
from utils.actuator_utils import actuator_get

for svc in ["inventory-service", "order-service", "payment-service"]:
    try:
        data = actuator_get(svc, "/actuator/health")
        print(f"{svc}: {data['status']}")
    except Exception as e:
        print(f"{svc}: ERROR — {e}")
