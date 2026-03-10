package com.amiunique.amiuniqueapp.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private var _totalFingerprints = MutableLiveData(60)
    private val totalFingerprints: LiveData<Int> get() = _totalFingerprints

    private var _totalApplications = MutableLiveData(60)
    private val totalApplications: LiveData<Int> get() = _totalApplications

    fun setTotalFingerprints(totalFingerprints : Int){
        _totalFingerprints.postValue(totalFingerprints)
    }
    fun getTotalFingerprints():Int{
        return totalFingerprints.value!!
    }

    fun setTotalApplications(totalApplications : Int){
        _totalApplications.postValue(totalApplications)
    }
    fun getTotalApplications():Int{
        return totalApplications.value!!
    }
}