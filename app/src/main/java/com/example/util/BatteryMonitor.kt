package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class BatteryInfo(
    val percentage: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val temperatureCelsius: Float = 0f,
    val voltageMv: Int = 0
) {
    val level: Int get() = percentage

    val isLowBattery: Boolean
        get() = percentage <= 20 && !isCharging

    val isCriticalBattery: Boolean
        get() = percentage <= 10 && !isCharging

    fun isSaverActive(saverFeatureEnabled: Boolean): Boolean {
        return saverFeatureEnabled && (isLowBattery || isPowerSaveMode)
    }

    fun getEffectiveFps(configuredFps: Int, saverFeatureEnabled: Boolean, saverFpsCap: Int = 2): Int {
        return if (isSaverActive(saverFeatureEnabled)) {
            configuredFps.coerceAtMost(saverFpsCap).coerceAtLeast(1)
        } else {
            configuredFps.coerceIn(1, 15)
        }
    }
}

object BatteryMonitor {

    fun getCurrentBatteryInfo(context: Context): BatteryInfo {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isPowerSaveMode = powerManager?.isPowerSaveMode ?: false

            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, filter)

            if (batteryStatus != null) {
                val level: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val temp: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                val voltage: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

                val percentage = if (level >= 0 && scale > 0) {
                    ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
                } else {
                    100
                }

                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                BatteryInfo(
                    percentage = percentage,
                    isCharging = isCharging,
                    isPowerSaveMode = isPowerSaveMode,
                    temperatureCelsius = temp / 10f,
                    voltageMv = voltage
                )
            } else {
                BatteryInfo(
                    percentage = 100,
                    isCharging = false,
                    isPowerSaveMode = isPowerSaveMode
                )
            }
        } catch (_: Exception) {
            BatteryInfo()
        }
    }

    fun observeBatteryInfo(context: Context): Flow<BatteryInfo> = callbackFlow {
        val appContext = context.applicationContext

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                trySend(getCurrentBatteryInfo(appContext))
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }

        appContext.registerReceiver(receiver, filter)
        // Emit initial value
        trySend(getCurrentBatteryInfo(appContext))

        awaitClose {
            try {
                appContext.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()
}
