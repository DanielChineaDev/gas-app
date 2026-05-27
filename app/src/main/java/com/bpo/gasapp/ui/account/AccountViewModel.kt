package com.bpo.gasapp.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.remote.ProfileRemoteDataSource
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.AuthUser
import com.bpo.gasapp.domain.repository.AuthRepository
import com.bpo.gasapp.domain.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountFormState(
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val stationRepository: StationRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRemote: ProfileRemoteDataSource
) : ViewModel() {

    private val _user = MutableStateFlow(authRepository.currentUser())
    val user: StateFlow<AuthUser?> = _user.asStateFlow()

    init {
        authRepository.authState
            .onEach { _user.value = it }
            .launchIn(viewModelScope)
    }

    val favoritesCount: StateFlow<Int> = stationRepository.observeFavorites()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    private val _form = MutableStateFlow(AccountFormState())
    val form: StateFlow<AccountFormState> = _form.asStateFlow()

    fun toggleMode() {
        _form.value = _form.value.copy(isRegisterMode = !_form.value.isRegisterMode, error = null)
    }

    fun submit(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            _form.value = _form.value.copy(error = "Introduce un email válido y una contraseña de 6+ caracteres.")
            return
        }
        viewModelScope.launch {
            _form.value = _form.value.copy(isLoading = true, error = null)
            val result = if (_form.value.isRegisterMode) {
                authRepository.register(email, password)
            } else {
                authRepository.login(email, password)
            }
            result.onSuccess {
                stationRepository.syncFavorites()
                syncDefaultFuel()
                _form.value = AccountFormState()
            }.onFailure {
                _form.value = _form.value.copy(isLoading = false, error = mapError(it))
            }
        }
    }

    /** Pulls the remote default fuel if present, otherwise pushes the local one. */
    private suspend fun syncDefaultFuel() {
        val remoteFuel = profileRemote.getDefaultFuel()
        if (remoteFuel != null) {
            settingsRepository.setDefaultFuel(remoteFuel)
        } else {
            profileRemote.setDefaultFuel(settingsRepository.settings.first().defaultFuel)
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _form.value = _form.value.copy(isLoading = true, error = null)
            authRepository.loginWithGoogle(idToken).onSuccess {
                stationRepository.syncFavorites()
                syncDefaultFuel()
                _user.value = it
                _form.value = AccountFormState()
            }.onFailure {
                _form.value = _form.value.copy(isLoading = false, error = "No se pudo iniciar sesión con Google.")
            }
        }
    }

    fun googleSignInFailed() {
        _form.value = _form.value.copy(isLoading = false, error = "Inicio con Google cancelado o no disponible.")
    }

    fun updateDisplayName(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            authRepository.updateDisplayName(name).onSuccess {
                _user.value = authRepository.currentUser()
            }
        }
    }

    fun logout() = authRepository.logout()

    private fun mapError(t: Throwable): String = when {
        t.message?.contains("password is invalid", true) == true -> "Contraseña incorrecta."
        t.message?.contains("no user record", true) == true -> "No existe una cuenta con ese email."
        t.message?.contains("email address is already in use", true) == true -> "Ese email ya está registrado."
        t.message?.contains("badly formatted", true) == true -> "El email no es válido."
        else -> "No se pudo completar. Revisa tus datos y la conexión."
    }
}
