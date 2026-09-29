"""Neutral stand-in apps for the home-screen mock (24-unit glyphs, drawn with iconkit)."""
import math
from iconkit import *


def gear():
    teeth = union(*[rotate(rrect(10.6, 2.6, 2.8, 5, 0.8), a, 12, 12) for a in range(0, 360, 45)])
    return diff(union(circle(12, 12, 7.2), teeth), circle(12, 12, 3))


def clock():
    ring = diff(circle(12, 12, 8.5), circle(12, 12, 6.6))
    hands = union(stroke(polyline((12, 12), (12, 7.4)), 1.8), stroke(polyline((12, 12), (15.2, 13.8)), 1.8))
    return union(ring, hands)


def calendar():
    frame = diff(rrect(4, 5, 16, 15, 2.5), rrect(5.8, 10, 12.4, 8.2, 1))
    dots = union(*[rrect(7.3 + c * 3.3, 11.4 + r * 3.3, 2.2, 2.2, 0.5) for r in range(2) for c in range(3)])
    pins = union(rrect(7.5, 3, 2, 4, 1), rrect(14.5, 3, 2, 4, 1))
    return union(frame, dots, pins)


def weather():
    sun = circle(9.5, 9.5, 4.2)
    cloud = union(circle(10.5, 15, 3.6), circle(15, 13, 4.4), rrect(7, 14.2, 13.8, 4.8, 2.4))
    return union(diff(sun, translate(cloud, -0.8, -0.8), cloud), cloud)


def photos():
    frame = diff(rrect(3.5, 5, 17, 14, 2.5), rrect(5.3, 6.8, 13.4, 10.4, 1))
    hills = inter(union(poly((5.3, 17.2), (10, 11), (13.5, 15), (15.5, 12.8), (18.7, 17.2))), rrect(5.3, 6.8, 13.4, 10.4, 1))
    return union(frame, hills, circle(15.3, 9.6, 1.5))


def folder():
    return union(rrect(3.5, 6, 8, 5, 1.5), rrect(3.5, 8, 17, 11, 2))


def calc():
    body = rrect(5.5, 3.5, 13, 17, 2.5)
    keys = union(rrect(7.5, 5.5, 9, 3.5, 0.8), *[rrect(7.5 + c * 3.4, 10.6 + r * 3.2, 2.2, 2.2, 0.5) for r in range(3) for c in range(3)])
    return diff(body, keys)


def music():
    return union(circle(8.5, 17, 3), rrect(10, 5, 2, 12.5, 0), poly((10, 5), (18, 3.5), (18, 7), (10, 8.5)), circle(15.5, 15.5, 3),
                 rrect(16.5, 4, 2, 11.8, 0))


def phone():
    return P("M20.01 15.38c-1.23 0-2.42-.2-3.53-.56-.35-.12-.74-.03-1.01.24l-1.57 1.97c-2.83-1.35-5.48-3.9-6.89-6.83l1.95-1.66"
             "c.27-.28.35-.67.24-1.02-.37-1.11-.56-2.3-.56-3.53 0-.54-.45-.99-.99-.99H4.19C3.65 3 3 3.24 3 3.99 3 13.28 10.73 21 20.01 21"
             "c.71 0 .99-.63.99-1.18v-3.45c0-.54-.45-.99-.99-.99z")


def sms():
    return union(rrect(3.5, 4.5, 17, 12.5, 4), poly((6.5, 15), (5.5, 20.5), (11, 16.5)))


def browser():
    ring = diff(circle(12, 12, 8.5), circle(12, 12, 6.8))
    merid = diff(ellipse(12, 12, 3.6, 8.5), ellipse(12, 12, 2.1, 7))
    lines = union(rrect(3.5, 11.2, 17, 1.6, 0), rrect(5, 7.4, 14, 1.4, 0), rrect(5, 15.2, 14, 1.4, 0))
    return inter(union(ring, merid, lines), circle(12, 12, 8.5))


def camera():
    body = union(rrect(3, 7, 18, 12.5, 2.5), rrect(8, 4.8, 8, 4, 1.4))
    return union(diff(body, circle(12, 13, 4.4)), circle(12, 13, 2.6))


APPS = {
    "clock": ("时钟", "#1F2328", clock()),
    "calendar": ("日历", "#E0483E", calendar()),
    "weather": ("天气", "#2E9BE0", weather()),
    "photos": ("相册", "#E8A21C", photos()),
    "files": ("文件管理", "#D9B21F", folder()),
    "calc": ("计算器", "#4A5160", calc()),
    "music": ("音乐", "#E0487F", music()),
    "phone": ("电话", "#2FA95A", phone()),
    "sms": ("信息", "#3A7FE0", sms()),
    "browser": ("浏览器", "#2C6BD8", browser()),
    "camera": ("相机", "#5B6270", camera()),
    "settings": ("设置", "#7A808C", gear()),
}
