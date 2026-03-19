package seriouschoi.aircheck.feature.weather.ui.weather

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import seriouschoi.aircheck.core.domain.model.*
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherRepository
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherResult
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import seriouschoi.aircheck.core.domain.repository.SmartLocation

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var combinedWeatherRepository: CombinedWeatherRepository
    private lateinit var locationProvider: LocationProvider
    private lateinit var savedLocationRepository: SavedLocationRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        combinedWeatherRepository = mockk()
        locationProvider = mockk()
        savedLocationRepository = mockk()

        // 기본 mock 설정
        coEvery { savedLocationRepository.getAll() } returns flowOf(emptyList())
        every { locationProvider.hasLocationPermission() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = WeatherViewModel(
        combinedWeatherRepository = combinedWeatherRepository,
        locationProvider = locationProvider,
        savedLocationRepository = savedLocationRepository
    )

    private fun createMockSmartLocation() = SmartLocation(
        bjdCode = "1168010100",
        bjdName = "서울특별시 강남구 역삼동",
        lat = 37.5,
        lng = 127.0
    )

    private fun createMockCombinedResult() = CombinedWeatherResult(
        airQuality = Result.success(createMockAirQuality()),
        weather = Result.success(createMockWeather())
    )

    // ══════════════════════════════════════════════════════════════════════════
    // 위치 권한 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `위치_권한_없으면_needsPermission_true`() = runTest {
        // Given
        every { locationProvider.hasLocationPermission() } returns false
        coEvery { locationProvider.getCurrentSmartLocation() } returns null

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("권한 필요 상태", state.needsLocationPermission)
        }
    }

    @Test
    fun `위치_권한_있으면_needsPermission_false`() = runTest {
        // Given
        every { locationProvider.hasLocationPermission() } returns true
        coEvery { locationProvider.getCurrentSmartLocation() } returns createMockSmartLocation()
        coEvery { combinedWeatherRepository.fetch(any(), any()) } returns createMockCombinedResult()

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("권한 있음", state.needsLocationPermission)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Intent.Load 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `위치_없으면_Error_상태`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentSmartLocation() } returns null

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("로딩 아님", state.isLoading)
            // 권한 없거나 에러 상태
            assertTrue("에러 또는 권한 필요", state.error != null || state.needsLocationPermission)
        }
    }

    @Test
    fun `위치_있으면_데이터_로드`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentSmartLocation() } returns createMockSmartLocation()
        coEvery { combinedWeatherRepository.fetch(any(), any()) } returns createMockCombinedResult()

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull("날씨 데이터 있음", state.weather.data)
            assertNotNull("대기질 데이터 있음", state.airQuality.data)
            assertNull("에러 없음", state.error)
            assertFalse("로딩 아님", state.isLoading)
        }
    }

    @Test
    fun `CombinedWeatherRepository_호출_확인`() = runTest {
        // Given
        val smartLocation = createMockSmartLocation()
        coEvery { locationProvider.getCurrentSmartLocation() } returns smartLocation
        coEvery { combinedWeatherRepository.fetch(any(), any()) } returns createMockCombinedResult()

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { combinedWeatherRepository.fetch(smartLocation.lat, smartLocation.lng) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Intent.Refresh 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `Refresh_시_데이터_재조회`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentSmartLocation() } returns createMockSmartLocation()
        coEvery { combinedWeatherRepository.fetch(any(), any()) } returns createMockCombinedResult()

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processIntent(WeatherContract.Intent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: 2번 호출됨 (Load + Refresh)
        coVerify(exactly = 2) { combinedWeatherRepository.fetch(any(), any()) }
    }

    @Test
    fun `Refresh_시_isRefreshing_상태_변화`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentSmartLocation() } returns createMockSmartLocation()
        coEvery { combinedWeatherRepository.fetch(any(), any()) } returns createMockCombinedResult()

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When & Then
        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse("초기: 리프레시 아님", initial.isRefreshing)

            viewModel.processIntent(WeatherContract.Intent.Refresh)
            
            val refreshing = awaitItem()
            assertTrue("리프레시 중", refreshing.isRefreshing)

            testDispatcher.scheduler.advanceUntilIdle()

            val done = awaitItem()
            assertFalse("리프레시 완료", done.isRefreshing)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 부분 실패 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `대기질_실패해도_날씨는_표시`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentSmartLocation() } returns createMockSmartLocation()
        coEvery { combinedWeatherRepository.fetch(any(), any()) } returns CombinedWeatherResult(
            airQuality = Result.failure(Exception("API 에러")),
            weather = Result.success(createMockWeather())
        )

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull("대기질 데이터 없음", state.airQuality.data)
            assertNotNull("대기질 에러 있음", state.airQuality.error)
            assertNotNull("날씨 데이터 있음", state.weather.data)
            assertNull("날씨 에러 없음", state.weather.error)
        }
    }

    @Test
    fun `날씨_실패해도_대기질은_표시`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentSmartLocation() } returns createMockSmartLocation()
        coEvery { combinedWeatherRepository.fetch(any(), any()) } returns CombinedWeatherResult(
            airQuality = Result.success(createMockAirQuality()),
            weather = Result.failure(Exception("API 에러"))
        )

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull("대기질 데이터 있음", state.airQuality.data)
            assertNull("대기질 에러 없음", state.airQuality.error)
            assertNull("날씨 데이터 없음", state.weather.data)
            assertNotNull("날씨 에러 있음", state.weather.error)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 다중 위치 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `저장된_위치도_함께_로드`() = runTest {
        // Given
        val savedLocations = listOf(
            SavedLocation(id = 1, name = "집", displayName = "서울 강남구 집", lat = 37.4, lng = 126.9),
            SavedLocation(id = 2, name = "회사", displayName = "서울 중구 회사", lat = 37.6, lng = 127.1)
        )
        coEvery { locationProvider.getCurrentSmartLocation() } returns createMockSmartLocation()
        coEvery { savedLocationRepository.getAll() } returns flowOf(savedLocations)
        coEvery { combinedWeatherRepository.fetch(any(), any()) } returns createMockCombinedResult()
        coEvery { combinedWeatherRepository.fetchBatch(any()) } returns listOf(
            createMockCombinedResult(),
            createMockCombinedResult()
        )

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.multiState.test {
            val state = awaitItem()
            assertEquals("3개 위치", 3, state.locations.size)
            assertTrue("첫 번째는 현재 위치", state.locations[0].isCurrentLocation)
            assertEquals("서울 강남구 집", state.locations[1].locationName)
            assertEquals("서울 중구 회사", state.locations[2].locationName)
        }
    }

    @Test
    fun `위치_권한_없어도_현재위치_페이지_유지`() = runTest {
        // Given
        every { locationProvider.hasLocationPermission() } returns false
        coEvery { locationProvider.getCurrentSmartLocation() } returns null
        val savedLocations = listOf(
            SavedLocation(id = 1, name = "집", displayName = "서울 강남구 집", lat = 37.4, lng = 126.9)
        )
        coEvery { savedLocationRepository.getAll() } returns flowOf(savedLocations)
        coEvery { combinedWeatherRepository.fetchBatch(any()) } returns listOf(createMockCombinedResult())

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.multiState.test {
            val state = awaitItem()
            assertEquals("2개 위치 (현재 위치 + 저장)", 2, state.locations.size)
            assertTrue("첫 번째는 현재 위치", state.locations[0].isCurrentLocation)
            assertTrue("권한 필요 표시", state.locations[0].needsPermission)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 다이얼로그 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `ShowInfoDialog_시_showInfoDialog_true`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentSmartLocation() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processIntent(WeatherContract.Intent.ShowInfoDialog)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            assertTrue("다이얼로그 표시", awaitItem().showInfoDialog)
        }
    }

    @Test
    fun `DismissInfoDialog_시_showInfoDialog_false`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentSmartLocation() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.processIntent(WeatherContract.Intent.ShowInfoDialog)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processIntent(WeatherContract.Intent.DismissInfoDialog)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            assertFalse("다이얼로그 닫힘", awaitItem().showInfoDialog)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 위치 삭제 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `RequestDeleteLocation_시_deleteConfirm_표시`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentSmartLocation() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processIntent(WeatherContract.Intent.RequestDeleteLocation(1, "집"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull("삭제 확인 다이얼로그", state.deleteConfirm)
            assertEquals("위치 ID", 1L, state.deleteConfirm?.locationId)
            assertEquals("위치 이름", "집", state.deleteConfirm?.locationName)
        }
    }

    @Test
    fun `ConfirmDeleteLocation_시_저장소_삭제_호출`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentSmartLocation() } returns null
        coEvery { savedLocationRepository.delete(any()) } returns Unit

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.processIntent(WeatherContract.Intent.RequestDeleteLocation(1, "집"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processIntent(WeatherContract.Intent.ConfirmDeleteLocation)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { savedLocationRepository.delete(1) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Mock 데이터
    // ══════════════════════════════════════════════════════════════════════════

    private fun createMockAirQuality() = AirQuality(
        aqi = 50,
        pm25 = PmData(15.0, AqiLevel.GOOD, "중구", false),
        pm10 = PmData(30.0, AqiLevel.GOOD, "중구", false),
        mainStationName = "중구 (서울)",
        updatedAt = "2026-03-19 12:00",
        level = AqiLevel.GOOD,
        worstLevel = AqiLevel.GOOD
    )

    private fun createMockWeather() = WeatherData(
        temperature = 15.0,
        feelsLike = 13.0,
        precipitation = 0.0,
        precipitationProbability = 10,
        snowfallProbability = 0,
        condition = WeatherCondition.CLEAR_DAY,
        cloudCover = 20,
        isDay = true,
        hourlyForecast = emptyList()
    )
}
