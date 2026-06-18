import re
with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\activity_anime_page.xml.bak', 'r', encoding='utf-16') as f:
    content = f.read()

match = re.search(r'android:id=\"@\+id/HomePageAnime\"[\s\S]*?(?=<!-- //////////////////////// DUBBED PAGE ////////////////////////////////////// -->)', content)
if match:
    block = match.group(0)
    ids = re.findall(r'android:id=\"@\+id/([^\"]+)\"', block)
    print('IDs inside HomePageAnime:')
    for id_name in sorted(set(ids)):
        print(id_name)
