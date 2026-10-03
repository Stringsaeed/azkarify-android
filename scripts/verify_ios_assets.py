"""Verify that bundled devotional content and the source font match iOS exactly."""
import hashlib
import json
import sys
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=Path(sys.argv[1]).resolve()
manifest=json.loads((root/'app/src/main/assets/source_manifest.json').read_text())
for relative,expected in manifest.items():
    local=root/'app/src/main'/relative
    original=source/('azkarify/Fonts/AlanSans.ttf' if relative=='res/font/alan_sans.ttf' else relative.removeprefix('assets/'))
    for path in [local,original]:
        assert hashlib.sha256(path.read_bytes()).hexdigest()==expected,f'Asset drift: {path}'
for language in ['en','ar']:
    folder=root/'app/src/main/assets/content'/language
    categories=json.loads((folder/f'husn_{language}.json').read_text())['items']
    ids=[x['id'] for x in categories]
    assert len(ids)==len(set(ids))
    for category in categories:
        details=root/'app/src/main/assets/content'/category['detailUrl'].lstrip('/')
        entries=json.loads(details.read_text())['items']
        entry_ids=[x['id'] for x in entries]
        assert len(entry_ids)==len(set(entry_ids))
        assert all(x['repeat']>=1 and x['text']['arabic' if language=='ar' else 'translated'] for x in entries)
    print(f'{language}: {len(categories)} categories, all referenced entries valid')
print(f'{len(manifest)} checksums match the canonical iOS source.')
