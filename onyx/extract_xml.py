import json

out_lines = []

with open(r'C:\Users\user\.gemini\antigravity\brain\1700abd2-c1a4-4666-b744-e03c483838bf\.system_generated\logs\transcript_full.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        try:
            obj = json.loads(line)
            if obj.get('step_index') == 57: # the output of view_file lines 1-200
                out_lines.append(obj.get('content'))
            elif obj.get('step_index') == 60: # the output of view_file lines 200-664
                out_lines.append(obj.get('content'))
        except Exception as e:
            pass

with open('extracted_anime.xml', 'w', encoding='utf-8') as f:
    for out in out_lines:
        f.write(out)
