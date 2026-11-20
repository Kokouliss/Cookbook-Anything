package com.cookbookanything.app

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.app.data.account.AccountRepository
import com.cookbookanything.app.data.account.AccountState
import com.cookbookanything.app.data.share.BackupManager
import com.cookbookanything.app.data.share.BackupPreferences
import com.cookbookanything.app.data.share.BackupStatus
import com.cookbookanything.app.data.share.RecipeSharer
import com.cookbookanything.app.data.share.RestoreResult
import com.cookbookanything.core.domain.FreeTier
import com.cookbookanything.core.domain.repository.EntitlementRepository
import com.cookbookanything.core.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val recipeCount: Int = 0,
    val unlocked: Boolean = false,
    val backup: BackupStatus = BackupStatus(),
    val account: AccountState = AccountState.Unavailable,
) {
    val canAddRecipe: Boolean get() = FreeTier.canAddRecipe(recipeCount, unlocked)
}

sealed interface MainEvent {
    data class OpenRecipe(val recipeId: String, val startCooking: Boolean = false) : MainEvent
    data object OpenShoppingList : MainEvent
    data object OpenCalendar : MainEvent
    /** Opens the editor with a recipe read out of shared text. */
    data class NewRecipeFromText(val text: String) : MainEvent
    /** Opens the editor with a recipe read off shared screenshots. */
    data class NewRecipeFromPhotos(val uris: List<String>) : MainEvent
    data object ImportFailed : MainEvent
    data object LimitReached : MainEvent
    data class BackupRestored(val added: Int) : MainEvent
    data class BackupSaved(val success: Boolean) : MainEvent
}

/** App-wide state: the free-tier limit, and recipes arriving from outside (shared files, reminders). */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val entitlementRepository: EntitlementRepository,
    private val recipeSharer: RecipeSharer,
    private val backupManager: BackupManager,
    private val backupPreferences: BackupPreferences,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        recipeRepository.observeCount(),
        entitlementRepository.isUnlocked,
        backupPreferences.status,
        accountRepository.state,
    ) { count, unlocked, backup, account ->
        MainUiState(recipeCount = count, unlocked = unlocked, backup = backup, account = account)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    private val eventChannel = Channel<MainEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun openRecipe(recipeId: String, startCooking: Boolean = false) {
        eventChannel.trySend(MainEvent.OpenRecipe(recipeId, startCooking))
    }

    fun openShoppingList() {
        eventChannel.trySend(MainEvent.OpenShoppingList)
    }

    fun openCalendar() {
        eventChannel.trySend(MainEvent.OpenCalendar)
    }

    fun newRecipeFromText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { eventChannel.send(if (canAddRecipe()) MainEvent.NewRecipeFromText(text) else MainEvent.LimitReached) }
    }

    fun newRecipeFromPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            eventChannel.send(if (canAddRecipe()) MainEvent.NewRecipeFromPhotos(uris.map(Uri::toString)) else MainEvent.LimitReached)
        }
    }

    private suspend fun canAddRecipe() = FreeTier.canAddRecipe(
        recipeCount = recipeRepository.observeCount().first(),
        unlocked = entitlementRepository.isUnlocked.first(),
    )

    fun setBackupFolder(folder: Uri) = backupPreferences.setFolder(folder)

    fun clearBackupFolder() = backupPreferences.clearFolder()

    fun signOut() = accountRepository.signOut()

    /** Opens a file from outside the app: a backup of the whole cookbook, or one shared recipe. */
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            val unlocked = entitlementRepository.isUnlocked.first()
            when (val restored = backupManager.restore(uri, unlocked)) {
                is RestoreResult.Restored -> return@launch eventChannel.send(MainEvent.BackupRestored(restored.added))
                RestoreResult.LimitReached -> return@launch eventChannel.send(MainEvent.LimitReached)
                RestoreResult.NotABackup -> Unit
            }
            val canAdd = FreeTier.canAddRecipe(
                recipeCount = recipeRepository.observeCount().first(),
                unlocked = unlocked,
            )
            if (!canAdd) {
                eventChannel.send(MainEvent.LimitReached)
                return@launch
            }
            val recipe = recipeSharer.importRecipeFile(uri)
            if (recipe == null) {
                eventChannel.send(MainEvent.ImportFailed)
            } else {
                recipeRepository.saveRecipe(recipe)
                eventChannel.send(MainEvent.OpenRecipe(recipe.id))
            }
        }
    }

    fun saveBackup(target: Uri) {
        viewModelScope.launch { eventChannel.send(MainEvent.BackupSaved(backupManager.export(target))) }
    }

    fun unlock() {
        viewModelScope.launch { entitlementRepository.setUnlocked(true) }
    }
}
