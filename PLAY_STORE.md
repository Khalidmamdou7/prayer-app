# Publishing Azan on Google Play

What's already prepared in this repo, and what only you can do in the Play Console.

## Already done in the code

| Requirement | Where |
| --- | --- |
| Target API 36 (Android 16), required for new apps since 31 Aug 2026 | `app/build.gradle.kts` |
| Package name `io.github.khalidmamdou7.azan` | `app/build.gradle.kts`, `applicationId`. **It can never change after the first upload**, so change it now if you want a different one. |
| Signed Android App Bundle (`.aab`) | `./gradlew bundleRelease`, signed from `keystore.properties` or CI secrets |
| Version code and name, from one file | `version.properties` (`versionName=1.0.0` → version code 10000). Every release must raise it. |
| All languages in every install, so the in-app Arabic/English switch works | `bundle { language { enableSplit = false } }` |
| No ads SDKs and no analytics | Nothing to declare beyond "No ads" |
| Store listing text, English and Arabic | `fastlane/metadata/android/{en-US,ar}/` |
| Icon (512×512), feature graphic (1024×500), 4 phone screenshots per language (1080×1920) | `fastlane/metadata/android/*/images/`, regenerated with `./gradlew testDebugUnitTest -Pscreenshots=true --tests '*StoreAssetsTest*'` |
| Privacy policy, English and Arabic | `docs/privacy-policy.md` (fill in your contact email) |

## Step by step

### 1. Developer account

Sign up at <https://play.google.com/console/signup>. There's a one-time US$25 fee and identity verification.

**New personal accounts can't publish to production straight away.** Google first requires a **closed test with at least 12 testers who stay opted in for 14 days**. Plan for that: friends, family or your mosque's community group work well. Organisation accounts (which need a D-U-N-S number) skip this requirement.

### 2. Create the upload key (once)

```
tools/create-upload-key.sh
```

This creates `upload-keystore.jks` and `keystore.properties`. Both are git-ignored. **Back up the .jks file and its password somewhere safe.** With Play App Signing (the default), Google can reset a lost upload key, but it takes time.

### 3. Build the bundle

On your computer:

```
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

Or on GitHub: add the secrets the script prints (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`), then push a tag that matches `version.properties`:

```
git tag v1.0.0 && git push origin v1.0.0
```

The **Release (Google Play)** workflow builds and signs the bundle and attaches it as an artifact.

### 4. Create the app in Play Console

- **App name:** Azan, or "أذان" for the Arabic listing.
- **Default language:** English (United States). Then add Arabic under *Store presence → Store listings → Manage translations*.
- **App or game:** App. **Free or paid:** Free.
- **Upload the first `.aab` by hand** to *Testing → Closed testing*. After that, the workflow can upload automatically (see step 7).

### 5. Store listing

Copy from `fastlane/metadata/android/en-US/` and `.../ar/`:

| Field | File |
| --- | --- |
| App name (≤30 chars) | `title.txt` |
| Short description (≤80 chars) | `short_description.txt` |
| Full description | `full_description.txt` |
| App icon | `images/icon.png` |
| Feature graphic | `images/featureGraphic.png` |
| Phone screenshots | `images/phoneScreenshots/*.png` |
| Release notes | `changelogs/1.txt` |

**Category:** Lifestyle. **Contact email:** required, and shown publicly.

### 6. App content (Policy → App content)

- **Privacy policy:** you need a public URL. Fill in your email in `docs/privacy-policy.md`, then either:
  - turn on GitHub Pages for the repo and use `https://khalidmamdou7.github.io/prayer-app/privacy-policy` (the repository must be public for free Pages), or
  - paste the text into any public page you control.
- **Ads:** No, the app does not contain ads.
- **App access:** All functionality is available without special access.
- **Content rating:** fill in the questionnaire as a reference/utility app with no violence, no user content, no gambling and no purchases. It should come out as *Everyone* / *PEGI 3*.
- **Target audience:** 18 and over, or 13 and over. Don't include under-13s; that brings in the Families policy requirements.
- **Data safety:**
  - *Does your app collect or share user data?* **Yes**. Approximate location leaves the device when online sync is on.
  - **Location → Approximate location:** *Collected*, *not shared*. aladhan.com acts as a service provider computing times for the app. Processed ephemerally: **No**, since you can't vouch for their logs. Required or optional: **Optional** (sync can be turned off). Purpose: **App functionality**.
  - Everything else: not collected.
  - *Is data encrypted in transit?* **Yes** (HTTPS).
  - *Can users request deletion?* The developer doesn't store any data. Say so in the free-text answer.
- **Foreground service permissions:** declare **Media playback**. Description: "Plays the adhan (Islamic call to prayer) audio at the prayer time the user chose for full adhan. The user can stop it from the notification." Google asks for a short video link. Record your screen showing a prayer set to *Adhan* and the "Play" button in Settings, and upload it unlisted to YouTube.
- **Exact alarms:** the app uses `SCHEDULE_EXACT_ALARM`, which the user grants, so no declaration is needed. It does *not* use the restricted `USE_EXACT_ALARM`.
- **Government app / financial features / health:** No.

### 7. Optional: automatic uploads from GitHub

1. In Google Cloud, create a service account and a JSON key.
2. In Play Console, go to *Users and permissions*, invite the service account, and give it *Release to testing tracks* for this app.
3. Add the JSON as the repository secret `PLAY_SERVICE_ACCOUNT_JSON`.

Each tag then uploads a **draft** release to the internal testing track. You review it and roll it out from the console.

### 8. Testing → production

1. Run the closed test, with 12+ testers for 14 days.
2. Apply for production access from the Dashboard.
3. Promote the release.

Review usually takes from a few hours to a few days.

## For every update

1. Raise `versionName` in `version.properties`, e.g. `1.0.0` → `1.0.1` for fixes or `1.1.0` for features. The version code follows automatically (`1.0.1` → 10001, `1.1.0` → 10100), so Play and Android always treat it as newer.
2. Add release notes as `fastlane/metadata/android/{en-US,ar}/changelogs/<versionCode>.txt`.
3. Commit, then tag the same version: `git tag v1.0.1 && git push origin v1.0.1`. The workflow refuses to build if the tag and `version.properties` disagree, so you can't accidentally upload an old version number.

Updates keep everything users set up: settings, location and prayed ticks live in the app's storage, and alarms are re-armed automatically after the update is installed.

**Test builds:** debug APKs (from the **Build APK** workflow) are installed as a separate app, *io.github.khalidmamdou7.azan.debug*, signed with the committed `app/debug.keystore`. Each new test build installs over the previous one, and it can sit next to the Play version.
