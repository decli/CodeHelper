package com.decli.codehelper.util

class PickupCodeExtractor {

    data class ExtractedCode(
        val code: String,
        val matchedRule: String,
    )

    companion object {
        val defaultPromptKeywords = listOf(
            "取件码",
            "提货码",
            "货码",
            "驿站码",
            "凭",
        )

        val defaultAdvancedRules = emptyList<String>()
        val defaultRules: List<String> = defaultAdvancedRules

        private val codeTokenRegex = Regex("""[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*""")
        private val bridgeCharacters = setOf(
            '为',
            '是',
            '|',
            '“',
            '”',
            '"',
            '\'',
            '‘',
            '’',
            '：',
            ':',
            '、',
            '，',
            ',',
            '；',
            ';',
            '（',
            '）',
            '(',
            ')',
            '【',
            '】',
            '[',
            ']',
            '/',
            '\\',
            '-',
            ' ',
            '\n',
            '\r',
            '\t',
        )
    }

    fun sanitizePromptKeywords(promptKeywords: List<String>): List<String> =
        promptKeywords
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun keywordValidationError(keyword: String): String? =
        if (keyword.trim().isEmpty()) {
            "提示词不能为空"
        } else {
            null
        }

    fun draftKeywordErrors(promptKeywords: List<String>): List<String?> =
        promptKeywords.map(::keywordValidationError)

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

        return if (
            runCatching {
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

    fun extract(
        body: String,
        promptKeywords: List<String>,
        advancedRules: List<String>,
    ): List<ExtractedCode> {
        val preparedPromptKeywords = sanitizePromptKeywords(promptKeywords).ifEmpty { defaultPromptKeywords }
        val preparedAdvancedRules = sanitizeRules(advancedRules).ifEmpty { defaultAdvancedRules }
        val matchesByKey = linkedMapOf<String, ExtractedCode>()

        preparedPromptKeywords.forEachIndexed { index, keyword ->
            extractCodesFromKeyword(body = body, keyword = keyword).forEach { code ->
                matchesByKey.putIfAbsent(
                    code.uppercase(),
                    ExtractedCode(
                        code = code,
                        matchedRule = "命中规则${index + 1}",
                    ),
                )
            }
        }

        val baseRuleIndex = preparedPromptKeywords.size
        preparedAdvancedRules.forEachIndexed { index, rawRule ->
            val regex = runCatching {
                rawRule.toRegex(setOf(RegexOption.IGNORE_CASE))
            }.getOrNull() ?: return@forEachIndexed

            regex.findAll(body).forEach { result ->
                extractCodes(result.groupValues).forEach { code ->
                    matchesByKey.putIfAbsent(
                        code.uppercase(),
                        ExtractedCode(
                            code = code,
                            matchedRule = "命中规则${baseRuleIndex + index + 1}",
                        ),
                    )
                }
            }
        }

        return matchesByKey.values.toList()
    }

    private fun extractCodesFromKeyword(body: String, keyword: String): List<String> {
        val codes = linkedSetOf<String>()
        val keywordRegex = Regex(Regex.escape(keyword), RegexOption.IGNORE_CASE)

        keywordRegex.findAll(body).forEach { match ->
            collectCodeSequence(body, match.range.last + 1).forEach(codes::add)
        }

        return codes.toList()
    }

    private fun collectCodeSequence(body: String, startIndex: Int): List<String> {
        var cursor = skipBridgeCharacters(body, startIndex)
        val codes = mutableListOf<String>()

        while (cursor < body.length) {
            val match = codeTokenRegex.find(body, cursor)
            if (match == null || match.range.first != cursor) {
                break
            }

            val code = match.value.trim()
            if (isValidCodeToken(code)) {
                codes += code
            }

            cursor = skipBridgeCharacters(body, match.range.last + 1)
        }

        return codes
    }

    private fun skipBridgeCharacters(body: String, startIndex: Int): Int {
        var cursor = startIndex
        while (cursor < body.length && isBridgeCharacter(body[cursor])) {
            cursor += 1
        }
        return cursor
    }

    private fun isBridgeCharacter(char: Char): Boolean =
        char.isWhitespace() || char in bridgeCharacters

    private fun extractCodes(groupValues: List<String>): List<String> {
        val capturedBlocks = groupValues.drop(1).filter { it.isNotBlank() }
        val sources = if (capturedBlocks.isEmpty()) {
            listOf(groupValues.firstOrNull().orEmpty())
        } else {
            capturedBlocks
        }

        return sources
            .flatMap(::extractCodeTokens)
            .distinctBy { it.uppercase() }
    }

    private fun extractCodeTokens(source: String): List<String> =
        codeTokenRegex
            .findAll(source)
            .map { it.value.trim() }
            .filter(::isValidCodeToken)
            .toList()

    private fun isValidCodeToken(code: String): Boolean =
        code.count { it.isLetterOrDigit() } >= 2
}
