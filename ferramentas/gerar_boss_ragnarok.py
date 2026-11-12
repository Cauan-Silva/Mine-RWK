"""O Ragnarok (boss do Cid): modelo, textura, animações e o ícone do Anel da Sucessão.

Rodar na raiz do projeto: python ferramentas/gerar_boss_ragnarok.py
Tudo é desenhado aqui, sem partir de nenhum arquivo de fora, e escrito em assets/minerwk:
  geo/boss/ragnarok.geo.json               o demônio de 7 blocos (112 unidades de altura)
  textures/boss/ragnarok.png               seis quadros de 16x16: corpo, membros, asa, osso, rosto e brasa
  textures/boss/ragnarok_glowmask.png      os olhos, a boca, as rachaduras de brasa e o peito, que brilham
  animations/boss/ragnarok.animation.json  as animações dos golpes (20 ticks por segundo)
  textures/item/anel_da_sucessao.png       o ícone do item
A aparência segue a light novel: colossal, preto-azulado, asas enormes, presas, quatro garras em cada mão e
os membros cor de sangue. A asa esquerda é o osso "asaEsquerda", que o jogo esconde quando o Shadow a corta.
Os nomes das animações repetem os de core/boss/RagnarokBoss.java.
"""
import json
import random
from pathlib import Path
from PIL import Image

RAIZ = Path(__file__).resolve().parents[1]
ASSETS = RAIZ / 'src/main/resources/assets/minerwk'
LADO = 64

CORPO, RUBRO, ASA, OSSO, ROSTO, BRASA = (0, 0), (16, 0), (32, 0), (48, 0), (0, 16), (16, 16)

AZUL_NEGRO = [(14, 16, 30, 255), (20, 23, 42, 255), (27, 31, 56, 255), (10, 11, 22, 255)]
SANGUE = [(118, 14, 22, 255), (142, 20, 28, 255), (96, 10, 18, 255), (164, 30, 34, 255)]
MEMBRANA = [(30, 12, 24, 255), (38, 16, 30, 255), (24, 10, 20, 255)]
VEIA = (120, 22, 34, 255)
MARFIM = [(232, 224, 204, 255), (214, 204, 180, 255), (196, 184, 158, 255)]
FOGO = [(255, 196, 64, 255), (255, 140, 32, 255), (255, 232, 140, 255), (236, 92, 24, 255)]
OLHO = (255, 226, 96, 255)


def textura():
    im = Image.new('RGBA', (LADO, LADO), (0, 0, 0, 0))
    brilho = Image.new('RGBA', (LADO, LADO), (0, 0, 0, 0))
    px, bx = im.load(), brilho.load()
    rng = random.Random(46)

    def ruido(canto, cores):
        for y in range(16):
            for x in range(16):
                px[canto[0] + x, canto[1] + y] = rng.choice(cores)

    def rachadura(canto, passos, inicio):
        """Uma rachadura de brasa que desce em ziguezague e brilha."""
        x, y = inicio
        for _ in range(passos):
            if 0 <= x < 16 and 0 <= y < 16:
                cor = rng.choice(FOGO[:2])
                px[canto[0] + x, canto[1] + y] = cor
                bx[canto[0] + x, canto[1] + y] = cor
            x += rng.choice((-1, 0, 0, 1))
            y += 1

    ruido(CORPO, AZUL_NEGRO)
    for inicio in ((3, 0), (11, 2), (7, 6)):
        rachadura(CORPO, 7, inicio)
    ruido(RUBRO, SANGUE)
    for x in (4, 10):
        for y in range(16):
            if rng.random() < .7:
                px[RUBRO[0] + x + rng.choice((0, 0, 1)), RUBRO[1] + y] = (64, 6, 14, 255)
    ruido(ASA, MEMBRANA)
    # As veias da membrana abrem em leque a partir do canto de cima.
    for k in range(4):
        for t in range(16):
            x, y = t, round(t * (k + 1) / 4)
            if y < 16:
                px[ASA[0] + x, ASA[1] + y] = VEIA
    ruido(OSSO, MARFIM)
    # O rosto: o corpo escuro, as sobrancelhas pesadas, os olhos acesos e a boca aberta em brasa com as presas.
    ruido(ROSTO, AZUL_NEGRO)
    x0, y0 = ROSTO
    for ex in (2, 10):
        for dx in range(4):
            px[x0 + ex + dx, y0 + 4] = AZUL_NEGRO[3]
            for dy in (5, 6):
                px[x0 + ex + dx, y0 + dy] = OLHO
                bx[x0 + ex + dx, y0 + dy] = OLHO
        px[x0 + ex + (3 if ex == 2 else 0), y0 + 5] = AZUL_NEGRO[3]
    for x in range(3, 13):
        for y in range(10, 14):
            cor = FOGO[1] if y < 12 else FOGO[3]
            px[x0 + x, y0 + y] = cor
            bx[x0 + x, y0 + y] = cor
    for x in (3, 6, 9, 12):
        for y in (10, 11):
            px[x0 + x, y0 + y] = MARFIM[0]
            bx[x0 + x, y0 + y] = (0, 0, 0, 0)
    for x in (4, 8, 11):
        px[x0 + x, y0 + 13] = MARFIM[1]
        bx[x0 + x, y0 + 13] = (0, 0, 0, 0)
    # A brasa do peito: clara no meio.
    x0, y0 = BRASA
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** .5
            cor = FOGO[2] if d < 3 else FOGO[0] if d < 5.5 else rng.choice(FOGO[1::2])
            px[x0 + x, y0 + y] = cor
            bx[x0 + x, y0 + y] = cor
    (ASSETS / 'textures/boss').mkdir(parents=True, exist_ok=True)
    im.save(ASSETS / 'textures/boss/ragnarok.png')
    brilho.save(ASSETS / 'textures/boss/ragnarok_glowmask.png')


def cubo(origem, tamanho, quadro, **faces):
    """Um cubo com o mesmo quadro 16x16 em todas as faces; {@code faces} troca o quadro de uma face."""
    uv = {f: {'uv': list(quadro), 'uv_size': [16, 16]} for f in ('north', 'south', 'east', 'west', 'up', 'down')}
    for face, canto in faces.items():
        uv[face] = {'uv': list(canto), 'uv_size': [16, 16]}
    return {'origin': list(origem), 'size': list(tamanho), 'uv': uv}


def espelho(c):
    """O mesmo cubo do outro lado do corpo."""
    m = dict(c)
    m['origin'] = [-(c['origin'][0] + c['size'][0]), c['origin'][1], c['origin'][2]]
    return m


def modelo():
    braco = [cubo([19, 72, -8], [14, 14, 16], CORPO), cubo([20, 44, -6], [12, 28, 12], RUBRO),
             cubo([19, 34, -7], [14, 10, 14], RUBRO)]
    # Quatro garras em cada mão.
    braco += [cubo([19.5 + 3.5 * i, 27, -8], [2, 7, 3], OSSO) for i in range(4)]
    perna = [cubo([3, 18, -7], [13, 22, 14], CORPO), cubo([4, 4, -6], [11, 14, 12], RUBRO),
             cubo([3, 0, -13], [13, 4, 19], RUBRO)]
    perna += [cubo([3.5 + 4.5 * i, 0, -16], [3, 3, 3], OSSO) for i in range(3)]
    asa = [cubo([8, 100, 10], [50, 4, 3], CORPO), cubo([54, 62, 10], [4, 40, 3], CORPO),
           cubo([8, 58, 11], [46, 42, 1], ASA), cubo([8, 44, 11], [28, 14, 1], ASA),
           cubo([56, 104, 10], [3, 8, 3], OSSO)]
    chifre = [cubo([8, 96, -4], [5, 5, 5], OSSO), cubo([11, 100, -4], [4, 12, 4], OSSO), cubo([12, 111, -4], [2, 5, 2], OSSO)]
    ossos = [
        {'name': 'root', 'pivot': [0, 0, 0]},
        {'name': 'body', 'parent': 'root', 'pivot': [0, 40, 0], 'cubes': [
            cubo([-13, 38, -8], [26, 20, 16], CORPO), cubo([-19, 58, -11], [38, 26, 22], CORPO),
            cubo([-6, 64, -11.6], [12, 12, 1], BRASA),
            # Os espinhos das costas.
            cubo([-2, 70, 11], [4, 8, 5], OSSO), cubo([-2, 58, 11], [4, 7, 4], OSSO)]},
        {'name': 'head', 'parent': 'body', 'pivot': [0, 84, 0], 'cubes': [
            cubo([-9, 86, -11], [18, 14, 18], CORPO, north=ROSTO)] + chifre + [espelho(c) for c in chifre]},
        {'name': 'mandibula', 'parent': 'head', 'pivot': [0, 86, 0], 'cubes': [
            cubo([-8, 82, -12], [16, 4, 14], CORPO), cubo([-7, 86, -12], [2, 4, 2], OSSO), cubo([5, 86, -12], [2, 4, 2], OSSO)]},
        {'name': 'leftArm', 'parent': 'body', 'pivot': [24, 80, 0], 'cubes': braco},
        {'name': 'rightArm', 'parent': 'body', 'pivot': [-24, 80, 0], 'cubes': [espelho(c) for c in braco]},
        {'name': 'leftLeg', 'parent': 'root', 'pivot': [9, 40, 0], 'cubes': perna},
        {'name': 'rightLeg', 'parent': 'root', 'pivot': [-9, 40, 0], 'cubes': [espelho(c) for c in perna]},
        {'name': 'asaEsquerda', 'parent': 'body', 'pivot': [8, 80, 11], 'rotation': [0, -24, 8], 'cubes': asa},
        {'name': 'asaDireita', 'parent': 'body', 'pivot': [-8, 80, 11], 'rotation': [0, 24, -8], 'cubes': [espelho(c) for c in asa]},
        {'name': 'cauda', 'parent': 'body', 'pivot': [0, 42, 8], 'rotation': [-28, 0, 0], 'cubes': [
            cubo([-3, 39, 8], [6, 6, 20], CORPO), cubo([-2, 40, 28], [4, 4, 14], RUBRO), cubo([-1, 41, 42], [2, 2, 6], OSSO)]},
    ]
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.ragnarok', 'texture_width': LADO, 'texture_height': LADO,
                        'visible_bounds_width': 12, 'visible_bounds_height': 9, 'visible_bounds_offset': [0, 3.5, 0]},
        'bones': ossos}]}
    (ASSETS / 'geo/boss').mkdir(parents=True, exist_ok=True)
    (ASSETS / 'geo/boss/ragnarok.geo.json').write_text(json.dumps(geo, separators=(',', ':')), encoding='utf-8')


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
        'body': {0: (4, 0, 0), 2: (7, 0, 0), 4: (4, 0, 0)},
        'head': {0: Z, 2: (-3, 5, 0), 4: Z},
        'mandibula': {0: Z, 2: (6, 0, 0), 4: Z},
        'leftArm': {0: (0, 0, -8), 2: (0, 0, -11), 4: (0, 0, -8)},
        'rightArm': {0: (0, 0, 8), 2: (0, 0, 11), 4: (0, 0, 8)},
        'asaEsquerda': {0: Z, 2: (0, -10, 4), 4: Z},
        'asaDireita': {0: Z, 2: (0, 10, -4), 4: Z},
        'cauda': {0: Z, 1: (0, 10, 0), 3: (0, -10, 0), 4: Z},
    }, loop=True)
    a['walk'] = anim(1.6, {
        'body': {0: (8, 0, -2), .8: (8, 0, 2), 1.6: (8, 0, -2)},
        'leftArm': {0: (18, 0, -8), .8: (-18, 0, -8), 1.6: (18, 0, -8)},
        'rightArm': {0: (-18, 0, 8), .8: (18, 0, 8), 1.6: (-18, 0, 8)},
        'leftLeg': {0: (-22, 0, 0), .8: (22, 0, 0), 1.6: (-22, 0, 0)},
        'rightLeg': {0: (22, 0, 0), .8: (-22, 0, 0), 1.6: (22, 0, 0)},
        'asaEsquerda': {0: Z, .8: (0, -8, 0), 1.6: Z},
        'asaDireita': {0: Z, .8: (0, 8, 0), 1.6: Z},
        'cauda': {0: (0, 8, 0), .8: (0, -8, 0), 1.6: (0, 8, 0)},
    }, loop=True)
    # Sai do portal curvado, abre as asas e ruge para o céu.
    a['entrada'] = anim(2.5, {
        'body': {0: (30, 0, 0), .9: (30, 0, 0), 1.3: (-14, 0, 0), 2.1: (-14, 0, 0), 2.5: Z},
        'head': {0: (20, 0, 0), .9: (20, 0, 0), 1.3: (-30, 0, 0), 2.1: (-30, 0, 0), 2.5: Z},
        'mandibula': {0: Z, 1.2: Z, 1.35: (32, 0, 0), 2.1: (32, 0, 0), 2.5: Z},
        'leftArm': {0: (20, 0, -6), .9: (20, 0, -6), 1.3: (10, 0, -50), 2.1: (10, 0, -50), 2.5: Z},
        'rightArm': {0: (20, 0, 6), .9: (20, 0, 6), 1.3: (10, 0, 50), 2.1: (10, 0, 50), 2.5: Z},
        'asaEsquerda': {0: (0, 40, 0), .9: (0, 40, 0), 1.3: (0, -30, 14), 2.1: (0, -30, 14), 2.5: Z},
        'asaDireita': {0: (0, -40, 0), .9: (0, -40, 0), 1.3: (0, 30, -14), 2.1: (0, 30, -14), 2.5: Z},
    })
    # O murro: o braço direito sobe atrás da cabeça e desce no chão aos 0,8 s.
    a['soco'] = anim(1.5, {
        'body': {0: Z, .65: (-12, -20, 0), .8: (38, 16, 0), 1.1: (38, 16, 0), 1.5: Z},
        'rightArm': {0: Z, .65: (-170, 0, 18), .8: (-62, 0, -8), 1.1: (-62, 0, -8), 1.5: Z},
        'leftArm': {0: Z, .65: (-20, 0, -24), .8: (30, 0, -14), 1.5: Z},
        'head': {0: Z, .65: (10, 0, 0), .8: (-24, 0, 0), 1.1: (-24, 0, 0), 1.5: Z},
        'mandibula': {0: Z, .8: (20, 0, 0), 1.1: (20, 0, 0), 1.5: Z},
        'leftLeg': {0: Z, .8: (-16, 0, 0), 1.1: (-16, 0, 0), 1.5: Z},
    })
    # Quatro murros alternados: aos 0,7, 1,2, 1,7 e 2,2 s.
    alto_d, baixo_d = (-165, 0, 16), (-60, 0, -8)
    alto_e, baixo_e = (-165, 0, -16), (-60, 0, 8)
    a['barragem'] = anim(3.2, {
        'body': {0: Z, .55: (-8, -18, 0), .7: (34, 14, 0), 1.05: (-4, 18, 0), 1.2: (34, -14, 0), 1.55: (-4, -18, 0),
                 1.7: (34, 14, 0), 2.05: (-4, 18, 0), 2.2: (36, -14, 0), 2.6: (36, -14, 0), 3.2: Z},
        'rightArm': {0: Z, .55: alto_d, .7: baixo_d, 1.05: (-40, 0, 10), 1.55: alto_d, 1.7: baixo_d, 2.2: (-30, 0, 10), 3.2: Z},
        'leftArm': {0: Z, .7: (-40, 0, -10), 1.05: alto_e, 1.2: baixo_e, 1.7: (-40, 0, -10), 2.05: alto_e, 2.2: baixo_e,
                    2.6: baixo_e, 3.2: Z},
        'head': {0: Z, .7: (-20, 0, 0), 2.6: (-20, 0, 0), 3.2: Z},
        'mandibula': {0: Z, .7: (18, 0, 0), 2.6: (18, 0, 0), 3.2: Z},
    })
    # A rajada: as asas fecham à frente do corpo e abrem de uma vez aos 0,9 s.
    a['rajada'] = anim(2.3, {
        'body': {0: Z, .75: (16, 0, 0), .9: (-12, 0, 0), 1.6: (-12, 0, 0), 2.3: Z},
        'asaEsquerda': {0: Z, .75: (0, 62, 0), .9: (0, -44, 10), 1.6: (0, -44, 10), 2.3: Z},
        'asaDireita': {0: Z, .75: (0, -62, 0), .9: (0, 44, -10), 1.6: (0, 44, -10), 2.3: Z},
        'leftArm': {0: Z, .75: (-30, 0, 20), .9: (10, 0, -40), 1.6: (10, 0, -40), 2.3: Z},
        'rightArm': {0: Z, .75: (-30, 0, -20), .9: (10, 0, 40), 1.6: (10, 0, 40), 2.3: Z},
        'head': {0: Z, .75: (14, 0, 0), .9: (-16, 0, 0), 1.6: (-16, 0, 0), 2.3: Z},
    })
    # A investida: bate as asas subindo (1,5 s), deita o corpo e atravessa de garras para a frente, e pousa.
    a['investida'] = anim(3.0, {
        'body': {0: Z, .3: (-10, 0, 0), 1.4: (-10, 0, 0), 1.6: (62, 0, 0), 2.2: (62, 0, 0), 2.5: (10, 0, 0), 3.0: Z},
        'head': {0: Z, 1.6: (-50, 0, 0), 2.2: (-50, 0, 0), 3.0: Z},
        'asaEsquerda': {0: Z, .25: (0, -40, 20), .5: (0, 20, -10), .75: (0, -40, 20), 1.0: (0, 20, -10),
                        1.25: (0, -40, 20), 1.6: (0, -30, 26), 2.2: (0, -30, 26), 3.0: Z},
        'asaDireita': {0: Z, .25: (0, 40, -20), .5: (0, -20, 10), .75: (0, 40, -20), 1.0: (0, -20, 10),
                       1.25: (0, 40, -20), 1.6: (0, 30, -26), 2.2: (0, 30, -26), 3.0: Z},
        'leftArm': {0: Z, 1.4: (-10, 0, -20), 1.6: (-150, 0, -10), 2.2: (-150, 0, -10), 3.0: Z},
        'rightArm': {0: Z, 1.4: (-10, 0, 20), 1.6: (-150, 0, 10), 2.2: (-150, 0, 10), 3.0: Z},
        'leftLeg': {0: Z, .4: (20, 0, 0), 1.6: (40, 0, 0), 2.2: (40, 0, 0), 2.5: (-10, 0, 0), 3.0: Z},
        'rightLeg': {0: Z, .4: (24, 0, 0), 1.6: (40, 0, 0), 2.2: (40, 0, 0), 2.5: (-10, 0, 0), 3.0: Z},
        'cauda': {0: Z, 1.6: (24, 0, 0), 2.2: (24, 0, 0), 3.0: Z},
    })
    # O Sopro: 2 s enchendo o peito de cabeça para trás, e a boca escancarada para a frente até os 4 s.
    a['sopro'] = anim(5.0, {
        'body': {0: Z, 1.8: (-16, 0, 0), 2.0: (22, 0, 0), 4.0: (22, 0, 0), 5.0: Z},
        'head': {0: Z, 1.8: (-34, 0, 0), 2.0: (8, 0, 0), 4.0: (8, 0, 0), 5.0: Z},
        'mandibula': {0: Z, 1.6: (10, 0, 0), 2.0: (44, 0, 0), 4.0: (44, 0, 0), 4.4: Z, 5.0: Z},
        'leftArm': {0: Z, 1.8: (20, 0, -34), 2.0: (30, 0, -20), 4.0: (30, 0, -20), 5.0: Z},
        'rightArm': {0: Z, 1.8: (20, 0, 34), 2.0: (30, 0, 20), 4.0: (30, 0, 20), 5.0: Z},
        'asaEsquerda': {0: Z, 1.8: (0, -30, 12), 2.0: (0, 14, 0), 4.0: (0, 14, 0), 5.0: Z},
        'asaDireita': {0: Z, 1.8: (0, 30, -12), 2.0: (0, -14, 0), 4.0: (0, -14, 0), 5.0: Z},
    })
    # O pisão: a perna direita sobe e desce aos 0,9 s.
    a['pisao'] = anim(2.3, {
        'body': {0: Z, .75: (-10, 0, 6), .9: (18, 0, 0), 1.4: (18, 0, 0), 2.3: Z},
        'rightLeg': {0: Z, .75: (-70, 0, 0), .9: Z, 2.3: Z},
        'leftArm': {0: Z, .75: (-40, 0, -40), .9: (20, 0, -20), 1.4: (20, 0, -20), 2.3: Z},
        'rightArm': {0: Z, .75: (-40, 0, 40), .9: (20, 0, 20), 1.4: (20, 0, 20), 2.3: Z},
        'root': {0: (Z, Z), .75: (Z, Z), .9: (Z, (0, -2, 0)), 1.2: (Z, Z), 2.3: (Z, Z)},
    })
    # A asa cortada (ela some com 1 s): ele se dobra para o lado dela, cambaleia e ruge.
    a['asa'] = anim(3.0, {
        'body': {0: Z, .2: (10, 0, 22), 1.0: (24, 0, 26), 1.8: (24, 0, 10), 2.2: (-14, 0, 0), 2.8: (-14, 0, 0), 3.0: Z},
        'head': {0: Z, .2: (-20, 0, 20), 1.8: (10, 0, 0), 2.2: (-30, 0, 0), 2.8: (-30, 0, 0), 3.0: Z},
        'mandibula': {0: Z, .2: (36, 0, 0), 1.0: (36, 0, 0), 1.8: (6, 0, 0), 2.2: (40, 0, 0), 2.8: (40, 0, 0), 3.0: Z},
        'asaEsquerda': {0: Z, .2: (0, 30, 40), 1.0: (0, 50, 70)},
        'asaDireita': {0: Z, .2: (0, 40, -10), 1.8: (0, 20, 0), 2.2: (0, 34, -16), 3.0: Z},
        'leftArm': {0: Z, .2: (-60, 40, 0), 1.8: (-40, 30, 0), 2.2: (10, 0, -44), 2.8: (10, 0, -44), 3.0: Z},
        'rightArm': {0: Z, 2.2: (10, 0, 44), 2.8: (10, 0, 44), 3.0: Z},
        'leftLeg': {0: Z, 1.0: (-18, 0, 0), 1.8: Z, 3.0: Z},
    })
    # Cai de joelhos e depois de frente.
    a['morte'] = anim(2.5, {
        'body': {0: Z, .6: (-16, 0, 0), 1.2: (20, 0, 0), 2.2: (30, 0, 0)},
        'head': {0: Z, .6: (-30, 0, 0), 1.2: (20, 0, 0), 2.2: (30, 0, 0)},
        'mandibula': {0: Z, .6: (40, 0, 0), 1.4: (10, 0, 0)},
        'leftArm': {0: Z, .6: (0, 0, -40), 2.2: (-20, 0, -20)},
        'rightArm': {0: Z, .6: (0, 0, 40), 2.2: (-20, 0, 20)},
        'asaEsquerda': {0: Z, 2.2: (0, 44, 20)},
        'asaDireita': {0: Z, 2.2: (0, -44, -20)},
        'leftLeg': {0: Z, 1.2: (-80, 0, 0), 2.2: (-80, 0, 0)},
        'rightLeg': {0: Z, 1.2: (-80, 0, 0), 2.2: (-80, 0, 0)},
        'root': {0: (Z, Z), .6: (Z, Z), 1.2: (Z, (0, -20, 0)), 2.2: ((60, 0, 0), (0, -26, 0)), 2.5: ((60, 0, 0), (0, -26, 0))},
    })
    (ASSETS / 'animations/boss').mkdir(parents=True, exist_ok=True)
    saida = ASSETS / 'animations/boss/ragnarok.animation.json'
    saida.write_text(json.dumps({'format_version': '1.8.0', 'animations': a}, indent=1), encoding='utf-8')


def icone():
    """O Anel da Sucessão 16x16: o aro de ouro e a pedra roxa em cima."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    ouro, ouro_claro, ouro_sombra, contorno = (226, 176, 56, 255), (252, 226, 122, 255), (158, 108, 28, 255), (84, 52, 14, 255)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 9.5) ** 2) ** .5
            if 3.2 <= d < 5.3:
                px[x, y] = ouro_claro if x + y < 15 else ouro_sombra if x + y > 19 else ouro
            elif 5.3 <= d < 6.2 or 2.4 <= d < 3.2:
                px[x, y] = contorno
    pedra = {(7, 2): 3, (8, 2): 3, (6, 3): 3, (7, 3): 1, (8, 3): 0, (9, 3): 3, (6, 4): 3, (7, 4): 0, (8, 4): 2, (9, 4): 3,
             (7, 5): 3, (8, 5): 3}
    cores = [(150, 70, 220, 255), (226, 170, 255, 255), (96, 36, 160, 255), (44, 14, 80, 255)]
    for (x, y), k in pedra.items():
        px[x, y] = cores[k]
    im.save(ASSETS / 'textures/item/anel_da_sucessao.png')


if __name__ == '__main__':
    textura()
    modelo()
    animacoes()
    icone()
    print('ok')
