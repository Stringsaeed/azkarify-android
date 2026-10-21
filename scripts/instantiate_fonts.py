"""Instantiate the variable fonts at the static weights the design system uses.
Requires fonttools and brotli. This preserves outlines and avoids platform weight defaults.

Alan Sans comes from the iOS variable font, kept in design/fonts. Space Grotesk comes from the
design system's variable font in design/fonts.
"""
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
repo=Path(__file__).resolve().parents[1]
root=repo/'app/src/main/res/font'
sources={
    'alan':repo/'design/fonts/alan-sans.ttf',
    'space_grotesk':repo/'design/fonts/space-grotesk-latin.woff2',
}
# Only the weights the type styles use: Latin UI 400-600, Arabic UI 400-700.
weights={
    'alan':[('regular',400),('medium',500),('semibold',600),('bold',700)],
    'space_grotesk':[('regular',400),('medium',500),('semibold',600)],
}
for family,source in sources.items():
    for name,weight in weights[family]:
        font=instantiateVariableFont(TTFont(source),{'wght':weight},inplace=False)
        font.flavor=None
        font.save(root/f'{family}_{name}.ttf')
