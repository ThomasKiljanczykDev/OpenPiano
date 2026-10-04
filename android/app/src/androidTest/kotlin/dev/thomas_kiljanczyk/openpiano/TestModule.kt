package dev.thomas_kiljanczyk.openpiano

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dev.thomas_kiljanczyk.openpiano.core.audio.AudioEngine
import dev.thomas_kiljanczyk.openpiano.core.audio.di.AudioModule
import dev.thomas_kiljanczyk.openpiano.core.data.di.RepositoryModule
import dev.thomas_kiljanczyk.openpiano.core.data.repository.UserPreferencesRepository
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeAudioEngine
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeUserPreferencesRepository
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class, AudioModule::class])
object TestModule {

    @Provides
    @Singleton
    fun providesFakeRepository(): FakeUserPreferencesRepository = FakeUserPreferencesRepository()

    @Provides
    fun providesRepository(fake: FakeUserPreferencesRepository): UserPreferencesRepository = fake

    @Provides
    @Singleton
    fun providesFakeAudioEngine(): FakeAudioEngine = FakeAudioEngine()

    @Provides
    fun providesAudioEngine(fake: FakeAudioEngine): AudioEngine = fake
}
