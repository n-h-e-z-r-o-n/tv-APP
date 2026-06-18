import os

for filename in ['fragment_anime.xml', 'fragment_shows.xml']:
    path = r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\\' + filename
    with open(path, 'r', encoding='utf-16') as f:
        content = f.read()
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
