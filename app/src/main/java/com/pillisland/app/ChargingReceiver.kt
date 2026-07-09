package com.pillisland.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val batteryIntent = context.registerReceiver(
                    null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
                PillOverlayService.instance?.showCharging(percent)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                PillOverlayService.instance?.hideCharging()
            }
        }
    }
}