# Play Console answers

Use these as paste-ready answers. Keep the Data safety answers consistent with the hosted privacy policy.

## Create app

- App name: `Noren VPN`
- Default language: English (United States)
- Additional language: Japanese (Japan); the app UI, first-connect disclosure, privacy dialog, and store listing are localized.
- App or game: App
- Free or paid: Free
- Category: Tools
- Contact email: `m980528@gmail.com`
- Privacy policy URL: `https://sclee980528-prog.github.io/noren-vpn-source/`
- Corresponding source URL: `https://github.com/sclee980528-prog/noren-vpn-source`
- VpnService review video URL: `https://github.com/sclee980528-prog/noren-vpn-source/releases/download/v1.0.0-v34/noren-vpn-v34-vpnservice-review.mp4`

The hosted policy matches `privacy_policy.html`. The 77.6-second review video
shows the versionCode 34 first-connect disclosure and live VPN connection.

## App access / sign-in details

Select: No login required.

Reviewer note:

`The app can be reviewed without an account. The free VPN connection flow is available immediately. Manual server selection is unlocked by the optional one-time Google Play product pro_server_selection. If purchase testing is unavailable in review, the free automatic connection flow still demonstrates the app's core VPN functionality.`

## Ads

Select: No, this app does not contain ads.

Reason: no AdMob, ad SDK, banner, interstitial, native ad, or ad-mediated traffic is present.

## In-app products

Create one product:

- Product type: One-time product / managed product
- Product ID: `pro_server_selection`
- Product name: `Pro Server Selection`
- Description: `Unlock manual server selection, server sorting, quality filtering, manual switching, and OVPN profile export.`
- Japanese product name: `Pro サーバー選択`
- Japanese description: `サーバーの手動選択、並べ替え、品質フィルター、手動切替、OVPNプロファイル書き出しを解除します。`
- Suggested launch price: JPY 480, or USD 2.99 equivalent.

Do not describe this as premium/private servers. It unlocks control features only.

## Target audience

Recommended:

- Target age: 18 and over
- Not designed for children
- Do not enroll in Families

Rationale: this is a VPN utility that routes device network traffic through independent public volunteer VPN servers.

## Content rating

Use the utility/tool answers. The app has no violence, gambling, user-generated content, social posting, explicit content, or account messaging.

## Data safety

Conservative recommendation: answer Yes, the app collects or shares user data types. This is more defensive for a VPN app because VPN traffic and public IP information are transmitted off-device, and Google states that collection includes data transmitted off device by the app or SDKs.

### Data collection and security

- Does the app collect or share required user data types? Yes
- Is all collected user data encrypted in transit? Yes, for app-controlled transport: HTTPS for public web requests and encrypted OpenVPN tunnel from device to VPN endpoint. Do not claim end-to-end encryption after the VPN endpoint.
- Do users have a way to request data deletion? No for VPN Gate connection/packet logs because the app developer does not control them. Local app data can be removed by clearing app data or uninstalling, and exported profiles can be deleted by the user. Do not claim that the privacy contact can delete third-party logs.

### Data types to declare

Declare these conservatively:

1. Web browsing > Web browsing history
   - Collected/shared: Collected and shared
   - Purpose: App functionality
   - Required or optional: Required when VPN is connected
   - Ephemeral: No. VPN Gate states that destination HTTP/HTTPS hostnames, IP addresses, hostnames, and ports are retained in central connection logs for at least three months.
   - Explanation: VPN traffic is routed through independent public volunteer endpoints. VPN Gate states that destination metadata is logged for anti-abuse purposes.

2. Location > Approximate location
   - Collected/shared: Collected and shared
   - Purpose: App functionality
   - Required or optional: Required for connection status/IP check after VPN connection
   - Explanation: Source IP addresses visible to VPN Gate and public IP services can imply approximate location. The app does not request Android location permission or precise GPS.

3. App info and performance > Diagnostics
   - Collected/shared: Collected and shared
   - Purpose: App functionality, security, and compliance
   - Required or optional: Required when VPN is connected
   - Ephemeral: No
   - Explanation: VPN Gate states that packet/byte totals and communication error details are retained in connection logs for at least three months.

4. Device or other IDs
   - Collected/shared: Collected and shared
   - Purpose: App functionality, fraud prevention, security, and compliance
   - Required or optional: Required when VPN is connected and where Google Play services apply
   - Explanation: VPN Gate states that source IP address, hostname, and available VPN client identifiers are logged. Google Play services may process identifiers for purchase handling. The app does not use advertising ID.

5. Financial info > Purchase history
   - Collected/shared: Collected
   - Purpose: App functionality
   - Required or optional: Optional; only needed if the user buys/restores Pro
   - Explanation: Google Play Billing provides purchase status so the app can unlock `pro_server_selection`. The app does not collect payment card details.

Do not declare contacts, photos/videos, audio, files/docs, calendar, SMS, call logs, health, precise location, or personal info unless later code changes add them.

## VpnService declaration

Select: Yes, providing a VPN is the core functionality of the app.

Pasteable explanation:

`Noren VPN uses Android VpnService as its core feature to create a user-initiated VPN tunnel on Android TV, Google TV, phones, and tablets. When the user presses Start VPN and accepts the Android VPN permission prompt, the app starts an encrypted OpenVPN tunnel to a selected public volunteer VPN server. The app does not use VpnService to redirect advertising traffic, manipulate traffic for monetization, or perform ad fraud. The app has no advertising SDK. The app shows an in-app disclosure before the first connection and includes a Privacy button in the main interface.`

If asked for foreground service / special use reason:

`The foreground service keeps the active VPN tunnel running and shows ongoing VPN status while connected. This is necessary for the user-visible VPN connection.`

## Prominent disclosure text in app

First-connect disclosure in versionCode 34:

`To provide the VPN, Noren VPN uses Android VpnService and sends this device's network traffic through a public VPN Gate volunteer server until you disconnect. VPN Gate states that its connection logs include connection times, your source IP address and hostname, the selected VPN server, protocol and client details, traffic totals and errors, and destination HTTP/HTTPS hostnames, IP addresses, and port numbers. Its central logs are kept for at least three months. Each volunteer server also keeps TCP/IP packet headers for at least two weeks. Logs may be disclosed when legally authorized. We do not operate those servers or receive their logs. We use the traffic only to provide the VPN; VPN Gate and the volunteer operator handle the logged data. Choose Agree and continue only if you consent.`

Current Privacy button summary:

The Privacy dialog repeats the VPN Gate data types and retention periods, explains local app data and public IP/Google Play processing, and links directly to VPN Gate's logging policy.

## Review video script

The final 77.6-second versionCode 34 TV review video shows:

1. Open Noren VPN from the Android TV launcher.
2. Press Start VPN and show the complete first-connect disclosure.
3. Choose Cancel and return to the disconnected home screen.
4. Press Start VPN again and show that the complete disclosure is presented again.
5. Choose Agree and continue.
6. Move focus from the Android VPN warning to OK and grant VpnService permission.
7. Show the live Connected status for a public Japan VPN server.

The submitted video must be 90 seconds or shorter.
