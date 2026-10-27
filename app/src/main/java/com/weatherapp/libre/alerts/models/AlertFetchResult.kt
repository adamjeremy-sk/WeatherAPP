package com.weatherapp.libre.alerts.models

sealed interface AlertFetchResult {
    data class Success(val alerts: List<UnifiedAlert>) : AlertFetchResult
    data class Failure(val cause: Throwable) : AlertFetchResult
}
