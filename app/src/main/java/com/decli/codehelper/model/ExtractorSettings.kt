package com.decli.codehelper.model

import com.decli.codehelper.util.PickupCodeExtractor

data class ExtractorSettings(
    val promptKeywords: List<String> = PickupCodeExtractor.defaultPromptKeywords,
    val advancedRules: List<String> = PickupCodeExtractor.defaultAdvancedRules,
)
