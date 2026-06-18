import os

def read_file(path):
    try:
        with open(path, 'r', encoding='utf-8') as f:
            return f.read()
    except UnicodeDecodeError:
        with open(path, 'r', encoding='utf-16') as f:
            return f.read()

def convert_shows_fragment():
    path = r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\original_shows.kt'
    content = read_file(path)

    content = content.replace('class Shows_Page : AppCompatActivity() {', 'import androidx.fragment.app.Fragment\nclass ShowsFragment : Fragment(R.layout.fragment_shows) {')
    content = content.replace('override fun onCreate(savedInstanceState: Bundle?) {', 'override fun onViewCreated(view: View, savedInstanceState: Bundle?) {')
    content = content.replace('super.onCreate(savedInstanceState)', 'super.onViewCreated(view, savedInstanceState)')
    content = content.replace('GlobalUtils.applyTheme(this)', 'GlobalUtils.applyTheme(requireActivity())')
    content = content.replace('setContentView(R.layout.activity_shows_page)', '')
    content = content.replace('NavAction.setupSidebar(this)', '')
    content = content.replace('findViewById<', 'view.findViewById<')
    content = content.replace('this@Shows_Page', 'requireActivity()')
    content = content.replace('this,', 'requireActivity(),')

    out_path = r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\ShowsFragment.kt'
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(content)

def convert_anime_fragment():
    path = r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\original_anime.kt'
    content = read_file(path)

    content = content.replace('class Anime_Page : AppCompatActivity() {', 'import androidx.fragment.app.Fragment\nclass AnimeFragment : Fragment(R.layout.fragment_anime) {')
    content = content.replace('override fun onCreate(savedInstanceState: Bundle?) {', 'override fun onViewCreated(view: View, savedInstanceState: Bundle?) {')
    content = content.replace('super.onCreate(savedInstanceState)', 'super.onViewCreated(view, savedInstanceState)')
    content = content.replace('GlobalUtils.applyTheme(this)', 'GlobalUtils.applyTheme(requireActivity())')
    content = content.replace('setContentView(R.layout.activity_anime_page)', '')
    content = content.replace('NavAction.setupSidebar(this)', '')
    content = content.replace('findViewById<', 'view.findViewById<')
    content = content.replace('this@Anime_Page', 'requireActivity()')
    content = content.replace('this,', 'requireActivity(),')

    out_path = r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt'
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(content)

def process_xml_files():
    import shutil
    shutil.copy(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\original_anime.xml', r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml')
    shutil.copy(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\original_shows.xml', r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_shows.xml')
    
    with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'r', encoding='utf-16') as f:
        content = f.read()
    content = content.replace('tools:context=".Anime_Page"', 'tools:context=".AnimeFragment"')
    with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'w', encoding='utf-16') as f:
        f.write(content)
        
    with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_shows.xml', 'r', encoding='utf-16') as f:
        content = f.read()
    content = content.replace('tools:context=".Shows_Page"', 'tools:context=".ShowsFragment"')
    with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_shows.xml', 'w', encoding='utf-16') as f:
        f.write(content)

convert_shows_fragment()
convert_anime_fragment()
process_xml_files()
print("Conversion successful.")
