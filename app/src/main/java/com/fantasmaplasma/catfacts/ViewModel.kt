package com.fantasmaplasma.catfacts
import android.content.res.Resources
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ViewModel: ViewModel() {
    val currentFactLiveData = MutableLiveData<String>()
    val currentFactIdx
        get() = repository.nextFactIdx-1
    private val repository = Repository()

    fun nextFact() {
        currentFactLiveData.postValue(
            repository.getNextFact()
        )
    }

    fun checkIfInitNeeded(resources: Resources) {
        repository.initFacts(resources)
    }

    fun setFact(idxOfLastSeenFact: Int) {
        currentFactLiveData.postValue(
            repository.setFact(idxOfLastSeenFact)
        )
    }

}