@file:OptIn(ExperimentalMaterial3Api::class)

package com.cookbookanything.app.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cookbookanything.app.R
import com.cookbookanything.app.data.account.AccountError
import com.cookbookanything.app.data.account.AccountState
import com.cookbookanything.app.ui.components.AppIconButton
import com.cookbookanything.app.ui.components.ConfirmDialog
import com.cookbookanything.app.ui.theme.appBarColors

@Composable
fun AccountRoute(onBack: () -> Unit, viewModel: AccountViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Signing in is the whole point of the screen, so it closes itself once that happened.
    LaunchedEffect(uiState.account) { if (uiState.account is AccountState.SignedIn) onBack() }
    AccountScreen(
        uiState = uiState,
        onBack = onBack,
        onSignIn = viewModel::signIn,
        onRegister = viewModel::register,
        onResetPassword = viewModel::resetPassword,
        onSignOut = viewModel::signOut,
        onDeleteAccount = viewModel::deleteAccount,
    )
}

@Composable
fun AccountScreen(
    uiState: AccountUiState,
    onBack: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onResetPassword: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val canSubmit = !uiState.busy && email.isNotBlank() && password.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(stringResource(R.string.account)) },
                navigationIcon = { AppIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(visible = uiState.busy) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            when (val account = uiState.account) {
                AccountState.Unavailable -> Text(stringResource(R.string.account_unavailable), style = MaterialTheme.typography.bodyLarge)

                is AccountState.SignedIn -> {
                    Text(stringResource(R.string.account_signed_in_as, account.email), style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = onSignOut, enabled = !uiState.busy) { Text(stringResource(R.string.sign_out)) }
                    TextButton(
                        onClick = { confirmDelete = true },
                        enabled = !uiState.busy,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.delete_account))
                    }
                }

                AccountState.SignedOut -> {
                    Text(
                        text = stringResource(R.string.account_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.email)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        supportingText = { Text(stringResource(R.string.password_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = { onSignIn(email, password) }, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.sign_in))
                    }
                    OutlinedButton(onClick = { onRegister(email, password) }, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.register))
                    }
                    TextButton(onClick = { onResetPassword(email) }, enabled = !uiState.busy && email.isNotBlank()) {
                        Text(stringResource(R.string.forgot_password))
                    }
                }
            }
            uiState.error?.let { error ->
                Text(stringResource(error.message()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            uiState.resetSentTo?.let { sentTo ->
                Text(stringResource(R.string.reset_sent, sentTo), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_account),
            text = stringResource(R.string.delete_account_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                onDeleteAccount()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

private fun AccountError.message(): Int = when (this) {
    AccountError.WRONG_CREDENTIALS -> R.string.error_wrong_credentials
    AccountError.EMAIL_IN_USE -> R.string.error_email_in_use
    AccountError.WEAK_PASSWORD -> R.string.error_weak_password
    AccountError.INVALID_EMAIL -> R.string.error_invalid_email
    AccountError.NETWORK -> R.string.error_network
    AccountError.SIGN_IN_AGAIN -> R.string.error_sign_in_again
    AccountError.UNKNOWN -> R.string.error_unknown
}
