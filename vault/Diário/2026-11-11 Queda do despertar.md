---
tags: [diario]
---
# 11/11/2026: a queda do despertar não aparecia

Voltar para [[Diário]]. Branch `arena-escola`.

## O problema
O autor reparou que, na luta do [[Toji]], o Gojo não caía na hora do despertar: ficava em pé, parado, durante a cutscene.

## A causa
A queda usava a pose forçada de nadar, definida só no servidor. O cliente do próprio jogador recalcula a pose dele e voltava para "em pé".

## A correção
- Animação nova do jogador, `gojo_queda` (em `ferramentas/gerar_animacoes_gojo.py`): cai de costas em 0,4 s, fica no chão, começa a se erguer 10 ticks antes do corte da câmera e está de pé no corte; depois ergue a cabeça.
- O servidor manda a animação a quem vê o jogador quando o Toji o derruba, e manda parar se a queda for cortada (o Toji morreu ou sumiu).
- A pose forçada saiu.
- Os tempos vêm de `CutsceneDespertar` (`LEVANTA`, `ERGUER`, `DEPOIS`), e o `AnimacoesGojoTest` confere que a animação cobre a queda e a cutscene, deita o corpo e está de pé no corte.

## Testes
`./gradlew build` e os 62 GameTests passam. Conferido na prévia (`-Pdemo=toji`, `previas/arena_atual.mp4`): ele aparece deitado na cutscene e de pé no corte.

## Falta
O teste do autor no jogo: ver [[Pendências]].
