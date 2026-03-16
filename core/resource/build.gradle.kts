plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "seriouschoi.aircheck.core.resource"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}
