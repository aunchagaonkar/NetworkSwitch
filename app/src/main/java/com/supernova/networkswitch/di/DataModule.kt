package com.supernova.networkswitch.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.supernova.networkswitch.data.repository.NetworkControlRepositoryImpl
import com.supernova.networkswitch.data.repository.PreferencesRepositoryImpl
import com.supernova.networkswitch.data.repository.SimRepositoryImpl
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.repository.SimRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "network_switch_preferences")

/**
 * Dependency injection module for data layer
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    
    @Binds
    @Singleton
    abstract fun bindNetworkControlRepository(
        networkControlRepositoryImpl: NetworkControlRepositoryImpl
    ): NetworkControlRepository
    
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository
    
    @Binds
    @Singleton
    abstract fun bindSimRepository(
        simRepositoryImpl: SimRepositoryImpl
    ): SimRepository
    
    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
            return context.dataStore
        }
    }
}
