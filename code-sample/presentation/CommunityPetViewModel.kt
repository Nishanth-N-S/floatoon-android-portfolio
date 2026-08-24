package com.tesseractplay.floatoon.presentation.screens.communitypet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tesseractplay.floatoon.data.repository.AppConfigRepositoryImpl.Companion.HARDCODED_DISCORD_URL
import com.tesseractplay.floatoon.domain.repository.pet.PetCatalogRepository
import com.tesseractplay.floatoon.domain.usecase.config.GetDiscordUrlUseCase
import com.tesseractplay.floatoon.domain.usecase.pet.RefreshPetCatalogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityPetViewModel @Inject constructor(
    private val petCatalogRepository: PetCatalogRepository,
    private val refreshPetCatalogUseCase: RefreshPetCatalogUseCase,
    getDiscordUrlUseCase: GetDiscordUrlUseCase
) : ViewModel() {

    val discordUrl: StateFlow<String> = getDiscordUrlUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HARDCODED_DISCORD_URL
        )

    val uiState: StateFlow<CommunityPetUiState> = combine(
        petCatalogRepository.getCommunityCatalog(),
        refreshPetCatalogUseCase.status
    ) { pets, status ->
        CommunityPetUiState(pets = pets, refreshStatus = status)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CommunityPetUiState()
    )

    fun refresh() {
        viewModelScope.launch {
            refreshPetCatalogUseCase()
        }
    }
}
