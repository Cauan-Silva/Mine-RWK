"""Gera o horizonte da Escola Jujutsu: quatro faixas de 90° que o cliente desenha num anel em volta do jogador.

Cada faixa tem 1024 × 768 e três linhas de 256, uma por camada (a de cima é a mais longe):
  0. montanhas distantes, claras e azuladas pela distância;
  1. montanhas do meio;
  2. morros perto, com a floresta escura e, ao sul (faixa 0), a cidade com uma torre de rádio genérica.
O panorama de 360° é feito inteiro e cortado em quatro, com ruído periódico: as faixas emendam, inclusive a
3 com a 0. O pátio fica na altura 171 de cada linha (o anel vai de 30 blocos abaixo a 60 acima dele).

Uso: python ferramentas/gerar_horizonte_escola.py
Não edite os PNGs à mão: mude este script e gere de novo.
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

RAIZ = Path(__file__).resolve().parent.parent
SAIDA = RAIZ / "src" / "main" / "resources" / "assets" / "minerwk" / "textures" / "ceu"
LARGURA = 4096          # o panorama inteiro
FAIXA = 1024
LINHA = 256
PATIO = 171             # a altura do pátio em cada linha
CIDADE = (300, 760)     # onde fica a cidade no panorama (o meio da faixa 0 é 512)

random.seed(1311)


def perfil(ondas, base, amplitude):
    """Altura (em px, de cima) de uma serra periódica: soma de senos com períodos que dividem o panorama."""
    fases = [(n, random.uniform(0, 2 * math.pi), random.uniform(0.4, 1.0) / n ** 0.8) for n in ondas]
    soma = sum(p for _, _, p in fases)
    alturas = []
    for x in range(LARGURA):
        t = 2 * math.pi * x / LARGURA
        # Cristas: 1 − 2|sen| tem pico agudo onde o seno zera, como uma serra, e continua periódica.
        v = sum(p * (1 - 2 * abs(math.sin(n * t + f))) for n, f, p in fases) / soma
        alturas.append(base - amplitude * v)
    return alturas


def mistura(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def camada(alturas, cor_topo, cor_base):
    img = Image.new("RGBA", (LARGURA, LINHA), (0, 0, 0, 0))
    px = img.load()
    for x in range(LARGURA):
        topo = max(0, int(alturas[x]))
        for y in range(topo, LINHA):
            t = (y - topo) / max(1, LINHA - topo)
            px[x, y] = mistura(cor_topo, cor_base, min(1.0, t * 1.6)) + (255,)
    return img


def floresta(img, alturas):
    """Pontas de pinheiro ao longo do morro perto, e o escuro da mata embaixo."""
    d = ImageDraw.Draw(img)
    escuro, sombra = (24, 34, 30, 255), (34, 46, 40, 255)
    x = 0
    while x < LARGURA:
        largura = random.randint(5, 9)
        altura = random.randint(8, 18)
        meio = x + largura // 2
        chao = alturas[meio % LARGURA] + 6
        cor = escuro if random.random() < 0.6 else sombra
        for dx in range(-largura, largura + 1):
            xx = (meio + dx) % LARGURA
            topo = chao - altura * (1 - abs(dx) / (largura + 1))
            d.line([(xx, topo), (xx, LINHA)], fill=cor)
        x += random.randint(3, 7)
    for y in range(PATIO - 6, LINHA):
        t = (y - PATIO + 6) / (LINHA - PATIO + 6)
        d.line([(0, y), (LARGURA, y)], fill=mistura((26, 36, 32), (14, 18, 20), t) + (255,))


def cidade(img):
    """Prédios em silhueta com janelas acesas, e uma torre treliçada vermelha e branca."""
    d = ImageDraw.Draw(img)
    x = CIDADE[0]
    base = PATIO + 8
    while x < CIDADE[1]:
        w = random.randint(14, 34)
        meio = (x - CIDADE[0]) / (CIDADE[1] - CIDADE[0])
        alto = random.randint(18, 40) + int(36 * math.sin(math.pi * meio))
        cor = random.choice([(46, 48, 64), (54, 54, 72), (40, 42, 56)])
        d.rectangle([x, base - alto, x + w, LINHA], fill=cor + (255,))
        for wy in range(base - alto + 4, base - 2, 5):
            for wx in range(x + 3, x + w - 2, 4):
                if random.random() < 0.35:
                    d.rectangle([wx, wy, wx + 1, wy + 1], fill=random.choice([(255, 214, 140), (255, 236, 190), (250, 190, 110)]) + (255,))
        x += w + random.randint(1, 6)
    # A torre: um tronco treliçado que afina, em faixas vermelhas e brancas, com uma luz no alto.
    cx, chao, topo = 560, base, base - 150
    for y in range(topo, chao):
        t = (y - topo) / (chao - topo)
        meia = 1 + int(12 * t ** 1.4)
        faixa = int((y - topo) / 12) % 2
        cor = (214, 64, 48) if faixa == 0 else (236, 232, 226)
        d.line([(cx - meia, y), (cx - meia + 1, y)], fill=cor + (255,))
        d.line([(cx + meia - 1, y), (cx + meia, y)], fill=cor + (255,))
        if y % 6 == 0:
            d.line([(cx - meia, y), (cx + meia, y)], fill=cor + (255,))
    for y in (topo + 40, topo + 90):
        meia = 4 + int(12 * ((y - topo) / (chao - topo)) ** 1.4)
        d.rectangle([cx - meia - 2, y, cx + meia + 2, y + 4], fill=(236, 232, 226, 255))
    d.line([(cx, topo - 18), (cx, topo)], fill=(214, 64, 48, 255))
    d.ellipse([cx - 2, topo - 21, cx + 2, topo - 17], fill=(255, 80, 60, 255))


def principal():
    longe = camada(perfil([2, 3, 5, 7, 11, 19, 29], 112, 58), (178, 160, 196), (150, 150, 186))
    meio = camada(perfil([3, 4, 6, 9, 13, 17, 31], 138, 40), (112, 104, 146), (84, 84, 118))
    perto_alturas = perfil([4, 5, 8, 12, 19, 27], 158, 12)
    perto = camada(perto_alturas, (58, 64, 74), (34, 40, 44))
    floresta(perto, perto_alturas)
    cidade(perto)

    SAIDA.mkdir(parents=True, exist_ok=True)
    camadas = [longe, meio, perto]
    faixas = []
    for f in range(4):
        img = Image.new("RGBA", (FAIXA, LINHA * 3), (0, 0, 0, 0))
        for i, c in enumerate(camadas):
            img.paste(c.crop((f * FAIXA, 0, (f + 1) * FAIXA, LINHA)), (0, i * LINHA))
        img.save(SAIDA / f"horizonte_escola_{f}.png")
        faixas.append(img)

    # A emenda: a borda direita de cada faixa casa com a esquerda da seguinte.
    for f in range(4):
        a, b = faixas[f].load(), faixas[(f + 1) % 4].load()
        dif = sum(abs(a[FAIXA - 1, y][c] - b[0, y][c]) for y in range(LINHA * 3) for c in range(4)) / (LINHA * 3 * 4)
        assert dif < 8, f"emenda da faixa {f} com a {(f + 1) % 4}: diferença média {dif:.1f}"
    print(f"4 faixas em {SAIDA}")


if __name__ == "__main__":
    principal()
