package seriouschoi.aircheck.feature.weather.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.PmData
import seriouschoi.aircheck.core.ui.mapper.toColor
import seriouschoi.aircheck.core.ui.mapper.toEmoji
import seriouschoi.aircheck.core.ui.mapper.toLabelRes
import seriouschoi.aircheck.core.resource.R as CoreR

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AirQualitySection(air: AirQuality, modifier: Modifier = Modifier) {
    val worstColor = air.worstLevel.toColor()

    Column(modifier = modifier.fillMaxWidth()) {
        // 섹션 라벨
        Text(
            text = stringResource(CoreR.string.section_air_quality),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        
        // 통합 공기질 등급 (최악 기준)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(worstColor.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = air.worstLevel.toEmoji(),
                    fontSize = 32.sp
                )
            }
            Column {
                Text(
                    text = stringResource(air.worstLevel.toLabelRes()),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = worstColor
                )
                Text(
                    text = stringResource(CoreR.string.air_quality_integrated),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 초미세먼지 (PM2.5)
        PollutantRow(
            label = stringResource(CoreR.string.label_pm25),
            pmData = air.pm25,
            mainStationName = air.mainStationName
        )
        Spacer(Modifier.height(8.dp))
        // 미세먼지 (PM10)
        PollutantRow(
            label = stringResource(CoreR.string.label_pm10),
            pmData = air.pm10,
            mainStationName = air.mainStationName
        )
        
        Spacer(Modifier.height(8.dp))
        
        // 측정소 정보 (글자 크기 커지면 자동 줄바꿈)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(CoreR.string.label_station, air.mainStationName),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (air.updatedAt.isNotBlank()) {
                Text(
                    text = air.updatedAt,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
internal fun PollutantRow(label: String, pmData: PmData?, mainStationName: String) {
    val level = pmData?.level ?: AqiLevel.UNKNOWN
    val color = level.toColor()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 왼쪽: 라벨 + 값 (중앙 기준 왼쪽)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = pmData?.let { "%.1f μg/m³".format(it.value) } ?: "- μg/m³",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.size(16.dp))
            
            // 오른쪽: WHO 등급 칩 (중앙 기준 오른쪽)
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${level.toEmoji()} ${stringResource(level.toLabelRes())}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }
        }
        
        // 폴백 측정소에서 가져온 경우 출처 표시
        if (pmData != null && pmData.isFromFallback) {
            Text(
                text = stringResource(CoreR.string.air_quality_from_station, pmData.stationName),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
