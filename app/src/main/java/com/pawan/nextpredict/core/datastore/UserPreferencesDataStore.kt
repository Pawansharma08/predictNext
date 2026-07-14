package com.pawan.nextpredict.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nextpredict_prefs"
)

/**
 * DataStore wrapper for all user preferences.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val dataStore = context.dataStore

    // ─── Keys ──────────────────────────────────────────────────────────────────
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_DYNAMIC_COLOR = booleanPreferencesKey("is_dynamic_color")
        val IS_ONBOARDING_DONE = booleanPreferencesKey("is_onboarding_done")
        val DEFAULT_WATCHLIST_ID = longPreferencesKey("default_watchlist_id")
    }

    // ─── Theme ─────────────────────────────────────────────────────────────────

    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { Timber.e(it, "DataStore read error"); emit(emptyPreferences()) }
        .map { prefs ->
            ThemeMode.fromString(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    val isDynamicColor: Flow<Boolean> = dataStore.data
        .catch { Timber.e(it, "DataStore read error"); emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.IS_DYNAMIC_COLOR] ?: true }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_DYNAMIC_COLOR] = enabled
        }
    }

    // ─── Onboarding ────────────────────────────────────────────────────────────

    val isOnboardingDone: Flow<Boolean> = dataStore.data
        .catch { Timber.e(it, "DataStore read error"); emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.IS_ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_ONBOARDING_DONE] = done
        }
    }

    // ─── Watchlist ─────────────────────────────────────────────────────────────

    val defaultWatchlistId: Flow<Long> = dataStore.data
        .catch { Timber.e(it, "DataStore read error"); emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.DEFAULT_WATCHLIST_ID] ?: -1L }

    suspend fun setDefaultWatchlistId(id: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_WATCHLIST_ID] = id
        }
    }
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun fromString(value: String): ThemeMode =
            values().firstOrNull { it.name == value } ?: SYSTEM
    }
}
