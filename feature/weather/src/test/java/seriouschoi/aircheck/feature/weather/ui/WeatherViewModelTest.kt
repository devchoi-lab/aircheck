package seriouschoi.aircheck.feature.weather.ui

import android.location.Location
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import seriouschoi.aircheck.core.domain.repository.AirQualityRepository
import seriouschoi.aircheck.core.domain.repository.LocationProvider
import seriouschoi.aircheck.core.domain.repository.SavedLocationRepository
import seriouschoi.aircheck.core.domain.repository.WeatherRepository
import seriouschoi.aircheck.feature.weather.ui.WeatherContract.*

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var airQualityRepository: AirQualityRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var locationProvider: LocationProvider
    private lateinit var savedLocationRepository: SavedLocationRepository
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        airQualityRepository = mockk()
        weatherRepository = mockk()
        locationProvider = mockk()
        savedLocationRepository = mockk()
        
        // 기본 mock 설정
        coEvery { savedLocationRepository.getAll() } returns flowOf(emptyList())
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    private fun createViewModel() = WeatherViewModel(
        airQualityRepository = airQualityRepository,
        weatherRepository = weatherRepository,
        locationProvider = locationProvider,
        savedLocationRepository = savedLocationRepository
    )
    
    private fun createMockLocation(lat: Double, lng: Double): Location {
        return mockk<Location> {
            every { latitude } returns lat
            every { longitude } returns lng
        }
    }
    
    // ── 위치 테스트 ─────────────────────────────────────────────────────────
    
    @Test
    fun `위치 없으면 Error 상태`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentLocation() } returns null
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue("Error 상태여야 함", state is State.Error)
        }
    }
    
    @Test
    fun `위치 있으면 Success 상태`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        val mockAirQuality = createMockAirQuality()
        val mockWeather = createMockWeather()
        
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(mockAirQuality)
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(mockWeather)
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue("Success 상태여야 함", state is State.Success)
        }
    }
    
    // ── Intent 테스트 ───────────────────────────────────────────────────────
    
    @Test
    fun `Refresh Intent 처리`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        val mockAirQuality = createMockAirQuality()
        val mockWeather = createMockWeather()
        
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(mockAirQuality)
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(mockWeather)
        
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.processIntent(Intent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue("Success 상태여야 함", state is State.Success)
            assertFalse("리프레시 완료됨", (state as State.Success).isRefreshing)
        }
    }
    
    // ── 데이터 로드 테스트 ───────────────────────────────────────────────────
    
    @Test
    fun `대기질 로드 실패해도 날씨는 표시`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        val mockWeather = createMockWeather()
        
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.failure(Exception("API 에러"))
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(mockWeather)
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem() as State.Success
            assertNull("대기질은 없어야 함", state.airQuality.data)
            assertNotNull("대기질 에러 메시지", state.airQuality.error)
            assertNotNull("날씨는 있어야 함", state.weather.data)
        }
    }
    
    @Test
    fun `날씨 로드 실패해도 대기질은 표시`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        val mockAirQuality = createMockAirQuality()
        
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(mockAirQuality)
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.failure(Exception("API 에러"))
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem() as State.Success
            assertNotNull("대기질은 있어야 함", state.airQuality.data)
            assertNull("날씨는 없어야 함", state.weather.data)
            assertNotNull("날씨 에러 메시지", state.weather.error)
        }
    }
    
    // ── 다중 위치 테스트 ─────────────────────────────────────────────────────
    
    @Test
    fun `저장된 위치도 함께 로드`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        val savedLocations = listOf(
            SavedLocation(id = 1, displayName = "집", lat = 37.4, lng = 126.9),
            SavedLocation(id = 2, displayName = "회사", lat = 37.6, lng = 127.1)
        )
        val mockAirQuality = createMockAirQuality()
        val mockWeather = createMockWeather()
        
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { savedLocationRepository.getAll() } returns flowOf(savedLocations)
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(mockAirQuality)
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(mockWeather)
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.multiState.test {
            val state = awaitItem()
            assertEquals("3개 위치 (현재 + 저장 2개)", 3, state.locations.size)
            assertTrue("첫 번째는 현재 위치", state.locations[0].isCurrentLocation)
            assertEquals("집", state.locations[1].locationName)
            assertEquals("회사", state.locations[2].locationName)
        }
    }
    
    // ── Mock 데이터 헬퍼 ─────────────────────────────────────────────────────
    
    private fun createMockAirQuality() = AirQuality(
        aqi = 0,
        pm25 = PmData(15.0, AqiLevel.GOOD, "중구", false),
        pm10 = PmData(30.0, AqiLevel.GOOD, "중구", false),
        mainStationName = "중구 (서울)",
        updatedAt = "2026-03-18 06:00",
        level = AqiLevel.GOOD,
        worstLevel = AqiLevel.GOOD
    )
    
    private fun createMockWeather() = WeatherData(
        temperature = 15.0,
        feelsLike = 13.0,
        precipitation = 0.0,
        precipitationProbability = 10,
        snowfallProbability = 0,
        condition = WeatherCondition.CLEAR,
        cloudCover = 20,
        isDay = true,
        hourlyForecast = emptyList()
    )
}
