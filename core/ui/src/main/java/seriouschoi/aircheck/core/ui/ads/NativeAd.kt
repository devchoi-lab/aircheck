package seriouschoi.aircheck.core.ui.ads

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import seriouschoi.aircheck.core.resource.R as CoreR
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import seriouschoi.aircheck.core.ui.R as UiR

/**
 * 네이티브 광고 Composable
 * 상단에 "광고" 라벨을 명확히 표시하여 어르신들도 구분 가능
 * 
 * @param adUnitId 광고 단위 ID (테스트: ca-app-pub-3940256099942544/2247696110)
 * @param modifier Modifier
 */
@Composable
fun NativeAdCard(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    DisposableEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd = ad
                isLoading = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
        
        adLoader.loadAd(AdRequest.Builder().build())
        
        onDispose {
            nativeAd?.destroy()
        }
    }
    
    // 광고 로딩 실패 시 표시 안 함
    if (!isLoading && nativeAd == null) return
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(AD_HEIGHT)  // 고정 높이로 레이아웃 시프트 방지
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // 상단 "광고" 라벨 (어르신용 명확한 구분)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(CoreR.string.label_ad),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (nativeAd != null) {
            // 네이티브 광고 콘텐츠
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx)
                        .inflate(UiR.layout.native_ad_layout, null) as NativeAdView
                    view
                },
                update = { adView ->
                    nativeAd?.let { ad ->
                        populateNativeAdView(ad, adView)
                    }
                },
                modifier = Modifier.padding(12.dp)
            )
        } else {
            // 로딩 중 placeholder
            Spacer(Modifier.weight(1f))
        }
    }
}

private val AD_HEIGHT = 100.dp

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // 헤드라인
    adView.headlineView = adView.findViewById(UiR.id.ad_headline)
    (adView.headlineView as? TextView)?.text = nativeAd.headline
    
    // 본문
    adView.bodyView = adView.findViewById(UiR.id.ad_body)
    (adView.bodyView as? TextView)?.text = nativeAd.body
    
    // 아이콘
    adView.iconView = adView.findViewById(UiR.id.ad_icon)
    nativeAd.icon?.let { icon ->
        (adView.iconView as? ImageView)?.setImageDrawable(icon.drawable)
    }
    
    // CTA 버튼
    adView.callToActionView = adView.findViewById(UiR.id.ad_call_to_action)
    (adView.callToActionView as? Button)?.text = nativeAd.callToAction
    
    // 광고 등록
    adView.setNativeAd(nativeAd)
}


