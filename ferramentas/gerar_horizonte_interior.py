"""Gera o horizonte do mundo interior do Ichigo: quatro faixas de 90° que o cliente desenha num anel em volta do jogador.

Mesmo formato do horizonte da Escola (gerar_horizonte_escola.py): cada faixa tem 1024 × 768 e três linhas de
256, uma por camada (a de cima é a mais longe), com o pátio na altura 171. O mundo interior não tem chão: são
prédios deitados de lado até onde a vista alcança, uns acima, outros abaixo da fachada da luta, alguns
atravessados. Cada camada mais clara e mais azul, a névoa da distância; o céu fica transparente.
O panorama de 360° é desenhado inteiro, repetindo nas pontas, e cortado em quatro: as faixas emendam.

Uso: python ferramentas/gerar_horizonte_interior.py
Não edite os PNGs à mão: mude este script e gere de novo.
"""
import random

from PIL import Image, ImageDraw

from gerar_horizonte_escola import FAIXA, LARGURA, LINHA, PATIO, SAIDA, mistura


def predio(d, x, y, comprimento, altura, concreto, vidro, angulo=0.0):
    """Um prédio deitado: um bloco comprido na horizontal com a grade de janelas (os andares viram colunas)."""
    def ponto(px, py):
        return (x + px, y + py + px * angulo)

    d.polygon([ponto(0, 0), ponto(comprimento, 0), ponto(comprimento, altura), ponto(0, altura)], fill=concreto + (255,))
    andar = max(4, altura // 5)
    for px in range(2, comprimento - 2, andar):
        for py in range(2, altura - 2, max(3, altura // 4)):
            w, h = andar - 2, max(2, altura // 4 - 2)
            cor = vidro if random.random() > .12 else mistura(vidro, (255, 230, 170), .6)
            d.polygon([ponto(px, py), ponto(px + w, py), ponto(px + w, py + h), ponto(px, py + h)], fill=cor + (255,))


def camada(n, faixa_y, tamanho, concreto, vidro, diagonais=0.0):
    """Uma camada de prédios espalhados pelo panorama, desenhados também uma volta antes e depois, para emendar."""
    img = Image.new("RGBA", (LARGURA, LINHA), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for _ in range(n):
        x = random.randint(0, LARGURA)
        y = random.randint(*faixa_y)
        comprimento = random.randint(*tamanho) * 3
        altura = random.randint(*tamanho)
        angulo = random.uniform(-.35, .35) if random.random() < diagonais else 0.0
        for desvio in (-LARGURA, 0, LARGURA):
            predio(d, x + desvio, y, comprimento, altura, concreto, vidro, angulo)
    return img


def principal():
    random.seed(1314)
    longe = camada(150, (70, 240), (14, 30), (206, 224, 242), (182, 208, 238))
    meio = camada(110, (90, 245), (22, 42), (190, 208, 230), (144, 182, 224), diagonais=.15)
    perto = camada(70, (110, 250), (32, 60), (216, 222, 230), (92, 140, 200), diagonais=.2)
    # Embaixo do pátio, na camada perto, uma massa de prédios: o mundo continua para baixo.
    d = ImageDraw.Draw(perto)
    for x in range(0, LARGURA, 70):
        y, comprimento, altura = PATIO + 18 + random.randint(-6, 10), random.randint(90, 160), random.randint(30, 50)
        for desvio in (-LARGURA, 0, LARGURA):
            predio(d, x + desvio, y, comprimento, altura, (206, 212, 220), (84, 128, 186))

    SAIDA.mkdir(parents=True, exist_ok=True)
    camadas = [longe, meio, perto]
    faixas = []
    for f in range(4):
        img = Image.new("RGBA", (FAIXA, LINHA * 3), (0, 0, 0, 0))
        for i, c in enumerate(camadas):
            img.paste(c.crop((f * FAIXA, 0, (f + 1) * FAIXA, LINHA)), (0, i * LINHA))
        img.save(SAIDA / f"horizonte_interior_{f}.png")
        faixas.append(img)

    # A emenda: as faixas internas são o mesmo panorama cortado, contínuas por construção; a que pode quebrar é a
    # volta completa (a 3 com a 0). A régua é a própria imagem, que tem bordas de prédio por todo lado: a volta não
    # pode diferir mais que a maior diferença entre duas colunas vizinhas por dentro.
    def diferenca(a, xa, b, xb):
        return sum(abs(a[xa, y][c] - b[xb, y][c]) for y in range(LINHA * 3) for c in range(4)) / (LINHA * 3 * 4)

    px = [f.load() for f in faixas]
    regua = max(diferenca(px[f], x, px[f], x + 1) for f in range(4) for x in range(0, FAIXA - 1, 7))
    volta = diferenca(px[3], FAIXA - 1, px[0], 0)
    assert volta <= regua + 2, f"a volta (faixa 3 com a 0) não emenda: {volta:.1f} (régua {regua:.1f})"
    print(f"4 faixas em {SAIDA}")


if __name__ == "__main__":
    principal()
