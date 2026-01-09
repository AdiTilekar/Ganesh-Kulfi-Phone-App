package com.ganeshkulfi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganeshkulfi.app.data.model.User
import com.ganeshkulfi.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUserFlow.collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email, password)
            _authState.value = if (result.isSuccess) {
                // Ensure currentUser is updated before signaling success
                val user = result.getOrNull()
                if (user != null) {
                    _currentUser.value = user
                }
                AuthState.Success
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }

    fun signUp(email: String, password: String, name: String, phone: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signUp(email, password, name, phone)
            _authState.value = if (result.isSuccess) {
                // Ensure currentUser is updated before signaling success
                val user = result.getOrNull()
                if (user != null) {
                    _currentUser.value = user
                }
                AuthState.Success
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _currentUser.value = null  // Explicitly clear current user
            _authState.value = AuthState.Idle
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.continueAsGuest()
            _authState.value = if (result.isSuccess) {
                // Ensure currentUser is updated before signaling success
                val user = result.getOrNull()
                if (user != null) {
                    _currentUser.value = user
                }
                AuthState.Success
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Failed to continue as guest")
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun isGuestUser(): Boolean = authRepository.isGuestUser()
    
    fun isAdmin(): Boolean {
        return _currentUser.value?.role == com.ganeshkulfi.app.data.model.UserRole.ADMIN
    }
    
    fun isRetailer(): Boolean {
        return _currentUser.value?.role == com.ganeshkulfi.app.data.model.UserRole.RETAILER
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
