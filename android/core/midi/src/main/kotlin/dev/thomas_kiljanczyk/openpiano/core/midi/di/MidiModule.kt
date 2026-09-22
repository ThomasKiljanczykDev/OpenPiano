package dev.thomas_kiljanczyk.openpiano.core.midi.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.thomas_kiljanczyk.openpiano.core.midi.MidiOutputPort
import dev.thomas_kiljanczyk.openpiano.core.midi.UsbMidiOutputPort
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MidiModule {

    @Binds
    @Singleton
    abstract fun bindsMidiOutputPort(port: UsbMidiOutputPort): MidiOutputPort
}
