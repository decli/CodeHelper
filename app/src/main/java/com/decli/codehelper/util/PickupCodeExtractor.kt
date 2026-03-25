package com.decli.codehelper.util

class PickupCodeExtractor {

    data class ExtractedCode(
        val code: String,
        val matchedRule: String,
    )

    companion object {
        private const val codeCapture = """([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)"""
        private const val separator = """[：:\s]*"""
        private const val openingQuote = """[“"'‘「『]?"""
        private const val closingQuote = """[”"'’」』]?"""

        val defaultRules = listOf(
            """(?:取件码|提货码|货码|驿站码)$separator$openingQuote$codeCapture$closingQuote""",
            """凭$separator$openingQuote$codeCapture$closingQuote""",
        )

        private val codeTokenRegex = Regex("""[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*""")
    }

    fun sanitizeRules(rules: List<String>): List<String> =
        rules
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun validationError(rule: String): String? {
        val sanitizedRule = rule.trim()
        if (sanitizedRule.isEmpty()) {
            return "规则不能为空"
        }

        return if (runCatching {
                sanitizedRule.toRegex(setOf(RegexOption.IGNORE_CASE))
            }.isSuccess
        ) {
            null
        } else {
            "正则语法错误"
        }
    }

    fun draftValidationErrors(rules: List<String>): List<String?> =
        rules.map(::validationError)

    fun firstInvalidRule(rules: List<String>): String? =
        sanitizeRules(rules).firstOrNull { rule ->
            validationError(rule) != null
        }

    fun extract(body: String, rules: List<String>): List<ExtractedCode> {
        val preparedRules = sanitizeRules(rules).ifEmpty { defaultRules }
        val matchesByKey = linkedMapOf<String, ExtractedCode>()

        preparedRules.forEachIndexed { index, rawRule ->
            val regex = runCatching {
                rawRule.toRegex(setOf(RegexOption.IGNORE_CASE))
            }.getOrNull() ?: return@forEachIndexed

            regex.findAll(body).forEach { result ->
                val code = extractCode(result.groupValues)
                if (!code.isNullOrBlank()) {
                    matchesByKey.putIfAbsent(
                        code.uppercase(),
                        ExtractedCode(
                            code = code,
                            matchedRule = "命中规则${index + 1}",
                        ),
                    )
                }
            }
        }

        return matchesByKey.values.toList()
    }

    private fun extractCode(groupValues: List<String>): String? {
        val captured = groupValues.drop(1).firstOrNull { it.isNotBlank() }?.trim()
        val source = captured ?: groupValues.firstOrNull().orEmpty()
        return codeTokenRegex.findAll(source).lastOrNull()?.value?.trim()
    }
}

