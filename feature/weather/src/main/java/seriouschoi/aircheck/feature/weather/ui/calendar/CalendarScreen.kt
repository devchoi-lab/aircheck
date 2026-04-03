package seriouschoi.aircheck.feature.weather.ui.calendar

import android.Manifest
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import seriouschoi.aircheck.core.ui.permission.PermissionEffect
import seriouschoi.aircheck.core.resource.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    PermissionEffect(
        permissions = listOf(Manifest.permission.READ_CALENDAR),
        onGranted = { viewModel.processIntent(CalendarContract.Intent.OnPermissionResult(true)) },
        onDenied = { viewModel.processIntent(CalendarContract.Intent.OnPermissionResult(false)) },
    )

    // Side Effect 처리 (시스템 캘린더 앱 열기)
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CalendarContract.Effect.LaunchCalendarIntent -> {
                    try {
                        context.startActivity(effect.intent)
                    } catch (_: android.content.ActivityNotFoundException) {
                        // 처리할 수 있는 앱이 없는 경우 무시
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(CoreR.string.calendar_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreR.string.content_description_back_calendar)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.processIntent(CalendarContract.Intent.AddEvent) }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(CoreR.string.content_description_add_event)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        CalendarPage(
            state = state,
            onIntent = viewModel::processIntent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
