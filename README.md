# 에어체크 (AirCheck) 🌤️

현재 위치 기반 **대기질 + 날씨** 정보를 한눈에 보여주는 Android 앱

---

## 🎯 개발 원칙

> **"좋은 앱 먼저, 수익은 나중에"**

1. **사용성 우선** — 수익성 고민 전에 쓰기 좋은 앱부터 만든다
2. **기능 제한 없음** — 위젯, 알림 등 필요한 기능은 전부 만든다
3. **광고는 마지막** — 사용성을 해치지 않는 범위에서만 수익화

*수익성 때문에 기능을 막거나 고민하지 않는다.*

---

## 스크린샷

<!-- TODO: 스크린샷 추가 -->

## 주요 기능

- 🌡️ **현재 날씨** - 기온, 체감온도, 날씨 상태
- 💨 **대기질** - PM2.5, PM10, 통합 AQI (8단계 한국 기준)
- 📊 **48시간 예보** - 시간별 기온, 강수확률
- 👕 **추천 아이템** - 날씨 기반 옷차림, 우산 추천
- 🔄 **Pull-to-Refresh** - 아래로 당겨서 새로고침

## 아키텍처

### 멀티모듈 + Port-Adapter (Hexagonal) 패턴

```
┌─────────────────────────────────────────────────────────┐
│                        :app                              │
│              (DI 설정, Navigation, Entry Point)          │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                   :feature:weather                       │
│            (UI, ViewModel, 화면별 Navigation)            │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                    :core:domain                          │
│        (Port 인터페이스, 도메인 모델, 비즈니스 로직)       │
│                                                          │
│  ┌─────────────────┐  ┌─────────────────┐               │
│  │ AirQualityRepo  │  │  WeatherRepo    │  ← Port       │
│  │   (interface)   │  │   (interface)   │               │
│  └─────────────────┘  └─────────────────┘               │
└─────────────────────────▲───────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────┐
│                     :core:data                           │
│              (Adapter - Port 구현체)                     │
│                                                          │
│  ┌─────────────────┐  ┌─────────────────┐               │
│  │ AirQualityRepo  │  │  WeatherRepo    │  ← Adapter    │
│  │     Impl        │  │     Impl        │               │
│  └────────┬────────┘  └────────┬────────┘               │
└───────────┼─────────────────────┼───────────────────────┘
            │                     │
┌───────────▼─────────────────────▼───────────────────────┐
│                    :core:network                         │
│              (API 클라이언트, DTO)                       │
│                                                          │
│  ┌─────────────────┐  ┌─────────────────┐               │
│  │   AirKoreaApi   │  │   OpenMeteoApi  │               │
│  └─────────────────┘  └─────────────────┘               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                   :core:resource                         │
│          (공통 리소스 - strings.xml, UiText)             │
└─────────────────────────────────────────────────────────┘
```

### 모듈 설명

| 모듈 | 역할 |
|------|------|
| `:app` | 앱 진입점, DI 설정, Navigation Host |
| `:feature:weather` | 날씨 화면 UI, ViewModel, 화면 라우팅 |
| `:core:domain` | Port 인터페이스, 도메인 모델 (순수 Kotlin) |
| `:core:data` | Adapter 구현체 (Repository Impl) |
| `:core:network` | API 클라이언트, 네트워크 DTO |
| `:core:resource` | 공통 문자열 리소스, UiText |

### 왜 이 구조인가?

**Port-Adapter 패턴의 장점:**
- **테스트 용이** - Port를 Mock으로 교체 가능
- **유연한 교체** - API 변경 시 Adapter만 수정
- **의존성 역전** - domain이 외부 의존성 없음

**멀티모듈의 장점:**
- **빌드 속도** - 변경된 모듈만 재빌드
- **의존성 격리** - feature → domain ← data
- **확장성** - 새 feature 모듈 추가 용이

## 기술 스택

| 영역 | 기술 |
|------|------|
| **UI** | Jetpack Compose, Material3 |
| **Architecture** | MVI (Model-View-Intent) |
| **DI** | Hilt |
| **Async** | Kotlin Coroutines, Flow |
| **Network** | OkHttp, Kotlinx Serialization |
| **Navigation** | Type-safe Navigation (sealed interface) |
| **CI/CD** | GitHub Actions (Self-hosted runner) |
| **Distribution** | Firebase App Distribution |

## API

| API | 용도 | 비고 |
|-----|------|------|
| [에어코리아](https://www.airkorea.or.kr/) | 대기질 정보 | 한국환경공단 |
| [Open-Meteo](https://open-meteo.com/) | 날씨 정보 | 무료, API 키 불필요 |

### 대기질 등급 (8단계 한국 기준)

| 등급 | PM2.5 (㎍/㎥) | PM10 (㎍/㎥) |
|------|--------------|--------------|
| 🌟 최고 | 0-8 | 0-15 |
| 😊 좋음 | 9-15 | 16-30 |
| 🙂 양호 | 16-20 | 31-40 |
| 😐 보통 | 21-25 | 41-50 |
| 😮 나쁨 | 26-35 | 51-75 |
| 😷 상당히나쁨 | 36-50 | 76-100 |
| 😱 매우나쁨 | 51-75 | 101-150 |
| 💀 최악 | 76+ | 151+ |

## 빌드

### 요구사항
- JDK 17+
- Android SDK (compileSdk 35)

### 로컬 빌드
```bash
# 에어코리아 API 키 설정 (local.properties)
echo "AIRKOREA_API_KEY=your_api_key" >> local.properties

# 빌드
./gradlew :app:assembleDebug
```

### CI/CD
- **Push to main** → 자동 빌드 + Firebase 배포
- **Pull Request** → 빌드 검증만

## 프로젝트 구조

```
aircheck/
├── app/                          # 앱 모듈
│   ├── MainActivity.kt
│   ├── AirCheckApplication.kt
│   └── di/AppModule.kt
│
├── feature/
│   └── weather/                  # 날씨 feature
│       ├── ui/
│       │   ├── WeatherScreen.kt
│       │   ├── WeatherViewModel.kt
│       │   └── WeatherUiState.kt
│       └── navigation/
│           ├── WeatherRoute.kt
│           └── WeatherNavigation.kt
│
├── core/
│   ├── domain/                   # 도메인 (Port)
│   │   ├── model/
│   │   │   ├── AirQuality.kt
│   │   │   └── Weather.kt
│   │   └── repository/
│   │       ├── AirQualityRepository.kt  ← Port
│   │       └── WeatherRepository.kt     ← Port
│   │
│   ├── data/                     # 데이터 (Adapter)
│   │   ├── repository/
│   │   │   ├── AirQualityRepositoryImpl.kt  ← Adapter
│   │   │   └── WeatherRepositoryImpl.kt     ← Adapter
│   │   └── di/DataModule.kt
│   │
│   ├── network/                  # 네트워크
│   │   └── api/
│   │       ├── AirKoreaApi.kt
│   │       └── OpenMeteoApi.kt
│   │
│   └── resource/                 # 공통 리소스
│       ├── UiText.kt
│       └── res/values/strings.xml
│
└── .github/workflows/build.yml   # CI/CD
```

## 라이선스

MIT License

---

**개발:** seriouschoi
