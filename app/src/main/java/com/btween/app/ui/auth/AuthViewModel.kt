package com.btween.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btween.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val didSucceed: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onUsernameChanged(value: String) = update { it.copy(username = value) }
    fun onDisplayNameChanged(value: String) = update { it.copy(displayName = value) }
    fun onEmailChanged(value: String) = update { it.copy(email = value) }
    fun onPasswordChanged(value: String) = update { it.copy(password = value) }
    fun consumeError() = update { it.copy(errorMessage = null) }

    private inline fun update(block: (AuthUiState) -> AuthUiState) {
        _uiState.value = block(_uiState.value)
    }

    fun onRegister() {
        val state = _uiState.value
        viewModelScope.launch {
            update { it.copy(isLoading = true) }
            authRepository.register(
                username = state.username.trim(),
                email = state.email.trim(),
                password = state.password,
                displayName = state.displayName.trim()
            ).onSuccess {
                update { it.copy(isLoading = false, didSucceed = true) }
            }.onFailure { error ->
                update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    fun onLogin() {
        val state = _uiState.value
        viewModelScope.launch {
            update { it.copy(isLoading = true) }
            authRepository.login(state.email.trim(), state.password)
                .onSuccess {
                    update { it.copy(isLoading = false, didSucceed = true) }
                }
                .onFailure { error ->
                    update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
