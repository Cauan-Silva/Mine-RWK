"""Gera src/main/resources/assets/minerwk/player_animations/cid.json.

Rodar: python ferramentas/gerar_animacoes_cid.py. Se o JSON for ajustado no Blockbench, o
Blockbench passa a ser a fonte e este script não deve ser rodado de novo por cima.

Mesmo formato e mesmas convenções de gerar_animacoes_ichigo.py: braço com X negativo vai para a frente
(-180 é acima da cabeça); Z do braço direito positivo abre para fora; Y do tronco gira a cintura; o osso
"body" é o corpo inteiro. A espada está na mão direita. Cada animação tem o id da habilidade e começa com
a carga dela; os tempos (em segundos) são os do kit em core/personagens/cid/Cid.
"""
import json, os

N = [0, 0, 0]
S, C = "easeInOutSine", "easeInOutCubic"
BACK, EXPO, OUTC, LIN = "easeOutBack", "easeOutExpo", "easeOutCubic", "linear"


def trilha(quadros):
    out = {}
    for t, v, *e in sorted(quadros, key=lambda q: q[0]):
        out[f"{t:.2f}"] = {"vector": v, "easing": e[0] if e else S}
    return out


def anim(duracao, ossos):
    return {"animation_length": duracao, "loop": False,
            "bones": {osso: {"rotation": trilha(q)} for osso, q in ossos.items()}}


A = {}

# Z, o Redemoinho de Ébano: 0,1 s de carga, 0,6 s de giro (três cortes) e 0,2 s de saída. Ele arma o braço
# para trás, estica a espada de lado e o corpo inteiro dá uma volta. Acaba em 360 graus, que é a pose de
# partida: quando a animação solta o osso, nada pula.
A["cid_redemoinho"] = anim(0.9, {
    "body": [(0, N), (0.10, [0, -35, 0], OUTC), (0.70, [0, 360, 0], LIN), (0.90, [0, 360, 0])],
    "right_arm": [(0, N), (0.10, [-30, 0, 60], OUTC), (0.18, [-88, 0, 88], EXPO), (0.70, [-86, 0, 86]), (0.90, N, C)],
    "left_arm": [(0, N), (0.10, [20, 0, -20], OUTC), (0.18, [35, 0, -55], EXPO), (0.70, [32, 0, -50]), (0.90, N, C)],
    "torso": [(0, N), (0.10, [6, -18, 0], OUTC), (0.18, [8, 12, 0], EXPO), (0.70, [8, 10, 0]), (0.90, N, C)],
    "head": [(0, N), (0.18, [6, 0, 0], OUTC), (0.70, [6, 0, 0]), (0.90, N, C)],
})

# X, a Pós-imagem: sai num tick. O corpo mergulha para a frente, a espada fica para trás, baixa.
A["cid_pos_imagem"] = anim(0.4, {
    "right_arm": [(0, N), (0.05, [48, 0, 22], EXPO), (0.22, [42, 0, 20]), (0.4, N, C)],
    "left_arm": [(0, N), (0.05, [36, 0, -12], EXPO), (0.22, [30, 0, -10]), (0.4, N, C)],
    "torso": [(0, N), (0.05, [22, 0, 0], EXPO), (0.22, [18, 0, 0]), (0.4, N, C)],
    "head": [(0, N), (0.05, [-16, 0, 0], EXPO), (0.22, [-14, 0, 0]), (0.4, N, C)],
})

# C, a Prisão de Correntes Negras: 0,15 s puxando a mão esquerda para trás, e ela lança o slime para a frente.
A["cid_prisao"] = anim(0.65, {
    "left_arm": [(0, N), (0.15, [35, 0, -25], OUTC), (0.22, [-92, 12, 0], EXPO), (0.42, [-88, 10, 0]), (0.65, N, C)],
    "right_arm": [(0, N), (0.15, [-10, 0, 12], OUTC), (0.22, [22, 0, 18], EXPO), (0.42, [20, 0, 16]), (0.65, N, C)],
    "torso": [(0, N), (0.15, [0, 22, 0], OUTC), (0.22, [6, -20, 0], EXPO), (0.42, [5, -17, 0]), (0.65, N, C)],
    "head": [(0, N), (0.15, [0, -12, 0]), (0.22, [0, 10, 0], OUTC), (0.42, [0, 9, 0]), (0.65, N, C)],
})

# G, o Overdrive: 0,1 s encolhendo e os braços abrem de uma vez, peito para cima, com a magia estourando.
A["cid_overdrive"] = anim(0.7, {
    "right_arm": [(0, N), (0.10, [-28, 0, -14], OUTC), (0.18, [24, 0, 52], EXPO), (0.45, [20, 0, 46]), (0.7, N, C)],
    "left_arm": [(0, N), (0.10, [-28, 0, 14], OUTC), (0.18, [24, 0, -52], EXPO), (0.45, [20, 0, -46]), (0.7, N, C)],
    "torso": [(0, N), (0.10, [12, 0, 0], OUTC), (0.18, [-12, 0, 0], EXPO), (0.45, [-10, 0, 0]), (0.7, N, C)],
    "head": [(0, N), (0.10, [14, 0, 0], OUTC), (0.18, [-22, 0, 0], EXPO), (0.45, [-18, 0, 0]), (0.7, N, C)],
})

saida = {"format_version": "1.8.0", "geckolib_format_version": 2, "animations": A}
p = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/minerwk/player_animations/cid.json")
with open(p, "w", encoding="utf-8") as f:
    json.dump(saida, f, ensure_ascii=False, indent=1)
    f.write("\n")
print("ok", list(A))
