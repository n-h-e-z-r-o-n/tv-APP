import os, re, shutil

# Read as UTF-16 and write as UTF-8
def fix_xml(src, dest, old_context, new_context):
    try:
        with open(src, 'r', encoding='utf-8') as f:
            content = f.read()
    except:
        with open(src, 'r', encoding='utf-16') as f:
            content = f.read()
            
    content = content.replace('ic_anime_search', 'ic_search')
    content = content.replace(old_context, new_context)
    
    with open(dest, 'w', encoding='utf-8') as f:
        f.write(content)

fix_xml('original_anime.xml', r'app\src\main\res\layout\fragment_anime.xml', 'tools:context=".Anime_Page"', 'tools:context=".AnimeFragment"')
fix_xml('original_shows.xml', r'app\src\main\res\layout\fragment_shows.xml', 'tools:context=".Shows_Page"', 'tools:context=".ShowsFragment"')

def convert_kt(input_path, output_path, fragment_name, xml_name):
    try:
        with open(input_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except UnicodeDecodeError:
        with open(input_path, 'r', encoding='utf-16') as f:
            content = f.read()
            
    activity_name = input_path.split('\\')[-1].replace('original_', '').replace('.kt', '').capitalize() + '_Page'
    if 'anime' in input_path:
        activity_name = 'Anime_Page'
    else:
        activity_name = 'Shows_Page'
        
    content = content.replace(f'class {activity_name} : AppCompatActivity() {{', f'import androidx.fragment.app.Fragment\nclass {fragment_name} : Fragment(R.layout.{xml_name}) {{')
    content = content.replace('override fun onCreate(savedInstanceState: Bundle?) {', 'override fun onViewCreated(view: View, savedInstanceState: Bundle?) {')
    content = content.replace('super.onCreate(savedInstanceState)', 'super.onViewCreated(view, savedInstanceState)')
    
    content = content.replace('GlobalUtils.applyTheme(this)', 'GlobalUtils.applyTheme(requireActivity())')
    content = content.replace(f'setContentView(R.layout.activity_{activity_name.lower()})', '')
    content = content.replace('NavAction.setupSidebar(this)', '')
    
    content = content.replace(f'this@{activity_name}', 'requireActivity()')
    content = content.replace('this,', 'requireActivity(),')
    content = content.replace('Glide.with(this)', 'Glide.with(requireActivity())')
    content = content.replace('Dialog(this)', 'Dialog(requireActivity())')
    content = content.replace('this)', 'requireActivity())')
    
    content = re.sub(r'\bwindow\b', 'requireActivity().window', content)
    content = content.replace('requireActivity().requireActivity().window', 'requireActivity().window')
    content = content.replace('requireActivity().windowToken', 'windowToken') 
    content = content.replace('requireActivity().window.clearFlags', 'requireActivity().window.clearFlags')
    content = re.sub(r'\btheme\b', 'requireActivity().theme', content)
    content = re.sub(r'\bcurrentFocus\b', 'requireActivity().currentFocus', content)
    content = re.sub(r'\bfinish\(\)', 'requireActivity().finish()', content)
    
    # Use requireView() for fragments!
    content = content.replace('findViewById<', 'requireView().findViewById<')
    content = re.sub(r'(?<!\.)\bfindViewById\(', 'requireView().findViewById(', content)
    
    content = content.replace('onBackPressedDispatcher', 'requireActivity().onBackPressedDispatcher')

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(content)

convert_kt('original_anime.kt', r'app\src\main\java\com\example\onyx\AnimeFragment.kt', 'AnimeFragment', 'fragment_anime')
convert_kt('original_shows.kt', r'app\src\main\java\com\example\onyx\ShowsFragment.kt', 'ShowsFragment', 'fragment_shows')

print("Restore complete.")
