
# Kubernetes utility layer shared across all diagnostic tools.

import logging
from typing import Optional
from kubernetes import client, config
from config import NAMESPACE

logger = logging.getLogger(__name__)


_k8s_init_error: Optional[Exception] = None

try:
    config.load_kube_config()
    _core_v1    = client.CoreV1Api()
    _custom_api = client.CustomObjectsApi()
    logger.info("Kubernetes client initialised successfully.")
except Exception as e:
    _k8s_init_error = e
    _core_v1    = None  # type: ignore[assignment]
    _custom_api = None  # type: ignore[assignment]
    logger.warning(f"Kubernetes client initialisation failed: {e}. "
                   "Tools will return errors until a valid kubeconfig is available.")


def _require_client() -> None:
    """Raise RuntimeError if K8s client failed to initialise."""
    if _k8s_init_error is not None:
        raise RuntimeError(
            f"Kubernetes client is not available: {_k8s_init_error}"
        )


# Pod resolution

def get_pod(
    service_name: str,
    namespace: str = NAMESPACE,
) -> Optional[client.V1Pod]:
    """Return a V1Pod object for the given service name, or None if not found.
    """
    _require_client()

    pods = _core_v1.list_namespaced_pod(
        namespace,
        label_selector=f"app={service_name}"
    )

    if not pods.items:
        logger.warning(f"No pods found for service '{service_name}' in namespace '{namespace}'.")
        return None

    # Prefer a Running pod.
    for pod in pods.items:
        if pod.status.phase == "Running":
            return pod

    # No Running pod — return the first available (handles transient states
    # like Pending or ContainerCreating during F6 recovery).
    logger.warning(
        f"No Running pod found for '{service_name}'. "
        f"Returning pod in phase: {pods.items[0].status.phase}"
    )
    return pods.items[0]


def get_pod_name(
    service_name: str,
    namespace: str = NAMESPACE,
) -> Optional[str]:
    """
    Convenience wrapper — returns the pod name string, or None.
    Use get_pod() directly when you need the full pod object.
    """
    pod = get_pod(service_name, namespace)
    return pod.metadata.name if pod else None


def get_pod_restart_count(
    service_name: str,
    namespace: str = NAMESPACE,
) -> int:
    """
    Return the restart count for the first container in the service's pod.
    Returns 0 if the pod is not found or container statuses are unavailable.
    """
    pod = get_pod(service_name, namespace)
    if pod is None:
        return 0
    try:
        return pod.status.container_statuses[0].restart_count
    except (TypeError, IndexError, AttributeError):
        return 0


# Kubernetes API client accessors

def core_v1_api() -> client.CoreV1Api:
    _require_client()
    return _core_v1


def custom_api() -> client.CustomObjectsApi:
    _require_client()
    return _custom_api


# Unit parsers

def parse_cpu_to_millicores(cpu_str: str) -> int:
    """
    Parse a Kubernetes CPU quantity string to millicores (integer).

    Handles all formats produced by the Metrics Server and resource specs:
        "245m"      → 245      (millicores suffix)
        "1"         → 1000     (whole core, no suffix)
        "0.5"       → 500      (decimal cores)
        "500000u"   → 500      (microcores → millicores, rounded)

    Args:
        cpu_str: CPU quantity string from K8s API.

    Returns:
        Integer millicore value.

    Raises:
        ValueError: If the format is not recognised.
    """
    s = cpu_str.strip()
    if s.endswith("m"):
        return int(s[:-1])
    if s.endswith("u"):
        # Microcores: 1000 microcores = 1 millicore
        return int(s[:-1]) // 1000
    if s.endswith("n"):
        # Nanocores: 1,000,000 nanocores = 1 millicore
        return int(s[:-1]) // 1_000_000
    # Whole or decimal cores
    try:
        return int(float(s) * 1000)
    except ValueError:
        raise ValueError(f"Unrecognised CPU quantity format: '{cpu_str}'")


def parse_memory_to_bytes(mem_str: str) -> int:
    """
    Parse a Kubernetes memory quantity string to bytes (integer).

    Handles binary (IEC) and decimal (SI) suffixes:
        "512Mi"      → 536870912    (mebibytes)
        "1Gi"        → 1073741824   (gibibytes)
        "524288Ki"   → 536870912    (kibibytes)
        "850M"       → 891289600    (megabytes, SI)
        "1G"         → 1000000000   (gigabytes, SI)
        "512000000"  → 512000000    (raw bytes, no suffix)

    Args:
        mem_str: Memory quantity string from K8s API.

    Returns:
        Integer byte value.

    Raises:
        ValueError: If the format is not recognised.
    """
    s = mem_str.strip()

    # Binary (IEC) suffixes — powers of 1024
    _binary: dict[str, int] = {
        "Ki": 1024,
        "Mi": 1024 ** 2,
        "Gi": 1024 ** 3,
        "Ti": 1024 ** 4,
        "Pi": 1024 ** 5,
    }
    # Decimal (SI) suffixes — powers of 1000
    _decimal: dict[str, int] = {
        "K": 1000,
        "M": 1000 ** 2,
        "G": 1000 ** 3,
        "T": 1000 ** 4,
        "P": 1000 ** 5,
    }

    for suffix, multiplier in _binary.items():
        if s.endswith(suffix):
            return int(s[: -len(suffix)]) * multiplier

    for suffix, multiplier in _decimal.items():
        if s.endswith(suffix):
            return int(s[: -len(suffix)]) * multiplier

    # Raw bytes — no suffix
    try:
        return int(s)
    except ValueError:
        raise ValueError(f"Unrecognised memory quantity format: '{mem_str}'")