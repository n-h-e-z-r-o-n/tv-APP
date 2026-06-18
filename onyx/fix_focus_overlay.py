import re

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace FocusOverlay block
content = re.sub(r'(FocusOverlay<AnimeGridItem>\([\s\S]*?\}\n)', r'/* \1 */\n', content)

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt', 'w', encoding='utf-8') as f:
    f.write(content)
