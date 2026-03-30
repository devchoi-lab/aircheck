package seriouschoi.aircheck.feature.weather.ui.weather.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

@Composable
internal fun RecommendationSection(
    recommendations: List<Recommendation>,
    modifier: Modifier = Modifier
) {
    if (recommendations.isEmpty()) return

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            recommendations.forEach { recommendation ->
                val label = recommendation.toDisplayLabel()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        painter = painterResource(recommendation.toIconRes()),
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
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
