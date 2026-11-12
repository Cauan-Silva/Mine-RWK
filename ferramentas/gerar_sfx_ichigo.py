"""Sons do Ichigo, sintetizados pelo motor em camadas (ferramentas/sfx_motor.py). Sem áudio de terceiros.

Rodar: python ferramentas/gerar_sfx_ichigo.py [--preview caminho.wav]
Escreve os .ogg em assets/minerwk/sounds/ichigo/, as entradas do sounds.json, as legendas e o manifesto
ferramentas/sfx_ichigo_manifesto.json. Os tempos repetem os ticks do Java (Ichigo.java, FormasIchigo,
GetsugaIchigo): o teste SonsIchigoTest confere os MARCOS contra o kit. Mudou um tempo no Java, mudar aqui e
gerar de novo.

O som do Ichigo é de lâmina e de pressão: ar cortado, aço, e o reiatsu como um peso grave que empurra. Nada
dos tons de vidro e dos zumbidos do Gojo. O Shikai é mais aberto e claro (azul); o Bankai é mais grave,
seco e rápido (preto e vermelho). Os sons do Ichigo no Minezin não são usados.

A máscara é osso e uma voz oca por baixo; o Mugetsu é o contrário de um estouro: tudo se cala e vem um
corte só, seco e enorme, com a escuridão rolando atrás.

Ficam para a parte 8 do spec (a forma Hollow completa): Cero e garra.
"""
import argparse
import numpy as np
import sfx_motor as m

T = m.TICK
GETSUGA_CARGA = 10 * T    # cargaMax do Getsuga Tensho (Z do Shikai)
VOO, FIM = 15 * T, 31 * T              # GetsugaIchigo.voo(False) e duracao(False): voo + dissipa + rastro
VOO_NEGRO, FIM_NEGRO = 12 * T, 28 * T  # voo(True) e duracao(True)
SHUNPO = 9 * T            # FormasIchigo.SHUNPO
ONDA = 16 * T             # FormasIchigo.ONDA
VOLTA, ARREMESSO = 8 * T, 16 * T       # FormasIchigo.ARREMESSO_IDA e ARREMESSO
BANKAI_CARGA = 30 * T     # carga do Bankai (G do Shikai)
BANKAI = 34 * T           # FormasIchigo.BANKAI
LAMINA = 120 * T          # Ichigo.LaminaCarregada.DURACAO
SAIR = 14 * T             # FormasIchigo.SAIR
CORTE = 7 * T             # FormasIchigo.CORTE
QUEBRA = 8 * T            # SonsIchigo.QUEBRA_DA_MASCARA: a máscara some neste tick do som
TRANSFORMA, SILENCIO, MUGETSU_CARGA = 30 * T, 84 * T, 120 * T  # Ichigo.Mugetsu: TRANSFORMA, SILENCIO e CARGA
PASSOU, MUGETSU_FIM = 20 * T, 50 * T   # Ichigo.Veu.TICKS, e mais Veu.RESTO
AVISO = 30 * T            # Ichigo.SemPoderes.TICKS_DO_AVISO

# nome -> (duração em s, alcance em blocos, legenda pt, legenda en)
SONS = {
    'ichigo_getsuga_carga': (GETSUGA_CARGA + .05, 32, 'O reiatsu sobe pela Zangetsu', 'Reiatsu climbs up Zangetsu'),
    'ichigo_getsuga': (FIM + .6, 64, 'Getsuga Tensho corta o ar', 'Getsuga Tensho cuts the air'),
    'ichigo_getsuga_negro': (FIM_NEGRO + .6, 64, 'Getsuga Tensho negro rasga o ar', 'Black Getsuga Tensho tears the air'),
    'ichigo_shunpo': (SHUNPO + .3, 32, 'Ichigo some num passo', 'Ichigo vanishes in a step'),
    'ichigo_onda': (ONDA + 1.2, 48, 'O Getsuga explode no chão', 'Getsuga bursts on the ground'),
    'ichigo_arremesso': (ARREMESSO + .25, 48, 'A Zangetsu é lançada e volta', 'Zangetsu is thrown and returns'),
    'ichigo_bankai_carga': (BANKAI_CARGA + .05, 48, 'O reiatsu se junta', 'Reiatsu gathers'),
    'ichigo_bankai': (BANKAI + 1.6, 96, 'Bankai: Tensa Zangetsu', 'Bankai: Tensa Zangetsu'),
    'ichigo_lamina': (LAMINA + .3, 32, 'O Getsuga arde na lâmina', 'Getsuga burns on the blade'),
    'ichigo_sair': (SAIR + .5, 32, 'O Bankai se desfaz', 'Bankai fades'),
    'ichigo_corte': (CORTE + .1, 24, 'A lâmina corta o ar', 'The blade cuts the air'),
    'ichigo_mascara_veste': (1.7, 48, 'A máscara Hollow se forma', 'The Hollow mask forms'),
    'ichigo_mascara_quebra': (QUEBRA + .7, 32, 'A máscara Hollow se quebra', 'The Hollow mask shatters'),
    'ichigo_mugetsu_carga': (MUGETSU_CARGA + .05, 64, 'O reiatsu se cala', 'The reiatsu falls silent'),
    'ichigo_mugetsu': (MUGETSU_FIM + 2.2, 128, 'Mugetsu', 'Mugetsu'),
    'ichigo_poderes_perdidos': (AVISO + .6, 32, 'O reiatsu do Ichigo se vai', "Ichigo's reiatsu fades away"),
    'ichigo_poderes_voltam': (AVISO + .4, 32, 'O reiatsu do Ichigo volta', "Ichigo's reiatsu returns"),
}

# Marcos em ticks, conferidos contra o Java pelo SonsIchigoTest.
MARCOS = {
    'ichigo_getsuga_carga': {'cheia': 10},
    'ichigo_getsuga': {'voo': 15, 'fim': 31},
    'ichigo_getsuga_negro': {'voo': 12, 'fim': 28},
    'ichigo_shunpo': {'fim': 9},
    'ichigo_onda': {'fim': 16},
    'ichigo_arremesso': {'volta': 8, 'fim': 16},
    'ichigo_bankai_carga': {'libera': 30},
    'ichigo_bankai': {'fim': 34},
    'ichigo_lamina': {'fim': 120},
    'ichigo_sair': {'fim': 14},
    'ichigo_corte': {'fim': 7},
    'ichigo_mascara_veste': {'fecha': 8},
    'ichigo_mascara_quebra': {'quebra': 8},
    'ichigo_mugetsu_carga': {'transforma': 30, 'silencio': 84, 'corte': 120},
    'ichigo_mugetsu': {'passou': 20, 'fim': 50},
    'ichigo_poderes_perdidos': {'fim': 30},
    'ichigo_poderes_voltam': {'fim': 30},
}


def aco(s, ini, f=2600, forca=1.):
    """O toque do aço: parciais inarmônicos curtos (uma lâmina vibrando), sem cara de vidro."""
    return forca * sum(p * s.tom(f * h) * s.env(ini, .001, q) for h, p, q in ((1, .5, 26), (1.48, .3, 34), (2.31, .22, 44), (3.9, .1, 60)))


def golpe_de_ar(s, ini, dur, lo=400, hi=7000, curva=1.6):
    """O ar cortado por uma lâmina: ruído que abre e fecha rápido, subindo de tom."""
    return s.ruido(lo, hi, .3) * s.crescer(ini, ini + dur * .6, curva) * s.janela(ini, ini + dur, dur * .25)


def getsuga(s, voo, fim, grave):
    """O corte voa (ar rasgando, cada vez mais longe) e estoura quando se desfaz; o rasgo no chão crepita."""
    f = .72 if grave else 1.
    saida = 1.1 * s.transiente(0, 7000 * f, .008) + .9 * golpe_de_ar(s, 0, .16, 500 * f, 9000 * f) + .5 * aco(s, .005, 2400 * f, .6)
    saida += .9 * s.sub(0, 120 * f, 46 * f, 5, 3)
    # O voo: um rugido que gira e desce de tom (vai embora), mais um fio agudo de lâmina.
    rugido = s.girar(s.passa_baixa(s.ruido(60, 5000 * f, .7), 1500 * f), hz=7 if grave else 5, prof=.45)
    rugido = rugido * s.janela(.03, voo + .1, .08) * (1 - .55 * s.crescer(0, voo, 1.))
    fio = .12 * s.tom(1900 * f, 1100 * f, 1., 0, voo, (1, 2.31)) * s.janela(.02, voo, .06)
    # O estouro no fim do voo, mais seco e grave no Bankai, e o chão rasgado crepitando até o fim.
    estouro = .8 * s.transiente(voo, 4500 * f, .015) + 1.0 * s.sub(voo, 85 * f, 30 * f, 3.2, 3.5)
    estouro += .5 * s.passa_baixa(s.ruido(40, 2500, .9), 600 * f) * s.env(voo, .01, 4.5)
    rastro = .22 * s.estalos(voo + .05, fim, 55, 4200 * f) * (1 - .8 * s.crescer(voo, fim, 1.))
    return saida + s.reverb(.75 * rugido + fio + estouro + rastro, 1.9 if grave else 2.3, .3, 5000 * f)


def sintese(nome, s):
    if nome == 'ichigo_getsuga_carga':
        # O reiatsu subindo pela lâmina: ar e um grave que crescem até a carga cheia, com o aço vibrando.
        g = s.crescer(0, GETSUGA_CARGA, 1.7)
        ar = s.girar(s.ruido(300, 6000, .4), hz=9, prof=.35)
        grave = s.tom(55, 92, 1.3, 0, GETSUGA_CARGA, (1, 2, 3), vibrato=.02, vib_hz=6)
        canta = .1 * s.tom(1700, 2500, 1.4, 0, GETSUGA_CARGA, (1, 2.31))
        return s.reverb((.45 * ar + .5 * np.tanh(2.2 * grave) + canta) * (.1 + .9 * g), 1.0, .2, 5000)
    if nome == 'ichigo_getsuga':
        return getsuga(s, VOO, FIM, False)
    if nome == 'ichigo_getsuga_negro':
        return getsuga(s, VOO_NEGRO, FIM_NEGRO, True)
    if nome == 'ichigo_shunpo':
        # O passo: um baque seco nos pés, o ar sugado atrás dele e um estalo curto onde ele surge.
        some = .8 * s.sub(0, 140, 60, 9, 2) + .45 * s.transiente(0, 5000, .006)
        ar = .7 * s.ruido(600, 11000, .25) * s.env(.005, .012, 9)
        chega = .5 * s.transiente(SHUNPO * .55, 6500, .005) + .35 * golpe_de_ar(s, SHUNPO * .5, .12, 900, 9000)
        return some + s.reverb(ar + chega, .7, .18, 8000)
    if nome == 'ichigo_onda':
        # A espada crava no chão (aço e pedra), a onda abre e os pilares sobem crepitando.
        crava = 1.2 * s.transiente(0, 6000, .012) + .6 * aco(s, .004, 1900, .8)
        boom = 1.2 * s.sub(.01, 95, 26, 2.4, 4) + .6 * s.passa_baixa(s.ruido(30, 1800, .9), 420) * s.env(.01, .02, 3)
        onda = .5 * s.girar(s.ruido(200, 5000, .5), hz=4, prof=.4) * s.janela(.03, ONDA, .12) * (1 - .6 * s.crescer(0, ONDA, 1.))
        pedras = .3 * s.estalos(.04, ONDA + .4, 70, 3200) * (1 - .7 * s.crescer(0, ONDA + .4, 1.))
        return crava + s.reverb(boom + onda + pedras, 2.2, .35, 3500)
    if nome == 'ichigo_arremesso':
        # A espada sai girando (o vum-vum da lâmina), para no fim da ida e volta mais rápida; o aço bate na mão.
        gira_ida = s.girar(s.ruido(250, 5500, .35), hz=14, prof=.9) * s.janela(0, VOLTA, .04) * (1 - .4 * s.crescer(0, VOLTA, 1.))
        gira_volta = s.girar(s.ruido(300, 6500, .35), hz=18, prof=.9) * s.janela(VOLTA, ARREMESSO - .02, .04) * (.45 + .4 * s.crescer(VOLTA, ARREMESSO, 1.5))
        sai = .7 * s.transiente(0, 5500, .008) + .5 * s.sub(0, 110, 50, 7, 2)
        pano = .18 * s.estalos(.02, ARREMESSO, 45, 2600)
        # O baque na mão tem de aparecer por cima do giro: estalo, aço e um grave curto.
        pega = 1.2 * s.transiente(ARREMESSO, 4500, .01) + 1.0 * aco(s, ARREMESSO, 2200, 1.) + .8 * s.sub(ARREMESSO, 120, 55, 9, 2.5)
        return sai + s.reverb(.7 * gira_ida + .6 * gira_volta + pano + pega, .9, .2, 6000)
    if nome == 'ichigo_bankai_carga':
        # A pressão se juntando: um grave que sobe e treme cada vez mais rápido, o ar puxado para dentro.
        g = s.crescer(0, BANKAI_CARGA, 1.8)
        grave = s.tom(38, 74, 1.4, 0, BANKAI_CARGA, (1, 2, 3), vibrato=.03, vib_hz=7)
        puxa = s.girar(s.ruido(150, 4500, .5), hz=11, prof=.5) * s.crescer(0, BANKAI_CARGA, 2.4)
        pulso = sum(.35 * s.sub(d, 80, 44, 9, 2) for d in np.linspace(.15, BANKAI_CARGA - .1, 6) ** 1.0)
        return s.reverb((.6 * np.tanh(2.6 * grave) + .35 * puxa) * (.12 + .88 * g) + pulso * g, 1.5, .25, 3500)
    if nome == 'ichigo_bankai':
        # O estouro de pressão: estalo seco, um sub enorme e o vento preto que varre e some; o aço da Tensa canta.
        estalo = 1.3 * s.transiente(0, 6500, .014) + .5 * s.ruido(1500, 12000) * s.env(0, .001, 22)
        sub = 1.3 * s.sub(0, 80, 20, 1.1, 5)
        corpo = np.tanh(3 * s.passa_baixa(s.ruido(20, 3500, .9), 650)) * s.env(0, .02, 1.1)
        vento = .55 * s.girar(s.ruido(150, 6000, .5), hz=3.5, prof=.5) * s.janela(.04, BANKAI, .3) * (1 - .6 * s.crescer(0, BANKAI, 1.))
        lamina = .35 * aco(s, .12, 1500, 1.) + .1 * s.tom(1500, 1480, 1., .12, BANKAI, (1, 2.31)) * s.janela(.12, BANKAI, .3)
        return estalo + s.reverb(sub + .9 * corpo + vento + lamina, 4.2, .45, 3000)
    if nome == 'ichigo_lamina':
        # O Getsuga aceso na lâmina por 6 s: acende com um sopro e fica ardendo, grave e crepitando.
        acende = .8 * golpe_de_ar(s, 0, .22, 300, 6000) + .7 * s.sub(0, 100, 50, 5, 2.5) + .3 * aco(s, .01, 1700, .7)
        arde = s.girar(s.passa_baixa(s.ruido(60, 3500, .7), 1100), hz=13, prof=.4) * s.janela(.12, LAMINA, .25)
        grave = .35 * np.tanh(2 * s.tom(62, harmonicos=(1, 2, 3), vibrato=.03, vib_hz=8)) * s.janela(.12, LAMINA, .3)
        crepita = .1 * s.estalos(.15, LAMINA, 26, 3800)
        return acende + s.reverb(.34 * arde + grave + crepita, 1.2, .2, 4000)
    if nome == 'ichigo_sair':
        # A pressão se soltando: o vento desce de tom e some; o aço dá um último toque.
        vento = s.girar(s.ruido(200, 6000, .45), hz=5, prof=.4) * s.janela(0, SAIR + .3, .2) * (1 - .8 * s.crescer(0, SAIR + .3, 1.))
        desce = .3 * np.tanh(2 * s.tom(90, 40, 1., 0, SAIR, (1, 2))) * s.janela(0, SAIR, .15)
        return s.reverb(.6 * vento + desce + .25 * aco(s, .02, 1300, .7) + .5 * s.sub(0, 90, 40, 6, 2), 1.4, .28, 4500)
    if nome == 'ichigo_corte':
        # O ataque básico: só a lâmina passando pelo ar, curto, para não cansar de tanto ouvir.
        return .55 * s.transiente(.01, 6000, .004) + s.reverb(.75 * golpe_de_ar(s, 0, CORTE * .8, 700, 9500, 1.4), .5, .12, 8000)
    if nome == 'ichigo_mascara_veste':
        # O osso se fechando sobre o rosto (estalos secos, sem brilho de vidro) e a voz oca por baixo: dois
        # tons graves quase iguais, distorcidos, com um eco que repete e some.
        osso = sum(.55 * s.transiente(d, 3200, .006) for d in (0, .06, .13, .19, .27, .34)) + .4 * s.estalos(0, .4, 60, 2400)
        fecha = .9 * s.sub(.4, 110, 48, 6, 3) + .5 * s.transiente(.4, 4000, .01)
        voz = np.tanh(3.2 * (s.tom(78, 64, 1., .35, 1.5, (1, 2, 3, 5), vibrato=.04, vib_hz=7)
                             + .8 * s.tom(81.5, 66, 1., .35, 1.5, (1, 2, 3, 5), vibrato=.03, vib_hz=5.3)))
        voz = s.passa_baixa(voz, 1400) * s.env(.38, .05, 2.6)
        eco = sum(g * np.roll(voz, int(m.RATE * d)) for d, g in ((.16, .5), (.32, .3), (.48, .16)))
        return osso + fecha + s.reverb(.55 * voz + .5 * eco, 1.6, .35, 2600)
    if nome == 'ichigo_mascara_quebra':
        # Rachaduras subindo e o último estalo quando ela some; os cacos caem secos, a voz oca se desfaz.
        racha = sum((.35 + .08 * i) * s.transiente(d, 3600, .005) for i, d in enumerate(np.linspace(0, QUEBRA - .05, 6)))
        parte = 1.0 * s.transiente(QUEBRA, 4200, .012) + .6 * s.sub(QUEBRA, 120, 60, 9, 2)
        cacos = .4 * s.estalos(QUEBRA, QUEBRA + .5, 70, 2800) * s.env(QUEBRA, .01, 5)
        some = .3 * np.tanh(2 * s.tom(74, 40, 1., 0, QUEBRA + .4, (1, 2, 3))) * s.janela(0, QUEBRA + .4, .1) * (1 - s.crescer(0, QUEBRA + .4, 1.))
        return racha + parte + s.reverb(cacos + some, 1.0, .25, 3500)
    if nome == 'ichigo_mugetsu_carga':
        # A transformação: o reiatsu preto sobe como fumaça (um grave que incha e um vento escuro) e fecha na
        # forma final; depois tudo vai murchando até o silêncio completo antes do corte.
        sobe = s.crescer(0, TRANSFORMA, 1.6) * s.janela(0, TRANSFORMA + .25, .2)
        fumaca = s.girar(s.passa_baixa(s.ruido(40, 2600, .8), 700), hz=2.3, prof=.5)
        grave = np.tanh(2.4 * s.tom(34, 58, 1.3, 0, TRANSFORMA, (1, 2, 3), vibrato=.02, vib_hz=1.7))
        forma = 1.0 * s.sub(TRANSFORMA, 70, 24, 1.6, 4) + .35 * s.transiente(TRANSFORMA, 2200, .03)
        # Do baque da forma final até o silêncio: um fio grave que só diminui.
        resto = (.5 * fumaca + .4 * np.tanh(2 * s.tom(46, 30, 1., TRANSFORMA, SILENCIO, (1, 2)))) \
            * s.janela(TRANSFORMA, SILENCIO, .15) * (1 - s.crescer(TRANSFORMA, SILENCIO, .8))
        x = (.55 * fumaca + .6 * grave) * sobe + forma + .7 * resto
        # Depois do SILENCIO não toca mais nada: o que sobra é só a cauda do reverb morrendo.
        return s.reverb(x, 1.6, .3, 2400) * (1 - s.crescer(SILENCIO + .2, SILENCIO + .9, 1.))
    if nome == 'ichigo_mugetsu':
        # O corte único: um estalo de lâmina, um sub enorme e a escuridão rolando para longe enquanto o véu
        # avança; quando ele passa, sobra um grave que demora a morrer.
        corte = 1.4 * s.transiente(0, 7500, .008) + .9 * golpe_de_ar(s, 0, .22, 300, 11000, 1.2) + .7 * aco(s, .004, 1300, 1.)
        sub = 1.4 * s.sub(0, 65, 16, .55, 5)
        parede = np.tanh(3 * s.passa_baixa(s.ruido(20, 2600, 1.), 520)) * s.janela(.02, PASSOU + .3, .25) * (1 - .5 * s.crescer(0, PASSOU, 1.))
        rasga = .4 * s.girar(s.ruido(120, 4200, .6), hz=1.9, prof=.5) * s.janela(.03, PASSOU, .2) * (1 - .7 * s.crescer(0, PASSOU, 1.))
        cauda = .55 * s.girar(s.passa_baixa(s.ruido(20, 700, 1.), 180), hz=.6, prof=.5) * s.env(PASSOU * .7, .8, .55)
        return corte + s.reverb(sub + .85 * parede + rasga + cauda, 6.0, .55, 1800)
    if nome == 'ichigo_poderes_perdidos':
        # O reiatsu indo embora: um tom que desce e se esvazia, com um sopro que some junto.
        desce = np.tanh(2 * s.tom(180, 46, .7, 0, AVISO + .3, (1, 2, 3), vibrato=.03, vib_hz=4)) * (1 - s.crescer(0, AVISO + .3, .8))
        sopro = s.ruido(300, 5000, .5) * s.janela(0, AVISO + .4, .15) * (1 - s.crescer(0, AVISO + .4, .7))
        return s.reverb(.5 * desce * s.janela(0, AVISO + .4, .05) + .35 * sopro + .15 * aco(s, .02, 900, .8), 1.8, .35, 3000)
    if nome == 'ichigo_poderes_voltam':
        # O contrário: o tom sobe, o ar volta e a lâmina dá um toque quando o reiatsu acende de novo.
        sobe = np.tanh(2 * s.tom(60, 170, 1.4, 0, AVISO * .8, (1, 2, 3), vibrato=.02, vib_hz=5)) * s.crescer(0, AVISO * .8, 1.6)
        ar = s.girar(s.ruido(300, 6000, .4), hz=8, prof=.35) * s.crescer(0, AVISO * .8, 2.) * s.janela(0, AVISO, .12)
        acende = .9 * s.sub(AVISO * .8, 110, 52, 5, 2.5) + .55 * aco(s, AVISO * .8, 2100, 1.) + .4 * s.transiente(AVISO * .8, 5000, .008)
        return s.reverb(.45 * sobe * s.janela(0, AVISO, .08) + .35 * ar + acende, 1.4, .28, 4500)
    raise ValueError(nome)


def gerar(preview=None):
    m.escrever(SONS, sintese, 'SFX do Ichigo:', preview, pasta_sons='ichigo', marcos=MARCOS)


if __name__ == '__main__':
    a = argparse.ArgumentParser()
    a.add_argument('--preview')
    gerar(a.parse_args().preview)
