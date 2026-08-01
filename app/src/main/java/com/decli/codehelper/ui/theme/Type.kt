package com.decli.codehelper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 适老化字阶：正文不低于 17sp，关键操作不低于 18sp，
// 首屏大数字与取件码使用超大号加粗等宽数字。
val CodeHelperTypography = Typography(
    // 首屏待取数量大数字
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 62.sp,
        fontFeatureSettings = "tnum",
    ),
    // 取件码基准字号（卡片内按长度自适应缩放）
    displayMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 50.sp,
        letterSpacing = 1.sp,
        fontFeatureSettings = "tnum",
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
