"""Gera os emblemas (icones simbolicos) dos kits do Minezin.

Estilo emblema/brasao: medalhao na cor do kit, moldura dourada ornamentada igual para todos e o simbolo do kit
(sem rosto nem cabelo). Desenhado em alta resolucao (768x1024) com formas vetoriais, gradientes, brilho e sombra,
depois reduzido com LANCZOS para 96x128 (antialiasing por supersampling).

Saida: src/main/resources/assets/minerwk/textures/gui/personagens/<kit>.png (96x128) e _generico.png.
O <kit> e o id do personagem no Mine-RWK. Reprodutivel: sem aleatoriedade.
Uso: py tools/build_retratos_personagens.py [pasta_folha_contato]
"""
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

S = 8                      # supersampling
W, H = 96 * S, 128 * S     # 768 x 1024
CX, CY, R = W // 2, 470, 300
RAIZ = Path(__file__).resolve().parent.parent
SAIDA = RAIZ / "src/main/resources/assets/minerwk/textures/gui/personagens"
FONTES = Path("C:/Windows/Fonts")

OURO_CLARO, OURO, OURO_ESC = (255, 236, 160), (222, 172, 70), (110, 70, 20)
BRONZE = (40, 22, 6)


def fonte(nome, tam):
    try:
        return ImageFont.truetype(str(FONTES / nome), tam)
    except OSError:
        return ImageFont.load_default()


def hexrgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def nova():
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


def mascara(fn):
    m = Image.new("L", (W, H), 0)
    fn(ImageDraw.Draw(m))
    return m


# ---------- gradientes ----------
YY, XX = np.mgrid[0:H, 0:W].astype(np.float32)


def grad_img(c1, c2, t):
    t = np.clip(t, 0, 1)[..., None]
    a, b = np.array(c1, np.float32), np.array(c2, np.float32)
    rgb = a + (b - a) * t
    return Image.fromarray(np.dstack([rgb, np.full((H, W, 1), 255, np.float32)]).astype(np.uint8), "RGBA")


def radial(c1, c2, cx, cy, r):
    return grad_img(c1, c2, np.hypot(XX - cx, YY - cy) / r)


def linear(c1, c2, x0, y0, x1, y1):
    dx, dy = x1 - x0, y1 - y0
    return grad_img(c1, c2, ((XX - x0) * dx + (YY - y0) * dy) / (dx * dx + dy * dy))


def pinta(base, m, fill):
    """Compoe fill (cor ou imagem RGBA) em base usando a mascara m."""
    if isinstance(fill, tuple):
        fill = Image.new("RGBA", (W, H), fill[:3] + (255,))
    camada = fill.copy()
    camada.putalpha(ImageChops.multiply(m, fill.getchannel("A")))
    base.alpha_composite(camada)


def brilho(base, m, cor, raio, forca=1.0):
    g = m.filter(ImageFilter.GaussianBlur(raio))
    if forca != 1:
        g = g.point(lambda v: min(255, int(v * forca)))
    pinta(base, g, cor)


def sombra(base, m, dx=10, dy=16, raio=14, alfa=170):
    g = m.filter(ImageFilter.GaussianBlur(raio)).point(lambda v: v * alfa // 255)
    pinta(base, ImageChops.offset(g, dx, dy), (0, 0, 0))


def contorno(m, w):
    return m.filter(ImageFilter.MaxFilter(w * 2 + 1))


def solido(base, m, fill, borda=(20, 12, 24), w=6, sombra_=True, glow=None):
    """Forma com brilho externo opcional, sombra, contorno escuro e preenchimento."""
    if glow:
        brilho(base, contorno(m, 10), glow, 28, 1.4)
    if sombra_:
        sombra(base, contorno(m, w))
    if borda:
        pinta(base, contorno(m, w), borda)
    pinta(base, m, fill)


def rot(pts, ang, cx, cy):
    a = math.radians(ang)
    ca, sa = math.cos(a), math.sin(a)
    return [(cx + (x - cx) * ca - (y - cy) * sa, cy + (x - cx) * sa + (y - cy) * ca) for x, y in pts]


def lamina(cx, cy, comp, larg, ang, ponta=0.18):
    """Poligono de lamina vertical (ponta para cima) girado em ang graus em torno de (cx, cy)."""
    t, b = cy - comp / 2, cy + comp / 2
    pts = [(cx, t), (cx + larg / 2, t + comp * ponta), (cx + larg / 2, b), (cx - larg / 2, b), (cx - larg / 2, t + comp * ponta)]
    return rot(pts, ang, cx, cy)


def circulo(d, cx, cy, r, **kw):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], **kw)


def polar(cx, cy, r, graus):
    a = math.radians(graus)
    return cx + r * math.cos(a), cy + r * math.sin(a)


# ---------- moldura comum ----------
def fundo(cor):
    img = nova()
    esc, base = mix(cor, (0, 0, 0), .82), mix(cor, (0, 0, 0), .45)
    card = mascara(lambda d: d.rounded_rectangle([0, 0, W - 1, H - 1], 64, fill=255))
    pinta(img, card, linear(base, esc, 0, 0, 0, H))
    pinta(img, card.point(lambda v: v * 150 // 255), radial(mix(cor, (255, 255, 255), .2), esc, CX, CY, 520))
    raios = mascara(lambda d: [d.polygon([(CX, CY), polar(CX, CY, 900, a - 4), polar(CX, CY, 900, a + 4)], fill=60)
                               for a in range(0, 360, 20)])
    pinta(img, ImageChops.multiply(raios, card).filter(ImageFilter.GaussianBlur(6)), mix(cor, (255, 255, 255), .5))
    return img, card


def medalhao(img, cor):
    claro, esc = mix(cor, (255, 255, 255), .35), mix(cor, (0, 0, 0), .7)
    orn = mascara(lambda d: [
        d.polygon([(CX, CY - R - 95), (CX + 52, CY - R + 10), (CX - 52, CY - R + 10)], fill=255),
        d.polygon([(CX, CY + R + 120), (CX + 60, CY + R - 10), (CX - 60, CY + R - 10)], fill=255),
        d.polygon([(CX - R - 70, CY), (CX - R + 10, CY - 60), (CX - R + 10, CY + 60)], fill=255),
        d.polygon([(CX + R + 70, CY), (CX + R - 10, CY - 60), (CX + R - 10, CY + 60)], fill=255),
        *[d.polygon(rot([(CX, CY - R - 48), (CX + 26, CY - R + 10), (CX - 26, CY - R + 10)], a, CX, CY), fill=255) for a in (45, 135, 225, 315)],
    ])
    tudo = ImageChops.lighter(orn, mascara(lambda d: circulo(d, CX, CY, R + 28, fill=255)))
    sombra(img, tudo, 0, 22, 24, 200)
    pinta(img, contorno(tudo, 6), BRONZE)
    pinta(img, tudo, linear(OURO_CLARO, OURO_ESC, CX - 200, CY - R, CX + 200, CY + R))
    gem = mascara(lambda d: [circulo(d, CX, CY - R - 20, 18, fill=255), circulo(d, CX, CY + R + 30, 20, fill=255),
                             circulo(d, CX - R - 20, CY, 14, fill=255), circulo(d, CX + R + 20, CY, 14, fill=255)])
    pinta(img, contorno(gem, 4), BRONZE)
    pinta(img, gem, radial(mix(cor, (255, 255, 255), .6), mix(cor, (0, 0, 0), .3), CX, CY, R + 40))
    pinta(img, mascara(lambda d: circulo(d, CX, CY, R + 6, fill=255)), BRONZE)
    pinta(img, mascara(lambda d: circulo(d, CX, CY, R, fill=255)), radial(claro, esc, CX, CY - 60, R * 1.15))
    marcas = mascara(lambda d: [circulo(d, *polar(CX, CY, R + 16, a), 5, fill=255) for a in range(0, 360, 15)])
    pinta(img, marcas, (90, 55, 12))
    return mascara(lambda d: circulo(d, CX, CY, R, fill=255))


def acabamento(img, cor, card, campo):
    vin = Image.fromarray((np.clip((np.hypot(XX - CX, YY - CY) - R * .7) / (R * .3), 0, 1) * 120).astype(np.uint8), "L")
    pinta(img, ImageChops.multiply(vin, campo), (0, 0, 0))
    vidro = mascara(lambda d: d.ellipse([CX - R * .8, CY - R * .95, CX + R * .8, CY - R * .05], fill=40))
    pinta(img, ImageChops.multiply(vidro.filter(ImageFilter.GaussianBlur(30)), campo), (255, 255, 255))
    borda = ImageChops.subtract(card, mascara(lambda d: d.rounded_rectangle([22, 22, W - 23, H - 23], 46, fill=255)))
    pinta(img, borda, linear(OURO_CLARO, OURO_ESC, 0, 0, W, H))
    fio = ImageChops.subtract(mascara(lambda d: d.rounded_rectangle([36, 36, W - 37, H - 37], 36, fill=255)),
                              mascara(lambda d: d.rounded_rectangle([44, 44, W - 45, H - 45], 30, fill=255)))
    pinta(img, fio, mix(OURO, cor, .3))
    cantos = mascara(lambda d: [d.polygon([(x, y - 34), (x + 34, y), (x, y + 34), (x - 34, y)], fill=255)
                                for x in (40, W - 41) for y in (40, H - 41)])
    pinta(img, contorno(cantos, 4), BRONZE)
    pinta(img, cantos, linear(OURO_CLARO, OURO, 0, 0, W, H))


# ---------- simbolos ----------
def gojo(img, campo):
    brilho(img, mascara(lambda d: circulo(d, CX - 95, CY, 150, fill=255)), (255, 40, 60), 50, 1.6)
    brilho(img, mascara(lambda d: circulo(d, CX + 95, CY, 150, fill=255)), (40, 120, 255), 50, 1.6)
    esfera = mascara(lambda d: circulo(d, CX, CY, 185, fill=255))
    brilho(img, esfera, (190, 80, 255), 40, 1.5)
    pinta(img, esfera, radial((250, 200, 255), (70, 0, 120), CX, CY - 30, 210))
    caixa = [CX - 185, CY - 185, CX + 185, CY + 185]
    pinta(img, mascara(lambda d: d.chord(caixa, 110, 250, fill=150)).filter(ImageFilter.GaussianBlur(20)), (255, 40, 70))
    pinta(img, mascara(lambda d: d.chord(caixa, -70, 70, fill=150)).filter(ImageFilter.GaussianBlur(20)), (40, 140, 255))
    olho = mascara(lambda d: d.polygon([(CX - 160 + i * 4, CY - 95 * math.sin(math.pi * i / 80)) for i in range(81)] +
                                       [(CX + 160 - i * 4, CY + 95 * math.sin(math.pi * i / 80)) for i in range(81)], fill=255))
    solido(img, olho, (240, 248, 255), (25, 0, 50), 9)
    pinta(img, mascara(lambda d: circulo(d, CX, CY, 78, fill=255)), radial((210, 250, 255), (0, 110, 230), CX, CY - 20, 95))
    pinta(img, mascara(lambda d: [d.line([*polar(CX, CY, 40, a), *polar(CX, CY, 74, a)], fill=160, width=5) for a in range(0, 360, 24)]), (230, 255, 255))
    pinta(img, mascara(lambda d: circulo(d, CX, CY, 30, fill=255)), (10, 20, 60))
    pinta(img, mascara(lambda d: circulo(d, CX - 24, CY - 28, 16, fill=255)), (255, 255, 255))


def cid(img, campo):
    lua = mascara(lambda d: circulo(d, CX, CY - 20, 200, fill=255))
    brilho(img, lua, (230, 210, 255), 50, 1.2)
    pinta(img, lua, radial((250, 245, 255), (170, 140, 220), CX - 50, CY - 90, 260))
    crateras = mascara(lambda d: [circulo(d, CX + x, CY - 20 + y, r, fill=70) for x, y, r in ((-70, -40, 34), (60, 50, 26), (40, -100, 18), (-30, 90, 22))])
    pinta(img, crateras, (120, 90, 170))
    for ang in (-38, 38):
        m = mascara(lambda d: d.polygon(lamina(CX, CY + 10, 560, 46, ang, .2), fill=255))
        solido(img, m, linear((240, 190, 255), (90, 20, 170), CX - 40, 0, CX + 40, 0), (20, 0, 40), 7, glow=(190, 90, 255))
        guarda = mascara(lambda d: d.polygon(rot([(CX - 60, CY + 210), (CX + 60, CY + 210), (CX + 60, CY + 236), (CX - 60, CY + 236)], ang, CX, CY + 10), fill=255))
        cabo = mascara(lambda d: d.polygon(rot([(CX - 18, CY + 236), (CX + 18, CY + 236), (CX + 18, CY + 320), (CX - 18, CY + 320)], ang, CX, CY + 10), fill=255))
        solido(img, cabo, (30, 20, 40), (10, 0, 20), 5)
        solido(img, guarda, linear(OURO_CLARO, OURO_ESC, 0, CY + 200, 0, CY + 250), (30, 10, 0), 5)
    f = fonte("impact.ttf", 74)
    txt = mascara(lambda d: d.text((CX, CY + R - 40), "I AM ATOMIC", font=f, anchor="mm", fill=255))
    brilho(img, txt, (200, 80, 255), 18, 2.5)
    pinta(img, contorno(txt, 5), (30, 0, 60))
    pinta(img, txt, (245, 225, 255))


def frieren(img, campo):
    circ = mascara(lambda d: [
        circulo(d, CX, CY, 250, outline=255, width=10), circulo(d, CX, CY, 215, outline=255, width=5),
        circulo(d, CX, CY, 120, outline=255, width=6),
        d.polygon([polar(CX, CY, 215, a) for a in range(-90, 270, 120)], outline=255, width=6),
        d.polygon([polar(CX, CY, 215, a) for a in range(-30, 330, 120)], outline=255, width=6),
        *[d.polygon(rot([(CX, CY - 245), (CX + 9, CY - 232), (CX, CY - 219), (CX - 9, CY - 232)], a, CX, CY), fill=255) for a in range(0, 360, 15)],
    ])
    brilho(img, circ, (140, 255, 220), 16, 1.8)
    pinta(img, circ, (220, 255, 240))
    ang = 30
    haste = mascara(lambda d: d.polygon(rot([(CX - 16, CY - 120), (CX + 16, CY - 120), (CX + 12, CY + 300), (CX - 12, CY + 300)], ang, CX, CY), fill=255))
    solido(img, haste, linear((255, 255, 255), (150, 120, 90), CX - 60, 0, CX + 60, 0), (40, 25, 10), 6)
    cab = rot([(CX, CY - 200)], ang, CX, CY)[0]
    meia = mascara(lambda d: [d.ellipse([cab[0] - 120, cab[1] - 120, cab[0] + 120, cab[1] + 120], fill=255),
                              d.ellipse([cab[0] - 88, cab[1] - 100, cab[0] + 88, cab[1] + 76], fill=0),
                              d.rectangle([0, cab[1] - 300, W, cab[1] - 20], fill=0)])
    meia = meia.rotate(-ang, center=cab, resample=Image.BICUBIC)
    asas = mascara(lambda d: d.polygon(rot([(CX - 120, CY - 220), (CX - 60, CY - 160), (CX - 20, CY - 130), (CX + 20, CY - 130),
                                            (CX + 60, CY - 160), (CX + 120, CY - 220), (CX + 70, CY - 120), (CX, CY - 100), (CX - 70, CY - 120)], ang, CX, CY), fill=255))
    ouro = ImageChops.lighter(meia, asas)
    solido(img, ouro, linear(OURO_CLARO, OURO_ESC, cab[0] - 120, cab[1] - 120, cab[0] + 120, cab[1] + 120), (50, 30, 5), 6)
    gema = mascara(lambda d: d.polygon([(cab[0], cab[1] - 70), (cab[0] + 44, cab[1] - 10), (cab[0], cab[1] + 50), (cab[0] - 44, cab[1] - 10)], fill=255))
    gema = gema.rotate(-ang, center=cab, resample=Image.BICUBIC)
    solido(img, gema, radial((255, 190, 190), (150, 0, 20), cab[0] - 15, cab[1] - 30, 90), (60, 0, 10), 6, glow=(255, 60, 60))
    pinta(img, mascara(lambda d: circulo(d, cab[0] - 12, cab[1] - 26, 10, fill=255)), (255, 255, 255))


def trevo(d, cx, cy, r, fill):
    for i in range(5):
        a = -90 + i * 72
        px, py = polar(cx, cy, r * .55, a)
        circulo(d, *polar(px, py, r * .18, a + 69), r * .38, fill=fill)
        circulo(d, *polar(px, py, r * .18, a - 69), r * .38, fill=fill)
        d.polygon([(cx, cy), polar(px, py, r * .45, a + 74), polar(px, py, r * .45, a - 74)], fill=fill)


def asta(img, campo):
    esp = mascara(lambda d: d.polygon(lamina(CX, CY - 10, 680, 150, -38, .12), fill=255))
    solido(img, esp, linear((90, 90, 100), (10, 10, 14), CX - 60, 0, CX + 60, 0), (0, 0, 0), 7, glow=(255, 60, 60))
    pinta(img, mascara(lambda d: d.line(rot([(CX + 60, CY - 260), (CX + 60, CY + 320)], -38, CX, CY - 10), fill=255, width=12)), (200, 200, 215))
    livro = mascara(lambda d: d.rounded_rectangle([CX - 150, CY - 170, CX + 150, CY + 190], 26, fill=255))
    solido(img, livro, linear((50, 50, 58), (8, 8, 12), CX - 150, CY - 170, CX + 150, CY + 190), (0, 0, 0), 8)
    pinta(img, mascara(lambda d: d.rectangle([CX - 150, CY - 170, CX - 118, CY + 190], fill=255)), (25, 25, 30))
    moldura = mascara(lambda d: [d.rounded_rectangle([CX - 110, CY - 140, CX + 130, CY + 160], 14, outline=255, width=8),
                                 *[d.polygon([(x - 26, y), (x, y - 26), (x + 26, y), (x, y + 26)], fill=255) for x in (CX - 110, CX + 130) for y in (CY - 140, CY + 160)]])
    pinta(img, moldura, linear(OURO_CLARO, OURO_ESC, 0, CY - 170, 0, CY + 190))
    tr = mascara(lambda d: trevo(d, CX + 10, CY + 10, 128, 255))
    brilho(img, tr, (60, 255, 90), 18, 1.6)
    pinta(img, contorno(tr, 7), OURO)
    pinta(img, contorno(tr, 3), (0, 20, 0))
    pinta(img, tr, radial((60, 170, 70), (0, 30, 8), CX + 10, CY - 20, 150))
    veias = mascara(lambda d: [d.line([(CX + 10, CY + 10), polar(CX + 10, CY + 10, 125, -90 + i * 72 + 36)], fill=255, width=12) for i in range(5)])
    pinta(img, veias, (5, 15, 5))
    pinta(img, mascara(lambda d: circulo(d, CX + 10, CY + 10, 16, fill=255)), OURO)


def ichigo(img, campo):
    esp = mascara(lambda d: d.polygon(lamina(CX, CY - 10, 620, 120, 40, .1), fill=255))
    solido(img, esp, linear((25, 25, 30), (70, 70, 80), CX - 60, 0, CX + 60, 0), (0, 0, 0), 7, glow=(255, 60, 60))
    pinta(img, mascara(lambda d: d.line(rot([(CX - 52, CY - 240), (CX - 52, CY + 300)], 40, CX, CY - 10), fill=255, width=14)), (210, 215, 230))
    cabo = mascara(lambda d: d.polygon(rot([(CX - 20, CY + 300), (CX + 20, CY + 300), (CX + 20, CY + 400), (CX - 20, CY + 400)], 40, CX, CY - 10), fill=255))
    solido(img, cabo, (240, 240, 240), (0, 0, 0), 5)
    mask = mascara(lambda d: [d.ellipse([CX - 150, CY - 200, CX + 150, CY + 130], fill=255),
                              d.polygon([(CX - 130, CY + 20), (CX + 130, CY + 20), (CX + 90, CY + 200), (CX - 90, CY + 200)], fill=255)])
    solido(img, mask, radial((255, 255, 255), (185, 185, 200), CX - 30, CY - 80, 330), (10, 10, 20), 8)
    listras = mascara(lambda d: [d.polygon([(CX + s * 60, CY - 200), (CX + s * 100, CY - 196), (CX + s * 80, CY - 40), (CX + s * 64, CY - 40)], fill=255) for s in (-1, 1)] +
                      [d.polygon([(CX + s * 64, CY + 10), (CX + s * 84, CY + 10), (CX + s * 108, CY + 170), (CX + s * 84, CY + 180)], fill=255) for s in (-1, 1)])
    pinta(img, ImageChops.multiply(listras, mask), linear((240, 40, 40), (140, 0, 0), 0, CY - 200, 0, CY + 200))
    olhos = mascara(lambda d: [d.polygon([(CX + s * 20, CY - 40), (CX + s * 120, CY - 70), (CX + s * 110, CY - 10), (CX + s * 40, CY + 6)], fill=255) for s in (-1, 1)])
    pinta(img, olhos, (5, 5, 10))
    pinta(img, mascara(lambda d: [circulo(d, CX + s * 78, CY - 32, 12, fill=255) for s in (-1, 1)]), (255, 210, 60))
    dentes = mascara(lambda d: [d.line([CX - 80, CY + 120, CX + 80, CY + 120], fill=255, width=8)] +
                     [d.line([CX + x, CY + 92, CX + x, CY + 150], fill=255, width=6) for x in range(-64, 80, 26)])
    pinta(img, ImageChops.multiply(dentes, mask), (40, 40, 50))


def jinwoo(img, campo):
    chama = mascara(lambda d: d.polygon([(CX + 210 * math.sin(t) * (1 - abs(math.cos(t)) * .2) + 30 * math.sin(t * 7),
                                          CY + 110 - 260 * (1 - math.cos(t)) / 2 - 70 * math.sin(t * 3) ** 2) for t in np.linspace(-math.pi, math.pi, 120)], fill=255))
    brilho(img, chama, (40, 120, 255), 40, 1.6)
    pinta(img, chama.filter(ImageFilter.GaussianBlur(10)), radial((220, 245, 255), (20, 60, 220), CX, CY + 60, 280))
    for ang in (-40, 40):
        m = mascara(lambda d: d.polygon(lamina(CX, CY + 40, 470, 64, ang, .3), fill=255))
        solido(img, m, linear((255, 170, 150), (120, 0, 10), 0, CY - 200, 0, CY + 260), (30, 0, 0), 7, glow=(255, 60, 40))
        g = mascara(lambda d: d.polygon(rot([(CX - 70, CY + 150), (CX, CY + 128), (CX + 70, CY + 150), (CX, CY + 176)], ang, CX, CY + 40), fill=255))
        solido(img, g, linear(OURO_CLARO, OURO_ESC, 0, CY + 120, 0, CY + 180), (40, 20, 0), 5)
        c = mascara(lambda d: d.polygon(rot([(CX - 16, CY + 170), (CX + 16, CY + 170), (CX + 16, CY + 270), (CX - 16, CY + 270)], ang, CX, CY + 40), fill=255))
        solido(img, c, (35, 20, 25), (10, 0, 0), 5)
    olhos = mascara(lambda d: [d.polygon([(CX + s * 24, CY - 175), (CX + s * 170, CY - 225), (CX + s * 140, CY - 157), (CX + s * 60, CY - 147)], fill=255) for s in (-1, 1)])
    brilho(img, olhos, (80, 200, 255), 26, 2.6)
    pinta(img, contorno(olhos, 5), (0, 20, 70))
    pinta(img, olhos, (230, 250, 255))


def tanjiro(img, campo):
    xadrez = Image.fromarray(np.where(((XX // 75).astype(int) + (YY // 75).astype(int)) % 2 == 0, 255, 0).astype(np.uint8), "L")
    pinta(img, ImageChops.multiply(xadrez, campo), (10, 90, 60))
    pinta(img, ImageChops.multiply(ImageChops.invert(xadrez), campo), (10, 12, 14))
    fogo = mascara(lambda d: [d.arc([CX - r, CY - r, CX + r, CY + r], a, a + 150, fill=255, width=w)
                              for r, a, w in ((260, 200, 40), (230, 20, 30), (200, 140, 22), (250, 330, 18))])
    brilho(img, fogo, (255, 140, 30), 22, 1.8)
    pinta(img, fogo, linear((255, 230, 120), (230, 60, 10), 0, CY - 260, 0, CY + 260))
    card = mascara(lambda d: d.rounded_rectangle([CX - 120, CY - 180, CX + 120, CY + 180], 22, fill=255))
    solido(img, card, linear((255, 250, 235), (220, 205, 180), 0, CY - 180, 0, CY + 180), (40, 20, 10), 8)
    raios = mascara(lambda d: [d.polygon([(CX, CY - 20), polar(CX, CY - 20, 300, a - 7), polar(CX, CY - 20, 300, a + 7)], fill=255) for a in range(0, 360, 30)])
    pinta(img, ImageChops.multiply(raios, mascara(lambda d: d.rounded_rectangle([CX - 104, CY - 164, CX + 104, CY + 164], 14, fill=255))), (240, 140, 60))
    sol = mascara(lambda d: circulo(d, CX, CY - 20, 92, fill=255))
    brilho(img, sol, (255, 40, 20), 18, 1.4)
    pinta(img, contorno(sol, 4), (90, 0, 0))
    pinta(img, sol, radial((255, 120, 90), (190, 0, 10), CX - 25, CY - 50, 120))
    pinta(img, mascara(lambda d: d.rectangle([CX - 104, CY + 110, CX + 104, CY + 164], fill=255)), (180, 20, 20))
    pinta(img, mascara(lambda d: d.ellipse([CX - 30, CY - 250, CX + 30, CY - 170], outline=255, width=10)), OURO)


def killua(img, campo):
    pts = [(CX + 40, CY - 270), (CX - 120, CY + 10), (CX - 10, CY + 10), (CX - 70, CY + 280), (CX + 140, CY - 60), (CX + 20, CY - 60), (CX + 110, CY - 270)]
    raio = mascara(lambda d: d.polygon(pts, fill=255))
    brilho(img, raio, (80, 180, 255), 46, 2.0)
    brilho(img, raio, (200, 240, 255), 14, 1.5)
    pinta(img, contorno(raio, 6), (0, 40, 120))
    pinta(img, raio, linear((255, 255, 255), (110, 200, 255), CX, CY - 270, CX, CY + 280))
    fa = mascara(lambda d: [d.line([(CX - 150, CY - 180), (CX - 210, CY - 100), (CX - 170, CY - 40)], fill=255, width=10),
                            d.line([(CX + 170, CY + 60), (CX + 230, CY + 130), (CX + 190, CY + 190)], fill=255, width=10)])
    brilho(img, fa, (120, 200, 255), 10, 2)
    pinta(img, fa, (230, 250, 255))
    pinta(img, mascara(lambda d: d.line([(CX + 90, CY - 300), (CX + 160, CY + 110)], fill=255, width=6)), (230, 230, 240))
    yo = mascara(lambda d: circulo(d, CX + 165, CY + 160, 80, fill=255))
    solido(img, yo, radial((250, 250, 255), (120, 130, 160), CX + 140, CY + 130, 120), (20, 20, 40), 7)
    pinta(img, mascara(lambda d: circulo(d, CX + 165, CY + 160, 52, outline=255, width=8)), (60, 70, 100))
    pinta(img, mascara(lambda d: circulo(d, CX + 165, CY + 160, 16, fill=255)), (40, 40, 60))


def subaru(img, campo):
    esp = mascara(lambda d: [d.line([(CX + (12 + t * 1.15) * math.cos(t * .055 + k * 2.09), CY + (12 + t * 1.15) * math.sin(t * .055 + k * 2.09))
                                     for t in range(0, 230)], fill=255, width=22, joint="curve") for k in range(3)])
    brilho(img, esp, (180, 60, 255), 24, 1.8)
    pinta(img, esp, radial((240, 200, 255), (110, 20, 200), CX, CY, 260))
    rel = mascara(lambda d: circulo(d, CX, CY, 262, outline=255, width=16))
    solido(img, rel, linear(OURO_CLARO, OURO_ESC, 0, CY - 260, 0, CY + 260), (30, 10, 40), 5)
    tic = mascara(lambda d: [d.polygon(rot([(CX - 9, CY - 240), (CX + 9, CY - 240), (CX + 5, CY - 200), (CX - 5, CY - 200)], a, CX, CY), fill=255) for a in range(0, 360, 30)])
    pinta(img, tic, (255, 235, 180))
    pont = mascara(lambda d: [d.polygon([(CX - 10, CY), (CX, CY - 190), (CX + 10, CY)], fill=255),
                              d.polygon(rot([(CX - 14, CY), (CX, CY - 120), (CX + 14, CY)], 130, CX, CY), fill=255)])
    solido(img, pont, (20, 0, 30), (240, 220, 255), 4, sombra_=False)

    def desenha_maos(d):
        for s in (-1, 1):
            bx = CX + s * 150
            d.polygon([(bx - 70, CY + 330), (bx + 70, CY + 330), (bx + 50, CY + 140), (bx - 50, CY + 140)], fill=255)
            for i, (dx, alt) in enumerate(((-45, 120), (-15, 160), (15, 150), (45, 110))):
                topo = (bx + dx * 1.1 - s * 30 * (i / 3), CY + 140 - alt)
                d.line([(bx + dx, CY + 160), topo], fill=255, width=22)
                circulo(d, topo[0], topo[1], 11, fill=255)
    maos = mascara(desenha_maos).filter(ImageFilter.GaussianBlur(3))
    brilho(img, maos, (150, 40, 220), 20, 1.4)
    pinta(img, maos, linear((40, 0, 60), (5, 0, 10), 0, CY, 0, CY + 330))


def guts(img, campo):
    ang = -32
    esp = mascara(lambda d: d.polygon(rot([(CX - 95, CY - 300), (CX + 95, CY - 320), (CX + 95, CY + 220), (CX - 95, CY + 220)], ang, CX, CY), fill=255))
    solido(img, esp, linear((45, 45, 50), (130, 130, 140), CX - 120, CY - 60, CX + 120, CY + 60), (5, 5, 8), 8)
    pinta(img, mascara(lambda d: d.polygon(rot([(CX - 95, CY - 300), (CX - 70, CY - 300), (CX - 70, CY + 220), (CX - 95, CY + 220)], ang, CX, CY), fill=255)), (185, 185, 195))
    solido(img, mascara(lambda d: d.polygon(rot([(CX - 160, CY + 215), (CX + 160, CY + 215), (CX + 160, CY + 250), (CX - 160, CY + 250)], ang, CX, CY), fill=255)), (50, 45, 45), (0, 0, 0), 6)
    solido(img, mascara(lambda d: d.polygon(rot([(CX - 24, CY + 250), (CX + 24, CY + 250), (CX + 24, CY + 340), (CX - 24, CY + 340)], ang, CX, CY), fill=255)), (60, 30, 20), (0, 0, 0), 5)
    sombra(img, campo, 0, 0, 1, 90)
    my = CY - 70

    def desenha_marca(d):
        d.line([(CX, my - 150), (CX, my + 150)], fill=255, width=34)
        d.line([(CX - 40, my - 120), (CX - 120, my - 160)], fill=255, width=30)
        d.line([(CX + 40, my - 120), (CX + 120, my - 160)], fill=255, width=30)
        d.line([(CX - 18, my - 40), (CX - 130, my + 30), (CX - 30, my + 110)], fill=255, width=30, joint="curve")
        d.line([(CX + 18, my - 40), (CX + 130, my + 30), (CX + 30, my + 110)], fill=255, width=30, joint="curve")
        for x, y, c in ((CX, my + 150, 70), (CX - 30, my + 110, 50), (CX + 120, my - 160, 40), (CX - 120, my - 160, 55)):
            d.line([(x, y), (x, y + c)], fill=255, width=12)
            circulo(d, x, y + c, 10, fill=255)
    marca = mascara(desenha_marca)
    brilho(img, marca, (255, 0, 0), 30, 2.2)
    sombra(img, contorno(marca, 8))
    pinta(img, contorno(marca, 8), (25, 0, 0))
    pinta(img, marca, linear((255, 70, 60), (140, 0, 0), 0, my - 160, 0, my + 220))


def douma(img, campo):
    """Golden war fan opened in a half circle, crimson lotus in the middle, ice-crystal edge."""
    import math
    by = CY + 170
    def leque(d):
        pts = [(CX, by)] + [(CX + 290 * math.cos(math.radians(a)), by - 290 * math.sin(math.radians(a))) for a in range(15, 166, 5)]
        d.polygon(pts, fill=255)
    m = mascara(leque)
    brilho(img, m, (160, 220, 255), 26, 1.6)
    solido(img, m, linear((255, 225, 140), (160, 110, 30), 0, by - 290, 0, by), (40, 25, 0), 8)
    def varetas(d):
        for a in range(15, 166, 15):
            d.line([(CX, by), (CX + 285 * math.cos(math.radians(a)), by - 285 * math.sin(math.radians(a)))], fill=255, width=8)
    pinta(img, mascara(varetas), (120, 80, 20))
    def borda(d):
        for a in range(15, 166, 10):
            x, y = CX + 300 * math.cos(math.radians(a)), by - 300 * math.sin(math.radians(a))
            circulo(d, x, y, 18, fill=255)
    bm = mascara(borda)
    brilho(img, bm, (170, 230, 255), 18, 1.5)
    pinta(img, bm, (215, 245, 255))
    def lotus(d):
        for a in (-50, -25, 0, 25, 50):
            r = rot([(CX - 26, by - 120), (CX, by - 230), (CX + 26, by - 120), (CX, by - 95)], a, CX, by - 110)
            d.polygon(r, fill=255)
    lm = mascara(lotus)
    sombra(img, contorno(lm, 6))
    pinta(img, lm, linear((235, 100, 130), (150, 20, 50), 0, by - 230, 0, by - 95))


def rudeus(img, campo):
    """Rudeus Greyrat: circulo magico azul atras do Aqua Heartia (cristal ultramarino preso em galhos de madeira escura)."""
    gy = CY - 70  # centro do cristal

    def runas(d):
        circulo(d, CX, CY, 262, outline=255, width=9)
        circulo(d, CX, CY, 226, outline=255, width=5)
        circulo(d, CX, CY, 150, outline=255, width=4)
        for i in range(24):  # glifos entre os dois aneis externos
            a = i * 15 + 7.5
            if i % 3 == 0:
                d.polygon(rot([(CX, CY - 252), (CX + 9, CY - 244), (CX, CY - 236), (CX - 9, CY - 244)], a, CX, CY), fill=255)
            else:
                d.line([polar(CX, CY, 234, a - 90 - 3), polar(CX, CY, 254, a - 90 - 3)], fill=255, width=5)
                d.line([polar(CX, CY, 244, a - 90 - 3), polar(CX, CY, 244, a - 90 + 3)], fill=255, width=5)
    circ = mascara(runas)
    estrela = mascara(lambda d: [d.polygon([polar(CX, CY, 226, a) for a in range(-90 + 60 * k, 270 + 60 * k, 120)], outline=150, width=5)
                                 for k in range(2)])
    brilho(img, estrela, (60, 140, 255), 12, 1.2)
    pinta(img, estrela, (170, 210, 255))
    brilho(img, circ, (60, 140, 255), 18, 1.9)
    pinta(img, circ, (200, 232, 255))

    def galho(d, pts, w0, w1):
        """Galho afilado por uma curva de Bezier (pontos de controle pts), de largura w0 ate w1."""
        n = 60
        for i in range(n + 1):
            t = i / n
            m = len(pts) - 1
            x = sum(math.comb(m, k) * (1 - t) ** (m - k) * t ** k * pts[k][0] for k in range(m + 1))
            y = sum(math.comb(m, k) * (1 - t) ** (m - k) * t ** k * pts[k][1] for k in range(m + 1))
            circulo(d, x, y, (w0 + (w1 - w0) * t) / 2, fill=255)
    base = (CX, gy + 120)
    tras = mascara(lambda d: [galho(d, [base, (CX + 30, gy + 40), (CX + 130, gy - 10), (CX + 70, gy - 150)], 34, 12),
                              galho(d, [(CX + 6, gy + 100), (CX - 40, gy + 10), (CX - 30, gy - 120), (CX + 10, gy - 190)], 24, 9)])
    madeira = linear((120, 78, 42), (34, 20, 10), CX - 120, gy - 160, CX + 120, gy + 160)
    solido(img, tras, madeira, (14, 8, 4), 6)
    haste = mascara(lambda d: [d.polygon([(CX - 26, gy + 110), (CX + 26, gy + 110), (CX + 18, CY + 285), (CX - 18, CY + 285)], fill=255),
                               *[d.ellipse([CX - 30, y - 8, CX + 30, y + 8], fill=255) for y in (gy + 170, gy + 230)]])
    solido(img, haste, linear((105, 66, 34), (40, 24, 12), CX - 22, 0, CX + 22, 0), (14, 8, 4), 6)
    # o cristal (bipiramide hexagonal alongada) com as facetas claras a esquerda
    topo, alto, baixo, fundo_ = (CX, gy - 165), gy - 55, gy + 55, (CX, gy + 135)
    gema = mascara(lambda d: d.polygon([topo, (CX + 78, alto), (CX + 68, baixo), fundo_, (CX - 68, baixo), (CX - 78, alto)], fill=255))
    solido(img, gema, radial((96, 140, 255), (16, 30, 125), CX - 25, gy - 70, 190), (8, 14, 50), 7, glow=(60, 120, 255))
    facetas = [([topo, (CX - 78, alto), (CX - 26, alto + 8)], (130, 175, 255), 130),
               ([topo, (CX - 26, alto + 8), (CX + 22, alto + 6)], (70, 120, 240), 90),
               ([(CX - 78, alto), (CX - 68, baixo), (CX - 24, baixo + 6), (CX - 26, alto + 8)], (60, 105, 235), 110),
               ([(CX + 22, alto + 6), (CX + 78, alto), (CX + 68, baixo), (CX + 20, baixo + 8)], (10, 25, 90), 140),
               ([(CX - 24, baixo + 6), (CX + 20, baixo + 8), fundo_], (20, 45, 150), 120)]
    for pts, cor, a in facetas:
        pinta(img, mascara(lambda d, pts=pts, a=a: d.polygon(pts, fill=a)), cor)
    pinta(img, mascara(lambda d: [d.line([topo, (CX - 26, alto + 8), (CX - 24, baixo + 6), fundo_], fill=120, width=4),
                                  d.line([topo, (CX + 22, alto + 6), (CX + 20, baixo + 8), fundo_], fill=90, width=4),
                                  d.line([(CX - 78, alto), (CX - 26, alto + 8), (CX + 22, alto + 6), (CX + 78, alto)], fill=110, width=4)]),
          (225, 240, 255))
    pinta(img, mascara(lambda d: d.polygon([(CX - 48, gy - 70), (CX - 36, gy - 112), (CX - 30, gy - 66), (CX - 40, gy - 10)], fill=170)), (235, 245, 255))
    # galhos da frente abracando o cristal
    frente = mascara(lambda d: [galho(d, [base, (CX - 40, gy + 60), (CX - 140, gy), (CX - 70, gy - 140)], 34, 12),
                                galho(d, [(CX - 4, gy + 112), (CX + 60, gy + 70), (CX + 50, gy - 10)], 22, 8)])
    solido(img, frente, madeira, (14, 8, 4), 6)
    pinta(img, mascara(lambda d: [circulo(d, CX - 70, gy - 140, 7, fill=255), circulo(d, CX + 70, gy - 150, 7, fill=255),
                                  circulo(d, CX + 10, gy - 190, 6, fill=255)]), (200, 230, 255))


def dio(img, campo):
    # relogio dourado parado (Za Warudo) + leque de 3 facas prateadas
    rel = mascara(lambda d: circulo(d, CX, CY, 250, outline=255, width=18))
    brilho(img, rel, (255, 210, 80), 26, 1.6)
    solido(img, rel, linear(OURO_CLARO, OURO_ESC, 0, CY - 250, 0, CY + 250), (30, 20, 0), 5)
    tic = mascara(lambda d: [d.polygon(rot([(CX - 8, CY - 232), (CX + 8, CY - 232), (CX + 4, CY - 196), (CX - 4, CY - 196)], a, CX, CY), fill=255) for a in range(0, 360, 30)])
    pinta(img, tic, (255, 230, 150))

    def facas(d):
        for a in (-28, 0, 28):
            d.polygon(rot([(CX - 22, CY + 40), (CX, CY - 250), (CX + 22, CY + 40)], a, CX, CY + 200), fill=255)
    lam = mascara(facas)
    solido(img, lam, linear((250, 250, 255), (140, 145, 160), CX - 100, CY - 250, CX + 100, CY + 40), (20, 20, 30), 6)
    cabo = mascara(lambda d: [d.polygon(rot([(CX - 18, CY + 40), (CX + 18, CY + 40), (CX + 14, CY + 150), (CX - 14, CY + 150)], a, CX, CY + 200), fill=255) for a in (-28, 0, 28)])
    solido(img, cabo, (90, 50, 25), (10, 5, 0), 5)


def homura(img, campo):
    # escudo-ampulheta (buckler) roxo com engrenagem
    esc = mascara(lambda d: circulo(d, CX, CY, 240, fill=255))
    solido(img, esc, radial((120, 90, 170), (30, 20, 50), CX - 60, CY - 60, 300), (10, 5, 20), 8)
    eng = mascara(lambda d: [d.polygon(rot([(CX - 22, CY - 250), (CX + 22, CY - 250), (CX + 16, CY - 215), (CX - 16, CY - 215)], a, CX, CY), fill=255) for a in range(0, 360, 30)])
    pinta(img, eng, (200, 180, 230))
    amp = mascara(lambda d: [d.polygon([(CX - 110, CY - 160), (CX + 110, CY - 160), (CX + 12, CY), (CX + 110, CY + 160), (CX - 110, CY + 160), (CX - 12, CY)], fill=255)])
    brilho(img, amp, (190, 150, 255), 22, 1.6)
    solido(img, amp, linear((240, 230, 255), (150, 110, 220), 0, CY - 160, 0, CY + 160), (20, 10, 40), 6)
    areia = mascara(lambda d: [d.polygon([(CX - 70, CY + 150), (CX + 70, CY + 150), (CX, CY + 60)], fill=255), d.polygon([(CX - 50, CY - 120), (CX + 50, CY - 120), (CX, CY - 40)], fill=255)])
    pinta(img, areia, (120, 70, 190))


def generico(img, campo):
    f = fonte("georgiab.ttf", 440)
    q = mascara(lambda d: d.text((CX, CY + 10), "?", font=f, anchor="mm", fill=255))
    brilho(img, q, (220, 230, 255), 26, 1.5)
    solido(img, q, linear((255, 255, 255), (130, 140, 165), 0, CY - 200, 0, CY + 200), (20, 25, 40), 8)


KITS = {
    "gojo": (0x7A3CFF, gojo), "cid": (0x8A3FD0, cid), "frieren": (0x3FAE9A, frieren),
    "asta": (0xB02030, asta), "ichigo": (0xC03030, ichigo), "jinwoo": (0x2E3FAF, jinwoo),
    "tanjiro": (0x1E8A5A, tanjiro), "killua": (0x3A86D0, killua), "subaru": (0x6A2DB0, subaru),
    "guts": (0x7A1010, guts), "douma": (0x6FB8E0, douma), "rudeus": (0x1F3C88, rudeus), "dio": (0xB8860B, dio), "homura": (0x5B3C99, homura), "_generico": (0x5F6E88, generico),
}


def emblema(cor, fn):
    cor = hexrgb(cor)
    img, card = fundo(cor)
    campo = medalhao(img, cor)
    simb = nova()
    fn(simb, campo)
    img.alpha_composite(simb)
    acabamento(img, cor, card, campo)
    return img.resize((96, 128), Image.LANCZOS).filter(ImageFilter.UnsharpMask(1, 60, 2))


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    imgs = {}
    for nome, (cor, fn) in KITS.items():
        imgs[nome] = emblema(cor, fn)
        imgs[nome].save(SAIDA / f"{nome}.png", optimize=True)
    if len(sys.argv) > 1:
        pasta = Path(sys.argv[1])
        pasta.mkdir(parents=True, exist_ok=True)
        cols = 6
        lin = (len(imgs) + cols - 1) // cols
        # cada celula: emblema 2x (NEAREST) + tamanho real do card ao lado
        folha = Image.new("RGBA", (cols * 310 + 10, lin * 290 + 10), (20, 20, 28, 255))
        d = ImageDraw.Draw(folha)
        for i, (nome, img) in enumerate(imgs.items()):
            x, y = 10 + (i % cols) * 310, 10 + (i // cols) * 290
            grande = img.resize((192, 256), Image.NEAREST)
            folha.alpha_composite(grande, (x, y))
            folha.alpha_composite(img, (x + 200, y))
            d.text((x, y + 262), nome, fill=(230, 230, 240))
        folha.convert("RGB").save(pasta / "folha.png")
        folha.convert("RGB").save(pasta / "folha.jpg", quality=88)


if __name__ == "__main__":
    main()
