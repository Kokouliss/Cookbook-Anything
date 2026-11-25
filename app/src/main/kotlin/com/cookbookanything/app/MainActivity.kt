package com.cookbookanything.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import com.cookbookanything.app.navigation.CookbookApp
import com.cookbookanything.app.ui.components.Preloader
import com.cookbookanything.app.ui.components.animationScale
import com.cookbookanything.app.ui.theme.CookbookTheme
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity (not ComponentActivity) so the in-app language switch also works before Android 13.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Bars share the card box's background, so the status bar icons follow light and dark.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        // The preloader plays when the app is opened from its icon, not when a share, a notification or a
        // restore opens it on a page, and not when the phone's animations are off.
        val preload = savedInstanceState == null && intent?.action == Intent.ACTION_MAIN && animationScale(this) > 0f
        setContent {
            CookbookTheme {
                var preloading by rememberSaveable { mutableStateOf(preload) }
                // The app is built while the ingredients hang still, so building it never stalls their fall.
                var showApp by rememberSaveable { mutableStateOf(!preload) }
                Box(modifier = Modifier.fillMaxSize()) {
                    // The app loads underneath meanwhile.
                    if (showApp) CookbookApp(viewModel)
                    if (preloading) {
                        Preloader(
                            onSettled = { showApp = true },
                            onFinished = {
                                preloading = false
                                showApp = true
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.getStringExtra(EXTRA_RECIPE_ID)?.let { recipeId ->
            viewModel.openRecipe(recipeId, startCooking = intent.getBooleanExtra(EXTRA_START_COOKING, false))
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_SHOPPING, false)) viewModel.openShoppingList()
        if (intent.getBooleanExtra(EXTRA_OPEN_CALENDAR, false)) viewModel.openCalendar()
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let(viewModel::importFile)
            // Text shared from another app (a chat, a browser) becomes a new recipe; shared screenshots are read like photos.
            Intent.ACTION_SEND ->
                if (intent.type?.startsWith("image/") == true) viewModel.newRecipeFromPhotos(sharedImages(intent))
                else intent.getStringExtra(Intent.EXTRA_TEXT)?.let(viewModel::newRecipeFromText)
            Intent.ACTION_SEND_MULTIPLE -> viewModel.newRecipeFromPhotos(sharedImages(intent))
        }
    }

    private fun sharedImages(intent: Intent): List<Uri> =
        IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))

    companion object {
        const val EXTRA_RECIPE_ID = "recipeId"
        const val EXTRA_START_COOKING = "startCooking"
        const val EXTRA_OPEN_SHOPPING = "openShopping"
        const val EXTRA_OPEN_CALENDAR = "openCalendar"
    }
}
