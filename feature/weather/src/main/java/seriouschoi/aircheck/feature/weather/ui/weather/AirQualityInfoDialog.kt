package seriouschoi.aircheck.feature.weather.ui.weather

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.RecommendedClothing
import seriouschoi.aircheck.core.resource.R as CoreR
import seriouschoi.aircheck.core.ui.mapper.*

@Composable
internal fun AirQualityInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(CoreR.string.dialog_air_quality_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 통합 등급 설명
                Text(
                    text = stringResource(CoreR.string.dialog_integrated_grade_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(CoreR.string.dialog_integrated_grade_desc),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // 초미세먼지 (PM2.5) - WHO 2021
                Text(
                    text = stringResource(CoreR.string.dialog_pm25_section),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                GradeTable(
                    grades = AqiLevel.entries
                        .filter { it != AqiLevel.UNKNOWN }
                        .map { level ->
                            Triple(
                                "${level.toEmoji()} ${stringResource(level.toLabelRes())}",
                                level.toPm25Range(),
                                level.toColor()
                            )
                        }
                )

                HorizontalDivider()

                // 미세먼지 (PM10) - WHO 2021
                Text(
                    text = stringResource(CoreR.string.dialog_pm10_section),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                GradeTable(
                    grades = AqiLevel.entries
                        .filter { it != AqiLevel.UNKNOWN }
                        .map { level ->
                            Triple(
                                "${level.toEmoji()} ${stringResource(level.toLabelRes())}",
                                level.toPm10Range(),
                                level.toColor()
                            )
                        }
                )

                HorizontalDivider()

                // 체감온도별 의상 추천
                Text(
                    text = stringResource(CoreR.string.dialog_clothing_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                ClothingRecommendationTable()

                HorizontalDivider()

                // 출처
                Text(
                    text = stringResource(CoreR.string.dialog_source_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(CoreR.string.dialog_source_content),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CoreR.string.button_confirm))
            }
        }
    )
}

@Composable
private fun GradeTable(grades: List<Triple<String, String, Color>>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        grades.forEach { (label, range, color) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = color
                )
                Text(
                    text = "$range μg/m³",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ClothingRecommendationTable() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecommendedClothing.entries.forEach { clothing ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(clothing.toIconRes()),
                        contentDescription = stringResource(clothing.toDetailedLabelRes()),
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Text(
                        text = stringResource(clothing.toDetailedLabelRes()),
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = clothing.toTemperatureRange(LocalContext.current),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
