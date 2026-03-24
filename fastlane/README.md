# Fastlane Metadata Mirror

This folder stores Play listing text and release notes in the standard metadata layout used by Fastlane `supply`.

The current repo keeps generated screenshots and graphics in [`docs/release/assets`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/docs/release/assets), while the listing copy lives in [`fastlane/metadata/android/en-US`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/fastlane/metadata/android/en-US).

Notes:

- The metadata files are safe to commit and review like source code.
- The CI workflow in [android-ci.yml](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/.github/workflows/android-ci.yml) verifies lint, debug assembly, and release bundle generation.
- A GitHub Actions release bundle will be debug-signed unless you later wire in your own signing secrets and keystore setup.
