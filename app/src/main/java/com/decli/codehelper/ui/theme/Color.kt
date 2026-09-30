package com.decli.codehelper.ui.theme

import androidx.compose.ui.graphics.Color

// ── 「柿柿如意」主题 · 浅色令牌 ──
// 柿橙 = 待办 / 主操作，松绿 = 已完成，全应用只使用这一套语义色。

/** 页面底色 · 暖纸 */
val Paper = Color(0xFFF5F2EC)

/** 卡片底色 · 纯白 */
val CardSurface = Color(0xFFFFFFFF)

/** 正文 · 浓墨 */
val Ink = Color(0xFF221D15)

/** 辅助文字 */
val InkMuted = Color(0xFF6E675A)

/** 装饰性分隔线 / 小票撕边（浅） */
val Outline = Color(0xFFECE6DB)

/** 单选圈、开关等小图形的轮廓（控件本身不描边，见 [ControlFill]） */
val OutlineStrong = Color(0xFF8A8171)

/**
 * 次要控件底色：时间范围、设置按钮、看短信 / 复制、分段控件轨道、未选中的选项。
 * 在暖纸上明度差 7、在白卡上明度差 12，不描边也看得出是一块可点的东西；浓墨字 12.4:1。
 */
val ControlFill = Color(0xFFE5DDCE)

/** 主色 · 柿橙（待办状态、主按钮） */
val Persimmon = Color(0xFFD2400E)

/** 深柿 · 浅橙底上的强调文字 */
val PersimmonDeep = Color(0xFFA93307)

/** 柿霜 · 主色浅底（选中态、提示条） */
val PersimmonTint = Color(0xFFFBEADF)

/** 状态徽标浅底 */
val PersimmonChip = Color(0xFFFFE3D2)

/** 完成色 · 松绿（已取件、确认按钮） */
val Pine = Color(0xFF1E7A3C)

/** 深松绿 · 浅绿底上的文字 */
val PineDeep = Color(0xFF166030)

/** 完成色浅底 */
val PineTint = Color(0xFFE4F3E8)

/** 表单错误色 */
val ErrorRed = Color(0xFFB3261E)
val ErrorRedTint = Color(0xFFF9DEDC)

// ── 深色令牌 ──
val DarkPaper = Color(0xFF17130D)
val DarkCardSurface = Color(0xFF221C13)
val DarkInk = Color(0xFFEFE8DB)
val DarkInkMuted = Color(0xFFA79C8A)
val DarkOutline = Color(0xFF3A3223)
val DarkOutlineStrong = Color(0xFF5C523F)
val DarkSurfaceMuted = Color(0xFF2E2718)
/** 深色控件底：在深纸上明度差 13、在深卡上明度差 8 */
val DarkControlFill = Color(0xFF352D20)
val PersimmonBright = Color(0xFFFF8A50)
val PersimmonOnBright = Color(0xFF3A1503)
val DarkPersimmonTint = Color(0xFF3A2412)
val PersimmonTintBright = Color(0xFFFFB694)
val PineBright = Color(0xFF6FC98A)
val PineOnBright = Color(0xFF0A2913)
val DarkPineTint = Color(0xFF1C2E1F)
val PineTintBright = Color(0xFFA8E0B5)

/** 提示条底色 · 浅色模式为深底浅字 */
val InverseSurface = Color(0xFF322F2A)
val InverseOnSurface = Color(0xFFF5F2EC)

/** 提示条底色 · 深色模式反相为浅底深字，保证从深色卡片中跳出 */
val DarkInverseSurface = Color(0xFFEFE8DB)
val DarkInverseOnSurface = Color(0xFF17130D)
