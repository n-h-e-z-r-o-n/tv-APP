import json

with open(r'C:\Users\user\.gemini\antigravity\brain\1700abd2-c1a4-4666-b744-e03c483838bf\.system_generated\logs\transcript_full.jsonl', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for line in lines:
    try:
        obj = json.loads(line)
        if obj.get('type') == 'PLANNER_RESPONSE':
            tool_calls = obj.get('tool_calls', [])
            for call in tool_calls:
                if call.get('name') == 'view_file':
                    print(f"Step {obj.get('step_index')}: view_file {call['args']}")
    except Exception as e:
        pass
