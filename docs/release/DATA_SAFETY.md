# DevToolkit Data Safety Guide

Last updated: March 24, 2026

This file is a practical reference for completing the Google Play Data safety form for the current DevToolkit codebase in this repo.

## Recommended form position

- Does the app collect or share any user data? `No`
- Is all processing performed on device? `Yes`
- Does the app include analytics SDKs? `No`
- Does the app include advertising SDKs? `No`
- Does the app transfer app activity, personal info, files, messages, photos, audio, location, contacts, identifiers, or diagnostics to the developer or third parties? `No`

## Why this is the current answer

- The manifest declares no network permission in [`AndroidManifest.xml`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/app/src/main/AndroidManifest.xml)
- The app opts out of Android cloud backup in [`AndroidManifest.xml`](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/app/src/main/AndroidManifest.xml)
- The app has no backend service and no sign-in flow
- Tool inputs, outputs, history, swatches, saved regexes, and saved pipelines stay on device
- There are no analytics, ads, or tracking dependencies in the app module
- File access is limited to files the user explicitly selects through the Android document picker

## Important nuance

- The app can prefill tools from the clipboard or from Android share intents while you are actively using it
- The app can also export content or files when you explicitly choose a share or save action
- Those user-initiated flows do not make DevToolkit a data-collecting service on their own, but if future versions add cloud sync, crash reporting, analytics, remote APIs, or account features, the form answers must be updated

## Local-only data the app stores

- Theme and display preferences
- Tool ordering and other settings
- Local history entries
- Saved regex patterns
- Saved text transform pipelines
- Saved colour swatches

## Release check before submitting the form

1. Confirm the app still declares no network permission.
2. Confirm no new SDKs were added for analytics, ads, crash reporting, or remote feature flags.
3. Confirm the privacy policy text still matches the shipped behavior.
4. If any networked feature was added, stop and update this file, the privacy policy, and the Play Console answers before publishing.
