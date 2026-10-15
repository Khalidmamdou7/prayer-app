# Azan

A small, ad-free Android prayer-times app.

- **No ads, no tracking.** The app has no ads or analytics SDKs. It only goes online to download prayer times.
- **Accurate, and it works offline.** When internet is available, the app downloads this month's and next month's times from [aladhan.com](https://aladhan.com/prayer-times-api) for your location and method. They're cached on the phone, so it keeps working offline for weeks. If it has never synced, or the cache runs out, the times are calculated on the device, and the main screen shows which source is in use. Downloaded times are checked against the on-device calculation, and any day that disagrees by more than an hour is thrown away.
- **Per-prayer ± minute adjustments,** to match your local mosque exactly.
- **Choose what happens at each prayer:** *Off*, *Notify* (a notification with sound), or *Adhan* (the full adhan plays, with a **Stop** button in the notification). Set this for each prayer in Settings, or tap the bell icon next to a prayer on the main screen.
- **Time remaining:** a live countdown to the next prayer, plus "in 2h 15m" / "Now" / "Passed" next to each prayer.
- **"Have you prayed?" reminder:** before the next adhan (20 minutes by default; you can pick 10–45), you get a notification asking whether you've prayed the current prayer. It has **Yes, I prayed** and **Not yet** buttons. For Fajr, the reminder comes before sunrise, because that's when Fajr ends. If you've already ticked the prayer as prayed, by tapping the checkbox on the main screen or a notification button, there's no reminder.
- Shows the Hijri date. Supports 20 calculation methods, including Egyptian, MWL, Umm al-Qura, ISNA, Moonsighting Committee, Karachi, the Gulf states, Turkey, Morocco, Algeria, Tunisia, Jordan, Malaysia, Indonesia, France and Russia. Asr can be Standard or Hanafi. High latitudes are adjusted too.

## Adhan sound

The app doesn't ship an adhan recording. In **Settings → At prayer time → Adhan sound → Choose file**, pick any adhan audio file on your phone (mp3, ogg, …). If you don't pick one, the phone's default alarm sound is used. Tap **Test** to hear it.

## Install

Every push builds a debug APK on GitHub Actions. Open the **Actions** tab, select the latest run, and download the `azan-debug-apk` artifact. Unzip it and install `app-debug.apk` on the phone. You'll need to allow installing from unknown sources.

To build it yourself, use JDK 17+ and the Android SDK:

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # prayer time + scheduling tests
```

## First run

1. Allow notifications.
2. Set your location: tap **Use current location**, or type in the latitude and longitude.
3. On Android 12+, tap **Allow** on the "Alarms & reminders" banner so the adhan plays on time.
4. If adhans come late on your phone, go to **Settings → Battery settings** and turn off battery optimization for Azan.

## Code layout

| Path | What |
| --- | --- |
| `prayer/PrayerCalculator.kt` | Astronomical prayer time calculation (PrayTimes algorithm). Tested against the `adhan` library. |
| `prayer/PrayerSchedule.kt` | Pure logic for the next adhan, the next reminder, the current prayer, and the next prayer. |
| `alarm/` | Exact alarms (`AlarmScheduler`), alarm/boot receivers, notifications, and the foreground `AdhanService` that plays the adhan. |
| `sync/` | Aladhan API client, offline cache, and the WorkManager job that syncs whenever the phone is online. |
| `prayer/PrayerTimesSource.kt` | Picks synced or calculated times, applies adjustments, and validates downloaded data. |
| `data/SettingsStore.kt` | Settings and "prayed" flags (SharedPreferences). |
| `ui/` | Jetpack Compose screens. |
