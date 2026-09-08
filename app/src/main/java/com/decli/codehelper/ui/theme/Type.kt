package com.decli.codehelper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 适老化字阶（v1.4 规范 02 节）：
// 正文最小 17sp，caption 最小 15sp/600，可操作文字 ≥17sp，数字一律 tnum。
// 全应用不再出现 14sp 与 12sp 文字。

/** 首页待取数量大数字：104sp / 900，直接落在纸面上，不套卡片 */
val HeroNumber = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 104.sp,
    lineHeight = 104.sp,
    letterSpacing = (-2).sp,
    fontFeatureSettings = "tnum",
)

/** 60–68dp 主按钮文字 */
val ButtonLarge = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 28.sp,
)

/** 全应用最小字号：只用于非关键说明，从不承载操作或状态 */
val Caption = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 22.sp,
)

val CodeHelperTypography = Typography(
    // 保留给需要超大数字但非首页英雄区的场景
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 72.sp,
        lineHeight = 78.sp,
        fontFeatureSettings = "tnum",
    ),
    // 取件码基准字号（按卡片宽度实测缩放，字间距按码长另行覆盖）
    displayMedium = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 50.sp,
        letterSpacing = 2.sp,
        fontFeatureSettings = "tnum",
    ),
    // 引导页 / 全屏状态标题
    displaySmall = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 40.sp,
    ),
    // 面板标题、空状态标题、关于弹窗标题
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    // 页面标题
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp,
    ),
    // 分段控件、设置项标题、面板选项
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
    ),
    // 正文最小字号
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    // 15sp 只允许 SemiBold 作 caption 用，禁止 400 字重 15sp 正文
    bodyMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    // 文字按钮、胶囊、分组标题、卡头时间
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
)
