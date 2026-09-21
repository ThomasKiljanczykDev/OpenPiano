package dev.thomas_kiljanczyk.openpiano.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.thomas_kiljanczyk.openpiano.core.common.di.ApplicationScope
import dev.thomas_kiljanczyk.openpiano.core.common.di.Dispatcher
import dev.thomas_kiljanczyk.openpiano.core.common.di.OpenPianoDispatcher
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.KeyboardSettingsSerializer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Singleton

private const val KEYBOARD_SETTINGS_FILE = "keyboard_settings.pb"

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providesKeyboardSettingsDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(OpenPianoDispatcher.IO) dispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<KeyboardSettings> = DataStoreFactory.create(
        serializer = KeyboardSettingsSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            KeyboardSettings.getDefaultInstance()
        },
        scope = scope + dispatcher,
        produceFile = { context.dataStoreFile(KEYBOARD_SETTINGS_FILE) },
    )
}
