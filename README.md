# Dynamic - 안드로이드 오버레이 앱

## 📱 프로젝트 개요

**Dynamic**은 Android에서 시스템 오버레이 기능을 구현한 앱입니다. 앱이 백그라운드에 있을 때 화면 위에 "따릉이 대여중" 메시지를 표시하는 오버레이를 제공합니다.

### 🎯 주요 기능

- **시스템 오버레이**: 다른 앱 위에 떠있는 UI 표시
- **앱 상태 감지**: 포그라운드/백그라운드 전환 자동 감지
- **드래그 앤 드롭**: 오버레이를 자유롭게 이동 가능
- **반응형 디자인**: 다양한 화면 크기에 대응
- **권한 관리**: SYSTEM_ALERT_WINDOW 권한 자동 요청
- **상태 지속성**: 앱 재시작 시 설정 복원

## 🏗️ 프로젝트 구조

```
dynamic/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/dynamic/
│   │   │   │   ├── DynamicApplication.kt          # 앱 생명주기 관리
│   │   │   │   ├── MainActivity.kt                # 메인 UI 액티비티
│   │   │   │   ├── service/                       # 서비스 레이어
│   │   │   │   │   ├── OverlayService.kt         # 오버레이 백그라운드 서비스
│   │   │   │   │   ├── OverlayManager.kt         # 오버레이 뷰 관리
│   │   │   │   │   └── DragHandler.kt            # 드래그 처리
│   │   │   │   └── viewmodel/                     # MVVM 패턴
│   │   │   │       └── OverlayViewModel.kt       # UI 상태 관리
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml         # 메인 화면 레이아웃
│   │   │   │   │   └── overlay_layout.xml        # 오버레이 레이아웃
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml               # 문자열 리소스
│   │   │   │   │   ├── colors.xml                # 색상 정의
│   │   │   │   │   └── themes.xml                # 앱 테마
│   │   │   │   └── mipmap-*/                     # 앱 아이콘들
│   │   │   └── AndroidManifest.xml               # 앱 권한 및 컴포넌트 정의
│   │   ├── androidTest/                          # UI 테스트
│   │   └── test/                                 # 단위 테스트
│   ├── build.gradle.kts                          # 앱 모듈 빌드 설정
│   └── proguard-rules.pro                        # 코드 난독화 규칙
├── gradle/                                       # Gradle 래퍼
├── build.gradle.kts                              # 프로젝트 레벨 빌드 설정
├── settings.gradle.kts                           # 프로젝트 설정
├── gradle.properties                             # Gradle 속성
└── README.md                                     # 프로젝트 문서
```

## 🔧 기술 스택

### 개발 환경
- **언어**: Kotlin
- **IDE**: Android Studio
- **빌드 시스템**: Gradle (Kotlin DSL)
- **최소 SDK**: API 24 (Android 7.0)
- **타겟 SDK**: API 35 (Android 15)
- **컴파일 SDK**: API 35

### 주요 라이브러리
- AndroidX Core KTX: 1.16.0
- AppCompat: 1.7.1
- Material Design: 1.12.0
- Activity: 1.10.1
- ConstraintLayout: 2.2.1
- JUnit: 4.13.2 (테스트)
- Espresso: 3.6.1 (UI 테스트)

## 📋 상세 컴포넌트 분석

### 1. DynamicApplication.kt
앱의 메인 Application 클래스로 전체 생명주기를 관리합니다.

**주요 기능:**
- Activity 생명주기 콜백 등록
- 포그라운드/백그라운드 상태 추적
- OverlayService 시작/중지 관리
- 앱 상태 변화를 서비스에 알림

**핵심 메서드:**
- `setupActivityLifecycleCallbacks()`: Activity 생명주기 감지
- `startOverlayService()`: 오버레이 서비스 시작
- `stopOverlayService()`: 오버레이 서비스 중지
- `notifyAppState()`: 서비스에 상태 알림

### 2. MainActivity.kt
사용자 인터페이스를 담당하는 메인 액티비티입니다.

**주요 기능:**
- 권한 상태 확인 및 요청
- 오버레이 on/off 토글 기능
- ViewModel과 UI 바인딩
- 사용자 피드백 제공

**UI 구성요소:**
- `statusText`: 현재 오버레이 상태 표시
- `permissionStatusText`: 권한 허용 여부 표시
- `toggleButton`: 오버레이 켜기/끄기 버튼

### 3. OverlayService.kt
백그라운드에서 실행되는 오버레이 관리 서비스입니다.

**주요 기능:**
- 오버레이 뷰 생성 및 관리
- 앱 상태 변화에 따른 오버레이 표시/숨김
- SharedPreferences를 통한 상태 저장
- 서비스 바인딩을 통한 ViewModel 연결

**핵심 로직:**
- `updateOverlayVisibility()`: 앱 상태와 플래그에 따라 오버레이 표시/숨김 결정

### 4. OverlayManager.kt
오버레이 뷰의 생성과 레이아웃 관리를 담당합니다.

**주요 기능:**
- XML 레이아웃 inflate
- 반응형 크기 계산 및 적용
- WindowManager 파라미터 설정
- 둥근 모서리 배경 생성

**반응형 디자인:**
- `ResponsiveSizes` 데이터 클래스로 화면 크기에 따른 동적 크기 계산

### 5. DragHandler.kt
오버레이의 드래그 앤 드롭 기능을 구현합니다.

**주요 기능:**
- 터치 이벤트 처리
- 드래그 임계값 적용
- 화면 경계 제한
- 터치 피드백 효과
- 클릭 시 메인 앱으로 이동

**터치 처리 로직:**
- `handleTouchEvent()`: 터치 이벤트를 분석하여 드래그/클릭 구분 처리

### 6. OverlayViewModel.kt
MVVM 패턴의 ViewModel로 UI 상태를 관리합니다.

**주요 기능:**
- LiveData를 통한 상태 관리
- SharedPreferences 연동
- 서비스와의 바인딩 관리
- 플래그 토글 로직

## 🎨 UI/UX 디자인

### 메인 화면 (activity_main.xml)
- 앱 제목, 설명 텍스트
- 현재 오버레이 상태 표시
- 권한 허용 여부 표시  
- 오버레이 켜기/끄기 토글 버튼

### 오버레이 (overlay_layout.xml)
- 상단/하단 여백으로 패딩 구현
- 로고 이미지 영역 (녹색 배경)
- "따릉이 대여중" 메인 텍스트
- "반납을 꼭 확인해주세요" 서브 텍스트

## ⚙️ 설정 및 권한

### Android 권한
- `SYSTEM_ALERT_WINDOW`: 다른 앱 위에 오버레이 표시 권한

### Gradle 설정
- 네임스페이스: `com.example.dynamic`
- 컴파일 SDK: 35 (Android 15)
- 최소 SDK: 24 (Android 7.0)
- 타겟 SDK: 35 (Android 15)
- Java 호환성: JDK 11

## 🔄 앱 플로우

### 1. 앱 시작
1. `DynamicApplication.onCreate()` 호출
2. Activity 생명주기 콜백 등록
3. `MainActivity` 실행
4. 권한 상태 확인

### 2. 권한 요청
1. `Settings.canDrawOverlays()` 확인
2. 권한이 없으면 설정 화면으로 이동
3. 권한 허용 후 UI 상태 업데이트

### 3. 오버레이 활성화
1. 토글 버튼 클릭
2. `OverlayViewModel.toggleOverlayFlag()` 호출
3. `OverlayService` 시작
4. `OverlayManager`로 뷰 생성
5. WindowManager에 오버레이 추가

### 4. 앱 상태 변화
1. 앱이 백그라운드로 이동
2. `DynamicApplication`에서 감지
3. `OverlayService`에 알림
4. 오버레이 표시

### 5. 드래그 처리
1. 오버레이 터치
2. `DragHandler.handleTouchEvent()` 호출
3. 드래그 임계값 확인
4. 위치 업데이트 또는 클릭 처리

## 🚀 빌드 및 실행

### 환경 요구사항
- Android Studio Arctic Fox 이상
- JDK 11 이상
- Android SDK API 24 이상

### 빌드 명령어
- `./gradlew assembleDebug`: 디버그 APK 빌드
- `./gradlew installDebug`: 디바이스에 설치
- `./gradlew testDebug`: 단위 테스트 실행

### 실행 방법
1. Android Studio에서 프로젝트 열기
2. 실제 Android 디바이스 연결 (에뮬레이터는 오버레이 권한 제한)
3. 앱 실행
4. 오버레이 권한 허용
5. 토글 버튼으로 오버레이 활성화
6. 홈 버튼으로 백그라운드 이동하여 오버레이 확인

## 🧪 테스트

### 단위 테스트
- `app/src/test/`: JUnit 기반 단위 테스트
- ViewModel 로직 테스트
- 유틸리티 함수 테스트

### UI 테스트
- `app/src/androidTest/`: Espresso 기반 UI 테스트
- 권한 요청 플로우 테스트
- 오버레이 표시/숨김 테스트

## 📱 지원 기기

### Android 버전
- **최소**: Android 7.0 (API 24)
- **타겟**: Android 15 (API 35)
- **테스트**: Android 8.0 ~ 14

### 화면 크기
- 반응형 디자인으로 모든 화면 크기 지원
- 기준 해상도: 412dp × 892dp
- 자동 스케일링 적용

## 🔧 개발자 정보

### 코드 스타일
- Kotlin 공식 코딩 컨벤션 준수
- 명시적 타입 선언
- 의미있는 변수명 사용
- 충분한 주석 제공

### 아키텍처 패턴
- **MVVM**: Model-View-ViewModel 패턴
- **Repository**: 데이터 레이어 분리
- **Service**: 백그라운드 작업 처리
- **Observer**: LiveData를 통한 반응형 UI

### 디버깅
- 각 클래스마다 고유한 TAG 상수 사용
- 이모지를 활용한 직관적인 로그 메시지
- 상세한 에러 정보와 상태 추적

## 📚 추가 정보

### 유용한 명령어
- `adb logcat | grep -E "(DynamicApplication|OverlayService|OverlayManager)"`: 앱 로그 확인
- `adb shell dumpsys window | grep "SYSTEM_ALERT_WINDOW"`: 권한 상태 확인
- `adb shell pm clear com.example.dynamic`: 앱 데이터 초기화

### 참고 자료
- [Android Overlay Documentation](https://developer.android.com/guide/topics/ui/window-management)
- [SYSTEM_ALERT_WINDOW Permission](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)
- [Service 가이드](https://developer.android.com/guide/components/services)

---

> **주의사항**: 이 앱은 시스템 권한이 필요하므로 실제 디바이스에서만 정상 동작합니다. 에뮬레이터에서는 권한 제한으로 인해 일부 기능이 동작하지 않을 수 있습니다.
