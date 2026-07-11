# oneclick free vpn Play submission packet

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

## Current app behavior summary

- Free users can start an automatically selected VPN connection.
- Pro unlock is a Google Play one-time purchase for manual server selection, sorting/filtering, manual switching, and OVPN export.
- The app uses Android `VpnService`, Google Play Billing, HTTPS server directory loading, and an optional/public IP check.
- The app does not include ads, AdMob, analytics, user accounts, email login, contacts, photos, microphone, camera, or location permission.
- VPN traffic is routed through independent public volunteer VPN servers, so the listing and privacy policy must not claim guaranteed anonymity, guaranteed security, streaming unlock, or official affiliation with any volunteer server project.
