import os, re

path = r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('private lateinit var dubbedAdapter: AnimeAiringAdapter', 'private lateinit var dubbedAdapter: AnimeGridAdapter')
content = content.replace('private lateinit var popularAdapter: AnimeAiringAdapter', 'private lateinit var popularAdapter: AnimeGridAdapter')
content = content.replace('private lateinit var RecentlyAdapter: AnimeAiringAdapter', 'private lateinit var RecentlyAdapter: AnimeGridAdapter')

content = content.replace('dubbedAdapter = AnimeAiringAdapter(', 'dubbedAdapter = AnimeGridAdapter(')
content = content.replace('popularAdapter = AnimeAiringAdapter(', 'popularAdapter = AnimeGridAdapter(')
content = content.replace('RecentlyAdapter = AnimeAiringAdapter(', 'RecentlyAdapter = AnimeGridAdapter(')

movieItem_replacement = '''val movieItem = AnimeGridItem(
    id = id,
    anilistId = "",
    malId = "",
    title = title,
    japaneseTitle = "",
    poster = imageUrl,
    backdropUrl = null,
    description = "",
    releaseDate = "",
    type = type,
    quality = "",
    status = "",
    genres = emptyList(),
    duration = "",
    sub = sub,
    dub = dub,
    rating = ""
)'''
# The current code has:
# val movieItem = AiringAnimeItem(
#    id,
#    title,
#    imageUrl,
#    type,
#    sub,
#    dub
# )
content = re.sub(r'val movieItem = AiringAnimeItem\(\s*id,\s*title,\s*imageUrl,\s*type,\s*sub,\s*dub\s*\)', movieItem_replacement, content)

searchItem_replacement = '''val searchItem = AnimeGridItem(
    id = id,
    anilistId = "",
    malId = "",
    title = title,
    japaneseTitle = "",
    poster = imageUrl,
    backdropUrl = null,
    description = "",
    releaseDate = "",
    type = type,
    quality = "",
    status = "",
    genres = emptyList(),
    duration = "",
    sub = sub,
    dub = dub,
    rating = ""
)'''
content = re.sub(r'val searchItem = AiringAnimeItem\(\s*id,\s*title,\s*imageUrl,\s*type,\s*sub,\s*dub\s*\)', searchItem_replacement, content)


with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
