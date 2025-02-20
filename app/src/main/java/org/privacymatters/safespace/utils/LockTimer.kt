package org.privacymatters.safespace.utils

import android.app.Activity
import android.content.Intent
import android.os.CountDownTimer
import org.privacymatters.safespace.AuthActivity

class LockTimer {

    companion object {

        private var timer: CountDownTimer? = null

        private var isLocked = false

        var firstActivity = true

        fun start() {
            timer = object : CountDownTimer(300000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                }

                override fun onFinish() {
                    isLocked = true
                }
            }.start()
        }

        fun checkLock(activity: Activity) {
            if (isLocked) {
                val intent = Intent(activity.applicationContext, AuthActivity::class.java)
                firstActivity = false
                activity.startActivity(intent)
            }
        }

        fun stop() {
            timer?.cancel()
        }

        fun removeLock() {
            isLocked = false
        }


    }


}