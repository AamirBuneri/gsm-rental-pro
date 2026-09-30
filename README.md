# AB Gsm Rental — Android app

Native Android app (Kotlin + Jetpack Compose, Material 3) for resellers of a **GSM Rental Pro** site
(v3.3.0 or newer). Light and dark themes, your AB branding, splash screens and icon.

## What resellers can do
- Sign in with their normal username + password (two-step codes and recovery codes supported)
- Home: wallet balance, credit, this month's stats, running rentals with live countdown rings, open orders
- Rent a tool: plans and prices, pay from the wallet, get the login instantly (copy / reveal)
- Rentals: running with timers, history, full details and login while running
- Remote services: catalogue by category, order form with the site's own fields, AnyDesk / UltraViewer,
  accept a price quote, cancel, live status timeline and the result
- Wallet history, notifications (mark read), top-up / help on WhatsApp
- Alerts on the phone: new notifications (checked every ~15 min) and "ends in 10 min" rental reminders
- Theme: System / Light / Dark

## How it connects
The app talks to `https://your-site/index.php?r=/api/v1/…` (works with clean URLs on or off).
Sign-in returns a per-phone token, stored encrypted with the Android Keystore. The admin can switch the
app off or sign every phone out in **Settings → Security → Android app**. Changing a password, resetting
two-step sign-in, or disabling / deleting a reseller signs their phones out.

The default site address is set in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "DEFAULT_SITE", "\"https://aamirbuneri.com\"")
```

## Build
- **GitHub**: every push builds `AB-Gsm-Rental.apk` (Actions → latest run → Artifacts).
  Push a tag like `v1.0.0` to attach the APK to a GitHub Release.
- **Android Studio**: open the folder, then *Build → Build APK(s)*.
- Command line: `./gradlew assembleRelease` (JDK 17).

Without your own key the release APK is signed with the debug key — fine for sharing directly.
For Play Store / stable updates, create a keystore once and either put a `keystore.properties` next to
`settings.gradle.kts`:
```
storeFile=/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```
or add GitHub secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`,
`SIGNING_KEY_PASSWORD`. Keep the keystore safe — updates must be signed with the same key.

`ic_launcher-playstore.png` (512×512) is the Play Store icon.

By Aamir-Buneri.
