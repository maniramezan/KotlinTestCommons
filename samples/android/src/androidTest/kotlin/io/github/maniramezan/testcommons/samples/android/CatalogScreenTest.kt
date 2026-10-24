package io.github.maniramezan.testcommons.samples.android

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.maniramezan.testcommons.compose.ThemedComposeRule
import io.github.maniramezan.testcommons.compose.assertMinimumTouchTarget
import io.github.maniramezan.testcommons.compose.deviceConfiguration
import io.github.maniramezan.testcommons.compose.replaceTextVerified
import io.github.maniramezan.testcommons.compose.scrollToNode
import io.github.maniramezan.testcommons.compose.waitForTag
import io.github.maniramezan.testcommons.compose.waitForText
import io.github.maniramezan.testcommons.compose.waitUntilGone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogScreenTest {
    @get:Rule
    val composeRule = ThemedComposeRule(theme = { content -> SampleTheme(content) })

    @Test
    fun editsANameAndRevealsALazyListItem() {
        composeRule.setContent {
            CatalogScreen(CatalogState(isLoading = false, lessons = List(40) { "Lesson ${it + 1}" }), onReload = {})
        }
        composeRule.waitForTag(SampleIds.name).replaceTextVerified("Ada")
        composeRule.waitForTag(SampleIds.save).assertMinimumTouchTarget().performClick()
        composeRule.waitForText("Hello, Ada!").assertIsDisplayed()
        composeRule.scrollToNode(hasTestTag(SampleIds.lesson(40))).assertIsDisplayed()
        composeRule.waitUntilGone(hasTestTag(SampleIds.loading))
    }

    @Test
    fun lessonsAndProfileRemainReachableInAShortWindowWithLargeText() {
        composeRule.setContent {
            DeviceConfigurationOverride(deviceConfiguration(size = DpSize(640.dp, 320.dp), fontScale = 2f)) {
                CatalogScreen(CatalogState(isLoading = false, lessons = List(40) { "Lesson ${it + 1}" }), onReload = {})
            }
        }
        composeRule.scrollToNode(hasTestTag(SampleIds.lesson(40))).assertIsDisplayed()
        composeRule.scrollToNode(hasTestTag(SampleIds.name)).assertIsDisplayed()
    }
}
