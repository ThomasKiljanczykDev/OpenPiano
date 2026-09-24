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
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.UserPreferences
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.UserPreferencesSerializer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Singleton

private const val USER_PREFERENCES_FILE = "user_preferences.pb"

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providesUserPreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(OpenPianoDispatcher.IO) dispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<UserPreferences> = DataStoreFactory.create(
        serializer = UserPreferencesSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            UserPreferences.getDefaultInstance()
        },
        scope = scope + dispatcher,
        produceFile = { context.dataStoreFile(USER_PREFERENCES_FILE) },
    )
}
