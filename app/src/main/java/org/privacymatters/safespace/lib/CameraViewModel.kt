package org.privacymatters.safespace.lib

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timerTask

class CameraViewModel(application: Application, private val initialTimerText: String) :
    AndroidViewModel(application) {

    private var timer: Timer? = null
    private var timerCounter = 0u

    var timerCounterText: MutableLiveData<String> = MutableLiveData()

    init {
        timerCounterText.value = initialTimerText
    }

    fun startTimer() {
        timer = Timer()
        viewModelScope.launch(Dispatchers.Main) {

            timer?.scheduleAtFixedRate(timerTask {
                timerCounter += 1u
                timerCounterText.postValue(
                    (timerCounter / 60u).toString()
                        .padStart(2, '0') + ":" + (timerCounter % 60u).toString().padStart(2, '0')
                )
            }, 1000, 1000)
        }
    }

    fun stopTimer() {
        if (timer != null) {
            timer?.cancel()
            timer?.purge()
            timerCounterText.postValue(initialTimerText)
            timerCounter = 0u
        }
    }


}