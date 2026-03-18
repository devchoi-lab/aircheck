package seriouschoi.aircheck.feature.weather.ui.locationpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import seriouschoi.aircheck.core.domain.model.Region
import seriouschoi.aircheck.core.domain.model.RegionSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationPickerScreen(
    uiState: LocationPickerContract.UiState,
    onIntent: (LocationPickerContract.Intent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val title = if (uiState.currentPath.isEmpty()) "지역 선택" else uiState.currentPath.last()

    // 알림 다이얼로그
    uiState.alertMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { onIntent(LocationPickerContract.Intent.DismissAlert) },
            title = { Text("안내", fontWeight = FontWeight.Bold) },
            text = { Text(message.asString()) },
            confirmButton = {
                TextButton(onClick = { onIntent(LocationPickerContract.Intent.DismissAlert) }) {
                    Text("확인")
                }
            }
        )
    }

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
                placeholder = { Text("도, 시, 구, 동 검색...") },
                singleLine = true
            )

            if (uiState.isSearching) {
                // ── 검색 모드 ────────────────────────────────────────────
                if (uiState.searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\"${uiState.searchQuery}\" 검색 결과가 없습니다",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            uiState.searchResults,
                            key = { it.fullName }
                        ) { result ->
                            SearchResultItem(
                                result = result,
                                onClick = {
                                    onIntent(LocationPickerContract.Intent.SearchResultClicked(result))
                                }
                            )
                        }
                    }
                }
            } else {
                // ── 탐색 모드 ────────────────────────────────────────────
                // 현재 경로 표시
                if (uiState.currentPath.isNotEmpty()) {
                    Text(
                        text = uiState.currentPath.joinToString(" > "),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.regions, key = { it.name }) { region ->
                        BrowseRegionItem(
                            region = region,
                            onClick = {
                                onIntent(LocationPickerContract.Intent.RegionClicked(region))
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── 탐색 모드 아이템 ─────────────────────────────────────────────────────────

@Composable
private fun BrowseRegionItem(
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
            // 최하위 지역: 선택 가능 표시
            SelectableBadge()
        }
    }
    HorizontalDivider()
}

// ── 검색 결과 아이템 ─────────────────────────────────────────────────────────

@Composable
private fun SearchResultItem(
    result: RegionSearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.region.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (result.path.isNotEmpty()) {
                Text(
                    text = result.pathDisplay,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (result.region.isLeaf) {
            SelectableBadge()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}

// ── 선택 가능 배지 ───────────────────────────────────────────────────────────

@Composable
private fun SelectableBadge() {
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "선택",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
