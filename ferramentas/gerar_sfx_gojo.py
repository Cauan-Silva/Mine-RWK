"""Sons do Gojo, sintetizados pelo motor em camadas (ferramentas/sfx_motor.py). Sem áudio de terceiros.

Rodar: python ferramentas/gerar_sfx_gojo.py [--preview caminho.wav]
Escreve os .ogg em assets/minerwk/sounds/gojo/, as entradas do sounds.json, as legendas e o manifesto
ferramentas/sfx_gojo_manifesto.json. Os tempos repetem os ticks do Java (Gojo.java): o teste SonsGojoTest
confere os MARCOS contra o kit. Mudou um tempo no Java, mudar aqui e gerar de novo.

A síntese de Azul, Vermelho, Infinito, Roxo e domínio veio do Minezin (tools/build_gojo_sfx.py, mesmo autor),
com os tempos do Mine-RWK. A explosão do Vermelho e o domínio rachando são novos.
"""
import argparse
import numpy as np
import sfx_motor as m

T = m.TICK
AZUL = 60 * T             # Gojo.CampoAzul.DURACAO
VERMELHO_CARGA = 30 * T   # cargaMax do Vermelho (X)
ROXO_CARGA = 30 * T       # carga do Roxo (V)
DOMINIO_ABERTURA = 10 * T  # carga do Vazio Infinito (G)

# nome -> (duração em s, alcance em blocos, legenda pt, legenda en)
SONS = {
    'gojo_azul': (AZUL + .4, 48, 'Azul puxa tudo para o centro', 'Blue pulls everything in'),
    'gojo_vermelho_carga': (VERMELHO_CARGA + .05, 32, 'Vermelho se concentra na ponta do dedo', 'Red gathers at the fingertip'),
    'gojo_vermelho_disparo': (.6, 48, 'Vermelho é disparado', 'Red is fired'),
    'gojo_vermelho_explosao': (2.5, 64, 'Vermelho explode', 'Red explodes'),
    'gojo_infinito_ativa': (1.0, 24, 'Infinito se ergue', 'Infinity rises'),
    'gojo_infinito_desativa': (.7, 24, 'Infinito se desfaz', 'Infinity fades'),
    'gojo_infinito_tic': (.45, 16, 'Infinito para o golpe', 'Infinity stops the blow'),
    'gojo_roxo_carga': (ROXO_CARGA + .05, 48, 'Azul e Vermelho se fundem', 'Blue and Red fuse'),
    'gojo_roxo_disparo': (5.0, 64, 'Vazio Roxo apaga o caminho', 'Hollow Purple erases its path'),
    'gojo_dominio': (DOMINIO_ABERTURA + 5.5, 64, 'Expansão de Domínio: Vazio Infinito', 'Domain Expansion: Infinite Void'),
    'gojo_dominio_fim': (1.6, 48, 'O domínio racha', 'The domain shatters'),
}

# Marcos em ticks, conferidos contra o Java pelo SonsGojoTest.
MARCOS = {
    'gojo_azul': {'fim': 60},
    'gojo_vermelho_carga': {'cheia': 30},
    'gojo_roxo_carga': {'dispara': 30},
    'gojo_dominio': {'abertura': 10},
}


def sintese(nome, s):
    if nome == 'gojo_azul':
        # Sucção grave (ruído filtrado baixo) + vento girando + núcleo sub pulsante.
        suga = s.passa_baixa(s.ruido(60, 3000, .8), 900) * s.crescer(0, .5, 2.5) * s.janela(0, .55, .06)
        vento = s.girar(s.ruido(150, 2400, .5), hz=3.2, prof=.85) * s.janela(.35, AZUL, .4)
        vento2 = s.girar(s.ruido(500, 5000, .2), hz=5.1, prof=.7) * s.janela(.5, AZUL, .4)
        sub = s.tom(38, 46, harmonicos=(1, 2), vibrato=.05, vib_hz=3.2) * s.janela(.3, AZUL, .5)
        x = .7 * suga + .45 * vento + .18 * vento2 + .55 * np.tanh(2 * sub) + .5 * s.sub(.45, 60, 26, 1.6, 3) + .12 * s.estalos(.6, AZUL, 18, 2500)
        return s.reverb(x, 1.8, .3, 2500)
    if nome == 'gojo_vermelho_carga':
        # Agudo subindo com estalos, cada vez mais forte até a carga cheia.
        g = s.crescer(0, VERMELHO_CARGA, 1.8)
        agudo = s.tom(700, 2600, 1.5, 0, VERMELHO_CARGA, (1, 2, 3))
        return (.42 * np.tanh(2.5 * agudo) + .22 * s.ruido(3000, 12000) + .25 * s.estalos(0, VERMELHO_CARGA, 90, 7000)) * g
    if nome == 'gojo_vermelho_disparo':
        # Estalo seco (transiente curtíssimo, sem reverb) + sopro de ar da esfera saindo.
        estalo = 1.2 * s.transiente(0, 9000, .006) + .6 * s.ruido(2000, 14000) * s.env(0, .001, 60)
        ar = .5 * s.ruido(200, 5000, .4) * s.env(.01, .02, 7)
        return estalo + s.reverb(ar + .5 * s.sub(.005, 110, 50, 6, 2), .8, .2, 5000)
    if nome == 'gojo_vermelho_explosao':
        # Boom grave com corpo de ruído rasgando; só a cauda leva reverb.
        estalo = .9 * s.transiente(0, 6000, .012)
        boom = 1.1 * s.sub(.01, 70, 24, 1.8, 4) + .5 * s.passa_baixa(s.ruido(30, 1200, .9), 300) * s.env(.01, .02, 2.2)
        ar = .4 * s.ruido(200, 5000, .4) * s.env(.02, .03, 4)
        return estalo + s.reverb(boom + ar + .2 * s.estalos(.05, .7, 40, 3000), 2.6, .4, 2500)
    if nome == 'gojo_infinito_ativa':
        # Dois toques curtos de vidro (parciais inarmônicos) + brilho de ar leve.
        x = sum(.3 * s.tom(f, harmonicos=(1, 2.76, 5.4), pesos=(1, .5, .25)) * s.env(d, .001, 28) for d, f in ((0, 3150), (.07, 4200)))
        x += .1 * s.ruido(6000, 16000) * s.env(0, .002, 18) + .08 * s.tom(1575, vibrato=.002) * s.env(.05, .05, 4)
        return s.reverb(x, 1.3, .3, 10000)
    if nome == 'gojo_infinito_desativa':
        x = .3 * s.tom(3600, harmonicos=(1, 2.76), pesos=(1, .4)) * s.env(0, .001, 22) + .2 * s.tom(2400, harmonicos=(1, 2.76)) * s.env(.05, .001, 20)
        return s.reverb(x + .06 * s.ruido(5000, 14000) * s.env(0, .01, 8), 1.0, .3, 9000)
    if nome == 'gojo_infinito_tic':
        x = .35 * s.tom(4200, harmonicos=(1, 2.76, 5.4), pesos=(1, .45, .2)) * s.env(0, .0008, 40)
        x += .08 * s.ruido(7000, 16000) * s.env(0, .001, 60)
        return s.reverb(x, .8, .25, 11000)
    if nome == 'gojo_roxo_carga':
        # Dois tons que se aproximam e batem cada vez mais rápido + hum grave por baixo.
        g = s.crescer(0, ROXO_CARGA, 1.5)
        zum = s.tom(220, 440, 1.3, 0, ROXO_CARGA, (1, 2, 3)) + s.tom(236, 441, 1.3, 0, ROXO_CARGA, (1, 2, 3))
        hum = s.tom(41, 55, 1., 0, ROXO_CARGA, (1, 2, 3), vibrato=.01, vib_hz=1.5)
        return s.reverb((.32 * np.tanh(1.5 * zum) + .5 * np.tanh(2.5 * hum) + .2 * s.passa_baixa(s.ruido(40, 2000, .8), 600)) * (.15 + .85 * g), 1.4, .25, 3500)
    if nome == 'gojo_roxo_disparo':
        # Explosão abafada (sem estalo agudo) + sub enorme + cauda longa ronronando.
        corpo = s.passa_baixa(s.ruido(20, 3000, .9), 450) * s.env(0, .03, .9)
        x = 1.0 * s.sub(0, 75, 20, .8, 4) + .9 * np.tanh(2 * corpo) + .25 * s.transiente(0, 1800, .03)
        x += .35 * s.girar(s.passa_baixa(s.ruido(20, 800, 1.), 250), hz=1.2, prof=.4) * s.env(.2, .5, .45)
        return s.reverb(x, 4.2, .5, 1500)
    if nome == 'gojo_dominio':
        # Drone crescendo na carga, baque na abertura e brilho agudo depois.
        g = s.crescer(0, DOMINIO_ABERTURA, 2.0)
        drone = sum(s.tom(f, harmonicos=(1, 2, 3, 4), pesos=(1, .5, .3, .2), vibrato=.003, vib_hz=.3 + .2 * i) for i, f in enumerate([36.7, 37.2, 55, 73.4])) / 4
        x = (.6 * np.tanh(2 * drone) + .2 * s.passa_baixa(s.ruido(20, 1500, .9), 400)) * (.1 + .9 * g)
        x += 1.0 * s.sub(DOMINIO_ABERTURA, 60, 20, .9, 3) + .4 * s.transiente(DOMINIO_ABERTURA, 2500, .03)
        x += .5 * np.tanh(2 * drone) * s.env(DOMINIO_ABERTURA, .1, .25)
        brilho = sum(s.tom(f, vibrato=.01, vib_hz=.15 + i * .1) for i, f in enumerate([880, 1318, 1760])) / 3
        x += .1 * brilho * s.env(DOMINIO_ABERTURA + .2, 1., .3)
        return s.reverb(x, 6.0, .6, 4000)
    if nome == 'gojo_dominio_fim':
        # Vidro grande trincando: trincas curtas se espalhando, depois o estilhaço e um sub de ar voltando.
        trincas = sum(.5 * s.transiente(d, 8000, .004) for d in (0, .09, .16, .27, .33, .41))
        vidro = sum(.16 * s.tom(f, harmonicos=(1, 2.76, 5.4), pesos=(1, .5, .25)) * s.env(d, .001, 14)
                    for d, f in ((.02, 2900), (.17, 3700), (.34, 4600), (.45, 2300), (.5, 5200)))
        estilhaco = .45 * s.estalos(.42, 1.2, 140, 9000) * s.env(.42, .01, 3.5) + .5 * s.transiente(.42, 5000, .02)
        return trincas + s.reverb(vidro + estilhaco + .6 * s.sub(.42, 80, 30, 3, 2), 1.8, .35, 9000)
    raise ValueError(nome)


def gerar(preview=None):
    m.escrever(SONS, sintese, 'SFX do Gojo:', preview, pasta_sons='gojo', marcos=MARCOS)


if __name__ == '__main__':
    a = argparse.ArgumentParser()
    a.add_argument('--preview')
    gerar(a.parse_args().preview)
