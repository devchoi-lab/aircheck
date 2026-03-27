package seriouschoi.aircheck.core.ui.ads

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * 배너 광고 Composable
 * 
 * @param adUnitId 광고 단위 ID (테스트: ca-app-pub-3940256099942544/6300978111)
 * @param modifier Modifier
 */
@Composable
fun BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context: Context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

/**
 * 광고 ID
 * 
 * 사용: AdUnitIds.banner, AdUnitIds.native
 * - Debug 빌드: 테스트 광고
 * - Release 빌드: 실제 광고
 */
object AdUnitIds {
    // Google 공식 테스트 광고 ID
    private const val BANNER_TEST = "ca-app-pub-3940256099942544/6300978111"
    private const val NATIVE_TEST = "ca-app-pub-3940256099942544/2247696110"
    
    // 실제 광고 ID
    private const val BANNER_PROD = "ca-app-pub-8381903489844516/7057899025"
    private const val NATIVE_PROD = "ca-app-pub-8381903489844516/6742337702"
    
    // 빌드 타입에 따라 자동 선택
    val banner: String get() = if (isDebug) BANNER_TEST else BANNER_PROD
    val native: String get() = if (isDebug) NATIVE_TEST else NATIVE_PROD
    
    private val isDebug: Boolean
        get() = try {
            Class.forName("seriouschoi.aircheck.BuildConfig")
                .getField("DEBUG")
                .getBoolean(null)
        } catch (e: Exception) {
            true // 기본값: 테스트 광고
        }
}
