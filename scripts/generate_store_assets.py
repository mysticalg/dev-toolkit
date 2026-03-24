from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
ASSETS_DIR = ROOT / "docs" / "release" / "assets"
SCREENSHOTS_DIR = ASSETS_DIR / "phone-screenshots"

BLUE_700 = "#1565C0"
BLUE_500 = "#1E88E5"
BLUE_400 = "#42A5F5"
TEAL = "#00897B"
ORANGE = "#FB8C00"
INK = "#09131F"
CARD = "#111E30"
CARD_ALT = "#0D1827"
TEXT = "#F8FBFF"
MUTED = "#A8B6C7"
LINE = "#223249"
SUCCESS = "#4CAF50"
ERROR = "#FF6B6B"


def font(size: int, bold: bool = False):
    candidates = [
        "C:/Windows/Fonts/seguisb.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size=size)
    return ImageFont.load_default()


def ensure_dirs():
    SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def rounded_box(draw: ImageDraw.ImageDraw, xy, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def draw_gradient(size, top_color, bottom_color):
    image = Image.new("RGB", size, top_color)
    draw = ImageDraw.Draw(image)
    width, height = size
    top = ImageColor(top_color)
    bottom = ImageColor(bottom_color)
    for y in range(height):
        blend = y / max(height - 1, 1)
        color = tuple(int(top[i] * (1 - blend) + bottom[i] * blend) for i in range(3))
        draw.line((0, y, width, y), fill=color)
    return image


def ImageColor(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def add_noise(image: Image.Image, opacity: int = 18):
    overlay = Image.new("RGBA", image.size, (255, 255, 255, 0))
    draw = ImageDraw.Draw(overlay)
    width, height = image.size
    step = 28
    for y in range(0, height, step):
        for x in range(0, width, step):
            if (x + y) // step % 2 == 0:
                draw.rectangle((x, y, x + 1, y + 1), fill=(255, 255, 255, opacity))
    return Image.alpha_composite(image.convert("RGBA"), overlay)


def draw_badge(draw, x, y, text, fill):
    badge_font = font(24, bold=True)
    bbox = draw.textbbox((0, 0), text, font=badge_font)
    width = bbox[2] - bbox[0] + 38
    rounded_box(draw, (x, y, x + width, y + 42), radius=21, fill=fill)
    draw.text((x + 19, y + 8), text, font=badge_font, fill=TEXT)
    return width


def draw_phone_shell(base: Image.Image, x: int, y: int, w: int, h: int):
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    rounded_box(draw, (x, y, x + w, y + h), radius=52, fill="#03060C", outline="#394B68", width=4)
    rounded_box(draw, (x + 18, y + 18, x + w - 18, y + h - 18), radius=40, fill=INK)
    rounded_box(draw, (x + w // 2 - 80, y + 20, x + w // 2 + 80, y + 38), radius=9, fill="#1B283A")
    base.alpha_composite(overlay)
    return x + 28, y + 42, w - 56, h - 70


def panel(draw, x, y, w, h, title):
    rounded_box(draw, (x, y, x + w, y + h), radius=26, fill=CARD, outline=LINE, width=2)
    draw.text((x + 24, y + 18), title, font=font(26, bold=True), fill=TEXT)


def draw_code_block(draw, x, y, w, lines, accent=BLUE_400):
    line_height = 22
    height = 26 + len(lines) * line_height
    rounded_box(draw, (x, y, x + w, y + height), radius=18, fill=CARD_ALT, outline=LINE, width=2)
    for index, text in enumerate(lines):
        line_y = y + 16 + index * line_height
        prefix = f"{index + 1}".rjust(2)
        draw.text((x + 16, line_y), prefix, font=font(16), fill=MUTED)
        draw.text((x + 52, line_y), text, font=font(18), fill=accent if index == 0 else TEXT)
    return height


def centered_text(draw, box, text, text_font, fill):
    bbox = draw.textbbox((0, 0), text, font=text_font)
    x = box[0] + (box[2] - box[0] - (bbox[2] - bbox[0])) / 2
    y = box[1] + (box[3] - box[1] - (bbox[3] - bbox[1])) / 2
    draw.text((x, y), text, font=text_font, fill=fill)


def fit_font_for_width(draw, text, start_size: int, min_size: int, max_width: int, bold: bool = False):
    size = start_size
    while size > min_size:
        candidate = font(size, bold=bold)
        bbox = draw.textbbox((0, 0), text, font=candidate)
        if bbox[2] - bbox[0] <= max_width:
            return candidate
        size -= 2
    return font(min_size, bold=bold)


def build_feature_graphic():
    img = Image.new("RGBA", (1024, 500), ImageColor(INK) + (255,))
    bg = Image.new("RGBA", img.size, ImageColor("#07101C") + (255,))
    gradient = Image.new("RGBA", img.size, (0, 0, 0, 0))
    grad_draw = ImageDraw.Draw(gradient)
    for y in range(img.height):
        blend = y / img.height
        color = (
            int(7 * (1 - blend) + 16 * blend),
            int(16 * (1 - blend) + 38 * blend),
            int(28 * (1 - blend) + 64 * blend),
            255,
        )
        grad_draw.line((0, y, img.width, y), fill=color)
    img.alpha_composite(bg)
    img.alpha_composite(gradient)
    img = add_noise(img, opacity=20)
    draw = ImageDraw.Draw(img)

    draw.ellipse((640, -40, 980, 300), fill=ImageColor(BLUE_700) + (80,))
    draw.ellipse((720, 220, 1120, 620), fill=ImageColor(TEAL) + (48,))
    draw.ellipse((-120, 320, 180, 620), fill=ImageColor(ORANGE) + (32,))

    draw.text((66, 76), "DevToolkit", font=font(72, bold=True), fill=TEXT)
    draw.text((66, 160), "Offline developer tools for Android", font=font(28), fill=MUTED)
    draw.text((66, 205), "JSON, regex, Base64, JWT, hash, colour, UUID, and more.", font=font(24), fill=TEXT)

    bx = 66
    for label, color in [
        ("JSON", BLUE_500),
        ("Regex", TEAL),
        ("Base64", BLUE_700),
        ("JWT", ORANGE),
        ("Hash", "#33691E"),
    ]:
        bx += draw_badge(draw, bx, 258, label, color) + 10

    inner_x, inner_y, inner_w, inner_h = draw_phone_shell(img, 700, 52, 260, 396)
    phone = ImageDraw.Draw(img)
    panel(phone, inner_x, inner_y, inner_w, 86, "Quick Tools")
    rounded_box(phone, (inner_x + 18, inner_y + 48, inner_x + 104, inner_y + 74), radius=13, fill=BLUE_700)
    centered_text(phone, (inner_x + 18, inner_y + 48, inner_x + 104, inner_y + 74), "JSON", font(16, bold=True), TEXT)
    rounded_box(phone, (inner_x + 112, inner_y + 48, inner_x + 210, inner_y + 74), radius=13, fill=TEAL)
    centered_text(phone, (inner_x + 112, inner_y + 48, inner_x + 210, inner_y + 74), "Regex", font(16, bold=True), TEXT)

    panel(phone, inner_x, inner_y + 104, inner_w, 172, "Formatter")
    draw_code_block(
        phone,
        inner_x + 18,
        inner_y + 142,
        inner_w - 36,
        ['{ "user": "dev"', '  "tools": ["json", "hash"]', "}"],
    )
    rounded_box(phone, (inner_x + 18, inner_y + 286, inner_x + inner_w - 18, inner_y + 324), radius=16, fill="#0E3A6A")
    centered_text(phone, (inner_x + 18, inner_y + 286, inner_x + inner_w - 18, inner_y + 324), "Fast. Local. No ads.", font(18, bold=True), TEXT)

    out = ASSETS_DIR / "feature-graphic.png"
    img.convert("RGB").save(out, "PNG")


def build_play_icon():
    img = Image.new("RGBA", (512, 512), ImageColor(BLUE_700) + (255,))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((0, 0, 511, 511), radius=108, fill=BLUE_700)
    draw.ellipse((300, -20, 560, 240), fill=ImageColor(BLUE_400) + (92,))
    draw.ellipse((-40, 310, 210, 560), fill=ImageColor(TEAL) + (70,))
    draw.polygon([(142, 118), (74, 256), (142, 394), (188, 394), (120, 256), (188, 118)], fill=TEXT)
    draw.polygon([(370, 118), (438, 256), (370, 394), (324, 394), (392, 256), (324, 118)], fill=TEXT)
    for y in (150, 226, 302):
        draw.rounded_rectangle((220, y, 292, y + 40), radius=10, fill=TEXT)
    out = ASSETS_DIR / "play-icon-512.png"
    img.save(out, "PNG")


def screenshot_canvas(title: str, subtitle: str):
    img = Image.new("RGBA", (1080, 1920), ImageColor("#07101C") + (255,))
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    for y in range(img.height):
        blend = y / img.height
        color = (
            int(7 * (1 - blend) + 15 * blend),
            int(16 * (1 - blend) + 29 * blend),
            int(28 * (1 - blend) + 48 * blend),
            255,
        )
        draw.line((0, y, img.width, y), fill=color)
    draw.ellipse((620, -120, 1180, 440), fill=ImageColor(BLUE_500) + (82,))
    draw.ellipse((-160, 1280, 340, 1780), fill=ImageColor(TEAL) + (64,))
    img.alpha_composite(overlay)
    img = add_noise(img, opacity=18)
    draw = ImageDraw.Draw(img)
    title_font = fit_font_for_width(draw, title, start_size=72, min_size=52, max_width=920, bold=True)
    subtitle_font = fit_font_for_width(draw, subtitle, start_size=30, min_size=24, max_width=920)
    draw.text((80, 84), title, font=title_font, fill=TEXT)
    title_bbox = draw.textbbox((80, 84), title, font=title_font)
    subtitle_y = title_bbox[3] + 18
    draw.text((80, subtitle_y), subtitle, font=subtitle_font, fill=MUTED)
    return img


def build_home_screenshot():
    img = screenshot_canvas("Every dev tool in one app", "Search, reorder, share, and jump in fast.")
    screen_x, screen_y, screen_w, screen_h = draw_phone_shell(img, 160, 330, 760, 1380)
    draw = ImageDraw.Draw(img)
    panel(draw, screen_x, screen_y, screen_w, 110, "DevToolkit")
    rounded_box(draw, (screen_x + 24, screen_y + 52, screen_x + screen_w - 24, screen_y + 86), radius=16, fill=CARD_ALT, outline=LINE)
    draw.text((screen_x + 42, screen_y + 58), "Search tools...", font=font(20), fill=MUTED)

    cards = [
        ("JSON Formatter", "Format, validate, convert"),
        ("Regex Tester", "Patterns, groups, replace"),
        ("Base64", "Text and file flows"),
        ("Hash Generator", "MD5, SHA, HMAC"),
        ("JWT Decoder", "Claims and expiry"),
        ("Colour Picker", "Contrast and swatches"),
    ]
    y = screen_y + 132
    for title, desc in cards:
        rounded_box(draw, (screen_x + 22, y, screen_x + screen_w - 22, y + 120), radius=22, fill=CARD, outline=LINE, width=2)
        draw.text((screen_x + 44, y + 26), title, font=font(28, bold=True), fill=TEXT)
        draw.text((screen_x + 44, y + 66), desc, font=font(20), fill=MUTED)
        y += 136
    img.convert("RGB").save(SCREENSHOTS_DIR / "01-home.png", "PNG")


def build_json_screenshot():
    img = screenshot_canvas("Format and validate on-device", "Pretty print, minify, export, and inspect large payloads.")
    screen_x, screen_y, screen_w, screen_h = draw_phone_shell(img, 160, 330, 760, 1380)
    draw = ImageDraw.Draw(img)
    panel(draw, screen_x, screen_y, screen_w, 266, "JSON / YAML / XML")
    draw_code_block(draw, screen_x + 20, screen_y + 52, screen_w - 40, [
        "{ \"user\": { \"name\": \"Dev\",",
        "  \"roles\": [\"author\", \"tester\"]",
        "} }",
    ])
    rounded_box(draw, (screen_x + 20, screen_y + 210, screen_x + 206, screen_y + 244), radius=16, fill=BLUE_700)
    centered_text(draw, (screen_x + 20, screen_y + 210, screen_x + 206, screen_y + 244), "Validate", font(18, bold=True), TEXT)
    rounded_box(draw, (screen_x + 220, screen_y + 210, screen_x + 388, screen_y + 244), radius=16, fill=TEAL)
    centered_text(draw, (screen_x + 220, screen_y + 210, screen_x + 388, screen_y + 244), "To YAML", font(18, bold=True), TEXT)

    panel(draw, screen_x, screen_y + 286, screen_w, 430, "Output")
    draw_code_block(draw, screen_x + 20, screen_y + 334, screen_w - 40, [
        "{",
        "  \"user\": {",
        "    \"name\": \"Dev\",",
        "    \"roles\": [",
        "      \"author\",",
        "      \"tester\"",
        "    ]",
        "  }",
        "}",
    ])
    rounded_box(draw, (screen_x + 20, screen_y + 734, screen_x + screen_w - 20, screen_y + 844), radius=22, fill="#291C24", outline="#6C2A3A", width=2)
    draw.text((screen_x + 42, screen_y + 760), "Line 3, column 18: Expected ',' before object end", font=font(22, bold=True), fill=ERROR)
    draw.text((screen_x + 42, screen_y + 800), "Validation markers point directly to parse errors.", font=font(19), fill=MUTED)
    img.convert("RGB").save(SCREENSHOTS_DIR / "02-json.png", "PNG")


def build_regex_screenshot():
    img = screenshot_canvas("Test regex patterns instantly", "Named groups, replacement preview, and saved pattern history.")
    screen_x, screen_y, screen_w, screen_h = draw_phone_shell(img, 160, 330, 760, 1380)
    draw = ImageDraw.Draw(img)
    panel(draw, screen_x, screen_y, screen_w, 220, "Regex Tester")
    draw_code_block(draw, screen_x + 20, screen_y + 50, screen_w - 40, [
        "(?<ticket>[A-Z]{3}-\\d{4})",
        "Flags: i",
    ], accent=TEAL)
    rounded_box(draw, (screen_x + 20, screen_y + 162, screen_x + 212, screen_y + 196), radius=16, fill=TEAL)
    centered_text(draw, (screen_x + 20, screen_y + 162, screen_x + 212, screen_y + 196), "Replace mode", font(18, bold=True), TEXT)

    panel(draw, screen_x, screen_y + 244, screen_w, 496, "Results")
    rounded_box(draw, (screen_x + 20, screen_y + 294, screen_x + screen_w - 20, screen_y + 374), radius=18, fill=CARD_ALT, outline=LINE, width=2)
    draw.text((screen_x + 36, screen_y + 318), "Matches found: 2", font=font(24, bold=True), fill=TEXT)
    draw.text((screen_x + 36, screen_y + 352), "Deploy [ABC-1024] before [XYZ-8891] closes", font=font(20), fill=TEXT)
    rounded_box(draw, (screen_x + 20, screen_y + 396, screen_x + screen_w - 20, screen_y + 680), radius=18, fill=CARD_ALT, outline=LINE, width=2)
    draw.text((screen_x + 36, screen_y + 422), "Match 1", font=font(22, bold=True), fill=BLUE_400)
    draw.text((screen_x + 36, screen_y + 458), "ticket = ABC-1024", font=font(20), fill=TEXT)
    draw.text((screen_x + 36, screen_y + 514), "Match 2", font=font(22, bold=True), fill=BLUE_400)
    draw.text((screen_x + 36, screen_y + 550), "ticket = XYZ-8891", font=font(20), fill=TEXT)
    draw.text((screen_x + 36, screen_y + 622), "Engine: java.util.regex", font=font(18), fill=MUTED)
    img.convert("RGB").save(SCREENSHOTS_DIR / "03-regex.png", "PNG")


def build_crypto_screenshot():
    img = screenshot_canvas("Base64, hash, JWT, and URL tools", "Handle quick decode, verification, and file workflows on the go.")
    screen_x, screen_y, screen_w, screen_h = draw_phone_shell(img, 160, 330, 760, 1380)
    draw = ImageDraw.Draw(img)
    panel(draw, screen_x, screen_y, screen_w, 164, "Hash Generator")
    rounded_box(draw, (screen_x + 20, screen_y + 52, screen_x + 150, screen_y + 86), radius=16, fill=BLUE_700)
    centered_text(draw, (screen_x + 20, screen_y + 52, screen_x + 150, screen_y + 86), "Text", font(18, bold=True), TEXT)
    rounded_box(draw, (screen_x + 164, screen_y + 52, screen_x + 292, screen_y + 86), radius=16, fill=TEAL)
    centered_text(draw, (screen_x + 164, screen_y + 52, screen_x + 292, screen_y + 86), "File", font(18, bold=True), TEXT)
    draw.text((screen_x + 24, screen_y + 116), "Match: SHA-256", font=font(26, bold=True), fill=SUCCESS)

    panel(draw, screen_x, screen_y + 188, screen_w, 362, "Hashes")
    draw_code_block(draw, screen_x + 20, screen_y + 236, screen_w - 40, [
        "SHA-256: 7f83b1657ff1fc53...",
        "SHA-512: 07e547d9586f6a73...",
        "HMAC-SHA256: 5d5c7ebe1e3d4c...",
    ], accent=BLUE_400)

    panel(draw, screen_x, screen_y + 576, screen_w, 260, "JWT Decoder")
    draw.text((screen_x + 24, screen_y + 626), "Valid for 5h until 2026-03-24T22:14:00+00:00", font=font(22, bold=True), fill=SUCCESS)
    draw.text((screen_x + 24, screen_y + 678), "iss = devtoolkit", font=font(20), fill=TEXT)
    draw.text((screen_x + 24, screen_y + 716), "aud = mobile", font=font(20), fill=TEXT)
    draw.text((screen_x + 24, screen_y + 754), "Signature verification is not performed offline.", font=font(18), fill=MUTED)
    img.convert("RGB").save(SCREENSHOTS_DIR / "04-crypto.png", "PNG")


def build_utilities_screenshot():
    img = screenshot_canvas("Utilities built for daily workflows", "Epoch, colour, UUID, mock data, widgets, and local history.")
    screen_x, screen_y, screen_w, screen_h = draw_phone_shell(img, 160, 330, 760, 1380)
    draw = ImageDraw.Draw(img)
    top_cards = [
        ("Epoch", "1711305600  ->  2026-03-24 12:00 UTC", BLUE_700),
        ("Colour", "Contrast ratio 8.14  AAA pass", TEAL),
        ("UUID", "550e8400-e29b-41d4-a716-446655440000", ORANGE),
        ("Mock Data", "JSON schema and CSV export", "#6A1B9A"),
    ]
    y = screen_y
    for title, desc, color in top_cards:
        rounded_box(draw, (screen_x, y, screen_x + screen_w, y + 162), radius=24, fill=CARD, outline=LINE, width=2)
        rounded_box(draw, (screen_x + 20, y + 18, screen_x + 144, y + 50), radius=16, fill=color)
        centered_text(draw, (screen_x + 20, y + 18, screen_x + 144, y + 50), title, font(16, bold=True), TEXT)
        draw.text((screen_x + 24, y + 72), desc, font=font(22, bold=True), fill=TEXT)
        y += 178

    rounded_box(draw, (screen_x, y, screen_x + screen_w, y + 260), radius=24, fill=CARD, outline=LINE, width=2)
    draw.text((screen_x + 24, y + 24), "Pinned History", font=font(26, bold=True), fill=TEXT)
    draw.text((screen_x + 24, y + 74), "Starred entries stay at the top and can be filtered by tool.", font=font(19), fill=MUTED)
    draw.text((screen_x + 24, y + 132), "JSON Formatter   Pinned   Mar 24, 14:20", font=font(20, bold=True), fill=BLUE_400)
    draw.text((screen_x + 24, y + 172), "{ \"release\": true, \"widgets\": 3 }", font=font(18), fill=TEXT)
    draw.text((screen_x + 24, y + 216), "Quick Paste widget sends clipboard content to the best tool.", font=font(18), fill=MUTED)
    img.convert("RGB").save(SCREENSHOTS_DIR / "05-utilities.png", "PNG")


def main():
    ensure_dirs()
    build_feature_graphic()
    build_play_icon()
    build_home_screenshot()
    build_json_screenshot()
    build_regex_screenshot()
    build_crypto_screenshot()
    build_utilities_screenshot()


if __name__ == "__main__":
    main()
