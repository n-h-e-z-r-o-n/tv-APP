import re

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('@drawable/ic_anime_search', '@drawable/ic_search')

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'w', encoding='utf-8') as f:
    f.write(content)
