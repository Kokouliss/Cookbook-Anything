package com.cookbookanything.app.data.account

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

sealed interface AccountState {
    /** The build has no Firebase configuration, so there is nothing to sign in to. */
    data object Unavailable : AccountState
    data object SignedOut : AccountState
    data class SignedIn(val email: String) : AccountState
}

enum class AccountError { WRONG_CREDENTIALS, EMAIL_IN_USE, WEAK_PASSWORD, INVALID_EMAIL, NETWORK, SIGN_IN_AGAIN, UNKNOWN }

/**
 * Email and password accounts through Firebase Authentication. Signing in does not move recipes
 * anywhere yet; it is the account that cloud sync will attach to.
 */
@Singleton
class AccountRepository @Inject constructor(@ApplicationContext context: Context) {
    private val auth: FirebaseAuth? = if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance()
    private val _state = MutableStateFlow(current())
    val state: StateFlow<AccountState> = _state

    init {
        auth?.addAuthStateListener { _state.value = current() }
    }

    suspend fun signIn(email: String, password: String): AccountError? =
        attempt { it.signInWithEmailAndPassword(email.trim(), password) }

    suspend fun register(email: String, password: String): AccountError? =
        attempt { it.createUserWithEmailAndPassword(email.trim(), password) }

    suspend fun sendPasswordReset(email: String): AccountError? = attempt { it.sendPasswordResetEmail(email.trim()) }

    fun signOut() {
        auth?.signOut()
    }

    /** Deletes the account itself. Firebase asks for a fresh sign-in when the last one is old. */
    suspend fun deleteAccount(): AccountError? {
        val user = auth?.currentUser ?: return AccountError.UNKNOWN
        return attempt { user.delete() }
    }

    private fun current(): AccountState {
        val firebase = auth ?: return AccountState.Unavailable
        val email = firebase.currentUser?.email ?: return AccountState.SignedOut
        return AccountState.SignedIn(email)
    }

    /** Runs a Firebase call and turns its failure into something the screen can word. */
    private suspend fun attempt(call: (FirebaseAuth) -> Task<*>): AccountError? {
        val firebase = auth ?: return AccountError.UNKNOWN
        return runCatching { call(firebase).await() }.exceptionOrNull()?.let { error ->
            when (error) {
                is FirebaseAuthWeakPasswordException -> AccountError.WEAK_PASSWORD
                is FirebaseAuthUserCollisionException -> AccountError.EMAIL_IN_USE
                is FirebaseAuthRecentLoginRequiredException -> AccountError.SIGN_IN_AGAIN
                // Firebase hides whether the email exists, so a wrong email and a wrong password look the same.
                is FirebaseAuthInvalidCredentialsException ->
                    if (error.errorCode == "ERROR_INVALID_EMAIL") AccountError.INVALID_EMAIL else AccountError.WRONG_CREDENTIALS
                is FirebaseAuthInvalidUserException -> AccountError.WRONG_CREDENTIALS
                is FirebaseNetworkException -> AccountError.NETWORK
                else -> AccountError.UNKNOWN
            }
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
