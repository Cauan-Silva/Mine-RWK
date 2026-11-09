"""Gera assets/minerwk/player_animations/gojo.json (formato GeckoLib/Bedrock, abre no Blockbench).

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
    for t, v, *e in quadros:
        out[f"{t:.2f}"] = {"vector": v, "easing": e[0] if e else S}
    return out


def anim(duracao, ossos):
    bones = {}
    for osso, props in ossos.items():
        bones[osso] = {p: trilha(q) for p, q in props.items()}
    return {"animation_length": duracao, "loop": False, "bones": bones}


def tremer(osso_base, t0, t1, passo, amp):
    """Quadros alternando em volta de osso_base entre t0 e t1 (mão que treme segurando a força)."""
    q, t, lado = [], t0 + passo, 1
    while t < t1 - 1e-6:
        q.append((round(t, 2), [osso_base[0] + amp * lado, osso_base[1] - amp * lado * .5, osso_base[2]], S))
        t += passo
        lado = -lado
    return q


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
    "left_arm": {"rotation": [(0, N), (0.18, [-15, 0, -20], OUTC), (0.32, [-55, 35, -5], BACK),
                              (3.0, [-58, 35, -5]), (3.4, N, C)]},
    "torso": {"rotation": [(0, N), (0.18, [0, 25, 0], OUTC), (0.30, [0, -15, 0], BACK), (3.0, [0, -12, 0]), (3.4, N, C)]},
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
A["gojo_vermelho_disparo"] = fatia(A["gojo_vermelho"], 1.40)

# C: Infinito. Mão esquerda de palma aberta à frente do peito, pose calma.
A["gojo_infinito"] = anim(1.2, {
    "left_arm": {"rotation": [(0, N), (0.25, [-80, -20, 0], BACK), (0.90, [-78, -22, 0]), (1.2, N, C)]},
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
                              (1.50, [-20, 0, -25], EXPO), (3.2, [-15, 0, -20]), (3.6, N, C)]},
    "torso": {"rotation": [(0, N), (0.35, [-4, 0, 0]), (1.30, [0, 0, 0], Q), (1.42, [0, 20, 0], OUTC),
                           (1.50, [0, -20, 0], BACK), (3.2, [0, -16, 0]), (3.6, N, C)]},
    "head": {"rotation": [(0, N), (0.35, [-6, 0, 0]), (1.30, [6, 0, 0], Q), (1.50, [-4, 0, 0], BACK), (3.2, [-4, 0, 0]), (3.6, N, C)]},
    "right_leg": {"rotation": [(0, N), (1.42, [4, 0, 2]), (1.50, [-20, 0, 4], BACK), (3.2, [-18, 0, 4]), (3.6, N, C)]},
    "left_leg": {"rotation": [(0, N), (1.42, [-4, 0, -2]), (1.50, [15, 0, -4], BACK), (3.2, [14, 0, -4]), (3.6, N, C)]},
})

# R: Roxo 200%. Cabeça baixa, quatro gestos (um por palavra do encantamento: ticks 2, 22, 42, 62),
# o corpo sobe 0,6 bloco ao longo da carga; disparo aos 4,0 s no tranco mais forte; desce no fim.
SOBE = [0, 9.6, 0]
A["gojo_roxo_200"] = anim(7.0, {
    "body": {"position": [(0, N), (3.8, SOBE, S), (6.2, SOBE), (7.0, N, C)]},
    "right_arm": {"rotation": [(0, N), (0.50, [-110, -30, 0], BACK), (1.0, [-108, -32, 0]),
                               (1.50, [-60, -40, 0], BACK), (2.0, [-60, -42, 0]),
                               (2.50, [-70, -45, 0], BACK), (3.0, [-72, -45, 0]),
                               (3.50, [-120, 0, 0], BACK), (3.88, [-118, 2, 0]), (3.96, [-70, 35, 15], OUTC),
                               (4.0, [-95, -5, 0], EXPO), (4.15, [-108, -8, 4], OUTC)]
                  + tremer([-96, -5, 0], 4.15, 6.2, 0.1, 3) + [(6.2, [-96, -5, 0]), (7.0, N, C)]},
    "left_arm": {"rotation": [(0, N), (0.50, [-10, 0, -10]), (1.0, [-10, 0, -10]),
                              (1.50, [-90, 30, 0], BACK), (2.0, [-92, 30, 0]),
                              (2.50, [-70, 45, 0], BACK), (3.0, [-72, 45, 0]),
                              (3.50, [-30, 0, -30], BACK), (3.96, [-35, 0, -30]),
                              (4.0, [-15, 0, -40], EXPO), (6.2, [-15, 0, -35]), (7.0, N, C)]},
    "torso": {"rotation": [(0, N), (0.50, [4, -10, 0]), (1.50, [4, 10, 0], C), (2.50, [0, 0, 0], C),
                           (3.50, [0, 18, 0], C), (3.96, [0, 25, 0], OUTC), (4.0, [0, -25, 0], EXPO),
                           (6.2, [0, -20, 0]), (7.0, N, C)]},
    "head": {"rotation": [(0, N), (0.30, [20, 0, 0], C), (2.40, [16, 0, 0]), (3.0, [-5, 0, 0], C),
                          (3.96, [-8, 0, 0]), (4.0, [-2, -6, 0], EXPO), (6.2, [-2, -6, 0]), (7.0, N, C)]},
    "right_leg": {"rotation": [(0, N), (3.8, [10, 0, 4], S), (3.96, [12, 0, 4]), (4.0, [-25, 0, 8], EXPO),
                               (6.2, [-22, 0, 8]), (7.0, N, C)]},
    "left_leg": {"rotation": [(0, N), (3.8, [6, 0, -4], S), (3.96, [8, 0, -4]), (4.0, [20, 0, -8], EXPO),
                              (6.2, [18, 0, -8]), (7.0, N, C)]},
})

# G: Vazio Infinito. Mão direita à frente do rosto com os dedos cruzados; cabeça abaixa e ergue.
A["gojo_vazio_infinito"] = anim(2.5, {
    "right_arm": {"rotation": [(0, N), (0.35, [-120, -35, 10], BACK), (2.0, [-118, -36, 10]), (2.5, N, C)]},
    "left_arm": {"rotation": [(0, N), (0.35, [5, 0, -8]), (2.0, [5, 0, -8]), (2.5, N, C)]},
    "torso": {"rotation": [(0, N), (0.35, [0, -5, 0], BACK), (2.0, [0, -5, 0]), (2.5, N, C)]},
    "head": {"rotation": [(0, N), (0.35, [15, 0, 0], OUTC), (0.60, [15, 0, 0]), (1.0, [-10, 0, 0], C), (2.0, [-10, 0, 0]), (2.5, N, C)]},
    "right_leg": {"rotation": [(0, N), (0.35, [0, 0, 3]), (2.0, [0, 0, 3]), (2.5, N, C)]},
    "left_leg": {"rotation": [(0, N), (0.35, [0, 0, -3]), (2.0, [0, 0, -3]), (2.5, N, C)]},
})

saida = {"format_version": "1.8.0", "geckolib_format_version": 2, "animations": A}
p = os.path.join(r"C:\Users\MAIS INTERNET\Documents\Mine-RWK", "src/main/resources/assets/minerwk/player_animations/gojo.json")
with open(p, "w", encoding="utf-8") as f:
    json.dump(saida, f, ensure_ascii=False, indent=1)
    f.write("\n")
print("ok", list(A))
