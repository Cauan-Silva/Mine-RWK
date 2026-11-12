"""Desenha um traje (geo do GeckoLib + textura) fora do jogo, em vistas ortográficas, para conferir o modelo.

Uso, na raiz do projeto: python ferramentas/previa_traje.py ichigo_hollow [saida.png] [giros em graus...]
Sem giros, desenha de frente, três quartos, lado e costas. Os giros dos cubos e dos ossos seguem a conta do
GeckoLib (X espelhado; em coordenadas do arquivo isso dá -rx, +ry, -rz, na ordem Z·Y·X).
O corpo do jogador não entra: só o que o traje desenha.
"""
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

RAIZ = Path(__file__).resolve().parents[1]
ASSETS = RAIZ / 'src/main/resources/assets/minerwk'
PX = 16          # pixels por unidade do modelo
LARGURA, ALTURA = 30, 38   # unidades do modelo que cabem em cada vista
FUNDO = (120, 150, 190)


def giro(rot):
    rx, ry, rz = (math.radians(a) for a in (-rot[0], rot[1], -rot[2]))
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    mx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    my = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    mz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return mz @ my @ mx


def em_torno(pontos, rot, pivo):
    p = np.array(pivo, float)
    return (pontos - p) @ giro(rot).T + p


def faces_do_cubo(c):
    o = np.array(c['origin'], float)
    s = np.array(c['size'], float)
    i = c.get('inflate', 0)
    a, b = o - i, o + s + i
    x0, y0, z0 = a
    x1, y1, z1 = b
    # Cada face: quatro cantos na ordem (alto-esq, alto-dir, baixo-dir, baixo-esq) de quem olha de fora.
    return {
        'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        'east': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        'west': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        'up': [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
        'down': [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
    }


def quadrilateros(geo):
    ossos = {o['name']: o for o in geo['bones']}
    saida = []
    for osso in geo['bones']:
        cadeia = []
        o = osso
        while o is not None:
            cadeia.append(o)
            o = ossos.get(o.get('parent'))
        for c in osso.get('cubes', []):
            for face, cantos in faces_do_cubo(c).items():
                if face not in c['uv']:
                    continue
                p = np.array(cantos, float)
                if 'rotation' in c:
                    p = em_torno(p, c['rotation'], c['pivot'])
                for o in cadeia:
                    if 'rotation' in o:
                        p = em_torno(p, o['rotation'], o['pivot'])
                uv = c['uv'][face]
                saida.append((p, uv['uv'], uv['uv_size']))
    return saida


def desenhar(quads, textura, graus, inclinacao=12):
    w, h = LARGURA * PX, ALTURA * PX
    cor = np.zeros((h, w, 3), np.uint8)
    cor[:] = FUNDO
    fundo = np.full((h, w), -1e9)
    tex = np.asarray(textura)
    th, tw = tex.shape[:2]
    vista = giro([0, graus, 0])
    vista = giro([-inclinacao, 0, 0]) @ vista
    for p, uv0, uvs in quads:
        q = p @ vista.T
        # A câmera olha de -z para +z: o que tem z menor fica na frente.
        # O GeckoLib espelha o X do arquivo: o braço direito (x negativo) aparece à esquerda de quem olha de frente.
        sx = (q[:, 0] + LARGURA / 2) * PX
        sy = (ALTURA - 4 - q[:, 1]) * PX
        prof = -q[:, 2]
        uvc = np.array([[0, 0], [1, 0], [1, 1], [0, 1]], float)
        for tri in ((0, 1, 2), (0, 2, 3)):
            ax, ay, bx, by, cx_, cy_ = sx[tri[0]], sy[tri[0]], sx[tri[1]], sy[tri[1]], sx[tri[2]], sy[tri[2]]
            area = (bx - ax) * (cy_ - ay) - (cx_ - ax) * (by - ay)
            if abs(area) < 1e-6:
                continue
            xa, xb = int(max(0, math.floor(min(ax, bx, cx_)))), int(min(w - 1, math.ceil(max(ax, bx, cx_))))
            ya, yb = int(max(0, math.floor(min(ay, by, cy_)))), int(min(h - 1, math.ceil(max(ay, by, cy_))))
            if xa > xb or ya > yb:
                continue
            gx, gy = np.meshgrid(np.arange(xa, xb + 1) + .5, np.arange(ya, yb + 1) + .5)
            l1 = ((bx - gx) * (cy_ - gy) - (cx_ - gx) * (by - gy)) / area
            l2 = ((cx_ - gx) * (ay - gy) - (ax - gx) * (cy_ - gy)) / area
            l3 = 1 - l1 - l2
            dentro = (l1 >= 0) & (l2 >= 0) & (l3 >= 0)
            z = l1 * prof[tri[0]] + l2 * prof[tri[1]] + l3 * prof[tri[2]]
            u = l1 * uvc[tri[0], 0] + l2 * uvc[tri[1], 0] + l3 * uvc[tri[2], 0]
            v = l1 * uvc[tri[0], 1] + l2 * uvc[tri[1], 1] + l3 * uvc[tri[2], 1]
            tx = np.clip((uv0[0] + u * uvs[0]).astype(int), 0, tw - 1)
            ty = np.clip((uv0[1] + v * uvs[1]).astype(int), 0, th - 1)
            amostra = tex[ty, tx]
            ok = dentro & (amostra[..., 3] > 127) & (z > fundo[ya:yb + 1, xa:xb + 1])
            fundo[ya:yb + 1, xa:xb + 1][ok] = z[ok]
            cor[ya:yb + 1, xa:xb + 1][ok] = amostra[..., :3][ok]
    return Image.fromarray(cor)


def main():
    nome = sys.argv[1]
    saida = sys.argv[2] if len(sys.argv) > 2 else f'previas/traje_{nome}.png'
    graus = [float(g) for g in sys.argv[3:]] or [0, 35, 90, 180]
    geo = json.loads((ASSETS / f'geo/traje/{nome}.geo.json').read_text(encoding='utf-8'))['minecraft:geometry'][0]
    textura = Image.open(ASSETS / f'textures/traje/{nome}.png').convert('RGBA')
    quads = quadrilateros(geo)
    vistas = [desenhar(quads, textura, g) for g in graus]
    folha = Image.new('RGB', (sum(v.width for v in vistas), vistas[0].height))
    x = 0
    for v in vistas:
        folha.paste(v, (x, 0))
        x += v.width
    Path(saida).parent.mkdir(parents=True, exist_ok=True)
    folha.save(saida)
    print(saida, folha.size)


if __name__ == '__main__':
    main()
