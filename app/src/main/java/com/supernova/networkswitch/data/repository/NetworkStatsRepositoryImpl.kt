package com.supernova.networkswitch.data.repository

import com.supernova.networkswitch.data.source.NetworkStatsDataSource
import com.supernova.networkswitch.domain.model.NetworkStats
import com.supernova.networkswitch.domain.repository.NetworkStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStatsRepositoryImpl @Inject constructor(
    private val dataSource: NetworkStatsDataSource
) : NetworkStatsRepository {
    override fun observeNetworkStats(): Flow<NetworkStats> {
        return dataSource.observeNetworkStats()
    }
}
