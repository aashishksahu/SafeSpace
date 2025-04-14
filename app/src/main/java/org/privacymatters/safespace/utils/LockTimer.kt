package org.privacymatters.safespace.utils

import android.app.Activity
import android.content.Intent
import android.os.CountDownTimer
import org.privacymatters.safespace.AuthActivity
import org.privacymatters.safespace.main.DataManager

class LockTimer {

    companion object {
        val ops = DataManager

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
                // if the lock is due to going back from a pinned item, it will always go to mainn activity
                if (!ops.lockItem) firstActivity = false
                activity.startActivity(intent)
            }
        }

        fun stop() {
            timer?.cancel()
        }

        fun removeLock() {
            ops.lockItem =
                false // unlock item opened using pin icon after successful authentication
            isLocked = false
        }

        fun setLockManually() {
            isLocked = true
        }


    }


}