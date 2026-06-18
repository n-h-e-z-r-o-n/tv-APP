import re

with open(r'c:\Users\user\Desktop\Kotlin Apps\pulseflix-stream-tv\onyx\app\src\main\res\layout\fragment_anime.xml', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace old IDs with new ones
replacements = {
    'SpotlightSection': 'animeSpotlightSection',
    'TrendingSection': 'animeTrendingSection',
    'AiringSection': 'animeAiringSection',
    'DubbedSection': 'dubbSection',
    'DubbSection': 'dubbSection',
    'FixedFocusOverlay': 'dubbFixedFocusOverlay', # might conflict with other sections, but dubbFixedFocusOverlay is needed
    'OverlayPoster': 'dubbOverlayPoster',
    'OverlayTitle': 'dubbOverlayTitle',
    'OverlayYear': 'dubbOverlayYear',
    'OverlayRating': 'dubbOverlayRating'
}

# Wait, Dubbed section has its own FixedFocusOverlay?
# Let's check what IDs exist in the XML for these.
