from agent import build_agent, build_initial_state, GRAPH_RECURSION_LIMIT

# Should build without error — no LLM call happens at build time
graph_a, prompt_a = build_agent("A")
graph_b, prompt_b = build_agent("B")

print(f"Condition A graph: {type(graph_a)}")
print(f"Condition B graph: {type(graph_b)}")
print(f"Recursion limit: {GRAPH_RECURSION_LIMIT}")   # 41
print(f"Prompt A contains health_a: {'get_service_health_a' in prompt_a}")   # True
print(f"Prompt B contains health_b: {'get_service_health_b' in prompt_b}")   # True
print(f"Prompt A contains health_b: {'get_service_health_b' in prompt_a}")   # False
print(f"Prompt B contains health_a: {'get_service_health_a' in prompt_b}")   # False

# Should raise
try:
    build_agent("C")
except ValueError as e:
    print(f"Correctly rejected: {e}")