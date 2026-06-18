import os, re

path = r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\ShowsFragment.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('currentContent.requireView().findViewById', 'currentContent.findViewById')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
