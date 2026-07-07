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
    }

    private fun createPill() {
        val inflater = LayoutInflater.from(this)
        pillView = inflater.inflate(R.layout.pill_layout, null)

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
        if (::pillView.isInitialized) windowManager.removeView(pillView)
    }

    override fun onBind(intent: Intent?) = null
}