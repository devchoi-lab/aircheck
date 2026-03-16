package seriouschoi.aircheck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.*

// ── 에어코리아 응답 모델 ──────────────────────────────────────────────────────

@Serializable
data class AirKoreaResponse(val response: AirKoreaBody? = null)

@Serializable
data class AirKoreaBody(val body: AirKoreaItems? = null)

@Serializable
data class AirKoreaItems(val items: List<AirKoreaItem> = emptyList())

@Serializable
data class AirKoreaItem(
    val stationName: String = "",
    val sidoName: String = "",
    val dataTime: String = "",
    val pm10Value: String = "-",
    val pm25Value: String = "-",
    val khaiValue: String = "-",
    val khaiGrade: String = "0"
)

// ── 측정소 정보 모델 ──────────────────────────────────────────────────────────

/** 좌표 기반 매칭 결과 */
data class NearestStationInfo(
    val stationName: String,
    val addr: String,     // 주소 (시/도 추출용)
    val distance: Double  // 거리 (km)
)

// ── Nominatim 역지오코딩 모델 ─────────────────────────────────────────────────

@Serializable
data class NominatimResponse(val address: NominatimAddress? = null)

@Serializable
data class NominatimAddress(
    val state: String = "",
    val city: String? = null,
    val city_district: String? = null,
    val suburb: String? = null,
    val quarter: String? = null,
    val town: String? = null,
    val village: String? = null,
    val county: String? = null,
    val neighbourhood: String? = null
)

// ── 도메인 모델 ───────────────────────────────────────────────────────────────

/** PM 데이터 (값 + 출처 측정소) */
data class PmData(
    val value: Double,
    val level: AqiLevel,
    val stationName: String,      // 데이터 출처 측정소
    val isFromFallback: Boolean   // 폴백 측정소에서 가져왔는지
)

data class AirQuality(
    val aqi: Int,
    val pm25: PmData?,             // PM2.5 데이터 + 출처
    val pm10: PmData?,             // PM10 데이터 + 출처
    val mainStationName: String,   // 메인 측정소 (AQI 출처)
    val updatedAt: String,
    val level: AqiLevel,           // 에어코리아 통합대기환경지수
    val worstLevel: AqiLevel       // AQI/PM2.5/PM10 중 최악 등급
)

enum class AqiLevel(val label: String, val emoji: String, val colorHex: Long) {
    BEST("최고", "🌟", 0xFF00E5FF),
    GOOD("좋음", "😊", 0xFF00C8A0),
    FAIR("양호", "🙂", 0xFF8BC34A),
    MODERATE("보통", "😐", 0xFFFFD700),
    BAD("나쁨", "😮", 0xFFFF9800),
    QUITE_BAD("상당히 나쁨", "😷", 0xFFFF5722),
    VERY_BAD("매우 나쁨", "🤢", 0xFF9400D3),
    WORST("최악", "☠️", 0xFF7E0023),
    UNKNOWN("알 수 없음", "❓", 0xFF9E9E9E);

    companion object {
        fun fromKhaiGrade(grade: String): AqiLevel = when (grade) {
            "1" -> GOOD; "2" -> MODERATE; "3" -> BAD; "4" -> VERY_BAD
            else -> UNKNOWN
        }
        // PM2.5 기준 (μg/m³): 0~5 최고, 6~9 좋음, 10~12 양호, 13~15 보통, 16~37 나쁨, 38~50 상당히 나쁨, 51~75 매우 나쁨, 76~ 최악
        fun fromPm25(v: Double): AqiLevel = when {
            v <= 5.0 -> BEST
            v <= 9.0 -> GOOD
            v <= 12.0 -> FAIR
            v <= 15.0 -> MODERATE
            v <= 37.0 -> BAD
            v <= 50.0 -> QUITE_BAD
            v <= 75.0 -> VERY_BAD
            else -> WORST
        }
        // PM10 기준 (μg/m³): 0~10 최고, 11~22 좋음, 23~35 양호, 36~45 보통, 46~75 나쁨, 76~100 상당히 나쁨, 101~150 매우 나쁨, 151~ 최악
        fun fromPm10(v: Double): AqiLevel = when {
            v <= 10.0 -> BEST
            v <= 22.0 -> GOOD
            v <= 35.0 -> FAIR
            v <= 45.0 -> MODERATE
            v <= 75.0 -> BAD
            v <= 100.0 -> QUITE_BAD
            v <= 150.0 -> VERY_BAD
            else -> WORST
        }
    }
}

// ── Repository ────────────────────────────────────────────────────────────────

class AirQualityRepository {

    private val apiKey = "34dddd00aa791a78203988d8151ead27a5b0c8c64fc5c0dea94c4b64160b2df5"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader("User-Agent", "AirCheckApp/1.0").build())
        }
        .build()

    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun fetch(lat: Double, lng: Double): Result<AirQuality> = withContext(Dispatchers.IO) {
        try {
            // Step 1: 좌표 기반으로 가까운 측정소들 찾기 (거리순 정렬)
            val nearbyStations = findNearbyStations(lat, lng)
            
            if (nearbyStations.isNotEmpty()) {
                // Step 2: PM2.5/PM10 각각 따로 폴백하여 데이터 수집
                return@withContext fetchWithSeparateFallback(nearbyStations)
            }
            
            // 좌표 기반 실패 시 → 기존 키워드 매칭 폴백
            fetchByKeywordMatching(lat, lng)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /** PM2.5/PM10 각각 별도로 폴백하여 데이터 수집 */
    private fun fetchWithSeparateFallback(stations: List<NearestStationInfo>): Result<AirQuality> {
        val mainStation = stations.first()
        val mainDisplayName = "${mainStation.stationName} (${extractSidoFromAddr(mainStation.addr)})"
        
        // 메인 측정소 데이터 조회
        val mainData = fetchStationData(mainStation.stationName)
            ?: return Result.failure(Exception("${mainStation.stationName} 측정소 데이터가 없어요"))
        
        // PM2.5 데이터 수집 (메인에 없으면 폴백)
        var pm25Data: PmData? = mainData.pm25Value.toDoubleOrNull()?.let { value ->
            PmData(value, AqiLevel.fromPm25(value), mainDisplayName, isFromFallback = false)
        }
        if (pm25Data == null) {
            pm25Data = findPm25FromFallback(stations.drop(1))
        }
        
        // PM10 데이터 수집 (메인에 없으면 폴백)
        var pm10Data: PmData? = mainData.pm10Value.toDoubleOrNull()?.let { value ->
            PmData(value, AqiLevel.fromPm10(value), mainDisplayName, isFromFallback = false)
        }
        if (pm10Data == null) {
            pm10Data = findPm10FromFallback(stations.drop(1))
        }
        
        // 둘 다 없으면 실패
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
    
    /** 측정소 원시 데이터 조회 */
    private fun fetchStationData(stationName: String): AirKoreaItem? {
        return try {
            val url = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" +
                    "?stationName=${java.net.URLEncoder.encode(stationName, "UTF-8")}" +
                    "&dataTerm=DAILY&pageNo=1&numOfRows=1&returnType=json" +
                    "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                    "&ver=1.0"
            val body = get(url)
            json.decodeFromString<AirKoreaResponse>(body).response?.body?.items?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /** PM2.5 데이터 있는 폴백 측정소 찾기 */
    private fun findPm25FromFallback(stations: List<NearestStationInfo>): PmData? {
        for (station in stations) {
            val data = fetchStationData(station.stationName) ?: continue
            val value = data.pm25Value.toDoubleOrNull() ?: continue
            val displayName = "${station.stationName} (${extractSidoFromAddr(station.addr)})"
            return PmData(value, AqiLevel.fromPm25(value), displayName, isFromFallback = true)
        }
        return null
    }
    
    /** PM10 데이터 있는 폴백 측정소 찾기 */
    private fun findPm10FromFallback(stations: List<NearestStationInfo>): PmData? {
        for (station in stations) {
            val data = fetchStationData(station.stationName) ?: continue
            val value = data.pm10Value.toDoubleOrNull() ?: continue
            val displayName = "${station.stationName} (${extractSidoFromAddr(station.addr)})"
            return PmData(value, AqiLevel.fromPm10(value), displayName, isFromFallback = true)
        }
        return null
    }

    /**
     * 좌표 기반 가까운 측정소들 찾기 (거리순 정렬, 최대 10개)
     * getMsrstnList로 전체 측정소 목록 조회 후 Haversine 거리 계산
     */
    private fun findNearbyStations(lat: Double, lng: Double): List<NearestStationInfo> {
        return try {
            // 전체 측정소 목록 조회 (좌표 포함, ver=1.1)
            val url = "https://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getMsrstnList" +
                    "?pageNo=1&numOfRows=700&returnType=json" +
                    "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                    "&ver=1.1"
            val body = get(url)
            
            // API 에러 체크
            if (body.startsWith("Forbidden") || body.startsWith("Unauthorized")) {
                return emptyList()
            }
            
            val jsonObj = json.parseToJsonElement(body)
            val items = jsonObj.jsonObject["response"]?.jsonObject
                ?.get("body")?.jsonObject
                ?.get("items")?.jsonArray ?: return emptyList()
            
            // 각 측정소와의 거리 계산 후 정렬
            val stationList = mutableListOf<NearestStationInfo>()
            
            for (item in items) {
                val obj = item.jsonObject
                val stationName = obj["stationName"]?.jsonPrimitive?.content ?: continue
                val addr = obj["addr"]?.jsonPrimitive?.content ?: ""
                // ver 1.1에서는 dmX가 경도(lng), dmY가 위도(lat)
                val dmX = obj["dmX"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: continue
                val dmY = obj["dmY"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: continue
                
                val distance = haversineDistance(lat, lng, dmY, dmX)
                stationList.add(NearestStationInfo(stationName, addr, distance))
            }
            
            // 거리순 정렬 후 상위 10개 반환
            stationList.sortedBy { it.distance }.take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Haversine 공식으로 두 좌표 간 거리 계산 (km)
     */
    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0 // 지구 반지름 (km)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return R * c
    }

    /** 측정소명으로 실시간 데이터 조회 */
    private fun fetchByStation(stationName: String, addr: String): Result<AirQuality> = try {
        val url = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" +
                "?stationName=${java.net.URLEncoder.encode(stationName, "UTF-8")}" +
                "&dataTerm=DAILY&pageNo=1&numOfRows=1&returnType=json" +
                "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.0"
        val body = get(url)
        val response = json.decodeFromString<AirKoreaResponse>(body)
        val item = response.response?.body?.items?.firstOrNull()
            ?: return Result.failure(Exception("$stationName 측정소 데이터가 없어요"))
        
        // 주소에서 시/도 추출 (예: "경기 부천시 소사본동..." → "경기")
        val sidoName = extractSidoFromAddr(addr)
        Result.success(toAirQuality(item, stationName, sidoName))
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /** 주소에서 시/도명 추출 */
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

    /** 기존 키워드 매칭 방식 (폴백) */
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

    /** Nominatim 역지오코딩 → (시도명, 키워드 목록) */
    private fun reverseGeocode(lat: Double, lng: Double): Pair<String, List<String>>? = try {
        val url = "https://nominatim.openstreetmap.org/reverse" +
                "?lat=$lat&lon=$lng&format=json&accept-language=ko&addressdetails=1"
        val body = get(url)
        val response = json.decodeFromString<NominatimResponse>(body)
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

    private fun inferSidoName(addr: NominatimAddress): String {
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
        val url = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty" +
                "?sidoName=${java.net.URLEncoder.encode(sidoName, "UTF-8")}" +
                "&pageNo=1&numOfRows=100&returnType=json" +
                "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.0"
        val body = get(url)
        if (body.startsWith("Unauthorized") || body.startsWith("Forbidden")) {
            throw Exception("API 인증 오류: $body")
        }
        return json.decodeFromString<AirKoreaResponse>(body).response?.body?.items
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

    /** API 응답에서 AirQuality 생성 (stationName/sidoName 포함된 경우) - 키워드 매칭용 */
    private fun toAirQuality(item: AirKoreaItem): AirQuality {
        return toAirQuality(item, item.stationName, item.sidoName)
    }
    
    /** API 응답에서 AirQuality 생성 (stationName/sidoName 별도 전달) - 키워드 매칭용 */
    private fun toAirQuality(item: AirKoreaItem, stationName: String, sidoName: String): AirQuality {
        val displayName = if (sidoName.isNotBlank()) "$stationName ($sidoName)" else stationName
        
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

    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { it.body?.string() ?: "" }
    }
}
