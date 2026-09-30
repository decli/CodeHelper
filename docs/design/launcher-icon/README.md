# 启动图标

`1.4.4` 起使用方向 **A「字标 · 取」**：柿橙底上一个白色「取」（Noto Sans SC Black 的字形轮廓），单色主题图标和通知栏小图标也是这个字。`1.4.3` 用的是方向 C「包裹 · 已取到」，真机试用后改成 A。

旧图标（`previous/`）里有信封、气泡、三块码块、白色条框、绿色胶囊，缩到桌面大小后挤成一团；米黄底配藏青，也不是应用内「柿柿如意」的柿橙 / 松绿。换图标前比较过 5 个方向，都保留在这里。

打开 `index.html` 可以对比全部候选：三种遮罩、56 / 40 / 28px、浅色和深色壁纸、Android 13 主题图标、通知栏小图标，以及带「3」角标的桌面预览。

| | 方向 | 一句话 |
| --- | --- | --- |
| **A** | **字标 · 取（`1.4.4` 起采用）** | 柿橙底 + 白色「取」。最简单，但和淘宝同色系 |
| B | 小票 · 取件票 | 首页取件卡的缩影：取件码、撕票线、松绿「我已取到」、锯齿底边 |
| C | 包裹 · 已取到（`1.4.3` 用过） | 柿橙包裹 + 松绿对勾，直接用两个语义色 |
| D | 柿子 · 柿柿如意 | 柿子身上写「取」。轮廓独一份，不会和其他应用撞脸 |
| E | 深色 · 短信取件 | 深墨底，短信气泡里一个包裹。容易和系统「信息」混淆 |

## 共同约束

- 108dp 画布，主体落在直径 66dp 的安全圆内，圆形遮罩不裁切
- 右上角留给桌面数字角标（`BadgeNotifier` 发的待取数量）
- 只用 `ui/theme/Color.kt` 里已有的颜色
- 每个候选都带单色层（主题图标）和 24dp 通知栏小图标
- 全部是矢量图，不需要按屏幕密度导出 PNG

## 目录

- `candidates/<a-e>/`：与 `app/src/main/res/` 同结构的替换文件，含 `drawable/ic_launcher_foreground.xml`、`drawable/ic_launcher_monochrome.xml`、`drawable/ic_notification.xml` 和 `values/colors.xml`（`ic_launcher_background`）。`candidates/a/` 与 `app/src/main/res/` 中的对应文件逐字节相同
- `previous/`：`1.4.2` 及以前的旧图标，对比页的「现在的图标」从这里读取
- `preview/<a-e>.svg`：带背景色的整张 108dp 画布
- `tools/`：生成脚本。图形用 `skia-pathops` 做布尔运算，保证单色层的镂空在 Android 上和预览一致；「取」和「3-3」取自 Noto Sans SC Black（OFL）的字形轮廓

## 换成另一个方向

以 C 为例：

```sh
cp -r docs/design/launcher-icon/candidates/c/* app/src/main/res/
```

`mipmap-anydpi-v26/ic_launcher.xml` 和 `ic_launcher_round.xml` 引用的资源名不变，不用改。

## 重新生成

```sh
pip install fonttools brotli skia-pathops
npm pack @fontsource/noto-sans-sc@5.3.0 && tar -xzf fontsource-noto-sans-sc-5.3.0.tgz
cd docs/design/launcher-icon/tools
ICON_FONT_DIR=/path/to/package/files python3 export.py
ICON_FONT_DIR=/path/to/package/files python3 build_page.py template.html page.html   # 对比页正文，index.html 是它加上 <html> 外壳
```

改了 `designs.py` 里的 A 之后，重新导出，再把 `candidates/a/` 复制到 `app/src/main/res/`。
