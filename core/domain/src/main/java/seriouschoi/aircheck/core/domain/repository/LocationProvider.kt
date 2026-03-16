package seriouschoi.aircheck.core.domain.repository

import android.location.Location

/** 위치 정보 제공자 (Port) */
interface LocationProvider {
    suspend fun getCurrentLocation(): Location?
}
