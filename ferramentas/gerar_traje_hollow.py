"""A forma Hollow completa do Ichigo: o traje que cobre o jogador inteiro durante os 10 s da forma.

Rodar na raiz do projeto: python ferramentas/gerar_traje_hollow.py
Escreve em assets/minerwk:
  geo/traje/ichigo_hollow.geo.json               o modelo, nos ossos de armadura do GeckoLib
  textures/traje/ichigo_hollow.png               a textura (64x64), pintada aqui
  animations/traje/ichigo_hollow.animation.json  vazio: quem anima é o jogador
Feito aqui, sem asset de terceiros. A obra: pele branca, cabelo laranja até a cintura, o buraco no peito com
as marcas pretas, tufos vermelhos no pescoço, nos pulsos e nos tornozelos, garras, o hakama preto rasgado e a
máscara que cobre a cabeça toda, com dois chifres longos apontando para a frente.
O nome do traje repete core/traje/TrajeIchigo.HOLLOW; o TrajeIchigoTest confere os ossos.
"""
import json
import random
from pathlib import Path
from PIL import Image

RAIZ = Path(__file__).resolve().parents[1]
ASSETS = RAIZ / 'src/main/resources/assets/minerwk'
LADO = 64

PELE = (240, 238, 234, 255)
PELE_SOMBRA = (208, 206, 204, 255)
OSSO = (246, 242, 232, 255)
OSSO_SOMBRA = (212, 206, 194, 255)
PRETO = (14, 12, 18, 255)
PANO = (26, 22, 32, 255)
IRIS = (255, 206, 52, 255)
RUBRO = (176, 28, 40, 255)
RUBRO_CLARO = (214, 58, 62, 255)
CABELO = (232, 126, 38, 255)
CABELO_SOMBRA = (190, 92, 26, 255)

# Quadros pintados (canto, largura, altura) e cores chapadas (um quadrado 2x2).
MASCARA = (0, 0, 8, 8)
CRANIO = (8, 0, 8, 8)
PEITO = (16, 0, 8, 12)
COSTAS = (24, 0, 8, 12)
JUBA = (32, 0, 9, 16)
BARRA = (44, 0, 4, 12)
TUFO = (48, 0, 5, 2)
COR_PELE, COR_SOMBRA, COR_OSSO, COR_PRETO, COR_RUBRO, COR_PANO, COR_CABELO, COR_PONTA = (
    (0, 60), (2, 60), (4, 60), (6, 60), (8, 60), (10, 60), (12, 60), (14, 60))


def textura():
    im = Image.new('RGBA', (LADO, LADO), (0, 0, 0, 0))
    px = im.load()
    rng = random.Random(11)

    def encher(quadro, cor):
        x0, y0, w, h = quadro
        for y in range(h):
            for x in range(w):
                px[x0 + x, y0 + y] = cor(x, y) if callable(cor) else cor

    # A máscara: osso, as órbitas pretas com a íris amarela, os dentes e as duas marcas que descem da testa.
    encher(MASCARA, lambda x, y: OSSO_SOMBRA if y == 7 or x in (0, 7) else OSSO)
    x0, y0 = MASCARA[:2]
    for ex in (1, 5):
        for dx in range(2):
            px[x0 + ex + dx, y0 + 3] = PRETO
            px[x0 + ex + dx, y0 + 4] = PRETO
    for ex in (2, 5):
        px[x0 + ex, y0 + 4] = IRIS
    for x in (1, 6):
        for y in range(0, 3):
            px[x0 + x, y0 + y] = PRETO
        px[x0 + x, y0 + 5] = PRETO
    for x in range(1, 7):
        px[x0 + x, y0 + 6] = PRETO if x % 2 else OSSO
    # O resto do crânio: osso liso com sombra nas bordas.
    encher(CRANIO, lambda x, y: OSSO_SOMBRA if y == 7 or (x + y) % 5 == 0 else OSSO)
    # O peito: pele branca, o buraco no meio e as marcas pretas que saem dele para os ombros e a cintura.
    encher(PEITO, lambda x, y: PELE_SOMBRA if x in (0, 7) or y == 11 else PELE)
    x0, y0 = PEITO[:2]
    for x, y in ((3, 3), (4, 3), (3, 4), (4, 4), (3, 2), (4, 2), (2, 3), (5, 3), (2, 4), (5, 4), (3, 5), (4, 5)):
        px[x0 + x, y0 + y] = PRETO
    for x, y in ((1, 1), (2, 2), (6, 1), (5, 2), (0, 0), (7, 0), (3, 6), (4, 6), (3, 7), (4, 8), (3, 9), (1, 5), (6, 5)):
        px[x0 + x, y0 + y] = PRETO
    # As costas: pele com sombra na espinha.
    encher(COSTAS, lambda x, y: PELE_SOMBRA if x in (0, 3, 4, 7) or y == 11 else PELE)
    # O cabelo: mechas laranja que acabam em pontas (o fim é transparente).
    pontas = [15, 13, 16, 14, 16, 13, 15, 14, 16]
    encher(JUBA, lambda x, y: (0, 0, 0, 0) if y >= pontas[x] else
           (CABELO_SOMBRA if (x + y // 3) % 3 == 0 or y == pontas[x] - 1 else CABELO))
    # O hakama: preto, com a barra rasgada.
    rasgo = [12, 10, 11, 9]
    encher(BARRA, lambda x, y: (0, 0, 0, 0) if y >= rasgo[x] else (PRETO if rng.random() < .25 else PANO))
    # Os tufos vermelhos.
    encher(TUFO, lambda x, y: RUBRO_CLARO if (x + y) % 2 == 0 else RUBRO)
    for canto, cor in ((COR_PELE, PELE), (COR_SOMBRA, PELE_SOMBRA), (COR_OSSO, OSSO), (COR_PRETO, PRETO),
                       (COR_RUBRO, RUBRO), (COR_PANO, PANO), (COR_CABELO, CABELO), (COR_PONTA, OSSO_SOMBRA)):
        for y in range(2):
            for x in range(2):
                px[canto[0] + x, canto[1] + y] = cor
    (ASSETS / 'textures/traje').mkdir(parents=True, exist_ok=True)
    im.save(ASSETS / 'textures/traje/ichigo_hollow.png')


def cubo(origem, tamanho, cor, inflar=0.0, **faces):
    """Um cubo de cor chapada; cada face dada em {@code faces} usa um quadro pintado da textura."""
    uv = {f: {'uv': [cor[0] + .5, cor[1] + .5], 'uv_size': [1, 1]} for f in ('north', 'south', 'east', 'west', 'up', 'down')}
    for face, (x, y, w, h) in faces.items():
        uv[face] = {'uv': [x, y], 'uv_size': [w, h]}
    c = {'origin': origem, 'size': tamanho, 'uv': uv}
    if inflar:
        c['inflate'] = inflar
    return c


def chifre(lado):
    """Um chifre: sai do lado da cabeça, vai para a frente e sobe um pouco, afinando; a ponta é escura."""
    def x(de, largura):
        return de if lado > 0 else -de - largura
    return [
        cubo([x(4.3, 1.7), 28.4, -5.2], [1.7, 1.9, 5.4], COR_OSSO),
        cubo([x(4.7, 1.3), 29.3, -9.6], [1.3, 1.5, 4.8], COR_OSSO),
        cubo([x(5.0, .9), 30.1, -13.4], [.9, 1.0, 4.2], COR_PONTA),
        cubo([x(5.15, .6), 30.7, -15.6], [.6, .6, 2.4], COR_PRETO),
    ]


def braco(lado):
    """Pele branca, o tufo vermelho no pulso e três garras pretas além da mão."""
    x0 = 4 if lado > 0 else -8
    cubos = [cubo([x0, 12, -2], [4, 12, 4], COR_PELE, inflar=.28),
             cubo([x0 - .5, 13.4, -2.5], [5, 2, 5], COR_RUBRO, north=TUFO, south=TUFO, east=TUFO, west=TUFO)]
    for k in range(3):
        cubos.append(cubo([x0 + .5 + k * 1.2, 10.3, -1.9 + k * .2], [.7, 1.7, .8], COR_PRETO))
    return cubos


def perna(lado):
    """O hakama preto de barra rasgada e o tufo vermelho no tornozelo."""
    x0 = 0 if lado > 0 else -4
    return [cubo([x0, 0, -2], [4, 12, 4], COR_PANO, inflar=.34, north=BARRA, south=BARRA, east=BARRA, west=BARRA),
            cubo([x0, 0, -2], [4, 3, 4], COR_PELE, inflar=.1),
            cubo([x0 - .5, 2.2, -2.5], [5, 1.8, 5], COR_RUBRO, north=TUFO, south=TUFO, east=TUFO, west=TUFO)]


def modelo():
    ossos = [
        {'name': 'armorHead', 'pivot': [0, 24, 0], 'cubes': [
            cubo([-4, 24, -4], [8, 8, 8], COR_OSSO, inflar=.6, north=MASCARA, south=CRANIO, east=CRANIO, west=CRANIO, up=CRANIO),
            *chifre(1), *chifre(-1),
            # O cabelo: sai da nuca e desce até a cintura.
            cubo([-4.5, 14, 4.5], [9, 16, 1.2], COR_CABELO, north=JUBA, south=JUBA),
            cubo([-4.3, 27, 3.6], [8.6, 5.6, 1.6], COR_CABELO),
        ]},
        {'name': 'armorBody', 'pivot': [0, 24, 0], 'cubes': [
            cubo([-4, 12, -2], [8, 12, 4], COR_PELE, inflar=.3, north=PEITO, south=COSTAS),
            # O tufo do pescoço e a cintura do hakama.
            cubo([-4.6, 22.4, -2.7], [9.2, 2, 5.4], COR_RUBRO, north=TUFO, south=TUFO, east=TUFO, west=TUFO),
            cubo([-4, 12, -2], [8, 2.6, 4], COR_PANO, inflar=.45),
        ]},
        {'name': 'armorLeftArm', 'pivot': [5, 22, 0], 'cubes': braco(1)},
        {'name': 'armorRightArm', 'pivot': [-5, 22, 0], 'cubes': braco(-1)},
        {'name': 'armorLeftLeg', 'pivot': [2, 12, 0], 'cubes': perna(1)},
        {'name': 'armorRightLeg', 'pivot': [-2, 12, 0], 'cubes': perna(-1)},
        {'name': 'armorLeftBoot', 'pivot': [2, 12, 0]},
        {'name': 'armorRightBoot', 'pivot': [-2, 12, 0]},
    ]
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.ichigo_hollow', 'texture_width': LADO, 'texture_height': LADO,
                        'visible_bounds_width': 5, 'visible_bounds_height': 5, 'visible_bounds_offset': [0, 1.5, 0]},
        'bones': ossos}]}
    (ASSETS / 'geo/traje').mkdir(parents=True, exist_ok=True)
    (ASSETS / 'geo/traje/ichigo_hollow.geo.json').write_text(json.dumps(geo, indent=1) + '\n', encoding='utf-8')
    (ASSETS / 'animations/traje/ichigo_hollow.animation.json').write_text(
        json.dumps({'format_version': '1.8.0', 'animations': {}}, indent=2) + '\n', encoding='utf-8')
    print(f'Traje ichigo_hollow: {len(ossos)} ossos, {sum(len(o.get("cubes", [])) for o in ossos)} cubos')


if __name__ == '__main__':
    textura()
    modelo()
