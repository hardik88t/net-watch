package com.netwatch.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.netwatch.app.ui.theme.NetWatchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboarding_defaultState_showsMissingPermissionsAndDisabledContinue() {
        composeTestRule.setContent {
            NetWatchTheme {
                OnboardingScreen(
                    onGrantAccess = {},
                    onSkip = {},
                    permissionStateOverride = OnboardingPermissionState() // All false by default
                )
            }
        }

        // Verify title
        composeTestRule.onNodeWithText("Permission Setup").assertIsDisplayed()

        // Verify Continue button is disabled
        composeTestRule.onNodeWithText("Continue").assertIsNotEnabled()

        // Verify missing permissions text exists
        composeTestRule.onNodeWithText("Action Required: Please grant", substring = true).assertIsDisplayed()
    }
    
    @Test
    fun onboarding_allPermissionsGranted_enablesContinue() {
        composeTestRule.setContent {
            NetWatchTheme {
                OnboardingScreen(
                    onGrantAccess = {},
                    onSkip = {},
                    permissionStateOverride = OnboardingPermissionState(
                        fineLocationGranted = true,
                        backgroundLocationGranted = true,
                        phoneStateGranted = true,
                        notificationsGranted = true,
                        usageAccessGranted = true
                    )
                )
            }
        }

        // Verify Continue button is enabled
        composeTestRule.onNodeWithText("Continue").assertIsEnabled()

        // Verify missing permissions text does NOT exist
        composeTestRule.onNodeWithText("Action Required: Please grant", substring = true).assertDoesNotExist()
    }
    
    @Test
    fun onboarding_skipDialog_showsCorrectly() {
        composeTestRule.setContent {
            NetWatchTheme {
                OnboardingScreen(
                    onGrantAccess = {},
                    onSkip = {},
                    permissionStateOverride = OnboardingPermissionState()
                )
            }
        }

        // Click on the skip text button
        composeTestRule.onNodeWithText("Skip with limited diagnostics").performClick()
        
        // Verify dialog components
        composeTestRule.onNodeWithText("Skip required access?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip Anyway").assertIsDisplayed()
    }
}
