"""Tiny toolkit: build icon shapes as boolean-clean paths, emit SVG + Android vector XML."""
import math
import os
import pathops
from fontTools.ttLib import TTFont
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.svgLib.path import parse_path

# Noto Sans SC (OFL) woff2 subsets from the @fontsource/noto-sans-sc npm package:
#   npm pack @fontsource/noto-sans-sc@5.3.0 && tar -xzf fontsource-noto-sans-sc-5.3.0.tgz
FONT_DIR = os.environ.get("ICON_FONT_DIR", "package/files/").rstrip("/") + "/"


# ---------- path construction ----------
def P(d):
    p = pathops.Path()
    parse_path(d, p.getPen())
    return p


def xform(path, t):
    out = pathops.Path()
    path.draw(TransformPen(out.getPen(), t))
    return out


def translate(path, dx, dy):
    return xform(path, (1, 0, 0, 1, dx, dy))


def rotate(path, deg, cx, cy):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    # translate(-cx,-cy) -> rotate -> translate(cx,cy)
    return xform(path, (c, s, -s, c, cx - c * cx + s * cy, cy - s * cx - c * cy))


def _op(a, b, op):
    return pathops.op(a, b, op, fix_winding=True)


def union(*ps):
    out = pathops.Path()
    for p in ps:
        out = _op(out, p, pathops.PathOp.UNION)
    return out


def diff(a, *bs):
    for b in bs:
        a = _op(a, b, pathops.PathOp.DIFFERENCE)
    return a


def inter(a, b):
    return _op(a, b, pathops.PathOp.INTERSECTION)


def rrect(x, y, w, h, r=0, tl=None, tr=None, br=None, bl=None):
    tl = r if tl is None else tl
    tr = r if tr is None else tr
    br = r if br is None else br
    bl = r if bl is None else bl
    d = f"M{x + tl},{y} H{x + w - tr} "
    d += f"A{tr},{tr} 0 0 1 {x + w},{y + tr} " if tr else ""
    d += f"V{y + h - br} "
    d += f"A{br},{br} 0 0 1 {x + w - br},{y + h} " if br else ""
    d += f"H{x + bl} "
    d += f"A{bl},{bl} 0 0 1 {x},{y + h - bl} " if bl else ""
    d += f"V{y + tl} "
    d += f"A{tl},{tl} 0 0 1 {x + tl},{y} " if tl else ""
    d += "Z"
    return P(d)


def circle(cx, cy, r):
    return P(f"M{cx - r},{cy} A{r},{r} 0 1 0 {cx + r},{cy} A{r},{r} 0 1 0 {cx - r},{cy} Z")


def ellipse(cx, cy, rx, ry, rot=0):
    p = P(f"M{cx - rx},{cy} A{rx},{ry} 0 1 0 {cx + rx},{cy} A{rx},{ry} 0 1 0 {cx - rx},{cy} Z")
    return rotate(p, rot, cx, cy) if rot else p


def poly(*pts):
    d = "M" + " L".join(f"{x},{y}" for x, y in pts) + " Z"
    return P(d)


def superellipse(cx, cy, a, b, n, steps=160):
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        c, s = math.cos(t), math.sin(t)
        x = a * math.copysign(abs(c) ** (2 / n), c)
        y = b * math.copysign(abs(s) ** (2 / n), s)
        pts.append((cx + x, cy + y))
    return poly(*pts)


def stroke(path, width, cap="round", join="round"):
    caps = {"butt": pathops.LineCap.BUTT_CAP, "round": pathops.LineCap.ROUND_CAP, "square": pathops.LineCap.SQUARE_CAP}
    joins = {"miter": pathops.LineJoin.MITER_JOIN, "round": pathops.LineJoin.ROUND_JOIN, "bevel": pathops.LineJoin.BEVEL_JOIN}
    p = pathops.Path(path)
    p.stroke(width, caps[cap], joins[join], 4)
    p.convertConicsToQuads()
    return union(p)


def polyline(*pts):
    return P("M" + " L".join(f"{x},{y}" for x, y in pts))


# ---------- glyphs ----------
_fonts = {}


def _font(file):
    if file not in _fonts:
        f = TTFont(FONT_DIR + file)
        _fonts[file] = (f, f.getGlyphSet(), f.getBestCmap())
    return _fonts[file]


def text_path(text, file, tracking=0.0):
    """Glyph outlines in font units, y flipped (y grows downward)."""
    f, gs, cmap = _font(file)
    out = pathops.Path()
    x = 0
    for ch in text:
        g = gs[cmap[ord(ch)]]
        tmp = pathops.Path()
        g.draw(TransformPen(tmp.getPen(), (1, 0, 0, -1, x, 0)))
        out = union(out, tmp)
        x += g.width + tracking * f["head"].unitsPerEm
    return out


def fit(path, x=None, y=None, w=None, h=None, cx=None, cy=None):
    """Scale uniformly to width w or height h, then place by top-left or center."""
    l, t, r, b = path.bounds
    s = (w / (r - l)) if w is not None else (h / (b - t))
    p = xform(path, (s, 0, 0, s, 0, 0))
    l, t, r, b = p.bounds
    dx = (cx - (l + r) / 2) if cx is not None else (x - l)
    dy = (cy - (t + b) / 2) if cy is not None else (y - t)
    return translate(p, dx, dy)


# ---------- output ----------
def _num(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def to_d(path):
    pen = SVGPathPen(None, ntos=_num)
    path.draw(pen)
    return pen.getCommands()


class Icon:
    def __init__(self, key, name, bg):
        self.key, self.name, self.bg = key, name, bg
        self.layers = []  # (color, path)
        self.mono = None
        self.notif = None

    def scale(self, s, cx=54, cy=54):
        t = (s, 0, 0, s, cx - s * cx, cy - s * cy)
        self.layers = [(c, xform(p, t)) for c, p in self.layers]
        self.mono = xform(self.mono, t)
        return self

    def add(self, color, path):
        self.layers.append((color, path))
        return path

    # full 108x108 artwork incl. background (for previews)
    def svg(self, view="0 0 108 108", with_bg=True, size=None):
        attrs = f' width="{size}" height="{size}"' if size else ""
        body = f'<rect x="0" y="0" width="108" height="108" fill="{self.bg}"/>' if with_bg else ""
        body += "".join(f'<path fill="{c}" d="{to_d(p)}"/>' for c, p in self.layers)
        return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view}"{attrs}>{body}</svg>'

    def mono_svg(self, fg="#000", view="0 0 108 108"):
        return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view}"><path fill="{fg}" d="{to_d(self.mono)}"/></svg>'

    def notif_svg(self, fg="#000"):
        return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="{fg}" d="{to_d(self.notif)}"/></svg>'

    @staticmethod
    def _vector(paths, size, viewport):
        head = (
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{size}dp"\n    android:height="{size}dp"\n'
            f'    android:viewportWidth="{viewport}"\n    android:viewportHeight="{viewport}">\n'
        )
        body = "".join(
            f'    <path\n        android:fillColor="{c}"\n        android:pathData="{to_d(p)}" />\n' for c, p in paths
        )
        return head + body + "</vector>\n"

    def android_foreground(self):
        return self._vector([(c.upper(), p) for c, p in self.layers], 108, 108)

    def android_monochrome(self):
        return self._vector([("#FFFFFFFF", self.mono)], 108, 108)

    def android_notification(self):
        return self._vector([("#FFFFFFFF", self.notif)], 24, 24)


def notif_from(mono, pad=1.5):
    """Scale a 108-space silhouette into the 24dp notification grid."""
    l, t, r, b = mono.bounds
    side = 24 - 2 * pad
    s = side / max(r - l, b - t)
    p = xform(mono, (s, 0, 0, s, 0, 0))
    l, t, r, b = p.bounds
    return translate(p, 12 - (l + r) / 2, 12 - (t + b) / 2)
