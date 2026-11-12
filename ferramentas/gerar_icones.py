"""Ícones da HUD e da árvore (32x32) e fundos da árvore (256x256, repetíveis), para o spec
[[2026-11-12 UI dos personagens]]. Os PNGs não se editam à mão: muda aqui e roda de novo.

Cada ícone: o fundo escuro com um brilho no meio, nas cores do tema do personagem, e o símbolo do golpe por cima,
desenhado em 128x128 e reduzido para 32x32 com o alfa cortado (pixel art), com um contorno escuro de 1 px.
O IconesTest confere que todo golpe, recurso e Despertar dos personagens jogáveis tem o seu ícone.

Uso: python ferramentas/gerar_icones.py [pasta de saída de uma folha de contato]
"""
import math
import random
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

RAIZ = Path(__file__).resolve().parent.parent
GUI = RAIZ / 'src/main/resources/assets/minerwk/textures/gui'
G = 128  # tamanho do desenho antes de reduzir
C = G // 2


def rgb(argb):
    return ((argb >> 16) & 255, (argb >> 8) & 255, argb & 255)


# As cores dos temas (as mesmas de core/hud/Temas.java).
TEMAS = {
    'gojo': (0xFF7FD8FF, 0xFFFFFFFF, 0xFF101828, 0xFFB48CFF),
    'ichigo': (0xFFE03030, 0xFF1A1A1A, 0xFF100808, 0xFF802020),
    'cid': (0xFF9A6BFF, 0xFFE8B84A, 0xFF0C0818, 0xFF5A3A9A),
}
AZUL, BRANCO, VERMELHO, ROXO = (90, 170, 255), (245, 245, 255), (230, 50, 50), (160, 90, 255)
PRETO, OURO, CINZA, VERDE = (20, 16, 24), (232, 184, 74), (150, 150, 165), (90, 210, 120)
OSSO, ROXO_ESCURO, AZUL_GELO = (240, 237, 224), (90, 50, 160), (190, 235, 255)
CONTORNO = (8, 6, 12, 255)


# ---------- ferramentas de desenho (em 128x128) ----------

def tela():
    return Image.new('RGBA', (G, G), (0, 0, 0, 0))


def esfera(d, cx, cy, r, cor, brilho=BRANCO):
    """Uma esfera: anéis do escuro de fora para o claro de dentro, com um ponto de luz."""
    for i in range(r, 0, -1):
        t = 1 - i / r
        c = tuple(int(cor[k] * (.55 + .45 * t) + brilho[k] * .35 * t * t) for k in range(3))
        d.ellipse((cx - i - r * .15 * t, cy - i - r * .15 * t, cx + i - r * .15 * t, cy + i - r * .15 * t), fill=c + (255,))
    d.ellipse((cx - r * .55, cy - r * .55, cx - r * .25, cy - r * .25), fill=brilho + (255,))


def anel(d, cx, cy, r, cor, largura=6):
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=cor + (255,), width=largura)


def raios(d, cx, cy, r0, r1, n, cor, largura=6, giro=0):
    for i in range(n):
        a = giro + 2 * math.pi * i / n
        d.line((cx + r0 * math.cos(a), cy + r0 * math.sin(a), cx + r1 * math.cos(a), cy + r1 * math.sin(a)),
               fill=cor + (255,), width=largura)


def espiral(d, cx, cy, r, cor, voltas=2.2, largura=7):
    pts = []
    for i in range(120):
        t = i / 119
        a = t * voltas * 2 * math.pi
        pts.append((cx + r * t * math.cos(a), cy + r * t * math.sin(a)))
    d.line(pts, fill=cor + (255,), width=largura, joint='curve')


def crescente(d, cx, cy, r, cor, deslocar=.42, borda=None):
    """Uma lua crescente virada para a direita: um disco menos outro deslocado para a esquerda."""
    m = Image.new('L', (G, G), 0)
    md = ImageDraw.Draw(m)
    md.ellipse((cx - r, cy - r, cx + r, cy + r), fill=255)
    o = r * deslocar
    md.ellipse((cx - r - o, cy - r * .92, cx + r - o, cy + r * .92), fill=0)
    camada = Image.new('RGBA', (G, G), cor + (255,))
    if borda:
        # Borda: o mesmo formato um pouco maior por trás.
        grande = m.resize((G, G))
        d._image.paste(Image.new('RGBA', (G, G), borda + (255,)), (0, 0), dilatar_l(grande, 4))
    d._image.paste(camada, (0, 0), m)


def dilatar_l(m, px):
    a = np.array(m) > 0
    out = a.copy()
    for dy in range(-px, px + 1):
        for dx in range(-px, px + 1):
            if dx * dx + dy * dy <= px * px:
                out |= np.roll(np.roll(a, dy, 0), dx, 1)
    return Image.fromarray((out * 255).astype('uint8'), 'L')


def espada(d, x0, y0, x1, y1, cor, largura=14, guarda=OURO, cabo=PRETO, ponta=True):
    """Uma lâmina de (x0, y0) (o cabo) até (x1, y1) (a ponta), com guarda e cabo."""
    ang = math.atan2(y1 - y0, x1 - x0)
    nx, ny = -math.sin(ang), math.cos(ang)
    comp = math.hypot(x1 - x0, y1 - y0)
    bx, by = x0 + math.cos(ang) * comp * .25, y0 + math.sin(ang) * comp * .25
    meia = largura / 2
    lam = [(bx + nx * meia, by + ny * meia), (x1 - math.cos(ang) * largura + nx * meia, y1 - math.sin(ang) * largura + ny * meia),
           (x1, y1) if ponta else (x1 + nx * meia, y1 + ny * meia),
           (x1 - math.cos(ang) * largura - nx * meia, y1 - math.sin(ang) * largura - ny * meia), (bx - nx * meia, by - ny * meia)]
    d.polygon(lam, fill=cor + (255,))
    d.line((bx + nx * largura * 1.2, by + ny * largura * 1.2, bx - nx * largura * 1.2, by - ny * largura * 1.2),
           fill=guarda + (255,), width=8)
    d.line((x0, y0, bx, by), fill=cabo + (255,), width=9)


def elo(d, cx, cy, ang, cor, comp=26, larg=14, w=6):
    ca, sa = math.cos(ang), math.sin(ang)
    pts = []
    for i in range(24):
        t = 2 * math.pi * i / 24
        px, py = comp / 2 * math.cos(t), larg / 2 * math.sin(t)
        pts.append((cx + px * ca - py * sa, cy + px * sa + py * ca))
    d.line(pts + [pts[0]], fill=cor + (255,), width=w)


def corrente(d, x0, y0, x1, y1, cor, n=5):
    ang = math.atan2(y1 - y0, x1 - x0)
    for i in range(n):
        t = (i + .5) / n
        elo(d, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, ang + (math.pi / 2 if i % 2 else 0) * 0, cor,
            comp=math.hypot(x1 - x0, y1 - y0) / n * 1.35, larg=16 if i % 2 == 0 else 8)


def chama(d, cx, base, alt, larg, cor_fora, cor_dentro):
    for cor, esc in ((cor_fora, 1), (cor_dentro, .55)):
        h, w = alt * esc, larg * esc
        pts = [(cx - w / 2, base), (cx - w * .55, base - h * .45), (cx - w * .15, base - h * .7), (cx - w * .05, base - h),
               (cx + w * .25, base - h * .62), (cx + w * .5, base - h * .4), (cx + w / 2, base)]
        d.polygon(pts, fill=cor + (255,))
        d.ellipse((cx - w / 2, base - w * .5, cx + w / 2, base + w * .45), fill=cor + (255,))


def gota(d, cx, cy, r, cor):
    d.ellipse((cx - r, cy - r * .4, cx + r, cy + r * 1.6), fill=cor + (255,))
    d.polygon([(cx - r * .92, cy + r * .3), (cx, cy - r * 1.6), (cx + r * .92, cy + r * .3)], fill=cor + (255,))
    d.ellipse((cx - r * .5, cy + r * .1, cx - r * .1, cy + r * .6), fill=BRANCO + (255,))


def silhueta(d, cx, cy, esc, cor):
    d.ellipse((cx - 9 * esc, cy - 40 * esc, cx + 9 * esc, cy - 22 * esc), fill=cor)
    d.polygon([(cx - 14 * esc, cy - 20 * esc), (cx + 14 * esc, cy - 20 * esc), (cx + 18 * esc, cy + 14 * esc),
               (cx + 8 * esc, cy + 40 * esc), (cx - 8 * esc, cy + 40 * esc), (cx - 18 * esc, cy + 14 * esc)], fill=cor)


def mascara(d, cx, cy, esc=1.):
    d.ellipse((cx - 30 * esc, cy - 38 * esc, cx + 30 * esc, cy + 34 * esc), fill=OSSO + (255,))
    for lado in (-1, 1):
        d.polygon([(cx + lado * 6 * esc, cy - 4 * esc), (cx + lado * 22 * esc, cy - 10 * esc),
                   (cx + lado * 20 * esc, cy + 4 * esc)], fill=PRETO + (255,))
        d.line((cx + lado * 13 * esc, cy - 36 * esc, cx + lado * 16 * esc, cy - 14 * esc), fill=VERMELHO + (255,), width=int(6 * esc))
        d.line((cx + lado * 16 * esc, cy + 2 * esc, cx + lado * 12 * esc, cy + 30 * esc), fill=VERMELHO + (255,), width=int(6 * esc))
    for i in range(-3, 4):
        d.line((cx + i * 5 * esc, cy + 16 * esc, cx + i * 5 * esc, cy + 26 * esc), fill=PRETO + (255,), width=2)


def atomo(d, cx, cy, r, cor, nucleo):
    for ang in (0, 60, 120):
        m = Image.new('RGBA', (G, G), (0, 0, 0, 0))
        ImageDraw.Draw(m).ellipse((cx - r, cy - r * .36, cx + r, cy + r * .36), outline=cor + (255,), width=6)
        d._image.alpha_composite(m.rotate(ang, center=(cx, cy), resample=Image.BICUBIC))
    esfera(d, cx, cy, int(r * .3), nucleo)


def seta_cima(d, cx, cy, r, cor):
    d.polygon([(cx, cy - r), (cx + r, cy), (cx + r * .4, cy), (cx + r * .4, cy + r), (cx - r * .4, cy + r),
               (cx - r * .4, cy), (cx - r, cy)], fill=cor + (255,))


def relogio(d, cx, cy, r, cor):
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=BRANCO + (255,), outline=cor + (255,), width=max(3, r // 4))
    d.line((cx, cy, cx, cy - r * .65), fill=PRETO + (255,), width=max(3, r // 5))
    d.line((cx, cy, cx + r * .5, cy), fill=PRETO + (255,), width=max(3, r // 5))


def raio(d, cx, cy, r, cor):
    d.polygon([(cx + r * .2, cy - r), (cx - r * .6, cy + r * .1), (cx - r * .05, cy + r * .1), (cx - r * .25, cy + r),
               (cx + r * .6, cy - r * .2), (cx + r * .05, cy - r * .2)], fill=cor + (255,))


def ampulheta(d, cx, cy, r, cor):
    d.polygon([(cx - r * .7, cy - r), (cx + r * .7, cy - r), (cx, cy), (cx + r * .7, cy + r), (cx - r * .7, cy + r), (cx, cy)],
              fill=cor + (255,))
    d.polygon([(cx - r * .35, cy + r * .75), (cx + r * .35, cy + r * .75), (cx, cy + r * .3)], fill=OURO + (255,))


# ---------- os ícones ----------

def gojo_azul(d):
    esfera(d, C, C, 40, AZUL)
    espiral(d, C, C, 38, AZUL_GELO, 2.4, 6)


def gojo_vermelho(d):
    raios(d, C, C, 34, 56, 12, VERMELHO, 7)
    esfera(d, C, C, 34, VERMELHO, (255, 200, 200))


def gojo_roxo(d):
    esfera(d, C, C, 44, ROXO, (230, 210, 255))
    d.arc((C - 44, C - 44, C + 44, C + 44), 100, 260, fill=AZUL + (255,), width=8)
    d.arc((C - 44, C - 44, C + 44, C + 44), 280, 80, fill=VERMELHO + (255,), width=8)


def gojo_roxo_200(d):
    raios(d, C, C, 40, 60, 8, OURO, 6, giro=math.pi / 8)
    esfera(d, C, C, 40, ROXO, (240, 220, 255))
    anel(d, C, C, 50, OURO, 6)


def gojo_infinito(d):
    for r, cor in ((54, AZUL), (40, AZUL_GELO), (26, BRANCO)):
        anel(d, C, C, r, cor, 6)
    d.ellipse((C - 8, C - 8, C + 8, C + 8), fill=BRANCO + (255,))


def gojo_vazio_infinito(d):
    d.ellipse((C - 54, C - 54, C + 54, C + 54), fill=(10, 10, 30, 255))
    rnd = random.Random(7)
    for _ in range(40):
        x, y = rnd.uniform(C - 46, C + 46), rnd.uniform(C - 46, C + 46)
        if (x - C) ** 2 + (y - C) ** 2 < 46 ** 2:
            d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=BRANCO + (255,))
    d.ellipse((C - 40, C - 18, C + 40, C + 18), fill=BRANCO + (255,))
    esfera(d, C, C, 16, AZUL, AZUL_GELO)
    d.ellipse((C - 6, C - 6, C + 6, C + 6), fill=PRETO + (255,))


def gojo_recurso(d):
    chama(d, C, 110, 96, 70, AZUL, AZUL_GELO)


def gojo_despertar(d):
    d.polygon([(C - 58, C), (C, C - 34), (C + 58, C), (C, C + 34)], fill=BRANCO + (255,))
    esfera(d, C, C, 26, AZUL, AZUL_GELO)
    d.ellipse((C - 8, C - 8, C + 8, C + 8), fill=PRETO + (255,))
    raios(d, C, C, 40, 58, 6, AZUL_GELO, 4, giro=-math.pi / 2)


def ichigo_getsuga(d):
    crescente(d, C + 4, C, 52, AZUL_GELO, borda=AZUL)


def ichigo_getsuga_negro(d):
    crescente(d, C + 4, C, 52, PRETO, borda=VERMELHO)


def ichigo_getsuga_chao(d):
    crescente(d, C + 4, C - 10, 40, AZUL_GELO, borda=AZUL)
    d.rectangle((8, 100, 120, 112), fill=(110, 80, 60, 255))
    for x in (24, 60, 96):
        d.polygon([(x - 8, 100), (x, 82), (x + 8, 100)], fill=(150, 120, 90, 255))


def ichigo_getsuga_lamina(d):
    chama(d, C, 120, 120, 74, PRETO, VERMELHO)
    espada(d, 24, 108, 104, 20, (225, 225, 235), 12, guarda=PRETO)


def ichigo_shunpo(d, cor=BRANCO, sombra=CINZA):
    for i, x in enumerate((30, 52, 76)):
        alfa = (sombra if i < 2 else cor)
        d.polygon([(x, 24), (x + 26, C), (x, 104), (x + 12, C)], fill=alfa + (255,))


def ichigo_shunpo_bankai(d):
    ichigo_shunpo(d, VERMELHO, PRETO)


def ichigo_arremesso(d):
    anel(d, C, C, 52, BRANCO, 5)
    espada(d, 30, 98, 100, 28, (210, 210, 220), 28, guarda=PRETO, ponta=False)


def ichigo_bankai(d):
    espada(d, 22, 106, 108, 20, PRETO, 13, guarda=OURO)
    d.line((22, 106, 10, 118), fill=PRETO + (255,), width=4)
    elo(d, 14, 114, .8, CINZA, 10, 6, 3)


def ichigo_sair_bankai(d):
    espada(d, 22, 106, 108, 20, PRETO, 13, guarda=OURO)
    # A seta para baixo: o Bankai se desfaz e volta ao Shikai.
    d.polygon([(76, 78), (116, 78), (96, 108)], fill=CINZA + (255,))


def ichigo_mascara(d):
    mascara(d, C, C + 4, 1.4)


def ichigo_mugetsu(d):
    chama(d, C, 124, 124, 96, PRETO, (60, 10, 10))
    crescente(d, C + 8, C - 6, 30, (40, 0, 0), borda=VERMELHO)


def ichigo_recurso(d):
    chama(d, C, 112, 98, 72, VERMELHO, (255, 180, 160))


def ichigo_despertar(d):
    d.ellipse((C - 50, C - 56, C + 50, C + 52), fill=OSSO + (255,))
    d.pieslice((C - 50, C - 56, C + 50, C + 52), 90, 270, fill=PRETO + (255,))
    d.ellipse((C - 14, C - 14, C + 14, C + 14), fill=(0, 0, 0, 0))
    for lado, cor in ((-1, OSSO), (1, PRETO)):
        d.polygon([(C + lado * 8, C - 20), (C + lado * 34, C - 26), (C + lado * 30, C - 8)], fill=cor + (255,))
    d.line((C + 22, C - 54, C + 26, C - 28), fill=VERMELHO + (255,), width=7)


def cid_redemoinho(d):
    for i in range(3):
        a0 = 120 * i
        d.arc((C - 50, C - 50, C + 50, C + 50), a0, a0 + 80, fill=ROXO + (255,), width=12)
        d.arc((C - 34, C - 34, C + 34, C + 34), a0 + 40, a0 + 100, fill=(200, 170, 255, 255), width=6)
    esfera(d, C, C, 12, ROXO_ESCURO, ROXO)


def cid_pos_imagem(d):
    silhueta(d, C - 16, C + 4, 1.35, (110, 80, 170, 150))
    silhueta(d, C + 14, C + 4, 1.35, PRETO + (255,))
    d.line((C + 2, C - 48, C + 2, C + 58), fill=ROXO + (255,), width=3)


def cid_prisao(d):
    corrente(d, 14, 22, 114, 106, ROXO, 6)
    corrente(d, 14, 106, 114, 22, (200, 170, 255), 6)


def cid_overdrive(d):
    raios(d, C, C, 22, 60, 10, ROXO, 8, giro=.3)
    raio(d, C, C, 46, OURO)


def cid_espada_atomica(d):
    espada(d, 20, 108, 106, 22, (40, 30, 60), 14, guarda=OURO)
    esfera(d, 70, 58, 16, ROXO, (240, 220, 255))
    anel(d, 70, 58, 26, (200, 170, 255), 4)


def cid_recovery(d):
    anel(d, C, C, 50, ROXO, 9)
    d.rectangle((C - 9, C - 34, C + 9, C + 34), fill=(200, 170, 255, 255))
    d.rectangle((C - 34, C - 9, C + 34, C + 9), fill=(200, 170, 255, 255))


def cid_atomic(d):
    atomo(d, C, C, 54, (140, 120, 255), ROXO)


def cid_recurso(d):
    gota(d, C, C - 14, 32, ROXO)


def cid_despertar(d):
    anel(d, C, C + 6, 40, OURO, 14)
    d.polygon([(C - 18, C - 40), (C, C - 60), (C + 18, C - 40), (C, C - 22)], fill=ROXO + (255,))
    d.polygon([(C - 8, C - 42), (C, C - 52), (C + 8, C - 42)], fill=(230, 210, 255, 255))


ICONES = {nome: f for nome, f in globals().items() if callable(f) and nome.split('_')[0] in TEMAS and nome[0].islower()}


def desenhar_icone(nome, f):
    personagem = nome.split('_')[0]
    principal, secundaria, fundo, borda = (rgb(c) for c in TEMAS[personagem])
    simbolo = tela()
    f(ImageDraw.Draw(simbolo))
    return compor(fundo_icone(principal, fundo), simbolo)


def fundo_icone(principal, fundo):
    y, x = np.mgrid[0:32, 0:32]
    dist = np.hypot(x - 15.5, y - 15.5) / 22
    t = np.clip(1 - dist, 0, 1) ** 1.6 * .45
    arr = np.zeros((32, 32, 4), 'uint8')
    for k in range(3):
        arr[..., k] = (np.array(fundo[k]) * (1 - t) + principal[k] * t).astype('uint8')
    arr[..., 3] = 255
    return Image.fromarray(arr, 'RGBA')


def reduzir(simbolo, tam=32):
    """De 128 para 32: média das cores e alfa cortado, com um contorno escuro de 1 px em volta."""
    p = simbolo.resize((tam, tam), Image.LANCZOS)
    a = np.array(p)
    cheio = a[..., 3] > 100
    a[..., 3] = np.where(cheio, 255, 0)
    borda = np.zeros_like(cheio)
    for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        borda |= np.roll(np.roll(cheio, dy, 0), dx, 1)
    borda &= ~cheio
    a[borda] = CONTORNO
    return Image.fromarray(a, 'RGBA')


def compor(fundo, simbolo):
    out = fundo.copy()
    out.alpha_composite(reduzir(simbolo))
    return out


SELOS = {
    'forca': lambda d: seta_cima(d, C, C, 52, VERDE),
    'recarga': lambda d: relogio(d, C, C, 52, AZUL),
    'carga': lambda d: raio(d, C, C, 56, OURO),
    'duracao': lambda d: ampulheta(d, C, C, 54, AZUL_GELO),
    'recurso': lambda d: gota(d, C, C - 20, 40, ROXO),
}


def desenhar_selo(f):
    """O selo vai no canto de baixo à direita (12x12), num disco escuro; o resto é transparente."""
    s = tela()
    f(ImageDraw.Draw(s))
    peq = reduzir(s, 10)
    out = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    ImageDraw.Draw(out).ellipse((19, 19, 31, 31), fill=(12, 10, 16, 230))
    out.alpha_composite(peq, (20, 20))
    return out


# ---------- fundos repetíveis da árvore ----------

FUNDOS = {
    # nome: (cor de base, cor do brilho, estrelas)
    'gojo': ((12, 18, 36), (60, 150, 230), 60),
    'ichigo': ((14, 8, 8), (120, 20, 20), 0),
    'ichigo_hollow': ((22, 8, 8), (170, 40, 30), 0),
    'cid': ((12, 8, 26), (110, 60, 200), 80),
    'generico': ((16, 16, 20), (60, 60, 70), 0),
}


def fundo_arvore(base, brilho, estrelas, semente):
    """Ruído feito de senos com frequências inteiras (repete sem emenda) e estrelas que dão a volta na borda."""
    rnd = random.Random(semente)
    y, x = np.mgrid[0:256, 0:256] / 256 * 2 * math.pi
    ruido = np.zeros((256, 256))
    for _ in range(7):
        fx, fy = rnd.randint(1, 4), rnd.randint(1, 4)
        ruido += np.sin(fx * x + rnd.uniform(0, 6.3)) * np.sin(fy * y + rnd.uniform(0, 6.3)) / (fx + fy)
    ruido = (ruido - ruido.min()) / (ruido.max() - ruido.min())
    t = ruido ** 2 * .35
    arr = np.zeros((256, 256, 4), 'uint8')
    for k in range(3):
        arr[..., k] = (base[k] * (1 - t) + brilho[k] * t).astype('uint8')
    arr[..., 3] = 255
    for _ in range(estrelas):
        sx, sy = rnd.randrange(256), rnd.randrange(256)
        v = rnd.randint(140, 255)
        arr[sy, sx, :3] = (v, v, min(255, v + 20))
        if rnd.random() < .2:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                arr[(sy + dy) % 256, (sx + dx) % 256, :3] = (v // 2, v // 2, v // 2 + 20)
    return Image.fromarray(arr, 'RGBA')


def main():
    (GUI / 'icones').mkdir(parents=True, exist_ok=True)
    (GUI / 'fundos').mkdir(parents=True, exist_ok=True)
    feitos = {}
    for nome, f in sorted(ICONES.items()):
        feitos[nome] = desenhar_icone(nome, f)
    for nome, f in SELOS.items():
        feitos['selo_' + nome] = desenhar_selo(f)
    for nome, img in feitos.items():
        img.save(GUI / 'icones' / (nome + '.png'))
    for i, (nome, (base, brilho, estrelas)) in enumerate(FUNDOS.items()):
        fundo_arvore(base, brilho, estrelas, 100 + i).save(GUI / 'fundos' / (nome + '.png'))
    print(len(feitos), 'ícones e', len(FUNDOS), 'fundos')
    if len(sys.argv) > 1:
        folha(feitos, Path(sys.argv[1]))


def folha(feitos, destino):
    """Folha de contato: cada ícone em 4x com o nome embaixo, para olhar de uma vez."""
    colunas, cel = 8, 140
    linhas = (len(feitos) + colunas - 1) // colunas
    out = Image.new('RGB', (colunas * cel, linhas * cel), (40, 40, 48))
    d = ImageDraw.Draw(out)
    for i, (nome, img) in enumerate(sorted(feitos.items())):
        x, y = i % colunas * cel, i // colunas * cel
        out.paste(img.resize((128, 128), Image.NEAREST), (x + 6, y + 2), img.resize((128, 128), Image.NEAREST))
        d.text((x + 4, y + 128), nome[:22], fill=(230, 230, 230))
    out.save(destino)
    print('folha em', destino)


if __name__ == '__main__':
    main()
