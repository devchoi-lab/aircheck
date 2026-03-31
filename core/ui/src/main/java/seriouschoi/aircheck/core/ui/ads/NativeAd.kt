package seriouschoi.aircheck.core.ui.ads

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
 * "광고" 라벨이 NativeAdView 안에 포함되어 AdMob 정책 준수
 * 
 * @param adUnitId 광고 단위 ID
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
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .defaultMinSize(minHeight = AD_MIN_HEIGHT)  // 로딩 중 최소 높이
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        if (nativeAd != null) {
            // 네이티브 광고 (XML에 "광고" 라벨 포함)
            AndroidView(
                factory = { ctx ->
                    LayoutInflater.from(ctx)
                        .inflate(UiR.layout.native_ad_layout, null) as NativeAdView
                },
                update = { adView ->
                    nativeAd?.let { ad ->
                        populateNativeAdView(ad, adView)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
        // 로딩 중에는 빈 Box 유지 (높이 예약)
    }
}

private val AD_MIN_HEIGHT = 100.dp

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
    
    // AdChoices (우측 상단 정보 아이콘)
    adView.adChoicesView = adView.findViewById(UiR.id.ad_choices)
    
    // 광고 등록
    adView.setNativeAd(nativeAd)
}


