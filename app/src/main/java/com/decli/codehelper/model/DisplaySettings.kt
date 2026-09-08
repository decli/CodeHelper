package com.decli.codehelper.model

/** 取件码字号档位：影响卡片与出示页的自适应字号上限 */
enum class CodeScale(
    val label: String,
    val multiplier: Float,
) {
    Standard(label = "标准", multiplier = 1f),
    Large(label = "特大", multiplier = 1.15f),
}

/** 外观：默认跟随系统，也允许老人手动锁定浅色 / 深色 */
enum class ThemeMode(
    val label: String,
) {
    System(label = "跟随系统"),
    Light(label = "浅色"),
    Dark(label = "深色"),
}

/**
 * 与识别规则无关的展示类设置。
 * [isLoaded] 用于区分「DataStore 还没读出来」与「确实是首次启动」，
 * 避免老用户在冷启动瞬间闪一下引导页。
 */
data class DisplaySettings(
    val codeScale: CodeScale = CodeScale.Standard,
    val groupBySender: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val onboardingDone: Boolean = false,
    val isLoaded: Boolean = false,
)
