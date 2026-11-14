"""Gera o horizonte de Oriana sob ataque: quatro faixas de 90° que o cliente desenha num anel em volta do jogador.

Mesmo formato do horizonte da Escola (gerar_horizonte_escola.py): cada faixa tem 1024 × 768 e três linhas de
256, uma por camada (a de cima é a mais longe), com o pátio na altura 171. A faixa 0 fica centrada no sul, e as
seguintes vão para oeste, norte e leste: a capital fica na faixa 2, atrás da muralha da arena.
  0. morros distantes, quase pretos contra a noite;
  1. morros do meio, com o brilho laranja dos incêndios e as colunas de fumaça por cima da capital;
  2. o campo perto, escuro, com vilas pequenas pegando fogo e, ao norte, a capital: telhados apinhados, o
     castelo de três torres e as chamas nos telhados.
O panorama de 360° é feito inteiro e cortado em quatro; as faixas emendam, inclusive a 3 com a 0.

Uso: python ferramentas/gerar_horizonte_oriana.py
Não edite os PNGs à mão: mude este script e gere de novo.
"""
import math
import random

from PIL import Image, ImageDraw, ImageFilter

import gerar_horizonte_escola as escola
from gerar_horizonte_escola import FAIXA, LARGURA, LINHA, PATIO, SAIDA, camada, mistura, perfil

CAPITAL = (2160, 2960)    # a capital no panorama: o meio da faixa 2 (o norte) é 2560
VILAS = (700, 1500, 3500)  # vilas pegando fogo nas outras direções
FOGO = [(255, 150, 40), (255, 196, 80), (240, 96, 30)]


def brilho(img, centro, largura, altura, base, cor, alfa):
    """Uma luz larga que sobe do chão e some no alto: o céu iluminado pelo incêndio."""
    luz = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(luz)
    for k in range(24, 0, -1):
        t = k / 24
        d.ellipse([centro - largura * t, base - altura * t, centro + largura * t, base + altura * t * .3],
                  fill=cor + (int(alfa * (1 - t) ** 1.3),))
    img.alpha_composite(luz.filter(ImageFilter.GaussianBlur(8)))


def fumaca(img, x, base, altura, largura):
    """Uma coluna de fumaça que sobe torta e se abre, mais clara embaixo, onde o fogo a ilumina."""
    camada_ = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(camada_)
    desvio = random.uniform(-.25, .25)
    for k in range(40):
        t = k / 40
        y = base - altura * t
        cx = x + desvio * altura * t + 6 * math.sin(t * 7 + x)
        r = largura * (.4 + 1.4 * t)
        cor = mistura((120, 60, 34), (34, 30, 34), min(1, t * 2.5))
        d.ellipse([cx - r, y - r * .6, cx + r, y + r * .6], fill=cor + (int(200 * (1 - t) ** .6),))
    img.alpha_composite(camada_.filter(ImageFilter.GaussianBlur(4)))


def chamas(d, x, chao, altura):
    """Uma chama pequena em cima de um telhado."""
    for k in range(3):
        w = random.randint(2, 4)
        h = altura * random.uniform(.6, 1)
        xx = x + random.randint(-3, 3)
        d.polygon([(xx - w, chao), (xx + w, chao), (xx + random.randint(-1, 1), chao - h)], fill=random.choice(FOGO) + (255,))


def casa(d, x, chao, w, alto, cor, acesa):
    """Uma casa em silhueta com telhado de duas águas; às vezes com janelas acesas pelo fogo."""
    d.rectangle([x, chao - alto, x + w, LINHA], fill=cor + (255,))
    d.polygon([(x - 1, chao - alto), (x + w + 1, chao - alto), (x + w // 2, chao - alto - w // 2 - 2)], fill=cor + (255,))
    if acesa:
        for wy in range(chao - alto + 3, chao - 1, 5):
            for wx in range(x + 2, x + w - 1, 4):
                if random.random() < .4:
                    d.rectangle([wx, wy, wx + 1, wy + 1], fill=random.choice(FOGO) + (255,))


def capital(img):
    """A capital ao norte: telhados apinhados, o castelo de três torres no meio e o fogo nos telhados."""
    d = ImageDraw.Draw(img)
    chao = PATIO + 6
    x = CAPITAL[0]
    while x < CAPITAL[1]:
        w = random.randint(8, 18)
        meio = (x - CAPITAL[0]) / (CAPITAL[1] - CAPITAL[0])
        alto = random.randint(10, 22) + int(20 * math.sin(math.pi * meio))
        casa(d, x, chao, w, alto, random.choice([(26, 18, 24), (32, 22, 28), (22, 16, 20)]), random.random() < .7)
        if random.random() < .35:
            chamas(d, x + w // 2, chao - alto - w // 2, random.randint(8, 18))
        x += w + random.randint(-2, 2)
    # O castelo: um corpo largo, três torres com teto cônico e bandeiras, e fogo numa das torres.
    cx, base = (CAPITAL[0] + CAPITAL[1]) // 2, chao - 40
    cor = (20, 14, 20, 255)
    d.rectangle([cx - 60, base - 30, cx + 60, LINHA], fill=cor)
    for k in range(-60, 61, 8):
        d.rectangle([cx + k, base - 36, cx + k + 4, base - 30], fill=cor)
    for tx, alto in ((cx - 50, 70), (cx, 110), (cx + 50, 80)):
        d.rectangle([tx - 10, base - alto, tx + 10, base], fill=cor)
        d.polygon([(tx - 14, base - alto), (tx + 14, base - alto), (tx, base - alto - 34)], fill=cor)
        d.line([(tx, base - alto - 34), (tx, base - alto - 46)], fill=cor, width=2)
        for wy in range(base - alto + 8, base - 6, 12):
            d.rectangle([tx - 2, wy, tx + 2, wy + 4], fill=random.choice(FOGO) + (255,))
    chamas(d, cx + 50, base - 80, 30)
    chamas(d, cx - 30, base - 30, 22)


def vila(img, centro):
    """Uma vila pequena pegando fogo ao longe, com o brilho e uma coluna de fumaça."""
    brilho(img, centro, 90, 60, PATIO + 6, (230, 100, 40), 170)
    d = ImageDraw.Draw(img)
    x = centro - 40
    while x < centro + 40:
        w = random.randint(6, 10)
        casa(d, x, PATIO + 8, w, random.randint(5, 9), (18, 14, 18), random.random() < .5)
        if random.random() < .5:
            chamas(d, x + w // 2, PATIO + 8 - 10, random.randint(6, 12))
        x += w + random.randint(1, 4)


def principal():
    random.seed(1313)
    escola.random.seed(1313)
    longe = camada(perfil([2, 3, 5, 7, 11, 19, 29], 120, 40), (30, 22, 38), (22, 16, 30))
    meio = camada(perfil([3, 4, 6, 9, 13, 17, 31], 140, 30), (26, 18, 30), (18, 12, 22))
    perto = camada(perfil([4, 5, 8, 12, 19, 27], 160, 10), (16, 12, 18), (10, 8, 12))

    # Atrás da capital: o céu iluminado pelo incêndio e as colunas de fumaça, por cima dos morros do meio.
    brilho(meio, (CAPITAL[0] + CAPITAL[1]) // 2, 520, 150, PATIO, (240, 110, 40), 190)
    for x in range(CAPITAL[0] + 60, CAPITAL[1] - 40, 110):
        fumaca(meio, x + random.randint(-30, 30), PATIO, random.randint(120, 175), random.randint(20, 30))
    for c in VILAS:
        fumaca(meio, c + random.randint(-10, 10), PATIO + 4, random.randint(70, 100), 14)
    for c in VILAS:
        vila(perto, c)
    capital(perto)

    SAIDA.mkdir(parents=True, exist_ok=True)
    camadas = [longe, meio, perto]
    faixas = []
    for f in range(4):
        img = Image.new("RGBA", (FAIXA, LINHA * 3), (0, 0, 0, 0))
        for i, c in enumerate(camadas):
            img.paste(c.crop((f * FAIXA, 0, (f + 1) * FAIXA, LINHA)), (0, i * LINHA))
        img.save(SAIDA / f"horizonte_oriana_{f}.png")
        faixas.append(img)

    # A emenda: a borda direita de cada faixa casa com a esquerda da seguinte.
    for f in range(4):
        a, b = faixas[f].load(), faixas[(f + 1) % 4].load()
        dif = sum(abs(a[FAIXA - 1, y][c] - b[0, y][c]) for y in range(LINHA * 3) for c in range(4)) / (LINHA * 3 * 4)
        assert dif < 8, f"emenda da faixa {f} com a {(f + 1) % 4}: diferença média {dif:.1f}"
    print(f"4 faixas em {SAIDA}")


if __name__ == "__main__":
    principal()
