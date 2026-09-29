from iconkit import *

HAN = "noto-sans-sc-117-900-normal.woff2"
LAT = "noto-sans-sc-latin-900-normal.woff2"

PERSIMMON = "#D2400E"
PERSIMMON_DEEP = "#A93307"
PERSIMMON_TINT = "#FBEADF"
PINE = "#1E7A3C"
INK = "#221D15"
PAPER = "#F5F2EC"
WHITE = "#FFFFFF"


def check_mark(cx, cy, s, w):
    """Rounded check centred on (cx, cy); s = half-width of the mark."""
    return stroke(polyline((cx - s, cy), (cx - s * 0.3, cy + s * 0.68), (cx + s, cy - s * 0.7)), w)


# ── A · 字标「取」 ────────────────────────────────────────────
def icon_a():
    ic = Icon("a", "字标 · 取", PERSIMMON)
    qu = fit(text_path("取", HAN), h=42, cx=54, cy=54)
    ic.add(WHITE, qu)
    ic.mono = qu
    ic.notif = notif_from(qu, pad=2)
    return ic


# ── B · 取件小票（首页卡片的缩影） ──────────────────────────────
def receipt_outline(x, y, w, h, r, teeth, depth):
    body = rrect(x, y, w, h, tl=r, tr=r, br=0, bl=0)
    step = w / teeth
    bites = [poly((x + i * step, y + h + 1), (x + (i + 0.5) * step, y + h - depth), (x + (i + 1) * step, y + h + 1))
             for i in range(teeth)]
    return diff(body, *bites)


def icon_b():
    ic = Icon("b", "小票 · 取件票", PERSIMMON)
    x, y, w, h = 34, 28, 40, 53
    paper = receipt_outline(x, y, w, h, 5, 5, 4.2)
    notch_y = 55.5
    notches = [circle(x, notch_y, 3.6), circle(x + w, notch_y, 3.6)]
    paper = diff(paper, *notches)
    code = fit(text_path("3-3", LAT, tracking=-0.02), w=28, cx=54, cy=43)
    dashes = union(*[rrect(39.2 + i * 6, notch_y - 0.9, 3.6, 1.8, 0.9) for i in range(5)])
    button = rrect(40, 62, 28, 9.5, 4.75)
    ic.add(WHITE, paper)
    ic.add(INK, code)
    ic.add("#E3DCCF", dashes)
    ic.add(PINE, button)
    ic.mono = diff(paper, code, dashes, button)
    ic.notif = notif_from(diff(paper, code, button), pad=1.5)
    return ic.scale(0.95)


# ── C · 包裹 + 对勾 ────────────────────────────────────────────
def box_parts(x, y, w, h, lid_h, overhang, r):
    lid = rrect(x - overhang, y, w + 2 * overhang, lid_h, r)
    body = rrect(x, y + lid_h - 1, w, h - lid_h + 1, tl=0, tr=0, br=r + 2, bl=r + 2)
    return lid, body


def icon_c():
    ic = Icon("c", "包裹 · 已取到", "#FFF3E8")
    lid, body = box_parts(31, 30, 44, 44, 12, 3, 3.5)
    tape = rrect(48, 30, 10, 26, 0, bl=1.5, br=1.5)
    shadow = rrect(31, 41, 44, 3.2, 0)  # lid shadow on body
    badge_ring = circle(70, 69, 16)
    badge = circle(70, 69, 12.5)
    tick = check_mark(70, 69.5, 6.2, 3.8)
    ic.add(PERSIMMON, body)
    ic.add(PERSIMMON_DEEP, inter(shadow, body))
    ic.add("#E8612A", lid)
    ic.add("#F29A6C", tape)
    ic.add("#FFF3E8", badge_ring)
    ic.add(PINE, badge)
    ic.add(WHITE, tick)
    seam = rrect(20, 41.2, 70, 2.4, 0)
    box = diff(union(lid, body), seam)
    ic.mono = union(diff(box, badge_ring), diff(badge, tick))
    ic.notif = notif_from(ic.mono, pad=1.5)
    return ic.scale(0.95)


# ── D · 柿柿如意（柿子吉祥物 + 取） ─────────────────────────────
def leaf(cx, cy, length, width, angle):
    # pointed leaf pointing along +x from the base (cx,cy)
    L, W = length, width
    p = P(f"M0,0 C{L * 0.25},{-W} {L * 0.75},{-W * 0.9} {L},0 C{L * 0.75},{W * 0.9} {L * 0.25},{W} 0,0 Z")
    return rotate(translate(p, cx, cy), angle, cx, cy)


def icon_d():
    ic = Icon("d", "柿子 · 柿柿如意", "#FFF1E3")
    body = superellipse(54, 60, 29, 24, 2.7)
    # little dip at the top where the calyx sits
    body = diff(body, ellipse(54, 34.5, 7, 3.2))
    shade = diff(body, translate(body, -3.5, -3.5))
    calyx_c = (54, 38.5)
    leaves = union(
        leaf(*calyx_c, 17, 6.2, 196),
        leaf(*calyx_c, 17, 6.2, -16),
        leaf(*calyx_c, 13, 5.2, 232),
        leaf(*calyx_c, 13, 5.2, -52),
    )
    stem = rrect(51.8, 29, 4.4, 9, 2.2)
    shine = ellipse(38, 49, 5.2, 3, -35)
    qu = fit(text_path("取", HAN), h=25, cx=54, cy=63.5)
    ic.add("#E4561B", body)
    ic.add("#C8440E", shade)
    ic.add("#F6A06C", shine)
    ic.add("#6B4226", stem)
    ic.add("#2E8B47", leaves)
    ic.add(WHITE, qu)
    ic.mono = union(diff(body, qu, stroke(leaves, 3.2)), leaves, stem)
    ic.notif = notif_from(ic.mono, pad=1)
    return ic


# ── E · 深色 · 短信里的包裹 ─────────────────────────────────────
def icon_e():
    ic = Icon("e", "深色 · 短信取件", INK)
    bubble = union(rrect(25, 27, 58, 45, 13), P("M33,66 L29,83 L49,70 Z"))
    lid, body = box_parts(40, 34, 28, 29, 8.5, 2.5, 2.5)
    tape = rrect(51.25, 34, 5.5, 16, 0, bl=1, br=1)
    ic.add(PAPER, bubble)
    ic.add(PERSIMMON, body)
    ic.add("#EC6A33", lid)
    ic.add("#F29A6C", tape)
    seam = rrect(30, 41.5, 50, 2, 0)
    ic.mono = union(diff(bubble, lid, body), inter(seam, union(lid, body)))
    ic.notif = notif_from(ic.mono, pad=1.5)
    return ic


ICONS = [icon_a(), icon_b(), icon_c(), icon_d(), icon_e()]
