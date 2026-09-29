# 新启动图标候选（v1.5）

现在的图标（`app/src/main/res/drawable/ic_launcher_foreground.xml`）里有信封、气泡、三块码块、白色条框、绿色胶囊，缩到桌面大小后挤成一团；米黄底配藏青，也不是应用内「柿柿如意」的柿橙 / 松绿。这里是 5 个替换方向，**还没有接入应用**，选定后再替换 `res`。

打开 `index.html` 可以对比全部候选：三种遮罩、56 / 40 / 28px、浅色和深色壁纸、Android 13 主题图标、通知栏小图标，以及带「3」角标的桌面预览。

| | 方向 | 一句话 |
| --- | --- | --- |
| A | 字标 · 取 | 柿橙底 + 白色「取」。最简单，但和淘宝同色系 |
| B | 小票 · 取件票 | 首页取件卡的缩影：取件码、撕票线、松绿「我已取到」、锯齿底边 |
| C | 包裹 · 已取到 | 柿橙包裹 + 松绿对勾，直接用两个语义色 |
| D | 柿子 · 柿柿如意（推荐） | 柿子身上写「取」。轮廓独一份，不会和其他应用撞脸 |
| E | 深色 · 短信取件 | 深墨底，短信气泡里一个包裹。容易和系统「信息」混淆 |

## 共同约束

- 108dp 画布，主体落在直径 66dp 的安全圆内，圆形遮罩不裁切
- 右上角留给桌面数字角标（`BadgeNotifier` 发的待取数量）
- 只用 `ui/theme/Color.kt` 里已有的颜色
- 每个候选都带单色层（主题图标）和 24dp 通知栏小图标
- 全部是矢量图，不需要按屏幕密度导出 PNG

## 目录

- `candidates/<a-e>/`：与 `app/src/main/res/` 同结构的替换文件，含 `drawable/ic_launcher_foreground.xml`、`drawable/ic_launcher_monochrome.xml`、`drawable/ic_notification.xml` 和 `values/colors.xml`（`ic_launcher_background`）
- `preview/<a-e>.svg`：带背景色的整张 108dp 画布
- `tools/`：生成脚本。图形用 `skia-pathops` 做布尔运算，保证单色层的镂空在 Android 上和预览一致；「取」和「3-3」取自 Noto Sans SC Black（OFL）的字形轮廓

## 接入选定的方向

以 D 为例：

```sh
cp -r docs/design/icon-v1.5/candidates/d/* app/src/main/res/
```

`mipmap-anydpi-v26/ic_launcher.xml` 和 `ic_launcher_round.xml` 引用的资源名不变，不用改。

## 重新生成

```sh
pip install fonttools brotli skia-pathops
npm pack @fontsource/noto-sans-sc@5.3.0 && tar -xzf fontsource-noto-sans-sc-5.3.0.tgz
cd docs/design/icon-v1.5/tools
ICON_FONT_DIR=/path/to/package/files python3 export.py
ICON_FONT_DIR=/path/to/package/files python3 build_page.py template.html page.html   # 对比页正文，index.html 是它加上 <html> 外壳
```
