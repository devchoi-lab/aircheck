plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "seriouschoi.aircheck.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

ksp {
    // Room compiler에게 schema JSON을 어디에 출력할지 알려준다.
    // (이 arg가 없으면 @Database(exportSchema = true) 가 무력화되고 export 워닝이 뜬다)
    // 출력 위치: core/data/schemas/<DB 클래스 FQN>/<version>.json
    // 매 빌드마다 자동 생성/덮어쓰기 되며, 향후 마이그레이션 검증과 auto-migration 입력으로 사용된다.
    arg("room.schemaLocation", "$projectDir/schemas")

    // Room compiler를 incremental annotation processor로 동작시킨다.
    // 변경된 Entity/DAO 파일만 재처리하여 빌드 시간을 단축한다.
    // (false면 매 빌드마다 모든 Room 클래스를 전체 재처리)
    arg("room.incremental", "true")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:network"))
    
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)


    // Location
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Android Instrumented Test
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
