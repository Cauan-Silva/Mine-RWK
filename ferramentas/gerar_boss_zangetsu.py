"""O Zangetsu interior (boss do Ichigo): modelo, textura, animações e o ícone da Máscara Rachada.

Rodar na raiz do projeto: python ferramentas/gerar_boss_zangetsu.py
Parte do traje do Bankai já gerado (geo/traje/ichigo_bankai.geo.json e a textura dele, por
gerar_traje_ichigo.py) e escreve em assets/minerwk:
  geo/boss/zangetsu.geo.json              o traje preso a um corpo próprio (cabeça, tronco, braços e pernas)
  textures/boss/zangetsu.png              o traje com o claro e o escuro trocados; o vermelho fica
  textures/boss/zangetsu_glowmask.png     só os olhos, que brilham
  animations/boss/zangetsu.animation.json as animações dos golpes (20 ticks por segundo)
  textures/item/mascara_rachada.png       o ícone do item
O traje é desenhado sobre a pele do jogador; o boss não tem pele, então ganha um corpo branco, o rosto com
os olhos de fundo preto e íris amarela, o cabelo branco e a máscara Hollow num osso que o jogo esconde até a
segunda fase ("mascara"). A espada sai da mão no arremesso: o jogo esconde o osso "arma_root".
Os nomes das animações repetem os de core/boss/ZangetsuBoss.java.
"""
import colorsys
import json
import random
from pathlib import Path
from PIL import Image

RAIZ = Path(__file__).resolve().parents[1]
ASSETS = RAIZ / 'src/main/resources/assets/minerwk'
LARGURA, ALTURA_BASE, ALTURA = 128, 128, 160

PELE = (238, 236, 232, 255)
PELE_SOMBRA = (212, 210, 208, 255)
CABELO = (250, 250, 252, 255)
CABELO_SOMBRA = (214, 216, 224, 255)
PRETO = (12, 11, 16, 255)
IRIS = (255, 204, 48, 255)
BOCA = (58, 50, 60, 255)
OSSO = (244, 240, 230, 255)
OSSO_SOMBRA = (214, 208, 196, 255)
RUBRO = (190, 30, 44, 255)

# Onde cada pedaço novo fica na faixa de baixo da textura (y de 128 a 159).
ROSTO, COURO, FRANJA, MASCARA = (0, 128), (8, 128), (16, 128), (32, 128)
COR_PELE, COR_PELE_SOMBRA, COR_OSSO = (24, 128), (26, 128), (28, 128)
# Um quadro todo transparente: a face de baixo do cabelo, que não existe.
VAZIO = (48, 144)


def trocar(cor):
    """Claro vira escuro e escuro vira claro; o que tem cor forte (o vermelho) fica como está."""
    r, g, b, a = cor
    if a == 0:
        return cor
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    if s > .45 and .12 < l < .8:
        return cor
    l = 1 - l * .92
    r, g, b = colorsys.hls_to_rgb(h, l, s * .5)
    return (round(r * 255), round(g * 255), round(b * 255), a)


def textura():
    base = Image.open(ASSETS / 'textures/traje/ichigo_bankai.png').convert('RGBA')
    assert base.size == (LARGURA, ALTURA_BASE), base.size
    im = Image.new('RGBA', (LARGURA, ALTURA), (0, 0, 0, 0))
    brilho = Image.new('RGBA', (LARGURA, ALTURA), (0, 0, 0, 0))
    px, bx = im.load(), brilho.load()
    origem = base.load()
    for y in range(ALTURA_BASE):
        for x in range(LARGURA):
            px[x, y] = trocar(origem[x, y])
    rng = random.Random(7)
    # Rosto: pele clara, olhos de fundo preto com a íris amarela e um sorriso torto.
    x0, y0 = ROSTO
    for y in range(8):
        for x in range(8):
            px[x0 + x, y0 + y] = PELE_SOMBRA if y == 7 or x in (0, 7) else PELE
    for ex in (1, 5):
        for dx in range(2):
            px[x0 + ex + dx, y0 + 3] = PRETO
            px[x0 + ex + dx, y0 + 4] = PRETO
    for ex in (2, 5):
        px[x0 + ex, y0 + 4] = IRIS
        bx[x0 + ex, y0 + 4] = IRIS
    for x in range(2, 6):
        px[x0 + x, y0 + 6] = BOCA
    px[x0 + 6, y0 + 5] = BOCA
    # Couro cabeludo: branco com mechas.
    x0, y0 = COURO
    for y in range(8):
        for x in range(8):
            px[x0 + x, y0 + y] = CABELO_SOMBRA if rng.random() < .28 else CABELO
    # Franja: as mechas descem em pontas e o resto do rosto fica à mostra.
    x0, y0 = FRANJA
    pontas = [3, 2, 3, 1, 2, 3, 2, 3]
    for x in range(8):
        for y in range(pontas[x]):
            px[x0 + x, y0 + y] = CABELO_SOMBRA if y == pontas[x] - 1 else CABELO
    # Máscara Hollow: osso claro, os olhos, os dentes e as listras vermelhas de um lado.
    x0, y0 = MASCARA
    for y in range(8):
        for x in range(8):
            px[x0 + x, y0 + y] = OSSO_SOMBRA if y == 7 or x in (0, 7) else OSSO
    for ex in (1, 5):
        for dx in range(2):
            px[x0 + ex + dx, y0 + 3] = PRETO
    for ex in (2, 5):
        px[x0 + ex, y0 + 3] = IRIS
        bx[x0 + ex, y0 + 3] = IRIS
    for x in range(1, 7):
        px[x0 + x, y0 + 6] = PRETO if x % 2 else OSSO_SOMBRA
    for y, ate in ((1, 3), (2, 2), (4, 2), (5, 3)):
        for x in range(ate):
            px[x0 + x, y0 + y] = RUBRO
    # Cores chapadas.
    for (cx, cy), cor in ((COR_PELE, PELE), (COR_PELE_SOMBRA, PELE_SOMBRA), (COR_OSSO, OSSO)):
        for y in range(2):
            for x in range(2):
                px[cx + x, cy + y] = cor
    im.save(ASSETS / 'textures/boss/zangetsu.png')
    brilho.save(ASSETS / 'textures/boss/zangetsu_glowmask.png')


def chapada(canto):
    return {'uv': [canto[0] + .5, canto[1] + .5], 'uv_size': [1, 1]}


def cubo(origem, tamanho, cor, inflar=0.0, **faces):
    """Um cubo de cor chapada; as faces dadas em {@code faces} usam um quadro 8x8 da textura."""
    uv = {f: chapada(cor) for f in ('north', 'south', 'east', 'west', 'up', 'down')}
    for face, canto in faces.items():
        uv[face] = {'uv': list(canto), 'uv_size': [8, 8]}
    c = {'origin': origem, 'size': tamanho, 'uv': uv}
    if inflar:
        c['inflate'] = inflar
    return c


def modelo():
    geo = json.loads((ASSETS / 'geo/traje/ichigo_bankai.geo.json').read_text(encoding='utf-8'))
    g = geo['minecraft:geometry'][0]
    g['description']['identifier'] = 'geometry.zangetsu'
    g['description']['texture_height'] = ALTURA
    corpo = [
        {'name': 'root', 'pivot': [0, 0, 0]},
        {'name': 'body', 'parent': 'root', 'pivot': [0, 12, 0],
         'cubes': [cubo([-4, 12, -2], [8, 12, 4], COR_PELE)]},
        {'name': 'head', 'parent': 'body', 'pivot': [0, 24, 0],
         'cubes': [cubo([-4, 24, -4], [8, 8, 8], COR_PELE, north=ROSTO, south=COURO, east=COURO, west=COURO, up=COURO),
                   cubo([-4, 24, -4], [8, 8, 8], COR_PELE, inflar=.45, north=FRANJA, south=COURO, east=COURO,
                        west=COURO, up=COURO, down=VAZIO)]},
        {'name': 'mascara', 'parent': 'head', 'pivot': [0, 28, -4],
         'cubes': [cubo([-4.2, 23.8, -5.1], [8.4, 8.4, 1], COR_OSSO, north=MASCARA)]},
        {'name': 'leftArm', 'parent': 'body', 'pivot': [5, 22, 0], 'cubes': [cubo([4, 12, -2], [4, 12, 4], COR_PELE)]},
        {'name': 'rightArm', 'parent': 'body', 'pivot': [-5, 22, 0], 'cubes': [cubo([-8, 12, -2], [4, 12, 4], COR_PELE)]},
        {'name': 'leftLeg', 'parent': 'root', 'pivot': [2, 12, 0], 'cubes': [cubo([0, 0, -2], [4, 12, 4], COR_PELE_SOMBRA)]},
        {'name': 'rightLeg', 'parent': 'root', 'pivot': [-2, 12, 0], 'cubes': [cubo([-4, 0, -2], [4, 12, 4], COR_PELE_SOMBRA)]},
    ]
    pais = {'armorHead': 'head', 'armorBody': 'body', 'armorLeftArm': 'leftArm', 'armorRightArm': 'rightArm',
            'armorLeftLeg': 'leftLeg', 'armorRightLeg': 'rightLeg', 'armorLeftBoot': 'leftLeg', 'armorRightBoot': 'rightLeg'}
    for osso in g['bones']:
        if osso['name'] in pais:
            assert 'parent' not in osso, osso['name']
            osso['parent'] = pais[osso['name']]
    assert any(o['name'] == 'arma_root' for o in g['bones'])
    g['bones'] = corpo + g['bones']
    saida = ASSETS / 'geo/boss/zangetsu.geo.json'
    saida.write_text(json.dumps(geo, separators=(',', ':')), encoding='utf-8')


def anim(duracao, ossos, loop=False):
    """{osso: {tempo: (rx, ry, rz) ou ((rx, ry, rz), (px, py, pz))}} no formato do GeckoLib."""
    saida = {}
    for nome, quadros in ossos.items():
        rot, pos = {}, {}
        for tempo, valor in quadros.items():
            chave = str(round(float(tempo), 3))
            if isinstance(valor[0], (tuple, list)):
                rot[chave] = [float(v) for v in valor[0]]
                pos[chave] = [float(v) for v in valor[1]]
            else:
                rot[chave] = [float(v) for v in valor]
        saida[nome] = {'rotation': rot}
        if any(any(v) for v in pos.values()):
            saida[nome]['position'] = pos
    return {'loop': loop, 'animation_length': duracao, 'bones': saida}


Z = (0, 0, 0)


def animacoes():
    a = {}
    a['idle'] = anim(4, {
        'head': {0: Z, 2: (2, 4, 0), 4: Z},
        'body': {0: Z, 2: (1.5, 0, 0), 4: Z},
        'rightArm': {0: (-8, 0, 6), 2: (-11, 0, 8), 4: (-8, 0, 6)},
        'leftArm': {0: (0, 0, -4), 2: (0, 0, -6), 4: (0, 0, -4)},
    }, loop=True)
    a['walk'] = anim(1, {
        'leftArm': {0: Z, .25: (-24, 0, 0), .5: Z, .75: (24, 0, 0), 1: Z},
        'rightArm': {0: (-12, 0, 6), .25: (0, 0, 6), .5: (-12, 0, 6), .75: (-24, 0, 6), 1: (-12, 0, 6)},
        'leftLeg': {0: Z, .25: (28, 0, 0), .5: Z, .75: (-28, 0, 0), 1: Z},
        'rightLeg': {0: Z, .25: (-28, 0, 0), .5: Z, .75: (28, 0, 0), 1: Z},
        'body': {0: (4, 0, 0), 1: (4, 0, 0)},
    }, loop=True)
    # Chega de cabeça baixa, ergue o rosto e aponta a espada.
    a['entrada'] = anim(2.0, {
        'head': {0: (28, 0, 0), 1.1: (28, 0, 0), 1.5: (-6, 0, 0), 2: Z},
        'body': {0: (6, 0, 0), 1.1: (6, 0, 0), 1.5: (-3, 0, 0), 2: Z},
        'rightArm': {0: Z, 1.1: Z, 1.5: (-70, 0, 12), 1.8: (-70, 0, 12), 2: Z},
        'leftArm': {0: Z, 1.5: (0, 0, -14), 2: Z},
    })
    # Corte de perto: janela aos 0,5 s.
    a['corte'] = anim(1.0, {
        'body': {0: Z, .35: (0, -28, 0), .5: (8, 32, 0), .7: (8, 32, 0), 1: Z},
        'rightArm': {0: Z, .35: (-140, -25, 20), .5: (-35, 35, -10), .7: (-30, 35, -10), 1: Z},
        'leftArm': {0: Z, .35: (-20, 0, -18), .5: (25, 0, -20), 1: Z},
        'rightLeg': {0: Z, .5: (-22, 0, 0), .7: (-22, 0, 0), 1: Z},
        'leftLeg': {0: Z, .5: (18, 0, 0), .7: (18, 0, 0), 1: Z},
    })
    # Getsuga: a espada sobe devagar acima da cabeça e desce aos 0,8 s.
    a['getsuga'] = anim(1.9, {
        'body': {0: Z, .7: (-10, -18, 0), .8: (18, 12, 0), 1.3: (18, 12, 0), 1.9: Z},
        'head': {0: Z, .7: (8, 0, 0), .8: (-12, 0, 0), 1.3: (-12, 0, 0), 1.9: Z},
        'rightArm': {0: Z, .7: (-168, 0, 14), .8: (-28, 12, 0), 1.3: (-24, 12, 0), 1.9: Z},
        'leftArm': {0: Z, .7: (-150, 0, -18), .8: (-20, 0, -12), 1.3: (-16, 0, -12), 1.9: Z},
        'rightLeg': {0: Z, .8: (-24, 0, 0), 1.3: (-24, 0, 0), 1.9: Z},
        'leftLeg': {0: Z, .8: (20, 0, 0), 1.3: (20, 0, 0), 1.9: Z},
        'root': {0: (Z, Z), .75: (Z, Z), .8: (Z, (0, -1.5, 0)), 1.3: (Z, (0, -1.5, 0)), 1.9: (Z, Z)},
    })
    # Três em sequência: aos 0,6, 1,2 e 1,8 s. Desce, corta de lado e desce de novo.
    a['getsuga_3'] = anim(3.1, {
        'body': {0: Z, .5: (-8, -18, 0), .6: (16, 14, 0), 1.1: (0, 38, 0), 1.2: (6, -36, 0), 1.7: (-8, -18, 0),
                 1.8: (18, 14, 0), 2.5: (18, 14, 0), 3.1: Z},
        'rightArm': {0: Z, .5: (-168, 0, 14), .6: (-28, 12, 0), 1.1: (-85, -50, 0), 1.2: (-85, 60, 0),
                     1.7: (-168, 0, 14), 1.8: (-26, 12, 0), 2.5: (-22, 12, 0), 3.1: Z},
        'leftArm': {0: Z, .5: (-30, 0, -20), .6: (20, 0, -14), 1.2: (30, 0, -24), 1.8: (20, 0, -14), 3.1: Z},
        'rightLeg': {0: Z, .6: (-22, 0, 0), 1.2: (-10, 0, 0), 1.8: (-24, 0, 0), 2.5: (-24, 0, 0), 3.1: Z},
        'leftLeg': {0: Z, .6: (18, 0, 0), 1.2: (10, 0, 0), 1.8: (20, 0, 0), 2.5: (20, 0, 0), 3.1: Z},
    })
    # Shunpo: agacha, some aos 0,3 s e já surge com a espada erguida; corta aos 0,7 s.
    a['shunpo'] = anim(1.3, {
        'root': {0: (Z, Z), .25: (Z, (0, -3, 0)), .35: (Z, Z), 1.3: (Z, Z)},
        'body': {0: Z, .25: (24, 0, 0), .4: (-6, -24, 0), .7: (12, 30, 0), .9: (12, 30, 0), 1.3: Z},
        'rightArm': {0: Z, .25: (20, 0, 10), .4: (-150, -20, 18), .65: (-150, -20, 18), .7: (-32, 34, -8),
                     .9: (-28, 34, -8), 1.3: Z},
        'leftArm': {0: Z, .25: (30, 0, -10), .7: (22, 0, -18), 1.3: Z},
        'rightLeg': {0: Z, .25: (-40, 0, 0), .4: Z, .7: (-20, 0, 0), 1.3: Z},
        'leftLeg': {0: Z, .25: (-40, 0, 0), .4: Z, .7: (16, 0, 0), 1.3: Z},
    })
    # Arremesso: gira a espada pela corrente acima da cabeça e a solta aos 0,7 s; ela volta aos 1,5 s.
    a['arremesso'] = anim(1.9, {
        'rightArm': {0: Z, .15: (-165, 0, 0), .65: (-165, 0, 720), .7: (-80, 10, 720), 1.5: (-80, 10, 720),
                     1.6: (-110, 0, 720), 1.9: (0, 0, 720)},
        'body': {0: Z, .65: (-6, -20, 0), .7: (10, 22, 0), 1.5: (10, 22, 0), 1.9: Z},
        'leftArm': {0: Z, .65: (-20, 0, -20), .7: (26, 0, -16), 1.5: (26, 0, -16), 1.9: Z},
        'rightLeg': {0: Z, .7: (-20, 0, 0), 1.5: (-20, 0, 0), 1.9: Z},
        'leftLeg': {0: Z, .7: (16, 0, 0), 1.5: (16, 0, 0), 1.9: Z},
    })
    # Cero: a mão esquerda aberta para a frente; dispara aos 1,2 s e recua.
    a['cero'] = anim(2.1, {
        'leftArm': {0: Z, .3: (-90, 8, 0), 1.2: (-90, 8, 0), 1.25: (-104, 8, 0), 1.5: (-90, 8, 0), 2.1: Z},
        'rightArm': {0: Z, .3: (24, 0, 14), 1.5: (24, 0, 14), 2.1: Z},
        'body': {0: Z, .3: (0, 24, 0), 1.2: (0, 24, 0), 1.25: (-6, 24, 0), 1.5: (0, 24, 0), 2.1: Z},
        'head': {0: Z, .3: (0, -24, 0), 1.5: (0, -24, 0), 2.1: Z},
        'rightLeg': {0: Z, .3: (16, 0, 0), 1.5: (16, 0, 0), 2.1: Z},
        'leftLeg': {0: Z, .3: (-14, 0, 0), 1.5: (-14, 0, 0), 2.1: Z},
    })
    # A máscara: a mão passa pelo rosto (ela aparece aos 0,6 s) e ele ri para trás.
    a['mascara'] = anim(2.0, {
        'leftArm': {0: Z, .45: (-152, 34, 0), .75: (-152, 34, 0), 1.0: (-60, 0, -24), 2.0: Z},
        'head': {0: Z, .45: (16, 0, 0), .75: (16, 0, 0), 1.1: (-16, 0, 0), 1.6: (-16, 0, 0), 2.0: Z},
        'body': {0: Z, .45: (10, 0, 0), .75: (10, 0, 0), 1.1: (-9, 0, 0), 1.6: (-9, 0, 0), 2.0: Z},
        'rightArm': {0: Z, 1.1: (10, 0, 22), 1.6: (10, 0, 22), 2.0: Z},
    })
    # Cai para trás e fica.
    a['morte'] = anim(1.5, {
        'body': {0: Z, .4: (-18, 0, 0), 1.5: (-6, 0, 0)},
        'head': {0: Z, .4: (-20, 0, 0), 1.5: (-10, 0, 0)},
        'rightArm': {0: Z, .5: (-40, 0, 50), 1.5: (-10, 0, 80)},
        'leftArm': {0: Z, .5: (-40, 0, -50), 1.5: (-10, 0, -80)},
        'root': {0: (Z, Z), .4: ((-12, 0, 0), Z), 1.2: ((-88, 0, 0), (0, 1, 0)), 1.5: ((-90, 0, 0), (0, 1, 0))},
    })
    saida = ASSETS / 'animations/boss/zangetsu.animation.json'
    saida.write_text(json.dumps({'format_version': '1.8.0', 'animations': a}, indent=1), encoding='utf-8')


def icone():
    """A Máscara Rachada 16x16: osso claro, as listras vermelhas, os olhos vazios e a rachadura."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    contorno = (92, 84, 74, 255)
    larguras = {2: (5, 10), 3: (4, 11), 4: (3, 12), 5: (3, 12), 6: (3, 12), 7: (3, 12), 8: (3, 12), 9: (4, 11),
                10: (4, 11), 11: (5, 10), 12: (5, 10), 13: (6, 9)}
    for y, (x0, x1) in larguras.items():
        for x in range(x0, x1 + 1):
            px[x, y] = OSSO if x < 10 else OSSO_SOMBRA
        px[x0 - 1, y] = contorno
        px[x1 + 1, y] = contorno
    for x in range(5, 11):
        px[x, 1] = contorno
    for x in range(6, 10):
        px[x, 14] = contorno
    for y, ate in ((3, 6), (5, 5), (8, 5), (10, 6)):
        for x in range(larguras[y][0], ate + 1):
            px[x, y] = RUBRO
    for x in (5, 6, 9, 10):
        px[x, 6] = PRETO
        px[x, 7] = PRETO
    for x in range(6, 10):
        px[x, 11] = PRETO if x % 2 else OSSO_SOMBRA
    # A rachadura desce do alto em ziguezague.
    for x, y in ((8, 2), (8, 3), (7, 4), (8, 5), (8, 6), (7, 7), (8, 8), (8, 9)):
        px[x, y] = (40, 34, 40, 255)
    im.save(ASSETS / 'textures/item/mascara_rachada.png')


if __name__ == '__main__':
    textura()
    modelo()
    animacoes()
    icone()
    print('ok')
