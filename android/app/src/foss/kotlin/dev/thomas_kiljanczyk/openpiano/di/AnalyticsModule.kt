package dev.thomas_kiljanczyk.openpiano.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.thomas_kiljanczyk.openpiano.core.analytics.AnalyticsHelper
import dev.thomas_kiljanczyk.openpiano.core.analytics.NoOpAnalyticsHelper
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsModule {

    @Provides
    @Singleton
    fun providesAnalyticsHelper(): AnalyticsHelper = NoOpAnalyticsHelper
}
