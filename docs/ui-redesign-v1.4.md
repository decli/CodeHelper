# v1.4 界面重构实现说明

> 对应设计交付包 `docs/design/v1.4/`（方向 1a「小票进化」）与规范正本 `docs/ui-design-spec-v1.4.md`。
> 业务层（`SmsRepository`、`PickupCodeExtractor`、`BadgeNotifier`）零改动；`HomeViewModel` 只增加展示类设置与件数预览的读写入口。

## 1. 做了什么

| 优先级 | 改动 | 位置 |
| --- | --- | --- |
| P0 | 字阶新增 `HeroNumber` 104/900、`displaySmall` 30/800、`ButtonLarge` 20/700、`Caption` 15/600；删除 14sp 与全部 12sp 用法 | `ui/theme/Type.kt` |
| P0 | 英雄区去卡片化：104sp 数字直接落纸面，0 时转松绿，右侧 48dp 时间胶囊 | `ui/home/HomeScreen.kt` |
| P0 | 取件卡重构：删短信正文与「识别依据」；卡头改「发件方 · 时间」；一个 60dp 松绿主按钮 + 三个 48dp 文字按钮 | 同上 |
| P0 | 已取件卡降为一行摘要（对勾 + 灰码 + 恢复） | 同上 |
| P0 | 按驿站分组，「全部包裹」尾部追加「已取件」组 | `ui/home/HomeRows.kt` |
| P1 | 全屏出示页：临时提亮、防锁屏、最大 150sp、复制 / 播报 / 标记一步到位 | `ui/show/ShowCodeScreen.kt` |
| P1 | 语音播报：逐字读、`-` 读「杠」、语速 0.75×、再点一次停止 | `util/CodeSpeaker.kt`、`util/CodeSpeech.kt` |
| P1 | 首次启动引导 3 步，权限申请前先解释「为什么」 | `ui/onboarding/OnboardingScreen.kt` |
| P1 | 时间面板每档显示「N 件待取」 | `HomeScreen.TimeFilterSheet` + `HomeViewModel.refreshPendingCountPreview` |
| P1 | 设置页即改即存，去掉「保存并刷新」；新增「显示」分组（字号 / 外观 / 分组）；高级规则折叠 | `ui/settings/SettingsScreen.kt` |
| P2 | 顶栏「设置」改 48dp 圆形图标按钮；分段文案改「待取 / 全部包裹」 | `ui/home/HomeScreen.kt` |
| P2 | 提示条改造：17sp、圆角 18、撤销是 48dp 实心按钮、带撤销 6s / 纯提示 2.5s | `ui/CodeHelperApp.kt` |
| P2 | 补上规范手势表里的下拉刷新（此前只有换时间范围一条路径） | `ui/home/HomeScreen.kt` |

新增持久化字段（DataStore）：`onboardingDone`、`codeScale`、`groupBySender`、`themeMode`。
`PickupCodeItem` 新增派生属性 `senderShort`。

## 2. 与规范的偏差及理由

规范原文优先。以下 7 处在实施中发现冲突或明显副作用，已按「规范意图优先于规范字面」调整，并在此登记：

1. **取件码字号下限**。规范写 40–96sp，检查表第 9 条又要求「永不折行、不省略」。二者在极端长码（如 15 位混合码）上冲突。实现为：正常在 40–96sp 内二分实测；只有当 40sp 仍放不下时才继续下探到 22sp。宁可字小一点，也不能把取件码截断成另一个码。
2. **`senderShort` 截断**。规范写「≤6 字，否则截断」。纯数字 / 号码类发件方截断后会变成另一个号码（`1065502…` → `106550`），比过长更危险，因此号码类保持完整。
3. **分组键**。规范写 `groupBy { sender }`，实现按 `senderShort` 分组。同一家驿站换了发送号码时不会被拆成两组，分组标题也不会过长。
4. **「正在朗读：…」提示条未实现**。按钮本身已变成「正在读…」并让图标脉冲，底部再弹一条内容重复的提示条会遮住卡片。其余提示条文案全部按规范落地。
5. **提示条入场动画**沿用 `SnackbarHost` 自带的淡入缩放，没有换成「上移 24dp + 淡入」。接管 host 动画需要自绘整个提示条队列，收益低于回归风险。
6. **开关尺寸**。Material 3 `Switch` 固定 52×36 左右，未自绘 64×36；整行可点，触控目标仍远大于 48dp。
7. **高级规则输入落盘时机**。其余控件都是「点了就存」，但正则输入框如果每次按键都写 DataStore 会连带触发一次全量短信重扫，因此改为停手 700ms 后落盘（用户视角仍是「不用点保存」）。

另外两处规范未写死、由实现决定的地方：

- **加载态阈值**：规范写「700ms 内显示；<300ms 不显示」，实现取「加载超过 300ms 才显示加载卡」，避免快速切换时闪烁。
- **标记已取到后的停留位置**：在「全部包裹」里标记时停留在当前标签，卡片就地降为已取件摘要行，用户能看到东西「去了哪里」；在「待取」里标记则照常消失。

## 3. 升级兼容

- 老用户升级后不会看到首次引导：`HomeViewModel` 启动时若发现已授予短信权限，直接把 `onboardingDone` 置为 true。
- 已取件记录、识别提示词、高级规则、角标频率的存储键全部沿用 v1.3，升级不丢数据。
- 「恢复默认设置」现在会同时把显示设置（字号 / 外观 / 分组）恢复出厂值，但不清除已取件记录。

## 4. 验证

- **单元测试**（`./gradlew testReleaseUnitTest`，已接入 CI）：
  - `PickupCodeItemTest` — `senderShort` 的括号剥离、首段截取、号码保全、空值兜底
  - `CodeSpeechTest` — 逐字朗读、「杠」、多码分隔、读屏描述与播报口径一致
  - `HomeRowsTest` — 分组顺序、关闭分组、已取件尾组、各时间档件数统计
  - `UiFormatTest` — 时间范围文案
  - `PickupCodeExtractorTest` — 原有提取逻辑回归
- **发版前人工走查**：按 `docs/ui-design-spec-v1.4.md` 第 6 节 12 项检查表逐项打钩，并用 `docs/design/v1.4/usability_test_script.md` 做一次 5 分钟老年用户任务测试（找码 → 出示 → 标记 → 撤销 → 换时间范围）。
