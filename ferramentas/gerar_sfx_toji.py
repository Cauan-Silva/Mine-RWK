"""Sons do Toji: síntese do motor em camadas (ferramentas/sfx_motor.py) com gravações CC0 por cima (gravacoes.py).

Rodar: python ferramentas/gerar_sfx_toji.py [--preview caminho.wav]
Escreve os .ogg em assets/minerwk/sounds/toji/, as entradas do sounds.json, as legendas e o manifesto
ferramentas/sfx_toji_manifesto.json. Os ids repetem core/boss/SonsToji.java (o SonsTojiTest confere).

O Toji não tem energia amaldiçoada: os sons são secos e físicos (impacto, metal, corrente, ar cortado),
sem os zumbidos e tons vítreos dos sons do Gojo. Só o despertar, que é do Gojo, tem tom.
"""
import argparse
import numpy as np
import sfx_motor as m
from gravacoes import *  # noqa: F401,F403 (as listas de gravações)
from gravacoes import _k

T = m.TICK
QUEDA = 60 * T   # TojiBoss.QUEDA: o tempo no chão antes de se levantar
BATIDAS = (.25, .95, 1.6, 2.15, 2.6)  # o coração de quem caiu, acelerando até o despertar

# nome -> (duração em s, alcance em blocos, legenda pt, legenda en)
SONS = {
    'toji_entrada': (1.6, 48, 'Toji chega', 'Toji arrives'),
    'toji_aviso': (.35, 32, 'Toji prepara um golpe', 'Toji readies a strike'),
    'toji_investida': (.7, 48, 'Toji investe', 'Toji dashes'),
    'toji_lanca': (.6, 48, 'A Lança Invertida do Céu estoca', 'The Inverted Spear of Heaven thrusts'),
    'toji_anula': (1.1, 48, 'O Infinito é desfeito', 'Infinity is undone'),
    'toji_corrente': (.9, 48, 'A corrente estica', 'The chain snaps taut'),
    'toji_soco': (.5, 32, 'Toji acerta um golpe', 'Toji lands a blow'),
    'toji_nuvem': (.45, 32, 'A Nuvem Brincalhona bate', 'Playful Cloud strikes'),
    'toji_sumir': (.6, 32, 'Toji some', 'Toji vanishes'),
    'toji_surgir': (.5, 32, 'Toji surge', 'Toji reappears'),
    'toji_marca': (1.0, 48, 'Toji marca o alvo', 'Toji marks the target'),
    'toji_fase': (1.8, 64, 'Toji fica sério', 'Toji gets serious'),
    'toji_queda': (QUEDA + .3, 48, 'Um coração bate devagar', 'A heart beats slowly'),
    'toji_despertar': (2.2, 64, 'Gojo desperta', 'Gojo awakens'),
    'toji_morte': (1.8, 64, 'Toji cai', 'Toji falls'),
}

# Marcos em ticks, conferidos contra o Java pelo SonsTojiTest.
MARCOS = {'toji_queda': {'levanta': 60}}


def metal(s, ini, f, queda=18., forca=1.):
    """Batida de metal: parciais desafinados que morrem rápido, com o estalo do contato."""
    x = s.tom(f, harmonicos=(1, 2.32, 3.87, 5.13), pesos=(1, .6, .35, .2)) * s.env(ini, .0008, queda)
    return forca * (x + .5 * s.transiente(ini, 9000, .004))


def sopro(s, ini, ataque, queda, lo=300, hi=6000, cor=.3):
    """Ar cortado: ruído em banda com um envelope curto."""
    return s.ruido(lo, hi, cor) * s.env(ini, ataque, queda)


def sintetico(nome, s):
    if nome == 'toji_entrada':
        # Ar descendo e o baque dos pés no chão.
        ar = .45 * sopro(s, 0, .28, 9, 200, 3000, .5)
        baque = 1.1 * s.sub(.3, 85, 30, 2.6, 3.5) + .7 * s.transiente(.3, 3500, .02)
        po = .3 * s.passa_baixa(s.ruido(80, 2500, .7), 900) * s.env(.31, .03, 3.5)
        return ar + s.reverb(baque + po, 1.1, .22, 3000)
    if nome == 'toji_aviso':
        # O clique seco da mão fechando no cabo.
        return .9 * metal(s, 0, 1900, 30) + .5 * metal(s, .06, 2500, 40, .6) + .15 * sopro(s, 0, .002, 40, 2000, 9000)
    if nome == 'toji_investida':
        # Arranque: o chão cedendo e o ar rasgado.
        arranque = .8 * s.sub(0, 95, 40, 5, 3) + .6 * s.transiente(0, 4000, .012)
        ar = .9 * sopro(s, .02, .05, 6, 400, 7000, .2) * (1 + .5 * s.crescer(0, .15, 1))
        return arranque + s.reverb(ar, .6, .15, 5000)
    if nome == 'toji_lanca':
        # Estocada: assobio curto e agudo da lâmina e o metal no fim do curso.
        assobio = .8 * sopro(s, 0, .012, 16, 1800, 12000, 0)
        return assobio + .7 * metal(s, .07, 3100, 22) + .4 * s.sub(.07, 120, 60, 9, 2)
    if nome == 'toji_anula':
        # A barreira estala e se desfaz em silêncio: estalos secos e o ar voltando, sem tom.
        estalo = 1.0 * s.transiente(0, 8000, .006) + .5 * s.estalos(0, .35, 110, 7000) * s.env(0, .005, 6)
        vazio = .55 * s.passa_baixa(s.ruido(40, 1500, .9), 500) * s.env(.02, .12, 3.2)
        return estalo + .5 * metal(s, 0, 2700, 16) + s.reverb(vazio + .6 * s.sub(.02, 70, 30, 3.5, 2.5), 1.0, .25, 2500)
    if nome == 'toji_corrente':
        # Elos correndo e a corrente esticando de uma vez.
        elos = .5 * s.estalos(0, .4, 90, 3500) + sum(.3 * metal(s, d, f, 45) for d, f in
                                                    ((.03, 2300), (.09, 2900), (.15, 2100), (.22, 3300), (.3, 2600)))
        tranco = .9 * metal(s, .4, 1500, 14) + .6 * s.sub(.4, 100, 45, 6, 2.5)
        return elos + s.reverb(tranco, .7, .18, 5000)
    if nome == 'toji_soco':
        # Golpe de corpo: baque curto e abafado.
        return 1.0 * s.sub(0, 130, 45, 9, 3.5) + .7 * s.transiente(0, 2500, .015) \
            + .35 * s.passa_baixa(s.ruido(100, 2000, .6), 800) * s.env(0, .004, 22)
    if nome == 'toji_nuvem':
        # O bastão de três seções: o ar, a madeira batendo e o elo entre as seções.
        ar = .5 * sopro(s, 0, .03, 20, 500, 5000, .3)
        madeira = .9 * s.tom(420, 260, .5, .05, .12, (1, 2.1, 3.4), (1, .5, .25)) * s.env(.05, .001, 30)
        return ar + madeira + .6 * s.transiente(.05, 5000, .008) + .35 * metal(s, .08, 2600, 40) + .5 * s.sub(.05, 110, 55, 12, 2)
    if nome == 'toji_sumir':
        # Um passo e o ar fechando onde ele estava.
        passo = .6 * s.transiente(0, 3000, .01) + .5 * s.sub(0, 90, 45, 10, 2)
        ar = .7 * s.ruido(300, 5000, .4) * s.janela(.02, .3, .08) * (1 - s.crescer(.02, .35, .7))
        return passo + s.reverb(ar, .5, .15, 4000)
    if nome == 'toji_surgir':
        # O ar deslocado de repente atrás de você.
        ar = .8 * s.ruido(250, 6000, .3) * s.crescer(0, .09, 2.5) * s.env(.09, .001, 14)
        return ar + .7 * s.transiente(.09, 4500, .01) + .6 * s.sub(.09, 100, 45, 8, 2.5)
    if nome == 'toji_marca':
        # A lâmina saindo devagar da bainha: raspar de metal subindo, e um toque seco no fim.
        raspa = .5 * s.ruido(2500, 11000, 0) * s.janela(0, .55, .05) * (.3 + .7 * s.crescer(0, .55, 1.5))
        fio = .25 * s.tom(2400, 3600, 1.2, 0, .55, (1, 2.32), (1, .4)) * s.janela(0, .55, .05)
        return raspa + fio + .8 * metal(s, .58, 3400, 9)
    if nome == 'toji_fase':
        # Ele para de brincar: um baque fundo, o pano rasgando e duas batidas de metal.
        baque = 1.0 * s.sub(0, 70, 26, 2.2, 4) + .6 * s.transiente(0, 3000, .02)
        pano = .4 * s.ruido(900, 7000, .2) * s.janela(.15, .5, .05) * (1 + .6 * s.estalos(.15, .5, 80, 3000))
        return s.reverb(baque + pano + .6 * metal(s, .7, 1300, 6) + .5 * metal(s, .95, 1700, 6), 1.4, .25, 3500)
    if nome == 'toji_queda':
        # O golpe que derruba, o som do mundo abafado e o coração, cada vez mais perto, até o tick de levantar.
        golpe = 1.0 * s.sub(0, 120, 35, 4, 4) + .8 * s.transiente(0, 4000, .015) + .5 * metal(s, 0, 2200, 12)
        abafado = .18 * s.passa_baixa(s.ruido(30, 600, 1.), 220) * s.janela(.15, QUEDA, .3)
        coracao = sum((.55 + .09 * i) * (s.sub(b, 62, 38, 14, 3) + .7 * s.sub(b + .13, 55, 34, 16, 3))
                      for i, b in enumerate(BATIDAS))
        return golpe + s.reverb(abafado + coracao, 1.2, .2, 1500)
    if nome == 'toji_despertar':
        # A Técnica Reversa: o único som com tom desta lista, porque é do Gojo. Um acorde claro que abre e fica no ar.
        abre = 1.0 * s.sub(0, 90, 40, 2.5, 3) + .6 * s.transiente(0, 7000, .01)
        acorde = sum(p * s.tom(f, f * 1.01, 1., 0, 1.6, (1, 2, 3), (1, .4, .15)) for f, p in
                     ((392, .3), (587, .26), (880, .2), (1175, .12))) * s.env(0, .05, 1.6)
        ar = .25 * s.ruido(3000, 12000, 0) * s.env(0, .1, 2.2)
        return abre + s.reverb(acorde + ar, 2.0, .4, 9000)
    if nome == 'toji_morte':
        # A lança cai no chão e depois o corpo.
        lanca = .8 * metal(s, .15, 1700, 7) + .5 * metal(s, .3, 2100, 12, .7) + .3 * metal(s, .4, 1900, 18, .5)
        corpo = 1.0 * s.sub(.75, 80, 30, 3, 3.5) + .5 * s.transiente(.75, 2500, .02) \
            + .3 * s.passa_baixa(s.ruido(80, 2000, .7), 700) * s.env(.76, .02, 5)
        return s.reverb(lanca + corpo, 1.3, .25, 4000)
    raise KeyError(nome)


def gravado(nome, s):
    """O Toji é todo físico: as camadas gravadas (CC0) fazem o grosso, e a síntese fica por baixo."""
    if nome == 'toji_entrada':
        return s.amostra(WHOOSH, 0, .5, .7) + s.amostra(BAQUES, .3, 1., .8) + s.amostra(PEDRAS, .32, .35, 1.2)
    if nome == 'toji_aviso':
        return s.amostra(SACAR, 0, .8, 1.)
    if nome == 'toji_investida':
        return s.amostra(BAQUES, 0, .7, 1.) + s.amostra(WHOOSH, .02, 1., 1.)
    if nome == 'toji_lanca':
        return s.amostra([ESPADA_AR, LAMINA_FACA], 0, .8, 1.1) + s.amostra(METAL, .07, .7, 1.1)
    if nome == 'toji_anula':
        return s.amostra(VIDRO, 0, .8, 1.) + s.amostra(BAQUES, .02, .4, .8)
    if nome == 'toji_corrente':
        return s.amostra(CORRENTE_MEDIA, 0, .8, 1.) + s.amostra(CORRENTE_PESADA, .35, 1., 1.)
    if nome == 'toji_soco':
        return s.amostra(MURROS, 0, 1., 1.)
    if nome == 'toji_nuvem':
        return s.amostra(WHOOSH, 0, .6, 1.2) + s.amostra(MADEIRA + [PICAR], .05, .9, .9) + s.amostra(CORRENTE_LEVE, .07, .35, 1.2)
    if nome == 'toji_sumir':
        return s.amostra(WHOOSH, 0, .7, 1.1) + s.amostra(PANO, 0, .4, 1.)
    if nome == 'toji_surgir':
        return s.amostra(WHOOSH, 0, .7, 1.2, inverter=True) + s.amostra(BAQUES, .09, .6, 1.)
    if nome == 'toji_marca':
        return s.amostra(SACAR, 0, .6, .7) + s.amostra(METAL, .58, .7, 1.2)
    if nome == 'toji_fase':
        return s.amostra(BAQUES, 0, .9, .7) + s.amostra(PANO, .15, .6, .8) + s.amostra(METAL, .7, .6, .9) + s.amostra(METAL, .95, .5, 1.)
    if nome == 'toji_queda':
        return s.amostra(MURROS, 0, 1., .8) + s.amostra(METAL, 0, .4, 1.)
    if nome == 'toji_morte':
        return (s.amostra(METAL, .15, .8, .9) + s.amostra(METAL, .3, .5, 1.) + s.amostra(BAQUES, .75, 1., .7)
                + s.amostra(PANO, .76, .4, .8))
    return None  # o despertar é do Gojo: só o acorde sintetizado


def sintese(nome, s):
    return hibrido(s, sintetico(nome, s), gravado(nome, s))


def gerar(preview=None):
    m.escrever(SONS, sintese, 'SFX do Toji:', preview, pasta_sons='toji', marcos=MARCOS)


if __name__ == '__main__':
    a = argparse.ArgumentParser()
    a.add_argument('--preview')
    gerar(a.parse_args().preview)
