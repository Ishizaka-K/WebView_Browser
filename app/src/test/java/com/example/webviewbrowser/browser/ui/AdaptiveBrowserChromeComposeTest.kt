package com.example.webviewbrowser.browser.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.webviewbrowser.core.ui.AppBackground
import com.example.webviewbrowser.core.ui.theme.WebViewBrowserTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdaptiveBrowserChromeComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `phone and light material layout renders`() {
        composeRule.setContent {
            WebViewBrowserTheme(darkTheme = false, dynamicColor = false) {
                AppBackground {
                    AdaptiveBrowserChrome(
                        widthDp = 412,
                        phoneContent = { Text("Phone", Modifier.testTag("phone-content")) },
                        tabletContent = { Text("Tablet") },
                    )
                }
            }
        }
        composeRule.onNodeWithTag("phone-layout").assertExists()
        composeRule.onNodeWithTag("phone-content").assertExists()
    }

    @Test
    fun `tablet and dark material layout renders`() {
        composeRule.setContent {
            WebViewBrowserTheme(darkTheme = true, dynamicColor = false) {
                AppBackground {
                    AdaptiveBrowserChrome(
                        widthDp = 1024,
                        phoneContent = { Text("Phone") },
                        tabletContent = { Text("Tablet", Modifier.testTag("tablet-content")) },
                    )
                }
            }
        }
        composeRule.onNodeWithTag("tablet-layout").assertExists()
        composeRule.onNodeWithTag("tablet-content").assertExists()
    }

    @Test
    fun `narrow split screen returns to phone chrome`() {
        composeRule.setContent {
            WebViewBrowserTheme(darkTheme = false, dynamicColor = false) {
                AdaptiveBrowserChrome(
                    widthDp = 520,
                    phoneContent = { Text("Split", Modifier.testTag("split-phone-content")) },
                    tabletContent = { Text("Tablet") },
                )
            }
        }
        composeRule.onNodeWithTag("phone-layout").assertExists()
        composeRule.onNodeWithTag("split-phone-content").assertExists()
    }
}
