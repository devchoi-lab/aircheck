package seriouschoi.aircheck.feature.weather.ui

import android.location.Location
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
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
    
    // ══════════════════════════════════════════════════════════════════════════
    // Intent.Load 테스트
    // ══════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `Intent_Load - 위치 없으면 Error 상태`() = runTest {
        // Given
        coEvery { locationProvider.getCurrentLocation() } returns null
        
        // When: ViewModel 생성 시 자동으로 Intent.Load 발송
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue("Error 상태여야 함", state is State.Error)
        }
    }
    
    @Test
    fun `Intent_Load - 위치 있으면 Success 상태`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(createMockAirQuality())
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(createMockWeather())
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue("Success 상태여야 함", state is State.Success)
        }
    }
    
    @Test
    fun `Intent_Load - Repository 호출 확인`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(createMockAirQuality())
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(createMockWeather())
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { airQualityRepository.fetch(37.5, 127.0) }
        coVerify { weatherRepository.fetch(37.5, 127.0) }
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // Intent.Refresh 테스트
    // ══════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `Intent_Refresh - 데이터 새로고침`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(createMockAirQuality())
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(createMockWeather())
        
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: Refresh Intent 발송
        viewModel.processIntent(Intent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Repository가 2번 호출됨 (Load + Refresh)
        coVerify(exactly = 2) { airQualityRepository.fetch(any(), any()) }
        coVerify(exactly = 2) { weatherRepository.fetch(any(), any()) }
    }
    
    @Test
    fun `Intent_Refresh - isRefreshing 상태 변화`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(createMockAirQuality())
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(createMockWeather())
        
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When & Then
        viewModel.state.test {
            val initialState = awaitItem() as State.Success
            assertFalse("초기 상태: 리프레시 아님", initialState.isRefreshing)
            
            viewModel.processIntent(Intent.Refresh)
            
            // isRefreshing = true 상태
            val refreshingState = awaitItem() as State.Success
            assertTrue("리프레시 중", refreshingState.isRefreshing)
            
            testDispatcher.scheduler.advanceUntilIdle()
            
            // isRefreshing = false 상태
            val completedState = awaitItem() as State.Success
            assertFalse("리프레시 완료", completedState.isRefreshing)
        }
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // 부분 실패 테스트
    // ══════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `대기질 실패해도 날씨는 표시`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.failure(Exception("API 에러"))
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(createMockWeather())
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem() as State.Success
            assertNull("대기질 데이터 없음", state.airQuality.data)
            assertNotNull("대기질 에러 있음", state.airQuality.error)
            assertNotNull("날씨 데이터 있음", state.weather.data)
            assertNull("날씨 에러 없음", state.weather.error)
        }
    }
    
    @Test
    fun `날씨 실패해도 대기질은 표시`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(createMockAirQuality())
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.failure(Exception("API 에러"))
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem() as State.Success
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
    fun `저장된 위치도 함께 로드`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        val savedLocations = listOf(
            SavedLocation(id = 1, displayName = "집", lat = 37.4, lng = 126.9),
            SavedLocation(id = 2, displayName = "회사", lat = 37.6, lng = 127.1)
        )
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { savedLocationRepository.getAll() } returns flowOf(savedLocations)
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(createMockAirQuality())
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(createMockWeather())
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.multiState.test {
            val state = awaitItem()
            assertEquals("3개 위치", 3, state.locations.size)
            assertTrue("첫 번째는 현재 위치", state.locations[0].isCurrentLocation)
            assertEquals("집", state.locations[1].locationName)
            assertEquals("회사", state.locations[2].locationName)
        }
    }
    
    @Test
    fun `각 위치별 State 확인`() = runTest {
        // Given
        val mockLocation = createMockLocation(37.5, 127.0)
        val savedLocations = listOf(
            SavedLocation(id = 1, displayName = "집", lat = 37.4, lng = 126.9)
        )
        coEvery { locationProvider.getCurrentLocation() } returns mockLocation
        coEvery { savedLocationRepository.getAll() } returns flowOf(savedLocations)
        coEvery { airQualityRepository.fetch(any(), any()) } returns Result.success(createMockAirQuality())
        coEvery { weatherRepository.fetch(any(), any()) } returns Result.success(createMockWeather())
        
        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        viewModel.multiState.test {
            val state = awaitItem()
            state.locations.forEach { location ->
                assertTrue("각 위치는 Success 상태", location.state is State.Success)
            }
        }
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // Mock 데이터
    // ══════════════════════════════════════════════════════════════════════════
    
    private fun createMockAirQuality() = AirQuality(
        aqi = 0,
        pm25 = PmData(15.0, AqiLevel.GOOD, "중구", false),
        pm10 = PmData(30.0, AqiLevel.GOOD, "중구", false),
        mainStationName = "중구 (서울)",
        updatedAt = "2026-03-18 07:00",
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
