import os, re

path = r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# We need to remove the variable declarations and their click listeners
# The variables are:
# homeAnimeBtn, favAnimeBtn, searchAnimeBtn, popularAnimeBtn, dubbedAnimeBtn, cWatchAnimeBtn, cNotificationAnimeBtn
# We can just use a regex to remove any lines containing these variables!

variables = ['homeAnimeBtn', 'favAnimeBtn', 'searchAnimeBtn', 'popularAnimeBtn', 'dubbedAnimeBtn', 'cWatchAnimeBtn', 'cNotificationAnimeBtn', 'navBar']

lines = content.split('\n')
new_lines = []
for line in lines:
    keep = True
    for var in variables:
        if var in line:
            keep = False
            break
    if keep:
        new_lines.append(line)

with open(path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(new_lines))
