"""Writes every candidate as drop-in Android resources plus a flat SVG preview.

    python3 export.py        # from this folder, with ICON_FONT_DIR pointing at the Noto Sans SC woff2 files
"""
import os
from designs import ICONS

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(text)


for ic in ICONS:
    res = os.path.join(ROOT, "candidates", ic.key)
    write(os.path.join(res, "drawable", "ic_launcher_foreground.xml"), ic.android_foreground())
    write(os.path.join(res, "drawable", "ic_launcher_monochrome.xml"), ic.android_monochrome())
    write(os.path.join(res, "drawable", "ic_notification.xml"), ic.android_notification())
    write(os.path.join(res, "values", "colors.xml"),
          f'<resources>\n    <color name="ic_launcher_background">{ic.bg.upper()}</color>\n</resources>\n')
    write(os.path.join(ROOT, "preview", f"{ic.key}.svg"), ic.svg(size=432) + "\n")
