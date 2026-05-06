# HTTP utility layer for Spring Boot Actuator endpoint access.

import logging
import requests
from config import ACTUATOR_BASE_HOST, ACTUATOR_NODE_PORTS, ACTUATOR_DEFAULT_TIMEOUT

logger = logging.getLogger(__name__)


def actuator_get(
    service: str,
    path: str,
    timeout: int = ACTUATOR_DEFAULT_TIMEOUT,
) -> dict:
    """
    Make a GET request to a Spring Boot Actuator endpoint and return
    the parsed JSON response body.
    """
    if service not in ACTUATOR_NODE_PORTS:
        raise ValueError(
            f"No NodePort configured for service '{service}'. "
            f"Known services: {sorted(ACTUATOR_NODE_PORTS.keys())}")

    port = ACTUATOR_NODE_PORTS[service]
    clean_path = "/" + path.lstrip("/")
    url = f"http://{ACTUATOR_BASE_HOST}:{port}{clean_path}"

    logger.debug(f"Actuator GET {url}")

    try:
        response = requests.get(url, timeout=timeout)
    except requests.exceptions.ConnectionError as e:
        raise ConnectionError(
            f"Could not connect to '{service}' actuator at {url}: {e}")
    except requests.exceptions.Timeout:
        raise TimeoutError(
            f"Request to '{service}' actuator at {url} timed out after {timeout}s."
        )

    if response.status_code != 200:
        raise ValueError(
            f"Actuator returned HTTP {response.status_code} for {url}. "
            f"Body: {response.text[:200]}")

    try:
        return response.json()
    except ValueError:
        raise ValueError(f"Actuator response from {url} is not valid JSON. "
                         f"Body: {response.text[:200]}")
