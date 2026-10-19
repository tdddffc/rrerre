package com.weatherfocus.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weatherfocus.app.data.alert.CustomWeatherAlertEngine
import com.weatherfocus.app.data.model.AppSettings
import com.weatherfocus.app.data.model.CustomAlertMatch
import com.weatherfocus.app.data.model.CustomAlertRules
import com.weatherfocus.app.data.model.DoNotDisturbSettings
import com.weatherfocus.app.data.model.GeoPlace
import com.weatherfocus.app.data.model.ThemeMode
import com.weatherfocus.app.data.prefs.SettingsRepository
import com.weatherfocus.app.data.repository.WeatherBundle
import com.weatherfocus.app.data.repository.WeatherRepository
import com.weatherfocus.app.notification.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WeatherUiState(
    val settings: AppSettings = AppSettings(),
    val bundle: WeatherBundle? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val citySuggestions: List<GeoPlace> = emptyList(),
    val activeAlerts: List<CustomAlertMatch> = emptyList(),
    val lastUpdatedMillis: Long? = null
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val weatherRepository = WeatherRepository()

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var citySearchJob: Job? = null

    /** Keys (type + trigger time) of alerts already pushed as a phone notification, so a manual refresh doesn't re-spam them. */
    private val notifiedAlertKeys = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            _uiState.value = _uiState.value.copy(settings = settings)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val settings = settingsRepository.settingsFlow.first()
            try {
                val bundle = weatherRepository.loadWeather(settings)
                val alerts = CustomWeatherAlertEngine.evaluate(bundle.hourlyForAlerts, settings.customAlertRules)
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    bundle = bundle,
                    isLoading = false,
                    activeAlerts = alerts,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
                notifyNewAlerts(alerts, settings.doNotDisturb)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Couldn't load weather. Check your connection.")
            }
        }
    }

    /** Pushes a phone notification (subject to Do Not Disturb) for any imminent alert we haven't already notified about. */
    private fun notifyNewAlerts(alerts: List<CustomAlertMatch>, dnd: DoNotDisturbSettings) {
        alerts.filter { it.leadWarning }.forEach { match ->
            val key = "${match.type}_${match.triggerAtMillis}"
            if (notifiedAlertKeys.add(key)) {
                NotificationHelper.showAlert(getApplication(), match, dnd)
            }
        }
    }

    fun onCityQueryChanged(query: String) {
        citySearchJob?.cancel()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(citySuggestions = emptyList())
            return
        }
        citySearchJob = viewModelScope.launch {
            delay(300) // debounce while the user is still typing
            val results = weatherRepository.searchCities(query)
            val countryFilter = _uiState.value.settings.countryCode
            val filtered = results.filter { it.countryCode.equals(countryFilter, ignoreCase = true) }
            _uiState.value = _uiState.value.copy(citySuggestions = filtered.ifEmpty { results })
        }
    }

    fun onCountrySelected(code: String, label: String) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            settingsRepository.setLocation(current.cityName, code, label, current.latitude, current.longitude)
            _uiState.value = _uiState.value.copy(
                settings = current.copy(countryCode = code, countryLabel = label),
                citySuggestions = emptyList()
            )
        }
    }

    fun onCitySelected(place: GeoPlace) {
        viewModelScope.launch {
            settingsRepository.setLocation(
                place.name, place.countryCode ?: _uiState.value.settings.countryCode,
                place.country ?: _uiState.value.settings.countryLabel, place.latitude, place.longitude
            )
            _uiState.value = _uiState.value.copy(citySuggestions = emptyList())
            refresh()
        }
    }

    fun onFahrenheitToggled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFahrenheit(value)
            _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(useFahrenheit = value))
        }
    }

    fun onOpenWeatherKeyChanged(key: String) {
        viewModelScope.launch {
            settingsRepository.setOpenWeatherApiKey(key)
            _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(openWeatherApiKey = key))
        }
    }

    fun updateAlertRules(rules: CustomAlertRules) {
        viewModelScope.launch {
            settingsRepository.updateAlertRules(rules)
            _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(customAlertRules = rules))
            refresh()
        }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
            _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(themeMode = mode))
        }
    }

    fun updateDoNotDisturb(dnd: DoNotDisturbSettings) {
        viewModelScope.launch {
            settingsRepository.setDoNotDisturb(dnd)
            _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(doNotDisturb = dnd))
        }
    }
}
