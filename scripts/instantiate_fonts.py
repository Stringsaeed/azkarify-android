"""Instantiate the iOS variable font at its named Regular and Bold weights.
Requires fonttools. This preserves outlines and avoids platform weight defaults.
"""
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
root=Path(__file__).resolve().parents[1]/'app/src/main/res/font'
for name,weight in [('alan_regular',400),('alan_bold',700)]:
    font=instantiateVariableFont(TTFont(root/'alan_sans.ttf'),{'wght':weight},inplace=False)
    font.save(root/f'{name}.ttf')
