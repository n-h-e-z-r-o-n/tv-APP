import json

with open(r'C:\Users\user\.gemini\antigravity\brain\1700abd2-c1a4-4666-b744-e03c483838bf\.system_generated\logs\transcript_full.jsonl', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for line in lines:
    obj = json.loads(line)
    if obj.get('step_index') == 426:
        content = obj.get('content', '')
        with open('AnimeFragment_10AM.txt', 'w', encoding='utf-8') as out:
            out.write(content)
        break
