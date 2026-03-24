package com.decli.codehelper.util

class PickupCodeExtractor {

    data class ExtractedCode(
        val code: String,
        val matchedRule: String,
    )

    companion object {
        val defaultRules = listOf(
            """取件码[：:\s]*([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)""",
            """凭[：:\s]*([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)""",
        )

        private val codeTokenRegex = Regex("""[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*""")
    }

    fun sanitizeRules(rules: List<String>): List<String> =
        rules
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun firstInvalidRule(rules: List<String>): String? =
        sanitizeRules(rules).firstOrNull { rule ->
            runCatching {
                rule.toRegex(setOf(RegexOption.IGNORE_CASE))
            }.isFailure
        }

    fun extract(body: String, rules: List<String>): List<ExtractedCode> {
        val preparedRules = sanitizeRules(rules).ifEmpty { defaultRules }
        val matchesByKey = linkedMapOf<String, ExtractedCode>()

        preparedRules.forEach { rawRule ->
            val regex = runCatching {
                rawRule.toRegex(setOf(RegexOption.IGNORE_CASE))
            }.getOrNull() ?: return@forEach

            regex.findAll(body).forEach { result ->
                val code = extractCode(result.groupValues)
                if (!code.isNullOrBlank()) {
                    matchesByKey.putIfAbsent(
                        code.uppercase(),
                        ExtractedCode(code = code, matchedRule = rawRule),
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

