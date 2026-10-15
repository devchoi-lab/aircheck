# 오늘공기 (AirCheck) 🌤️

현재 위치 기반 **대기질 + 날씨** 정보를 한눈에 보여주는 Android 앱

> **6,800+ Lines** | **76 Files** | **10 Modules**

---

## 🎯 개발 원칙

> **"좋은 앱 먼저, 수익은 나중에"**

1. **사용성 우선** — 수익성 고민 전에 쓰기 좋은 앱부터 만든다
2. **기능 제한 없음** — 위젯, 알림 등 필요한 기능은 전부 만든다
3. **광고는 최소화** — 어르신도 헷갈리지 않는 명확한 광고 배치

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 🌡️ **현재 날씨** | 기온, 체감온도, 날씨 상태 |
| 💨 **대기질** | PM2.5, PM10 (WHO 기준 등급) |
| 📊 **48시간 예보** | 시간별 기온, 강수확률, 요약 정보 |
| 👕 **추천 아이템** | 체감온도 기반 옷차림, 우산 추천 |
| 📍 **다중 위치** | 현재 위치 + 저장된 위치 관리 |
| 🔄 **위치 순서 관리** | 드래그로 위치 순서 변경 |
| 🔄 **Pull-to-Refresh** | 개별/전체 새로고침 |
| 🏠 **홈 위젯** | 대기질 + 추천 의상 |
| 📢 **광고** | 배너 + 네이티브 (어르신 친화적) |

---

## 타겟 사용자

> **"자녀가 부모님께 깔아드리는 앱"**

- 👴👵 어르신도 쉽게 사용 가능한 UI
- 📱 큰 글씨, 명확한 구분
- 🚫 전면 광고 없음 (광고 ≠ 콘텐츠 명확 구분)

---

## 아키텍처

### 멀티모듈 + Clean Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        :app                              │
│              (DI 설정, Navigation, Entry Point)          │
└─────────────────────────┬───────────────────────────────┘
                          │
    ┌─────────────────────┴─────────────────────┐
    ▼                                           ▼
┌──────────────────┐                 ┌──────────────────┐
│ :feature:weather │                 │ :feature:widget  │
│  (메인 화면 UI)   │                 │   (홈 위젯)      │
└────────┬─────────┘                 └────────┬─────────┘
         │                                    │
         └────────────────┬───────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────┐
│                    :core:domain                          │
│        (UseCase, Repository Interface, Model)            │
└─────────────────────────┬───────────────────────────────┘
                          │
    ┌─────────────────────┴─────────────────────┐
    ▼                                           ▼
┌──────────────────┐                 ┌──────────────────┐
│   :core:data     │                 │  :core:network   │
│ (Repository Impl)│────────────────▶│    (API 호출)    │
└──────────────────┘                 └──────────────────┘

┌─────────────────────────────────────────────────────────┐
│  :core:ui  |  :core:resource  |  :core:config           │
│      (공통 컴포넌트, 문자열 리소스, URL 설정)             │
└─────────────────────────────────────────────────────────┘
```

### 모듈 구성

| 모듈 | 역할 |
|------|------|
| `:app` | 앱 진입점, Hilt DI, AdMob 초기화 |
| `:feature:weather` | 메인 날씨 화면, ViewModel (MVI) |
| `:feature:widget` | 홈 화면 위젯 |
| `:core:domain` | UseCase, Repository 인터페이스, 도메인 모델 |
| `:core:data` | Repository 구현체 |
| `:core:network` | Retrofit API 클라이언트 |
| `:core:ui` | 공통 Compose 컴포넌트, 테마, 광고 |
| `:core:resource` | strings.xml, UiText |
| `:core:config` | 서버 URL 설정 |
| `:core:firebase` | Firebase App Check |

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| **Language** | Kotlin 100% |
| **UI** | Jetpack Compose, Material3 |
| **Architecture** | MVI (ViewModel + StateFlow) |
| **DI** | Hilt |
| **Async** | Coroutines, Flow |
| **Network** | Retrofit, Kotlinx Serialization |
| **Storage** | DataStore Preferences |
| **Ads** | Google AdMob (배너 + 네이티브) |
| **Security** | Firebase App Check |
| **CI/CD** | GitHub Actions |
| **Backend** | [aircheck-server](https://github.com/devchoi-lab/aircheck-server) |

---

## 서버 연동

자체 백엔드 서버 사용:

```
앱 ──▶ aircheck-server ──▶ 에어코리아 API (대기질)
                       ──▶ 기상청 API (날씨)
```

- **캐싱**: 서버에서 1시간 캐시 → API 할당량 절약
- **병렬 호출**: 다중 위치 배치 조회
- **App Check**: 앱 위변조 방지

---

## 대기질 등급 (WHO 기준)

| 등급 | PM2.5 (㎍/㎥) | PM10 (㎍/㎥) |
|------|--------------|--------------|
| 🌟 AQG (목표) | 0-15 | 0-45 |
| 😊 IT-4 | 15-25 | 45-50 |
| 🙂 IT-3 | 25-37.5 | 50-75 |
| 😐 IT-2 | 37.5-50 | 75-100 |
| 😮 IT-1 | 50-75 | 100-150 |
| 😷 초과 | 75+ | 150+ |

---

## 빌드

### 요구사항
- JDK 17+
- Android SDK 35

### 로컬 빌드
```bash
./gradlew :app:assembleDebug
```

### CI/CD
- **Push to develop** → 빌드 + 테스트
- **Push to main** → 빌드 + 테스트 + Artifacts

---

## 개발 기간

- **시작**: 2026-03-16
- **11일 만에** 출시 준비 완료
- AI 협업 개발 (Claude)

---

## 라이선스

MIT License

---

**개발:** seriouschoi
