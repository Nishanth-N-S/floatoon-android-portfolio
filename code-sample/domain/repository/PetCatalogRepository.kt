package com.tesseractplay.floatoon.domain.repository.pet

import com.tesseractplay.floatoon.domain.model.PetInfo
import kotlinx.coroutines.flow.Flow

interface PetCatalogRepository {
    fun getStoreCatalog(): Flow<List<PetInfo>>
    fun getCommunityCatalog(): Flow<List<PetInfo>>
    suspend fun refreshPetCatalog(): Result<Unit>
    suspend fun getPet(id: String): PetInfo?
}
