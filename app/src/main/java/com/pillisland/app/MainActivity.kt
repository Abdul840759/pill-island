package com.pillisland.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        when {
            !hasOverlayPermission() -> requestOverlayPermission()
            !isBatteryOptimizationIgnored() -> requestBatteryOptimizationExemption()
            else -> startPillService()
        }
    }

    private fun hasOverlayPermission(): Boolean =
        Settings.canDrawOverlays(this)

    private fun hasNotificationListenerPermission(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        )
        return flat != null && flat.contains(packageName)
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("Overlay Permission Needed")
            .setMessage("Pill Island needs to draw over other apps to show the pill. Tap OK to enable it in Settings.")
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 1001)
            }
            .setCancelable(false)
            .show()
    }

    private fun requestNotificationListenerPermission() {
        AlertDialog.Builder(this)
            .setTitle("Notification Access Needed")
            .setMessage("Pill Island needs notification access to show your notifications in the pill. Tap OK then find 'Pill Island' in the list and enable it.")
            .setPositiveButton("OK") { _, _ ->
                startActivityForResult(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), 1002
                )
            }
            .setCancelable(false)
            .show()
    }

    private fun requestBatteryOptimizationExemption() {
        AlertDialog.Builder(this)
            .setTitle("Battery Optimization")
            .setMessage("To keep Pill Island running always, please disable battery optimization for it. Tap OK to open settings.")
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 1003)
            }
            .setCancelable(false)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (hasOverlayPermission() && hasNotificationListenerPermission()) {
            startPillService()
        }
    }

    private fun startPillService() {
        val intent = Intent(this, PillOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        finish()
    }
}