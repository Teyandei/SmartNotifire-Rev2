package com.example.smartnotifier.core.rule

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * 通知タイトル検索の詳細条件。
 *
 * 既存DBの RuleEntity.srhTitle に保存できるよう、文字列へエンコードする。
 * 旧形式の単純な検索語は AND 条件1件として扱う。
 */
data class TitleSearchCondition(
    val andList: List<String> = emptyList(),
    val notList: List<String> = emptyList(),
    val orList: List<String> = emptyList()
) {

    fun matches(title: String): Boolean {
        fun contains(word: String): Boolean = title.contains(word, ignoreCase = true)

        val andOk = andList.all { contains(it) }
        val notOk = notList.none { contains(it) }
        val orOk = orList.isEmpty() || orList.any { contains(it) }

        return andOk && notOk && orOk
    }

    fun toRuleText(): String {
        val normalizedAndList = normalizeList(andList)
        val normalizedNotList = normalizeList(notList)
        val normalizedOrList = normalizeList(orList)
        if (normalizedAndList.isEmpty() && normalizedNotList.isEmpty() && normalizedOrList.isEmpty()) {
            return ""
        }
        simpleSearchText(
            normalizedAndList = normalizedAndList,
            normalizedNotList = normalizedNotList,
            normalizedOrList = normalizedOrList
        )?.let { return it }

        val andPart = encodeList(normalizedAndList)
        val notPart = encodeList(normalizedNotList)
        val orPart = encodeList(normalizedOrList)

        return "$PREFIX$SECTION_AND$andPart$SECTION_NOT$notPart$SECTION_OR$orPart"
    }

    fun displayAsSimpleTextOrNull(): String? =
        simpleSearchText(
            normalizedAndList = normalizeList(andList),
            normalizedNotList = normalizeList(notList),
            normalizedOrList = normalizeList(orList)
        )

    fun needsDetailedSettingsDisplay(): Boolean =
        displayAsSimpleTextOrNull() == null

    private fun simpleSearchText(
        normalizedAndList: List<String>,
        normalizedNotList: List<String>,
        normalizedOrList: List<String>
    ): String? {
        if (normalizedNotList.isNotEmpty()) {
            return null
        }

        val positiveConditions = normalizedAndList + normalizedOrList
        if (positiveConditions.size > 1) {
            return null
        }

        return positiveConditions.firstOrNull().orEmpty()
    }

    companion object {
        private const val PREFIX = "SNCOND:v1:"
        private const val SECTION_AND = "A="
        private const val SECTION_NOT = ";N="
        private const val SECTION_OR = ";O="
        private const val ITEM_SEPARATOR = ","

        fun fromRuleText(ruleText: String): TitleSearchCondition {
            val text = ruleText.trim()
            if (text.isBlank()) return TitleSearchCondition()
            if (!text.startsWith(PREFIX)) {
                return TitleSearchCondition(andList = listOf(text))
            }

            val body = text.removePrefix(PREFIX)
            val andPart = body.substringAfter(SECTION_AND, "")
                .substringBefore(SECTION_NOT)
            val notPart = body.substringAfter(SECTION_NOT, "")
                .substringBefore(SECTION_OR)
            val orPart = body.substringAfter(SECTION_OR, "")

            return TitleSearchCondition(
                andList = decodeList(andPart),
                notList = decodeList(notPart),
                orList = decodeList(orPart)
            )
        }

        fun isDetailedRuleText(ruleText: String): Boolean =
            ruleText.trim().startsWith(PREFIX)

        fun needsDetailedSettingsDisplay(ruleText: String): Boolean =
            isDetailedRuleText(ruleText) &&
                fromRuleText(ruleText).needsDetailedSettingsDisplay()

        fun displayText(ruleText: String): String =
            fromRuleText(ruleText).displayAsSimpleTextOrNull() ?: ruleText

        private fun normalizeList(items: List<String>): List<String> =
            items.map { it.trim() }.filter { it.isNotEmpty() }

        private fun encodeList(items: List<String>): String =
            normalizeList(items)
                .map { item ->
                    Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(item.toByteArray(StandardCharsets.UTF_8))
                }
                .joinToString(ITEM_SEPARATOR)

        private fun decodeList(part: String): List<String> {
            if (part.isBlank()) return emptyList()

            return part.split(ITEM_SEPARATOR)
                .filter { it.isNotBlank() }
                .mapNotNull { item ->
                    runCatching {
                        String(
                            Base64.getUrlDecoder().decode(item),
                            StandardCharsets.UTF_8
                        )
                    }.getOrNull()
                }
                .filter { it.isNotBlank() }
        }
    }
}
