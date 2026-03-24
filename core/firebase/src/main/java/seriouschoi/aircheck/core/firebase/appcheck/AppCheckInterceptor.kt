package seriouschoi.aircheck.core.firebase.appcheck

import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCheckInterceptor @Inject constructor() : Interceptor {
    
    companion object {
        private const val HEADER_APP_CHECK = "X-Firebase-AppCheck"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = getAppCheckToken()
        
        val request = chain.request().newBuilder().apply {
            token?.let { addHeader(HEADER_APP_CHECK, it) }
        }.build()
        
        return chain.proceed(request)
    }
    
    private fun getAppCheckToken(): String? {
        return runBlocking {
            try {
                FirebaseAppCheck.getInstance()
                    .getAppCheckToken(false)
                    .await()
                    .token
            } catch (e: Exception) {
                null
            }
        }
    }
}
