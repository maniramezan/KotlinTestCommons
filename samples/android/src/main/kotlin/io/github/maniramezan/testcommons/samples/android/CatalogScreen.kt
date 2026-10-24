package io.github.maniramezan.testcommons.samples.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SampleTheme(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF4F5FA))) {
        content()
    }
}

@Composable
internal fun CatalogScreen(state: CatalogState, onReload: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var savedName by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().safeDrawingPadding().testTag(SampleIds.list)
            .semantics { testTagsAsResourceId = true },
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BasicText("TEST COMMONS", style = TextStyle(color = Color(0xFF555B80), fontSize = 12.sp))
        }
        item {
            BasicText("Learning library", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold))
        }
        item {
            BasicText("A small app. Predictable tests.", style = TextStyle(fontSize = 16.sp))
        }
        item {
            BasicText("Your name", style = TextStyle(fontSize = 14.sp))
        }
        item {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth().testTag(SampleIds.name)
                    .semantics { contentDescription = "Your name" }
                    .background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp),
            )
        }
        item {
            SampleButton("Save name", SampleIds.save, enabled = name.isNotBlank()) { savedName = name.trim() }
        }
        item {
            BasicText(
                if (savedName.isEmpty()) "Make yourself at home." else "Hello, $savedName!",
                Modifier.testTag(SampleIds.greeting),
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
            )
        }
        item {
            SampleButton("Reload lessons", SampleIds.reload, enabled = !state.isLoading, onClick = onReload)
        }
        when {
            state.isLoading -> item {
                BasicText("Loading lessons…", Modifier.testTag(SampleIds.loading))
            }

            state.error != null -> item {
                BasicText(state.error, style = TextStyle(color = Color(0xFF9E1837)))
            }

            else -> items(state.lessons.size, key = { "lesson-$it" }) { index ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        .testTag(SampleIds.lesson(index + 1))
                        .background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicText(state.lessons[index], style = TextStyle(fontSize = 18.sp))
                }
            }
        }
    }
}

@Composable
private fun SampleButton(label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Color(0xFF393D73) else Color(0xFF6A6D8E))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium))
    }
}
