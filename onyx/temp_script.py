import re
with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'r', encoding='utf-8') as f:
    content = f.read()

ids = re.findall(r'android:id=\"@\+id/([^\"]+)\"', content)
for id_name in sorted(set(ids)):
    if 'Section' in id_name or 'Overlay' in id_name or 'Spotlight' in id_name or 'Trending' in id_name or 'Airing' in id_name or 'Dubbed' in id_name:
        print(id_name)
