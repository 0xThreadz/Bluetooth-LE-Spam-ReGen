package de.simon.dankelmann.bluetoothlespam.ui.clone

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CloneViewModel : ViewModel() {
    val isListening = MutableLiveData<Boolean>(false)
}
