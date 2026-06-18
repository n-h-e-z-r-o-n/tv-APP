import re

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove references to dubbFixedFocusOverlay, dubbOverlayPoster, dubbOverlayTitle, dubbOverlayYear, dubbOverlayRating
lines = content.split('\n')
new_lines = []
for line in lines:
    if 'dubbFixedFocusOverlay' in line or 'dubbOverlayPoster' in line or 'dubbOverlayTitle' in line or 'dubbOverlayYear' in line or 'dubbOverlayRating' in line:
        new_lines.append('// ' + line)
    else:
        new_lines.append(line)

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(new_lines))
