"""A forma Hollow completa do Ichigo (o Vasto Lorde): o traje que cobre o jogador inteiro durante os 10 s da forma.

Rodar na raiz do projeto: python ferramentas/gerar_traje_hollow.py
Escreve em assets/minerwk:
  geo/traje/ichigo_hollow.geo.json               o modelo, nos ossos de armadura do GeckoLib
  textures/traje/ichigo_hollow.png               a textura (128x128), pintada aqui
  animations/traje/ichigo_hollow.animation.json  vazio: quem anima é o jogador
Feito aqui, sem asset de terceiros. A obra: pele branca; a máscara de listras pretas com os dois chifres que
saem para os lados e dobram para a frente; a gola de pelo vermelho; o buraco no peito com as marcas pretas
que vão até os ombros; a manga preta rasgada só no braço direito; os tufos vermelhos nos pulsos e nos
tornozelos; as garras; a faixa branca na cintura e o hakama preto rasgado, mais curto na perna esquerda.
Sem o cabelo longo, a pedido do autor (12/11). A Tensa Zangetsu na mão direita é a do traje do Bankai
(gerar_traje_ichigo.espada), no osso "arma".
O nome do traje repete core/traje/TrajeIchigo.HOLLOW; o TrajeIchigoTest confere os ossos.
"""
import json
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gerar_traje_ichigo as bankai  # noqa: E402

RAIZ = Path(__file__).resolve().parents[1]
ASSETS = RAIZ / 'src/main/resources/assets/minerwk'
LADO = 128

PELE = (244, 242, 238, 255)
PELE_SOMBRA = (190, 188, 197, 255)
OSSO = (248, 245, 236, 255)
OSSO_SOMBRA = (214, 208, 198, 255)
PRETO = (16, 14, 20, 255)
PANO = (25, 22, 31, 255)
PANO_CLARO = (44, 39, 54, 255)
IRIS = (214, 168, 40, 255)
RUBRO = (190, 30, 36, 255)
RUBRO_CLARO = (232, 78, 52, 255)
RUBRO_ESCURO = (128, 16, 30, 255)
FAIXA = (234, 232, 226, 255)
FAIXA_SOMBRA = (196, 194, 192, 255)
NADA_COR = (0, 0, 0, 0)

# Quadros pintados (canto, largura, altura): o dobro da resolução do cubo, para caber o desenho.
MASCARA = (0, 0, 16, 16)
TOPO = (16, 0, 16, 16)
TEMPORA = (32, 0, 16, 16)
NUCA = (48, 0, 16, 16)
MANGA = (64, 0, 8, 20)
BARRA = (72, 0, 8, 24)
BARRA_CURTA = (80, 0, 8, 24)
BRACO = (88, 0, 8, 24)
PERNA = (96, 0, 8, 24)
PEITO = (0, 16, 16, 24)
COSTAS = (16, 16, 16, 24)
LATERAL = (32, 16, 8, 24)
PELO = (48, 24, 16, 8)
PELO_BAIXO = (48, 32, 16, 8)
NADA = (110, 60, 1, 1)
# Cores chapadas (um quadrado 2x2).
COR_PELE, COR_OSSO, COR_PRETO, COR_RUBRO, COR_PANO, COR_FAIXA, COR_SOMBRA, COR_FAIXA_SOMBRA = (
    (0, 100), (2, 100), (4, 100), (6, 100), (8, 100), (10, 100), (12, 100), (14, 100))


def textura():
    im = Image.new('RGBA', (LADO, LADO), NADA_COR)
    px = im.load()

    def encher(quadro, cor):
        x0, y0, w, h = quadro
        for y in range(h):
            for x in range(w):
                px[x0 + x, y0 + y] = cor(x, y) if callable(cor) else cor

    def pontos(quadro, cor, lista):
        for x, y in lista:
            px[quadro[0] + x, quadro[1] + y] = cor

    def espelhado(quadro, cor, lista):
        w = quadro[2]
        pontos(quadro, cor, lista)
        pontos(quadro, cor, [(w - 1 - x, y) for x, y in lista])

    def linha(quadro, cor, de, ate, grossura=1):
        """Uma linha de {@code de} a {@code ate}, espelhada, com a grossura para baixo."""
        (x1, y1), (x2, y2) = de, ate
        n = max(abs(x2 - x1), abs(y2 - y1))
        for i in range(n + 1):
            x = round(x1 + (x2 - x1) * i / n)
            y = round(y1 + (y2 - y1) * i / n)
            espelhado(quadro, cor, [(x, y + g) for g in range(grossura)])

    # A máscara: osso, duas listras que descem da testa pelos olhos até o queixo, duas mais curtas no meio,
    # as órbitas pretas rasgadas e os dentes cerrados.
    encher(MASCARA, lambda x, y: OSSO_SOMBRA if x in (0, 15) or y == 15 else OSSO)
    espelhado(MASCARA, PRETO, [(x, y) for y in range(0, 6) for x in (2, 3)])
    espelhado(MASCARA, PRETO, [(x, y) for y in range(0, 4) for x in (5, 6)] + [(6, 4)])
    espelhado(MASCARA, PRETO, [(2, 6), (3, 6), (4, 6), (2, 7), (3, 7), (4, 7), (5, 7), (3, 8), (4, 8), (5, 8)])
    espelhado(MASCARA, IRIS, [(4, 7)])
    espelhado(MASCARA, PRETO, [(3, y) for y in range(9, 16)])
    espelhado(MASCARA, OSSO_SOMBRA, [(7, 9)])
    for x in range(4, 12):
        px[x, 11] = PRETO
        px[x, 14] = PRETO
        for y in (12, 13):
            px[x, y] = OSSO if x % 2 == 0 else PRETO
    # O alto e a nuca: as listras continuam de frente para trás.
    encher(TOPO, lambda x, y: PRETO if x in (2, 3, 5, 6, 9, 10, 12, 13) else OSSO)
    encher(NUCA, lambda x, y: PRETO if (x in (2, 3, 12, 13) and y < 9) or (x in (5, 6, 9, 10) and y < 5)
           else (OSSO_SOMBRA if y >= 13 or x in (0, 15) else OSSO))
    encher(TEMPORA, lambda x, y: OSSO_SOMBRA if y >= 13 or (x + y) % 7 == 0 else OSSO)
    for y in range(0, 2):
        for x in range(16):
            px[TEMPORA[0] + x, TEMPORA[1] + y] = PRETO if x % 5 < 2 else OSSO

    # O peito: o buraco, as duas marcas de cada lado que vão até os ombros, as do pescoço, e o abdômen.
    encher(PEITO, lambda x, y: PELE_SOMBRA if x in (0, 15) else PELE)
    linha(PEITO, PRETO, (5, 8), (0, 4), 2)
    linha(PEITO, PRETO, (5, 11), (0, 10), 2)
    linha(PEITO, PRETO, (6, 6), (5, 3))
    espelhado(PEITO, PRETO, [(7, 12), (7, 13)])
    for y in range(24):
        for x in range(16):
            if (x - 7.5) ** 2 + (y - 9.5) ** 2 <= 7.4:
                px[PEITO[0] + x, PEITO[1] + y] = PRETO
    espelhado(PEITO, PELE_SOMBRA, [(x, 14) for x in range(2, 7)])
    espelhado(PEITO, PELE_SOMBRA, [(7, y) for y in range(15, 24)])
    for y in (16, 18):
        espelhado(PEITO, PELE_SOMBRA, [(x, y) for x in range(4, 8)])
    espelhado(PEITO, PELE_SOMBRA, [(2, 16), (3, 17), (2, 18)])
    # As costas e os lados: as marcas dão a volta nos ombros.
    encher(COSTAS, lambda x, y: PELE_SOMBRA if x in (0, 7, 8, 15) or y == 23 else PELE)
    linha(COSTAS, PRETO, (4, 4), (0, 1), 2)
    linha(COSTAS, PRETO, (3, 8), (0, 7), 2)
    espelhado(COSTAS, PELE_SOMBRA, [(3, 11), (4, 12), (5, 12)])
    encher(LATERAL, lambda x, y: PRETO if y in (1, 2, 7, 8) else (PELE_SOMBRA if x in (0, 7) else PELE))
    # O braço e a perna: pele, com a marca do ombro e as sombras.
    encher(BRACO, lambda x, y: PRETO if y in (2, 3) else (PELE_SOMBRA if x in (0, 7) or y in (11, 19) else PELE))
    encher(PERNA, lambda x, y: PELE_SOMBRA if x in (0, 7) or y == 11 else PELE)
    # O pelo vermelho: chamas que sobem (ou descem), claras nas pontas.
    alturas = [3, 6, 4, 8, 5, 7, 3, 6, 8, 4, 6, 3, 7, 5, 8, 4]

    def pelo(x, y, alto):
        if y < 8 - alto:
            return NADA_COR
        if y < 8 - alto + 2:
            return RUBRO_CLARO
        return RUBRO_ESCURO if x % 4 == 2 and y >= 5 else RUBRO
    encher(PELO, lambda x, y: pelo(x, y, alturas[x]))
    encher(PELO_BAIXO, lambda x, y: pelo(x, 7 - y, alturas[x]))
    # O pano preto rasgado: a manga e as duas barras do hakama (a da perna esquerda é mais curta).
    def pano(compridos):
        return lambda x, y: NADA_COR if y >= compridos[x] else (PANO_CLARO if x % 3 == 1 and y % 5 != 4 else PANO)
    encher(MANGA, pano([20, 15, 18, 13, 19, 14, 17, 20]))
    encher(BARRA, pano([24, 20, 23, 18, 22, 24, 19, 23]))
    encher(BARRA_CURTA, pano([17, 11, 14, 8, 12, 16, 9, 13]))
    for canto, cor in ((COR_PELE, PELE), (COR_OSSO, OSSO), (COR_PRETO, PRETO), (COR_RUBRO, RUBRO), (COR_PANO, PANO),
                       (COR_FAIXA, FAIXA), (COR_SOMBRA, OSSO_SOMBRA), (COR_FAIXA_SOMBRA, FAIXA_SOMBRA)):
        for y in range(2):
            for x in range(2):
                px[canto[0] + x, canto[1] + y] = cor
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


def em_volta(quadro):
    """As quatro faces de lado com o mesmo quadro, e as tampas transparentes."""
    return dict(north=quadro, south=quadro, east=quadro, west=quadro, up=NADA, down=NADA)


def chifre(lado):
    """Sai do lado da máscara, sobe, dobra para a frente e desce afinando até a ponta."""
    def x(de, largura):
        return de if lado > 0 else -de - largura
    dobra = [7.6 * lado, 29.6, -6.0]
    return [
        cubo([x(4.4, 3.2), 26.2, -1.9], [3.2, 2.6, 2.8], COR_OSSO),
        cubo([x(4.55, .5), 26.0, -2.1], [.5, 3.0, 3.2], COR_PRETO),
        cubo([x(6.4, 2.3), 27.4, -1.7], [2.3, 3.0, 2.4], COR_OSSO),
        cubo([x(6.6, 2.0), 28.6, -6.4], [2.0, 2.0, 5.6], COR_OSSO),
        cubo([x(6.9, 1.5), 28.75, -11.6], [1.5, 1.6, 5.6], COR_OSSO, giro=[26, 0, 0], pivo=dobra),
        cubo([x(7.2, .9), 29.0, -15.6], [.9, 1.0, 4.2], COR_SOMBRA, giro=[26, 0, 0], pivo=dobra),
    ]


def braco(lado):
    """Pele branca, as chamas vermelhas subindo do pulso e as garras; o direito ainda tem a manga rasgada."""
    x0 = 4 if lado > 0 else -8
    cubos = [cubo([x0, 12, -2], [4, 12, 4], COR_PELE, inflar=.25, north=BRACO, south=BRACO, east=BRACO, west=BRACO),
             cubo([x0 - .6, 12.7, -2.6], [5.2, 2.9, 5.2], COR_RUBRO, **em_volta(PELO))]
    for k in range(4):
        cubos.append(cubo([x0 + .25 + k * .95, 10.0, -1.9 + (k % 2) * .5], [.6, 1.9, .7], COR_PRETO))
    if lado < 0:
        cubos.append(cubo([x0, 14.6, -2], [4, 9.6, 4], COR_PANO, inflar=.5,
                          north=MANGA, south=MANGA, east=MANGA, west=MANGA, down=NADA))
    return cubos


def perna(lado):
    """O hakama rasgado (mais curto na esquerda), as chamas no tornozelo e o pé de garras."""
    x0 = 0 if lado > 0 else -4
    barra = BARRA_CURTA if lado > 0 else BARRA
    fora = x0 - .1 if lado > 0 else x0 - .7
    cubos = [cubo([x0, 0, -2], [4, 12, 4], COR_PELE, inflar=.1, north=PERNA, south=PERNA, east=PERNA, west=PERNA),
             cubo([x0 - .6, 1.0, -2.6], [5.2, 3.2, 5.2], COR_RUBRO, **em_volta(PELO)),
             cubo([fora, .4, -2.6], [4.8, 11.6, 5.2], COR_PANO, north=barra, south=barra, east=barra, west=barra, down=NADA)]
    for k in range(3):
        cubos.append(cubo([x0 + .45 + k * 1.2, 0, -3.3], [.7, .8, 1.3], COR_PRETO))
    return cubos


def gola():
    """A gola de pelo: o anel no pescoço, as pontas nos ombros, o V que desce até o buraco e a ponta das costas."""
    cubos = [cubo([-5, 22.3, -3], [10, 2.8, 6], COR_RUBRO, **em_volta(PELO))]
    for lado in (1, -1):
        cubos.append(cubo([4.2 if lado > 0 else -7.8, 21.4, -2.8], [3.6, 3.4, 5.6], COR_RUBRO,
                          giro=[0, 0, -24 * lado], pivo=[4.4 * lado, 22.6, 0], **em_volta(PELO)))
        cubos.append(cubo([2.0 if lado > 0 else -4.8, 20.6, -3.25], [2.8, 3.0, .9], COR_RUBRO,
                          giro=[0, 0, 22 * lado], pivo=[3.4 * lado, 23, -2.8], north=PELO_BAIXO, south=PELO_BAIXO,
                          east=NADA, west=NADA, up=NADA, down=NADA))
    cubos.append(cubo([-4.4, 19.2, 2.3], [8.8, 4.6, .9], COR_RUBRO, north=PELO_BAIXO, south=PELO_BAIXO,
                      east=NADA, west=NADA, up=NADA, down=NADA))
    return cubos


def modelo(im):
    ossos = [
        {'name': 'armorHead', 'pivot': [0, 24, 0], 'cubes': [
            cubo([-4, 24, -4], [8, 8, 8], COR_OSSO, inflar=.55, north=MASCARA, south=NUCA, east=TEMPORA, west=TEMPORA, up=TOPO),
            *chifre(1), *chifre(-1),
        ]},
        {'name': 'armorBody', 'pivot': [0, 24, 0], 'cubes': [
            cubo([-4, 12, -2], [8, 12, 4], COR_PELE, inflar=.3, north=PEITO, south=COSTAS, east=LATERAL, west=LATERAL),
            *gola(),
            # A faixa branca da cintura, com o laço e as duas pontas caídas.
            cubo([-4, 12, -2], [8, 1.4, 4], COR_PANO, inflar=.5),
            cubo([-4, 13.1, -2], [8, 1.3, 4], COR_FAIXA, inflar=.62),
            cubo([-1.3, 12.9, -3.6], [2.6, 1.8, .9], COR_FAIXA_SOMBRA),
            cubo([-2.0, 8.8, -3.3], [1.1, 3.8, .5], COR_FAIXA, giro=[0, 0, -10], pivo=[-1.4, 12.6, -3]),
            cubo([.9, 9.8, -3.3], [1.1, 2.8, .5], COR_FAIXA, giro=[0, 0, 12], pivo=[1.4, 12.6, -3]),
        ]},
        {'name': 'armorLeftArm', 'pivot': [5, 22, 0], 'cubes': braco(1)},
        {'name': 'armorRightArm', 'pivot': [-5, 22, 0], 'cubes': braco(-1)},
        {'name': 'armorLeftLeg', 'pivot': [2, 12, 0], 'cubes': perna(1)},
        {'name': 'armorRightLeg', 'pivot': [-2, 12, 0], 'cubes': perna(-1)},
        {'name': 'armorLeftBoot', 'pivot': [2, 12, 0]},
        {'name': 'armorRightBoot', 'pivot': [-2, 12, 0]},
    ]
    arma = bankai.espada('tensa_zangetsu', im)
    ossos += arma
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.ichigo_hollow', 'texture_width': LADO, 'texture_height': LADO,
                        'visible_bounds_width': 6, 'visible_bounds_height': 5, 'visible_bounds_offset': [0, 1.5, 0]},
        'bones': ossos}]}
    (ASSETS / 'geo/traje').mkdir(parents=True, exist_ok=True)
    (ASSETS / 'geo/traje/ichigo_hollow.geo.json').write_text(json.dumps(geo, indent=1) + '\n', encoding='utf-8')
    (ASSETS / 'animations/traje/ichigo_hollow.animation.json').write_text(
        json.dumps({'format_version': '1.8.0', 'animations': {}}, indent=2) + '\n', encoding='utf-8')
    print(f'Traje ichigo_hollow: {len(ossos)} ossos ({len(arma)} da espada), '
          f'{sum(len(o.get("cubes", [])) for o in ossos)} cubos')


if __name__ == '__main__':
    textura_pintada = textura()
    modelo(textura_pintada)
    (ASSETS / 'textures/traje').mkdir(parents=True, exist_ok=True)
    textura_pintada.save(ASSETS / 'textures/traje/ichigo_hollow.png')
