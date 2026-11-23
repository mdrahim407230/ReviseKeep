package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.security.CryptoManager
import com.example.i18n.AppLang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "revisekeep_prefs")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AccentPreset(val key: String) {
    MIDNIGHT_TEAL("midnight_teal"),
    OCEAN_SAPPHIRE("ocean_sapphire"),
    WARM_DAWN("warm_dawn"),
    SOFT_EMERALD("soft_emerald");

    companion object {
        fun fromKey(key: String?): AccentPreset =
            entries.firstOrNull { it.key == key } ?: MIDNIGHT_TEAL
    }
}

data class AppSettings(
    val language: AppLang = AppLang.EN,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPreset: AccentPreset = AccentPreset.MIDNIGHT_TEAL,
    val fontScale: Float = 1.0f,
    val soundFeedbackEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val isStudyingFocusMode: Boolean = false,
    val dailyNotificationCap: Int = 5,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val snoozedUntilMillis: Long = 0L,
    val notificationsSentDateKey: String = "",
    val notificationsSentToday: Int = 0,
    val consecutiveDismissals: Int = 0,
    val adaptiveThrottled: Boolean = false,
    val encryptedApiKeyBlob: String = "",
    val aiStylePrompt: String = "",
    val sendFullNoteToAi: Boolean = false,
    val cachedModelId: String = "",
    val cachedModelAtMillis: Long = 0L,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastRevisionDayKey: String = "",
    val pinHash: String = "",
    val pinSalt: String = "",
    val biometricEnabled: Boolean = false,
    val developerTimeOffsetMillis: Long = 0L,
    val developerMenuUnlocked: Boolean = false,
    val lastCloudSyncAtMillis: Long = 0L
) {
    val hasApiKey: Boolean get() = encryptedApiKeyBlob.isNotBlank()
    val hasPinLock: Boolean get() = pinHash.isNotBlank() && pinSalt.isNotBlank()
}

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val STUDYING_FOCUS = booleanPreferencesKey("studying_focus")
        val DAILY_CAP = intPreferencesKey("daily_cap")
        val QUIET_START = intPreferencesKey("quiet_start")
        val QUIET_END = intPreferencesKey("quiet_end")
        val SNOOZED_UNTIL = longPreferencesKey("snoozed_until")
        val SENT_DATE_KEY = stringPreferencesKey("sent_date_key")
        val SENT_TODAY = intPreferencesKey("sent_today")
        val CONSECUTIVE_DISMISSALS = intPreferencesKey("consecutive_dismissals")
        val ADAPTIVE_THROTTLED = booleanPreferencesKey("adaptive_throttled")
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_api_key")
        val AI_STYLE_PROMPT = stringPreferencesKey("ai_style_prompt")
        val SEND_FULL_NOTE_TO_AI = booleanPreferencesKey("send_full_note_to_ai")
        val CACHED_MODEL_ID = stringPreferencesKey("cached_model_id")
        val CACHED_MODEL_AT = longPreferencesKey("cached_model_at")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val LAST_REVISION_DAY = stringPreferencesKey("last_revision_day")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val DEV_TIME_OFFSET = longPreferencesKey("dev_time_offset")
        val DEV_MENU_UNLOCKED = booleanPreferencesKey("dev_menu_unlocked")
        val LAST_CLOUD_SYNC_AT = longPreferencesKey("last_cloud_sync_at")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            language = if (prefs[Keys.LANGUAGE] == AppLang.BN.code) AppLang.BN else AppLang.EN,
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            accentPreset = AccentPreset.fromKey(prefs[Keys.ACCENT_PRESET]),
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            soundFeedbackEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            isStudyingFocusMode = prefs[Keys.STUDYING_FOCUS] ?: false,
            dailyNotificationCap = prefs[Keys.DAILY_CAP] ?: 5,
            quietStartHour = prefs[Keys.QUIET_START] ?: 22,
            quietEndHour = prefs[Keys.QUIET_END] ?: 7,
            snoozedUntilMillis = prefs[Keys.SNOOZED_UNTIL] ?: 0L,
            notificationsSentDateKey = prefs[Keys.SENT_DATE_KEY] ?: "",
            notificationsSentToday = prefs[Keys.SENT_TODAY] ?: 0,
            consecutiveDismissals = prefs[Keys.CONSECUTIVE_DISMISSALS] ?: 0,
            adaptiveThrottled = prefs[Keys.ADAPTIVE_THROTTLED] ?: false,
            encryptedApiKeyBlob = prefs[Keys.ENCRYPTED_API_KEY] ?: "",
            aiStylePrompt = prefs[Keys.AI_STYLE_PROMPT] ?: "",
            sendFullNoteToAi = prefs[Keys.SEND_FULL_NOTE_TO_AI] ?: false,
            cachedModelId = prefs[Keys.CACHED_MODEL_ID] ?: "",
            cachedModelAtMillis = prefs[Keys.CACHED_MODEL_AT] ?: 0L,
            currentStreak = prefs[Keys.CURRENT_STREAK] ?: 0,
            bestStreak = prefs[Keys.BEST_STREAK] ?: 0,
            lastRevisionDayKey = prefs[Keys.LAST_REVISION_DAY] ?: "",
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            developerTimeOffsetMillis = prefs[Keys.DEV_TIME_OFFSET] ?: 0L,
            developerMenuUnlocked = prefs[Keys.DEV_MENU_UNLOCKED] ?: false,
            lastCloudSyncAtMillis = prefs[Keys.LAST_CLOUD_SYNC_AT] ?: 0L
        )
    }

    suspend fun getSettingsSync(): AppSettings = settingsFlow.first()

    suspend fun setLanguage(lang: AppLang) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang.code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentPreset(preset: AccentPreset) {
        context.dataStore.edit { it[Keys.ACCENT_PRESET] = preset.key }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.35f) }
    }

    suspend fun setSoundFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = done }
    }

    suspend fun setStudyingFocusMode(active: Boolean) {
        context.dataStore.edit { it[Keys.STUDYING_FOCUS] = active }
    }

    suspend fun setDailyNotificationCap(cap: Int) {
        context.dataStore.edit { it[Keys.DAILY_CAP] = cap.coerceIn(1, 20) }
    }

    suspend fun setQuietHours(startHour: Int, endHour: Int) {
        context.dataStore.edit {
            it[Keys.QUIET_START] = startHour.coerceIn(0, 23)
            it[Keys.QUIET_END] = endHour.coerceIn(0, 23)
        }
    }

    suspend fun setSnoozedUntil(untilMillis: Long) {
        context.dataStore.edit { it[Keys.SNOOZED_UNTIL] = untilMillis }
    }

    suspend fun recordNotificationSent(dateKey: String) {
        context.dataStore.edit { prefs ->
            val prevDate = prefs[Keys.SENT_DATE_KEY] ?: ""
            val prevCount = if (prevDate == dateKey) prefs[Keys.SENT_TODAY] ?: 0 else 0
            prefs[Keys.SENT_DATE_KEY] = dateKey
            prefs[Keys.SENT_TODAY] = prevCount + 1
        }
    }

    suspend fun incrementConsecutiveDismissals() {
        context.dataStore.edit { prefs ->
            val next = (prefs[Keys.CONSECUTIVE_DISMISSALS] ?: 0) + 1
            prefs[Keys.CONSECUTIVE_DISMISSALS] = next
            if (next >= 3) {
                prefs[Keys.ADAPTIVE_THROTTLED] = true
            }
        }
    }

    suspend fun resetDismissalThrottle() {
        context.dataStore.edit { prefs ->
            prefs[Keys.CONSECUTIVE_DISMISSALS] = 0
            prefs[Keys.ADAPTIVE_THROTTLED] = false
        }
    }

    suspend fun saveEncryptedApiKey(plainKey: String) {
        val blob = if (plainKey.isBlank()) "" else CryptoManager.encryptApiKeyWithKeystore(plainKey.trim())
        context.dataStore.edit {
            it[Keys.ENCRYPTED_API_KEY] = blob
            if (plainKey.isBlank()) {
                it[Keys.CACHED_MODEL_ID] = ""
                it[Keys.CACHED_MODEL_AT] = 0L
            }
        }
    }

    suspend fun getDecryptedApiKey(): String {
        val blob = getSettingsSync().encryptedApiKeyBlob
        return CryptoManager.decryptApiKeyWithKeystore(blob)
    }

    suspend fun saveAiStylePrompt(prompt: String) {
        context.dataStore.edit { it[Keys.AI_STYLE_PROMPT] = prompt }
    }

    suspend fun setSendFullNoteToAi(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SEND_FULL_NOTE_TO_AI] = enabled }
    }

    suspend fun saveCachedModel(modelId: String, timestamp: Long) {
        context.dataStore.edit {
            it[Keys.CACHED_MODEL_ID] = modelId
            it[Keys.CACHED_MODEL_AT] = timestamp
        }
    }

    suspend fun updateStreakStats(current: Int, best: Int, dayKey: String) {
        context.dataStore.edit {
            it[Keys.CURRENT_STREAK] = current
            it[Keys.BEST_STREAK] = best
            it[Keys.LAST_REVISION_DAY] = dayKey
            it[Keys.CONSECUTIVE_DISMISSALS] = 0
        }
    }

    suspend fun setPinLock(pin: String) {
        if (pin.isBlank()) {
            context.dataStore.edit {
                it[Keys.PIN_HASH] = ""
                it[Keys.PIN_SALT] = ""
                it[Keys.BIOMETRIC_ENABLED] = false
            }
        } else {
            val salt = CryptoManager.generateSaltBase64()
            val hash = CryptoManager.hashPin(pin, salt)
            context.dataStore.edit {
                it[Keys.PIN_SALT] = salt
                it[Keys.PIN_HASH] = hash
            }
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setDeveloperTimeOffset(offsetMillis: Long) {
        context.dataStore.edit { it[Keys.DEV_TIME_OFFSET] = offsetMillis }
    }

    suspend fun unlockDeveloperMenu() {
        context.dataStore.edit { it[Keys.DEV_MENU_UNLOCKED] = true }
    }

    suspend fun setLastCloudSyncAt(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_CLOUD_SYNC_AT] = timestamp }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
        CryptoManager.deleteKeystoreKey()
    }
}
