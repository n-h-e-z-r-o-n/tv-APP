import re

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'r', encoding='utf-8') as f:
    content = f.read()

idx = content.find('ic_anime_search')
print(f'Found ic_anime_search at char index: {idx}')
if idx != -1:
    print('Context:')
    print(content[idx-200:idx+200])

