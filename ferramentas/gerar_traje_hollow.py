"""A forma Hollow completa do Ichigo (o Vasto Lorde): o traje que cobre o jogador inteiro durante os 10 s da forma.

Rodar na raiz do projeto: python ferramentas/gerar_traje_hollow.py
Escreve em assets/minerwk:
  geo/traje/ichigo_hollow.geo.json               o modelo, nos ossos de armadura do GeckoLib
  textures/traje/ichigo_hollow.png               a textura (256x256), pintada aqui
  animations/traje/ichigo_hollow.animation.json  vazio: quem anima é o jogador
Feito aqui, sem asset de terceiros. A obra: pele branca; a máscara de listras pretas, olhos rasgados e presas,
com os dois chifres que saem para os lados e curvam para a frente; a gola de pelo vermelho; o buraco no peito
com as marcas pretas que vão até os ombros; a manga preta rasgada só no braço direito; os tufos vermelhos nos
pulsos e nos tornozelos; as garras; a faixa branca na cintura e o hakama preto rasgado, mais curto na perna
esquerda. Sem o cabelo longo, a pedido do autor (12/11).
O hakama, a manga e as pontas da faixa são pano: ossos com os nomes do casaco do Bankai (bk_capa_*, bk_manga_*),
que o TecidoIchigo já balança, sem código novo. A Tensa Zangetsu na mão direita é a do traje do Bankai
(gerar_traje_ichigo.espada), no osso "arma".
As faces pintadas têm quatro vezes a resolução do cubo (a máscara é 32x32), para caber o desenho.
O nome do traje repete core/traje/TrajeIchigo.HOLLOW; o TrajeIchigoTest confere os ossos.
"""
import json
import math
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gerar_traje_ichigo as bankai  # noqa: E402

RAIZ = Path(__file__).resolve().parents[1]
ASSETS = RAIZ / 'src/main/resources/assets/minerwk'
LADO = 256

PELE = (244, 242, 238, 255)
PELE_MEIA = (224, 222, 226, 255)
PELE_SOMBRA = (192, 190, 202, 255)
OSSO = (248, 245, 236, 255)
OSSO_MEIA = (230, 225, 214, 255)
OSSO_SOMBRA = (204, 197, 186, 255)
PRETO = (16, 14, 20, 255)
PANO = (25, 22, 31, 255)
PANO_CLARO = (44, 39, 54, 255)
RUBRO = (190, 30, 36, 255)
RUBRO_CLARO = (236, 84, 54, 255)
RUBRO_ESCURO = (124, 16, 30, 255)
FAIXA = (234, 232, 226, 255)
FAIXA_SOMBRA = (196, 194, 192, 255)
NADA_COR = (0, 0, 0, 0)

# Quadros pintados (canto, largura, altura).
MASCARA = (0, 0, 32, 32)
TOPO = (32, 0, 32, 32)
TEMPORA = (64, 0, 32, 32)
NUCA = (96, 0, 32, 32)
PEITO = (0, 32, 32, 48)
COSTAS = (32, 32, 32, 48)
LATERAL = (64, 32, 16, 48)
BRACO = (80, 32, 16, 48)
PERNA = (96, 32, 16, 48)
MANGA = (112, 32, 16, 40)
NADA = (200, 100, 1, 1)
# Cores chapadas (um quadrado 2x2).
(COR_PELE, COR_OSSO, COR_PRETO, COR_RUBRO, COR_PANO, COR_FAIXA, COR_SOMBRA, COR_FAIXA_SOMBRA, COR_PANO_CLARO,
 COR_RUBRO_CLARO, COR_RUBRO_ESCURO, COR_OSSO_MEIA) = [(2 * i, 200) for i in range(12)]
COR_PANO_UV = (8.5, 200.5, 1, 1)
# A face da máscara cobre a cabeça inflada: de y 23,45 a 32,55 em 32 linhas, e de x -4,55 a 4,55 em 32 colunas.
CABECA, POR_PIXEL = 4.55, 9.1 / 32


def ao_segmento(x, y, a, b):
    """Distância do ponto ao segmento a-b."""
    (ax, ay), (bx, by) = a, b
    dx, dy = bx - ax, by - ay
    t = max(0, min(1, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)))
    return math.hypot(x - ax - t * dx, y - ay - t * dy)


def a_linha(x, y, pontos):
    return min(ao_segmento(x, y, pontos[i], pontos[i + 1]) for i in range(len(pontos) - 1))


def dentro(x, y, poligono):
    c = False
    n = len(poligono)
    for i in range(n):
        (x1, y1), (x2, y2) = poligono[i], poligono[(i + 1) % n]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            c = not c
    return c


def textura():
    im = Image.new('RGBA', (LADO, LADO), NADA_COR)
    px = im.load()

    def pintar(quadro, cor):
        """{@code cor(xm, y, x)} devolve a cor do pixel ou None; xm é o x dobrado no meio (o desenho é simétrico)."""
        x0, y0, w, h = quadro
        for y in range(h):
            for x in range(w):
                c = cor(min(x, w - 1 - x) + .5, y + .5, x)
                if c is not None:
                    px[x0 + x, y0 + y] = c

    # --- A máscara ---
    olho = [(3, 10.2), (13.6, 14.2), (13.6, 16.6), (6.5, 16.2), (3, 13.4)]

    def mascara(xm, y, x):
        # A boca: uma faixa preta de lado a lado, com as presas de cima e de baixo desencontradas.
        if xm >= 5 and 21 <= y < 30:
            k = int(xm - 6) % 3 if xm >= 6 else 2
            linha = int(y)
            if xm >= 6 and linha in (22, 23) and k < 2:
                return OSSO
            if xm >= 6 and linha == 24 and (k == 1 or int(xm) == 15):
                return OSSO_MEIA
            if xm >= 7 and linha in (27, 28) and k > 0:
                return OSSO
            if xm >= 7 and linha == 26 and k == 1:
                return OSSO_MEIA
            return PRETO
        if dentro(xm, y, olho):
            return PRETO
        # As listras: a de fora desce da testa, passa pelo olho e segue pela bochecha; a de dentro afina até o nariz.
        if a_linha(xm, y, [(6.5, 0), (6.5, 7), (5.5, 11)]) <= 1.5:
            return PRETO
        if y > 15 and a_linha(xm, y, [(5.5, 15), (4, 21), (4.2, 32)]) <= .95:
            return PRETO
        if y < 11 and a_linha(xm, y, [(12.2, 0), (12.6, 5), (14.2, 10.5)]) <= 1.45 * (1 - y / 11) + .35:
            return PRETO
        if 17.5 <= y < 20 and 13.5 <= xm < 14.6:
            return PRETO
        if xm < 1.2 or y >= 30.5:
            return OSSO_SOMBRA
        # O relevo: a sombra debaixo da sobrancelha, a bochecha funda e o osso do nariz.
        if ((xm - 9) / 4.2) ** 2 + ((y - 19) / 2.4) ** 2 < 1 or (xm < 2.4) or y >= 29.5:
            return OSSO_MEIA
        if 9.2 < y < 10.4 and xm > 3:
            return OSSO_MEIA
        return OSSO
    pintar(MASCARA, mascara)

    def topo(xm, y, x):
        if abs(xm - 6.5) <= 1.5 or abs(xm - 12.2) <= 1.4:
            return PRETO
        return OSSO_MEIA if xm < 1.5 or y < 1.5 or y > 30.5 else OSSO
    pintar(TOPO, topo)

    def nuca(xm, y, x):
        if y < 19 and abs(xm - 6.5) <= 1.5 * (1 - y / 19) + .3:
            return PRETO
        if y < 12 and abs(xm - 12.2) <= 1.4 * (1 - y / 12) + .3:
            return PRETO
        if y > 27 or xm < 1.2:
            return OSSO_SOMBRA
        return OSSO_MEIA if y > 22 or xm < 2.6 or abs(xm - 15.5) < .6 else OSSO
    pintar(NUCA, nuca)

    def tempora(xm, y, x):
        # (Sem espelho: x corre da nuca para o rosto ou o contrário; o desenho serve aos dois lados.)
        if y > 27.5:
            return OSSO_SOMBRA
        if a_linha(x + .5, y, [(4, 30), (14, 23), (27, 21)]) <= .8:
            return OSSO_SOMBRA
        return OSSO_MEIA if y > 22 or y < 1.5 else OSSO
    pintar(TEMPORA, tempora)

    # --- O tronco ---
    def peito(xm, y, x):
        if (xm - 15.5) ** 2 + (y - 19) ** 2 <= 5.3 ** 2:
            return PRETO
        # Duas marcas de cada lado até o ombro, as do pescoço e a ponta para baixo.
        if a_linha(xm, y, [(11, 16), (5, 10), (0, 7)]) <= 1.6 or a_linha(xm, y, [(11, 21.5), (5, 20.5), (0, 21)]) <= 1.5:
            return PRETO
        if a_linha(xm, y, [(12.5, 14.5), (10.5, 8), (10, 0)]) <= 1.0:
            return PRETO
        if 24 <= y < 30 and abs(xm - 15.5) <= .5 + (30 - y) * .25:
            return PRETO
        if xm < 1.5:
            return PELE_SOMBRA
        # O peitoral, a linha do meio e os gomos do abdômen.
        if a_linha(xm, y, [(3, 25.5), (8, 28), (14, 27)]) <= .8:
            return PELE_SOMBRA
        if y > 29 and xm > 14.4:
            return PELE_SOMBRA
        for linha in (32.5, 36.5, 40.5, 44.5):
            if abs(y - linha) <= .6 and 8.5 < xm <= 14.4:
                return PELE_SOMBRA
        if y > 29 and a_linha(xm, y, [(8, 29), (7, 38), (8.5, 48)]) <= .6:
            return PELE_SOMBRA
        if y > 30 and a_linha(xm, y, [(2, 31), (5, 34)]) <= .6 or y > 35 and a_linha(xm, y, [(2, 37), (5, 40)]) <= .6:
            return PELE_MEIA
        return PELE_MEIA if xm < 3.2 or (y > 29 and xm < 8) else PELE
    pintar(PEITO, peito)

    def costas(xm, y, x):
        if a_linha(xm, y, [(8, 12), (4, 9), (0, 7)]) <= 1.6 or a_linha(xm, y, [(7, 21.5), (0, 21)]) <= 1.5:
            return PRETO
        if xm < 1.5 or xm > 14.6:
            return PELE_SOMBRA
        if a_linha(xm, y, [(5, 14), (10, 18), (11, 26)]) <= .7 or (y > 30 and abs(xm - 11) <= .5):
            return PELE_SOMBRA
        return PELE_MEIA if xm < 3.2 or y > 40 else PELE
    pintar(COSTAS, costas)
    pintar(LATERAL, lambda xm, y, x: PRETO if 5.5 <= y < 8.5 or 19.5 <= y < 22.5 else
           (PELE_SOMBRA if xm < 1.5 else (PELE_MEIA if xm < 3.5 else PELE)))
    # O braço: a marca do ombro, o músculo e a mão.
    pintar(BRACO, lambda xm, y, x: PRETO if 5 <= y < 8 else
           (PELE_SOMBRA if xm < 1.5 or 43 <= y < 44 or a_linha(xm, y, [(4, 12), (7, 18), (5, 24)]) <= .6 else
            (PELE_MEIA if xm < 3.2 or y >= 44 else PELE)))
    pintar(PERNA, lambda xm, y, x: PELE_SOMBRA if xm < 1.5 or a_linha(xm, y, [(4, 20), (8, 23)]) <= .7 else
           (PELE_MEIA if xm < 3.2 or 24 < y < 27 else PELE))
    # A manga preta: dobras, e a barra em farrapos.
    farrapo = [40, 33, 37, 30, 39, 31, 35, 28, 38, 32, 40, 29, 36, 31, 39, 34]
    pintar(MANGA, lambda xm, y, x: NADA_COR if y >= farrapo[x] else
           (PANO_CLARO if x % 5 in (1, 2) and int(y) % 9 != 8 else PANO))
    for i, cor in enumerate((PELE, OSSO, PRETO, RUBRO, PANO, FAIXA, OSSO_SOMBRA, FAIXA_SOMBRA, PANO_CLARO, RUBRO_CLARO,
                             RUBRO_ESCURO, OSSO_MEIA)):
        for y in range(2):
            for x in range(2):
                px[2 * i + x, 200 + y] = cor
    return im


def cubo(origem, tamanho, cor, inflar=0.0, giro=None, pivo=None, **faces):
    """Um cubo de cor chapada; cada face dada em {@code faces} usa um quadro pintado da textura."""
    uv = {f: {'uv': [cor[0] + .5, cor[1] + .5], 'uv_size': [1, 1]} for f in ('north', 'south', 'east', 'west', 'up', 'down')}
    for face, (x, y, w, h) in faces.items():
        uv[face] = {'uv': [x, y], 'uv_size': [w, h]}
    c = {'origin': origem, 'size': tamanho, 'uv': uv}
    if inflar:
        c['inflate'] = inflar
    if giro:
        c['rotation'] = giro
        c['pivot'] = pivo
    return c


def haste(de, ate, raio, cor, sobra=.25):
    """Um cubo fino de {@code de} a {@code ate}. O cubo nasce apontando para a frente (-z) e é girado: na conta
    do GeckoLib, rx positivo desce a ponta e ry vira para os lados."""
    d = [ate[i] - de[i] for i in range(3)]
    comprido = math.sqrt(sum(v * v for v in d))
    d = [v / comprido for v in d]
    rx = -math.degrees(math.asin(max(-1, min(1, d[1]))))
    ry = math.degrees(math.atan2(-d[0], -d[2]))
    return cubo([de[0] - raio, de[1] - raio, de[2] - comprido - sobra], [2 * raio, 2 * raio, comprido + sobra], cor,
                giro=[round(rx, 2), round(ry, 2), 0], pivo=list(de))


def chifre(lado):
    """Sai do lado da máscara, sobe, curva para a frente e desce afinando até a ponta, na altura do queixo."""
    caminho = [((4.3, 27.4, -.4), 1.0), ((6.3, 28.5, -.7), .92), ((7.7, 30.0, -1.7), .82), ((8.2, 31.0, -4.0), .72),
               ((7.9, 31.1, -6.9), .6), ((7.2, 30.3, -9.8), .48), ((6.3, 28.8, -12.4), .35), ((5.5, 27.0, -14.4), .22),
               ((5.0, 25.6, -15.6), .12)]
    cubos = []
    for i in range(len(caminho) - 1):
        (a, raio), (b, _) = caminho[i], caminho[i + 1]
        cor = COR_OSSO if i < 4 else (COR_OSSO_MEIA if i < 6 else COR_SOMBRA)
        cubos.append(haste([a[0] * lado, a[1], a[2]], [b[0] * lado, b[1], b[2]], raio, cor))
    # O anel preto onde ele nasce da máscara.
    cubos.append(cubo([4.5 if lado > 0 else -4.95, 26.2, -1.6], [.45, 2.4, 2.4], COR_PRETO))
    return cubos


def tufo(base, rumo, comprido, raio, cor, ponta):
    """Um tufo de pelo: uma haste grossa e a ponta fina, mais clara."""
    n = math.sqrt(sum(v * v for v in rumo))
    d = [v / n for v in rumo]
    meio = [base[i] + d[i] * comprido * .7 for i in range(3)]
    fim = [base[i] + d[i] * comprido for i in range(3)]
    alem = [base[i] + d[i] * (comprido + raio * 2.2) for i in range(3)]
    return [haste(base, fim, raio, cor, sobra=0), haste(meio, alem, raio * .5, ponta, sobra=0)]


CORES_DO_PELO = [(COR_RUBRO, COR_RUBRO_CLARO), (COR_RUBRO_ESCURO, COR_RUBRO), (COR_RUBRO, COR_RUBRO_CLARO),
                 (COR_RUBRO_CLARO, COR_RUBRO_CLARO)]


def chamas(centro_x, y, raio, quantos, comprido, grosso):
    """Os tufos que sobem em volta de um pulso ou tornozelo."""
    cubos = []
    for k in range(quantos):
        a = 2 * math.pi * (k + .5) / quantos
        fora = (math.cos(a), math.sin(a))
        cor, ponta = CORES_DO_PELO[k % 4]
        cubos += tufo([centro_x + fora[0] * raio, y, fora[1] * raio], [fora[0] * .42, 1, fora[1] * .42],
                      comprido * (1 + .3 * ((k * 7) % 3 - 1)), grosso, cor, ponta)
    return cubos


def braco(lado):
    """Pele branca, as chamas vermelhas subindo do pulso e as garras (a manga do direito é pano: ver manga())."""
    x0 = 4 if lado > 0 else -8
    cubos = [cubo([x0, 12, -2], [4, 12, 4], COR_PELE, inflar=.25, north=BRACO, south=BRACO, east=BRACO, west=BRACO),
             cubo([x0 - .45, 12.9, -2.45], [4.9, 1.3, 4.9], COR_RUBRO_ESCURO),
             *chamas(x0 + 2, 13.6, 2.35, 8, 2.3, .5)]
    for k in range(4):
        z = -1.9 + (k % 2) * .5
        cubos.append(cubo([x0 + .25 + k * .95, 10.9, z], [.6, 1.0, .7], COR_PRETO))
        cubos.append(cubo([x0 + .4 + k * .95, 9.8, z + .15], [.3, 1.2, .4], COR_PRETO))
    return cubos


def perna(lado):
    """A perna nua, as chamas no tornozelo e o pé de garras (o hakama é pano preso na cintura: ver saia())."""
    x0 = 0 if lado > 0 else -4
    cubos = [cubo([x0, 0, -2], [4, 12, 4], COR_PELE, inflar=.1, north=PERNA, south=PERNA, east=PERNA, west=PERNA),
             cubo([x0 - .35, 1.1, -2.35], [4.7, 1.2, 4.7], COR_RUBRO_ESCURO),
             *chamas(x0 + 2, 1.7, 2.25, 8, 2.2, .5)]
    for k in range(3):
        cubos.append(cubo([x0 + .45 + k * 1.2, 0, -3.3], [.7, .8, 1.3], COR_PRETO))
        cubos.append(cubo([x0 + .6 + k * 1.2, 0, -4.0], [.4, .5, .8], COR_PRETO))
    return cubos


# --- O pano: os nomes dos ossos são os do casaco do Bankai (core/traje/tecido/TecidoIchigo), que já balança ---
SEGMENTOS = 6
ALTO, PASSO = 12.2, 1.85


def tira(nome, pivo_xz, origem_xz, tamanho_xz, cor, comprimento, dente=0.0, giro=None, pai='armorBody'):
    """Uma tira de pano de {@code comprimento} segmentos (os de baixo existem, mas vazios: o tecido conta seis).
    {@code dente} é a ponta rasgada, mais estreita, abaixo do último segmento."""
    ossos = []
    for s in range(SEGMENTOS):
        topo = ALTO - s * PASSO
        cubos = []
        if s < comprimento:
            cubos.append(cubo([origem_xz[0], topo - PASSO, origem_xz[1]], [tamanho_xz[0], PASSO, tamanho_xz[1]], cor,
                              giro=giro, pivo=[pivo_xz[0], topo, pivo_xz[1]] if giro else None))
            if s == comprimento - 1 and dente:
                largo = [tamanho_xz[0] * (.5 if tamanho_xz[0] > tamanho_xz[1] else 1),
                         tamanho_xz[1] * (.5 if tamanho_xz[1] >= tamanho_xz[0] else 1)]
                cubos.append(cubo([origem_xz[0], topo - PASSO - dente, origem_xz[1]], [largo[0], dente, largo[1]], cor,
                                  giro=giro, pivo=[pivo_xz[0], topo, pivo_xz[1]] if giro else None))
        ossos.append({'name': f'{nome}_{s}', 'parent': pai if s == 0 else f'{nome}_{s - 1}',
                      'pivot': [pivo_xz[0], topo, pivo_xz[1]], 'cubes': cubos})
    return ossos


def saia():
    """O hakama rasgado: uma saia de tiras presas na cintura, inteira atrás e do lado direito, e curta e mais
    rasgada do lado esquerdo, por onde a perna aparece."""
    ossos = []
    # Atrás: seis colunas, em pregas (claro e escuro alternados), cada uma rasgada num comprimento.
    for c, (n, dente) in enumerate([(6, 1.0), (6, .2), (5, 1.3), (6, .6), (5, .5), (4, 1.2)]):
        x = -4.38 + c * 1.46
        ossos += tira(f'bk_capa_costas_{c}', [x + .73, 2.45], [x, 2.45], [1.46, .35],
                      COR_PANO if c % 2 == 0 else COR_PANO_CLARO, n, dente)
    # A frente: três tiras de cada lado, fechando no meio; as da esquerda acabam na coxa.
    for lado, nome, compridos in ((-1, 'dir', [(6, .9), (5, 1.4), (6, .3)]), (1, 'esq', [(3, 1.1), (2, 1.5), (3, .4)])):
        for c, (n, dente) in enumerate(compridos):
            x = (c * 1.45) if lado > 0 else (-(c + 1) * 1.45)
            ossos += tira(f'bk_capa_frente_{nome}_{c}', [3.6 * lado, -2.5], [x, -2.85], [1.45, .35],
                          COR_PANO if c % 2 == 0 else COR_PANO_CLARO, n, dente)
        n_lado, n_quina = (6, 5) if lado < 0 else (3, 4)
        ossos += tira(f'bk_capa_lado_{nome}_0', [4.35 * lado, -.45], [4.35 if lado > 0 else -4.7, -2.65], [.35, 4.4],
                      COR_PANO_CLARO, n_lado, 1.4)
        ossos += tira(f'bk_capa_quina_{nome}_0', [3.95 * lado, 2.05], [3.2 if lado > 0 else -4.7, 1.87], [1.5, .36],
                      COR_PANO, n_quina, .8, giro=[0, 45 * lado, 0])
        ossos += tira(f'bk_capa_quinaf_{nome}_0', [4.15 * lado, -2.3], [3.6 if lado > 0 else -4.7, -2.48], [1.1, .36],
                      COR_PANO, n_lado, .8, giro=[0, -45 * lado, 0])
    return ossos


def manga():
    """A manga preta rasgada do braço direito, em três segmentos que seguem o balanço do braço."""
    ossos = []
    linhas = [(0, 14), (14, 13), (27, 13)]
    for s in range(3):
        topo = 24.4 - s * 3.3
        quadro = (MANGA[0], MANGA[1] + linhas[s][0], 16, linhas[s][1])
        ossos.append({'name': f'bk_manga_dir_{s}', 'parent': 'armorRightArm' if s == 0 else f'bk_manga_dir_{s - 1}',
                      'pivot': [-6.0, topo, 0], 'cubes': [
                          cubo([-8.5, topo - 3.3, -2.5], [5.0, 3.3, 5.0], COR_PANO, north=quadro, south=quadro, east=quadro,
                               west=quadro, up=COR_PANO_UV if s == 0 else NADA, down=NADA)]})
    return ossos


def pontas_da_faixa():
    """As duas pontas caídas do laço: leves, andam na mola da manga esquerda (que este traje não tem)."""
    ossos = []
    for s in range(3):
        topo = 12.6 - s * 1.3
        ossos.append({'name': f'bk_manga_esq_{s}', 'parent': 'armorBody' if s == 0 else f'bk_manga_esq_{s - 1}',
                      'pivot': [0, topo, -3.2], 'cubes': [
                          cubo([-2.0, topo - 1.3, -3.45], [1.1, 1.3, .4], COR_FAIXA),
                          *([cubo([.9, topo - 1.3, -3.45], [1.1, 1.3, .4], COR_FAIXA)] if s < 2 else [])]})
    return ossos


def gola():
    """A gola de pelo: um anel escuro no pescoço e os tufos em volta. Os dos ombros abrem para cima e para fora,
    os de trás caem nas costas e os da frente descem abrindo um V, que deixa o buraco à mostra."""
    cubos = [cubo([-4.5, 22.2, -2.7], [9.0, 2.3, 5.4], COR_RUBRO_ESCURO)]
    quantos = 16
    for k in range(quantos):
        a = 2 * math.pi * (k + .5) / quantos
        fora = (math.cos(a), math.sin(a))
        base = [fora[0] * 4.3, 23.2, fora[1] * 2.7]
        cor, ponta = CORES_DO_PELO[k % 4]
        de_lado, na_frente = abs(fora[0]), fora[1] < -.25
        if na_frente and de_lado < .3:
            continue
        if de_lado > .7:
            cubos += tufo(base, [fora[0] * .85, .62, fora[1] * .5], 4.2, .85, cor, ponta)
            cubos += tufo([base[0], base[1] - .9, base[2]], [fora[0], .12, fora[1] * .5], 3.2, .75, ponta, cor)
        elif na_frente:
            cubos += tufo(base, [fora[0] * .25, -.8, fora[1] * .45], 3.4, .75, cor, ponta)
        else:
            cubos += tufo(base, [fora[0] * .5, -.45, fora[1] * .8], 3.4, .8, cor, ponta)
    return cubos


def linha_da_mascara(linha):
    return 23.45 + 9.1 - linha * POR_PIXEL


def modelo(im):
    # O relevo do rosto: a testa e o focinho saltam da máscara, com o mesmo desenho, e a sombra fica por baixo.
    testa = cubo([-CABECA, linha_da_mascara(10), -CABECA - .45], [2 * CABECA, 10 * POR_PIXEL, .45], COR_OSSO,
                 north=(0, 0, 32, 10), down=(COR_SOMBRA[0] + .5, COR_SOMBRA[1] + .5, 1, 1))
    focinho = cubo([-CABECA + 5 * POR_PIXEL, linha_da_mascara(30), -CABECA - .55], [22 * POR_PIXEL, 10 * POR_PIXEL, .55],
                   COR_OSSO_MEIA, north=(5, 20, 22, 10))
    ossos = [
        {'name': 'armorHead', 'pivot': [0, 24, 0], 'cubes': [
            cubo([-4, 24, -4], [8, 8, 8], COR_OSSO, inflar=.55, north=MASCARA, south=NUCA, east=TEMPORA, west=TEMPORA, up=TOPO),
            testa, focinho,
            *chifre(1), *chifre(-1),
        ]},
        {'name': 'armorBody', 'pivot': [0, 24, 0], 'cubes': [
            cubo([-4, 12, -2], [8, 12, 4], COR_PELE, inflar=.3, north=PEITO, south=COSTAS, east=LATERAL, west=LATERAL),
            *gola(),
            # O cós preto do hakama e a faixa branca, com o laço.
            cubo([-4, 12, -2], [8, 1.4, 4], COR_PANO, inflar=.5),
            cubo([-4, 13.1, -2], [8, 1.3, 4], COR_FAIXA, inflar=.62),
            cubo([-1.3, 12.9, -3.6], [2.6, 1.8, .9], COR_FAIXA_SOMBRA),
            cubo([-2.4, 13.3, -3.5], [1.2, 1.0, .7], COR_FAIXA),
            cubo([1.2, 13.3, -3.5], [1.2, 1.0, .7], COR_FAIXA),
        ]},
        {'name': 'armorLeftArm', 'pivot': [5, 22, 0], 'cubes': braco(1)},
        {'name': 'armorRightArm', 'pivot': [-5, 22, 0], 'cubes': braco(-1)},
        {'name': 'armorLeftLeg', 'pivot': [2, 12, 0], 'cubes': perna(1)},
        {'name': 'armorRightLeg', 'pivot': [-2, 12, 0], 'cubes': perna(-1)},
        {'name': 'armorLeftBoot', 'pivot': [2, 12, 0]},
        {'name': 'armorRightBoot', 'pivot': [-2, 12, 0]},
    ]
    pano = saia() + manga() + pontas_da_faixa()
    arma = bankai.espada('tensa_zangetsu', im)
    ossos += pano + arma
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.ichigo_hollow', 'texture_width': LADO, 'texture_height': LADO,
                        'visible_bounds_width': 6, 'visible_bounds_height': 5, 'visible_bounds_offset': [0, 1.5, 0]},
        'bones': ossos}]}
    (ASSETS / 'geo/traje').mkdir(parents=True, exist_ok=True)
    (ASSETS / 'geo/traje/ichigo_hollow.geo.json').write_text(json.dumps(geo, indent=1) + '\n', encoding='utf-8')
    (ASSETS / 'animations/traje/ichigo_hollow.animation.json').write_text(
        json.dumps({'format_version': '1.8.0', 'animations': {}}, indent=2) + '\n', encoding='utf-8')
    print(f'Traje ichigo_hollow: {len(ossos)} ossos ({len(pano)} de pano, {len(arma)} da espada), '
          f'{sum(len(o.get("cubes", [])) for o in ossos)} cubos')


if __name__ == '__main__':
    textura_pintada = textura()
    modelo(textura_pintada)
    (ASSETS / 'textures/traje').mkdir(parents=True, exist_ok=True)
    textura_pintada.save(ASSETS / 'textures/traje/ichigo_hollow.png')
