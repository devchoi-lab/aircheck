package seriouschoi.aircheck.core.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import seriouschoi.aircheck.core.domain.model.Recommendation
import seriouschoi.aircheck.core.resource.R as CoreR

fun Recommendation.toIconRes(): Int = when (this) {
    is Recommendation.Clothing -> clothing.toIconRes()
    is Recommendation.Umbrella -> CoreR.drawable.ic_umbrella_blue
    is Recommendation.Snow -> CoreR.drawable.ic_snow_man
    is Recommendation.Mask -> CoreR.drawable.ic_mask
}

@Composable
fun Recommendation.toDisplayLabel(): String = when (this) {
    is Recommendation.Clothing -> stringResource(clothing.toLabelRes())
    is Recommendation.Umbrella -> stringResource(CoreR.string.item_umbrella_with_prob, maxPrecipitationProbability)
    is Recommendation.Snow -> stringResource(CoreR.string.item_snow_cm, totalSnowfall)
    is Recommendation.Mask -> stringResource(CoreR.string.item_mask, stringResource(aqiLevel.toLabelRes()))
}
