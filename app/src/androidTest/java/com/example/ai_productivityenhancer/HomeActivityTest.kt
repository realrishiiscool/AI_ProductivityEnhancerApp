package com.example.ai_productivityenhancer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class HomeActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<HomeActivity>()

    @Test
    fun testWelcomeMessageIsDisplayed() {
        composeTestRule.onNodeWithText("Welcome, User").assertIsDisplayed()
    }

    @Test
    fun testAccessibilityButtonIsDisplayed() {
        composeTestRule.onNodeWithText("Enable Accessibility Service").assertIsDisplayed()
    }

    @Test
    fun testAutoManualToggle() {
        composeTestRule.onNodeWithText("Manual Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Auto Mode (AI)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Auto Mode (AI)").performClick()
    }

    @Test
    fun testAppListIsDisplayedInManualMode() {
        composeTestRule.onNodeWithText("Manual Mode").assertIsDisplayed()
        // more checks can be added here
    }

    @Test
    fun testAIModeScreenIsDisplayedInAutoMode() {
        composeTestRule.onNodeWithText("Auto Mode (AI)").performClick()
        composeTestRule.onNodeWithText("AI Suggestions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Smart Scheduling").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Activity Log").assertIsDisplayed()
    }
}
