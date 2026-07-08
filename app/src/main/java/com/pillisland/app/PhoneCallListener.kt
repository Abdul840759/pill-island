package com.pillisland.app

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

class PhoneCallListener(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var listener: PhoneStateListener? = null

    fun start() {
        listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        PillOverlayService.instance?.showIncomingCall(
                            phoneNumber ?: "Unknown"
                        )
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        PillOverlayService.instance?.showOngoingCall()
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        PillOverlayService.instance?.hideCallUI()
                    }
                }
            }
        }
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    fun stop() {
        listener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
    }
}