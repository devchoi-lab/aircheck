package seriouschoi.aircheck.feature.weather.ui.locationpicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.Region

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationPickerScreen(
    uiState: LocationPickerContract.UiState,
    onIntent: (LocationPickerContract.Intent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val title = if (uiState.currentPath.isEmpty()) "지역 선택" else uiState.currentPath.last()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentPath.isEmpty()) onNavigateBack()
                        else onIntent(LocationPickerContract.Intent.BackClicked)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 검색 필드
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onIntent(LocationPickerContract.Intent.SearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("검색...") },
                singleLine = true
            )

            // 현재 경로 표시
            if (uiState.currentPath.isNotEmpty()) {
                Text(
                    text = uiState.currentPath.joinToString(" > "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // 지역 목록
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.regions, key = { it.name }) { region ->
                    RegionItem(
                        region = region,
                        onClick = { onIntent(LocationPickerContract.Intent.RegionClicked(region)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionItem(
    region: Region,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = region.name,
            fontSize = 16.sp
        )

        if (region.hasChildren) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    HorizontalDivider()
}
