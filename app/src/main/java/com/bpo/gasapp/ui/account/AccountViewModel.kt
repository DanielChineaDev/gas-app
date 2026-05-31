package com.bpo.gasapp.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpo.gasapp.data.remote.ProfileRemoteDataSource
import com.bpo.gasapp.data.settings.SettingsRepository
import com.bpo.gasapp.domain.model.AuthUser
import com.bpo.gasapp.domain.model.FuelType
import com.bpo.gasapp.domain.repository.AuthRepository
import com.bpo.gasapp.domain.repository.FavoriteMergeStrategy
import com.bpo.gasapp.domain.repository.RefuelRepository
import com.bpo.gasapp.domain.repository.StationRepository
import com.bpo.gasapp.domain.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
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
    private val refuelRepository: RefuelRepository,
    private val vehicleRepository: VehicleRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRemote: ProfileRemoteDataSource
) : ViewModel() {

    private val _user = MutableStateFlow(authRepository.currentUser())
    val user: StateFlow<AuthUser?> = _user.asStateFlow()

    /** Number of local favorites pending resolution after a fresh login. */
    private val _pendingLocalFavorites = MutableStateFlow(0)
    val pendingLocalFavorites: StateFlow<Int> = _pendingLocalFavorites.asStateFlow()

    init {
        authRepository.authState
            .onEach { _user.value = it }
            .launchIn(viewModelScope)

        // Si ya hay sesión iniciada al abrir la app, sincroniza repostajes y
        // vehículos del perfil (ahorro, logros y estadísticas derivan de ellos).
        if (authRepository.currentUser() != null) {
            viewModelScope.launch { syncProfileData() }
        }
    }

    /** Fusiona repostajes y vehículos del perfil (vehículos primero por el enlace). */
    private suspend fun syncProfileData() {
        runCatching { vehicleRepository.sync() }
        runCatching { refuelRepository.sync() }
    }

    val favoritesCount: StateFlow<Int> = stationRepository.observeFavorites()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    /** Money saved by refueling below the current average price for each fuel. */
    val moneySaved: StateFlow<Double> =
        combine(
            refuelRepository.observeRefuels(),
            stationRepository.observeStations()
        ) { refuels, stations ->
            val avgByFuel = FuelType.entries.associateWith { fuel ->
                val prices = stations.mapNotNull { it.priceOf(fuel) }
                if (prices.isNotEmpty()) prices.average() else null
            }
            refuels.sumOf { r ->
                val avg = avgByFuel[r.fuel]
                val price = r.pricePerLiter
                if (avg != null && price != null && avg > price) (avg - price) * r.liters else 0.0
            }
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
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
                onLoggedIn()
                _form.value = AccountFormState()
            }.onFailure {
                _form.value = _form.value.copy(isLoading = false, error = mapError(it))
            }
        }
    }

    /**
     * After login: if there are unsynced local favorites, ask the user how to
     * resolve them instead of merging silently. Otherwise merge directly.
     */
    private suspend fun onLoggedIn() {
        syncDefaultFuel()
        syncProfileData()
        val localCount = stationRepository.localFavoritesCount()
        if (localCount > 0) {
            _pendingLocalFavorites.value = localCount
        } else {
            stationRepository.syncFavorites()
        }
    }

    fun resolveLocalFavorites(strategy: FavoriteMergeStrategy) {
        viewModelScope.launch {
            stationRepository.resolveFavoritesOnLogin(strategy)
            _pendingLocalFavorites.value = 0
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
                _user.value = it
                onLoggedIn()
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
