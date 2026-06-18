import re
with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. SpotlightAnimes -> animeSpotlightSection
content = content.replace('android:id="@+id/spotlightAnimes"', 'android:id="@+id/animeSpotlightSection"')

# 2. Anime_Trending_widget's parent LinearLayout -> animeTrendingSection
# We find Anime_Trending_widget, then find the nearest <LinearLayout before it.
# Actually, we can just replace <LinearLayout above <TextView android:text=" Trending Anime"
content = re.sub(
    r'<LinearLayout([^>]*)>\s*<TextView\s*android:layout_width=\"match_parent\"\s*android:layout_height=\"wrap_content\"\s*android:text=\" Trending Anime\"',
    r'<LinearLayout\1 android:id="@+id/animeTrendingSection">\n<TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text=" Trending Anime"',
    content
)

# 3. Anime_Airing_widget's parent LinearLayout -> animeAiringSection
content = re.sub(
    r'<LinearLayout([^>]*)>\s*<TextView\s*android:layout_width=\"match_parent\"\s*android:layout_height=\"wrap_content\"\s*android:text=\" Airing Anime\"',
    r'<LinearLayout\1 android:id="@+id/animeAiringSection">\n<TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text=" Airing Anime"',
    content
)

# 4. DubbedPageAnime -> dubbSection
content = content.replace('android:id="@+id/DubbedPageAnime"', 'android:id="@+id/dubbSection"')

# 5. FavPageAnime -> favoriteSection
content = content.replace('android:id="@+id/FavPageAnime"', 'android:id="@+id/favoriteSection"')

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'w', encoding='utf-8') as f:
    f.write(content)
