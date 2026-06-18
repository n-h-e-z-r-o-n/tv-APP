import re

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('androidx.drawerlayout.widget.DrawerLayout', 'androidx.constraintlayout.widget.ConstraintLayout')
content = re.sub(r'<include layout=\"@layout/side_bar\"[\s\S]*?/>', '', content)
content = re.sub(r'<androidx\.coordinatorlayout\.widget\.CoordinatorLayout[\s\S]*?>', '', content, count=1)
content = content.replace('</androidx.coordinatorlayout.widget.CoordinatorLayout>', '')
content = re.sub(r'<com\.google\.android\.material\.appbar\.AppBarLayout[\s\S]*?</com\.google\.android\.material\.appbar\.AppBarLayout>', '', content)

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'w', encoding='utf-8') as f:
    f.write(content)
