# AB Gsm Rental — Android app

Native Android app (Kotlin + Jetpack Compose, Material 3) for a **GSM Rental Pro** site — for resellers,
staff and the owner. Light and dark themes, your AB branding, splash screens and icon.
Needs site **v3.4.0** or newer for sign-up and the admin screens (3.3.0 works for reseller sign-in only).

## New resellers
- **Create an account in the app**: name, username, email, WhatsApp number, shop, billing currency, password
- Same rules as the website: if email confirmation or admin approval is on, the app shows a
  "Confirm your email" (send the link again) or "Waiting for approval" screen and signs them in by itself
  once it's done — no typing the password again
- **Forgot password** in the app (emails the reset link)

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

## What the owner and staff can do
The same sign-in screen; admin accounts get the admin screens. Staff only see the areas ticked for them
in **Admin → Staff** on the website (the app follows changes on its next start / refresh).
- Dashboard: sales today vs yesterday, 7-day chart, month revenue and profit (Money permission),
  running rentals, free slots, open orders, "needs attention" (registrations, orders to price or start,
  expiring tool accounts)
- Rentals: running / ended / closed, search, live timers, the renter's login, extend (+30 min … +7 days), close
- Service orders: queue, start, send a price, complete with a result, cancel / fail with refund,
  see the remote login (logged), WhatsApp the reseller
- Resellers: search and filters (waiting, in debt, disabled), approve / reject sign-ups, enable / disable,
  wallet add / deduct (Money), mark email confirmed, recent rentals and wallet history
- Tools & slots: usage per tool, show a slot's login, save a new password (one-tap strong password),
  enable / disable a slot, expiring accounts
- Admin notifications on the phone (new sign-ups, orders, alerts)

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
