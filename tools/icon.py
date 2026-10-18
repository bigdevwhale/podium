"""Draws the launcher icon (the runner pictogram on a light-blue field, a stripe of the Munich
rainbow under its feet) and docs/logo.png.

python tools/icon.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
BLUE = (28, 136, 204)
WHITE = (255, 255, 255)
STRIPES = [(60, 157, 80), (124, 192, 138), (127, 188, 230), (234, 132, 36)]
S = 4  # supersampling

# The runner, on the 24-unit grid of the in-app pictograms (ui/art/Pictograms.kt).
HEAD = (14.5, 3.4)
LIMBS = [
    [(13.6, 7.2), (10.6, 13)],
    [(10.6, 13), (15, 15.2), (14, 20.8)],
    [(10.6, 13), (7.8, 16.8), (3.8, 17.6)],
    [(13.2, 8), (16.4, 10), (18.8, 7.8)],
    [(12.8, 8.4), (9.6, 9.6), (7.8, 12.4)],
]


def runner(d: ImageDraw.ImageDraw, x0: float, y0: float, k: float, color):
    w = 2.5 * k
    for limb in LIMBS:
        pts = [(x0 + x * k, y0 + y * k) for x, y in limb]
        d.line(pts, fill=color, width=int(w), joint="curve")
        for px, py in pts:  # Round caps and joints.
            d.ellipse((px - w / 2, py - w / 2, px + w / 2, py + w / 2), fill=color)
    hx, hy = x0 + HEAD[0] * k, y0 + HEAD[1] * k
    r = 2.3 * k
    d.ellipse((hx - r, hy - r, hx + r, hy + r), fill=color)


def foreground(size: int, background: bool) -> Image.Image:
    """The icon on a size x size canvas in 108 dp units; the safe zone is the middle 72."""
    W = size * S
    u = W / 108
    im = Image.new("RGBA", (W, W), BLUE + (255,) if background else (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # The track under the runner: four stripes of the Munich colours.
    for i, c in enumerate(STRIPES):
        y = (71 + i * 3.2) * u
        d.rectangle((26 * u, y, 82 * u, y + 2.4 * u), fill=c)
    runner(d, 30 * u, 28 * u, 2.0 * u, WHITE)
    return im.resize((size, size), Image.LANCZOS)


def main():
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dpi, px in sizes.items():
        out = RES / f"mipmap-{dpi}"
        out.mkdir(parents=True, exist_ok=True)
        fg = 108 * px // 48
        foreground(fg, False).save(out / "ic_launcher_foreground.png")
        full = foreground(fg, True).crop(((fg - px) // 2 - px // 6, (fg - px) // 2 - px // 6, (fg + px) // 2 + px // 6, (fg + px) // 2 + px // 6)).resize((px, px), Image.LANCZOS)
        full.save(out / "ic_launcher.png")
        mask = Image.new("L", full.size, 0)
        ImageDraw.Draw(mask).ellipse((0, 0, px - 1, px - 1), fill=255)
        rnd = Image.new("RGBA", full.size, (0, 0, 0, 0))
        rnd.paste(full, (0, 0), mask)
        rnd.save(out / "ic_launcher_round.png")
    logo = foreground(432, True).crop((54, 54, 378, 378)).resize((256, 256), Image.LANCZOS)
    mask = Image.new("L", logo.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, 255, 255), radius=56, fill=255)
    out = Image.new("RGBA", logo.size, (0, 0, 0, 0))
    out.paste(logo, (0, 0), mask)
    (ROOT / "docs").mkdir(exist_ok=True)
    out.save(ROOT / "docs/logo.png")


if __name__ == "__main__":
    main()
