package com.example.djremixpro.core.model

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val languageTag: String = "vi",
    val latencyMs: Int = 40,
    val precue: Boolean = false,
    val masterVolumePct: Int = 80,
    val recordFormat: RecordFormat = RecordFormat.MP3,
    val recordQualityKbps: Int = 320,
    val saveFolder: String = "Music/MixDeck",
    val pitchRangePct: Int = 8,
    val defaultJogMode: DeckMode = DeckMode.JOG,
    val haptic: Boolean = true,
    val keepScreenOn: Boolean = true,
    val onboardingDone: Boolean = false,
)

/** Choices offered by the single-choice dialogs of the settings screen (D-14). */
object SettingsOptions {
    val latencyMs = listOf(20, 40, 60, 80, 100, 150, 200)
    val masterVolumePct = listOf(50, 60, 70, 80, 90, 100)
    val recordQualityKbps = listOf(128, 192, 256, 320)
    val pitchRangePct = listOf(6, 8, 10, 16, 50)
}

data class AppLanguage(val tag: String, val nativeName: String, val vietnameseName: String)

/** LANGS of App.dc.html:284, in design order. */
object Languages {
    val all: List<AppLanguage> = listOf(
        AppLanguage("vi", "Tiếng Việt", "Tiếng Việt"),
        AppLanguage("hi", "हिन्दी", "Tiếng Hindi"),
        AppLanguage("es", "Español", "Tiếng Tây Ban Nha"),
        AppLanguage("pt-BR", "Português (Brasil)", "Tiếng Bồ Đào Nha (Brazil)"),
        AppLanguage("en", "English", "Tiếng Anh"),
        AppLanguage("pt-PT", "Português (Portugal)", "Tiếng Bồ Đào Nha (Bồ Đào Nha)"),
        AppLanguage("fr", "Français", "Tiếng Pháp"),
        AppLanguage("ar", "العربية", "Tiếng Ả Rập"),
        AppLanguage("bn", "বাংলা", "Tiếng Bengal"),
        AppLanguage("ru", "Русский", "Tiếng Nga"),
        AppLanguage("de", "Deutsch", "Tiếng Đức"),
        AppLanguage("ja", "日本語", "Tiếng Nhật"),
        AppLanguage("tr", "Türkçe", "Tiếng Thổ Nhĩ Kỳ"),
        AppLanguage("ko", "한국어", "Tiếng Hàn"),
        AppLanguage("id", "Bahasa Indonesia", "Tiếng Indonesia"),
        AppLanguage("zh-Hans", "简体中文", "Tiếng Trung (Giản thể)"),
        AppLanguage("zh-Hant", "繁體中文", "Tiếng Trung (Phồn thể)"),
    )

    fun find(tag: String): AppLanguage? = all.firstOrNull { it.tag == tag }
}
