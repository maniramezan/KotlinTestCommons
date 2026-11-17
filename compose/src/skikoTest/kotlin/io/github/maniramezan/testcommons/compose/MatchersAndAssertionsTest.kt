package io.github.maniramezan.testcommons.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MatchersAndAssertionsTest {
    @Test
    fun matchesTagPrefixesAndExactDescriptions() =
        runComposeUiTest {
            setContent {
                Column {
                    Box(Modifier.size(48.dp).testTag("app.row.1"))
                    Box(Modifier.size(48.dp).testTag("app.row.2"))
                    Box(Modifier.size(40.dp).testTag("other").semantics { contentDescription = "Close dialog" })
                }
            }
            onAllNodes(hasTestTagStartingWith("app.row.")).assertCountEquals(2)
            onAllNodes(hasTestTag("none") or hasTestTagStartingWith("x")).assertCountEquals(0)
            onNodeWithRequiredContentDescription("Close dialog").assertIsDisplayed()
            assertFailsWith<AssertionError> { onNodeWithRequiredContentDescription("close dialog").assertIsDisplayed() }
            onNodeWithTag("app.row.1").assertMinimumTouchTarget()
            assertFailsWith<AssertionError> { onNodeWithTag("other").assertMinimumTouchTarget() }
            onNodeWithTag("other").assertMinimumTouchTarget(minSize = 40.dp)
        }

    @Test
    fun replaceTextVerifiedChecksTheResultingText() =
        runComposeUiTest {
            setContent {
                var plain by remember { mutableStateOf("") }
                var digits by remember { mutableStateOf("") }
                Column {
                    BasicTextField(plain, { plain = it }, Modifier.testTag("plain"))
                    BasicTextField(digits, { input ->
                        digits = input.filter(Char::isDigit)
                    }, Modifier.testTag("digits"))
                }
            }
            onNodeWithTag("plain").replaceTextVerified("hello")
            onNodeWithTag("digits").replaceTextVerified("123")
            assertFailsWith<AssertionError> { onNodeWithTag("digits").replaceTextVerified("12a") }
        }
}
