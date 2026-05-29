# -*- coding: utf-8 -*-
"""Genera imágenes promocionales de GasApp para Instagram:
- Post cuadrado 1080x1080
- Post vertical 1080x1350
- Stories 1080x1920
Reutiliza el logo y capturas reales de la app.
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SH = os.path.join(ROOT, "landing", "assets")
LOGO = os.path.join(SH, "logo.png")
OUT = os.path.join(ROOT, "instagram")
os.makedirs(OUT, exist_ok=True)

SCREENS = {
    "inicio": os.path.join(SH, "screen-inicio.png"),
    "mapa": os.path.join(SH, "screen-mapa.png"),
    "favoritas": os.path.join(SH, "screen-favoritas.png"),
}

BLUE   = (25, 118, 210); BLUE_D = (16, 78, 184); BLUE_L = (52, 150, 236)
SKY    = (231, 243, 255); SKY2 = (200, 226, 255)
NAVY   = (11, 27, 42); ORANGE = (245, 124, 0); WHITE = (255, 255, 255)
CREAM  = (255, 214, 153)

FB = "C:/Windows/Fonts/seguibl.ttf"
FH = "C:/Windows/Fonts/segoeuib.ttf"
FR = "C:/Windows/Fonts/segoeui.ttf"
FE = "C:/Windows/Fonts/seguiemj.ttf"
def font(p, s): return ImageFont.truetype(p, s)

def gradient(size, c1, c2, vertical=True):
    w, h = size
    base = Image.new("RGB", size, c1); top = Image.new("RGB", size, c2)
    mask = Image.new("L", size); md = mask.load()
    n = h if vertical else w
    for i in range(n):
        v = int(255 * i / max(1, n - 1))
        if vertical:
            for j in range(w): md[j, i] = v
        else:
            for j in range(h): md[i, j] = v
    base.paste(top, (0, 0), mask)
    return base.convert("RGBA")

def rmask(size, r):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0]-1, size[1]-1], radius=r, fill=255)
    return m

def round_img(img, r):
    img = img.convert("RGBA"); img.putalpha(rmask(img.size, r)); return img

def phone(screen, w):
    shot = Image.open(SCREENS[screen]).convert("RGBA")
    ih = int(w * shot.height / shot.width)
    shot = round_img(shot.resize((w, ih), Image.LANCZOS), int(w * 0.085))
    pad = max(8, int(w * 0.035)); fw, fh = w + 2*pad, ih + 2*pad
    frame = Image.new("RGBA", (fw, fh), (0,0,0,0))
    body = Image.new("RGBA", (fw, fh), (12, 20, 32, 255)); body.putalpha(rmask((fw, fh), int(w*0.085)+pad))
    frame.alpha_composite(body); frame.alpha_composite(shot, (pad, pad))
    mg = int(fw*0.22); canvas = Image.new("RGBA", (fw+2*mg, fh+2*mg), (0,0,0,0))
    sh = Image.new("RGBA", canvas.size, (0,0,0,0)); sd = Image.new("RGBA", (fw, fh), (0,0,0,140))
    sd.putalpha(Image.eval(rmask((fw, fh), int(w*0.085)+pad), lambda v: int(v*0.55)))
    sh.alpha_composite(sd, (mg, mg+int(mg*0.25))); sh = sh.filter(ImageFilter.GaussianBlur(mg*0.42))
    canvas.alpha_composite(sh); canvas.alpha_composite(frame, (mg, mg))
    return canvas

def white_drop():
    d = Image.open(LOGO).convert("RGBA"); r, g, b, a = d.split()
    m = ImageChops.darker(ImageChops.darker(r, g), b).point(lambda v: 0 if v < 60 else min(255, int((v-60)*255/175)))
    w = Image.new("RGBA", d.size, (255,255,255,0)); w.putalpha(ImageChops.multiply(m, a)); return w

def tw(d, s, f): return d.textbbox((0,0), s, font=f)[2]
def wrap(d, text, f, mw):
    out, cur = [], ""
    for w in text.split():
        t = (cur+" "+w).strip()
        if tw(d, t, f) <= mw: cur = t
        else: out.append(cur); cur = w
    if cur: out.append(cur)
    return out

def fit(d, lines, hi, lo, mw, path=FB):
    for s in range(hi, lo-1, -2):
        f = font(path, s)
        if all(tw(d, ln, f) <= mw for ln in lines): return f
    return font(path, lo)

def play_pill(d, x, y, w=560, h=104):
    d.rounded_rectangle([x, y, x+w, y+h], radius=h//2, fill=WHITE)
    # triángulo play
    cx, cy = x+58, y+h//2; s = 26
    d.polygon([(cx-s*0.5, cy-s), (cx-s*0.5, cy+s), (cx+s, cy)], fill=BLUE)
    f1 = font(FR, 28); f2 = font(FB, 40)
    d.text((x+108, y+22), "Disponible en", font=f1, fill=(90,108,128))
    d.text((x+108, y+50), "Google Play", font=f2, fill=NAVY)

def brand(d, img, x, y, size=88, color=WHITE, name=True):
    lg = Image.open(LOGO).convert("RGBA").resize((size, size), Image.LANCZOS)
    img.alpha_composite(lg, (x, y))
    if name:
        d.text((x+size+22, y+size//2), "GasApp", font=font(FB, int(size*0.86)), fill=color, anchor="lm")

def watermark_drop(img, size, xy, alpha=0.10):
    wd = white_drop().resize((size, size), Image.LANCZOS)
    fade = Image.new("RGBA", wd.size, (0,0,0,0))
    img.alpha_composite(Image.blend(fade, wd, alpha), xy)

def save(img, name):
    img.convert("RGB").save(os.path.join(OUT, name), quality=95)
    print("ok", name)

# ── 1. POST CUADRADO — Lanzamiento (1080x1080) ──────────────────────────────
def square_launch():
    W = H = 1080
    img = gradient((W, H), BLUE_L, BLUE_D); d = ImageDraw.Draw(img)
    watermark_drop(img, 620, (W-360, -120), 0.10)
    brand(d, img, 80, 80, 78)
    d.text((80, 235), "YA DISPONIBLE", font=font(FH, 34), fill=CREAM)
    hl = ["La gasolina", "más barata", "cerca de ti"]
    f = font(FB, 96); y = 290
    for ln in hl:
        d.text((80, y), ln, font=f, fill=WHITE); y += 104
    ph = phone("inicio", 360)
    img.alpha_composite(ph, (W-ph.size[0]+30, 250))
    play_pill(d, 80, 900)
    save(img, "ig-post-1-lanzamiento.png")

# ── 2. POST CUADRADO — Ahorro (1080x1080) ───────────────────────────────────
def square_ahorro():
    W = H = 1080
    img = gradient((W, H), SKY, SKY2); d = ImageDraw.Draw(img)
    LW = 560  # ancho del bloque de texto a la izquierda
    d.text((80, 120), "Cada repostaje cuenta", font=font(FH, 34), fill=BLUE)
    lines = wrap(d, "Ahorra en cada repostaje", font(FB, 92), LW)
    f = fit(d, lines, 92, 64, LW); y = 185
    for ln in lines:
        d.text((80, y), ln, font=f, fill=NAVY); y += f.size + 8
    for ln in wrap(d, "Compara precios en tiempo real con datos oficiales de toda España.", font(FR, 38), LW):
        d.text((80, y + 18), ln, font=font(FR, 38), fill=(70, 92, 116)); y += 50
    # móvil a la derecha
    ph = phone("mapa", 360)
    img.alpha_composite(ph, (W - ph.size[0] + 30, 230))
    # tarjetas de precio flotantes (abajo izquierda)
    for (px, py, t, c) in [(80, 820, "1,380€", BLUE), (80, 940, "-0,19€", ORANGE)]:
        d.rounded_rectangle([px, py, px+260, py+96], radius=24, fill=c)
        d.text((px+130, py+48), t, font=font(FB, 50), fill=WHITE, anchor="mm")
    save(img, "ig-post-2-ahorro.png")

# ── 3. POST CUADRADO — Funciones (1080x1080) ────────────────────────────────
def square_features():
    W = H = 1080
    img = gradient((W, H), BLUE_D, BLUE_L); d = ImageDraw.Draw(img)
    brand(d, img, 80, 80, 70)
    d.text((80, 210), "Todo en una app", font=font(FB, 76), fill=WHITE)
    feats = [("📍", "Mapa de precios"), ("🔔", "Alertas de bajada"),
             ("❤️", "Favoritas sync"), ("📊", "Gasto y ahorro")]
    y = 360
    ef = font(FE, 46)
    for emo, txt in feats:
        d.rounded_rectangle([80, y, 600, y+96], radius=24, fill=WHITE)
        el = Image.new("RGBA", (64,64), (0,0,0,0)); ImageDraw.Draw(el).text((32,32), emo, font=ef, embedded_color=True, anchor="mm")
        img.alpha_composite(el, (110, y+16))
        d.text((190, y+48), txt, font=font(FH, 40), fill=NAVY, anchor="lm")
        y += 120
    ph = phone("favoritas", 330)
    img.alpha_composite(ph, (W-ph.size[0]+20, 300))
    save(img, "ig-post-3-funciones.png")

# ── 4/5. POST VERTICAL (1080x1350) ──────────────────────────────────────────
def portrait(name, screen, kicker, title_lines, sub, mode):
    W, H = 1080, 1350
    if mode == "dark":
        img = gradient((W, H), BLUE_L, BLUE_D); tc, sc, kc = WHITE, (224,238,252), CREAM
    else:
        img = gradient((W, H), SKY, SKY2); tc, sc, kc = NAVY, (70,92,116), BLUE
    d = ImageDraw.Draw(img)
    d.text((W//2, 96), kicker, font=font(FH, 36), fill=kc, anchor="ma")
    f = fit(d, title_lines, 92, 60, W-160)
    y = 158
    for ln in title_lines:
        d.text((W//2, y), ln, font=f, fill=tc, anchor="ma"); y += f.size+10
    sf = font(FR, 38)
    for sl in wrap(d, sub, sf, W-200):
        d.text((W//2, y+14), sl, font=sf, fill=sc, anchor="ma"); y += 50
    # móvil debajo del texto (recortado por la parte inferior)
    ph = phone(screen, 440)
    top = max(470, y + 44)
    img.alpha_composite(ph, ((W-ph.size[0])//2, top))
    save(img, name)

# ── 6/7. STORIES (1080x1920) ────────────────────────────────────────────────
def story(name, screen, title_lines, sub, mode):
    W, H = 1080, 1920
    if mode == "dark":
        img = gradient((W, H), BLUE_L, BLUE_D); tc, sc, kc = WHITE, (224,238,252), CREAM
    else:
        img = gradient((W, H), SKY, SKY2); tc, sc, kc = NAVY, (70,92,116), BLUE
    d = ImageDraw.Draw(img)
    watermark_drop(img, 520, (W-300, 40), 0.10 if mode=="dark" else 0.06)
    brand(d, img, 80, 150, 78, color=tc)
    f = fit(d, title_lines, 104, 64, W-160)
    y = 360
    for ln in title_lines:
        d.text((80, y), ln, font=f, fill=tc); y += f.size+10
    d.text((80, y+16), sub, font=font(FR, 40), fill=sc)
    ph = phone(screen, 560)
    img.alpha_composite(ph, ((W-ph.size[0])//2, 760))
    play_pill(d, (W-560)//2, H-320, 560, 104)
    d.text((W//2, H-140), "gasapp.app", font=font(FH, 34), fill=tc, anchor="ma")
    save(img, name)

square_launch()
square_ahorro()
square_features()
portrait("ig-vertical-1-mapa.png", "mapa", "MAPA EN TIEMPO REAL",
         ["Todo el mapa", "de precios"], "Mira las gasolineras y su precio al instante", "dark")
portrait("ig-vertical-2-favoritas.png", "favoritas", "TUS FAVORITAS",
         ["Siempre", "contigo"], "Guárdalas y sincronízalas entre dispositivos", "light")
story("ig-story-1.png", "inicio", ["La gasolina", "más barata", "cerca de ti"],
      "Precios oficiales · Gratis", "dark")
story("ig-story-2.png", "mapa", ["Encuentra,", "compara", "y ahorra"],
      "Toda España · Datos oficiales", "dark")
print("\nTODO LISTO en", OUT)
