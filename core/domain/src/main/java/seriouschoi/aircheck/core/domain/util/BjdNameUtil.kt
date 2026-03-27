package seriouschoi.aircheck.core.domain.util

/**
 * 법정동 주소를 축약: 2단계 + 끝단계
 * - 3단계 "서울특별시 강남구 청담동" → "강남구 청담동"
 * - 4단계 "경기도 성남시 분당구 정자동" → "성남시 정자동"
 * - 2단계 이하는 그대로 반환
 */
fun shortenBjdName(fullName: String): String {
    val parts = fullName.split(" ")
    return when {
        parts.size <= 2 -> fullName
        else -> "${parts[1]} ${parts.last()}"
    }
}
