package dev.thomas_kiljanczyk.openpiano.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.thomas_kiljanczyk.openpiano.core.data.repository.UserPreferencesRepository
import dev.thomas_kiljanczyk.openpiano.core.data.repository.UserPreferencesRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindsUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl,
    ): UserPreferencesRepository
}
