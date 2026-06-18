import os, re

files = [
    r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt',
    r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\ShowsFragment.kt'
]

sidebar_ids = [
    'sidebarBtnShows', 'sidebarBtnAnime', 'sidebarSearchBtn', 
    'sidebarWatchListBtn', 'sidebarNotificationBtn', 'sidebarBtnProfile',
    'cNotificationAnimeIcon'
]

for path in files:
    if not os.path.exists(path): continue
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # remove enableEdgeToEdge entirely
    content = re.sub(r'^.*enableEdgeToEdge\(\).*$\n', '', content, flags=re.MULTILINE)
    content = re.sub(r'^.*import androidx\.activity\.enableEdgeToEdge.*$\n', '', content, flags=re.MULTILINE)

    # replace view.findViewById with requireActivity().findViewById for sidebar ids
    for sid in sidebar_ids:
        content = content.replace(f'view.findViewById<ImageView>(R.id.{sid})', f'requireActivity().findViewById<ImageView>(R.id.{sid})')
        content = content.replace(f'view.findViewById<CardView>(R.id.{sid})', f'requireActivity().findViewById<CardView>(R.id.{sid})')
        content = content.replace(f'view.findViewById<ImageButton>(R.id.{sid})', f'requireActivity().findViewById<ImageButton>(R.id.{sid})')

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
