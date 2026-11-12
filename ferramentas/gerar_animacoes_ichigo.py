"""Gera src/main/resources/assets/minerwk/player_animations/ichigo.json.

Rodar: python ferramentas/gerar_animacoes_ichigo.py. Se o JSON for ajustado no Blockbench, o
Blockbench passa a ser a fonte e este script não deve ser rodado de novo por cima.

Mesmo formato e mesmas convenções de gerar_animacoes_gojo.py: braço com X negativo vai para a frente
(-180 é acima da cabeça); Z do braço direito positivo abre para fora; Y do tronco gira a cintura.
A espada está na mão direita. As poses do Getsuga, da onda e da entrada do Bankai partem das do Minezin
(mesmo autor), com os tempos dos golpes daqui. Só braços, tronco e cabeça: as pernas seguem a passada.
"""
import json, os

N = [0, 0, 0]
S, C = "easeInOutSine", "easeInOutCubic"
BACK, EXPO, OUTC = "easeOutBack", "easeOutExpo", "easeOutCubic"


def trilha(quadros):
    out = {}
    for t, v, *e in sorted(quadros, key=lambda q: q[0]):
        out[f"{t:.2f}"] = {"vector": v, "easing": e[0] if e else S}
    return out


def anim(duracao, ossos):
    return {"animation_length": duracao, "loop": False,
            "bones": {osso: {"rotation": trilha(q)} for osso, q in ossos.items()}}


def tremer(base, t0, t1, passo, amp):
    """Quadros alternando em volta de base entre t0 e t1 (o braço que treme segurando a força)."""
    q, t, lado = [], t0 + passo, 1
    while t < t1 - 1e-6:
        q.append((round(t, 2), [base[0] + amp * lado, base[1] - amp * lado * .5, base[2]], S))
        t += passo
        lado = -lado
    return q


A = {}

# Z do Shikai, a carga: as duas mãos erguem a Zangetsu acima do ombro direito e seguram (até 0,5 s).
ALTO_D, ALTO_E, ALTO_T, ALTO_C = [-165, -35, -25], [-150, 22, -10], [-8, -28, 0], [4, 14, 0]
A["ichigo_getsuga"] = anim(1.2, {
    "right_arm": [(0, N), (0.10, [-60, -25, -20], OUTC), (0.25, ALTO_D, BACK)] + tremer(ALTO_D, 0.25, 1.0, 0.1, 2)
                 + [(1.0, ALTO_D), (1.2, N, C)],
    "left_arm": [(0, N), (0.10, [-45, 20, -10], OUTC), (0.25, ALTO_E, BACK), (1.0, [-152, 22, -10]), (1.2, N, C)],
    "torso": [(0, N), (0.25, ALTO_T, OUTC), (1.0, [-9, -29, 0]), (1.2, N, C)],
    "head": [(0, N), (0.25, ALTO_C), (1.0, ALTO_C), (1.2, N, C)],
})
# Z do Shikai, o corte: desce na diagonal de uma vez e segura o fim do golpe.
A["ichigo_getsuga_disparo"] = anim(0.7, {
    "right_arm": [(0, ALTO_D), (0.08, [-40, 30, 10], EXPO), (0.16, [-18, 40, 14], OUTC), (0.35, [-24, 36, 12]), (0.7, N, C)],
    "left_arm": [(0, ALTO_E), (0.08, [-55, -8, -6], EXPO), (0.16, [-32, -18, -10], OUTC), (0.35, [-34, -16, -10]), (0.7, N, C)],
    "torso": [(0, ALTO_T), (0.08, [10, 22, 0], EXPO), (0.16, [14, 28, 0], OUTC), (0.35, [12, 25, 0]), (0.7, N, C)],
    "head": [(0, ALTO_C), (0.08, [-2, -10, 0], OUTC), (0.35, [-2, -12, 0]), (0.7, N, C)],
})

# Z do Bankai: um corte de lado com uma mão só, seco (o golpe sai em 0,1 s).
A["ichigo_getsuga_negro"] = anim(0.55, {
    "right_arm": [(0, N), (0.08, [-70, 0, 70], OUTC), (0.18, [-85, 45, 0], EXPO), (0.32, [-80, 40, 0]), (0.55, N, C)],
    "left_arm": [(0, N), (0.08, [10, 0, -15], OUTC), (0.18, [22, 0, -28], EXPO), (0.32, [20, 0, -25]), (0.55, N, C)],
    "torso": [(0, N), (0.08, [0, -25, 0], OUTC), (0.18, [4, 30, 0], EXPO), (0.32, [4, 26, 0]), (0.55, N, C)],
    "head": [(0, N), (0.08, [0, 12, 0]), (0.18, [0, -14, 0], OUTC), (0.32, [0, -12, 0]), (0.55, N, C)],
})

# X: o Shunpo. O corpo mergulha para a frente com os braços para trás.
A["ichigo_shunpo"] = anim(0.4, {
    "right_arm": [(0, N), (0.05, [40, 0, 14], EXPO), (0.2, [34, 0, 12]), (0.4, N, C)],
    "left_arm": [(0, N), (0.05, [40, 0, -14], EXPO), (0.2, [34, 0, -12]), (0.4, N, C)],
    "torso": [(0, N), (0.05, [20, 0, 0], EXPO), (0.2, [16, 0, 0]), (0.4, N, C)],
    "head": [(0, N), (0.05, [-14, 0, 0], EXPO), (0.2, [-12, 0, 0]), (0.4, N, C)],
})

# C do Shikai: as duas mãos erguem a espada (0,4 s de carga) e a cravam no chão.
A["ichigo_getsuga_chao"] = anim(1.15, {
    "right_arm": [(0, N), (0.26, [-165, 40, 40], OUTC), (0.40, [-178, 40, 40]), (0.48, [-40, 0, -38], EXPO),
                  (0.54, [-26, 0, -38], OUTC), (0.80, [-28, 0, -38]), (1.15, N, C)],
    "left_arm": [(0, N), (0.26, [-165, -40, -40], OUTC), (0.40, [-178, -40, -40]), (0.48, [-40, 0, 38], EXPO),
                 (0.54, [-26, 0, 38], OUTC), (0.80, [-28, 0, 38]), (1.15, N, C)],
    "torso": [(0, N), (0.40, [-14, 0, 0], OUTC), (0.48, [24, 0, 0], EXPO), (0.54, [30, 0, 0], OUTC), (0.80, [26, 0, 0]),
              (1.15, N, C)],
    "head": [(0, N), (0.40, [10, 0, 0]), (0.48, [-14, 0, 0], OUTC), (0.80, [-14, 0, 0]), (1.15, N, C)],
})

# V do Shikai: gira a Zangetsu pelo pano acima da cabeça (0,3 s), lança e fica de braço estendido até
# ela voltar (0,8 s depois); o braço recolhe ao pegá-la.
SOLTO = [-88, 10, 0]
A["ichigo_arremesso"] = anim(1.4, {
    "right_arm": [(0, N), (0.08, [-150, 0, 35], OUTC), (0.16, [-172, 0, -20]), (0.24, [-150, 0, 35]),
                  (0.30, [-130, -20, 30]), (0.38, SOLTO, EXPO)] + tremer(SOLTO, 0.38, 1.05, 0.14, 2.5)
                 + [(1.05, SOLTO), (1.15, [-55, -12, 12], BACK), (1.4, N, C)],
    "left_arm": [(0, N), (0.30, [-20, 0, -30], OUTC), (0.38, [25, 0, -30], EXPO), (1.05, [20, 0, -25]), (1.4, N, C)],
    "torso": [(0, N), (0.30, [-4, -22, 0], OUTC), (0.38, [8, 24, 0], EXPO), (1.05, [5, 18, 0]), (1.15, [0, -6, 0], OUTC),
              (1.4, N, C)],
    "head": [(0, N), (0.30, [0, 10, 0]), (0.38, [0, -12, 0], OUTC), (1.05, [0, -10, 0]), (1.4, N, C)],
})

# G do Shikai: a liberação. A espada aponta para a frente com a mão esquerda no braço (1,5 s de carga,
# o braço tremendo); no estouro o braço varre para o lado e a Tensa Zangetsu aparece.
PONTA = [-92, 0, 0]
A["ichigo_bankai"] = anim(2.2, {
    "right_arm": [(0, N), (0.40, [-90, 0, 0], BACK)] + tremer(PONTA, 0.40, 1.30, 0.1, 2) + [(1.30, PONTA),
                  (1.40, [-90, 50, 0], OUTC), (1.50, [-90, -70, 0], EXPO), (1.62, [-84, -82, 0], OUTC),
                  (1.85, [-80, -78, 0]), (2.2, N, C)],
    "left_arm": [(0, N), (0.40, [-90, 55, 0], BACK), (1.30, [-92, 55, 0]), (1.50, [-30, -20, -25], EXPO),
                 (1.85, [-25, -15, -22]), (2.2, N, C)],
    "torso": [(0, N), (0.40, [0, -10, 0]), (0.70, [-8, -10, 0]), (1.30, [-4, -8, 0]), (1.40, [0, 30, 0], OUTC),
              (1.50, [4, -30, 0], EXPO), (1.62, [6, -36, 0], OUTC), (1.85, [5, -32, 0]), (2.2, N, C)],
    "head": [(0, N), (0.40, [0, 10, 0]), (0.70, [-15, 10, 0]), (1.30, [-6, 8, 0]), (1.50, [0, 25, 0], OUTC),
             (1.85, [0, 28, 0]), (2.2, N, C)],
})

# G do Bankai: sacode a lâmina para o lado e baixa a espada.
A["ichigo_sair_bankai"] = anim(0.55, {
    "right_arm": [(0, N), (0.12, [-35, 0, 40], EXPO), (0.28, [8, 0, 14], OUTC), (0.55, N, C)],
    "torso": [(0, N), (0.12, [0, -12, 0], EXPO), (0.28, [2, 4, 0], OUTC), (0.55, N, C)],
    "head": [(0, N), (0.28, [8, 0, 0]), (0.55, N, C)],
})

# C do Bankai: ergue a lâmina na frente do rosto enquanto o Getsuga a cobre.
ERGUIDA = [-115, 25, 0]
A["ichigo_getsuga_lamina"] = anim(0.85, {
    "right_arm": [(0, N), (0.20, ERGUIDA, BACK)] + tremer(ERGUIDA, 0.20, 0.55, 0.1, 2) + [(0.55, ERGUIDA), (0.85, N, C)],
    "left_arm": [(0, N), (0.20, [10, 0, -22], OUTC), (0.55, [10, 0, -20]), (0.85, N, C)],
    "torso": [(0, N), (0.20, [-4, 12, 0], OUTC), (0.55, [-4, 12, 0]), (0.85, N, C)],
    "head": [(0, N), (0.20, [-6, -8, 0]), (0.55, [-6, -8, 0]), (0.85, N, C)],
})

# V do Bankai: a mão livre passa pelo rosto de cima para baixo e a máscara aparece (0,4 s); a cabeça
# baixa com a mão e sobe já mascarada.
A["ichigo_mascara"] = anim(0.9, {
    "left_arm": [(0, N), (0.14, [-150, 38, 12], OUTC), (0.26, [-128, 44, 8]), (0.40, [-92, 36, 0], EXPO),
                 (0.55, [-40, 10, -14], OUTC), (0.9, N, C)],
    "right_arm": [(0, N), (0.26, [8, 0, 16], OUTC), (0.55, [10, 0, 18]), (0.9, N, C)],
    "torso": [(0, N), (0.26, [6, 8, 0], OUTC), (0.40, [-4, 6, 0], EXPO), (0.55, [-3, 4, 0]), (0.9, N, C)],
    "head": [(0, N), (0.26, [18, 0, 0], OUTC), (0.40, [-10, 0, 0], EXPO), (0.55, [-8, 0, 0]), (0.9, N, C)],
})

# R do Bankai: o Mugetsu. Os tempos seguem a cutscene (core/vfx/MugetsuIchigo): até 1,5 s o reiatsu sobe e
# o corpo se abre para trás; na forma final ele fica reto, de braços soltos; aos 4,2 s (o silêncio) o braço
# direito sobe devagar com a lâmina preta; aos 6 s, o corte: desce de uma vez e segura.
ABERTO_D, ABERTO_E = [20, 0, 38], [20, 0, -38]
ERGUE = [-172, -8, -10]
A["ichigo_mugetsu"] = anim(7.3, {
    "right_arm": [(0, N), (0.5, ABERTO_D, OUTC)] + tremer(ABERTO_D, 0.5, 1.4, 0.1, 2.5) + [(1.4, ABERTO_D),
                  (1.6, [4, 0, 8], OUTC), (4.2, [4, 0, 8]), (5.6, ERGUE, C), (5.95, [-176, -8, -10]),
                  (6.06, [-30, 30, 12], EXPO), (6.16, [-12, 38, 14], OUTC), (6.9, [-16, 34, 12]), (7.3, N, C)],
    "left_arm": [(0, N), (0.5, ABERTO_E, OUTC)] + tremer(ABERTO_E, 0.5, 1.4, 0.1, 2.5) + [(1.4, ABERTO_E),
                 (1.6, [4, 0, -8], OUTC), (4.2, [4, 0, -8]), (5.6, [14, 0, -20], C), (6.06, [30, 0, -26], EXPO),
                 (6.9, [26, 0, -22]), (7.3, N, C)],
    "torso": [(0, N), (0.5, [-12, 0, 0], OUTC), (1.4, [-14, 0, 0]), (1.6, N, OUTC), (4.2, N), (5.6, [-8, -14, 0], C),
              (6.06, [14, 22, 0], EXPO), (6.16, [18, 26, 0], OUTC), (6.9, [15, 22, 0]), (7.3, N, C)],
    "head": [(0, N), (0.5, [-22, 0, 0], OUTC), (1.4, [-24, 0, 0]), (1.6, [6, 0, 0], OUTC), (4.2, [4, 0, 0]),
             (5.6, [-6, 8, 0], C), (6.06, [0, -10, 0], OUTC), (6.9, [0, -10, 0]), (7.3, N, C)],
})

# A forma Hollow completa (ninguém aperta tecla: quem toca é a própria forma).
# A transformação: o corpo se abre para trás num rugido e depois se curva para a frente, de garras soltas.
A["ichigo_hollow"] = anim(1.2, {
    "right_arm": [(0, N), (0.15, [20, 0, 55], OUTC), (0.6, [25, 0, 60]), (0.9, [-22, 0, 26], C), (1.2, N, C)],
    "left_arm": [(0, N), (0.15, [20, 0, -55], OUTC), (0.6, [25, 0, -60]), (0.9, [-22, 0, -26], C), (1.2, N, C)],
    "torso": [(0, N), (0.15, [-14, 0, 0], OUTC), (0.6, [-16, 0, 0]), (0.9, [18, 0, 0], C), (1.2, N, C)],
    "head": [(0, N), (0.15, [-30, 0, 0], OUTC), (0.6, [-32, 0, 0]), (0.9, [12, 0, 0], C), (1.2, N, C)],
})
# A garra: o braço direito sobe e desce rasgando na diagonal (o risco aparece aos 0,1 s).
A["ichigo_hollow_garra"] = anim(0.5, {
    "right_arm": [(0, N), (0.08, [-155, -20, 20], OUTC), (0.16, [-30, 40, -10], EXPO), (0.3, [-22, 36, -8]), (0.5, N, C)],
    "left_arm": [(0, N), (0.08, [25, 0, -30], OUTC), (0.3, [20, 0, -26]), (0.5, N, C)],
    "torso": [(0, N), (0.08, [-6, -22, 0], OUTC), (0.16, [14, 26, 0], EXPO), (0.3, [12, 22, 0]), (0.5, N, C)],
    "head": [(0, N), (0.16, [6, -10, 0], OUTC), (0.3, [6, -10, 0]), (0.5, N, C)],
})
# O Cero: a cabeça baixa para os chifres apontarem (1 s de carga), o tranco do disparo, e volta.
A["ichigo_hollow_cero"] = anim(1.9, {
    "right_arm": [(0, N), (0.3, [30, 0, 36], OUTC), (1.0, [34, 0, 40]), (1.08, [44, 0, 50], EXPO), (1.5, [36, 0, 40]), (1.9, N, C)],
    "left_arm": [(0, N), (0.3, [30, 0, -36], OUTC), (1.0, [34, 0, -40]), (1.08, [44, 0, -50], EXPO), (1.5, [36, 0, -40]), (1.9, N, C)],
    "torso": [(0, N), (0.3, [14, 0, 0], OUTC), (1.0, [16, 0, 0]), (1.08, [6, 0, 0], EXPO), (1.5, [10, 0, 0]), (1.9, N, C)],
    "head": [(0, N), (0.3, [22, 0, 0], OUTC), (1.0, [24, 0, 0]), (1.08, [10, 0, 0], EXPO), (1.5, [16, 0, 0]), (1.9, N, C)],
})

saida = {"format_version": "1.8.0", "geckolib_format_version": 2, "animations": A}
p = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/minerwk/player_animations/ichigo.json")
with open(p, "w", encoding="utf-8") as f:
    json.dump(saida, f, ensure_ascii=False, indent=1)
    f.write("\n")
print("ok", list(A))
