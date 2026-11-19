package com.cookbookanything.app.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbookanything.app.data.account.AccountError
import com.cookbookanything.app.data.account.AccountRepository
import com.cookbookanything.app.data.account.AccountState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val account: AccountState = AccountState.Unavailable,
    val busy: Boolean = false,
    val error: AccountError? = null,
    /** The address a password reset link was just sent to. */
    val resetSentTo: String? = null,
)

@HiltViewModel
class AccountViewModel @Inject constructor(private val accountRepository: AccountRepository) : ViewModel() {
    private data class Progress(val busy: Boolean = false, val error: AccountError? = null, val resetSentTo: String? = null)

    private val progress = MutableStateFlow(Progress())

    val uiState: StateFlow<AccountUiState> = combine(accountRepository.state, progress) { account, progress ->
        AccountUiState(account = account, busy = progress.busy, error = progress.error, resetSentTo = progress.resetSentTo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    fun signIn(email: String, password: String) = attempt { accountRepository.signIn(email, password) }

    fun register(email: String, password: String) = attempt { accountRepository.register(email, password) }

    fun resetPassword(email: String) = attempt(resetSentTo = email) { accountRepository.sendPasswordReset(email) }

    fun signOut() = accountRepository.signOut()

    fun deleteAccount() = attempt { accountRepository.deleteAccount() }

    private fun attempt(resetSentTo: String? = null, call: suspend () -> AccountError?) {
        viewModelScope.launch {
            progress.update { Progress(busy = true) }
            val error = call()
            progress.update { Progress(error = error, resetSentTo = resetSentTo?.takeIf { error == null }) }
        }
    }
}
