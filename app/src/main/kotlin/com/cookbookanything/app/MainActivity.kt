package com.cookbookanything.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cookbookanything.app.navigation.CookbookApp
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
        setContent {
            CookbookTheme {
                CookbookApp(viewModel)
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
            // Text shared from another app (a chat, a browser) becomes a new recipe.
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let(viewModel::newRecipeFromText)
        }
    }

    companion object {
        const val EXTRA_RECIPE_ID = "recipeId"
        const val EXTRA_START_COOKING = "startCooking"
        const val EXTRA_OPEN_SHOPPING = "openShopping"
        const val EXTRA_OPEN_CALENDAR = "openCalendar"
    }
}
