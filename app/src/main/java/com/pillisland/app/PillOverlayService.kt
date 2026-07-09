package com.pillisland.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.*
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.core.app.NotificationCompat

class PillOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var pillView: View
    private var isExpanded = false
    private val CHANNEL_ID = "pill_island_channel"
    private val chargingReceiver = ChargingReceiver()
    var latestNotifTitle = ""
    var latestNotifText = ""

    companion object {
        var instance: PillOverlayService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(1, buildForegroundNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createPill()

        callListener = PhoneCallListener(this)
        callListener?.start()

        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(chargingReceiver, filter)
    }

    private fun createPill() {
        val inflater = LayoutInflater.from(this)
        pillView = inflater.inflate(R.layout.pill_layout, null)

        val sizeMultiplier = PillPrefs.getSize(this)
        val shape = PillPrefs.getShape(this)
        val pillContainer = pillView.findViewById<LinearLayout>(R.id.pill_container)
        val baseSizeDp = 40
        val newHeightDp = (baseSizeDp * sizeMultiplier).toInt()
        val density = resources.displayMetrics.density
        pillContainer.layoutParams.height = (newHeightDp * density).toInt()

        if (shape == "android") {
            pillContainer.setBackgroundResource(R.drawable.circle_background)
            pillContainer.layoutParams.width = (newHeightDp * density).toInt()
        } else {
            pillContainer.setBackgroundResource(R.drawable.pill_background)
            val pillWidthDp = (baseSizeDp * 2.4f * sizeMultiplier).toInt()
            pillContainer.layoutParams.width = (pillWidthDp * density).toInt()
        }
        pillContainer.requestLayout()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 8

        windowManager.addView(pillView, params)
        setupPillInteractions(params)
    }

    private fun setupPillInteractions(params: WindowManager.LayoutParams) {
        val pillContainer = pillView.findViewById<LinearLayout>(R.id.pill_container)
        val expandedPanel = pillView.findViewById<LinearLayout>(R.id.expanded_panel)
        val notifTitle = pillView.findViewById<TextView>(R.id.notif_title)
        val notifText = pillView.findViewById<TextView>(R.id.notif_text)

        var longPressHandler = Handler(Looper.getMainLooper())
        var isLongPress = false

        pillContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPress = false
                    longPressHandler.postDelayed({
                        isLongPress = true
                        toggleExpand(expandedPanel, notifTitle, notifText)
                    }, 600)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressHandler.removeCallbacksAndMessages(null)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacksAndMessages(null)
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleExpand(
        panel: LinearLayout,
        title: TextView,
        text: TextView
    ) {
        isExpanded = !isExpanded
        if (isExpanded) {
            title.text = if (latestNotifTitle.isEmpty()) "No notifications" else latestNotifTitle
            text.text = latestNotifText
            panel.visibility = View.VISIBLE
            panel.alpha = 0f
            panel.animate().alpha(1f).setDuration(250)
                .setInterpolator(OvershootInterpolator()).start()
        } else {
            panel.animate().alpha(0f).setDuration(200).withEndAction {
                panel.visibility = View.GONE
            }.start()
        }
    }

    fun updateNotification(title: String, text: String) {
        latestNotifTitle = title
        latestNotifText = text
        pillView.post {
            val dot = pillView.findViewById<View>(R.id.notif_dot)
            dot?.visibility = View.VISIBLE
        }
    }


    private var callListener: PhoneCallListener? = null
    private var isRingerSilenced = false

    fun showIncomingCall(number: String) {
        isRingerSilenced = false
        pillView.post {
            val title = pillView.findViewById<TextView>(R.id.notif_title)
            val text = pillView.findViewById<TextView>(R.id.notif_text)
            val buttons = pillView.findViewById<LinearLayout>(R.id.call_buttons)
            val rejectBtn = pillView.findViewById<Button>(R.id.btn_reject)

            title.text = "Incoming Call"
            text.text = number
            buttons.visibility = View.VISIBLE
            expandPillForCall()

            rejectBtn.setOnClickListener {
                silenceRinger()
            }
        }
    }

    private fun answerCall() {
        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        this, android.Manifest.permission.ANSWER_PHONE_CALLS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    telecomManager.acceptRingingCall()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showOngoingCall() {
        pillView.post {
            val title = pillView.findViewById<TextView>(R.id.notif_title)
            title.text = "Call in progress"
        }
    }

    fun hideCallUI() {
        pillView.post {
            val panel = pillView.findViewById<LinearLayout>(R.id.expanded_panel)
            val buttons = pillView.findViewById<LinearLayout>(R.id.call_buttons)
            panel.visibility = View.GONE
            buttons.visibility = View.GONE
            isExpanded = false
        }
    }

    private fun expandPillForCall() {
        val panel = pillView.findViewById<LinearLayout>(R.id.expanded_panel)
        panel.visibility = View.VISIBLE
        isExpanded = true
    }

    fun silenceRinger() {
        if (!isRingerSilenced) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
            isRingerSilenced = true
        }
    }

    fun showCharging(percent: Int) {
        pillView.post {
            val title = pillView.findViewById<TextView>(R.id.notif_title)
            val text = pillView.findViewById<TextView>(R.id.notif_text)
            title.text = "Charging"
            text.text = "$percent%"
            val panel = pillView.findViewById<LinearLayout>(R.id.expanded_panel)
            panel.visibility = View.VISIBLE
            isExpanded = true
        }
    }

    fun hideCharging() {
        pillView.post {
            val panel = pillView.findViewById<LinearLayout>(R.id.expanded_panel)
            panel.visibility = View.GONE
            isExpanded = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Pill Island Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Keeps Pill Island running" }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pill Island Active")
            .setContentText("Tap to open")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        unregisterReceiver(chargingReceiver)
        if (::pillView.isInitialized) windowManager.removeView(pillView)
    }

    override fun onBind(intent: Intent?) = null
}