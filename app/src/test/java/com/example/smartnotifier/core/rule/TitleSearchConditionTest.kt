package com.example.smartnotifier.core.rule

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
