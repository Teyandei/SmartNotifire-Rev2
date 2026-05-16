package com.example.smartnotifier.core.rule

/*
 * SmartNotifier-Rev2
 * Copyright (C) 2026  Takeaki Yoshizawa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * 複数の検索語を AND / OR / NOT で組み合わせる論理検索条件。
 *
 * 保存先に依存しない文字列形式へエンコードでき、永続化された文字列から復元できる。
 * 旧形式として保存されている単純な検索語は、後方互換のため AND 条件1件として扱う。
 *
 * @property andList すべて含まれている必要がある検索語。
 * @property notList 含まれていてはいけない検索語。
 * @property orList いずれかが含まれている必要がある検索語。空の場合は OR 条件なしとして扱う。
 */
data class TitleSearchCondition(
    val andList: List<String> = emptyList(),
    val notList: List<String> = emptyList(),
    val orList: List<String> = emptyList()
) {

    /**
     * この条件に判定対象の文字列が一致するかを調べる。
     *
     * AND 条件の検索語はすべて含まれ、NOT 条件の検索語はどれも含まれず、
     * OR 条件の検索語はいずれかが含まれる場合に一致とする。OR 条件が空の場合は、
     * OR 条件による制約はないものとして扱う。大文字と小文字は区別しない。
     *
     * @param title 判定対象の文字列。
     * @return 条件全体に一致する場合は `true`、一致しない場合は `false`。
     */
    fun matches(title: String): Boolean {
        fun contains(word: String): Boolean = title.contains(word, ignoreCase = true)

        val andOk = andList.all { contains(it) }
        val notOk = notList.none { contains(it) }
        val orOk = orList.isEmpty() || orList.any { contains(it) }

        return andOk && notOk && orOk
    }

    /**
     * この条件を永続化向けの文字列へ変換する。
     *
     * 空白だけの検索語は取り除き、単純な1語の条件で表現できる場合は旧形式の文字列として返す。
     * NOT 条件や複数条件を含む場合は、各検索語をエンコードした詳細形式の文字列として返す。
     *
     * @return 永続化に使用できる条件文字列。条件が空の場合は空文字列。
     */
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

    /**
     * この条件を単純な検索語として表示できる場合、その表示文字列を返す。
     *
     * NOT 条件がなく、AND 条件と OR 条件を合わせて検索語が0件または1件だけの場合に、
     * 詳細形式ではなく単純な文字列として表示できる。複数条件を含む場合は単純表示できない。
     *
     * @return 単純表示できる文字列。単純表示できない場合は `null`。
     */
    fun displayAsSimpleTextOrNull(): String? =
        simpleSearchText(
            normalizedAndList = normalizeList(andList),
            normalizedNotList = normalizeList(notList),
            normalizedOrList = normalizeList(orList)
        )

    /**
     * この条件を編集または表示する際に、詳細設定として扱う必要があるかを判定する。
     *
     * 単純な検索語だけでは表現できない条件、つまり NOT 条件や複数条件を含む条件は
     * 詳細設定が必要な条件として扱う。
     *
     * @return 詳細設定として扱う必要がある場合は `true`、単純な検索語として扱える場合は `false`。
     */
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

        /**
         * 永続化された条件文字列から [TitleSearchCondition] を復元する。
         *
         * 詳細形式の文字列は AND / NOT / OR の各条件へデコードする。
         * 詳細形式ではない旧形式の文字列は、後方互換のため AND 条件1件として扱う。
         * 空白だけの文字列は空の条件として扱う。
         *
         * @param ruleText 復元元の条件文字列。
         * @return 復元された論理検索条件。
         */
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

        /**
         * 条件文字列が詳細形式で保存されているかを判定する。
         *
         * 文字列前後の空白を除いたうえで、詳細形式の識別子を持つかどうかを確認する。
         * 旧形式の単純な検索語は詳細形式ではないものとして扱う。
         *
         * @param ruleText 判定対象の条件文字列。
         * @return 詳細形式の条件文字列である場合は `true`、それ以外の場合は `false`。
         */
        fun isDetailedRuleText(ruleText: String): Boolean =
            ruleText.trim().startsWith(PREFIX)

        /**
         * 永続化された条件文字列を表示する際に、詳細設定として扱う必要があるかを判定する。
         *
         * 詳細形式として保存されており、かつ単純な検索語だけでは表示できない場合に
         * 詳細設定が必要な条件として扱う。旧形式の文字列は単純表示できるため `false` になる。
         *
         * @param ruleText 判定対象の条件文字列。
         * @return 詳細設定として扱う必要がある場合は `true`、単純表示できる場合は `false`。
         */
        fun needsDetailedSettingsDisplay(ruleText: String): Boolean =
            isDetailedRuleText(ruleText) &&
                fromRuleText(ruleText).needsDetailedSettingsDisplay()

        /**
         * 永続化された条件文字列から、表示に適した文字列を取得する。
         *
         * 単純な検索語として表示できる条件は、その検索語を返す。
         * 複数条件や NOT 条件を含み単純表示できない場合は、元の条件文字列を返す。
         *
         * @param ruleText 表示対象の条件文字列。
         * @return 画面表示や一覧表示に使用できる文字列。
         */
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
