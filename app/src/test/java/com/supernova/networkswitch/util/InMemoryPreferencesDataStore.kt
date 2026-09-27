package com.supernova.networkswitch.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory DataStore for tests that need real read-modify-write behaviour, which a
 * mocked DataStore cannot express.
 */
class InMemoryPreferencesDataStore(
    initial: Preferences = emptyPreferences()
) : DataStore<Preferences> {

    private val state = MutableStateFlow(initial)
    private val writeLock = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences
    ): Preferences = writeLock.withLock {
        val updated = transform(state.value)
        state.value = updated
        updated
    }
}
