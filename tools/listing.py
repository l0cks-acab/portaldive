"""Portal Dive CurseForge / README art: icon, banner and captioned gallery images.
python3 tools/listing.py <shots_dir> <out_dir>"""
import math
import os
import random
import sys
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

SH, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)
FD = '/mnt/skills/examples/canvas-design/canvas-fonts/'
TITLE = lambda s: ImageFont.truetype(FD + 'BricolageGrotesque-Bold.ttf', s)
BODY = lambda s: ImageFont.truetype(FD + 'BricolageGrotesque-Regular.ttf', s)
W, H = 1280, 720
GOLD, GOLD_DIM = (232, 178, 58), (140, 100, 24)
FRAME, FRAME_DARK = (120, 70, 190), (22, 16, 32)
PURPLE, PURPLE_HI = (179, 107, 255), (214, 170, 255)
DIMS = [(143, 203, 255), (255, 90, 42), (179, 107, 255)]  # overworld, nether, end
TEXT = (233, 228, 240)


def shot(name):
    return Image.open(os.path.join(SH, name)).convert('RGB')


def bg(img, tint=(14, 8, 24), H=H):
    im = img.resize((W, H), Image.LANCZOS).filter(ImageFilter.GaussianBlur(16))
    im = ImageEnhance.Brightness(im).enhance(0.45)
    im = Image.blend(im, Image.new('RGB', (W, H), tint), 0.45)
    grad = Image.linear_gradient('L').rotate(90).resize((W, H))
    shade = Image.new('RGB', (W, H), (6, 4, 10))
    im = Image.composite(im, shade, grad.point(lambda v: int(80 + v * 0.68)))
    return im.convert('RGBA')


def window(img, w, crop=None):
    if crop:
        img = img.crop(crop)
    h = round(img.height * w / img.width)
    return img.resize((w, h), Image.LANCZOS).convert('RGBA')


def place(canvas, win, x, y, glow=None):
    """Screenshot with a soft drop shadow, offset down and right."""
    pad, off = 60, 14
    sh = Image.new('RGBA', (win.width + pad * 2, win.height + pad * 2), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rectangle([pad + off, pad + off, pad + off + win.width, pad + off + win.height], fill=(0, 0, 0, 235))
    canvas.alpha_composite(sh.filter(ImageFilter.GaussianBlur(20)), (x - pad, y - pad))
    canvas.alpha_composite(win, (x, y))


def glow_text(canvas, xy, text, font, fill=(255, 255, 255), glow=PURPLE, radius=16, strength=200):
    layer = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(layer).text(xy, text, font=font, fill=glow + (strength,))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(radius)))
    ImageDraw.Draw(canvas).text(xy, text, font=font, fill=fill + (255,))


def chips(canvas, x, y):
    d = ImageDraw.Draw(canvas)
    for i, c in enumerate(DIMS):
        d.rectangle([x + i * 46, y, x + i * 46 + 38, y + 12], fill=c + (255,))


def trace(canvas, x0, x1, y):
    d = ImageDraw.Draw(canvas)
    d.line([x0, y, x1, y], fill=GOLD_DIM + (255,), width=2)
    d.rectangle([x1 - 3, y - 3, x1 + 3, y + 3], fill=GOLD + (255,))


def brand_text(canvas, xy, font, radius=16, strength=190):
    """PORTAL in end purple, DIVE in white."""
    x, y = xy
    glow_text(canvas, (x, y), 'PORTAL', font, fill=PURPLE_HI, glow=PURPLE, radius=radius, strength=strength)
    w = ImageDraw.Draw(canvas).textlength('PORTAL ', font=font)
    glow_text(canvas, (x + w, y), 'DIVE', font, glow=PURPLE, radius=radius, strength=strength)


def text_block(canvas, x, y, title, lines, tsize=60):
    if title == 'PORTAL DIVE':
        brand_text(canvas, (x, y), TITLE(tsize), radius=14, strength=170)
    else:
        glow_text(canvas, (x, y), title, TITLE(tsize), radius=14, strength=160)
    y += tsize + 34
    d = ImageDraw.Draw(canvas)
    for ln in lines:
        d.text((x, y), ln, font=BODY(26), fill=TEXT + (255,))
        y += 38
    return y


def save(canvas, name):
    im = canvas.convert('RGB')
    Image.frombytes('RGB', im.size, im.tobytes()).save(os.path.join(OUT, name), optimize=True)


# ---------- icon: pixel portal with hyperspace streaks bursting out ----------
def icon_art(px=32):
    n = 32
    a = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(a)
    rnd = random.Random(4)
    cx, cy = 15.5, 15.5
    # streaks behind the frame, radiating from the centre
    for i in range(22):
        ang = i / 22 * math.tau + rnd.uniform(-0.08, 0.08)
        r0, r1 = rnd.uniform(9, 11), rnd.uniform(13.5, 16)
        col = rnd.choice([(255, 255, 255), PURPLE_HI, (150, 220, 255)])
        for t in range(int(r0 * 4), int(r1 * 4)):
            r = t / 4
            x, y = round(cx + math.cos(ang) * r), round(cy + math.sin(ang) * r)
            if 0 <= x < n and 0 <= y < n:
                fade = 1 - (r - r0) / (r1 - r0)
                a.putpixel((x, y), col + (int(90 + 165 * fade),))
    # obsidian frame 18x24 centred
    x0, y0, x1, y1 = 7, 4, 24, 27
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            edge = x in (x0, x0 + 1, x1 - 1, x1) or y in (y0, y0 + 1, y1 - 1, y1)
            if edge:
                v = rnd.random()
                c = (24, 15, 38) if v < 0.6 else (40, 26, 62) if v < 0.9 else (78, 52, 120)
                a.putpixel((x, y), c + (255,))
    # portal swirl, brighter towards the centre
    for y in range(y0 + 2, y1 - 1):
        for x in range(x0 + 2, x1 - 1):
            dx, dy = (x - cx) / 7, (y - cy) / 10
            r = math.hypot(dx, dy)
            sw = 0.5 + 0.5 * math.sin(r * 7 - math.atan2(dy, dx) * 2)
            k = max(0.0, 1 - r) ** 1.3
            base = (88, 30, 170)
            hi = (190, 120, 255)
            c = tuple(int(base[i] + (hi[i] - base[i]) * (0.35 * sw + 0.65 * k)) for i in range(3))
            a.putpixel((x, y), c + (255,))
    # bright core + streaks in front, crossing the portal
    for (x, y), c in {(15, 15): (255, 255, 255), (16, 15): (255, 255, 255), (15, 16): (255, 255, 255), (16, 16): (255, 255, 255),
                      (14, 15): PURPLE_HI, (17, 16): PURPLE_HI, (15, 14): PURPLE_HI, (16, 17): PURPLE_HI}.items():
        a.putpixel((x, y), c + (255,))
    for ang in [0.5, 1.4, 2.5, 3.7, 4.6, 5.6]:
        for t in range(10, 26):
            r = t / 4
            x, y = round(cx + math.cos(ang) * r), round(cy + math.sin(ang) * r)
            a.putpixel((x, y), (255, 255, 255, int(255 - (r - 2.5) * 40)))
    return a.resize((n * px, n * px), Image.NEAREST)


def icon():
    S = 1024
    c = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    t = icon_art(28)
    x, y = (S - t.width) // 2, (S - t.height) // 2
    mask = Image.new('L', (S, S), 0)
    mask.paste(t.split()[3], (x, y))
    for r, k, col in [(90, 1.5, PURPLE), (45, 1.6, PURPLE), (18, 1.2, PURPLE_HI)]:
        al = mask.filter(ImageFilter.GaussianBlur(r)).point(lambda v, k=k: min(255, int(v * k)))
        layer = Image.new('RGBA', (S, S), col + (0,))
        layer.putalpha(al)
        c.alpha_composite(layer)
    edge = Image.new('L', (S, S), 0)
    ImageDraw.Draw(edge).ellipse([40, 40, S - 40, S - 40], fill=255)
    edge = edge.filter(ImageFilter.GaussianBlur(50))
    c.putalpha(Image.composite(c.split()[3], Image.new('L', (S, S), 0), edge))
    c.alpha_composite(t, (x, y))
    Image.frombytes('RGBA', c.size, c.tobytes()).save(os.path.join(OUT, 'icon-1024.png'))
    Image.frombytes('RGBA', c.size, c.tobytes()).resize((400, 400), Image.LANCZOS).save(os.path.join(OUT, 'icon.png'))
    return c


# ---------- banner ----------
def banner(ic):
    BW, BH = 1600, 400
    src = shot('c04.png')
    c = bg(src).resize((BW, BH * 2)).crop((0, BH // 2, BW, BH + BH // 2)).resize((BW, BH))
    # faint streaks on the right half
    st = src.resize((BW // 2 + 200, round((BW // 2 + 200) * src.height / src.width)), Image.LANCZOS)
    st = ImageEnhance.Brightness(st).enhance(0.55).convert('RGBA')
    m = Image.linear_gradient('L').rotate(90).resize(st.size)
    st.putalpha(m.point(lambda v: int(v * 0.8)))
    c.alpha_composite(st, (BW - st.width, (BH - st.height) // 2))
    c.alpha_composite(ic.resize((320, 320), Image.LANCZOS), (60, 40))
    brand_text(c, (400, 70), TITLE(124), radius=22, strength=200)
    ImageDraw.Draw(c).text((404, 236), 'Dimension travel at hyperspeed.', font=BODY(42), fill=TEXT + (255,))
    chips(c, 406, 310)
    save(c, 'banner.png')


# ---------- gallery ----------
def gallery():
    pages = [
        ('01-portal-dive.png', 'b22.png', 'PORTAL DIVE', ['Jump through a portal and ride', 'a hyperspace tunnel instead of', 'staring at a loading screen.'], None, 660, DIMS[1]),
        ('02-windup.png', 'b05.png', 'Windup', ['Stand in a Nether portal and', 'the view darkens, the stars', 'start to streak, the FOV creeps.'], (160, 0, 1440, 760), 640, PURPLE),
        ('03-the-jump.png', 'c03.png', 'The jump', ['The loading screen becomes', 'a tunnel. Streaks stretch,', 'the view punches out.'], None, 640, PURPLE),
        ('04-cruise.png', 'c04.png', 'Cruise', ['It flies for as long as', 'chunks take to load, then', 'shows where you are headed.'], None, 640, PURPLE),
        ('05-hyper-braking.png', 'a15.png', 'Hyper-braking', ['Streaks snap to dots, the', 'FOV springs back, a flash,', 'a jolt and a rumble.'], None, 640, DIMS[0]),
        ('06-arrival.png', 'b26.png', 'Arrival', ['The tunnel fades out over', 'the new dimension.'], (200, 0, 1400, 675), 640, DIMS[1]),
    ]
    for name, src, title, lines, crop, ww, glow in pages:
        img = shot(src)
        c = bg(img)
        y = text_block(c, 70, 190 if len(lines) < 3 else 175, title, lines, 64 if name.startswith('01') else 58)
        if name.startswith('01'):
            chips(c, 72, y + 16)
        win = window(img, ww, crop)
        place(c, win, W - win.width - 60, (H - win.height) // 2, glow)
        save(c, name)
    # every dimension: three tunnels side by side
    c = bg(shot('a07.png'))
    text_block(c, 70, 60, 'Every dimension', ['Tinted per dimension. Modded dimensions get', 'their own colour, or set one in the config.'], 58)
    names = ['The Overworld', 'The Nether', 'The End']
    for i, (src, col) in enumerate(zip(['a07.png', 'b22.png', 'c04.png'], DIMS)):
        img = shot(src).crop((200, 0, 1400, 900))
        win = window(img, 360)
        x = 70 + i * 392
        place(c, win, x, 340, col)
        ImageDraw.Draw(c).text((x + 4, 340 + win.height + 18), names[i], font=BODY(26), fill=col + (255,))
    save(c, '07-every-dimension.png')
    # modded dimensions: the same dive into other mods' worlds
    c = bg(shot('mod_undergarden.png'))
    text_block(c, 70, 60, 'Modded dimensions', ['Works with any dimension from any mod, and', 'takes over their own transition screens too.'], 58)
    names = ['The Aether', 'The Undergarden', 'The Otherside']
    cols = [(255, 214, 120), (236, 120, 220), (196, 140, 255)]
    for i, src in enumerate(['mod_aether.png', 'mod_undergarden.png', 'mod_otherside.png']):
        img = shot(src).crop((200, 120, 1400, 900))
        win = window(img, 360)
        x = 70 + i * 392
        place(c, win, x, 330)
        ImageDraw.Draw(c).text((x + 4, 330 + win.height + 18), names[i], font=BODY(26), fill=cols[i] + (255,))
    save(c, '08-modded-dimensions.png')
    combined()


def grid(c, items, cols, w, crop, x0, y0, gap_x, gap_y):
    for i, (src, label, col) in enumerate(items):
        win = window(shot(src), w, crop)
        x, y = x0 + (i % cols) * (w + gap_x), y0 + (i // cols) * (win.height + gap_y)
        place(c, win, x, y)
        ImageDraw.Draw(c).text((x + 4, y + win.height + 16), label, font=BODY(26), fill=col + (255,))


# ---------- combined images for the description / README ----------
def combined():
    c = bg(shot('c04.png'), H=920)
    glow_text(c, (70, 50), 'How a dive plays out', TITLE(58), radius=14, strength=160)
    grid(c, [('b05.png', '1  Windup', PURPLE_HI), ('c03.png', '2  The jump', PURPLE_HI),
             ('c04.png', '3  Cruise', PURPLE_HI), ('a15.png', '4  Hyper-braking', DIMS[0])],
         2, 540, (160, 0, 1440, 720), 70, 165, 60, 70)
    save(c, 'dive-steps.png')
    c = bg(shot('a07.png'), H=830)
    glow_text(c, (68, 50), 'Every dimension', TITLE(58), radius=14, strength=160)
    grid(c, [('a07.png', 'The Overworld', DIMS[0]), ('b22.png', 'The Nether', DIMS[1]), ('c04.png', 'The End', DIMS[2]),
             ('mod_aether.png', 'The Aether', (255, 214, 120)), ('mod_undergarden.png', 'The Undergarden', (236, 120, 220)),
             ('mod_otherside.png', 'The Otherside', (196, 140, 255))],
         3, 360, (200, 120, 1400, 900), 68, 165, 32, 72)
    save(c, 'dimensions.png')


ic = icon()
banner(ic)
gallery()
print('done')
