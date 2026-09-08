package com.decli.codehelper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 适老化字阶 v1.4.2：在 v1.4 规范基础上整体上调一档。
// 真机走查（6.9″ / 430dp）发现原字阶在老人手里仍偏小，故：
// 正文最小 18sp，caption 最小 16sp，可操作文字 ≥19sp，数字一律 tnum。
// 同时把正文字重从 400 提到 500——深色模式下细笔画会发虚，加粗一档明显更清楚。

/**
 * 首页待取数量大数字：104sp，直接落在纸面上，不套卡片。
 *
 * 字重用 ExtraBold(800) 而不是 Black(900)：系统中文字体多数没有真实的 900 字面，
 * 请求 900 会触发合成加粗——笔画糊、字碗（0/3/8 的洞）被填小，深色模式下尤其明显，
 * 老人反而更难认。真机对比后确认 800 比 900 清楚。
 */
val HeroNumber = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 104.sp,
    lineHeight = 104.sp,
    letterSpacing = (-2).sp,
    fontFeatureSettings = "tnum",
)

/** 60–68dp 主按钮文字 */
val ButtonLarge = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 30.sp,
)

/** 全应用最小字号：只用于非关键说明，从不承载操作或状态 */
val Caption = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 24.sp,
)

val CodeHelperTypography = Typography(
    // 保留给需要超大数字但非首页英雄区的场景
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 72.sp,
        lineHeight = 78.sp,
        fontFeatureSettings = "tnum",
    ),
    // 取件码基准字号（实际字号按卡片宽度实测缩放，字间距按码长另行覆盖）
    // 字重同 HeroNumber，保持 v1.3.1 的 ExtraBold：900 会被合成加粗，笔画糊且字面更宽
    displayMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 54.sp,
        letterSpacing = 1.sp,
        fontFeatureSettings = "tnum",
    ),
    // 引导页 / 全屏状态标题
    displaySmall = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 42.sp,
    ),
    // 面板标题、空状态标题、关于弹窗标题
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    // 页面标题
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.5.sp,
    ),
    // 分段控件、设置项标题、面板选项
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    // 正文最小字号，字重 500：深色模式下 400 太细
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 28.sp,
    ),
    // 16sp 只允许 SemiBold 作 caption 用，禁止 400 字重正文
    bodyMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    // 文字按钮、胶囊、分组标题、卡头时间
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
    ),
)
