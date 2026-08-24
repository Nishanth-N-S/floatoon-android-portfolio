package com.tesseractplay.floatoon.domain.usecase.pet

import android.util.Log
import com.tesseractplay.floatoon.domain.model.DataError
import com.tesseractplay.floatoon.domain.model.DataLoadState
import com.tesseractplay.floatoon.domain.repository.pet.PetCatalogRepository
import com.tesseractplay.floatoon.domain.repository.pet.PetAssetRepository
import com.tesseractplay.floatoon.domain.repository.pet.PetCollectionRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshPetCatalogUseCase @Inject constructor(
    private val petCatalogRepository: PetCatalogRepository,
    private val petCollectionRepository: PetCollectionRepository,
    private val petAssetRepository: PetAssetRepository
) {
    private val _status = MutableStateFlow<DataLoadState>(DataLoadState.Loading)
    val status: StateFlow<DataLoadState> = _status.asStateFlow()

    suspend operator fun invoke() {
        _status.value = DataLoadState.Loading
        val result = petCatalogRepository.refreshPetCatalog()
        if (result.isSuccess) {
            val collection = petCollectionRepository.getCollection().first()
            coroutineScope {
                collection.forEach { pet ->
                    launch { petAssetRepository.syncPetAsset(pet.id) }
                }
            }
            Log.d("RefreshPetCatalogUseCase", "Pet catalog refreshed successfully")
            _status.value = DataLoadState.Success
        } else {
            val exception = result.exceptionOrNull()
            if (exception is IOException) {
                Log.e("RefreshPetCatalogUseCase", "No internet connection. Error: ${exception.message}")
                _status.value = DataLoadState.Failure(DataError.NO_INTERNET)
            } else {
                Log.e("RefreshPetCatalogUseCase", "Error: ${exception?.message}")
                _status.value = DataLoadState.Failure(DataError.UNKNOWN)
            }
        }
    }
}
