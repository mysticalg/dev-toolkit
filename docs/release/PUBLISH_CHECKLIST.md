# DevToolkit Publish Checklist

Last updated: March 24, 2026

## Build artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release bundle: `app/build/outputs/bundle/release/app-release.aab`

## Before upload
1. Create `keystore.properties` from [`keystore.properties.example`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/keystore.properties.example) and point it at your real upload key.
2. Build a fresh release bundle with `.\gradlew.bat :app:bundleRelease`.
3. Host [`privacy-policy.html`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/docs/release/privacy-policy.html) or [`privacy-policy.md`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/docs/release/privacy-policy.md) at a public, non-editable URL for the Play Console privacy policy field.
   GitHub Pages is already wired in [`GITHUB_PAGES_SETUP.md`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/docs/release/GITHUB_PAGES_SETUP.md) with the expected policy URL `https://mysticalg.github.io/dev-toolkit/privacy-policy/`.
4. Fill out the Google Play Data safety form using [`DATA_SAFETY.md`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/docs/release/DATA_SAFETY.md) as the repo-backed answer sheet.
5. Add store assets from [`docs/release/assets/README.md`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/docs/release/assets/README.md) and copy from [`PLAY_STORE_LISTING.md`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/docs/release/PLAY_STORE_LISTING.md).
6. If you want CI verification or Fastlane-compatible metadata, use [android-ci.yml](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/.github/workflows/android-ci.yml) and [`fastlane/metadata/android/en-US`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/fastlane/metadata/android/en-US).

## App status in this repo
- `targetSdk = 35` and `compileSdk = 35` in [`app/build.gradle.kts`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/app/build.gradle.kts)
- Release shrinking is enabled and verified
- Adaptive launcher icon is configured
- In-app privacy policy screen is available from Settings
- Android cloud backup is disabled in [`AndroidManifest.xml`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/app/src/main/AndroidManifest.xml)
- No network permission is declared in [`AndroidManifest.xml`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/app/src/main/AndroidManifest.xml)

## Official references
- Target API requirement: [Android Developers](https://developer.android.com/google/play/requirements/target-sdk)
- User data, Data safety, and privacy policy requirements: [Play Console Help](https://support.google.com/googleplay/android-developer/answer/10144311)
- App bundle publishing overview: [Android App Bundles](https://developer.android.com/guide/app-bundle)
