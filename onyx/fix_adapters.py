import os, re

path = r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('private lateinit var dubbedAdapter: AnimeGridAdapter', 'private lateinit var dubbedAdapter: AnimeAiringAdapter')
content = content.replace('private lateinit var popularAdapter: AnimeGridAdapter', 'private lateinit var popularAdapter: AnimeAiringAdapter')
content = content.replace('private lateinit var RecentlyAdapter: AnimeGridAdapter', 'private lateinit var RecentlyAdapter: AnimeAiringAdapter')

content = content.replace('dubbedAdapter = AnimeGridAdapter(', 'dubbedAdapter = AnimeAiringAdapter(')
content = content.replace('popularAdapter = AnimeGridAdapter(', 'popularAdapter = AnimeAiringAdapter(')
content = content.replace('RecentlyAdapter = AnimeGridAdapter(', 'RecentlyAdapter = AnimeAiringAdapter(')

content = content.replace('val movieItem = AnimeGridItem(', 'val movieItem = AiringAnimeItem(')
content = content.replace('val searchItem = AnimeGridItem(', 'val searchItem = AiringAnimeItem(')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
