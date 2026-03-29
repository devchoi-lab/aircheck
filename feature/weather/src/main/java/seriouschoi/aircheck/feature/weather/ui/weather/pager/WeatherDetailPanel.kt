package seriouschoi.aircheck.feature.weather.ui.weather.pager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.WeatherData
import seriouschoi.aircheck.feature.weather.ui.weather.WeatherContract
import seriouschoi.aircheck.feature.weather.ui.weather.components.AirQualitySection
import seriouschoi.aircheck.feature.weather.ui.weather.components.ErrorSection
import seriouschoi.aircheck.feature.weather.ui.weather.components.RecommendationSection
import seriouschoi.aircheck.feature.weather.ui.weather.components.WeatherSection
import seriouschoi.aircheck.core.ui.ads.AdUnitIds
import seriouschoi.aircheck.core.ui.ads.NativeAdCard
import seriouschoi.aircheck.core.resource.R as CoreR

@Composable
internal fun WeatherDetailPanel(
    air: WeatherContract.DataResult<AirQuality>,
    weather: WeatherContract.DataResult<WeatherData>,
    onIntent: (WeatherContract.Intent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 추천 아이템 (가장 위에) ─────────────────────────────
        if (weather.data != null || air.data != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                RecommendationSection(
                    weather = weather.data,
                    airQuality = air.data,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // ── 네이티브 광고 ────────────────────────────────────
        NativeAdCard(
            adUnitId = AdUnitIds.native,
            modifier = Modifier.fillMaxWidth()
        )

        // ── 날씨 섹션 ────────────────────────────────────────
        when {
            weather.data != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    WeatherSection(weather.data)
                }
            }
            weather.error != null -> ErrorSection(
                title = stringResource(CoreR.string.weather_title),
                message = weather.error.asString(),
                onRetry = { onIntent(WeatherContract.Intent.Refresh) }
            )
        }

        // ── 공기질 섹션 ──────────────────────────────────────
        when {
            air.data != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    AirQualitySection(air.data, modifier = Modifier.padding(16.dp))
                }
            }
            air.error != null -> ErrorSection(
                title = stringResource(CoreR.string.air_quality_title),
                message = air.error.asString(),
                onRetry = { onIntent(WeatherContract.Intent.Refresh) }
            )
        }

    }
}
