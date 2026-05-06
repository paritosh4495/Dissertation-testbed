# Condition B diagnostic tools — Framework-Native Observability.

from __future__ import annotations

import logging
from typing import Optional

from config import VALID_SERVICES
from schemas.schemas import ToolResponse
from utils.actuator_utils import actuator_get

logger = logging.getLogger(__name__)

# Maximum circuit breaker events to return to the agent.

_CB_MAX_EVENTS = 20

# When filtering SUCCESS events, keep only this many most recent.
_CB_SUCCESS_KEEP = 5

# Tool 1 — get_service_health_b


def get_service_health_b(service: str) -> ToolResponse:
    """
    Return the full Spring Boot Actuator health hierarchy for a service.
    """
    tool_name = "get_service_health_b"

    if service not in VALID_SERVICES:
        return ToolResponse(
            tool=tool_name,
            status="error",
            service=service,
            error_message=(f"Unknown service '{service}'. "
                           f"Valid services: {sorted(VALID_SERVICES)}"),
        )

    try:
        data = actuator_get(service, "/actuator/health")
        return ToolResponse(
            tool=tool_name,
            status="success",
            service=service,
            data=data,
        )

    except Exception as e:
        logger.exception(f"[{tool_name}] Error for service '{service}'")
        return ToolResponse(
            tool=tool_name,
            status="error",
            service=service,
            error_message=str(e),
        )


# Tool 2 — query_actuator_metrics


def query_actuator_metrics(
    service: str,
    metric_name: Optional[str] = None,
) -> ToolResponse:
    """
    Query Spring Boot Actuator metrics for a service.

    When called without metric_name, returns the full list of available
    metric names. Use this first if you are unsure which metrics exist,
    then call again with a specific metric_name to get its value.

    When called with metric_name, returns the current measurement(s)
    for that metric including value, statistic type, and available tags.
    Call repeatedly across reasoning steps to observe trends — each
    call is a fresh independent sample.

    Commonly useful metric names for fault diagnosis:

      HikariCP connection pool:
        hikaricp.connections.active   — currently acquired connections
        hikaricp.connections.pending  — threads waiting for a connection
        hikaricp.connections.max      — pool size ceiling
        hikaricp.connections.timeout  — cumulative acquisition timeouts

      JVM memory (use tag: area=heap or area=nonheap):
        jvm.memory.used               — bytes currently used
        jvm.memory.max                — maximum heap size

      JVM threads:
        jvm.threads.live              — current live thread count
        jvm.threads.peak              — peak since JVM start
        jvm.threads.states            — breakdown by state (use tag: state=...)
          thread state tag values: runnable, blocked, waiting, timed-waiting

      CPU:
        process.cpu.usage             — JVM process CPU (0.0–1.0)
        system.cpu.usage              — host CPU (0.0–1.0)

      HTTP:
        http.server.requests          — request count/time by uri and status
          use tags: uri=..., status=..., outcome=...
    """
    tool_name = "query_actuator_metrics"

    if service not in VALID_SERVICES:
        return ToolResponse(
            tool=tool_name,
            status="error",
            service=service,
            error_message=(f"Unknown service '{service}'. "
                           f"Valid services: {sorted(VALID_SERVICES)}"),
        )

    try:
        if metric_name is None:
            # Return full list of available metric names
            raw = actuator_get(service, "/actuator/metrics")
            names = raw.get("names", [])
            return ToolResponse(
                tool=tool_name,
                status="success",
                service=service,
                data={
                    "metric_name":
                    None,
                    "available_metrics":
                    sorted(names),
                    "count":
                    len(names),
                    "note":
                    ("Call again with a specific metric_name to get its value. "
                     "See tool description for commonly useful metric names."),
                },
            )

        else:
            # Return measurement for the requested metric
            path = f"/actuator/metrics/{metric_name.strip()}"
            raw = actuator_get(service, path)

            return ToolResponse(
                tool=tool_name,
                status="success",
                service=service,
                data={
                    "metric_name": raw.get("name"),
                    "description": raw.get("description"),
                    "base_unit": raw.get("baseUnit"),
                    "measurements": raw.get("measurements", []),
                    "available_tags": raw.get("availableTags", []),
                },
            )

    except Exception as e:
        logger.exception(
            f"[{tool_name}] Error for service '{service}', metric '{metric_name}'"
        )
        return ToolResponse(
            tool=tool_name,
            status="error",
            service=service,
            error_message=str(e),
        )


# Tool 3 — get_circuit_breaker_state


def get_circuit_breaker_state(service: str) -> ToolResponse:
    """
    Return the current Resilience4j circuit breaker state and recent
    event history for a service.
    """
    tool_name = "get_circuit_breaker_state"

    if service not in VALID_SERVICES:
        return ToolResponse(
            tool=tool_name,
            status="error",
            service=service,
            error_message=(f"Unknown service '{service}'. "
                           f"Valid services: {sorted(VALID_SERVICES)}"),
        )

    # --- Fetch state snapshot ---
    try:
        state_raw = actuator_get(service, "/actuator/circuitbreakers")
    except Exception as e:
        logger.exception(
            f"[{tool_name}] Failed to fetch circuit breaker state for '{service}'"
        )
        return ToolResponse(
            tool=tool_name,
            status="error",
            service=service,
            error_message=f"Could not fetch circuit breaker state: {e}",
        )

    # --- Fetch event history ---
    # Use a longer timeout — service may be under stress during F3.
    events_raw: dict = {}
    events_error: Optional[str] = None
    try:
        events_raw = actuator_get(
            service,
            "/actuator/circuitbreakerevents",
            timeout=8,
        )
    except Exception as e:
        # Events unavailable is non-fatal — state snapshot is still useful.
        logger.warning(
            f"[{tool_name}] Could not fetch circuit breaker events for '{service}': {e}"
        )
        events_error = str(e)

    # --- Filter and rank events ---
    all_events = events_raw.get("circuitBreakerEvents", [])
    filtered = _filter_cb_events(all_events)

    # --- Build response ---
    cb_states = state_raw.get("circuitBreakers", {})

    return ToolResponse(
        tool=tool_name,
        status="success",
        service=service,
        data={
            "circuit_breakers":
            cb_states,
            "event_count_total":
            len(all_events),
            "events_shown":
            len(filtered),
            "events":
            filtered,
            "events_note":
            ("STATE_TRANSITION and ERROR events always shown. "
             f"SUCCESS events capped at last {_CB_SUCCESS_KEEP}. "
             "Events ordered most recent first."),
            **({
                "events_error": events_error
            } if events_error else {}),
        },
    )


def _filter_cb_events(events: list[dict]) -> list[dict]:
    """
    Filter and rank circuit breaker events for agent consumption.

    Priority order (most recent first within each group):
      1. STATE_TRANSITION — definitive fault signal
      2. ERROR            — individual call failures
      3. NOT_PERMITTED    — calls blocked while OPEN
      4. SUCCESS          — last _CB_SUCCESS_KEEP only

    Total capped at _CB_MAX_EVENTS.
    """
    # Reverse so most recent is first (API returns oldest first)
    events = list(reversed(events))

    priority = []
    successes = []

    for e in events:
        t = e.get("type", "")
        if t in ("STATE_TRANSITION", "ERROR", "NOT_PERMITTED"):
            priority.append(e)
        elif t == "SUCCESS":
            if len(successes) < _CB_SUCCESS_KEEP:
                successes.append(e)

    combined = priority + successes
    return combined[:_CB_MAX_EVENTS]
