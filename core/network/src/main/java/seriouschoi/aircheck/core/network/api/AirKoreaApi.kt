package seriouschoi.aircheck.core.network.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

// ── API 응답 모델 ──────────────────────────────────────────────────────────

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

// ── API 클라이언트 ─────────────────────────────────────────────────────────

@Singleton
class AirKoreaApiClient @Inject constructor() {
    
    private val apiKey = "34dddd00aa791a78203988d8151ead27a5b0c8c64fc5c0dea94c4b64160b2df5"
    
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .addHeader("User-Agent", "AirCheckApp/1.0")
                .build())
        }
        .build()
    
    val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    fun getStationList(): String {
        val url = "https://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getMsrstnList" +
                "?pageNo=1&numOfRows=700&returnType=json" +
                "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.1"
        return get(url)
    }
    
    fun getStationData(stationName: String): String {
        val url = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" +
                "?stationName=${java.net.URLEncoder.encode(stationName, "UTF-8")}" +
                "&dataTerm=DAILY&pageNo=1&numOfRows=1&returnType=json" +
                "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.0"
        return get(url)
    }
    
    fun getSidoData(sidoName: String): String {
        val url = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty" +
                "?sidoName=${java.net.URLEncoder.encode(sidoName, "UTF-8")}" +
                "&pageNo=1&numOfRows=100&returnType=json" +
                "&serviceKey=${java.net.URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.0"
        return get(url)
    }
    
    fun reverseGeocode(lat: Double, lng: Double): String {
        val url = "https://nominatim.openstreetmap.org/reverse" +
                "?lat=$lat&lon=$lng&format=json&accept-language=ko&addressdetails=1"
        return get(url)
    }
    
    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { it.body?.string() ?: "" }
    }
}
