package com.example.djremixpro.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.model.DeckMode
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/** R08 / D-08: cài đặt và cờ onboarding lưu thật bằng DataStore; lỗi đọc → giá trị mặc định. */
class DataStoreSettingsRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    private fun store(file: File): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    private fun file() = File(tmp.root, "settings.preferences_pb")

    @Test
    fun emptyStore_emitsDefaults() = runBlocking {
        assertEquals(AppSettings(), DataStoreSettingsRepository(store(file())).settings.first())
    }

    @Test
    fun update_roundTripsEveryField() = runBlocking {
        val repo = DataStoreSettingsRepository(store(file()))
        val changed = AppSettings(
            themeMode = ThemeMode.SYSTEM, languageTag = "zh-Hant", latencyMs = 150, precue = true,
            masterVolumePct = 60, recordFormat = RecordFormat.WAV, recordQualityKbps = 128,
            saveFolder = "Music/MixDeck", pitchRangePct = 50, defaultJogMode = DeckMode.PAD,
            haptic = false, keepScreenOn = false, onboardingDone = true,
        )
        repo.update { changed }
        assertEquals(changed, repo.settings.first())
    }

    @Test
    fun values_surviveNewRepositoryInstance() = runBlocking {
        val f = file()
        DataStoreSettingsRepository(store(f)).update { it.copy(onboardingDone = true, themeMode = ThemeMode.LIGHT) }
        // Mô phỏng tắt app: DataStore cũ phải đóng hẳn trước khi mở lại cùng tệp.
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
        scopes.clear()

        val reopened = DataStoreSettingsRepository(store(f)).settings.first()
        assertEquals(true, reopened.onboardingDone)
        assertEquals(ThemeMode.LIGHT, reopened.themeMode)
    }

    @Test
    fun update_transformReceivesStoredValue() = runBlocking {
        // Chỉ ghi một lần mỗi tệp: DataStore trên JVM Windows không rename đè được tệp đã có
        // ("Unable to rename …preferences_pb.tmp"), hạn chế môi trường test, không xảy ra trên Android.
        val repo = DataStoreSettingsRepository(store(file()))
        var seen: AppSettings? = null
        repo.update { seen = it; it.copy(precue = !it.precue) }
        assertEquals(AppSettings(), seen)
        assertEquals(true, repo.settings.first().precue)
    }

    @Test
    fun unknownEnumValue_fallsBackToDefault() = runBlocking {
        val ds = store(file())
        ds.edit { it[stringPreferencesKey("theme_mode")] = "PURPLE" }
        assertEquals(ThemeMode.DARK, DataStoreSettingsRepository(ds).settings.first().themeMode)
    }

    @Test
    fun ioErrorOnRead_emitsDefaults_insteadOfThrowing() = runBlocking {
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("unreadable") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("unwritable")
        }
        assertEquals(AppSettings(), DataStoreSettingsRepository(broken).settings.first())
    }
}
