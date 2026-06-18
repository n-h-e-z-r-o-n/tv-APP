import json

with open(r'C:\Users\user\.gemini\antigravity\brain\1700abd2-c1a4-4666-b744-e03c483838bf\.system_generated\logs\transcript_full.jsonl', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for line in lines:
    if 'Anime_Page.kt.bak' in line:
        try:
            obj = json.loads(line)
            print(f"Step {obj.get('step_index')}: {obj.get('type')} {str(obj.get('content'))[:50]}")
            if obj.get('type') == 'PLANNER_RESPONSE':
                print(str(obj.get('tool_calls'))[:200])
        except Exception as e:
            pass
