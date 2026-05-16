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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleSearchConditionTest {

    @Test
    fun toRuleText_singleOrCondition_isStoredAsSimpleText() {
        val ruleText = TitleSearchCondition(orList = listOf("明")).toRuleText()

        assertEquals("明", ruleText)
        assertFalse(TitleSearchCondition.needsDetailedSettingsDisplay(ruleText))
    }

    @Test
    fun needsDetailedSettingsDisplay_encodedSingleOrCondition_isFalse() {
        val ruleText = "SNCOND:v1:A=;N=;O=5piO"

        assertFalse(TitleSearchCondition.needsDetailedSettingsDisplay(ruleText))
        assertEquals("明", TitleSearchCondition.displayText(ruleText))
    }

    @Test
    fun needsDetailedSettingsDisplay_multipleOrConditions_isTrue() {
        val ruleText = TitleSearchCondition(orList = listOf("明", "正明")).toRuleText()

        assertTrue(TitleSearchCondition.needsDetailedSettingsDisplay(ruleText))
    }

    @Test
    fun needsDetailedSettingsDisplay_notCondition_isTrue() {
        val ruleText = TitleSearchCondition(notList = listOf("正明")).toRuleText()

        assertTrue(TitleSearchCondition.needsDetailedSettingsDisplay(ruleText))
    }
}
