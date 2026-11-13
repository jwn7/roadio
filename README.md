# Roadio

목적지까지 가는 길을 테마 경로로 골라, 명소를 창밖으로 보며 지나가게 해 주는 Pleos Connect 차량용 투어 가이드 앱.

내비게이션에 설정된 목적지를 가져와 "빠른 길"과 "테마 경로"를 나란히 보여 준다. 테마 경로를 고르면 그 길 위의 지점을 내비게이션 경유지로 등록해 실제 안내 경로를 바꾸고, 주행 중 지점에 가까워지면 "오른쪽 창밖, 한계령"처럼 짧게 알려 준다. 마음에 드는 곳은 버튼 하나로 들를 수 있다.

## 화면 흐름

| 단계 | 화면 | Activity | 사용 조건 |
|---|---|---|---|
| 1 | 목적지 확인 + 테마 선택 | `PlanActivity` | 정차 중 |
| 2 | 경로 찾는 중 | `PlanActivity` | 정차 중 |
| 3 | 경로 결과 (안내 시작) | `PlanActivity` | 정차 중 |
| 5 | 주행 화면 (다음 지점까지 거리) | `DriveActivity` | 주행 중 |
| 6 | 안내 중 (좌우 방향 + 음성) | `DriveActivity` | 주행 중 |
| 7 | 들러보기 확인 | `DriveActivity` | 주행 중 |
| 8 | 모든 곳을 지남 (지나온 곳 요약, 내비게이션으로 돌아가기) | `DriveActivity` | 주행 중 |

## 실행 방법

### 1. 빌드

JDK 17과 Android SDK(API 34)가 필요하다. Gradle은 저장소에 포함된 wrapper(8.7)를 쓴다.

```powershell
.\gradlew.bat assembleDebug
```

macOS / Linux:

```bash
./gradlew assembleDebug
```

Android Studio(Koala 이상)에서는 폴더를 열고 Gradle sync 후 실행하면 된다.

**프로젝트 경로에 한글이 있으면 Windows에서 빌드가 거부된다.** 영문 경로에 두거나 임시 드라이브로 연결해서 빌드한다.

```powershell
subst R: "C:\한글이 든\경로\roadio-app"
R:
.\gradlew.bat assembleDebug
subst R: /d
```

### 2. 에뮬레이터에 설치

Pleos Connect 에뮬레이터를 Cold boot로 띄운 뒤 설치한다. 첫 부팅에서 화면이 하얗거나 검게 나오면 에뮬레이터를 한 번 재부팅한다(차량 시스템 서비스가 늦게 뜨는 경우가 있다).

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. 사용 순서

1. 앱 목록에서 Roadio를 실행한다. 내비게이션에 목적지가 잡혀 있으면 그 목적지가, 없으면 예시 목적지(양양 낙산해변)가 첫 화면에 표시된다. 처음 실행하면 알림 허용을 묻는데 **허용**을 누른다(내비게이션 위에 뜨는 안내 카드가 알림이다).
2. 테마를 고르고 "경로 찾기"를 누른다. 빠른 길과 테마 경로가 소요 시간, 지나가는 지점과 함께 표시된다.
3. 경로를 고르고 "안내 시작"을 누른다. 앱이 목적지와 경유지를 내비게이션에 넘기고, 화면은 내비게이션으로 바뀐다. 그 위에 "산 경로 안내를 시작합니다" 카드가 뜬다.
4. 지점에 가까워지면 내비게이션 위로 "오른쪽 창밖 · 한계령" 카드가 뜨고 음성 안내가 나온다. 카드의 "들르기"를 누르면 해당 장소가 경유지로 추가된다.
5. 카드를 누르거나 하단 바에서 Roadio를 열면 주행 화면(다음 지점, 거리, 지점 목록)이 보인다. 이 상태에서 화면 오른쪽 끝의 창 막대(⋮)를 왼쪽으로 끌면 Roadio와 내비게이션이 한 칸씩 나란히 놓인다.

가짜 주행(기본 설정)은 지점 하나에 약 24초, 산 경로 전체에 약 2분 30초가 걸린다.

### 4. 단위 테스트

```powershell
.\gradlew.bat test
```

주행 상태 판정(`DriveEngine`)을 검증한다.

## 테스트 환경 및 필요한 설정

| 항목 | 값 |
|---|---|
| 언어 / UI | Kotlin 2.0.20 / Jetpack Compose (BOM 2024.09.00) |
| 빌드 | Android Gradle Plugin 8.5.2, Gradle 8.7, JDK 17 |
| SDK 레벨 | minSdk 28, targetSdk 34, compileSdk 34 |
| Pleos SDK | `ai.pleos.playground:NaviHelper:3.0.5` |
| SDK 저장소 | `https://nexus-playground.pleos.ai/repository/maven-releases/` (`settings.gradle.kts`에 등록됨) |

### 권한

`AndroidManifest.xml`에 선언되어 있으며 별도 설정은 필요 없다.

| 권한 | 용도 |
|---|---|
| `pleos.car.permission.NAVI_ROUTE` | 목적지 등 내비게이션 정보 조회 |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | 경로 요청, 경유지 추가 |
| `android.permission.ACCESS_FINE_LOCATION` | 현재 위치 (Android Automotive 표준 위치 API) |
| `android.permission.FOREGROUND_SERVICE` 외 2개 | 내비게이션에 가려진 동안에도 안내를 이어 가기 위한 서비스 |
| `android.permission.POST_NOTIFICATIONS` | 내비게이션 위에 뜨는 안내 카드(헤드업 알림). 첫 실행 때 허용을 묻는다 |

### 실행 전 확인

- 내비게이션에 목적지를 미리 잡지 않아도 된다. "안내 시작"에서 앱이 목적지와 경유지를 함께 요청한다 (`requestRouteWithWaypoints`).
- 내비게이션 경유지는 최대 2개다. 앱은 경로를 바꾸는 데 필요한 지점 1곳만 등록하고, 한 칸은 "들르기"용으로 남긴다.
- "안내 시작" 뒤에는 내비게이션이 화면 오른쪽 영역을 쓴다. 창밖 안내(위치 판정, 음성, 내비게이션 위 카드)는 포그라운드 서비스 `GuideService`가 맡아 이 앱의 화면이 없어도 이어진다.
- 안내 카드는 헤드업 알림이다. 알림을 허용하지 않으면 카드 대신 주행 화면이 앞에 뜬다.
- 위치 값은 에뮬레이터의 Extended controls > Location에서 넣을 수 있다.

### 개발용 스위치

`app/src/main/java/app/roadio/Session.kt`

| 스위치 | 기본값 | 설명 |
|---|---|---|
| `USE_FAKE_NAVI` | `false` | `true`면 NaviHelper 없이 화면 흐름만 확인한다 (일반 Android 에뮬레이터용) |
| `SIMULATE_DRIVE` | `true` | `true`면 실제 GPS 대신 가짜 주행으로 주행 화면을 넘긴다 |

## Pleos Connect 에뮬레이터 버전과 화면 규격

| 항목 | 값 |
|---|---|
| 시스템 이미지 | Pleos Connect v2.1.0 (`pleos_car_emulator`, x86_64, revision 1), Android 14 (API 34) |
| 이미지 배포처 | `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml` |
| 기기 프로필 | Pleos Connect v2.1.0 (이미지에 포함된 프로필) |
| 화면 해상도 / 밀도 | 2560 × 1440, mdpi (160dpi), 12.8인치 |
| AVD 설정 | RAM 4GB, Cold boot |

화면 배치는 Pleos Connect의 창 규칙을 따른다.

| Activity | 방향 선언 | 배치 |
|---|---|---|
| `PlanActivity` | `landscape` | 가로, 창 두 칸 |
| `DriveActivity` | 미지정 | 직전 화면의 배치를 따른다(보통 두 칸). 창 막대를 끌면 내비게이션 옆 한 칸. 폭에 따라 두 가지로 그린다. `distractionOptimized` 선언 |

주행 화면은 글자 28 이상, 문장 3줄 이하, 터치 영역 76dp 이상으로 만들고 애니메이션·자동 스크롤·키보드 입력을 쓰지 않는다.

## APK 파일 위치

`apk/LLQ_Roadio_release.apk` — 서명된 release 빌드 (`app.roadio`, 버전 0.1.0).

```bash
adb install -r apk/LLQ_Roadio_release.apk
```

debug 빌드가 이미 설치되어 있으면 서명이 달라 덮어 설치되지 않는다. 먼저 `adb uninstall app.roadio`로 지운다.

직접 release를 빌드하려면 루트에 `keystore.properties`(`storeFile`, `storePassword`, `keyAlias`, `keyPassword`)를 두고 `.\gradlew.bat assembleRelease`를 실행한다. 서명 키와 이 파일은 저장소에 포함하지 않는다.

## 구조

| 파일 | 역할 |
|---|---|
| `data/Models.kt`, `RouteRepository.kt` | 경로 JSON 읽기 |
| `drive/DriveEngine.kt` | 위치 → 접근 중 / 안내 중 / 끝 판정. 순수 Kotlin |
| `GuideService.kt` | 창밖 안내를 화면과 따로 돌리는 포그라운드 서비스 (위치 → 판정 → 음성 + 내비게이션 위 카드) |
| `geo/Geo.kt` | 거리, 방위각, 좌우 판정 |
| `navi/NaviGateway.kt` | 내비게이션 접점 인터페이스와 `FakeNavi` |
| `navi/PleosNavi.kt` | NaviHelper SDK 호출 (SDK 호출은 전부 이 파일) |
| `navi/LocationSource.kt` | GPS 또는 가짜 주행 |
| `audio/Narrator.kt` | 안내 음성. 녹음 파일이 있으면 재생, 없으면 TTS |
| `ui/*` | Compose 화면 |

경로는 모두 `app/src/main/java/app/roadio/` 아래다. `.tours/`에는 코드를 순서대로 읽을 수 있는 CodeTour 투어가 있다.

## 데이터

`app/src/main/assets/routes_seoul_yangyang.json` — 경로와 지나가는 지점.

지점 좌표는 손으로 넣지 않고 조망 판정 스크립트의 산출물을 쓴다. 현재 파일에서 `verified: false`인 경로의 좌표와 소요 시간은 검증 전 임시값이다.

안내 음성은 `app/src/main/assets/audio/`에 두며, 파일 이름은 JSON의 `audioFile` 값과 같아야 한다. 음성으로 읽는 긴 설명은 `app/src/main/assets/narrations.json`에 있고, 글을 고친 뒤에는 `python tools/make_audio.py`(edge-tts 필요)로 음성을 다시 만든다.

## 현재 범위

- 서울 → 양양 구간의 예시 경로로 동작한다.
- 임의 목적지에 대한 실시간 경로 구성, Vehicle SDK 연동(와이퍼·라이트·좌석 상태)은 다음 단계에서 구현한다.
