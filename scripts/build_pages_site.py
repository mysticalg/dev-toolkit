from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_POLICY = ROOT / "docs" / "release" / "privacy-policy.html"
SOURCE_ADSENSE_CONFIG = ROOT / "docs" / "release" / "adsense-config.js"
SOURCE_ADS_TXT = ROOT / "docs" / "release" / "ads.txt"
SITE_DIR = ROOT / "site"
PRIVACY_DIR = SITE_DIR / "privacy-policy"


INDEX_HTML = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>DevToolkit</title>
  <style data-footer-adsense-style>
    .footer-adsense {
      margin-top: 24px;
      padding-top: 18px;
      display: grid;
      gap: 12px;
      width: 100%;
      border-top: 1px solid rgba(255, 255, 255, 0.08);
    }

    .footer-adsense__label {
      margin: 0;
      font-size: 12px;
      letter-spacing: 0.14em;
      text-transform: uppercase;
      color: var(--muted);
    }

    .footer-adsense__unit {
      display: block;
      width: 100%;
      min-height: 90px;
    }
  </style>
  <script src="./adsense-config.js"></script>
  <script>
    (function () {
      const config = window.__FOOTER_ADSENSE__;
      if (
        !config ||
        !config.client ||
        !config.slot ||
        document.querySelector("script[data-footer-adsense-loader]")
      ) {
        return;
      }

      const script = document.createElement("script");
      script.async = true;
      script.src =
        "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=" +
        encodeURIComponent(config.client);
      script.crossOrigin = "anonymous";
      script.dataset.footerAdsenseLoader = "true";
      document.head.appendChild(script);
    })();
  </script>
  <style>
    :root {
      color-scheme: light;
      --bg: #09131f;
      --panel: rgba(17, 30, 48, 0.9);
      --panel-border: rgba(100, 149, 237, 0.22);
      --text: #f8fbff;
      --muted: #b9c6d6;
      --accent: #42a5f5;
      --accent-strong: #1565c0;
      --success: #0f766e;
    }

    * {
      box-sizing: border-box;
    }

    body {
      margin: 0;
      min-height: 100vh;
      font-family: "Segoe UI", Arial, sans-serif;
      color: var(--text);
      background:
        radial-gradient(circle at top right, rgba(21, 101, 192, 0.65), transparent 32%),
        radial-gradient(circle at bottom left, rgba(0, 137, 123, 0.32), transparent 26%),
        linear-gradient(180deg, #07101c 0%, var(--bg) 100%);
    }

    main {
      max-width: 960px;
      margin: 0 auto;
      padding: 56px 20px 80px;
    }

    .hero {
      padding: 40px;
      border-radius: 28px;
      background: var(--panel);
      border: 1px solid var(--panel-border);
      box-shadow: 0 24px 60px rgba(0, 0, 0, 0.3);
      backdrop-filter: blur(14px);
    }

    .eyebrow {
      display: inline-block;
      margin-bottom: 18px;
      padding: 8px 14px;
      border-radius: 999px;
      background: rgba(66, 165, 245, 0.14);
      color: var(--accent);
      font-size: 0.9rem;
      font-weight: 600;
      letter-spacing: 0.02em;
    }

    h1 {
      margin: 0 0 16px;
      font-size: clamp(2.4rem, 6vw, 4.6rem);
      line-height: 0.98;
    }

    p {
      color: var(--muted);
      line-height: 1.65;
      font-size: 1.02rem;
    }

    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 14px;
      margin-top: 28px;
    }

    .button {
      display: inline-block;
      padding: 14px 20px;
      border-radius: 16px;
      font-weight: 600;
      text-decoration: none;
    }

    .button-primary {
      background: var(--accent-strong);
      color: #fff;
    }

    .button-secondary {
      background: rgba(255, 255, 255, 0.08);
      color: var(--text);
      border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .grid {
      display: grid;
      gap: 18px;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      margin-top: 28px;
    }

    .card {
      padding: 22px;
      border-radius: 20px;
      background: rgba(10, 18, 29, 0.82);
      border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .card h2 {
      margin: 0 0 10px;
      font-size: 1.05rem;
      color: var(--text);
    }

    .card p {
      margin: 0;
      font-size: 0.96rem;
    }

    .status {
      margin-top: 26px;
      color: #d1fae5;
      font-weight: 600;
    }
  </style>
</head>
<body>
  <main>
    <section class="hero">
      <span class="eyebrow">Offline Android developer tools</span>
      <h1>DevToolkit</h1>
      <p>DevToolkit bundles JSON, regex, Base64, hash, JWT, URL, UUID, colour, epoch, diff, mock data, widgets, and local history workflows into one offline-first Android app.</p>
      <div class="actions">
        <a class="button button-primary" href="./privacy-policy/">Open Privacy Policy</a>
        <a class="button button-secondary" href="https://github.com/mysticalg/dev-toolkit">View Repository</a>
      </div>
      <div class="grid">
        <article class="card">
          <h2>Fast and local</h2>
          <p>No backend dependency for the core tool experience. Inputs and outputs stay on-device.</p>
        </article>
        <article class="card">
          <h2>Play-ready docs</h2>
          <p>This Pages site gives the app a public privacy policy URL for the Play Console listing.</p>
        </article>
        <article class="card">
          <h2>Built for release</h2>
          <p>The repo also includes store assets, Data safety notes, release metadata, and Android CI.</p>
        </article>
      </div>
      <p class="status">Expected privacy policy URL after GitHub Pages is enabled: https://mysticalg.github.io/dev-toolkit/privacy-policy/</p>
      <div class="footer-adsense" data-footer-adsense hidden>
        <p class="footer-adsense__label">Advertisement</p>
        <ins
          class="adsbygoogle footer-adsense__unit"
          style="display:block"
          data-ad-format="auto"
          data-full-width-responsive="true"
        ></ins>
      </div>
      <script>
        (function () {
          const wrapper = document.currentScript.previousElementSibling;
          const config = window.__FOOTER_ADSENSE__;
          if (!wrapper || !config || !config.client || !config.slot) {
            return;
          }

          const unit = wrapper.querySelector(".adsbygoogle");
          if (!unit) {
            return;
          }

          unit.setAttribute("data-ad-client", config.client);
          unit.setAttribute("data-ad-slot", config.slot);
          wrapper.hidden = false;
          try {
            (window.adsbygoogle = window.adsbygoogle || []).push({});
          } catch (error) {
            console.error("Footer AdSense failed to render", error);
          }
        })();
      </script>
    </section>
  </main>
</body>
</html>
"""


def main() -> None:
    SITE_DIR.mkdir(parents=True, exist_ok=True)
    PRIVACY_DIR.mkdir(parents=True, exist_ok=True)

    SITE_DIR.joinpath("index.html").write_text(INDEX_HTML, encoding="utf-8")
    SITE_DIR.joinpath(".nojekyll").write_text("", encoding="utf-8")
    SITE_DIR.joinpath("404.html").write_text(INDEX_HTML, encoding="utf-8")
    PRIVACY_DIR.joinpath("index.html").write_text(SOURCE_POLICY.read_text(encoding="utf-8"), encoding="utf-8")
    SITE_DIR.joinpath("adsense-config.js").write_text(SOURCE_ADSENSE_CONFIG.read_text(encoding="utf-8"), encoding="utf-8")
    PRIVACY_DIR.joinpath("adsense-config.js").write_text(SOURCE_ADSENSE_CONFIG.read_text(encoding="utf-8"), encoding="utf-8")
    SITE_DIR.joinpath("ads.txt").write_text(SOURCE_ADS_TXT.read_text(encoding="utf-8"), encoding="utf-8")
    PRIVACY_DIR.joinpath("ads.txt").write_text(SOURCE_ADS_TXT.read_text(encoding="utf-8"), encoding="utf-8")


if __name__ == "__main__":
    main()
