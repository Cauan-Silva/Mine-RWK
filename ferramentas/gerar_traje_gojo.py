"""Traje do Gojo: parte do modelo e da textura do Minezin (mesmo autor), troca os óculos pela venda e põe os olhos.

Rodar: python ferramentas/gerar_traje_gojo.py
Lê ferramentas/traje/gojo_base.geo.json e gojo_base.png (cópias de geo/armaduras/gojo.geo.json e
textures/armor/trajes/gojo.png do Minezin) e escreve em assets/minerwk:
  geo/traje/gojo.geo.json              o uniforme, com os ossos "venda" e "olhos" na cabeça e sem os óculos
  textures/traje/gojo.png              a textura com o pano da venda e a cor dos olhos
  textures/traje/gojo_glowmask.png     só os pixels dos olhos (o GeckoLib os desenha brilhando no escuro)
Os nomes dos ossos repetem core/traje/TrajeGojo.java; o TrajeGojoTest confere.
"""
import json
from pathlib import Path
from PIL import Image

RAIZ = Path(__file__).resolve().parents[1]
BASE = RAIZ / 'ferramentas/traje'
ASSETS = RAIZ / 'src/main/resources/assets/minerwk'

# Bloco livre (transparente) da textura do Minezin onde entram o pano da venda e os olhos.
LIVRE = (30, 4)
PANO = (10, 3)    # largura e altura do pano, em pixels
PRETO, DOBRA, SOMBRA = (11, 12, 15, 255), (30, 32, 38, 255), (5, 6, 8, 255)
OLHO = (159, 232, 255, 255)


def faces(u, v, w, h):
    """O mesmo retalho da textura em todas as faces do cubo."""
    return {f: {'uv': [u, v], 'uv_size': [w, h]} for f in ('north', 'south', 'east', 'west', 'up', 'down')}


def gerar():
    dados = json.loads((BASE / 'gojo_base.geo.json').read_text(encoding='utf-8'))
    geo = dados['minecraft:geometry'][0]
    ossos = geo['bones']
    cabeca = next(o for o in ossos if o['name'] == 'armorHead')
    cabeca['cubes'] = []  # os óculos redondos do Minezin saem; o traje não tem cabelo

    textura = Image.open(BASE / 'gojo_base.png').convert('RGBA')
    x0, y0 = LIVRE
    assert textura.crop((x0, y0, x0 + 16, y0 + 8)).getextrema()[3][1] == 0, 'o bloco da venda não está livre'
    for x in range(PANO[0]):
        for y in range(PANO[1]):
            textura.putpixel((x0 + x, y0 + y), DOBRA if y == 0 else SOMBRA if y == PANO[1] - 1 else PRETO)
    ox, oy = x0 + 12, y0
    for x in range(2):
        textura.putpixel((ox + x, oy), OLHO)

    pano = faces(x0, y0, PANO[0], PANO[1])
    # A cabeça da skin vai de -4 a 4 e a camada de chapéu até 4,5: a venda passa por fora dela.
    ossos.append({'name': 'venda', 'parent': 'armorHead', 'pivot': [0, 28, 0], 'cubes': [
        {'origin': [-4.6, 26.6, -4.6], 'size': [9.2, 2.4, 9.2], 'uv': pano},
        # As duas pontas do nó, caindo atrás da cabeça.
        {'origin': [-1.3, 24.3, 4.6], 'size': [1.0, 2.5, 0.3], 'uv': pano},
        {'origin': [0.3, 24.9, 4.6], 'size': [1.0, 1.9, 0.3], 'uv': pano},
    ]})
    # Os olhos ficam na linha dos olhos de uma skin padrão, logo à frente da camada de chapéu.
    olho = faces(ox, oy, 2, 1)
    ossos.append({'name': 'olhos', 'parent': 'armorHead', 'pivot': [0, 28, 0], 'cubes': [
        {'origin': [-3.0, 27.0, -4.56], 'size': [2.0, 1.0, 0.04], 'uv': olho},
        {'origin': [1.0, 27.0, -4.56], 'size': [2.0, 1.0, 0.04], 'uv': olho},
    ]})

    brilho = Image.new('RGBA', textura.size, (0, 0, 0, 0))
    for x in range(2):
        brilho.putpixel((ox + x, oy), OLHO)

    (ASSETS / 'geo/traje').mkdir(parents=True, exist_ok=True)
    (ASSETS / 'textures/traje').mkdir(parents=True, exist_ok=True)
    (ASSETS / 'geo/traje/gojo.geo.json').write_text(json.dumps(dados, indent=1) + '\n', encoding='utf-8')
    textura.save(ASSETS / 'textures/traje/gojo.png')
    brilho.save(ASSETS / 'textures/traje/gojo_glowmask.png')
    print('Traje do Gojo:', len(ossos), 'ossos; venda e olhos na cabeça; textura', textura.size)


if __name__ == '__main__':
    gerar()
