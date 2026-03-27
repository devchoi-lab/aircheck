package seriouschoi.aircheck.core.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import seriouschoi.aircheck.core.domain.model.DayLabel
import seriouschoi.aircheck.core.resource.R

/**
 * DayLabel → localized String 변환
 */
@Composable
fun DayLabel.toDisplayString(): String = when (this) {
    DayLabel.Today -> stringResource(R.string.date_today)
    DayLabel.Tomorrow -> stringResource(R.string.date_tomorrow)
    DayLabel.DayAfterTomorrow -> stringResource(R.string.date_day_after_tomorrow)
    is DayLabel.Date -> "$month/$day"
}
