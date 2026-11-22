@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cookbookanything.app.R
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.theme.appBarColors

// One short section per place a recipe can come from.
private val sections = listOf(
    R.string.guide_instagram_title to R.string.guide_instagram,
    R.string.guide_tiktok_title to R.string.guide_tiktok,
    R.string.guide_facebook_title to R.string.guide_facebook,
    R.string.guide_youtube_title to R.string.guide_youtube,
    R.string.guide_web_title to R.string.guide_web,
    R.string.guide_photo_title to R.string.guide_photo,
    R.string.guide_text_title to R.string.guide_text,
)

/** How to get a recipe into the app from each source: plain text, nothing to tap. */
@Composable
fun ImportGuideScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(stringResource(R.string.import_guide)) },
                navigationIcon = { AppIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.guide_intro), style = MaterialTheme.typography.bodyLarge)
            sections.forEach { (title, body) ->
                Column {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
