import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
}

android {
    namespace = "seriouschoi.aircheck"
    compileSdk = 35

    signingConfigs {
        getByName("debug") {
            // 프로젝트 내 debug.keystore가 있으면 사용 (App Check용 고정 SHA-256)
            // 없으면 기본 Android debug keystore 사용 (CI 환경)
            val debugKeystoreFile = file("debug.keystore")
            if (debugKeystoreFile.exists()) {
                storeFile = debugKeystoreFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        create("release") {
            val localProperties = rootProject.file("local.properties")
            if (localProperties.exists()) {
                val props = Properties()
                localProperties.inputStream().use { props.load(it) }
                storeFile = file(props.getProperty("RELEASE_STORE_FILE"))
                storePassword = props.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = props.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = props.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "seriouschoi.aircheck"
        minSdk = 26
        targetSdk = 35
        // CI: VERSION_CODE 환경변수, 로컬: 수동 관리
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 6
        versionName = "1.0.0"
    }
    
    buildTypes {
        debug {
            buildConfigField("String", "APP_CHECK_DEBUG_TOKEN", "\"b69f4d24-92dd-4e27-ba86-247ca838d843\"")
            // AdMob 테스트 App ID
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "APP_CHECK_DEBUG_TOKEN", "\"\"")
            // 실제 AdMob App ID
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-8381903489844516~7225419989"
        }
    }

    // 빌드 파일명에 버전정보 + 날짜시간 추가
    val buildTime = SimpleDateFormat("yyyyMMdd-HHmm").format(Date())
    val vCode = defaultConfig.versionCode
    val vName = defaultConfig.versionName
    
    // APK 파일명
    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "aircheck-${variant.buildType.name}-${vCode}-${vName}-${buildTime}.apk"
        }
    }
    
    // AAB 파일명 (base.archivesName)
    base.archivesName.set("aircheck-${vCode}-${vName}-${buildTime}")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures { 
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:firebase"))
    implementation(project(":feature:weather"))
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.navigation.compose)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    
    // WorkManager
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    
    // Firebase
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    
    // Ads
    implementation(libs.play.services.ads)
    
    debugImplementation(libs.compose.ui.tooling)
}
