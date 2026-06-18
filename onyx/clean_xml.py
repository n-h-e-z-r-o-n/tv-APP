import re

with open('extracted_anime.xml', 'r', encoding='utf-8') as f:
    content = f.read()

# The output from view_file includes some header lines like "File Path: ...", "Total Lines: ...", "Showing lines ..."
# And each line is like "1: <?xml version="1.0" encoding="utf-8"?>"
# We just want to extract the actual code.

lines = content.split('\n')
cleaned_lines = []
for line in lines:
    m = re.match(r'^\d+:\s(.*)$', line)
    if m:
        cleaned_lines.append(m.group(1))

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'w', encoding='utf-8') as f:
    f.write('\n'.join(cleaned_lines))
