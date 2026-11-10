package com.cookbookanything.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
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
        // Every screen has a dark brand-coloured top bar, so status bar icons stay light.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
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
        intent.getStringExtra(EXTRA_RECIPE_ID)?.let(viewModel::openRecipe)
        if (intent.action == Intent.ACTION_VIEW) intent.data?.let(viewModel::importFile)
    }

    companion object {
        const val EXTRA_RECIPE_ID = "recipeId"
    }
}
