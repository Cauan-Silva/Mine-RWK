"""Sons do Ragnarok, sintetizados pelo motor em camadas (ferramentas/sfx_motor.py). Sem áudio de terceiros.

Rodar: python ferramentas/gerar_sfx_ragnarok.py [--preview caminho.wav]
Escreve os .ogg em assets/minerwk/sounds/ragnarok/, as entradas do sounds.json, as legendas e o manifesto
ferramentas/sfx_ragnarok_manifesto.json. Os ids repetem core/boss/SonsRagnarok.java (o SonsRagnarokTest confere).

Um demônio de sete blocos em chamas: tudo é grave e pesado. O rugido é um tom bem baixo cheio de harmônicos,
rasgado por ruído e tremendo; o fogo é ruído largo que estala; a terra é o sub-grave com pedras caindo.
"""
import argparse
import numpy as np
import sfx_motor as m

T = m.TICK
CARGA = 40 * T   # RagnarokBoss.SOPRO.aviso()
FEIXE = 40 * T   # RagnarokBoss.SOPRO.janela()

# nome -> (duração em s, alcance em blocos, legenda pt, legenda en)
SONS = {
    'ragnarok_entrada': (3.0, 96, 'Ragnarok ruge', 'Ragnarok roars'),
    'ragnarok_soco': (1.4, 64, 'Um murro racha o chão', 'A fist cracks the ground'),
    'ragnarok_rajada': (1.2, 64, 'As asas lançam magia', 'Wings hurl magic'),
    'ragnarok_asas': (1.6, 64, 'Asas enormes batem', 'Huge wings beat'),
    'ragnarok_investida': (1.4, 64, 'Ragnarok passa voando', 'Ragnarok swoops past'),
    'ragnarok_sopro_carga': (CARGA + .1, 64, 'O fogo se junta na boca', 'Fire gathers in the maw'),
    'ragnarok_sopro': (FEIXE + .6, 80, 'O Sopro do Demônio', 'Demon Breath roars'),
    'ragnarok_pisao': (1.6, 64, 'Um pisão faz o chão tremer', 'A stomp shakes the ground'),
    'ragnarok_corte': (.9, 64, 'Shadow corta a asa', 'Shadow cuts the wing'),
    'ragnarok_rugido': (2.4, 96, 'Ragnarok urra de dor', 'Ragnarok howls in pain'),
    'ragnarok_morte': (3.2, 96, 'Ragnarok cai', 'Ragnarok falls'),
}

MARCOS = {'ragnarok_sopro_carga': {'feixe': 40}}


def rugido(s, ini, dur, f0=78, f1=58, forca=1., aspero=.7):
    """A garganta: um tom grave cheio de harmônicos, tremendo, com o ruído rasgado por cima."""
    fim = ini + dur
    voz = s.tom(f0, f1, .8, ini, fim, (1, 2, 3, 4, 5, 6, 7, 9), (1, .8, .7, .55, .45, .35, .25, .15),
                vibrato=.04, vib_hz=7.5)
    rasgo = s.passa_baixa(s.ruido(60, 3500, .5), 1400)
    forma = s.janela(ini, fim, .12) * (.6 + .4 * s.crescer(ini, ini + dur * .35, 1.5))
    return forca * s.girar((voz + aspero * rasgo) * forma, 11, .35)


def fogo(s, ini, fim, forca=1., brilho=5000):
    """Chamas: ruído largo que respira e estala."""
    corpo = s.ruido(80, brilho, .45) * s.janela(ini, fim, .08)
    return forca * (s.girar(corpo, 3.2, .3) + .35 * s.estalos(ini, fim, 60, 4500))


def terra(s, ini, forca=1.):
    """O chão cedendo: o sub-grave, o estalo e as pedras caindo depois."""
    baque = 1.1 * s.sub(ini, 70, 26, 2.4, 4) + .8 * s.transiente(ini, 2500, .03)
    pedras = .45 * s.estalos(ini + .05, ini + .9, 45, 2500) * s.env(ini + .05, .02, 2.5)
    po = .35 * s.passa_baixa(s.ruido(40, 1800, .8), 600) * s.env(ini + .02, .05, 1.8)
    return forca * (baque + pedras + po)


def batida_de_asa(s, ini, forca=1.):
    """Uma batida: o pano enorme puxando o ar, grave e abafado."""
    ar = s.passa_baixa(s.ruido(30, 900, .8), 380) * s.janela(ini, ini + .32, .1)
    return forca * (1.1 * ar + .5 * s.sub(ini + .12, 55, 35, 6, 2))


def sintese(nome, s):
    if nome == 'ragnarok_entrada':
        # O portal ronca, os pés pesam no chão e o rugido sobe para o céu.
        portal = .4 * s.passa_baixa(s.ruido(20, 400, 1.), 160) * s.janela(0, 1.0, .3)
        return s.reverb(portal + terra(s, .25, .8) + rugido(s, .7, 2.1, 82, 60), 2.2, .35, 3000)
    if nome == 'ragnarok_soco':
        # O braço caindo (ar), o murro e a terra; as chamas do punho espalham.
        ar = .5 * s.passa_baixa(s.ruido(100, 2500, .6), 900) * s.crescer(0, .14, 2) * s.env(.14, .001, 30)
        return ar + s.reverb(terra(s, .14) + fogo(s, .16, .7, .4), 1.4, .25, 2500)
    if nome == 'ragnarok_rajada':
        # Duas batidas de asa juntas e cinco silvos de magia saindo em leque.
        silvos = sum(.35 * s.tom(900 + 140 * k, 380 + 60 * k, .6, .1 + .04 * k, .9, (1, 2.01), (1, .3))
                     * s.env(.1 + .04 * k, .02, 4) for k in range(5))
        return batida_de_asa(s, 0, 1.2) + s.reverb(silvos + .4 * fogo(s, .1, .6, .5, 7000), 1.0, .25, 6000)
    if nome == 'ragnarok_asas':
        # Três batidas na subida, cada vez mais fortes.
        return s.reverb(sum(batida_de_asa(s, d, f) for d, f in ((0, .8), (.4, 1.), (.8, 1.2))), 1.2, .2, 1500)
    if nome == 'ragnarok_investida':
        # O rasante: o ar rasgado chegando e indo, com o fogo do corpo passando junto.
        passa = np.where(s.t < .55, s.crescer(0, .55, 2.5), np.exp(-(s.t - .55) * 3))
        ar = 1.0 * s.ruido(150, 4500, .4) * passa
        return s.reverb(ar + .6 * fogo(s, .2, 1.0, .8) * passa + .5 * s.sub(.5, 60, 35, 3, 2.5), 1.0, .2, 3000)
    if nome == 'ragnarok_sopro_carga':
        # Ele puxa o ar: o fogo sendo sugado e um ronco que sobe até o feixe sair, no tick 40.
        puxa = s.ruido(200, 6000, .3) * s.crescer(0, CARGA, 1.8) * s.janela(0, CARGA, .05)
        ronco = .6 * s.tom(45, 95, 1.5, 0, CARGA, (1, 2, 3, 5), (1, .6, .4, .2)) * s.janela(0, CARGA, .2)
        return .5 * puxa + ronco + .4 * fogo(s, 0, CARGA, .6, 3000) * s.crescer(0, CARGA, 1.2)
    if nome == 'ragnarok_sopro':
        # O feixe: um rugido de fogo de 2 s, com a garganta por baixo.
        jato = 1.0 * fogo(s, 0, FEIXE + .2, 1.0, 9000) + .5 * s.ruido(800, 12000, .1) * s.janela(0, FEIXE, .05)
        return s.reverb(jato + .7 * rugido(s, 0, FEIXE, 70, 55, aspero=1.), 1.6, .3, 5000)
    if nome == 'ragnarok_pisao':
        # A perna sobe, desce, e a onda corre pelo chão rachando tudo.
        onda = .5 * s.estalos(.2, 1.0, 70, 3000) * s.env(.2, .05, 2) + .4 * fogo(s, .2, .9, .5)
        return s.reverb(terra(s, 0, 1.2) + onda, 1.6, .3, 2500)
    if nome == 'ragnarok_corte':
        # A lâmina de slime: o silvo fino, o corte molhado e o osso partindo.
        silvo = .8 * s.ruido(2500, 14000, 0) * s.crescer(0, .07, 2) * s.env(.07, .001, 20)
        osso = .7 * s.transiente(.07, 3500, .02) + .5 * s.estalos(.07, .2, 120, 3000) + .7 * s.sub(.07, 90, 40, 6, 3)
        magia = .3 * s.tom(1200, 600, .5, .07, .6, (1, 2), (1, .3)) * s.env(.07, .01, 5)
        return s.reverb(silvo + osso + magia, .8, .25, 5000)
    if nome == 'ragnarok_rugido':
        # O urro: começa estrangulado e se abre em raiva.
        return s.reverb(rugido(s, 0, 2.2, 110, 64, 1.1, 1.0), 2.0, .35, 3500)
    if nome == 'ragnarok_morte':
        # O último rugido, caindo, e o corpo enorme no chão.
        ultimo = rugido(s, 0, 1.8, 74, 38, .9, .8) * (1 - .6 * s.crescer(0, 1.8, 1))
        return s.reverb(ultimo + terra(s, 1.7, 1.3), 2.4, .35, 2500)
    raise KeyError(nome)


def gerar(preview=None):
    m.escrever(SONS, sintese, 'SFX do Ragnarok:', preview, pasta_sons='ragnarok', marcos=MARCOS)


if __name__ == '__main__':
    a = argparse.ArgumentParser()
    a.add_argument('--preview')
    gerar(a.parse_args().preview)
