package dev.thomas_kiljanczyk.openpiano.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.thomas_kiljanczyk.openpiano.data.LocaleManagerImpl
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LocaleManager

@Module
@InstallIn(SingletonComponent::class)
interface LocaleModule {

    @Binds
    fun bindLocaleManager(impl: LocaleManagerImpl): LocaleManager
}
