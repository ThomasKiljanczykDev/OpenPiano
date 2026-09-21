package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOptionsProvider
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.ResourceLanguageOptionsProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    abstract fun bindsLanguageOptionsProvider(impl: ResourceLanguageOptionsProvider): LanguageOptionsProvider
}
