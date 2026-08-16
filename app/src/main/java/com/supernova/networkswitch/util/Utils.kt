package com.supernova.networkswitch.util

import com.topjohnwu.superuser.Shell

object Utils {
    fun isRootGranted(): Boolean {
        Shell.getShell()
        return Shell.isAppGrantedRoot() == true
    }

    /**
     * Format bits per second to human readable string (example: 1.2 Mbps)
     */
    fun formatSpeed(bps: Long): String {
        return when {
            bps >= 1_000_000_000 -> String.format("%.1f Gbps", bps / 1_000_000_000.0)
            bps >= 1_000_000 -> String.format("%.1f Mbps", bps / 1_000_000.0)
            bps >= 1_000 -> String.format("%.1f Kbps", bps / 1_000.0)
            else -> "$bps bps"
        }
    }
}
