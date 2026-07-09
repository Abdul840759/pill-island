package com.pillisland.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        when {
            !hasOverlayPermission() -> requestOverlayPermission()
            !isBatteryOptimizationIgnored() -> requestBatteryOptimizationExemption()
            else -> showSettingsScreen()
        }
    }

    private fun showSettingsScreen() {
        setContentView(R.layout.activity_main)

        val seekBar = findViewById<SeekBar>(R.id.size_seekbar)
        val radioGroup = findViewById<RadioGroup>(R.id.shape_radio_group)
        val radioIos = findViewById<android.widget.RadioButton>(R.id.radio_ios)
        val radioAndroid = findViewById<android.widget.RadioButton>(R.id.radio_android)
        val saveBtn = findViewById<android.widget.Button>(R.id.btn_save)

        val currentSize = PillPrefs.getSize(this)
        seekBar.progress = ((currentSize - 0.7f) / (1.5f - 0.7f) * 100).toInt()

        if (PillPrefs.getShape(this) == "ios") {
            radioIos.isChecked = true
        } else {
            radioAndroid.isChecked = true
        }

        saveBtn.setOnClickListener {
            val sizeFraction = seekBar.progress / 100f
            val actualSize = 0.7f + (sizeFraction * (1.5f - 0.7f))
            PillPrefs.setSize(this, actualSize)

            val shape = if (radioGroup.checkedRadioButtonId == R.id.radio_ios) "ios" else "android"
            PillPrefs.setShape(this, shape)

            startPillService()
        }
    }

    private fun hasOverlayPermission(): Boolean =
        Settings.canDrawOverlays(this)

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

    private fun startPillService() {
        val intent = Intent(this, PillOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}