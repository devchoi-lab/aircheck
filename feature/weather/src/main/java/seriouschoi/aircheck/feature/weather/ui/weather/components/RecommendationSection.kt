package seriouschoi.aircheck.feature.weather.ui.weather.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.Recommendation
import seriouschoi.aircheck.core.ui.mapper.toDisplayLabel
import seriouschoi.aircheck.core.ui.mapper.toIconRes
import seriouschoi.aircheck.core.resource.R as CoreR

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecommendationSection(
    recommendations: List<Recommendation>,
    modifier: Modifier = Modifier
) {
    if (recommendations.isEmpty()) return
    
    // 글자 크기에 따라 아이콘 크기 조절
    val fontScale = LocalDensity.current.fontScale
    val iconSize = (32 * fontScale).dp

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(CoreR.string.recommendation_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(CoreR.string.recommendation_description),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            recommendations.forEach { recommendation ->
                val label = recommendation.toDisplayLabel()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(recommendation.toIconRes()),
                        contentDescription = label,
                        modifier = Modifier.size(iconSize),
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
