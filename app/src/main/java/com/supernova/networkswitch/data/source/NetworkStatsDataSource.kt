package com.supernova.networkswitch.data.source

import android.content.Context
import android.net.TrafficStats
import android.os.Build
import android.telephony.*
import com.supernova.networkswitch.domain.model.NetworkStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStatsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    
    private var lastRxBytes: Long = 0
    private var lastTxBytes: Long = 0
    private var lastTime: Long = 0
    
    private var currentSignalStrengthDbm: Int = 0
    private var currentSignalLevel: Int = 0

    fun observeNetworkStats(): Flow<NetworkStats> = flow {
        setupSignalStrengthListener()
        
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTime = System.currentTimeMillis()

        while (true) {
            val currentTime = System.currentTimeMillis()
            val currentRxBytes = TrafficStats.getTotalRxBytes()
            val currentTxBytes = TrafficStats.getTotalTxBytes()
            
            val timeDiff = (currentTime - lastTime) / 1000.0
            if (timeDiff >= 0.5) { // Update at least every 0.5s
                val rxDiff = currentRxBytes - lastRxBytes
                val txDiff = currentTxBytes - lastTxBytes
                
                val downloadSpeedBps = if (lastRxBytes > 0 && rxDiff >= 0) (rxDiff * 8 / timeDiff).toLong() else 0
                val uploadSpeedBps = if (lastTxBytes > 0 && txDiff >= 0) (txDiff * 8 / timeDiff).toLong() else 0
                
                emit(NetworkStats(
                    downloadSpeedBps = downloadSpeedBps,
                    uploadSpeedBps = uploadSpeedBps,
                    signalStrengthDbm = currentSignalStrengthDbm,
                    signalLevel = currentSignalLevel
                ))
                
                lastRxBytes = currentRxBytes
                lastTxBytes = currentTxBytes
                lastTime = currentTime
            }
            
            delay(1000)
        }
    }

    private fun setupSignalStrengthListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        updateSignalMetrics(signalStrength)
                    }
                }
            )
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    updateSignalMetrics(signalStrength)
                }
            }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        }
    }

    private fun updateSignalMetrics(signalStrength: SignalStrength) {
        currentSignalLevel = signalStrength.level
        
        // Try to get dBm from LTE, NR, or WCDMA
        val cellSignalStrengths = signalStrength.cellSignalStrengths
        for (css in cellSignalStrengths) {
            if (css.dbm != CellInfo.UNAVAILABLE) {
                currentSignalStrengthDbm = css.dbm
                break
            }
        }
    }
}
