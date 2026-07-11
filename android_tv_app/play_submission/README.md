# Noren VPN Play submission packet

Updated: 2026-07-12

Use this folder when filling Play Console for the Android TV app.

## Release artifact

Upload this file to Internal testing:

`/Users/sangchan/Documents/New project/android_tv_app/app/build/outputs/bundle/tvRelease/app-tv-release.aab`

Expected app package:

`com.oneclickvpn.tv`

Google Play one-time product:

`pro_server_selection`

Expected release version:

`1.0.0` (`versionCode 34`)

## Required before final review

- Confirm the exact developer name and support contact shown on the Play listing match the privacy policy.

## Public submission dependencies

- Privacy policy: `https://sclee980528-prog.github.io/noren-vpn-source/`
- Corresponding source: `https://github.com/sclee980528-prog/noren-vpn-source`
- VpnService review video: `https://github.com/sclee980528-prog/noren-vpn-source/releases/download/v1.0.0-v34/noren-vpn-v34-vpnservice-review.mp4`
- Review video: 77.6 seconds, 1920x1080, showing app launch, full disclosure, non-consent and retrigger flow, consent, Android VPN permission, and a live connected VPN.

## File map

- `store_listing.md`: store listing copy and reviewer-facing descriptions.
- `privacy_policy.html`: HTML privacy policy ready to host.
- `play_console_answers.md`: App content, Data safety, VpnService, ads, target audience, and in-app product answers.
- `reviewer_notes.md`: short review notes to paste into Play Console sign-in/access instructions.
- `review/noren-vpn-v34-vpnservice-review.mp4`: verified VpnService review video.
- `store_assets/en-US/`: English 1024x500 feature graphic, 1280x720 TV banner, and unaltered 1920x1080 TV screenshot.
- `store_assets/ja-JP/`: Japanese 1024x500 feature graphic, 1280x720 TV banner, and unaltered 1920x1080 TV screenshot.

## Verified release state

- AAB SHA-256: `b71132dd22cc631f511cdf56cdd0a0d551fb39fb7e9e3597c3bbf6a55cb3a051`
- APK SHA-256: `9bfe4f52875d1cf585d0b1ace75d76dc3b8260ce22ddec54a16e974dfa99c4bd`
- Native symbols SHA-256: `2e2acc9707c46de0dfaf0b255902c56247d300fd866279c34b07bfc85a8803c9`
- VpnService review video SHA-256: `a432e57888bcba28133dbbb2e203e5bedc4e11c5a2a80133ca4bde814ba6bbe8`
- APK signature: verified with APK Signature Scheme v1 and v2.
- Packaged native engine: OpenSSL `4.0.1` for `arm64-v8a` and `armeabi-v7a`.
- Native packaging: APK zip alignment and every ARM64 ELF `LOAD` segment verified
  at `0x4000` (16 KB).
- Unit tests: 6 tests, 0 failures.
- Release Lint: 0 errors, 540 warnings; remaining warnings are primarily inherited from the vendored ics-openvpn skeleton.
- Languages: English and Japanese app-owned string and plural resources have matching key sets.
- Android TV 16 runtime: three clean cold starts without a native crash, followed
  by a validated Japan VPN connection, disconnect, reconnect, and sleep/wake pass.
- UI QA: English and Japanese home, disclosure, privacy, and Pro flows verified
  with D-pad navigation at 1920x1080 and representative 1280x720 TV density.

## Current app behavior summary

- Free users can start an automatically selected VPN connection.
- Pro unlock is a Google Play one-time purchase for manual server selection, sorting/filtering, manual switching, and OVPN export.
- The app uses Android `VpnService`, Google Play Billing, HTTPS server directory loading, and an optional/public IP check.
- The app does not include ads, AdMob, analytics, user accounts, email login, contacts, photos, microphone, camera, or location permission.
- VPN traffic is routed through independent public volunteer VPN servers, so the listing and privacy policy must not claim guaranteed anonymity, guaranteed security, streaming unlock, or official affiliation with any volunteer server project.
