package dev.thomas_kiljanczyk.openpiano.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.thomas_kiljanczyk.openpiano.core.data.touch.AndroidTouchAreaSupport
import dev.thomas_kiljanczyk.openpiano.core.data.touch.TouchAreaSupport
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TouchModule {

    @Binds
    @Singleton
    abstract fun bindsTouchAreaSupport(impl: AndroidTouchAreaSupport): TouchAreaSupport
}
