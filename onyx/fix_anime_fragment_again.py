import re

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Replace R.id.animeAiringSection with R.id.Anime_Airing_widget and LinearLayout with RecyclerView
content = content.replace('requireView().findViewById<LinearLayout>(R.id.animeAiringSection)', 'requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.Anime_Airing_widget)')

# 2. Fix No value passed for parameter 'overlay' at line 200
# It's probably nimeAdapter = Anime_Grid(dubbedAnimeList, this, overlay) -> nimeAdapter = Anime_Grid(dubbedAnimeList, this) (we'll regex this safely)
content = re.sub(r'Anime_Grid\(([^,]+),\s*(this|requireContext\(\)),\s*dubbFixedFocusOverlay\)', r'Anime_Grid(\1, \2)', content)

# 3. Unresolved reference 'overlayPoster' at line 285
content = re.sub(r'(.*?overlayPoster.*?)\n', r'// \1\n', content)

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt', 'w', encoding='utf-8') as f:
    f.write(content)
