"""Copy the canonical iOS content, font, localized copy and journey catalog.
Run: python3 scripts/import_ios_assets.py ../azkarify
Artwork PNGs are rendered separately from the unchanged source SVGs.
"""
import hashlib
import json
import re
import shutil
import sys
from pathlib import Path

source = Path(sys.argv[1]).resolve()
target = Path(__file__).resolve().parents[1] / 'app/src/main'
if (target / 'assets/content').exists():
    shutil.rmtree(target / 'assets/content')
shutil.copytree(source / 'content', target / 'assets/content')
shutil.copyfile(source / 'azkarify/Fonts/AlanSans.ttf', target / 'res/font/alan_sans.ttf')
shutil.copyfile(source / 'azkarify/Fonts/ALAN-SANS-LICENSE.txt', target / 'assets/ALAN-SANS-LICENSE.txt')
strings = json.loads((source / 'azkarify/Localizable.xcstrings').read_text())['strings']
for lang in ('en', 'ar'):
    values = {key: value.get('localizations', {}).get(lang, {}).get('stringUnit', {}).get('value', key) for key, value in strings.items()}
    (target / f'assets/copy_{lang}.json').write_text(json.dumps(values, ensure_ascii=False, indent=2) + '\n')
assets = source / 'azkarify/Assets.xcassets'
icon = next((source / 'new-icon.icon/Assets').glob('*.svg'))
shutil.copyfile(icon, target / 'assets/app_icon.svg')
for name, file in [('JourneyCrescent','crescent_variation_3.svg'), ('MosaicCurrency','mosaic_variation_2.svg')]:
    shutil.copyfile(assets / f'{name}.imageset' / file, target / 'assets' / file)

# Extract labels and category references while retaining the iOS catalog as the source.
swift = (source / 'azkarify/Features/Journeys/JourneyCatalog.swift').read_text()
journeys = []
for name in ('morning','goingOut','evening','sleep'):
    block = swift.split(f'static let {name} = JourneyDefinition(',1)[1].split('\n  static let ',1)[0]
    header = re.search(r'id: "([^"]+)", title: .init\("([^"]+)", "([^"]+)"\),\s*subtitle: .init\("([^"]+)", "([^"]+)"\)',block)
    steps=[]
    # Balanced parenthesis parser keeps options and note metadata with their step.
    for match in re.finditer(r'JourneyStep\(',block):
        start=match.end(); depth=1; pos=start; quote=False; escape=False
        while depth:
            c=block[pos]
            if c=='"' and not escape: quote=not quote
            if not quote:
                if c=='(': depth+=1
                elif c==')': depth-=1
            escape=c=='\\' and not escape; pos+=1
        raw=block[start:pos-1]
        fields=re.match(r'\s*"([^"]+)", "([^"]+)", "([^"]+)"',raw)
        if not fields: raise ValueError(raw)
        cats=re.search(r'categories: \[([^]]*)\]',raw)
        note=re.search(r'note: .init\("([^"]+)", "([^"]+)"\)',raw)
        options=[dict(id=a,en=b,ar=c) for a,b,c in re.findall(r'.init\(id: "([^"]+)", title: .init\("([^"]+)", "([^"]+)"\)\)',raw)]
        steps.append(dict(id=fields[1],en=fields[2],ar=fields[3],categories=[int(x.strip()) for x in cats[1].split(',')] if cats else [],optional='optional: true' in raw,noteEn=note[1] if note else '',noteAr=note[2] if note else '',options=options))
    journeys.append(dict(id=header[1],en=header[2],ar=header[3],subtitleEn=header[4],subtitleAr=header[5],steps=steps))
(target / 'assets/journeys.json').write_text(json.dumps(journeys,ensure_ascii=False,indent=2)+'\n')
manifest={str(p.relative_to(target)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((target/'assets/content').rglob('*.json'))}
manifest['res/font/alan_sans.ttf']=hashlib.sha256((target/'res/font/alan_sans.ttf').read_bytes()).hexdigest()
(target/'assets/source_manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(f'Imported {len(manifest)-1} content files, Alan Sans, localized copy, artwork and {len(journeys)} routines.')

from android_localization import export
export()
