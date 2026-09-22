package dev.thomas_kiljanczyk.openpiano.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.thomas_kiljanczyk.openpiano.core.data.repository.KeyboardSettingsRepository
import dev.thomas_kiljanczyk.openpiano.core.data.repository.KeyboardSettingsRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindsKeyboardSettingsRepository(
        impl: KeyboardSettingsRepositoryImpl,
    ): KeyboardSettingsRepository
}
