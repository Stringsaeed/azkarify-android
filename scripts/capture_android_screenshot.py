"""Capture the emulator's primary HWC display without corrupting the PNG stream.

Usage: python3 scripts/capture_android_screenshot.py emulator-5558 output.png
The fold emulator has multiple displays; screencap's default selection prints a
warning before the binary PNG. Selecting the primary display avoids that warning.
"""
import argparse
from pathlib import Path
import re
import struct
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('serial')
parser.add_argument('output', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
sdk = next(line.split('=', 1)[1] for line in (root / 'local.properties').read_text().splitlines() if line.startswith('sdk.dir='))
adb = str(Path(sdk) / 'platform-tools/adb')
displays = subprocess.check_output([adb, '-s', args.serial, 'shell', 'dumpsys', 'SurfaceFlinger', '--display-id'], text=True)
match = re.search(r'Display (\d+) \(HWC display 0\)', displays)
command = [adb, '-s', args.serial, 'exec-out', 'screencap', '-p']
if match:
    command += ['-d', match[1]]
else:
    api = int(subprocess.check_output([adb, '-s', args.serial, 'shell', 'getprop', 'ro.build.version.sdk'], text=True).strip())
    if api >= 29:
        raise RuntimeError('Primary display was not reported by SurfaceFlinger')
png = subprocess.check_output(command)
if not png.startswith(b'\x89PNG\r\n\x1a\n'):
    raise RuntimeError('Screenshot did not return a PNG')
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_bytes(png)
width, height = struct.unpack('>II', png[16:24])
print(f'{args.serial}: {width} x {height} saved to {args.output}')
