# oneclick free vpn for Android

Android TV, Google TV, 스마트폰용 VPN Gate 클라이언트입니다. VPN Gate 공개 CSV API에서 현재 OpenVPN 설정이 열려 있는 서버를 가져오고, 앱 안에 포함된 OpenVPN 엔진으로 직접 연결합니다.

외부 OpenVPN 앱 설치나 `.ovpn` 파일 import는 기본 흐름에서 필요하지 않습니다.

## APK

TV 릴리스 빌드 산출물:

```text
android_tv_app/app/build/outputs/bundle/tvRelease/app-tv-release.aab
android_tv_app/app/build/outputs/apk/tv/release/app-tv-release.apk
```

## 설치

Android 기기에서 개발자 옵션과 USB 디버깅을 켠 뒤:

```bash
adb install -r android_tv_app/app/build/outputs/apk/tv/release/app-tv-release.apk
```

첫 연결 시 Android 시스템의 VPN 권한 승인 창이 뜹니다. 정상 VPN 앱은 이 승인 과정을 건너뛸 수 없습니다.

## 사용 흐름

1. `oneclick free vpn` 앱을 엽니다.
2. 현재 열린 국가 목록에서 국가를 선택합니다.
3. 속도순, Ping순, Score순으로 서버를 정렬합니다.
4. 서버를 선택하고 `연결`을 누릅니다.
5. Android VPN 권한을 승인합니다.
6. 앱 안의 OpenVPN 엔진이 연결을 시작합니다.
7. 끊을 때는 `VPN 끊기`를 누릅니다.

## 포함 기능

- VPN Gate CSV API 다운로드
- 현재 OpenVPN 설정이 있는 서버만 표시
- 현재 열린 국가 목록 자동 생성
- Rank, HostName, IP, Ping, Speed Mbps, Sessions, Uptime 표시
- 속도/Ping/Score 정렬
- 선택 서버를 내부 OpenVPN 엔진으로 직접 연결
- Android `VpnService` 기반 터널 생성
- 공인 IP/국가 확인
- `.ovpn` 프로필 저장 fallback
- IPv6 누수 완화 설정 추가
  - `block-ipv6`
  - `pull-filter ignore "ifconfig-ipv6"`
  - `pull-filter ignore "route-ipv6"`

## 다시 빌드

프로젝트는 Gradle 설정에서 Android SDK와 Java 21 경로를 고정합니다.

```bash
gradle :app:bundleTvRelease :app:lintTvRelease
```

필요한 로컬 도구:

- Android SDK platform 36
- Android NDK `30.0.14904198`
- Android CMake `3.22.1`
- OpenJDK 21

## 보안 주의

VPN Gate는 공용 자원봉사 서버입니다. VPN Gate는 중앙 연결 로그를 3개월 이상, 각 자원봉사 서버는 TCP/IP 패킷 헤더를 2주 이상 보관한다고 밝히고 있습니다. 금융, 개인정보, 회사 계정 로그인 같은 민감한 용도에는 적합하지 않을 수 있습니다. HTTPS가 아닌 사이트에서 비밀번호나 결제 정보를 입력하지 마세요.

## 라이선스 주의

내부 OpenVPN 엔진은 `ics-openvpn` 기반입니다. 해당 코드는 GPL 계열 라이선스 조건을 따르므로, 앱을 배포할 때 소스 공개와 라이선스 고지가 필요합니다.
