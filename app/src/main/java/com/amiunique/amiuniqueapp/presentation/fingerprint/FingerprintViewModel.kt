package com.amiunique.amiuniqueapp.presentation.fingerprint

import androidx.lifecycle.*
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FingerprintViewModel(private val fingerprintRepository: FingerprintRepository) : ViewModel() {
    val fingerprintLiveData: LiveData<FingerprintModel> =
        fingerprintRepository.getFingerprintLiveData()
    val fingerprintLoadingStateData: LiveData<Int> =
        fingerprintRepository.getFingerprintLoadingStateData()
    val fingerprintLoadingStateString: LiveData<String> =
        fingerprintRepository.getFingerprintLoadingStateString()

    suspend fun fetchFingerprint() {
        viewModelScope.launch(Dispatchers.IO) {
            // Coroutine code for background thread (IO dispatcher)
            try {
                fingerprintRepository.fetchFingerprint()
            } catch (e: Exception) {

            }
        }
    }

    // Define ViewModel factory in a companion object
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])
                return FingerprintViewModel(FingerprintRepository(application)) as T
            }
        }
    }

}