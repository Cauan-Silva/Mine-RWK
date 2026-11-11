"""Trajes do Ichigo: o shihakushō do Shikai com a Zangetsu e o casaco do Bankai com a Tensa Zangetsu.

Rodar: python ferramentas/gerar_traje_ichigo.py
Lê de ferramentas/traje (cópias do Minezin, mesmo autor):
  ichigo_base.geo.json, ichigo_base.png                  geo/armaduras/ichigo.geo.json e a textura
  ichigo_bankai_base.geo.json, ichigo_bankai_base.png    geo/armaduras/ichigo_bankai.geo.json e a textura
  zangetsu_base.geo.json, tensa_zangetsu_base.geo.json   geo/ichigo/*.geo.json (as espadas)
e escreve em assets/minerwk:
  geo/traje/ichigo.geo.json, textures/traje/ichigo.png
  geo/traje/ichigo_bankai.geo.json, textures/traje/ichigo_bankai.png
A espada entra no modelo do traje, presa ao braço direito (osso "arma"): assim ela segue o braço em
terceira e em primeira pessoa sem item no inventário. No Minezin as faces da espada apontavam para manchas
de cor lisa de um atlas grande; aqui cada cor vira um quadradinho num canto livre da textura do traje.
Os nomes dos ossos repetem core/traje/TrajeIchigo.java; o TrajeIchigoTest confere.
"""
import json
from pathlib import Path
from PIL import Image

RAIZ = Path(__file__).resolve().parents[1]
BASE = RAIZ / 'ferramentas/traje'
ASSETS = RAIZ / 'src/main/resources/assets/minerwk'

# A mão direita no modelo do traje (o braço vai de y 12 a 24, centrado em x -6) e o tamanho da espada.
MAO = (-6.0, 12.6, 0.0)
ESCALA = 0.8
# Cor de cada mancha do atlas do Minezin, pelo canto (u, v) da face.
CORES = {
    (360.525, 987.525): (26, 21, 41, 255),      # tinta: cabo e lâmina escura
    (674.025, 47.025): (237, 232, 222, 255),    # marfim: o pano e os losangos do cabo
    (47.025, 360.525): (104, 111, 122, 255),    # aço
    (987.525, 674.025): (246, 244, 235, 255),   # o fio claro da Zangetsu
    (987.525, 47.025): (35, 25, 44, 255),       # preto da Tensa Zangetsu
    (674.025, 674.025): (182, 35, 51, 255),     # vermelho dos losangos e da corrente
}
LADO = 2  # cada cor ocupa LADO x LADO pixels


def bloco_livre(textura, largura, altura):
    """Canto de um retângulo todo transparente e fora de qualquer face usada (procura de baixo para cima)."""
    w, h = textura.size
    alfa = textura.getchannel('A').load()
    for y in range(h - altura, -1, -1):
        for x in range(w - largura, -1, -1):
            if all(alfa[x + i, y + j] == 0 for i in range(largura) for j in range(altura)):
                return x, y
    raise SystemExit('sem espaço livre na textura')


def mover(p):
    return [MAO[i] + p[i] * ESCALA for i in range(3)]


def espada(nome, textura):
    """Os ossos da espada, já no lugar da mão e com as faces apontando para a paleta pintada na textura."""
    ossos = json.loads((BASE / f'{nome}_base.geo.json').read_text(encoding='utf-8'))['minecraft:geometry'][0]['bones']
    usadas = sorted({tuple(f['uv']) for o in ossos for c in o.get('cubes', []) for f in c['uv'].values()})
    x0, y0 = bloco_livre(textura, LADO * len(usadas), LADO)
    onde = {}
    for i, uv in enumerate(usadas):
        for dx in range(LADO):
            for dy in range(LADO):
                textura.putpixel((x0 + i * LADO + dx, y0 + dy), CORES[uv])
        onde[uv] = [x0 + i * LADO + 0.5, y0 + 0.5]
    saida = [
        # "arma" deita a espada para a frente da mão, com a ponta um pouco erguida; "arma_fio" vira o fio
        # para baixo, mas não de todo: assim a face larga da lâmina aparece para quem olha de cima (1ª pessoa).
        {'name': 'arma', 'parent': 'armorRightArm', 'pivot': list(MAO), 'rotation': [78, 0, 0]},
        {'name': 'arma_fio', 'parent': 'arma', 'pivot': list(MAO), 'rotation': [0, 62, 0]},
    ]
    for o in ossos:
        novo = {'name': 'arma_' + o['name'], 'parent': 'arma_fio' if o['name'] == 'root' else 'arma_' + o['parent'],
                'pivot': mover(o['pivot'])}
        if 'rotation' in o:
            novo['rotation'] = o['rotation']
        cubos = []
        for c in o.get('cubes', []):
            cubo = {'origin': mover(c['origin']), 'size': [s * ESCALA for s in c['size']],
                    'uv': {face: {'uv': onde[tuple(f['uv'])], 'uv_size': [1, 1]} for face, f in c['uv'].items()}}
            if 'rotation' in c:
                cubo['rotation'] = c['rotation']
                cubo['pivot'] = mover(c['pivot'])
            if 'inflate' in c:
                cubo['inflate'] = c['inflate'] * ESCALA
            cubos.append(cubo)
        novo['cubes'] = cubos
        saida.append(novo)
    return saida


def gerar(traje, arma):
    dados = json.loads((BASE / f'{traje}_base.geo.json').read_text(encoding='utf-8'))
    geo = dados['minecraft:geometry'][0]
    textura = Image.open(BASE / f'{traje}_base.png').convert('RGBA')
    assert textura.size == (geo['description']['texture_width'], geo['description']['texture_height'])
    geo['description']['identifier'] = f'geometry.{traje}'
    nomes = {o['name'] for o in geo['bones']}
    novos = espada(arma, textura)
    assert not nomes & {o['name'] for o in novos}
    geo['bones'] += novos
    (ASSETS / 'geo/traje').mkdir(parents=True, exist_ok=True)
    (ASSETS / 'textures/traje').mkdir(parents=True, exist_ok=True)
    (ASSETS / f'geo/traje/{traje}.geo.json').write_text(json.dumps(dados, indent=1) + '\n', encoding='utf-8')
    textura.save(ASSETS / f'textures/traje/{traje}.png')
    print(f'Traje {traje}: {len(geo["bones"])} ossos, {len(novos)} da espada {arma}; textura {textura.size}')


if __name__ == '__main__':
    gerar('ichigo', 'zangetsu')
    gerar('ichigo_bankai', 'tensa_zangetsu')
