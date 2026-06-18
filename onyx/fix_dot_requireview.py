import os

files = [
    r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt',
    r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\ShowsFragment.kt'
]

for path in files:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    content = content.replace('.requireView().findViewById', '.findViewById')

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
