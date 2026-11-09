package com.cookbookanything.app.data.repository

import android.content.Context
import com.cookbookanything.core.domain.repository.EntitlementRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// shortcut: a local flag stands in for the purchase, replace with Google Play Billing before selling the app
@Singleton
class LocalEntitlementRepository @Inject constructor(
    @ApplicationContext context: Context,
) : EntitlementRepository {
    private val preferences = context.getSharedPreferences("entitlement", Context.MODE_PRIVATE)
    private val unlocked = MutableStateFlow(preferences.getBoolean(KEY_UNLOCKED, false))

    override val isUnlocked: Flow<Boolean> = unlocked

    override suspend fun setUnlocked(unlocked: Boolean) {
        preferences.edit().putBoolean(KEY_UNLOCKED, unlocked).apply()
        this.unlocked.value = unlocked
    }

    private companion object {
        const val KEY_UNLOCKED = "lifetime_unlocked"
    }
}
