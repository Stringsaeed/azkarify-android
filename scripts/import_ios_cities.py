"""Import iOS's bundled prayer-city fallback into the Android asset bundle.

Run: python3 scripts/import_ios_cities.py ../azkarify
Use --check to fail if the committed Android copy differs from iOS.
"""

import argparse
import json
import re
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("ios_root", type=Path)
parser.add_argument("--check", action="store_true")
args = parser.parse_args()

source = args.ios_root / "azkarify/Data/PrayerCity.swift"
target = Path(__file__).resolve().parents[1] / "app/src/main/assets/prayer_cities.json"
pattern = re.compile(
    r'\.init\(id: "([^"]+)", arabicName: "([^"]+)", countryCode: "([A-Z]{2})", '
    r'latitude: (-?[\d.]+), longitude: (-?[\d.]+),\s*'
    r'timeZone: "([^"]+)", method: \.([A-Za-z]+)\)'
)
methods = {
    "dubai": "DUBAI",
    "egyptian": "EGYPTIAN",
    "ummAlQura": "UMM_AL_QURA",
    "qatar": "QATAR",
    "kuwait": "KUWAIT",
    "moonsightingCommittee": "MOON_SIGHTING_COMMITTEE",
    "singapore": "SINGAPORE",
}
cities = [
    {
        "name": name,
        "arabicName": arabic,
        "countryCode": country,
        "latitude": float(latitude),
        "longitude": float(longitude),
        "timeZone": time_zone,
        "method": methods[method],
    }
    for name, arabic, country, latitude, longitude, time_zone, method in pattern.findall(
        source.read_text()
    )
]
if not cities or len({(city["countryCode"], city["name"]) for city in cities}) != len(cities):
    raise ValueError("Could not import unique iOS fallback cities")
contents = json.dumps(cities, ensure_ascii=False, indent=2) + "\n"
if args.check:
    if target.read_text() != contents:
        raise SystemExit("Android fallback cities differ from iOS")
else:
    target.write_text(contents)
print(f"{len(cities)} iOS fallback cities match Android")
