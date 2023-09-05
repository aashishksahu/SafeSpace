package org.privacymatters.safespace.cameraUtils

import android.app.Application
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.privacymatters.safespace.R
import java.util.Timer
import kotlin.concurrent.timerTask

class CameraViewModel(private val application: Application) : AndroidViewModel(application) {

    private var timer: Timer? = null
    private var timeCount = 0u

    fun startTimer(timerText: TextView) {
        timer = Timer()
        viewModelScope.launch(Dispatchers.IO) {
            timer?.scheduleAtFixedRate(timerTask {
                timeCount += 1u
                val tempTimerText = (timeCount / 60u).toString().padStart(2, '0') + ":" +
                        (timeCount % 60u).toString().padStart(2, '0')
                timerText.text = tempTimerText
            }, 1000, 1000)
        }

    }

    fun stopTimer(timerText: TextView) {
        if (timer != null) {
            timer?.cancel()
            timer?.purge()
            timerText.text = application.getString(R.string.video_timer)
        }
    }

}