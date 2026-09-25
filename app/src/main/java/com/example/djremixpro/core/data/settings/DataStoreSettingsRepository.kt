package com.example.djremixpro.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.djremixpro.core.data.SettingsRepository
import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.model.DeckMode
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Settings persisted with Preferences DataStore (D-08). Missing or unreadable values fall back to [AppSettings] defaults. */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toAppSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toAppSettings())) }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val LATENCY_MS = intPreferencesKey("latency_ms")
        val PRECUE = booleanPreferencesKey("precue")
        val MASTER_VOLUME_PCT = intPreferencesKey("master_volume_pct")
        val RECORD_FORMAT = stringPreferencesKey("record_format")
        val RECORD_QUALITY_KBPS = intPreferencesKey("record_quality_kbps")
        val SAVE_FOLDER = stringPreferencesKey("save_folder")
        val PITCH_RANGE_PCT = intPreferencesKey("pitch_range_pct")
        val DEFAULT_JOG_MODE = stringPreferencesKey("default_jog_mode")
        val HAPTIC = booleanPreferencesKey("haptic")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = enumOrNull<ThemeMode>(this[Keys.THEME_MODE]) ?: d.themeMode,
            languageTag = this[Keys.LANGUAGE_TAG] ?: d.languageTag,
            latencyMs = this[Keys.LATENCY_MS] ?: d.latencyMs,
            precue = this[Keys.PRECUE] ?: d.precue,
            masterVolumePct = this[Keys.MASTER_VOLUME_PCT] ?: d.masterVolumePct,
            recordFormat = enumOrNull<RecordFormat>(this[Keys.RECORD_FORMAT]) ?: d.recordFormat,
            recordQualityKbps = this[Keys.RECORD_QUALITY_KBPS] ?: d.recordQualityKbps,
            saveFolder = this[Keys.SAVE_FOLDER] ?: d.saveFolder,
            pitchRangePct = this[Keys.PITCH_RANGE_PCT] ?: d.pitchRangePct,
            defaultJogMode = enumOrNull<DeckMode>(this[Keys.DEFAULT_JOG_MODE]) ?: d.defaultJogMode,
            haptic = this[Keys.HAPTIC] ?: d.haptic,
            keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: d.keepScreenOn,
            onboardingDone = this[Keys.ONBOARDING_DONE] ?: d.onboardingDone,
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[Keys.THEME_MODE] = s.themeMode.name
        this[Keys.LANGUAGE_TAG] = s.languageTag
        this[Keys.LATENCY_MS] = s.latencyMs
        this[Keys.PRECUE] = s.precue
        this[Keys.MASTER_VOLUME_PCT] = s.masterVolumePct
        this[Keys.RECORD_FORMAT] = s.recordFormat.name
        this[Keys.RECORD_QUALITY_KBPS] = s.recordQualityKbps
        this[Keys.SAVE_FOLDER] = s.saveFolder
        this[Keys.PITCH_RANGE_PCT] = s.pitchRangePct
        this[Keys.DEFAULT_JOG_MODE] = s.defaultJogMode.name
        this[Keys.HAPTIC] = s.haptic
        this[Keys.KEEP_SCREEN_ON] = s.keepScreenOn
        this[Keys.ONBOARDING_DONE] = s.onboardingDone
    }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } }
}
