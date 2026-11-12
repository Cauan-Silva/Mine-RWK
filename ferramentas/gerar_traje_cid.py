"""O traje do Shadow (Cid Kagenou, The Eminence in Shadow), com a espada de slime na mão.

Rodar na raiz do projeto: python ferramentas/gerar_traje_cid.py
Escreve em assets/minerwk:
  geo/traje/cid.geo.json               o modelo, nos ossos de armadura do GeckoLib
  textures/traje/cid.png               a textura (256x256), pintada aqui
  textures/traje/cid_glowmask.png      nada no traje brilha: só um pixel fora do desenho, que o GeckoLib exige
  animations/traje/cid.animation.json  vazio: quem anima é o jogador
Feito aqui, sem asset de terceiros. A obra: o sobretudo de slime cor de ébano, de gola alta e aberta, com frisos
dourados e o avesso roxo; o capuz largo e pontudo, aberto na frente (o rosto é o da skin do jogador, nada é pintado
por cima); o colete roxo-escuro, luvas, calça e botas; e a espada longa toda preta, de guarda em cruz com as pontas
em losango e o pomo em cruz menor.
A barra do sobretudo é pano: ossos com os nomes do casaco do Bankai do Ichigo (bk_capa_*), que o TecidoIchigo já
balança, sem código novo. A espada fica no osso "arma", preso ao braço direito, como a do Ichigo.
Os utilitários de cubo, haste e tira vêm de gerar_traje_hollow.py.
O nome do traje repete core/traje/TrajeCid.SHADOW; o TrajeCidTest confere os ossos.
"""
import json
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gerar_traje_hollow as h  # noqa: E402

ASSETS = h.ASSETS
LADO = 256

EBANO = (22, 20, 31, 255)
EBANO_CLARO = (40, 36, 56, 255)
EBANO_SOMBRA = (12, 11, 18, 255)
OURO = (218, 176, 78, 255)
OURO_ESCURO = (150, 112, 42, 255)
ROXO = (112, 62, 178, 255)
ROXO_ESCURO = (62, 34, 104, 255)
BREU = (14, 9, 20, 255)
COURO = (27, 25, 35, 255)
COURO_CLARO = (45, 42, 57, 255)
LAMINA = (17, 15, 25, 255)
FIO = (150, 118, 255, 255)
NADA_COR = (0, 0, 0, 0)

# Quadros pintados (canto, largura, altura): quatro pixels por unidade do modelo.
# O canto (0, 0, 32, 32) fica vazio: é onde estaria o rosto, que é o da skin do jogador.
TOPO = (32, 0, 32, 32)
TEMPORA = (64, 0, 32, 32)
NUCA = (96, 0, 32, 32)
PEITO = (0, 32, 32, 48)
COSTAS = (32, 32, 32, 48)
LATERAL = (64, 32, 16, 48)
BRACO = (80, 32, 16, 48)
PERNA = (96, 32, 16, 48)
# Cores chapadas (um quadrado 2x2 cada), na linha 200.
CHAPADAS = [EBANO, EBANO_CLARO, EBANO_SOMBRA, OURO, OURO_ESCURO, ROXO, ROXO_ESCURO, COURO, COURO_CLARO, LAMINA, FIO, BREU]
(COR_EBANO, COR_EBANO_CLARO, COR_EBANO_SOMBRA, COR_OURO, COR_OURO_ESCURO, COR_ROXO, COR_ROXO_ESCURO, COR_COURO,
 COR_COURO_CLARO, COR_LAMINA, COR_FIO, COR_BREU) = [(2 * i, 200) for i in range(len(CHAPADAS))]
def uv_de(cor):
    return (cor[0] + .5, cor[1] + .5, 1, 1)


def pano(xm, y, claro_a_cada=7):
    """O tecido do sobretudo: ébano com dobras verticais mais claras e a beirada mais escura."""
    if xm < 1.2:
        return EBANO_SOMBRA
    return EBANO_CLARO if int(xm) % claro_a_cada == 3 else EBANO


def textura():
    im = Image.new('RGBA', (LADO, LADO), NADA_COR)
    brilho = Image.new('RGBA', (LADO, LADO), NADA_COR)
    px = im.load()

    def pintar(quadro, cor):
        """{@code cor(xm, y, x)} devolve a cor do pixel; xm é o x dobrado no meio (o desenho é simétrico)."""
        x0, y0, w, a = quadro
        for y in range(a):
            for x in range(w):
                c = cor(min(x, w - 1 - x) + .5, y + .5, x)
                if c is not None:
                    px[x0 + x, y0 + y] = c

    pintar(TOPO, lambda xm, y, x: (EBANO_CLARO if xm > 15 or int(xm) % 7 == 3 else (EBANO_SOMBRA if xm < 1.2 else EBANO)))
    pintar(TEMPORA, lambda xm, y, x: EBANO_SOMBRA if y > 29.5 else
           (EBANO_CLARO if h.a_linha(x + .5, y, [(6, 0), (11, 14), (9, 32)]) <= .6
            or h.a_linha(x + .5, y, [(21, 0), (25, 16), (22, 32)]) <= .6 else EBANO))
    pintar(NUCA, lambda xm, y, x: ROXO_ESCURO if xm > 15.2 else
           (EBANO_CLARO if h.a_linha(xm, y, [(7, 0), (10, 18), (8, 32)]) <= .6 else pano(xm, y, 99)))

    def peito(xm, y, x):
        # O sobretudo aberto na frente, com o friso dourado na beirada, e o colete roxo-escuro por baixo.
        if xm > 10:
            # As duas linhas douradas da cintura e o X entre elas.
            if 31 <= y < 32 or 36 <= y < 37:
                return OURO
            if 32 <= y < 36 and abs((16 - xm) - (y - 32) * 1.5) < .75:
                return OURO_ESCURO
            if xm > 15.4:
                return BREU
            return ROXO_ESCURO if y < 38 else EBANO_SOMBRA
        if xm > 9:
            return OURO
        return pano(xm, y)
    pintar(PEITO, peito)
    pintar(COSTAS, lambda xm, y, x: EBANO_CLARO if h.a_linha(xm, y, [(6, 3), (9, 20), (7, 48)]) <= .6
           else pano(xm, y, 99))
    pintar(LATERAL, lambda xm, y, x: pano(xm + 1.2, y, 5))

    def braco(xm, y, x):
        # A ombreira com friso, a manga, o punho dourado com filete roxo e a luva.
        if y < 1.5 or 6 <= y < 7:
            return OURO
        if 34 <= y < 35:
            return ROXO
        if 35 <= y < 37.5:
            return OURO if int(x) % 4 != 3 else OURO_ESCURO
        if y >= 37.5:
            return COURO_CLARO if 43 <= y < 44 or xm > 6.5 else COURO
        return pano(xm, y, 5)
    pintar(BRACO, braco)

    def perna(xm, y, x):
        # A calça preta com o losango na coxa, o cano roxo da bota e a sola.
        if y < 21:
            if (8 - xm) + abs(y - 10) * .5 < 1.6:
                return ROXO_ESCURO
            return EBANO_CLARO if xm > 6.5 else (EBANO_SOMBRA if xm < 1.2 else EBANO)
        if y < 23:
            return ROXO if y < 22 else ROXO_ESCURO
        if y >= 46:
            return EBANO_SOMBRA
        return COURO_CLARO if xm > 6.5 else COURO
    pintar(PERNA, perna)

    for i, cor in enumerate(CHAPADAS):
        for y in range(2):
            for x in range(2):
                px[2 * i + x, 200 + y] = cor
    # O GeckoLib recusa uma máscara de brilho vazia: um pixel num canto que nenhum cubo usa.
    brilho.putpixel((LADO - 1, LADO - 1), EBANO)
    return im, brilho


cubo = h.cubo


def capuz():
    """O capuz: teto, nuca e laterais em volta da cabeça, mais comprido que ela na frente e sem nada sobre o rosto,
    que é o da skin do jogador. O avesso é roxo, a beirada da frente dourada, e o teto sobe em ponta."""
    roxo = uv_de(COR_ROXO_ESCURO)
    cubos = [
        cubo([-5.2, 23.4, 4.5], [10.4, 9.1, .5], COR_EBANO, south=NUCA, north=roxo),
        cubo([-5.2, 32.5, -5.0], [10.4, .5, 10.0], COR_EBANO, up=TOPO, down=roxo),
        cubo([-5.2, 23.4, -5.0], [.5, 9.1, 9.5], COR_EBANO, west=TEMPORA, east=roxo),
        cubo([4.7, 23.4, -5.0], [.5, 9.1, 9.5], COR_EBANO, east=TEMPORA, west=roxo),
        # A beirada dourada da abertura.
        cubo([-5.24, 32.7, -5.12], [10.48, .34, .2], COR_OURO),
        cubo([-5.24, 23.4, -5.12], [.3, 9.3, .2], COR_OURO),
        cubo([4.94, 23.4, -5.12], [.3, 9.3, .2], COR_OURO),
    ]
    # A ponta do teto, em degraus, cada um com a beirada dourada na frente.
    for meia, y, z0, fundo in ((3.6, 33.0, -5.0, 9.6), (2.3, 33.55, -5.0, 8.6), (1.2, 34.1, -5.0, 7.0), (.45, 34.65, -5.0, 4.6)):
        cubos.append(cubo([-meia, y, z0], [2 * meia, .55, fundo], COR_EBANO if meia > 1 else COR_EBANO_CLARO))
        cubos.append(cubo([-meia, y + .3, z0 - .12], [2 * meia, .28, .2], COR_OURO))
    return cubos


def gola():
    """A gola alta do sobretudo: duas abas que abrem para os lados e uma atrás, de avesso roxo e beirada dourada."""
    cubos = []
    for lado in (-1, 1):
        x = 4.9 if lado > 0 else -5.4
        giro, pivo = [0, 0, 24 * lado], [5.15 * lado, 22.8, 0]
        cubos.append(cubo([x, 22.8, -2.7], [.5, 5.4, 5.6], COR_EBANO, giro=giro, pivo=pivo,
                          **{'west' if lado > 0 else 'east': uv_de(COR_ROXO)}))
        cubos.append(cubo([x - .04, 28.2, -2.74], [.58, .25, 5.68], COR_OURO, giro=giro, pivo=pivo))
        cubos.append(cubo([x - .04, 22.8, -2.86], [.58, 5.4, .2], COR_OURO, giro=giro, pivo=pivo))
    giro, pivo = [-14, 0, 0], [0, 23.2, 2.6]
    cubos.append(cubo([-5.2, 23.2, 2.4], [10.4, 3.6, .5], COR_EBANO, giro=giro, pivo=pivo, north=uv_de(COR_ROXO)))
    cubos.append(cubo([-5.24, 26.8, 2.36], [10.48, .25, .58], COR_OURO, giro=giro, pivo=pivo))
    return cubos


def braco(lado):
    x0 = 4 if lado > 0 else -8
    return [
        cubo([x0, 12, -2], [4, 12, 4], COR_EBANO, inflar=.28, north=BRACO, south=BRACO, east=BRACO, west=BRACO,
             up=uv_de(COR_EBANO_CLARO), down=uv_de(COR_COURO)),
        # A ombreira, um pouco mais larga, com o friso.
        cubo([x0 - .55, 22.3, -2.55], [5.1, 2.1, 5.1], COR_EBANO),
        cubo([x0 - .65, 22.05, -2.65], [5.3, .35, 5.3], COR_OURO),
        # A boca da manga sobre a luva.
        cubo([x0 - .42, 14.6, -2.42], [4.84, .8, 4.84], COR_EBANO),
    ]


def perna(lado):
    x0 = 0 if lado > 0 else -4
    return [
        cubo([x0, 0, -2], [4, 12, 4], COR_EBANO, inflar=.12, north=PERNA, south=PERNA, east=PERNA, west=PERNA,
             down=uv_de(COR_EBANO_SOMBRA)),
        # O cano roxo da bota e o bico.
        cubo([x0 - .3, 6.0, -2.3], [4.6, .9, 4.6], COR_ROXO_ESCURO),
        cubo([x0 + .2, 0, -3.0], [3.6, 1.5, 1.0], COR_COURO),
    ]


def com_avesso(ossos, face):
    """As tiras do sobretudo com o avesso roxo na face de dentro."""
    for o in ossos:
        for c in o['cubes']:
            c['uv'][face] = {'uv': [COR_ROXO_ESCURO[0] + .5, COR_ROXO_ESCURO[1] + .5], 'uv_size': [1, 1]}
    return ossos


# A barra é rasgada: cada tira acaba num comprimento, com um dente mais estreito na ponta.
RASGO_COSTAS = [(6, 1.2), (6, .3), (5, 1.4), (6, .9), (6, .2), (5, 1.1)]
RASGO_FRENTE = {1: (6, .4), 2: (5, 1.2)}


def sobretudo():
    """A saia do sobretudo, da cintura ao tornozelo: inteira atrás e dos lados, aberta na frente."""
    ossos = []
    for c in range(6):
        x = -4.38 + c * 1.46
        ossos += com_avesso(h.tira(f'bk_capa_costas_{c}', [x + .73, 2.45], [x, 2.45], [1.46, .35],
                                   COR_EBANO if c % 2 == 0 else COR_EBANO_CLARO, *RASGO_COSTAS[c]), 'north')
    for lado, nome in ((-1, 'dir'), (1, 'esq')):
        # Na frente só as duas tiras de fora: o meio fica aberto e as pernas aparecem. O TecidoIchigo lê a coluna
        # e o segmento do nome do osso, então as colunas 1 e 2 balançam como a coluna 0 do casaco do Bankai.
        for c in (1, 2):
            x = (c * 1.45) if lado > 0 else (-(c + 1) * 1.45)
            ossos += com_avesso(h.tira(f'bk_capa_frente_{nome}_{c}', [3.6 * lado, -2.5], [x, -2.85], [1.45, .35],
                                       COR_EBANO if c % 2 == 0 else COR_EBANO_CLARO, *RASGO_FRENTE[c]), 'south')
        ossos += com_avesso(h.tira(f'bk_capa_lado_{nome}_0', [4.35 * lado, -.45], [4.35 if lado > 0 else -4.7, -2.65],
                                   [.35, 4.4], COR_EBANO_CLARO, 6, .8), 'west' if lado > 0 else 'east')
        ossos += h.tira(f'bk_capa_quina_{nome}_0', [3.95 * lado, 2.05], [3.2 if lado > 0 else -4.7, 1.87], [1.5, .36],
                        COR_EBANO, 6, giro=[0, 45 * lado, 0])
        ossos += h.tira(f'bk_capa_quinaf_{nome}_0', [4.15 * lado, -2.3], [3.6 if lado > 0 else -4.7, -2.48], [1.1, .36],
                        COR_EBANO, 6, giro=[0, -45 * lado, 0])
    return ossos


# A espada: desenhada de pé (a lâmina sobe em +y a partir da mão) e levada à mão como a do Ichigo.
MAO, ESCALA = (-6.0, 12.6, 0.0), .7


def na_mao(origem, tamanho, cor, giro=None):
    o = [MAO[i] + origem[i] * ESCALA for i in range(3)]
    tam = [v * ESCALA for v in tamanho]
    return cubo(o, tam, cor, giro=giro, pivo=[o[i] + tam[i] / 2 for i in range(3)] if giro else None)


def espada():
    """A espada do Shadow: toda preta, lâmina longa e estreita, guarda em cruz fina com as pontas em losango,
    cabo comprido e o pomo em cruz menor."""
    cubos = [
        na_mao([-.35, -7.0, -.35], [.7, 10.0, .7], COR_COURO),              # o cabo
        na_mao([-1.9, -7.5, -.3], [3.8, .6, .6], COR_LAMINA),               # o pomo: a travessa,
        na_mao([-.3, -8.8, -.3], [.6, 1.4, .6], COR_LAMINA),                # a ponta de baixo
        na_mao([-2.35, -7.6, -.3], [.8, .8, .6], COR_LAMINA, giro=[0, 0, 45]),   # e os dois losangos
        na_mao([1.55, -7.6, -.3], [.8, .8, .6], COR_LAMINA, giro=[0, 0, 45]),
        na_mao([-.95, 2.6, -.6], [1.9, 1.7, 1.2], COR_COURO_CLARO),         # o miolo da guarda
        na_mao([-5.4, 3.15, -.3], [10.8, .6, .6], COR_LAMINA),              # a guarda em cruz
        na_mao([-6.2, 2.9, -.35], [1.1, 1.1, .7], COR_LAMINA, giro=[0, 0, 45]),
        na_mao([5.1, 2.9, -.35], [1.1, 1.1, .7], COR_LAMINA, giro=[0, 0, 45]),
    ]
    # A lâmina, em trechos que afinam até a ponta, com o vinco do meio um tom mais claro.
    for y0, alto, meia in ((4.3, 12, .7), (16.3, 12, .62), (28.3, 8, .5), (36.3, 3, .34), (39.3, 1.6, .18)):
        cubos.append(na_mao([-meia, y0, -.2], [2 * meia, alto, .4], COR_LAMINA))
        if meia > .4:
            cubos.append(na_mao([-.13, y0, -.26], [.26, alto, .52], COR_COURO_CLARO))
    return [
        # Os mesmos giros da espada do Ichigo: deitada para a frente da mão, com a ponta um pouco erguida.
        {'name': 'arma', 'parent': 'armorRightArm', 'pivot': list(MAO), 'rotation': [78, 0, 0]},
        {'name': 'arma_fio', 'parent': 'arma', 'pivot': list(MAO), 'rotation': [0, 62, 0]},
        {'name': 'arma_lamina', 'parent': 'arma_fio', 'pivot': list(MAO), 'cubes': cubos},
    ]


def modelo():
    ossos = [
        {'name': 'armorHead', 'pivot': [0, 24, 0], 'cubes': capuz()},
        {'name': 'armorBody', 'pivot': [0, 24, 0], 'cubes': [
            cubo([-4, 12, -2], [8, 12, 4], COR_EBANO, inflar=.32, north=PEITO, south=COSTAS, east=LATERAL, west=LATERAL,
                 up=uv_de(COR_EBANO_CLARO), down=uv_de(COR_EBANO_SOMBRA)),
            *gola(),
        ]},
        {'name': 'armorLeftArm', 'pivot': [5, 22, 0], 'cubes': braco(1)},
        {'name': 'armorRightArm', 'pivot': [-5, 22, 0], 'cubes': braco(-1)},
        {'name': 'armorLeftLeg', 'pivot': [2, 12, 0], 'cubes': perna(1)},
        {'name': 'armorRightLeg', 'pivot': [-2, 12, 0], 'cubes': perna(-1)},
        {'name': 'armorLeftBoot', 'pivot': [2, 12, 0]},
        {'name': 'armorRightBoot', 'pivot': [-2, 12, 0]},
    ]
    barra, arma = sobretudo(), espada()
    ossos += barra + arma
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.cid', 'texture_width': LADO, 'texture_height': LADO,
                        'visible_bounds_width': 6, 'visible_bounds_height': 5, 'visible_bounds_offset': [0, 1.5, 0]},
        'bones': ossos}]}
    (ASSETS / 'geo/traje').mkdir(parents=True, exist_ok=True)
    (ASSETS / 'geo/traje/cid.geo.json').write_text(json.dumps(geo, indent=1) + '\n', encoding='utf-8')
    (ASSETS / 'animations/traje/cid.animation.json').write_text(
        json.dumps({'format_version': '1.8.0', 'animations': {}}, indent=2) + '\n', encoding='utf-8')
    print(f'Traje cid: {len(ossos)} ossos ({len(barra)} de pano, {len(arma)} da espada), '
          f'{sum(len(o.get("cubes", [])) for o in ossos)} cubos')


if __name__ == '__main__':
    pintada, brilho = textura()
    modelo()
    (ASSETS / 'textures/traje').mkdir(parents=True, exist_ok=True)
    pintada.save(ASSETS / 'textures/traje/cid.png')
    brilho.save(ASSETS / 'textures/traje/cid_glowmask.png')
