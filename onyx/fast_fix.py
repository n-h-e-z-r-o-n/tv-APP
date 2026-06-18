import os, re

files = [
    r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt',
    r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\ShowsFragment.kt'
]

for path in files:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Fix edge to edge
    content = re.sub(r'^.*enableEdgeToEdge\(\).*$\n', '', content, flags=re.MULTILINE)
    content = re.sub(r'^.*import androidx\.activity\.enableEdgeToEdge.*$\n', '', content, flags=re.MULTILINE)

    # Fix card.requireView()
    content = content.replace('card.requireView().', 'card.')
    content = content.replace('cardTitle.requireView().', 'cardTitle.')
    
    # Fix AnimeGridItem vs AiringAnimeItem
    content = content.replace('val movieItem = AiringAnimeItem(', 'val movieItem = AnimeGridItem(')
    content = content.replace('val searchItem = AiringAnimeItem(', 'val searchItem = AnimeGridItem(')

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
