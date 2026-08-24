package com.tesseractplay.floatoon.presentation.screens.communitypet

import com.tesseractplay.floatoon.domain.model.PetInfo
import com.tesseractplay.floatoon.domain.model.DataLoadState

data class CommunityPetUiState(
    val pets: List<PetInfo> = emptyList(),
    val refreshStatus: DataLoadState = DataLoadState.Loading
)
