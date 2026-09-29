"""Builds the icon-candidate comparison page from the same geometry that produces the Android resources."""
import os
import re
import sys
from iconkit import Icon, P, to_d
from designs import ICONS
from placeholders import APPS

# the icon that shipped before 1.4.3, kept for the "现在的图标" comparison
RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../previous/")


def current_icon():
    ic = Icon("now", "现在的图标", "#F2DEB6")
    xml = open(RES + "drawable/ic_launcher_foreground.xml").read()
    for color, d in re.findall(r'fillColor="(#[0-9A-Fa-f]+)"\s+android:pathData="([^"]+)"', xml):
        ic.add(color, P(d))
    mono = open(RES + "drawable/ic_launcher_monochrome.xml").read()
    # the shipped mono layer paints white shapes over a dark silhouette; show just the silhouette
    ic.mono = P(re.findall(r'pathData="([^"]+)"', mono)[0])
    notif = open(RES + "drawable/ic_notification.xml").read()
    ic.notif = P(re.findall(r'pathData="([^"]+)"', notif)[0])
    return ic


NOW = current_icon()
ALL = ICONS + [NOW]

META = {
    "a": dict(
        title="字标 · 取",
        idea="一个字就是名字。柿橙底上只放一个白色「取」，老人在桌面上找的就是「取快递的那个」。",
        good="最简单、字最大。缩到最小也认得出，单色主题图标和通知栏图标直接成立。",
        watch="橙底白字和淘宝图标同一个色系，两个图标在同一屏时，老人可能看混。",
        colors=[("底", "#D2400E"), ("字", "#FFFFFF")],
    ),
    "b": dict(
        title="小票 · 取件票",
        idea="首页取件卡的缩影：上面是取件码，中间是撕票线和两侧打孔，下面是松绿的「我已取到」按钮，底边是小票的锯齿。",
        good="图标里的样子就是打开应用看到的样子。「3-3」这种码是老人在驿站天天见的格式。",
        watch="元素比 A 多一些。小尺寸下撕票线的虚线会变淡，但只是装饰，不影响认出这是一张票。",
        colors=[("底", "#D2400E"), ("票", "#FFFFFF"), ("码", "#221D15"), ("按钮", "#1E7A3C")],
    ),
    "c": dict(
        title="包裹 · 已取到",
        idea="柿橙包裹配松绿对勾，直接用应用里的两个语义色：柿橙是待取，松绿是已取到。",
        good="「快递」最通用的符号，不认字也看得懂。浅底放在深色壁纸上很醒目，对勾在右下角，不会挡住右上角的数字角标。",
        watch="包裹加对勾是快递类应用的常见组合，形状本身不独特，主要靠配色区分。",
        colors=[("底", "#FFF3E8"), ("盖", "#E8612A"), ("箱", "#D2400E"), ("对勾", "#1E7A3C")],
    ),
    "d": dict(
        title="柿子 · 柿柿如意",
        idea="应用主题叫「柿柿如意」，图标就是一个柿子，身上写着「取」。",
        good="轮廓独一份：圆柿子加绿柿蒂，桌面上不会和任何快递或购物应用撞脸。「取」字保证一眼知道是干什么的，配色和应用内同源。",
        watch="细节最多（高光、阴影、柿蒂），但主体色块大，缩到 28px 仍能认出柿子和「取」。",
        colors=[("底", "#FFF1E3"), ("柿子", "#E4561B"), ("柿蒂", "#2E8B47"), ("字", "#FFFFFF")],
        rec=True,
    ),
    "e": dict(
        title="深色 · 短信取件",
        idea="深墨底上一个短信气泡，气泡里是一个包裹：从短信里找包裹。",
        good="满屏亮色图标里，深色底反而最显眼。气泡交代了取件码的来源是短信。",
        watch="气泡容易让人联想到系统的「信息」应用，老人可能点错。",
        colors=[("底", "#221D15"), ("气泡", "#F5F2EC"), ("箱", "#D2400E")],
    ),
}

VIS = "18 18 72 72"  # the part of the 108dp canvas a launcher actually shows


def use(kind, key, view=VIS, cls=""):
    c = f' class="{cls}"' if cls else ""
    return f'<svg viewBox="{view}"{c} aria-hidden="true"><use href="#{kind}-{key}"/></svg>'


def defs():
    out = []
    for ic in ALL:
        out.append(f'<g id="art-{ic.key}"><rect width="108" height="108" fill="{ic.bg}"/>'
                   + "".join(f'<path fill="{c}" d="{to_d(p)}"/>' for c, p in ic.layers) + "</g>")
        out.append(f'<path id="mono-{ic.key}" fill="currentColor" d="{to_d(ic.mono)}"/>')
        out.append(f'<path id="notif-{ic.key}" fill="currentColor" d="{to_d(ic.notif)}"/>')
    for k, (_, _, g) in APPS.items():
        out.append(f'<path id="app-{k}" fill="currentColor" d="{to_d(g)}"/>')
    return "".join(out)


def tile(key, shape, size, label=None):
    cap = f"<figcaption>{label}</figcaption>" if label else ""
    return f'<figure class="t"><div class="ic {shape}" style="--s:{size}px">{use("art", key)}</div>{cap}</figure>'


def card(ic):
    m = META[ic.key]
    k = ic.key
    rec = '<span class="rec">推荐</span>' if m.get("rec") else ""
    sizes = "".join(f'<div class="ic circle" style="--s:{s}px">{use("art", k)}</div>' for s in (56, 40, 28))
    swatches = "".join(f'<li><i style="background:{c}"></i>{n} <code>{c}</code></li>' for n, c in m["colors"])
    return f"""
<article class="cand" id="cand-{k}" aria-labelledby="h-{k}">
  <header class="cand-head">
    <span class="letter">{k.upper()}</span>
    <h2 id="h-{k}">{m['title']}</h2>{rec}
    <button type="button" class="try" data-pick="{k}">放到桌面上看</button>
  </header>
  <div class="spec">
    <div class="ic circle hero" style="--s:132px">{use('art', k)}</div>
    <div class="shapes">
      {tile(k, 'circle', 64, '圆形')}{tile(k, 'squircle', 64, '超椭圆')}{tile(k, 'rounded', 64, '圆角方')}
    </div>
  </div>
  <div class="strips">
    <div class="strip wall-light" role="img" aria-label="浅色壁纸上 56、40、28 像素">{sizes}</div>
    <div class="strip wall-dark" role="img" aria-label="深色壁纸上 56、40、28 像素">{sizes}</div>
    <div class="strip themed-light" role="img" aria-label="主题图标（浅色）"><div class="ic circle mono" style="--s:48px">{use('mono', k)}</div><span>主题图标</span></div>
    <div class="strip themed-dark" role="img" aria-label="主题图标（深色）"><div class="ic circle mono" style="--s:48px">{use('mono', k)}</div><span>主题图标</span></div>
    <div class="strip notif" role="img" aria-label="通知栏小图标">
      <svg viewBox="0 0 24 24" class="n" aria-hidden="true"><use href="#notif-{k}"/></svg>
      <div><b>还有 3 个未取件码</b><span>打开取件码助手查看</span></div>
    </div>
  </div>
  <dl class="notes">
    <div><dt>想法</dt><dd>{m['idea']}</dd></div>
    <div><dt>好在</dt><dd>{m['good']}</dd></div>
    <div><dt>要留意</dt><dd>{m['watch']}</dd></div>
  </dl>
  <ul class="swatches">{swatches}</ul>
</article>"""


def app_cell(k):
    name, color, _ = APPS[k]
    return (f'<div class="app"><div class="ic tile" style="--tile:{color}">'
            f'<svg viewBox="0 0 24 24" aria-hidden="true"><use href="#app-{k}"/></svg></div><span>{name}</span></div>')


def me_cell():
    return ('<div class="app me"><div class="icwrap"><div class="ic mine">'
            f'<svg viewBox="{VIS}" aria-hidden="true"><use id="me-use" href="#art-d"/></svg></div>'
            '<span class="badge" aria-label="3 个待取">3</span></div><span>取件码助手</span></div>')


def radios(name, legend, options, checked):
    items = "".join(
        f'<input type="radio" name="{name}" id="{name}-{v}" value="{v}"{" checked" if v == checked else ""}>'
        f'<label for="{name}-{v}">{t}</label>' for v, t in options)
    return f'<fieldset><legend>{legend}</legend><div class="chips">{items}</div></fieldset>'


page = open(sys.argv[1]).read()
page = page.replace("{{DEFS}}", defs())
page = page.replace("{{NOW}}", f'<div class="ic squircle" style="--s:64px">{use("art", "now")}</div>')
page = page.replace("{{CARDS}}", "".join(card(ic) for ic in ICONS))
grid = [app_cell(k) for k in ("clock", "calendar", "weather", "photos", "files", "calc")] + [me_cell(), app_cell("music")]
page = page.replace("{{APPS}}", "".join(grid))
page = page.replace("{{DOCK}}", "".join(app_cell(k) for k in ("phone", "sms", "browser", "camera")))
page = page.replace("{{CONTROLS}}", "".join([
    radios("pick", "图标", [("a", "A"), ("b", "B"), ("c", "C"), ("d", "D"), ("e", "E"), ("now", "现在")], "d"),
    radios("mask", "遮罩", [("circle", "圆形"), ("squircle", "超椭圆"), ("rounded", "圆角方")], "circle"),
    radios("wall", "壁纸", [("light", "浅色"), ("dark", "深色")], "light"),
    radios("themed", "主题图标", [("off", "关"), ("on", "开")], "off"),
]))
open(sys.argv[2], "w").write(page)
