"""Generates the three megaphone item icons (16x16, flat style) for the Amplified Jukebox mod."""

import os
from PIL import Image, ImageDraw

OUT = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src", "main", "resources", "assets", "amplifiedjukebox", "textures", "item",
)

TIERS = {
    1: {"mid": (110, 193, 228), "light": (188, 232, 250), "dark": (43, 110, 138), "wave": (140, 205, 235)},
    2: {"mid": (242, 180, 65), "light": (255, 219, 152), "dark": (138, 90, 22), "wave": (250, 200, 110)},
    3: {"mid": (226, 100, 90), "light": (255, 168, 162), "dark": (126, 42, 34), "wave": (243, 140, 130)},
}


def draw_icon(tier: int) -> Image.Image:
    c = TIERS[tier]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Horn body: cone that widens towards the mouth on the right.
    horn = [(2, 7), (9, 3), (9, 12), (2, 9)]
    d.polygon(horn, fill=c["mid"])
    # Outline.
    d.line([horn[0], horn[1]], fill=c["dark"])
    d.line([horn[1], horn[2]], fill=c["dark"])
    d.line([horn[2], horn[3]], fill=c["dark"])
    d.line([horn[3], horn[0]], fill=c["dark"])
    # Highlight along the upper face.
    d.line([(4, 6), (8, 4)], fill=c["light"])
    # Mouth rim.
    d.rectangle([9, 3, 10, 12], fill=c["dark"])
    d.line([(9, 4), (9, 11)], fill=c["light"])

    # Handle below the horn.
    d.rectangle([3, 10, 5, 14], fill=c["dark"])
    d.rectangle([4, 11, 5, 13], fill=c["mid"])

    # Sound waves: one bar per tier, further to the right each time.
    waves = {1: [(12, 6, 10)], 2: [(12, 5, 11), (14, 7, 9)], 3: [(12, 4, 12), (14, 6, 10)]}
    for x, y0, y1 in waves[tier]:
        d.line([(x, y0), (x, y1)], fill=c["wave"])
    if tier == 3:
        d.point((15, 8), fill=c["wave"])

    return img


def main() -> None:
    os.makedirs(OUT, exist_ok=True)
    for tier in TIERS:
        path = os.path.join(OUT, f"amplifier_{tier}.png")
        draw_icon(tier).save(path)
        print(path)


if __name__ == "__main__":
    main()
