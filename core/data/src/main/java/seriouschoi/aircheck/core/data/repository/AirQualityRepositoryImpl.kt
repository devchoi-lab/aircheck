package seriouschoi.aircheck.core.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import seriouschoi.aircheck.core.domain.model.AirQuality
import seriouschoi.aircheck.core.domain.model.AqiLevel
import seriouschoi.aircheck.core.domain.model.PmData
import seriouschoi.aircheck.core.domain.repository.AirQualityRepository
import seriouschoi.aircheck.core.network.api.AirKoreaApiClient
import seriouschoi.aircheck.core.network.api.AirKoreaItem
import seriouschoi.aircheck.core.network.api.AirKoreaResponse
import seriouschoi.aircheck.core.network.api.NominatimResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

private data class NearestStationInfo(
    val stationName: String,
    val addr: String,
    val distance: Double
)

@Singleton
class AirQualityRepositoryImpl @Inject constructor(
    private val apiClient: AirKoreaApiClient
) : AirQualityRepository {

    override suspend fun fetch(lat: Double, lng: Double): Result<AirQuality> = withContext(Dispatchers.IO) {
        try {
            val nearbyStations = findNearbyStations(lat, lng)
            
            if (nearbyStations.isNotEmpty()) {
                return@withContext fetchWithSeparateFallback(nearbyStations)
            }
            
            fetchByKeywordMatching(lat, lng)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchWithSeparateFallback(stations: List<NearestStationInfo>): Result<AirQuality> {
        val mainStation = stations.first()
        val mainDisplayName = "${mainStation.stationName} (${extractSidoFromAddr(mainStation.addr)})"
        
        val mainData = fetchStationData(mainStation.stationName)
            ?: return Result.failure(Exception("${mainStation.stationName} 측정소 데이터가 없어요"))
        
        var pm25Data: PmData? = mainData.pm25Value.toDoubleOrNull()?.let { value ->
            PmData(value, AqiLevel.fromPm25(value), mainDisplayName, isFromFallback = false)
        }
        if (pm25Data == null) {
            pm25Data = findPm25FromFallback(stations.drop(1))
        }
        
        var pm10Data: PmData? = mainData.pm10Value.toDoubleOrNull()?.let { value ->
            PmData(value, AqiLevel.fromPm10(value), mainDisplayName, isFromFallback = false)
        }
        if (pm10Data == null) {
            pm10Data = findPm10FromFallback(stations.drop(1))
        }
        
        if (pm25Data == null && pm10Data == null) {
            return Result.failure(Exception("주변 측정소에서 미세먼지 데이터를 찾을 수 없어요"))
        }
        
        val khaiLevel = AqiLevel.fromKhaiGrade(mainData.khaiGrade)
        val levels = listOfNotNull(
            khaiLevel.takeIf { it != AqiLevel.UNKNOWN },
            pm25Data?.level?.takeIf { it != AqiLevel.UNKNOWN },
            pm10Data?.level?.takeIf { it != AqiLevel.UNKNOWN }
        )
        val worstLevel = levels.maxByOrNull { it.ordinal } ?: AqiLevel.UNKNOWN
        
        return Result.success(AirQuality(
            aqi = mainData.khaiValue.toIntOrNull() ?: -1,
            pm25 = pm25Data,
            pm10 = pm10Data,
            mainStationName = mainDisplayName,
            updatedAt = mainData.dataTime,
            level = khaiLevel,
            worstLevel = worstLevel
        ))
    }

    private fun fetchStationData(stationName: String): AirKoreaItem? = try {
        val body = apiClient.getStationData(stationName)
        apiClient.json.decodeFromString<AirKoreaResponse>(body).response?.body?.items?.firstOrNull()
    } catch (e: Exception) {
        null
    }

    private fun findPm25FromFallback(stations: List<NearestStationInfo>): PmData? {
        for (station in stations) {
            val data = fetchStationData(station.stationName) ?: continue
            val value = data.pm25Value.toDoubleOrNull() ?: continue
            val displayName = "${station.stationName} (${extractSidoFromAddr(station.addr)})"
            return PmData(value, AqiLevel.fromPm25(value), displayName, isFromFallback = true)
        }
        return null
    }

    private fun findPm10FromFallback(stations: List<NearestStationInfo>): PmData? {
        for (station in stations) {
            val data = fetchStationData(station.stationName) ?: continue
            val value = data.pm10Value.toDoubleOrNull() ?: continue
            val displayName = "${station.stationName} (${extractSidoFromAddr(station.addr)})"
            return PmData(value, AqiLevel.fromPm10(value), displayName, isFromFallback = true)
        }
        return null
    }

    private fun findNearbyStations(lat: Double, lng: Double): List<NearestStationInfo> {
        return try {
            val body = apiClient.getStationList()
            
            if (body.startsWith("Forbidden") || body.startsWith("Unauthorized")) {
                return emptyList()
            }
            
            val jsonObj = apiClient.json.parseToJsonElement(body)
            val items = jsonObj.jsonObject["response"]?.jsonObject
                ?.get("body")?.jsonObject
                ?.get("items")?.jsonArray ?: return emptyList()
            
            val stationList = mutableListOf<NearestStationInfo>()
            
            for (item in items) {
                val obj = item.jsonObject
                val stationName = obj["stationName"]?.jsonPrimitive?.content ?: continue
                val addr = obj["addr"]?.jsonPrimitive?.content ?: ""
                val dmX = obj["dmX"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: continue
                val dmY = obj["dmY"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: continue
                
                val distance = haversineDistance(lat, lng, dmY, dmX)
                stationList.add(NearestStationInfo(stationName, addr, distance))
            }
            
            stationList.sortedBy { it.distance }.take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return R * c
    }

    private fun fetchByKeywordMatching(lat: Double, lng: Double): Result<AirQuality> {
        val (sidoName, keywords) = reverseGeocode(lat, lng)
            ?: return Result.failure(Exception("위치 정보를 확인할 수 없어요"))
        
        val allItems = fetchSidoItems(sidoName)
        
        val matched = findBestMatch(allItems, keywords)
            ?: allItems.firstOrNull { it.pm25Value != "-" && it.pm25Value.isNotEmpty() }
            ?: allItems.firstOrNull()
            ?: return Result.failure(Exception("$sidoName 측정 데이터가 없어요"))
        
        return Result.success(toAirQuality(matched))
    }

    private fun reverseGeocode(lat: Double, lng: Double): Pair<String, List<String>>? = try {
        val body = apiClient.reverseGeocode(lat, lng)
        val response = apiClient.json.decodeFromString<NominatimResponse>(body)
        val addr = response.address ?: return null
        val sido = inferSidoName(addr)
        val rawNames = listOfNotNull(
            addr.quarter, addr.suburb, addr.neighbourhood,
            addr.city_district, addr.village, addr.town, addr.city, addr.county
        ).filter { it.isNotBlank() }
        val keywords = rawNames.flatMap { name ->
            listOf(name, name.replace(Regex("[시군구동읍면리]$"), "").trim())
        }.filter { it.length >= 2 }.distinct()
        Pair(sido, keywords)
    } catch (e: Exception) {
        null
    }

    private fun inferSidoName(addr: seriouschoi.aircheck.core.network.api.NominatimAddress): String {
        if (addr.state.isNotBlank()) return koreanSidoName(addr.state)
        val city = addr.city ?: ""
        val directCities = mapOf(
            "서울" to "서울", "부산" to "부산", "대구" to "대구", "인천" to "인천",
            "광주" to "광주", "대전" to "대전", "울산" to "울산", "세종" to "세종"
        )
        for ((key, value) in directCities) {
            if (city.contains(key)) return value
        }
        val gyeonggiCities = listOf(
            "수원", "성남", "안양", "안산", "부천", "광명", "평택", "과천", "오산", "시흥",
            "군포", "의왕", "하남", "이천", "용인", "파주", "안성", "김포", "화성", "광주",
            "양주", "포천", "고양", "의정부", "동두천", "구리", "남양주", "여주", "양평",
            "가평", "연천"
        )
        for (gc in gyeonggiCities) {
            if (city.contains(gc)) return "경기"
        }
        val county = addr.county ?: ""
        for (gc in gyeonggiCities) {
            if (county.contains(gc)) return "경기"
        }
        return "경기"
    }

    private fun fetchSidoItems(sidoName: String): List<AirKoreaItem> {
        val body = apiClient.getSidoData(sidoName)
        if (body.startsWith("Unauthorized") || body.startsWith("Forbidden")) {
            throw Exception("API 인증 오류: $body")
        }
        return apiClient.json.decodeFromString<AirKoreaResponse>(body).response?.body?.items
            ?: throw Exception("$sidoName 응답 파싱 실패")
    }

    private fun findBestMatch(items: List<AirKoreaItem>, keywords: List<String>): AirKoreaItem? {
        for (keyword in keywords) {
            val match = items.firstOrNull { item ->
                item.stationName.contains(keyword) || keyword.contains(item.stationName)
            }
            if (match != null) return match
        }
        return null
    }

    private fun toAirQuality(item: AirKoreaItem): AirQuality {
        val displayName = if (item.sidoName.isNotBlank()) 
            "${item.stationName} (${item.sidoName})" else item.stationName
        
        val pm25Value = item.pm25Value.toDoubleOrNull()
        val pm10Value = item.pm10Value.toDoubleOrNull()
        
        val pm25Data = pm25Value?.let { 
            PmData(it, AqiLevel.fromPm25(it), displayName, isFromFallback = false)
        }
        val pm10Data = pm10Value?.let { 
            PmData(it, AqiLevel.fromPm10(it), displayName, isFromFallback = false)
        }
        
        val khaiLevel = AqiLevel.fromKhaiGrade(item.khaiGrade)
        val levels = listOfNotNull(
            khaiLevel.takeIf { it != AqiLevel.UNKNOWN },
            pm25Data?.level?.takeIf { it != AqiLevel.UNKNOWN },
            pm10Data?.level?.takeIf { it != AqiLevel.UNKNOWN }
        )
        val worstLevel = levels.maxByOrNull { it.ordinal } ?: AqiLevel.UNKNOWN
        
        return AirQuality(
            aqi = item.khaiValue.toIntOrNull() ?: -1,
            pm25 = pm25Data,
            pm10 = pm10Data,
            mainStationName = displayName,
            updatedAt = item.dataTime,
            level = khaiLevel,
            worstLevel = worstLevel
        )
    }

    private fun extractSidoFromAddr(addr: String): String {
        if (addr.isBlank()) return ""
        val firstPart = addr.split(" ").firstOrNull() ?: return ""
        return when {
            firstPart.contains("서울") -> "서울"
            firstPart.contains("부산") -> "부산"
            firstPart.contains("대구") -> "대구"
            firstPart.contains("인천") -> "인천"
            firstPart.contains("광주") -> "광주"
            firstPart.contains("대전") -> "대전"
            firstPart.contains("울산") -> "울산"
            firstPart.contains("세종") -> "세종"
            firstPart.contains("경기") -> "경기"
            firstPart.contains("강원") -> "강원"
            firstPart.contains("충북") || firstPart.contains("충청북") -> "충북"
            firstPart.contains("충남") || firstPart.contains("충청남") -> "충남"
            firstPart.contains("전북") || firstPart.contains("전라북") -> "전북"
            firstPart.contains("전남") || firstPart.contains("전라남") -> "전남"
            firstPart.contains("경북") || firstPart.contains("경상북") -> "경북"
            firstPart.contains("경남") || firstPart.contains("경상남") -> "경남"
            firstPart.contains("제주") -> "제주"
            else -> firstPart
        }
    }

    private fun koreanSidoName(state: String): String = when {
        state.contains("서울") -> "서울"; state.contains("부산") -> "부산"
        state.contains("대구") -> "대구"; state.contains("인천") -> "인천"
        state.contains("광주") -> "광주"; state.contains("대전") -> "대전"
        state.contains("울산") -> "울산"; state.contains("세종") -> "세종"
        state.contains("경기") -> "경기"; state.contains("강원") -> "강원"
        state.contains("충청북") || state.contains("충북") -> "충북"
        state.contains("충청남") || state.contains("충남") -> "충남"
        state.contains("전라북") || state.contains("전북") -> "전북"
        state.contains("전라남") || state.contains("전남") -> "전남"
        state.contains("경상북") || state.contains("경북") -> "경북"
        state.contains("경상남") || state.contains("경남") -> "경남"
        state.contains("제주") -> "제주"
        else -> "서울"
    }
}
