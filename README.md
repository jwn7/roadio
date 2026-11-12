# Roadio

가는 길을 테마 경로로 골라, 명소를 창밖으로 보며 지나가게 해 주는 Pleos Connect 차량용 앱.

## 빌드

```powershell
# Android Studio (Koala 이상)로 폴더를 열고 Gradle sync
# 명령줄:
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

- minSdk 28 / compileSdk 34 / Kotlin 2.0 / Jetpack Compose
- Pleos Connect Emulator(API 34)에서 실행

## 데이터

`app/src/main/assets/routes_seoul_yangyang.json` — 경로와 지나가는 지점. 조망 판정 스크립트의 산출물로 교체한다.
