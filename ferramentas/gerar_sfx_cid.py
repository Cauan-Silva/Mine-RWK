"""Sons do Cid: síntese do motor em camadas (ferramentas/sfx_motor.py) com gravações CC0 por cima (gravacoes.py).

Rodar: python ferramentas/gerar_sfx_cid.py [--preview caminho.wav]
Escreve os .ogg em assets/minerwk/sounds/cid/, as entradas do sounds.json, as legendas e o manifesto
ferramentas/sfx_cid_manifesto.json. Os tempos repetem os ticks do Java (Cid.java e FormasCid): o teste
SonsCidTest confere os MARCOS contra o kit. Mudou um tempo no Java, mudar aqui e gerar de novo.

O som do Cid é de slime e de magia contida: a lâmina é fina e molhada (um silvo curto, sem o aço cheio do
Ichigo), o slime é um estalo úmido e grave, as correntes são elos secos que correm e travam, e a magia é
um zumbido grave, com dois tons quase juntos batendo um no outro. Nada de vidro (Gojo) nem de rugido (Hollow).
O atômico é a mesma magia, só que limpa: um tom que sobe e some, e um acorde claro quando ela se solta.
Partes 4 a 6: os golpes base, a Espada Atômica, o Recovery Atomic e o I Am Atomic final (os socos secos, o
tom que sobe nas três palavras, o instante de silêncio do espaço e o estouro). O Ragnarok ganha os seus na parte dele.
"""
import argparse
import numpy as np
import sfx_motor as m
from gravacoes import *  # noqa: F401,F403 (as listas de gravações)

T = m.TICK
ENTRE_CORTES, VIDA_DO_CORTE, REDEMOINHO = 5 * T, 7 * T, 17 * T   # FormasCid: ENTRE_CORTES, VIDA_DO_CORTE, REDEMOINHO
RISCOS = 9 * T            # FormasCid.RISCOS: a partida da Pós-imagem
VOO = 10 * T              # Cid.Prisao: ALCANCE / PASSO, o voo inteiro do slime
FECHAR, SOLTAR, PRESO = 4 * T, 6 * T, 50 * T   # FormasCid.FECHAR e SOLTAR, e Cid.Prisao.PRESO
ESTOURO, OVERDRIVE = 8 * T, 160 * T    # FormasCid.ESTOURO e Cid.Overdrive.DURACAO
TRAJE = 12 * T            # FormasCid.TRAJE
CARGA_DA_ESPADA, VOO_DO_CORTE, CORTE_ATOMICO = 12 * T, 4 * T, 10 * T   # FormasCid, os mesmos nomes
CARGA_DO_RECOVERY, ONDA, COLUNA = 60 * T, 10 * T, 40 * T               # FormasCid, os mesmos nomes
# AtomicFinal: ENTRE_SOCOS, TICKS_DO_RISCO, TICKS_DA_FALA, TICKS_DO_CLARAO e TICKS_DA_LUZ
SOCO, RISCO, FALA, CLARAO, LUZ = 12 * T, 14 * T, 110 * T, 130 * T, 180 * T
PULSO = 95 * T            # SistemaSolar.PULSO: o último pulso do ponto roxo, no fim do recuo
# AtomicFinal: PARADA, CARGA - FALA, TERCEIRA - FALA e TICKS_DA_ONDA
PARADA, DUAS, MUDA, CHOQUE = 3 * T, 56 * T, 86 * T, 24 * T
SILENCIO = .22            # o instante sem som antes do estouro, no espaço

# nome -> (duração em s, alcance em blocos, legenda pt, legenda en)
SONS = {
    'cid_redemoinho': (REDEMOINHO + .35, 32, 'A espada de slime gira em três cortes', 'The slime sword spins in three cuts'),
    'cid_pos_imagem': (RISCOS + .45, 32, 'Shadow some e deixa um vulto', 'Shadow vanishes, leaving an afterimage'),
    'cid_slime': (VOO + .2, 32, 'Um pedaço de slime é lançado', 'A lump of slime is thrown'),
    'cid_correntes': (PRESO + .4, 32, 'Correntes negras se fecham', 'Black chains snap shut'),
    'cid_overdrive': (OVERDRIVE + .3, 32, 'A magia transborda', 'Magic overflows'),
    'cid_traje': (TRAJE + .3, 24, 'O traje de slime absorve o golpe', 'The slime suit absorbs the blow'),
    'cid_espada_carga': (CARGA_DA_ESPADA + .1, 32, 'A magia se junta na lâmina', 'Magic gathers on the blade'),
    'cid_espada_atomica': (CORTE_ATOMICO + .9, 48, 'A Espada Atômica corta', 'The Atomic Sword cuts'),
    'cid_recovery_carga': (CARGA_DO_RECOVERY + .1, 32, 'Shadow crava a espada no chão', 'Shadow plants the sword in the ground'),
    'cid_recovery': (COLUNA + 1.2, 64, 'Uma coluna de luz cura', 'A pillar of light heals'),
    'cid_atomic_soco': (SOCO + .15, 48, 'Um soco de Shadow', 'Shadow lands a punch'),
    'cid_atomic_risco': (RISCO + .5, 96, 'Shadow some céu acima', 'Shadow vanishes into the sky'),
    'cid_atomic_aura': (FALA + .1, 48, 'A magia cresce em silêncio', 'Magic swells in silence'),
    'cid_atomic': (CLARAO + 1.6, 96, 'I am atomic', 'I am atomic'),
    'cid_atomic_luz': (LUZ + 1.5, 160, 'Um estouro distante no céu', 'A distant burst in the sky'),
    'cid_atomic_onda': (CHOQUE + 1.3, 64, 'Uma onda de choque varre o chão', 'A shockwave sweeps the ground'),
}



def ticks(segundos):
    return round(segundos / T)


# Marcos em ticks, tirados das constantes que a síntese usa e conferidos contra o Java pelo SonsCidTest.
MARCOS = {
    'cid_redemoinho': {'entre_cortes': ticks(ENTRE_CORTES), 'vida_do_corte': ticks(VIDA_DO_CORTE), 'fim': ticks(REDEMOINHO)},
    'cid_pos_imagem': {'fim': ticks(RISCOS)},
    'cid_slime': {'fim': ticks(VOO)},
    'cid_correntes': {'fecha': ticks(FECHAR), 'soltar': ticks(SOLTAR), 'solta': ticks(PRESO)},
    'cid_overdrive': {'estouro': ticks(ESTOURO), 'fim': ticks(OVERDRIVE)},
    'cid_traje': {'fim': ticks(TRAJE)},
    'cid_espada_carga': {'fim': ticks(CARGA_DA_ESPADA)},
    'cid_espada_atomica': {'voo': ticks(VOO_DO_CORTE), 'fim': ticks(CORTE_ATOMICO)},
    'cid_recovery_carga': {'fim': ticks(CARGA_DO_RECOVERY)},
    'cid_recovery': {'onda': ticks(ONDA), 'fim': ticks(COLUNA)},
    'cid_atomic_soco': {'parada': ticks(PARADA), 'fim': ticks(SOCO)},
    'cid_atomic_risco': {'fim': ticks(RISCO)},
    'cid_atomic_aura': {'carga': ticks(DUAS), 'muda': ticks(MUDA), 'fim': ticks(FALA)},
    'cid_atomic': {'clarao': ticks(CLARAO), 'pulso': ticks(PULSO)},
    'cid_atomic_luz': {'fim': ticks(LUZ)},
    'cid_atomic_onda': {'fim': ticks(CHOQUE)},
}


def silvo(s, ini, dur, lo=1800, hi=12000, curva=1.4):
    """A lâmina fina cortando o ar: ruído agudo que abre e fecha rápido."""
    return s.ruido(lo, hi, .2) * s.crescer(ini, ini + dur * .55, curva) * s.janela(ini, ini + dur, dur * .3)


def umido(s, ini, forca=1., f=170):
    """O estalo molhado do slime: um baque grave e curto com um chiado abafado por cima."""
    baque = s.sub(ini, f, f * .4, 14, 2.5)
    chiado = s.passa_baixa(s.ruido(300, 3200, .5), 1800) * s.env(ini, .004, 26)
    gota = .5 * s.tom(f * 4.2, f * 2.1, .5, ini, ini + .07) * s.env(ini, .002, 38)
    return forca * (baque + .7 * chiado + gota)


def magia(s, f=62, bate=3.1):
    """O zumbido da magia: dois tons graves quase juntos (batem um no outro) e um terceiro uma quinta acima."""
    return np.tanh(2.2 * (s.tom(f, harmonicos=(1, 2, 3)) + .9 * s.tom(f + bate, harmonicos=(1, 2, 3))
                          + .35 * s.tom(f * 1.5 + bate * .5, harmonicos=(1, 2))))


def elos(s, ini, fim, densidade, f=2100):
    """Elos correndo: estalos metálicos curtos e secos, cada um num tom um pouco diferente."""
    x = np.zeros(s.n)
    for k in range(max(1, int(densidade * (fim - ini)))):
        t0 = ini + (fim - ini) * (k + s.rng.uniform(0, .6)) / max(1, int(densidade * (fim - ini)))
        tom = f * s.rng.uniform(.8, 1.35)
        x += s.rng.uniform(.5, 1.) * (s.tom(tom) + .5 * s.tom(tom * 1.52) + .3 * s.tom(tom * 2.7)) * s.env(t0, .001, 70)
    return x


def sintetico(nome, s):
    if nome == 'cid_redemoinho':
        # Três cortes, um a cada 5 ticks: o silvo fino da lâmina esticada, um fio de magia na ponta e o slime
        # chicoteando por baixo. O terceiro é o mais forte e deixa a cauda.
        x = np.zeros(s.n)
        for i in range(3):
            t0, f = i * ENTRE_CORTES, 1 + .12 * i
            x += (.75 + .15 * i) * silvo(s, t0, VIDA_DO_CORTE * .75, 1500 * f, 12500)
            x += .5 * s.transiente(t0, 7500, .006)
            x += .22 * s.tom(980 * f, 1900 * f, .7, t0, t0 + VIDA_DO_CORTE * .7, (1, 2.01)) * s.janela(t0, t0 + VIDA_DO_CORTE * .7, .05)
            x += .45 * s.passa_baixa(s.ruido(80, 900, .8), 420) * s.janela(t0, t0 + VIDA_DO_CORTE * .6, .06)
        return s.reverb(x + .5 * s.sub(2 * ENTRE_CORTES, 110, 50, 7, 2.5), .7, .2, 7000)
    if nome == 'cid_pos_imagem':
        # Ele some: o ar puxado de uma vez, um baque abafado, e o vulto fica chiando baixinho e se desfazendo.
        some = 1.0 * silvo(s, 0, .16, 500, 9000, 1.1) + .7 * s.transiente(0, 4500, .008) + .7 * s.sub(0, 140, 48, 9, 2.5)
        vulto = .22 * s.girar(magia(s, 110, 4.3), hz=11, prof=.6) * s.janela(.04, RISCOS + .35, .12) * (1 - .8 * s.crescer(.04, RISCOS + .35, .8))
        chia = .2 * s.ruido(2500, 9000, .3) * s.janela(.05, RISCOS + .2, .1) * (1 - s.crescer(.05, RISCOS + .2, .7))
        return s.reverb(some + vulto + chia, .9, .25, 5500)
    if nome == 'cid_slime':
        # O arremesso: o estalo molhado ao sair da mão e o slime voando, um borbulhar grave que vai embora.
        sai = 1.1 * umido(s, 0, 1., 190) + .5 * silvo(s, 0, .1, 700, 6000)
        voa = s.girar(s.passa_baixa(s.ruido(90, 2400, .7), 900), hz=14, prof=.7) * s.janela(.03, VOO + .05, .06) * (1 - .6 * s.crescer(0, VOO, 1.))
        bolhas = .3 * sum(s.tom(300 + 90 * k, 520 + 120 * k, .6, .06 * k + .04, .06 * k + .1) * s.env(.06 * k + .04, .004, 45) for k in range(6))
        return s.reverb(sai + .5 * voa + bolhas, .6, .16, 4500)
    if nome == 'cid_correntes':
        # As correntes sobem do chão correndo (4 ticks), travam num estalo seco e ficam esticadas, rangendo
        # de vez em quando, até soltarem.
        sobem = .55 * elos(s, 0, FECHAR, 110) + .5 * umido(s, 0, .8, 120)
        trava = 1.2 * s.transiente(FECHAR, 6500, .008) + .9 * elos(s, FECHAR, FECHAR + .04, 160, 1700) + .9 * s.sub(FECHAR, 130, 55, 9, 3)
        range_ = .14 * s.girar(np.tanh(3 * s.tom(310, 290, 1., FECHAR, PRESO, (1, 2.3, 3.7), vibrato=.02, vib_hz=6)), hz=1.3, prof=.9)
        range_ = range_ * s.janela(FECHAR + .1, PRESO - SOLTAR, .2)
        # Elas começam a soltar junto com o visual, que some nos últimos ticks.
        solta = .5 * elos(s, PRESO - SOLTAR, PRESO + .05, 60, 1500) * (1 - .6 * s.crescer(PRESO - SOLTAR, PRESO + .05, 1.))
        return s.reverb(sobem + trava + s.passa_baixa(range_, 2600) + solta, 1.0, .22, 5000)
    if nome == 'cid_overdrive':
        # A magia transborda: um estouro seco, o grave abrindo junto com o anel (8 ticks) e depois o zumbido
        # baixo que fica os 8 s, pulsando devagar, e some no fim.
        estouro = 1.2 * s.transiente(0, 6000, .01) + 1.2 * s.sub(0, 100, 34, 2.4, 4) + .6 * silvo(s, 0, ESTOURO, 400, 8000, .8)
        zumbe = s.girar(magia(s), hz=.9, prof=.35) * s.crescer(0, ESTOURO, .7) * s.janela(0, OVERDRIVE, .25)
        fio = .07 * s.girar(s.tom(742, 760, 1., 0, OVERDRIVE, (1, 2.01)), hz=2.3, prof=.8) * s.janela(.2, OVERDRIVE, .4)
        return estouro + s.reverb(.42 * s.passa_baixa(zumbe, 900) + fio, 1.6, .25, 3000)
    if nome == 'cid_traje':
        # O traje engole o golpe: um baque surdo, o slime espirrando e voltando para o lugar.
        engole = 1.1 * umido(s, 0, 1., 120) + .6 * s.transiente(0, 2500, .012)
        espirra = .5 * s.passa_baixa(s.ruido(400, 5000, .4), 3000) * s.env(.01, .01, 12)
        volta = .3 * sum(s.tom(240 + 70 * k, 420 + 90 * k, .6, .12 + .07 * k, .18 + .07 * k) * s.env(.12 + .07 * k, .004, 40) for k in range(4))
        return s.reverb(engole + espirra + volta, .5, .15, 3500)
    if nome == 'cid_espada_carga':
        # A magia corre para a lâmina: o zumbido cresce e um tom limpo sobe até o corte (que é outro som).
        sobe = .5 * s.tom(220, 1320, 1.6, 0, CARGA_DA_ESPADA, (1, 2.01, 3.02)) * s.crescer(0, CARGA_DA_ESPADA, 1.3)
        zumbe = .5 * s.passa_baixa(magia(s, 70, 5), 1200) * s.crescer(0, CARGA_DA_ESPADA, 1.)
        puxa = .3 * s.ruido(900, 9000, .3) * s.crescer(0, CARGA_DA_ESPADA, 2.2)
        return s.reverb((sobe + zumbe + puxa) * s.janela(0, CARGA_DA_ESPADA + .05, .03), .4, .15, 7000)
    if nome == 'cid_espada_atomica':
        # O corte: um estalo seco, o silvo largo que voa os 4 ticks até o alcance, o grave por baixo e o
        # tom atômico descendo e sumindo na cauda.
        corta = 1.3 * s.transiente(0, 8000, .008) + 1.1 * silvo(s, 0, VOO_DO_CORTE + .12, 600, 14000, .7)
        peso = 1.2 * s.sub(0, 120, 36, 4, 3.5)
        tomq = .4 * s.tom(1320, 440, .6, 0, CORTE_ATOMICO + .5, (1, 2.01, 3.02)) * s.env(0, .004, 5)
        brilho = .25 * s.girar(s.ruido(4000, 14000, .2), hz=17, prof=.7) * s.env(.02, .01, 6)
        rastro = .3 * s.passa_baixa(magia(s, 82, 4), 1500) * s.env(0, .02, 4.5)
        return s.reverb(corta + peso + tomq + brilho + rastro, 1.3, .28, 7500)
    if nome == 'cid_recovery_carga':
        # A espada entra no chão (um baque e o aço), e a luz sobe devagar os 3 s: um acorde claro que cresce,
        # com o zumbido da magia por baixo, até a coluna (que é outro som).
        crava = 1.0 * s.sub(0, 110, 45, 9, 3) + .8 * s.transiente(0, 5500, .008) + .35 * elos(s, 0, .05, 80, 2600)
        acorde = sum(a * s.tom(f, f * 1.012, 1., 0, CARGA_DO_RECOVERY, (1, 2.005), vibrato=.004, vib_hz=4.5)
                     for f, a in ((262, .5), (392, .4), (523, .35), (784, .22)))
        acorde = .4 * s.girar(acorde, hz=.8, prof=.3) * s.crescer(.15, CARGA_DO_RECOVERY, 1.5)
        zumbe = .28 * s.passa_baixa(magia(s, 65, 2.4), 800) * s.crescer(.1, CARGA_DO_RECOVERY, 1.)
        ar = .16 * s.ruido(3000, 12000, .2) * s.crescer(.5, CARGA_DO_RECOVERY, 2.5)
        return crava + s.reverb((acorde + zumbe + ar) * s.janela(0, CARGA_DO_RECOVERY + .05, .04), 1.2, .3, 6500)
    if nome == 'cid_recovery':
        # A coluna sobe: o acorde abre de uma vez, largo e claro, a onda corre pelo chão (10 ticks) e tudo
        # fica soando e some com a luz.
        abre = 1.0 * s.sub(0, 90, 40, 2.2, 3) + .7 * s.transiente(0, 5000, .012) + .7 * silvo(s, 0, ONDA, 500, 10000, .6)
        acorde = sum(a * s.tom(f, harmonicos=(1, 2.005, 3.01), vibrato=.004, vib_hz=5)
                     for f, a in ((262, .5), (330, .35), (392, .45), (523, .4), (784, .28), (1047, .16)))
        acorde = .5 * acorde * s.env(0, .03, 1.6, .25) * s.janela(0, COLUNA + .9, .6)
        sinos = .25 * sum(s.tom(1568 * (1 + .25 * k), harmonicos=(1, 2.76)) * s.env(.08 + .11 * k, .002, 7) for k in range(6))
        ar = .2 * s.girar(s.ruido(2500, 12000, .25), hz=6, prof=.5) * s.janela(0, COLUNA, .5)
        return abre + s.reverb(acorde + sinos + ar, 2.0, .35, 7000)
    if nome == 'cid_atomic_soco':
        # Um soco pesado: o estalo seco e o baque grave no instante do golpe, um nada de silêncio (a parada de
        # impacto) e então o alvo sai: o segundo baque, mais fundo, e o ar rasgado que vai embora.
        bate = 1.3 * s.transiente(0, 3200, .007) + 1.6 * s.sub(0, 120, 42, 11, 3.5)
        osso = .5 * s.passa_baixa(s.ruido(200, 2400, .6), 1500) * s.env(0, .002, 34)
        sai = 1.2 * s.sub(PARADA, 85, 30, 7, 3) + .5 * s.transiente(PARADA, 1800, .012)
        ar = .6 * silvo(s, PARADA, SOCO - PARADA, 500, 6000, .7)
        fio = .15 * s.tom(660, 220, .6, PARADA, SOCO, (1, 2.01)) * s.env(PARADA, .004, 14)
        return s.reverb(bate + osso + sai + ar + fio, .5, .16, 4200)
    if nome == 'cid_atomic_risco':
        # Os dois somem para cima: o ar rasgado que sobe e vai embora, com um fio de magia junto.
        sobe = 1.0 * s.ruido(500, 11000, .3) * s.janela(0, RISCO, .04) * (1 - .7 * s.crescer(0, RISCO, .8))
        tomq = .4 * s.tom(300, 2400, 1.8, 0, RISCO, (1, 2.01)) * s.janela(0, RISCO, .03) * (1 - s.crescer(0, RISCO, 1.))
        parte = 1.0 * s.sub(0, 120, 40, 6, 3) + .6 * s.transiente(0, 5000, .008)
        return s.reverb(sobe + tomq + parte, 1.4, .3, 6500)
    if nome == 'cid_atomic_aura':
        # "I" e "am": quase nada no começo, e um tom limpo que sobe devagar. Na carga ele dispara para o agudo
        # e some, como se fosse sugado, e o zumbido grave some junto: a terceira palavra é dita no silêncio.
        sobe = .45 * s.tom(180, 880, 1.8, 0, DUAS, (1, 2.005, 3.01)) * s.crescer(0, DUAS, 1.6) * s.janela(0, DUAS + .05, .04)
        zumbe = .4 * s.passa_baixa(magia(s, 58, 2.2), 700) * s.crescer(0, DUAS, 1.2) * s.janela(0, MUDA - .2, .5)
        suga = .5 * s.tom(880, 5200, 2.4, DUAS, MUDA, (1, 2.005)) * s.janela(DUAS, MUDA, .05) * (1 - s.crescer(DUAS + .2, MUDA, 1.3))
        ar = .35 * s.ruido(2500, 13000, .3) * s.crescer(DUAS, MUDA - .25, 2.) * s.janela(DUAS, MUDA - .15, .1)
        fecha = .7 * s.transiente(MUDA - .12, 6000, .01) + .5 * s.sub(MUDA - .12, 200, 60, 20, 3)
        return s.reverb(sobe + zumbe + suga + ar + fecha, .6, .2, 8000) * s.janela(0, MUDA + .25, .03)
    if nome == 'cid_atomic':
        # O espaço fica mudo um instante (só um fio agudo), e a magia se solta: o estalo, o grave que abre
        # devagar, o acorde atômico largo e a luz chiando por cima; o ronco cresce com a esfera até o branco.
        fio = .12 * s.tom(3520, 3520, 1., 0, SILENCIO, (1,)) * s.janela(0, SILENCIO, .04)
        abre = 1.7 * s.sub(SILENCIO, 70, 22, .8, 4) + 1.2 * s.transiente(SILENCIO, 4500, .016)
        acorde = sum(a * s.tom(f, harmonicos=(1, 2.005, 3.01), vibrato=.004, vib_hz=5)
                     for f, a in ((110, .6), (165, .45), (220, .5), (330, .4), (440, .32), (660, .2), (880, .12)))
        acorde = .5 * acorde * s.env(SILENCIO, .05, 1.1, .3) * s.janela(SILENCIO, CLARAO + 1.2, .7)
        luz = .3 * s.girar(s.ruido(1500, 13000, .25), hz=5, prof=.5) * s.env(SILENCIO, .04, 1.4, .2) * s.janela(SILENCIO, CLARAO + .8, .6)
        ronco = .7 * s.passa_baixa(s.ruido(30, 400, .9), 180) * (.35 + .65 * s.crescer(SILENCIO, CLARAO, 1.4)) * s.janela(SILENCIO, CLARAO + 1.2, .8)
        cresce = .3 * s.tom(55, 110, 1.5, SILENCIO, CLARAO, (1, 2, 3)) * s.crescer(SILENCIO, CLARAO, 1.8) * s.janela(SILENCIO, CLARAO + .6, .5)
        # O recuo até o sistema solar: um grave largo e parado, o tamanho do espaço, que cresce até o pulso;
        # no pulso, o último estouro, curto e fundo, e o ronco sobe até o branco.
        vasto = .5 * s.passa_baixa(s.ruido(20, 160, 1.), 90) * s.crescer(SILENCIO + 1., PULSO, 1.2) * s.janela(SILENCIO + .8, CLARAO + 1., .9)
        orgao = .18 * sum(s.tom(f, harmonicos=(1, 2.003)) for f in (41.2, 61.7, 82.4)) * s.crescer(SILENCIO + 1.5, PULSO, 1.5) * s.janela(SILENCIO + 1.5, CLARAO + .8, .8)
        pulso = 1.8 * s.sub(PULSO, 60, 20, 1.4, 3.5) + .9 * s.transiente(PULSO, 2600, .02)
        return fio + abre + pulso + s.reverb(acorde + luz + ronco + cresce + vasto + orgao, 2.4, .4, 6000)
    if nome == 'cid_atomic_luz':
        # Do chão: o estouro vem de muito longe e de cima, abafado, um trovão comprido que rola e some.
        ronco = s.passa_baixa(s.ruido(25, 500, 1.), 220) * s.env(.12, .25, .7, .25) * s.janela(.1, LUZ + 1.3, 1.)
        rola = .5 * s.girar(s.passa_baixa(s.ruido(40, 900, .8), 380), hz=3.3, prof=.6) * s.env(.2, .3, 1.0, .2) * s.janela(.15, LUZ + 1.2, .9)
        grave = 1.0 * s.sub(.12, 55, 24, .9, 3)
        acorde = .12 * sum(s.tom(f, harmonicos=(1, 2.005)) for f in (110, 165, 220)) * s.env(.2, .3, .8, .2) * s.janela(.2, LUZ + 1., .8)
        return s.reverb(ronco + rola + grave + acorde, 3.0, .5, 2200)
    if nome == 'cid_atomic_onda':
        # A volta: o baque fundo no chão, a poeira varrida para fora e o ronco que rola e some.
        baque = 1.6 * s.sub(0, 75, 24, 2.2, 3.5) + .9 * s.transiente(0, 2200, .014)
        varre = .7 * s.passa_baixa(s.ruido(120, 5000, .5), 2600) * s.janela(0, CHOQUE, .03) * (1 - .8 * s.crescer(0, CHOQUE, .7))
        rola = .4 * s.girar(s.passa_baixa(s.ruido(40, 600, .8), 260), hz=4, prof=.5) * s.janela(.05, CHOQUE + 1., .7)
        return s.reverb(baque + varre + rola, 1.6, .3, 3800)
    raise ValueError(nome)


def gravado(nome, s):
    """As camadas gravadas (CC0) por cima da síntese: a lâmina, o slime, as correntes e os golpes de verdade.
    A magia continua sintetizada; o atômico ganha explosão e trovão."""
    if nome == 'cid_redemoinho':
        return sum((.55 + .15 * i) * s.amostra(WHOOSH, i * ENTRE_CORTES, 1., 1.15) + .25 * s.amostra(SLIME, i * ENTRE_CORTES, 1., 1.3)
                   for i in range(3))
    if nome == 'cid_pos_imagem':
        return s.amostra(WHOOSH, 0, .7, .85) + s.amostra(PANO, 0, .3, .8)
    if nome == 'cid_slime':
        return s.amostra(SLIME, 0, .9, .9) + s.amostra(LAMA, .02, .4, 1.) + s.amostra(RESPINGO, .03, .3, 1.3)
    if nome == 'cid_correntes':
        return (s.amostra(CORRENTE_MEDIA, 0, .7, 1.15) + s.amostra(CORRENTE_PESADA, FECHAR, 1., .9)
                + s.amostra(CORRENTE_LEVE, PRESO - SOLTAR, .55, 1.))
    if nome == 'cid_overdrive':
        return s.amostra(EXPLOSOES, 0, .6, .7) + s.amostra(CAMPO, 0, .45, .6) * s.janela(0, 1.6, .3)
    if nome == 'cid_traje':
        return s.amostra(SLIME, 0, 1., .7) + s.amostra(BAQUES, 0, .6, .9) + s.amostra(LAMA, .03, .4, 1.1)
    if nome == 'cid_espada_carga':
        return (s.amostra(SOLDA, 0, .3, 1.2) * s.crescer(0, CARGA_DA_ESPADA, 1.5) * s.janela(0, CARGA_DA_ESPADA + .05, .03)
                + s.amostra(SACAR, 0, .35, .9))
    if nome == 'cid_espada_atomica':
        return s.amostra(ESPADA_CORTA, 0, .9, .9) + s.amostra(WHOOSH, 0, .6, .7) + s.amostra(EXPLOSOES, .02, .4, .8)
    if nome == 'cid_recovery_carga':
        return s.amostra(PICARETA, 0, .8, .7) + s.amostra(METAL, 0, .6, .8) + s.amostra(PEDRAS, .03, .3, 1.1)
    if nome == 'cid_recovery':
        return s.amostra(GONGOS, 0, .45, 1.) + s.amostra(TROVOES, 0, .3, 1.2)
    if nome == 'cid_atomic_soco':
        return (s.amostra(MURROS, 0, 1., .8) + s.amostra(MURROS, PARADA, .8, .6)
                + s.amostra(WHOOSH, PARADA, .5, .8))
    if nome == 'cid_atomic_risco':
        return s.amostra(WHOOSH, 0, .8, .6) + s.amostra(VENTO, 0, .4, 1.) * s.janela(0, RISCO + .3, .1)
    if nome == 'cid_atomic':
        return (s.amostra(SUBGRAVE, SILENCIO, 1., .7) + s.amostra(EXPLOSOES, SILENCIO, .8, .5)
                + s.amostra(TROVOES, SILENCIO + .1, .6, .8)
                + s.amostra(EXPLOSOES, PULSO, .7, .45) + s.amostra(SUBGRAVE, PULSO, .8, .6))
    if nome == 'cid_atomic_luz':
        return s.amostra(TROVOES, .12, 1., .8) + .7 * s.amostra(TROVOES, .3, 1., .7)
    if nome == 'cid_atomic_onda':
        return (s.amostra(EXPLOSOES, 0, .9, .55) + s.amostra(PEDRAS, .05, .5, 1.)
                + s.amostra(VENTO, 0, .5, 1.2) * s.janela(0, CHOQUE + .3, .1))
    return None  # cid_atomic_aura: só tons, o silêncio é dela


def sintese(nome, s):
    return hibrido(s, sintetico(nome, s), gravado(nome, s))


def gerar(preview=None):
    m.escrever(SONS, sintese, 'SFX do Cid:', preview, pasta_sons='cid', marcos=MARCOS)


if __name__ == '__main__':
    a = argparse.ArgumentParser()
    a.add_argument('--preview')
    gerar(a.parse_args().preview)
