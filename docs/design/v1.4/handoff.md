> **阅读须知（工程侧补充）**
>
> 本文件是 **v1.4 的设计输入**（设计侧交付的原始 handoff），不是现行实现口径。
> 真机走查后有 3 处数值被推翻，**以 `docs/ui-design-spec-v1.4.md` 为准**：
>
> | 本文写的 | 实际实现 | 理由 |
> | --- | --- | --- |
> | 取件码 / 大数字字重 900 | **800** | 系统中文字体没有真实 900 字面，合成加粗使笔画糊、字碗被填小、字面变宽（实测字号反而更小） |
> | 页面左右 20dp | 首页列表 **16dp** | 这 8dp 直接换成取件码字号；其余页面仍 20dp |
> | 卡内文字按钮 48dp 横排 | **64dp 图标在上文字在下** | 三个并排横排放不下「读给我听」，1.3× 系统字号更放不下 |
>
> 其余差异与理由见 `docs/ui-redesign-v1.4.md` 第 2 / 2.5 / 2.6 节。

---

# Handoff: 取件码助手 v1.4 · 方向 1a「小票进化」

仓库：`decli/CodeHelper` @ `main` (b7df407, v1.3.1) · Kotlin + Jetpack Compose + Material 3 · minSdk 见 `gradle/libs.versions.toml`
目标机型：6.9″ 2608×1200（≈430×932dp @3x），Android 16。所有 dp 数值以 430dp 宽为基准，宽度用 `fillMaxWidth()`，不写死。

## 关于本包中的设计文件

`03 交互原型.dc.html` 与 `04 设计规范.dc.html` 是 **HTML 设计参考**（可点击原型 + 规范），不是可直接复用的代码。任务是在现有 Compose 代码库中按既有模式重建这些界面：沿用 `CodeHelperTheme` / `MaterialTheme.colorScheme` / `CodeHelperTypography`，不引入新 UI 框架。业务层（`HomeViewModel`、`SmsRepository`、`PickupCodeExtractor`、`BadgeNotifier`）**零改动**，除文末列出的 3 个新增字段。

## 保真度

**高保真**。颜色、字号、间距、圆角、按钮高度、文案均为最终值，按 1:1 实现。原型中「读给我听」用浏览器 TTS 模拟，真机用 Android `TextToSpeech`。

---

## 0. 改动总览（按优先级）

| # | 改动 | 涉及文件 | 工作量 |
|---|---|---|---|
| P0 | 字阶新增 3 档 + 删除 12sp 用法 | `ui/theme/Type.kt` | 0.5h |
| P0 | HeroCard 去卡片化（数字直接落纸面，104sp） | `ui/CodeHelperApp.kt` `HeroCard` | 1h |
| P0 | 取件卡重构：删短信正文/识别依据；单主按钮 + 3 个文字按钮；卡头「发件方 · 时间」 | `PickupCodeCardBody` | 3h |
| P0 | 已取件卡降为一行摘要 | `PickupCodeCardBody`（新增 `PickedUpRow`） | 1h |
| P0 | 按驿站分组（列表 UI 层 groupBy） | `HomeContent` | 1h |
| P1 | 全屏出示页 `ShowCodeScreen`（点码进入；提亮、防锁屏、复制、播报、标记） | 新文件 `ui/show/ShowCodeScreen.kt` | 3h |
| P1 | TTS 工具 `CodeSpeaker`（逐字朗读，`-` 读「杠」） | 新文件 `util/CodeSpeaker.kt` | 1.5h |
| P1 | 首次启动引导 `OnboardingScreen`（3 步） | 新文件 `ui/onboarding/OnboardingScreen.kt` + `SettingsRepository` 新字段 | 3h |
| P1 | 时间面板选项显示件数预览 | `TimeFilterSheet` / `TimeOptionRow` | 1h |
| P1 | 设置页：即改即存、去「保存并刷新」、新增「显示」分组、高级规则折叠 | `ui/settings/SettingsScreen.kt` | 4h |
| P2 | 顶栏「设置」改 48dp 圆形图标按钮；分段控件文案「待取 / 全部包裹」 | `HomeHeader` / `ListModeTabs` | 0.5h |
| P2 | 提示条改造（17sp、圆角 18、撤销为实心按钮） | `CodeHelperApp` SnackbarHost 自定义 | 1h |

---

## 1. 设计令牌

### 1.1 颜色 —— `Color.kt` **不变**，语义映射沿用 `Theme.kt`
| 用途 | 浅色 | 深色 | colorScheme 槽位 |
|---|---|---|---|
| 页面底 | `#F5F2EC` Paper | `#17130D` | `background` |
| 卡片 | `#FFFFFF` | `#221C13` | `surface` |
| 正文 | `#221D15` Ink | `#EFE8DB` | `onSurface` |
| 辅助字 | `#6E675A` | `#A79C8A` | `onSurfaceVariant` |
| 撕票线/分隔 | `#ECE6DB` | `#3A3223` | `outlineVariant` |
| 未选中描边 | `#8A8171` | `#5C523F` | `outline` |
| 柿橙（待办/选中/主数字） | `#D2400E` | `#FF8A50` | `primary` |
| 柿橙上文字 | `#FFFFFF` | `#3A1503` | `onPrimary` |
| 可点击文字/浅底强调字 | `#A93307` | `#FFB694` | `onPrimaryContainer` / `secondary` |
| 柿霜（胶囊底） | `#FBEADF` | `#3A2412` | `primaryContainer` |
| 未取件标签底 | `#FFE3D2` | `#3A2412` | `secondaryContainer` |
| 松绿（完成/主按钮） | `#1E7A3C` | `#6FC98A` | `tertiary` |
| 松绿上文字 | `#FFFFFF` | `#0A2913` | `onTertiary` |
| 已取件底 | `#E4F3E8` | `#1C2E1F` | `tertiaryContainer` |
| 填充按钮底（tonal） | `#F5F2EC` | `#2E2718` | `surfaceVariant` |
| 提示条 | 底 `#322F2A` 字 `#F5F2EC` | 底 `#EFE8DB` 字 `#17130D` | `inverseSurface` / `inverseOnSurface` |

规则：一屏最多一个柿橙实心块 + 一个松绿实心块；可点击文字一律 `onPrimaryContainer`；不用 alpha 弱化文字（删除现有 `.copy(alpha = 0.75f)` 与 `0.45f`）；深色模式去阴影改 1dp `outlineVariant` 描边。

### 1.2 字阶 —— `Type.kt` 改动
```kotlin
// 新增
val HeroNumber   = TextStyle(fontWeight = FontWeight.Black, fontSize = 104.sp, lineHeight = 104.sp, letterSpacing = (-2).sp, fontFeatureSettings = "tnum")
val DisplaySmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 40.sp)   // 引导页/全屏状态标题
val ButtonLarge  = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp)         // 60dp 主按钮
// 修改
displayMedium (取件码基准) → fontWeight = FontWeight.Black, letterSpacing = 2.sp   // 5 位以下码 letterSpacing 4.sp
headlineMedium 24sp → FontWeight.ExtraBold
bodyMedium 15sp → 仅允许 FontWeight.SemiBold 用作 caption；禁止 400 字重 15sp 正文
// 删除用法
labelMedium 14sp 与所有 12.sp 覆写（识别依据、supportingText）
```
取件码自适应：保留 `AutoSizeCodeLines` 的 TextMeasurer 二分实测，范围改为 **40–96sp**（`MIN_CODE_FONT_SP = 40`, `MAX_CODE_FONT_SP = 96`）；「特大」设置时 max ×1.15（=110）；出示页 max = 150。一码一行永不折行不变。

### 1.3 间距 / 圆角 / 层级
- 页面左右 **20dp**（现 16）；卡片间 16；卡内边 16–20；元素间 8/12；列表底部 contentPadding **120dp**（给提示条留位）。
- 圆角：卡片 24 · 面板/出示页 28 · 分段外框 20 / 内段 16 · 主按钮 18（60dp）/ 20（68dp）· 次级按钮 14–16 · 胶囊 CircleShape。
- 阴影：卡片 `shadowElevation = 2.dp`；浮层 `6.dp` + 40% 遮罩。深色模式 `shadowElevation = 0` + `border(1.dp, outlineVariant)`。
- 触控目标 ≥48dp；主按钮 60dp；出示页主按钮 68dp。

---

## 2. 屏幕逐个说明

### 2.1 首页 `HomeContent`
LazyColumn，`padding(horizontal = 20.dp)`，`contentPadding(top = 6.dp, bottom = 120.dp)`，`spacedBy(16.dp)`。

**顶栏 `HomeHeader`**：左「取件码助手」`titleLarge 20sp/700, letterSpacing .5sp`；右 48dp 圆形 `Surface(shape=CircleShape, color=surface, shadowElevation=2)` 内 `Icons.Rounded.Tune` 24dp，`contentDescription="设置"`。删除文字「设置」和 38dp 品牌方块。

**英雄区（替换 `HeroCard`，不再是 Surface）**：
```
Row(fillMaxWidth, SpaceBetween, Alignment.Bottom, padding(horizontal 2))
 ├ Column
 │  ├ Text(label, labelLarge 18sp/600, onSurfaceVariant)       // "待取包裹" | loading→"正在读取短信…" | 未授权→"等待授权"
 │  └ Row(alignByBaseline) Text("$pendingCount", HeroNumber, color = if(0) tertiary else primary)
 │                          Spacer(8) Text("个", 26sp/700, onSurface)
 └ RangeChip(paddingBottom 6dp)
```
`RangeChip`：高 **48dp**，`CircleShape`，`primaryContainer` 底，`border 2.dp primary`，padding start 16 / end 10，文字 `labelLarge 18sp/700 onPrimaryContainer`（`最近 12 小时`），`KeyboardArrowDown` 22dp。数字颜色变化 `animateColorAsState(300ms)`。

**分段控件 `ListModeTabs`**：外框 `RoundedCornerShape(20)`，padding 5，gap 5；段高 **56dp**，`RoundedCornerShape(16)`；选中 `primary` 底 + `Check` 22dp + 文字 `19sp/700 onPrimary`；未选中透明底 `onSurfaceVariant`。文案 **「待取」/「全部包裹」**。切换 200ms 底色过渡（`animateColorAsState`）。

**分组标题（新增）**：`Row(gap 10, padding horizontal 2)`：`Text(sender, 17sp/700 onSurfaceVariant, softWrap=false)` + `HorizontalDivider(weight 1, minWidth 16, outline @50%)` + `Text("N 件", 16sp/600)`。分组逻辑：`items.filter{!picked}.groupBy{sender}`（保持时间倒序）；「全部包裹」时尾部追加一组「已取件」。分组开关来自设置（默认开）；关闭时不显示标题。

**待取卡 `PickupCodeCardBody`（重写）**：`Surface(RoundedCornerShape(24), surface, shadow 2)`
1. 卡头 `Row(padding 16/20/6, SpaceBetween)`：左 `"$senderShort · $time"` 17sp/600 onSurfaceVariant，`softWrap=false`；右 StatusChip：`CircleShape secondaryContainer`，padding 6/12，**10dp 圆点 primary** + 「未取件」16sp/700 onSecondaryContainer（删除 Inventory2 图标；多码时不再写「· N 个码」——码本身逐行可见）。
2. 码区 `Column(padding 2/16/6, centerHorizontally, clickable → ShowCodeScreen, semantics "放大出示取件码 ${codes}")`：`AutoSizeCodeLines(40–96sp, Black, tnum, letterSpacing 2)`；下方 caption「点一下取件码可放大出示」16sp onSurfaceVariant。**删除**短信正文 `item.body` 与「识别依据」两段。
3. `TicketDivider` 高 20dp，虚线 2dp outlineVariant，两侧 10dp 半圆缺口（`background` 色）。
4. 动作区 `Column(padding 6/16/14, gap 4)`：
   - 主按钮 `Surface(height 60, RoundedCornerShape(18), tertiary)`：`Check` 24 + 「我已取到」`ButtonLarge onTertiary`；按下 `scale(.98)` 80ms。
   - 文字按钮行 `Row(height 48)` 三等分：`看短信`(ChatBubbleOutline) · `复制`(ContentCopy) · `读给我听`(VolumeUp)，图标 20 + 文字 17sp/600 `onPrimaryContainer`；`RoundedCornerShape(12)` ripple。播报中：图标 `infiniteRepeatable scale 1→1.25`，文字变「正在读…」。
   - 手势保留：左滑抽屉 132dp（松绿「我已取到」）、双击打开短信；`SwipeActionContainer` 不变。

**已取件行 `PickedUpRow`（新增，替代已取件大卡）**：`Surface(RoundedCornerShape(20), surface, shadow 1, padding 14/12/14/18)`, `Row(gap 14, centerVertically)`：
44dp 圆 `tertiaryContainer` + `Check` 26 tertiary · `Column(weight 1)`：码 `28sp/800 onSurfaceVariant tnum letterSpacing 1 maxLines 1 ellipsis`（多码用两个空格连接）+ `"已取 · $senderShort · $time"` 16sp · 右「恢复」文字按钮 48dp（`Undo` 20 + 17sp/700 onPrimaryContainer）。左滑抽屉「恢复未取」柿橙保留。

**加载态**：`hoursAgo` 切换或切到「全部」后 700ms 内显示；<300ms 不显示。卡片：48dp 环形 `CircularProgressIndicator(strokeWidth 5, primary, track primaryContainer)` + 「正在读取短信」22sp/700 + 「马上就好，只在这台手机上查找」17sp。英雄数字保留旧值，label 改「正在读取短信…」。

**空态 `StateCard`**：图标容器 **96dp / RoundedCornerShape(32)**，图标 52dp；标题 `26sp/800`；正文 `18sp/400 lineHeight 28`；按钮 60dp `surfaceVariant` 「看更早的短信」19sp/700。
- 待取空：TaskAlt 松绿，「包裹都取完了」/「{范围}内没有待取的包裹。想看更早的，可以换个时间范围。」
- 全部空：Inventory2 灰，「这段时间没有取件码」/「可以换个时间范围，或到「设置」检查识别提示词。」
- 未授权：VerifiedUser 柿橙，标题 `DisplaySmall`「需要允许读取短信」，主按钮 60dp primary「允许读取短信」。

### 2.2 时间范围面板 `TimeFilterSheet`
`ModalBottomSheet(shape top 28, surface)`，padding 20/0/20/20，gap 10。把手 44×5 `outline`。标题「选择时间范围」`headlineMedium 24/800`，副标「选好后立刻重新读取短信」17sp onSurfaceVariant。
`TimeOptionRow` 高 60，`RoundedCornerShape(18)`：选中 `primary` 底，左 28dp 白圆 + `Check` 18 primary，文字 `20sp/700 onPrimary`，**右侧新增件数** `"$n 件待取" / "无待取"` 16sp/600 onPrimary@85%（唯一允许的 alpha，仅装饰性）；未选中 `surfaceVariant` 底，28dp 圆 `border 2.5 outline`，文字 onSurface，件数 onSurfaceVariant。
件数计算：`ViewModel` 新增 `pendingCountByWindow: Map<CodeFilterWindow, Int>`（对当前已加载的 14 天数据本地统计即可；若仅加载了当前档，则对更大档显示 `—`）。「取消」56dp `surfaceVariant`。

### 2.3 全屏出示页 `ShowCodeScreen`（新）
全屏 `Surface(surface)`，padding 8/20/24。进入动画：`slideInVertically(24dp) + fadeIn`, 250ms。
- 顶部 `Row(height 56, SpaceBetween)`：「出示给驿站工作人员」18sp/700 onSurfaceVariant；右 48dp 圆 `surfaceVariant` + `Close` 26，`contentDescription="关闭"`。
- 中部 `Column(weight 1, center)`：`sender` 20sp/700；`"$time 到店"` 17sp onSurfaceVariant；spacer 18；「取件码」17sp/600 letterSpacing 8 onSurfaceVariant；`AutoSizeCodeLines(max 150sp, letterSpacing 3, width = 屏宽-40)`；胶囊「已临时调到最亮」15sp/600（`BrightnessAuto` 18）。
- 底部 `Column(gap 10)`：`Row(gap 10)` 两个 56dp `surfaceVariant` 按钮「复制」「读给我听」18sp/700 onSurface；主按钮 **68dp** `RoundedCornerShape(20) tertiary` 「我已取到」22sp/800 + Check 26 → 标记后关闭出示页并回首页显示撤销提示条。
- 系统：`window.attributes.screenBrightness = 1f` 进入时设置、退出恢复；`FLAG_KEEP_SCREEN_ON`；系统返回 = 关闭。

### 2.4 首次启动引导 `OnboardingScreen`（新）
仅首次（`SettingsRepository.onboardingDone: Boolean` 新增，DataStore）。`Column(padding 24/28/28)`。
- 顶部进度点：3 个 10dp 圆 primary，当前步宽 28dp（`animateDpAsState 250ms`），未到步 alpha .3。
- 中部 `Column(center, gap 18)`：132dp `RoundedCornerShape(44) primaryContainer` 图标容器 + 72dp 图标 primary；标题 `DisplaySmall 30/800`；描述 19sp/400 lineHeight 30 onSurfaceVariant maxWidth 330。
- 底部：主按钮 **64dp** `RoundedCornerShape(20) primary` 21sp/700；可选跳过文字按钮 56dp 18sp/600 onSurfaceVariant。

| 步 | 图标 | 标题 | 描述 | 主按钮 | 跳过 |
|---|---|---|---|---|---|
| 1 | Inventory2 | 取件码，一眼看到 | 自动从短信里找出取件码，用最大的字显示出来。不联网、不上传，只在这台手机上工作。 | 开始使用 | — |
| 2 | VerifiedUser | 需要允许读取短信 | 取件码都在短信里。接下来系统会弹出询问，请选择「允许」。我们不会发送或修改任何短信。 | 允许读取短信 → `READ_SMS` 请求 | 以后再说 |
| 3 | NotificationsActive | 桌面图标显示待取数量 | 开启通知后，桌面图标角标会显示还有几个包裹没取。不会打扰您，没有声音。 | 开启通知 → `POST_NOTIFICATIONS`（<33 直接下一步） | 暂不开启 |

第 1 步禁用系统返回；完成后 `onboardingDone = true` → 首页（若第 2 步跳过，首页显示未授权态）。

### 2.5 设置页 `SettingsScreen`（重构）
顶栏高 64：左「返回」48dp（`ArrowBack` 24 + 18sp/700 onPrimaryContainer，CircleShape ripple）；中「设置」22sp/700。内容 `padding 4/20/40, gap 16`。分组卡 `RoundedCornerShape(22) surface shadow 2, padding 18`，分组标题 16sp/700 onSurfaceVariant letterSpacing 1。**删除底部「保存并刷新」与错误汇总文字**；每项改动立即写入 DataStore 并触发 `viewModel.reload()`；页脚 caption「改动会自动保存，不需要再点「保存」」15sp 居中。

| 分组 | 控件 | 规格 | 数据 |
|---|---|---|---|
| 显示（新） | 取件码字号 标准/特大 | 行高 56；右侧分段：外框 `surfaceVariant` R14 padding 4；段 44dp R11；选中 primary/onPrimary，未选透明/onSurfaceVariant；17sp/700 | `codeScale` 新字段 |
| | 外观 浅色/深色 | 同上 | `themeMode`（默认跟随系统；提供 浅色/深色/跟随系统 三段亦可） |
| | 按驿站分组 | `Switch` 64×36，柿橙=开；标题 19sp/600 + 副标 15sp「同一家店的包裹放在一起」 | `groupBySender` 新字段 |
| 识别提示词 | 芯片开关 | 48dp 高 CircleShape；开：`secondaryContainer` 底 + Check 18 + 17sp/700 onSecondaryContainer；关：透明 + `border 2 outline` + onSurface；「＋ 新增」虚线描边芯片 → 大字号输入对话框（输入 `bodyLarge 19sp`，确定 56dp） | 沿用 `promptKeywords`；关闭 = 从列表移除但保留在模板中 |
| 桌面角标 | 5/10/15/30 分钟 | `Row(gap 8)` 四等分 52dp R14；选中 primary；未选 surfaceVariant；17sp/700 | `badgeRefreshMinutes`（去掉自由输入框；上限仍 120 可在高级里输） |
| | 通知权限 | 未授权：56dp `primaryContainer` 按钮「开启通知，角标才能显示」；已授权：CheckCircle 松绿 + 「通知已开启，角标正常显示」16sp/600 onTertiaryContainer | |
| 高级 | 高级规则（折叠） | 60dp 行「高级规则 / 给懂正则的家人用，一般不需要」+ 旋转箭头；展开显示现有 TextField（`bodyLarge`, monospace）、错误红字 `error`、「＋ 新增规则」48dp | `advancedRules` 逻辑不变；语法错误时该条不生效并红字提示，其它设置仍可用 |
| | 恢复默认设置 | 60dp NavRow，点击即恢复 + 提示条「已恢复默认设置」 | |
| | 关于取件码助手 | 60dp NavRow，右侧版本号 16sp onSurfaceVariant | 见 2.6 |

### 2.6 关于弹窗
自定义 `Dialog`（非 AlertDialog）：`Surface(RoundedCornerShape(28), surface, padding 28/24/20)`，居中列：72dp `RoundedCornerShape(22) primary` + Inventory2 40 onPrimary；「取件码助手」24sp/800；「版本 {versionName}\n不联网 · 不上传 · 只读取本机短信」17sp onSurfaceVariant 居中；按钮 56dp `primary`「知道了」19sp/700。

### 2.7 提示条（Snackbar 自定义外观）
`SnackbarHost` 自定义 `Snackbar` 内容：`Surface(RoundedCornerShape(18), inverseSurface, shadow 6)`，padding 10/10/10/18；文字 17sp/600 inverseOnSurface；含操作时右侧 **48dp 实心按钮** `RoundedCornerShape(12) primary`，`Undo` 20 + 「撤销」17sp/700 onPrimary。时长：带撤销 **6s**（`SnackbarDuration.Long` 改自定义 6000ms）；纯提示 2.5s。位置 bottom 20dp，左右 16dp。入场 `slideInVertically 24dp + fadeIn 250ms`。
文案：`「{codes 用、连接}」已标记为取到` / `「…」已恢复为待取` / `已复制 {codes}` / `正在朗读：{逐字}` / `已恢复默认设置` / `通知已开启`。

---

## 3. TTS `CodeSpeaker`（新，`util/CodeSpeaker.kt`）
```kotlin
class CodeSpeaker(context: Context) {
  private val tts = TextToSpeech(context) { if (it == SUCCESS) { tts.language = Locale.CHINA; tts.setSpeechRate(0.75f) } }
  fun textFor(item: PickupCodeItem): String {
    val spoken = item.codes.map { code -> code.map { ch -> if (ch == '-') "杠" else ch.toString() }.joinToString(" ") }
                           .joinToString("，下一个，")
    return "${item.senderShort}，取件码，$spoken"
  }
  fun speak(item, onDone) { tts.stop(); tts.speak(textFor(item), QUEUE_FLUSH, null, item.uniqueKey); setOnUtteranceProgressListener(onDone) }
  fun stop(); fun release() // Activity onDestroy
}
```
- 字母按字母读（A→"A"），不做整数读法；再点一次 = 停止；切屏/离开出示页 = 停止。
- 无 TTS 引擎：按钮变灰并 Toast「本机没有语音引擎」（不隐藏按钮）。
- TalkBack `contentDescription` 与此一致：`"取件码 3 杠 3 杠 0 6 0 0 6"`。

## 4. 数据层新增（`SettingsRepository` + `ExtractorSettings`/新 `DisplaySettings`）
```kotlin
onboardingDone: Boolean = false
codeScale: CodeScale = Standard   // enum { Standard, Large }
groupBySender: Boolean = true
themeMode: ThemeMode = System     // enum { System, Light, Dark }
```
`PickupCodeItem` 新增派生 `senderShort`：取 `sender` 去除【】后按「 · 」/ 空格 / 「，」切分的首段（如「菜鸟驿站」「兔喜快递」），≤6 字，否则截断。

## 5. 交互与状态速查
- 首页四态：未授权 / 读取中 / 空 / 有数据（见 2.1）。已取件仅在「全部包裹」尾部。
- 「我已取到」：卡片 `AnimatedVisibility shrinkVertically 200ms` → 数字减少 → 撤销提示条 6s。不弹确认框。
- 每个手势都有按钮替身：左滑↔卡内按钮；双击↔看短信；下拉刷新↔换时间范围自动刷新。
- 动效上限：入场 250ms ease-out；底色切换 200ms；按下 scale .98 80ms；`Settings.Global.ANIMATOR_DURATION_SCALE == 0` 时全部即时。
- 无障碍验收：见 `04 设计规范.dc.html` 第 06 节 12 项检查表，发版前逐项打钩。

## 6. 资产
仅 Material Icons Rounded（已在依赖中）：Tune, Check, KeyboardArrowDown, ChatBubbleOutline, ContentCopy, VolumeUp, Undo, TaskAlt, Inventory2, VerifiedUser, NotificationsActive, ArrowBack, Close, ExpandMore, ChevronRight, BrightnessAuto, CheckCircle。无图片资产。

## 7. 文件
- `03 交互原型.dc.html` — 可点击原型（浏览器打开；右上 Tweaks 切换 浅/深色、标准/特大、分组、起始页）。旁注面板可跳到任意场景。
- `04 设计规范.dc.html` — 色彩/字阶/间距/组件/交互/无障碍检查表。
- `02 视觉方向对比.dc.html` — 已定稿 1a；1b/1c 仅存档。
- `01 现状复刻.dc.html` — v1.3.1 现状对照。
- `android-frame.jsx` — 原型用手机外框（非交付物）。
- `usability_test_script.md` — 老年用户 5 分钟任务测试脚本。
