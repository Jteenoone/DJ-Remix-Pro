package com.example.djremixpro.app

import android.content.Context
import com.example.djremixpro.core.data.LearnRepository
import com.example.djremixpro.core.data.MixSessionRepository
import com.example.djremixpro.core.data.PlaybackRepository
import com.example.djremixpro.core.data.RecordingRepository
import com.example.djremixpro.core.data.SettingsRepository
import com.example.djremixpro.core.data.TrackRepository
import com.example.djremixpro.core.data.fake.FakeLearnRepository
import com.example.djremixpro.core.data.fake.FakeRecordingRepository
import com.example.djremixpro.core.data.fake.FakeTrackRepository
import com.example.djremixpro.core.data.fake.InMemoryMixSessionRepository
import com.example.djremixpro.core.data.fake.SimulatedPlaybackRepository
import com.example.djremixpro.core.data.settings.DataStoreSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual DI: one instance per process, shared by MainActivity and MixerActivity (DECISIONS D-08, D-11). */
class AppContainer(context: Context) {
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val trackRepository: TrackRepository = FakeTrackRepository()
    val recordingRepository: RecordingRepository = FakeRecordingRepository()
    val learnRepository: LearnRepository = FakeLearnRepository()
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context.applicationContext)
    val mixSessionRepository: MixSessionRepository = InMemoryMixSessionRepository(trackRepository)
    val playbackRepository: PlaybackRepository = SimulatedPlaybackRepository(appScope)
}
