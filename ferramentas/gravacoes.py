"""As gravações CC0 de art/audio/cc0 (fontes em fontes.json), em grupos, para os geradores de som misturarem à
síntese com Som.amostra. Uma lista sorteia uma gravação por variação; um nome sozinho usa sempre a mesma.

hibrido(): a síntese de antes, um pouco mais baixa, com as camadas gravadas por cima e um pouco de sala.
"""
G = 'bigsoundbank/'
K = 'kenney/'


def _k(base, quantos, ini=0):
    return [K + '%s_%03d.ogg' % (base, i) for i in range(ini, ini + quantos)]


# A garganta e o corpo.
ROSNADOS = [G + 'rosnado_zumbi_%s.ogg' % c for c in 'abcd']
MUGIDO, RUGIDO_GATO, GRUNHIDOS = G + 'mugido.ogg', G + 'rugido_gato.ogg', G + 'grunhidos.ogg'
OSSOS = G + 'ossos_quebrando.ogg'
# Golpes e terra.
MURROS = _k('impactPunch_heavy', 3) + [G + 'soco_a.ogg', G + 'soco_b.ogg']
BAQUES = _k('impactSoft_heavy', 3)
EXPLOSOES = [G + 'explosao_a.ogg', G + 'explosao_b.ogg'] + _k('explosionCrunch', 5)
SUBGRAVE = _k('lowFrequency_explosion', 2)
PEDRAS = [G + 'pedras_%s.ogg' % c for c in 'abc']
PICARETA = _k('impactMining', 3)
MADEIRA = _k('impactWood_heavy', 3)
TROVOES = [G + 'trovao_a.ogg', G + 'trovao_b.ogg']
# Ar, pano e lâmina.
WHOOSH = [G + 'whoosh_%s.ogg' % c for c in 'abcdef']
VENTO = G + 'vento_forte.ogg'
BANDEIRAS = [G + 'bandeira_a.ogg', G + 'bandeira_b.ogg']
VOO_POMBO = G + 'voo_pombo.ogg'
PANO = [K + 'cloth%d.ogg' % i for i in (1, 2, 3)]
ESPADA_CORTA, ESPADA_AR, LAMINA_FACA = G + 'espada_corta.ogg', G + 'espada_ar.ogg', G + 'lamina_faca.ogg'
FACAS = [K + 'knifeSlice.ogg', K + 'knifeSlice2.ogg']
SACAR = [K + 'drawKnife%d.ogg' % i for i in (1, 2, 3)]
PICAR = K + 'chop.ogg'
METAL = _k('impactMetal_heavy', 3)
CORRENTE_PESADA, CORRENTE_MEDIA, CORRENTE_LEVE = G + 'corrente_pesada.ogg', G + 'corrente_media.ogg', G + 'corrente_leve.ogg'
# Fogo, vidro, gelo, líquido.
CHAMAS = [G + 'fogo_chama.ogg', G + 'fogo_chama_b.ogg', G + 'fogo_galhos.ogg']
IGNICAO = G + 'fogo_ignicao.ogg'
JATO = _k('thrusterFire', 2)
VIDRO = [G + 'vidro_estoura.ogg', G + 'espelho_explode.ogg'] + _k('impactGlass_heavy', 3)
GELO = G + 'gelo_rachando.ogg'
SLIME = _k('slime', 2)
LAMA, RESPINGO = G + 'lama.ogg', G + 'respingo_grande.ogg'
# Energia.
CAMPO = _k('forceField', 5)
LASER_GRANDE, LASER_PEQUENO = _k('laserLarge', 3), _k('laserSmall', 3)
CHOQUE_ELETRICO, SOLDA = G + 'choque_eletrico.ogg', G + 'solda.ogg'
MOTOR_GRAVE = K + 'spaceEngineLow_000.ogg'
GONGOS = [G + 'gongo.ogg', G + 'gongo_forte.ogg']


def hibrido(s, sintetico, gravado, peso=.6, sala=.12):
    """A síntese de antes (com {@code peso}) e as gravações por cima, com um pouco de reverb para colarem."""
    if gravado is None:
        return sintetico
    return peso * sintetico + s.reverb(gravado, .7, sala, 6000)
