# Noren VPN - corresponding source

This repository contains the source and build scripts corresponding to
`Noren VPN` for Android, package `com.oneclickvpn.tv`, version `1.0.0`
(`versionCode 34`).

The app uses Android `VpnService`, the public VPN Gate server directory, and a
vendored ics-openvpn/OpenVPN engine. It is an independent client and is not an
official VPN Gate application.

## Layout

- `android_tv_app/`: Android application and Play submission text.
- `third_party/ics-openvpn/`: vendored ics-openvpn, OpenVPN, OpenSSL, and other
  native dependencies needed by the build.
- `third_party/ics-openvpn/UPSTREAM_PROVENANCE.md`: recorded upstream revisions
  and the OpenSSL 4.0.1 security update procedure.

## Build requirements

- JDK 21
- Android SDK platform 36
- Android NDK `30.0.14904198`
- Android CMake 3.22.1
- SWIG 3.0 or newer

Set `ANDROID_HOME` and `JAVA_HOME` for your machine. If needed, create
`android_tv_app/local.properties` with a local Android SDK path. Do not commit
that file.

Build the TV release bundle, run the security-focused unit tests, and run Lint:

```sh
cd android_tv_app
./gradlew --no-daemon :app:testTvDebugUnitTest :app:bundleTvRelease :app:lintTvRelease
```

The public source tree intentionally does not contain the private upload key or
`release-keystore.properties`. Without a signing configuration, locally built
release artifacts are unsigned. A debug APK can be built with:

```sh
./gradlew --no-daemon :app:assembleTvDebug
```

## Privacy notice

VPN Gate states that it retains central VPN connection logs for at least three
months and that volunteer servers retain TCP/IP packet headers for at least two
weeks. Read its current logging policy before using or redistributing the app:
https://www.vpngate.net/en/about_abuse.aspx

Noren VPN's English and Japanese privacy policy is published at:
https://sclee980528-prog.github.io/noren-vpn-source/

Versioned source archives and the Google Play VpnService review video are
published with the `v1.0.0-v34` GitHub release.

## License

The combined app is distributed under GNU GPL version 2 or later, subject to
the upstream clarification and linking exceptions in
`third_party/ics-openvpn/doc/LICENSE.txt`. See `COPYING` and
`SOURCE_LICENSE.md`. Individual third-party directories retain their own
license and notice files.
