import os, re

files = [
    r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\AnimeFragment.kt',
    r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\java\com\example\onyx\ShowsFragment.kt'
]

for path in files:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Fix view.findViewById to requireView().findViewById
    content = content.replace('view.findViewById<', 'requireView().findViewById<')
    content = content.replace('view?.findViewById<', 'requireView().findViewById<')
    
    # Fix view.findViewById(...) without type parameters
    content = re.sub(r'\bview\.findViewById\(', 'requireView().findViewById(', content)
    
    # Fix onBackPressedDispatcher
    content = content.replace('onBackPressedDispatcher', 'requireActivity().onBackPressedDispatcher')
    content = content.replace('requireActivity().requireActivity().onBackPressedDispatcher', 'requireActivity().onBackPressedDispatcher')
    
    # Fix 'this' passed as Context. We replaced 'this,' with 'requireActivity(),' in the first pass.
    # What about 'this)' ?
    content = content.replace('this)', 'requireActivity())')
    
    # Fix specifically Glide.with(this) -> Glide.with(requireActivity()) or requireView()
    content = content.replace('Glide.with(this)', 'Glide.with(requireActivity())')
    
    # Let's fix specific Context mismatch errors like Dialog(this)
    content = content.replace('Dialog(this)', 'Dialog(requireActivity())')
    content = content.replace('CustomKeyboardManager(\n            this,', 'CustomKeyboardManager(\n            requireActivity(),')
    content = content.replace('CustomKeyboardManager(this,', 'CustomKeyboardManager(requireActivity(),')

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
