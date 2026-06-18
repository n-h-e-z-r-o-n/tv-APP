import json
with open(r'C:\Users\user\.gemini\antigravity\brain\1700abd2-c1a4-4666-b744-e03c483838bf\.system_generated\logs\transcript_full.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        try:
            obj = json.loads(line)
            if obj.get('type') == 'PLANNER_RESPONSE':
                for call in obj.get('tool_calls', []):
                    if 'fix_ids.py' in str(call) or 'fix_ids_anime.py' in str(call) or 'fix_all_errors' in str(call) or 'enableEdgeToEdge' in str(call):
                        print(f"Step {obj.get('step_index')}: {str(call)}")
        except:
            pass
