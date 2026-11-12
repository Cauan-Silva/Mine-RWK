"""Sons do Ragnarok, sintetizados pelo motor em camadas (ferramentas/sfx_motor.py). Gravações CC0 de terceiros e síntese.

Rodar: python ferramentas/gerar_sfx_ragnarok.py [--preview caminho.wav]
Escreve os .ogg em assets/minerwk/sounds/ragnarok/, as entradas do sounds.json, as legendas e o manifesto
ferramentas/sfx_ragnarok_manifesto.json. Os ids repetem core/boss/SonsRagnarok.java (o SonsRagnarokTest confere).

Um demônio de sete blocos em chamas: tudo é grave e pesado. Híbrido: gravações CC0 (art/audio/cc0, fontes em
fontes.json) dão o corpo real (rosnados descidos de tom, fogo, explosões, pedras, pano, lâmina) e a síntese reforça
os graves e faz a magia.
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


from gravacoes import *  # noqa: F401,F403 (as listas de gravações)

MAGIA = CAMPO[:2]
LASERS = LASER_GRANDE


def garganta(s, ini, dur, tom=.5, forca=1.):
    """O rugido gravado: um rosnado descido de tom (a garganta de sete blocos) com um mugido ainda mais grave por
    baixo, e a voz sintetizada só como reforço dos graves."""
    forma = s.janela(ini, ini + dur, .1)
    gravado = s.amostra(ROSNADOS, ini, 1., tom) + s.amostra(G + 'mugido.ogg', ini + .05, .55, tom * .95)
    return forca * (gravado + .3 * rugido(s, ini, dur, 70, 52, 1., .4)) * forma


def chao(s, ini, forca=1., tom=.6):
    """O chão cedendo, gravado: a explosão grave, o baque e as pedras caindo depois."""
    return forca * (s.amostra(EXPLOSOES, ini, .9, tom) + s.amostra(SUBGRAVE, ini, .8, .9)
                    + s.amostra(PEDRAS, ini + .06, .45, .9) + .5 * s.sub(ini, 60, 26, 2.4, 4))


def sintese(nome, s):
    if nome == 'ragnarok_entrada':
        # O portal ronca (trovão), os pés pesam e o rugido sobe para o céu.
        portal = s.amostra(TROVOES, 0, .7, .7) + .3 * s.passa_baixa(s.ruido(20, 400, 1.), 160) * s.janela(0, 1.0, .3)
        pes = s.amostra(BAQUES, .25, .8, .6) + s.amostra(PEDRAS, .3, .35, .8)
        return s.reverb(portal + pes + garganta(s, .7, 2.2, .5, 1.1), 2.2, .3, 3000)
    if nome == 'ragnarok_soco':
        # O braço caindo (ar), o murro, a terra e as chamas do punho.
        ar = s.amostra(WHOOSH, 0, .45, .6) * (s.t < .16)
        murro = s.amostra(MURROS, .14, 1., .55) + chao(s, .14, .9)
        chama = s.amostra(G + 'fogo_ignicao.ogg', .15, .4, .8) * s.janela(.15, 1.2, .2)
        return ar + s.reverb(murro + chama, 1.3, .2, 2500)
    if nome == 'ragnarok_rajada':
        # Duas batidas de asa juntas e a magia saindo em leque.
        asa = s.amostra(BANDEIRAS, 0, 1., .55) + .4 * batida_de_asa(s, 0, 1.)
        magia = s.amostra(MAGIA, .1, .5, .8) + s.amostra(LASERS, .1, .35, .6)
        silvos = sum(.2 * s.tom(900 + 140 * k, 380 + 60 * k, .6, .1 + .04 * k, .9, (1, 2.01), (1, .3))
                     * s.env(.1 + .04 * k, .02, 4) for k in range(5))
        return asa + s.reverb(magia + silvos, 1.0, .25, 6000)
    if nome == 'ragnarok_asas':
        # Três batidas na subida, cada vez mais fortes: pano gravado descido de tom.
        batidas = sum(f * (s.amostra(BANDEIRAS, d, 1., .5) + .3 * batida_de_asa(s, d, 1.))
                      for d, f in ((0, .7), (.4, .85), (.8, 1.)))
        return s.reverb(batidas + s.amostra(G + 'voo_pombo.ogg', 0, .25, .35), 1.2, .2, 1500)
    if nome == 'ragnarok_investida':
        # O rasante: o ar rasgado chegando e indo, com o fogo do corpo passando junto.
        passa = np.where(s.t < .55, s.crescer(0, .55, 2.5), np.exp(-(s.t - .55) * 3))
        ar = s.amostra(WHOOSH, .1, 1., .5) + s.amostra(G + 'vento_forte.ogg', 0, .5, .8) * passa
        chama = s.amostra(CHAMAS, 0, .5, .9) * passa
        return s.reverb(ar + chama + .4 * s.sub(.5, 60, 35, 3, 2.5), 1.0, .2, 3000)
    if nome == 'ragnarok_sopro_carga':
        # Ele puxa o ar: o ronco sobe, o fogo é sugado para a boca até o feixe sair, no tick 40.
        sobe = s.crescer(0, CARGA, 1.6) * s.janela(0, CARGA, .05)
        ronco = s.amostra(K + 'spaceEngineLow_000.ogg', 0, .6, .7) + .4 * s.tom(45, 95, 1.5, 0, CARGA, (1, 2, 3), (1, .5, .3))
        puxa = s.amostra(WHOOSH, CARGA - 1.0, .6, .5, inverter=True) + s.amostra(G + 'fogo_galhos.ogg', 0, .5, .7)
        return (ronco + puxa) * sobe
    if nome == 'ragnarok_sopro':
        # O feixe: o jato de fogo gravado, a chama e a garganta por baixo.
        corpo = s.janela(0, FEIXE + .25, .05)
        jato = s.amostra(JATO, 0, 1., .65) + s.amostra(CHAMAS, 0, .6, .8) + s.amostra(G + 'fogo_ignicao.ogg', 0, .6, .7)
        return s.reverb((jato + .6 * garganta(s, 0, FEIXE, .45)) * corpo, 1.5, .25, 5000)
    if nome == 'ragnarok_pisao':
        # A perna desce, a onda corre pelo chão rachando tudo.
        onda = (s.amostra(TROVOES, .05, .5, .9) + s.amostra(PEDRAS, .2, .6, 1.)
                + s.amostra(CHAMAS, .2, .3, .9) * s.janela(.2, 1.2, .2))
        return s.reverb(chao(s, 0, 1.1, .5) + onda, 1.5, .25, 2500)
    if nome == 'ragnarok_corte':
        # A lâmina de slime: o corte gravado, o osso partindo e o eco violeta da magia.
        lamina = (s.amostra(G + 'espada_corta.ogg', 0, 1., 1.) + s.amostra(FACAS, .02, .6, .8)
                  + s.amostra(G + 'espada_ar.ogg', 0, .4, .9))
        osso = s.amostra(MURROS, .07, .6, .8) + s.amostra(K + 'chop.ogg', .08, .5, .6) + .5 * s.sub(.07, 90, 40, 6, 3)
        magia = .3 * s.tom(1200, 600, .5, .07, .6, (1, 2), (1, .3)) * s.env(.07, .01, 5)
        return s.reverb(lamina + osso + magia, .8, .25, 5000)
    if nome == 'ragnarok_rugido':
        # O urro de dor: o rosnado e o rugido do gato bem graves, abrindo em raiva.
        dor = garganta(s, 0, 2.2, .55, 1.) + s.amostra(G + 'rugido_gato.ogg', .1, .6, .4) * s.janela(.1, 2.2, .2)
        return s.reverb(dor + s.amostra(G + 'grunhidos.ogg', 0, .25, .5), 2.0, .3, 3500)
    if nome == 'ragnarok_morte':
        # O último rugido, caindo, e o corpo enorme no chão.
        ultimo = garganta(s, 0, 1.8, .42, 1.) * (1 - .6 * s.crescer(0, 1.8, 1))
        return s.reverb(ultimo + chao(s, 1.7, 1.3, .5) + s.amostra(BAQUES, 1.7, .9, .5), 2.4, .3, 2500)
    raise KeyError(nome)


def gerar(preview=None):
    m.escrever(SONS, sintese, 'SFX do Ragnarok:', preview, pasta_sons='ragnarok', marcos=MARCOS)


if __name__ == '__main__':
    a = argparse.ArgumentParser()
    a.add_argument('--preview')
    gerar(a.parse_args().preview)
