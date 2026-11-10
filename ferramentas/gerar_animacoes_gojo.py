"""Gera src/main/resources/assets/minerwk/player_animations/gojo.json.

Rodar: python ferramentas/gerar_animacoes_gojo.py. Se o JSON for ajustado no Blockbench, o
Blockbench passa a ser a fonte e este script não deve ser rodado de novo por cima.

Formato (formato GeckoLib/Bedrock, abre no Blockbench).

Convenções do modelo do jogador: braço com X negativo vai para a frente; Z do braço direito positivo
abre para fora (o esquerdo é o espelho, Z negativo); Y do tronco gira a cintura. Posição em pixels
(16 = 1 bloco). A suavização de cada quadro vale para o trecho que chega nele.
"""
import json, os

N = [0, 0, 0]
S, Q, C = "easeInOutSine", "easeInOutQuad", "easeInOutCubic"
BACK, EXPO, OUTC = "easeOutBack", "easeOutExpo", "easeOutCubic"


def trilha(quadros):
    out = {}
    for t, v, *e in sorted(quadros, key=lambda q: q[0]):
        out[f"{t:.2f}"] = {"vector": v, "easing": e[0] if e else S}
    return out


def anim(duracao, ossos, loop=False):
    bones = {}
    for osso, props in ossos.items():
        bones[osso] = {p: trilha(q) for p, q in props.items()}
    return {"animation_length": duracao, "loop": loop, "bones": bones}


def tremer(osso_base, t0, t1, passo, amp):
    """Quadros alternando em volta de osso_base entre t0 e t1 (mão que treme segurando a força)."""
    q, t, lado = [], t0 + passo, 1
    while t < t1 - 1e-6:
        q.append((round(t, 2), [osso_base[0] + amp * lado, osso_base[1] - amp * lado * .5, osso_base[2]], S))
        t += passo
        lado = -lado
    return q


def balancar(base, t0, t1, amp=4, passo=0.3):
    """Pose segurada que respira: oscila devagar em volta de base (braço nunca fica congelado)."""
    q, t, lado = [], t0 + passo, 1
    while t < t1 - 1e-6:
        q.append((round(t, 2), [base[0] + amp * lado, base[1], base[2] + amp * .5 * lado], S))
        t += passo
        lado = -lado
    return q


def sem_pernas(a):
    """Golpes usados andando: tira pernas e corpo para a passada normal do jogo aparecer."""
    for osso in ("right_leg", "left_leg", "body"):
        a["bones"].pop(osso, None)
    return a


def fatia(a, inicio):
    """A mesma animação a partir de `inicio` (para pular direto para a pose do disparo)."""
    nova = {"animation_length": round(a["animation_length"] - inicio, 2), "loop": False, "bones": {}}
    for osso, props in a["bones"].items():
        nova["bones"][osso] = {}
        for p, q in props.items():
            tempos = sorted(q, key=float)
            antes = [t for t in tempos if float(t) <= inicio + 1e-6]
            base = q[antes[-1]]["vector"] if antes else N
            out = {"0.00": {"vector": base, "easing": S}}
            for t in tempos:
                if float(t) > inicio + 1e-6:
                    out[f"{float(t) - inicio:.2f}"] = q[t]
            nova["bones"][osso][p] = out
    return nova


A = {}

# Z: Azul. Puxa o braço para trás, empurra a mão aberta com um tranco e segura tremendo.
braco_azul = [-95, 0, 5]
A["gojo_azul"] = anim(3.4, {
    "right_arm": {"rotation": [(0, N), (0.18, [-25, 10, 40], OUTC), (0.30, braco_azul, BACK)]
                  + tremer(braco_azul, 0.30, 3.0, 0.15, 3) + [(3.0, braco_azul), (3.4, N, C)]},
    "left_arm": {"rotation": [(0, N), (0.18, [-15, 0, -20], OUTC), (0.32, [-55, 35, -5], BACK)]
                 + balancar([-56, 35, -5], 0.32, 3.0) + [(3.0, [-58, 35, -5]), (3.4, N, C)]},
    "torso": {"rotation": [(0, N), (0.18, [0, 25, 0], OUTC), (0.30, [0, -15, 0], BACK)]
              + balancar([0, -13, 0], 0.30, 3.0, 2, 0.5) + [(3.0, [0, -12, 0]), (3.4, N, C)]},
    "head": {"rotation": [(0, N), (0.18, [5, 12, 0], OUTC), (0.32, [-4, -8, 6], BACK), (3.0, [-4, -8, 6]), (3.4, N, C)]},
    "right_leg": {"rotation": [(0, N), (0.30, [-14, 0, 2], BACK), (3.0, [-14, 0, 2]), (3.4, N, C)]},
    "left_leg": {"rotation": [(0, N), (0.30, [10, 0, -2], BACK), (3.0, [10, 0, -2]), (3.4, N, C)]},
})

# X: Vermelho. Carga com o indicador à frente e a outra mão no pulso; o corpo pende para trás.
# No disparo (1,5 s com a carga cheia) o braço dá um coice e o corpo recua um passo.
A["gojo_vermelho"] = anim(3.0, {
    "right_arm": {"rotation": [(0, N), (0.40, [-88, 12, 0], BACK)] + tremer([-88, 12, 0], 0.40, 1.40, 0.2, 2)
                  + [(1.40, [-80, 18, 4]), (1.50, [-100, -2, 0], EXPO), (1.62, [-135, -6, 6], OUTC),
                     (2.10, [-95, 0, 0], C), (2.60, [-90, 0, 0]), (3.0, N, C)]},
    "left_arm": {"rotation": [(0, N), (0.40, [-70, 40, 0], BACK), (1.40, [-66, 44, 0]), (1.50, [-40, 20, -15], EXPO),
                              (1.62, [-20, 0, -25], OUTC), (2.60, [-10, 0, -10]), (3.0, N, C)]},
    "torso": {"rotation": [(0, N), (0.40, [0, 12, 0]), (1.40, [6, 18, 0], Q), (1.50, [-4, -6, 0], EXPO),
                           (1.62, [8, -12, 0], OUTC), (2.10, [2, -6, 0], C), (3.0, N, C)]},
    "head": {"rotation": [(0, N), (0.40, [0, 8, 0]), (1.40, [-6, 12, 0], Q), (1.62, [6, -6, 0], OUTC), (3.0, N, C)]},
    "right_leg": {"rotation": [(0, N), (1.40, [-6, 0, 2]), (1.62, [18, 0, 3], OUTC), (2.40, [10, 0, 2], C), (3.0, N, C)]},
    "left_leg": {"rotation": [(0, N), (1.40, [6, 0, -2]), (1.62, [-12, 0, -3], OUTC), (2.40, [-6, 0, -2], C), (3.0, N, C)]},
})
sem_pernas(A["gojo_vermelho"])
A["gojo_vermelho_disparo"] = fatia(A["gojo_vermelho"], 1.40)

# C: Infinito. Mão esquerda de palma aberta à frente do peito, pose calma.
A["gojo_infinito"] = anim(1.2, {
    "left_arm": {"rotation": [(0, N), (0.25, [-80, -20, 0], BACK)] + balancar([-79, -21, 0], 0.25, 0.9, 3, 0.2)
                 + [(0.90, [-78, -22, 0]), (1.2, N, C)]},
    "right_arm": {"rotation": [(0, N), (0.25, [6, 0, 6]), (0.90, [6, 0, 6]), (1.2, N, C)]},
    "torso": {"rotation": [(0, N), (0.25, [0, 10, 0], BACK), (0.90, [0, 10, 0]), (1.2, N, C)]},
    "head": {"rotation": [(0, N), (0.30, [-8, 0, 0]), (0.90, [-8, 0, 0]), (1.2, N, C)]},
    "right_leg": {"rotation": [(0, N), (0.25, [0, 0, 3]), (0.90, [0, 0, 3]), (1.2, N, C)]},
    "left_leg": {"rotation": [(0, N), (0.25, [0, 0, -3]), (0.90, [0, 0, -3]), (1.2, N, C)]},
})

# V: Roxo. Braços abertos (Azul numa mão, Vermelho na outra) se juntam devagar à frente;
# disparo com o braço direito esticando no tranco, o corpo avança e as pernas abrem em base.
A["gojo_roxo"] = anim(3.6, {
    "right_arm": {"rotation": [(0, N), (0.35, [-60, 0, 60], BACK), (1.30, [-85, 25, 0], Q), (1.42, [-60, 30, 12], OUTC),
                               (1.50, [-100, -5, 0], BACK)] + tremer([-100, -5, 0], 1.50, 3.2, 0.15, 2.5)
                  + [(3.2, [-98, -4, 0]), (3.6, N, C)]},
    "left_arm": {"rotation": [(0, N), (0.35, [-60, 0, -60], BACK), (1.30, [-85, -25, 0], Q), (1.42, [-70, -20, 0]),
                              (1.50, [-20, 0, -25], EXPO)] + balancar([-18, 0, -22], 1.50, 3.2)
                 + [(3.2, [-15, 0, -20]), (3.6, N, C)]},
    "torso": {"rotation": [(0, N), (0.35, [-4, 0, 0]), (1.30, [0, 0, 0], Q), (1.42, [0, 20, 0], OUTC),
                           (1.50, [0, -20, 0], BACK), (3.2, [0, -16, 0]), (3.6, N, C)]},
    "head": {"rotation": [(0, N), (0.35, [-6, 0, 0]), (1.30, [6, 0, 0], Q), (1.50, [-4, 0, 0], BACK), (3.2, [-4, 0, 0]), (3.6, N, C)]},
    "right_leg": {"rotation": [(0, N), (1.42, [4, 0, 2]), (1.50, [-20, 0, 4], BACK), (3.2, [-18, 0, 4]), (3.6, N, C)]},
    "left_leg": {"rotation": [(0, N), (1.42, [-4, 0, -2]), (1.50, [15, 0, -4], BACK), (3.2, [14, 0, -4]), (3.6, N, C)]},
})

# R: Vazio Roxo máximo. Encantamento de 4 s, um gesto por palavra (ticks 2, 22, 42, 62), com transição,
# a mão segurando a força (tremor) e o corpo respirando entre eles:
#   Nove Cordas: mãos abrem para os lados, palmas para cima (Azul na esquerda, Vermelho na direita);
#   Luz Polarizada: braços cruzam na frente do peito, o tronco gira de um lado para o outro;
#   Corvo e Declaração: cabeça para trás, braços abertos para cima, o corpo arqueando;
#   Entre a Frente e o Verso: mãos voltam para a frente quase se tocando, tremendo, olhar no alvo.
# No disparo (4,0 s) o Gojo avança com o punho direito para trás (até 1 s, o avanço mais longo). O cliente
# move o Gojo de verdade, então o modelo não mexe o body. O resto vem de gojo_roxo_maximo, que o servidor
# toca no soco.
CORDAS_D, CORDAS_E = [-35, 0, 65], [-35, 0, -65]
CRUZ_D, CRUZ_E = [-85, 38, 0], [-80, -38, 0]
CEU_D, CEU_E = [-165, 0, 35], [-165, 0, -35]
FRENTE_D, FRENTE_E = [-88, -14, 0], [-88, 14, 0]
A["gojo_roxo_200"] = anim(5.0, {
    "right_arm": {"rotation": [(0, N), (0.45, CORDAS_D, BACK)] + tremer(CORDAS_D, 0.45, 0.95, 0.1, 3)
                  + [(0.95, CORDAS_D), (1.3, CRUZ_D, BACK)] + tremer(CRUZ_D, 1.3, 1.95, 0.13, 3)
                  + [(1.95, CRUZ_D), (2.35, CEU_D, BACK)] + tremer(CEU_D, 2.35, 2.95, 0.12, 4)
                  + [(2.95, CEU_D), (3.35, FRENTE_D, BACK)] + tremer(FRENTE_D, 3.35, 3.9, 0.06, 4)
                  + [(3.9, FRENTE_D), (4.0, [45, 0, 12], EXPO)] + tremer([45, 0, 12], 4.0, 5.0, 0.1, 3)
                  + [(5.0, [45, 0, 12])]},
    "left_arm": {"rotation": [(0, N), (0.5, CORDAS_E, BACK)] + tremer(CORDAS_E, 0.5, 0.95, 0.1, 3)
                 + [(0.95, CORDAS_E), (1.35, CRUZ_E, BACK)] + tremer(CRUZ_E, 1.35, 1.95, 0.13, 3)
                 + [(1.95, CRUZ_E), (2.4, CEU_E, BACK)] + tremer(CEU_E, 2.4, 2.95, 0.12, 4)
                 + [(2.95, CEU_E), (3.4, FRENTE_E, BACK)] + tremer(FRENTE_E, 3.4, 3.9, 0.06, 4)
                 + [(3.9, FRENTE_E), (4.0, [-60, 0, -10], EXPO), (5.0, [-62, 0, -10])]},
    "torso": {"rotation": [(0, N), (0.45, [-4, 0, 0]), (0.7, [-2, 4, 0]), (0.95, [-4, -4, 0]),
                           (1.3, [6, 15, 0], C), (1.5, [6, -15, 0], C), (1.7, [6, 14, 0], C), (1.95, [5, -10, 0], C),
                           (2.35, [-14, 0, 0], C), (2.65, [-16, 3, 0]), (2.95, [-13, -3, 0]),
                           (3.35, [6, 0, 0], C), (3.6, [7, 3, 0]), (3.9, [6, 0, 0]),
                           (4.0, [18, 22, 0], EXPO), (5.0, [20, 22, 0])]},
    "head": {"rotation": [(0, N), (0.30, [20, 0, 0], C), (0.95, [16, 0, 0]),
                          (1.3, [10, -12, 0], C), (1.95, [10, 10, 0], C),
                          (2.35, [-32, 0, 0], C), (2.95, [-28, 0, 0]),
                          (3.35, [-3, 0, 0], C), (3.9, [-6, 0, 0]),
                          (4.0, [-14, -10, 0], EXPO), (5.0, [-14, -10, 0])]},
    "right_leg": {"rotation": [(0, N), (1.3, [0, 0, 3]), (2.35, [-4, 0, 5], C), (3.35, [6, 0, 4], C),
                               (3.8, [10, 0, 4], S), (3.96, [12, 0, 4]), (4.0, [40, 0, 4], EXPO), (5.0, [42, 0, 4])]},
    "left_leg": {"rotation": [(0, N), (1.3, [0, 0, -3]), (2.35, [6, 0, -5], C), (3.35, [-4, 0, -4], C),
                              (3.8, [6, 0, -4], S), (3.96, [8, 0, -4]), (4.0, [-30, 0, -4], EXPO), (5.0, [-32, 0, -4])]},
})

# Depois do soco (tempos em segundos a partir do uppercut; ticks do RoxoMaximo): uppercut com o braço
# direito subindo (0), lá no alto as mãos se abrem para os lados e para trás, esquerda e depois direita
# (1,5 s = esferas), fusão às costas carregando (3 s) com as mãos tremendo cada vez mais, parado no
# silêncio antes do estouro, explosão (5 s) e fim (6,5 s). No ar, uma perna dobrada
# para a frente e a outra solta, balançando. Na explosão a esfera passa por ele: tronco para a frente,
# cabeça baixa e braços abertos segurando o vento; depois o punho estendido até o fim.
ABRE, FUSAO, EXPLODE, FIM = 1.5, 3.0, 5.0, 6.5
SILENCIO = EXPLODE - 0.2
FIM_ANIM = 7.0
REACAO = EXPLODE + 0.35
A["gojo_roxo_maximo"] = anim(FIM_ANIM, {
    "right_arm": {"rotation": [(0, [45, 0, 12]), (0.12, [-170, 0, 8], EXPO), (0.6, [-165, 0, 10]),
                               (ABRE + 0.2, [20, 0, 80], BACK)]
                  + tremer([20, 0, 80], ABRE + 0.2, SILENCIO, 0.15, 3)
                  + [(EXPLODE, [-40, 0, 70], EXPO), (REACAO, [-45, 0, 72]), (REACAO + 0.15, [-90, 0, 0], BACK)]
                  + tremer([-90, 0, 0], REACAO + 0.15, FIM, 0.1, 2) + [(FIM, [-90, 0, 0]), (FIM_ANIM, N, C)]},
    "left_arm": {"rotation": [(0, [-62, 0, -10]), (0.12, [10, 0, -20], EXPO), (0.6, [12, 0, -22]),
                              (ABRE, [20, 0, -80], BACK)] + balancar([20, 0, -80], ABRE, SILENCIO, 3, 0.25)
                 + [(EXPLODE, [-40, 0, -70], EXPO), (REACAO, [-45, 0, -72]), (REACAO + 0.2, [10, 0, -30], C),
                    (FIM, [10, 0, -28]), (FIM_ANIM, N, C)]},
    "torso": {"rotation": [(0, [20, 22, 0]), (0.12, [-12, -20, 0], EXPO), (0.6, [-10, -18, 0]),
                           (ABRE + 0.2, [-6, 0, 0], C), ((ABRE + FUSAO) / 2, [-4, 3, 0]), (SILENCIO, [-8, -2, 0]),
                           (EXPLODE, [16, 0, 0], EXPO), (EXPLODE + 0.15, [19, 2, 0]), (REACAO, [17, -2, 0]),
                           (REACAO + 0.15, [4, -12, 0], C), (FIM, [4, -10, 0]), (FIM_ANIM, N, C)]},
    "head": {"rotation": [(0, [-14, -10, 0]), (0.12, [-25, 0, 0], EXPO), (0.6, [-20, 0, 0]),
                          (ABRE + 0.2, [0, 0, 0], C), (SILENCIO, [2, 0, 0]),
                          (EXPLODE, [28, 0, 0], EXPO), (REACAO, [26, 0, 0]),
                          (REACAO + 0.15, [-4, 8, 0], C), (FIM, [-4, 8, 0]), (FIM_ANIM, N, C)]},
    "right_leg": {"rotation": [(0, [42, 0, 4]), (0.12, [-35, 0, 6], EXPO), (0.6, [-40, 0, 6], C)]
                  + balancar([-42, 0, 6], 0.6, EXPLODE, 4, 0.2)
                  + [(EXPLODE, [-20, 0, 8], EXPO), (FIM, [-25, 0, 6]), (FIM_ANIM, N, C)]},
    "left_leg": {"rotation": [(0, [-32, 0, -4]), (0.12, [25, 0, -6], EXPO), (0.6, [10, 0, -4], C)]
                 + balancar([10, 0, -4], 0.6, EXPLODE, 3, 0.25)
                 + [(EXPLODE, [18, 0, -8], EXPO), (FIM, [12, 0, -6]), (FIM_ANIM, N, C)]},
})
# Sem alvo: as mãos abrem na hora; os tempos se ajustam (esferas 1,5 s, fusão 2 s, explosão 1,5 s).
A["gojo_roxo_maximo_sem_alvo"] = fatia(A["gojo_roxo_maximo"], ABRE - 0.2)

# Descida lenta depois do R, em loop até pousar: braços meio abertos, pernas soltas, olhando para baixo.
DESC_D, DESC_E = [-10, 0, 35], [-10, 0, -35]
A["gojo_roxo_maximo_descida"] = anim(2.0, {
    "right_arm": {"rotation": [(0, DESC_D), (1.0, [-16, 0, 42], S), (2.0, DESC_D, S)]},
    "left_arm": {"rotation": [(0, DESC_E), (1.0, [-16, 0, -42], S), (2.0, DESC_E, S)]},
    "torso": {"rotation": [(0, [4, 0, 0]), (1.0, [7, 0, 0], S), (2.0, [4, 0, 0], S)]},
    "head": {"rotation": [(0, [24, 0, 0]), (1.0, [28, 4, 0], S), (2.0, [24, 0, 0], S)]},
    "right_leg": {"rotation": [(0, [8, 0, 4]), (1.0, [-4, 0, 5], S), (2.0, [8, 0, 4], S)]},
    "left_leg": {"rotation": [(0, [-6, 0, -4]), (1.0, [6, 0, -5], S), (2.0, [-6, 0, -4], S)]},
}, loop=True)


# G: Vazio Infinito. Mão direita à frente do rosto com os dedos cruzados; cabeça abaixa e ergue.
A["gojo_vazio_infinito"] = anim(2.5, {
    "right_arm": {"rotation": [(0, N), (0.35, [-120, -35, 10], BACK), (2.0, [-118, -36, 10]), (2.5, N, C)]},
    "left_arm": {"rotation": [(0, N), (0.35, [5, 0, -8]), (2.0, [5, 0, -8]), (2.5, N, C)]},
    "torso": {"rotation": [(0, N), (0.35, [0, -5, 0], BACK), (2.0, [0, -5, 0]), (2.5, N, C)]},
    "head": {"rotation": [(0, N), (0.35, [15, 0, 0], OUTC), (0.60, [15, 0, 0]), (1.0, [-10, 0, 0], C), (2.0, [-10, 0, 0]), (2.5, N, C)]},
    "right_leg": {"rotation": [(0, N), (0.35, [0, 0, 3]), (2.0, [0, 0, 3]), (2.5, N, C)]},
    "left_leg": {"rotation": [(0, N), (0.35, [0, 0, -3]), (2.0, [0, 0, -3]), (2.5, N, C)]},
})

for nome in ("gojo_azul", "gojo_infinito", "gojo_roxo"):
    sem_pernas(A[nome])

saida = {"format_version": "1.8.0", "geckolib_format_version": 2, "animations": A}
p = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/minerwk/player_animations/gojo.json")
with open(p, "w", encoding="utf-8") as f:
    json.dump(saida, f, ensure_ascii=False, indent=1)
    f.write("\n")
print("ok", list(A))
