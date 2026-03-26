@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package seriouschoi.aircheck.feature.weather.ui.weather

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import seriouschoi.aircheck.core.domain.model.*
import seriouschoi.aircheck.core.domain.repository.CombinedWeatherResult
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherOutput
import seriouschoi.aircheck.core.domain.usecase.FetchWeatherUseCase
import seriouschoi.aircheck.core.domain.usecase.GetLocationListUseCase

class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLocationListUseCase: GetLocationListUseCase
    private lateinit var fetchWeatherUseCase: FetchWeatherUseCase
    private lateinit var savedLocationRepository: SavedLocationRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getLocationListUseCase = mockk()
        fetchWeatherUseCase = mockk()
        savedLocationRepository = mockk()

        // 기본 mock 설정
        coEvery { savedLocationRepository.getAll() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = WeatherViewModel(
        getLocationListUseCase = getLocationListUseCase,
        fetchWeatherUseCase = fetchWeatherUseCase,
        savedLocationRepository = savedLocationRepository
    )

    private fun createMockLocationList(
        needsPermission: Boolean = false,
        hasCurrentLocation: Boolean = true
    ): List<LocationItem> {
        return if (hasCurrentLocation) {
            listOf(
                LocationItem(
                    name = "역삼동",
                    isCurrentLocation = true,
                    lat = 37.5,
                    lng = 127.0,
                    needsPermission = needsPermission
                )
            )
        } else {
            listOf(
                LocationItem(
                    name = "현재 위치",
                    isCurrentLocation = true,
                    lat = 0.0,
                    lng = 0.0,
                    needsPermission = needsPermission
                )
            )
        }
    }

    private fun createMockFetchOutput(
        weatherSuccess: Boolean = true,
        airQualitySuccess: Boolean = true
    ) = FetchWeatherOutput(
        smartLocation = null,
        result = CombinedWeatherResult(
            weather = if (weatherSuccess) Result.success(createMockWeather())
            else Result.failure(Exception("API 에러")),
            airQuality = if (airQualitySuccess) Result.success(createMockAirQuality())
            else Result.failure(Exception("API 에러"))
        )
    )

    /** 위치 목록이 로드될 때까지 대기 후 상태 반환 */
    private suspend fun app.cash.turbine.ReceiveTurbine<UiWeatherScreenState>.awaitLoadedState(): UiWeatherScreenState {
        var state = awaitItem()
        while (state.locations.locations.isEmpty()) {
            state = awaitItem()
        }
        return state
    }

    /** 날씨 데이터까지 로드될 때까지 대기 후 상태 반환 */
    private suspend fun app.cash.turbine.ReceiveTurbine<UiWeatherScreenState>.awaitWeatherLoadedState(): UiWeatherScreenState {
        var state = awaitLoadedState()
        while (state.locations.locations.any { !it.locationInfo.needsPermission && it.weather.data == null && it.weather.error == null }) {
            state = awaitItem()
        }
        return state
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 위치 권한 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `위치_권한_없으면_needsPermission_true`() = runTest {
        // Given
        every { getLocationListUseCase() } returns flowOf(
            createMockLocationList(needsPermission = true, hasCurrentLocation = false)
        )

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.screenState.test {
            val state = awaitLoadedState()
            assertTrue("권한 필요 상태", state.locations.locations.first().locationInfo.needsPermission)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `위치_권한_있으면_needsPermission_false`() = runTest {
        // Given
        every { getLocationListUseCase() } returns flowOf(createMockLocationList())
        coEvery { fetchWeatherUseCase(any(), any()) } returns createMockFetchOutput()

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.screenState.test {
            val state = awaitLoadedState()
            assertFalse("권한 있음", state.locations.locations.first().locationInfo.needsPermission)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 데이터 로드 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `위치_있으면_데이터_로드`() = runTest {
        // Given
        every { getLocationListUseCase() } returns flowOf(createMockLocationList())
        coEvery { fetchWeatherUseCase(any(), any()) } returns createMockFetchOutput()

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.screenState.test {
            val state = awaitWeatherLoadedState()
            val currentLocation = state.locations.locations.firstOrNull { it.locationInfo.isCurrentLocation }
            assertNotNull("현재 위치 있음", currentLocation)
            assertNotNull("날씨 데이터 있음", currentLocation?.weather?.data)
            assertNotNull("대기질 데이터 있음", currentLocation?.airQuality?.data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 부분 실패 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `대기질_실패해도_날씨는_표시`() = runTest {
        // Given
        every { getLocationListUseCase() } returns flowOf(createMockLocationList())
        coEvery { fetchWeatherUseCase(any(), any()) } returns createMockFetchOutput(
            weatherSuccess = true,
            airQualitySuccess = false
        )

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.screenState.test {
            val state = awaitWeatherLoadedState()
            val currentLocation = state.locations.locations.firstOrNull { it.locationInfo.isCurrentLocation }!!
            assertNull("대기질 데이터 없음", currentLocation.airQuality.data)
            assertNotNull("대기질 에러 있음", currentLocation.airQuality.error)
            assertNotNull("날씨 데이터 있음", currentLocation.weather.data)
            assertNull("날씨 에러 없음", currentLocation.weather.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `날씨_실패해도_대기질은_표시`() = runTest {
        // Given
        every { getLocationListUseCase() } returns flowOf(createMockLocationList())
        coEvery { fetchWeatherUseCase(any(), any()) } returns createMockFetchOutput(
            weatherSuccess = false,
            airQualitySuccess = true
        )

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.screenState.test {
            val state = awaitWeatherLoadedState()
            val currentLocation = state.locations.locations.firstOrNull { it.locationInfo.isCurrentLocation }!!
            assertNotNull("대기질 데이터 있음", currentLocation.airQuality.data)
            assertNull("대기질 에러 없음", currentLocation.airQuality.error)
            assertNull("날씨 데이터 없음", currentLocation.weather.data)
            assertNotNull("날씨 에러 있음", currentLocation.weather.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 다중 위치 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `저장된_위치도_함께_로드`() = runTest {
        // Given
        every { getLocationListUseCase() } returns flowOf(
            listOf(
                LocationItem(name = "역삼동", isCurrentLocation = true, lat = 37.5, lng = 127.0),
                LocationItem(name = "서울 강남구 집", isCurrentLocation = false, savedLocationId = 1, lat = 37.4, lng = 126.9),
                LocationItem(name = "서울 중구 회사", isCurrentLocation = false, savedLocationId = 2, lat = 37.6, lng = 127.1),
            )
        )
        coEvery { fetchWeatherUseCase(any(), any()) } returns createMockFetchOutput()

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.screenState.test {
            val state = awaitLoadedState()
            val locations = state.locations.locations
            assertEquals("3개 위치", 3, locations.size)
            assertTrue("첫 번째는 현재 위치", locations[0].locationInfo.isCurrentLocation)
            assertEquals("서울 강남구 집", locations[1].locationInfo.name)
            assertEquals("서울 중구 회사", locations[2].locationInfo.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 다이얼로그 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `ShowInfoDialog_시_showInfoDialog_true`() = runTest {
        // Given
        every { getLocationListUseCase() } returns flowOf(createMockLocationList())
        coEvery { fetchWeatherUseCase(any(), any()) } returns createMockFetchOutput()

        val viewModel = createViewModel()

        // Then
        viewModel.screenState.test {
            awaitLoadedState() // 초기 로드 대기

            // When
            viewModel.processIntent(WeatherContract.Intent.ShowInfoDialog)

            val state = awaitItem()
            assertTrue("다이얼로그 표시", state.uiControl.showInfoDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `RequestDeleteLocation_시_deleteConfirm_표시`() = runTest {
        // Given
        every { getLocationListUseCase() } returns flowOf(createMockLocationList())
        coEvery { fetchWeatherUseCase(any(), any()) } returns createMockFetchOutput()

        val viewModel = createViewModel()

        // Then
        viewModel.screenState.test {
            awaitLoadedState() // 초기 로드 대기

            // When
            viewModel.processIntent(WeatherContract.Intent.RequestDeleteLocation(1, "집"))

            val state = awaitItem()
            assertNotNull("삭제 확인 다이얼로그", state.uiControl.deleteConfirm)
            assertEquals("위치 ID", 1L, state.uiControl.deleteConfirm?.locationId)
            assertEquals("위치 이름", "집", state.uiControl.deleteConfirm?.locationName)
            cancelAndIgnoreRemainingEvents()
        }
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
