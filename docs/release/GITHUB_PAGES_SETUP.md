# GitHub Pages Setup

Last updated: March 24, 2026

This repo now includes a GitHub Pages deployment workflow for the public privacy policy URL required by Google Play.

## Expected URL

Based on the current `origin` remote, the public privacy policy URL will be:

`https://mysticalg.github.io/dev-toolkit/privacy-policy/`

## What is in the repo

- Pages workflow: [pages-privacy-policy.yml](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/.github/workflows/pages-privacy-policy.yml)
- Static site builder: [build_pages_site.py](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/scripts/build_pages_site.py)
- Source privacy policy: [privacy-policy.html](/C:/Users/drhoo/OneDrive/Documents/GitHub/dev-toolkit/docs/release/privacy-policy.html)

## One-time GitHub setup

1. Push the repo to GitHub.
2. Open `Settings > Pages` for the repository.
3. Set the source to `GitHub Actions`.
4. Push to `main` or run the `Deploy Privacy Policy Site` workflow manually from the Actions tab.
5. Wait for the workflow to finish and then open the expected URL above.
6. Paste that public URL into the Play Console privacy policy field.

## Notes

- The workflow only rebuilds when the Pages workflow or privacy policy source changes, unless you trigger it manually.
- The generated site includes a small landing page at the site root and the privacy policy under `/privacy-policy/`.
