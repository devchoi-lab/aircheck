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
 * 테스트용 광고 ID
 */
object AdUnitIds {
    // Google 공식 테스트 광고 ID
    const val BANNER_TEST = "ca-app-pub-3940256099942544/6300978111"
    const val NATIVE_TEST = "ca-app-pub-3940256099942544/2247696110"
    
    // TODO: 실제 광고 ID로 교체
    const val BANNER_WEATHER_BOTTOM = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
    const val NATIVE_WEATHER_CARD = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
}
