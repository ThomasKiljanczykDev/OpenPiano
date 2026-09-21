package dev.thomas_kiljanczyk.openpiano.core.common.di

import javax.inject.Qualifier

enum class OpenPianoDispatcher {
    Default,
    IO,
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: OpenPianoDispatcher)

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
