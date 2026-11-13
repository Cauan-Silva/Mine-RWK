"""Prévia da planta da arena (entrada da Escola Jujutsu), sem abrir o jogo.

Lê build/arena.txt, que o teste PlantaEscolaTest.despejaAPlantaParaAPrevia escreve
(./gradlew test --tests "*PlantaEscola*"), e desenha previas/arena.png com três vistas:
de cima, de frente (olhando para o norte, como o jogador ao entrar) e de lado.

Uso: python ferramentas/previa_arena.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

RAIZ = Path(__file__).resolve().parent.parent
ENTRADA = RAIZ / "build" / "arena.txt"
SAIDA = RAIZ / "previas" / "arena.png"

CORES = {
    "PEDRA": (112, 112, 112),
    "TIJOLO_PEDRA": (132, 132, 128),
    "LAJE_PEDRA": (150, 150, 146),
    "ANDESITO_POLIDO": (170, 174, 172),
    "ESCADA_PEDRA": (150, 146, 138),
    "MADEIRA_ESCURA": (62, 40, 22),
    "MADEIRA_VERMELHA": (150, 44, 40),
    "TRONCO": (84, 58, 34),
    "FOLHA": (58, 96, 58),
    "LANTERNA": (250, 200, 90),
    "MURO": (100, 100, 104),
    "GRAMA": (96, 140, 70),
    "PAREDE_BRANCA": (232, 228, 218),
    "PILAR": (50, 34, 20),
    "ASSOALHO": (118, 86, 50),
    "TELHA": (58, 58, 64),
    "LAJE_TELHA": (70, 70, 78),
    "CONCRETO_CLARO": (226, 228, 230),
    "VIDRO_AZUL": (120, 170, 220),
    "FUNDO_ESCURO": (40, 50, 120),
    "PORTAL_NEGRO": (16, 16, 20),
    "PORTAL_ROXO": (90, 30, 140),
    "TATAME": (196, 170, 80),
    "BAMBU_PISO": (186, 160, 70),
    "SHOJI": (244, 244, 240),
    "GRADE_NS": (110, 78, 44),
    "GRADE_LO": (110, 78, 44),
    "VELA": (240, 232, 200),
    "BARRIL": (120, 88, 52),
    "ESTANTE": (140, 100, 60),
    "SUPORTE_ARMADURA": (100, 70, 40),
    "LAJE_MADEIRA": (70, 46, 26),
    "TAPETE": (170, 40, 40),
    "FENO": (200, 176, 60),
    "ALVO": (230, 210, 200),
    "QUADRO": (20, 24, 22),
    "CERCA_ESCURA": (54, 36, 20),
    "AGUA": (60, 100, 200),
    "VITORIA_REGIA": (40, 120, 40),
    "CASCALHO": (140, 130, 126),
    "PEDRA_MUSGO": (100, 120, 90),
    "FOLHA_CEREJEIRA": (240, 170, 200),
    "TRONCO_CEREJEIRA": (60, 30, 40),
    "BAMBU": (110, 160, 50),
    "SINO": (230, 190, 60),
    "ESCADA_MADEIRA_N": (66, 44, 24),
    "ESCADA_MADEIRA_S": (66, 44, 24),
    "ESCADA_MADEIRA_L": (66, 44, 24),
    "ESCADA_MADEIRA_O": (66, 44, 24),
}
CEU = (236, 190, 140)
MEIA = 50          # a caixa vai de -50 a +50
ESCALA = 6         # pixels por bloco
MARGEM = 24
Y_MIN, Y_MAX = -20, 26


def ler():
    blocos = []
    for linha in ENTRADA.read_text().splitlines():
        dx, dy, dz, material = linha.split()
        blocos.append((int(dx), int(dy), int(dz), material))
    return blocos


def sombra(cor, fator):
    return tuple(max(0, min(255, int(c * fator))) for c in cor)


def de_cima(blocos):
    """O bloco mais alto de cada coluna, mais claro quanto mais alto."""
    topo = {}
    for dx, dy, dz, m in blocos:
        if (dx, dz) not in topo or dy > topo[(dx, dz)][0]:
            topo[(dx, dz)] = (dy, m)
    lado = (2 * MEIA + 1) * ESCALA
    img = Image.new("RGB", (lado, lado), (20, 20, 28))
    d = ImageDraw.Draw(img)
    for (dx, dz), (dy, m) in topo.items():
        x, y = (dx + MEIA) * ESCALA, (dz + MEIA) * ESCALA
        d.rectangle([x, y, x + ESCALA - 1, y + ESCALA - 1], fill=sombra(CORES[m], 0.8 + 0.03 * min(dy + 1, 12)))
    # Onde o jogador e o Toji entram.
    for dz, cor in ((24, (60, 120, 255)), (8, (255, 60, 60))):
        x, y = MEIA * ESCALA, (dz + MEIA) * ESCALA
        d.ellipse([x - 2, y - 2, x + ESCALA + 1, y + ESCALA + 1], outline=cor, width=2)
    return img


def de_perfil(blocos, eixo):
    """Corta a caixa olhando ao longo de um eixo: de cada coluna aparece o bloco mais perto de quem olha."""
    perto = {}
    for dx, dy, dz, m in blocos:
        if eixo == "norte":      # olhando para o norte: o que tem dz maior está mais perto
            h, prof = dx, dz
        else:                     # de lado, olhando para o oeste: dx maior está mais perto
            h, prof = dz, dx
        if (h, dy) not in perto or prof > perto[(h, dy)][0]:
            perto[(h, dy)] = (prof, m)
    largura = (2 * MEIA + 1) * ESCALA
    altura = (Y_MAX - Y_MIN + 1) * ESCALA
    img = Image.new("RGB", (largura, altura), CEU)
    d = ImageDraw.Draw(img)
    for (h, dy), (prof, m) in perto.items():
        if not Y_MIN <= dy <= Y_MAX:
            continue
        x, y = (h + MEIA) * ESCALA, (Y_MAX - dy) * ESCALA
        d.rectangle([x, y, x + ESCALA - 1, y + ESCALA - 1], fill=sombra(CORES[m], 0.75 + 0.25 * (prof + MEIA) / (2 * MEIA)))
    return img


def main():
    blocos = ler()
    cima, frente, lado = de_cima(blocos), de_perfil(blocos, "norte"), de_perfil(blocos, "oeste")
    largura = cima.width + frente.width + 3 * MARGEM
    altura = max(cima.height, frente.height + lado.height + 2 * MARGEM) + 2 * MARGEM
    img = Image.new("RGB", (largura, altura), (30, 30, 36))
    d = ImageDraw.Draw(img)
    img.paste(cima, (MARGEM, MARGEM + 12))
    d.text((MARGEM, 6), "De cima (norte para cima; azul = jogador, vermelho = Toji)", fill=(230, 230, 230))
    x = cima.width + 2 * MARGEM
    img.paste(frente, (x, MARGEM + 12))
    d.text((x, 6), "De frente, olhando para o norte (a escadaria e o torii grande ao fundo)", fill=(230, 230, 230))
    y = MARGEM + 12 + frente.height + MARGEM
    img.paste(lado, (x, y + 12))
    d.text((x, y - 4), "De lado (norte à esquerda)", fill=(230, 230, 230))
    SAIDA.parent.mkdir(exist_ok=True)
    img.save(SAIDA)
    print(f"{len(blocos)} blocos -> {SAIDA}")


if __name__ == "__main__":
    main()
