import json
with open(r'C:\Users\user\.gemini\antigravity\brain\1700abd2-c1a4-4666-b744-e03c483838bf\.system_generated\logs\transcript_full.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        obj = json.loads(line)
        if obj.get('step_index') == 463:
            print(str(obj.get('tool_calls', [])))
            break
