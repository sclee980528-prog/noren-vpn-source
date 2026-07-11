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

- Host the updated `privacy_policy.html` at a stable public, non-PDF, non-geofenced, non-editable URL. Do not reuse the older v33 policy without replacing it.
- Publish the complete GPL corresponding source and build scripts, excluding signing keys and local secrets, and add the public source URL to the app/listing.
- Re-record the VpnService review video. The older v33 video does not show the expanded VPN Gate logging disclosure required by versionCode 34.
- Confirm the exact developer name and support contact shown on the Play listing match the privacy policy.

Google requires the privacy policy to be publicly accessible, non-geofenced, non-editable, and not a PDF.

## File map

- `store_listing.md`: store listing copy and reviewer-facing descriptions.
- `privacy_policy.html`: HTML privacy policy ready to host.
- `play_console_answers.md`: App content, Data safety, VpnService, ads, target audience, and in-app product answers.
- `reviewer_notes.md`: short review notes to paste into Play Console sign-in/access instructions.
- `store_assets/en-US/`: English 1024x500 feature graphic and 1280x720 TV banner.
- `store_assets/ja-JP/`: Japanese 1024x500 feature graphic and 1280x720 TV banner.

## Verified release state

- AAB SHA-256: `72a1cfdf1e8ac2ae7cee951efd16636e62c195fcde4965d1af12183dbafea3fc`
- APK SHA-256: `294bea32490c0c54aaa6ef4b4e3c78577a04811c19191b752fcc293b92c323db`
- Native symbols SHA-256: `2e2acc9707c46de0dfaf0b255902c56247d300fd866279c34b07bfc85a8803c9`
- APK signature: verified with APK Signature Scheme v1 and v2.
- Packaged native engine: OpenSSL `4.0.1` for `arm64-v8a` and `armeabi-v7a`.
- Native packaging: APK zip alignment and every ARM64 ELF `LOAD` segment verified
  at `0x4000` (16 KB).
- Unit tests: 6 tests, 0 failures.
- Release Lint: 0 errors, 544 warnings; remaining warnings are primarily inherited from the vendored ics-openvpn skeleton.
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
