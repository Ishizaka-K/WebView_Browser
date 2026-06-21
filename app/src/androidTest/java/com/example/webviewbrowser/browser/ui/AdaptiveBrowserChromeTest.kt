package com.example.webviewbrowser.browser.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class AdaptiveBrowserChromeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun phoneWidthRendersPhoneChrome() {
        composeRule.setContent {
            AdaptiveBrowserChrome(
                widthDp = 412,
                phoneContent = { Text("Phone") },
                tabletContent = { Text("Tablet") },
            )
        }
        composeRule.onNodeWithTag("phone-layout").assertExists()
    }

    @Test
    fun tabletWidthRendersTabletChrome() {
        composeRule.setContent {
            AdaptiveBrowserChrome(
                widthDp = 1024,
                phoneContent = { Text("Phone") },
                tabletContent = { Text("Tablet") },
            )
        }
        composeRule.onNodeWithTag("tablet-layout").assertExists()
    }
}
