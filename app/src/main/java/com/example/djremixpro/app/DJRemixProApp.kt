package com.example.djremixpro.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.djremixpro.core.model.ThemeMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DJRemixProApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Apply the stored theme before the first Activity inflates, so there is no light flash (D-16).
        val initial = runBlocking { container.settingsRepository.settings.first() }
        applyTheme(initial.themeMode)
        applyLanguage(initial.languageTag)

        container.appScope.launch {
            container.settingsRepository.settings
                .map { it.themeMode }
                .distinctUntilChanged()
                .collect(::applyTheme)
        }
        container.appScope.launch {
            container.settingsRepository.settings
                .map { it.languageTag }
                .distinctUntilChanged()
                .collect(::applyLanguage)
        }
    }

    private fun applyTheme(mode: ThemeMode) {
        val nightMode = when (mode) {
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    /** Only Vietnamese strings exist; the locale still drives layout direction and system formats (D-15). */
    private fun applyLanguage(tag: String) {
        val current = AppCompatDelegate.getApplicationLocales()
        // Default strings are already Vietnamese: forcing "vi" on a fresh install only recreates the
        // first Activity (a black frame between the system splash and onboarding, VIS-13).
        if (current.isEmpty && tag == DEFAULT_LANGUAGE) return
        val locales = LocaleListCompat.forLanguageTags(tag)
        if (current.toLanguageTags() != locales.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    private companion object {
        const val DEFAULT_LANGUAGE = "vi"
    }
}
