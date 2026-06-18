import os, re

files = [
    r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt',
    r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\ShowsFragment.kt'
]

for path in files:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace ' window' -> ' requireActivity().window'
    content = re.sub(r'\bwindow\b', 'requireActivity().window', content)
    # Undo it if it became 'requireActivity().requireActivity().window'
    content = content.replace('requireActivity().requireActivity().window', 'requireActivity().window')
    # Undo 'window' in LayoutManager context if any... actually let's just do targeted replaces.
    content = content.replace('requireActivity().windowToken', 'windowToken') # view.windowToken
    content = content.replace('requireActivity().window.clearFlags', 'requireActivity().window.clearFlags')

    # Replace ' theme' -> ' requireActivity().theme'
    content = re.sub(r'\btheme\b', 'requireActivity().theme', content)
    
    # Replace ' currentFocus' -> ' requireActivity().currentFocus'
    content = re.sub(r'\bcurrentFocus\b', 'requireActivity().currentFocus', content)
    
    # Replace ' finish()' -> ' requireActivity().finish()'
    content = re.sub(r'\bfinish\(\)', 'requireActivity().finish()', content)
    
    # Replace 'findViewById' with 'requireView().findViewById' if it's not preceded by '.'
    content = re.sub(r'(?<!\.)\bfindViewById\b', 'requireView().findViewById', content)

    # Any requireView().requireView() mistakes?
    content = content.replace('requireView().requireView()', 'requireView()')
    
    # What about card.requireView().findViewById which I broke before?
    # I already fixed it to card.findViewById. Let's make sure I didn't break it again.
    
    # Some other fixes for ShowsFragment Unresolved reference requireView
    content = content.replace('card.requireView().findViewById', 'card.findViewById')
    content = content.replace('cardTitle.requireView().findViewById', 'cardTitle.findViewById') # just in case

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
