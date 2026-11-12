# 2026-11-12 O buraco do Hollow de verdade

A pedido do autor, o buraco no peito da forma Hollow do [[Ichigo]] deixou de ser pintado: agora atravessa o corpo e mostra o que está atrás.

## O que mudou
- **O traje** (`ferramentas/gerar_traje_hollow.py`): o vão tem 3x3 pixels do modelo, no meio do peito, 5 pixels abaixo do ombro, com a borda preta. Fica transparente na frente e nas costas, e quatro paredes pretas finas fecham o túnel por dentro. As marcas pretas até os ombros continuam.
- **O tronco do jogador some** enquanto ele está na forma Hollow (`CamadaTraje.antesDoJogador`, no `RenderPlayerEvent.Pre`), senão a skin apareceria no vão. Só o tronco e a jaqueta da skin; cabeça, braços e pernas continuam embaixo do traje como antes.

## Achado no caminho
- **A camada de brilho do GeckoLib pintava de preto os pixels transparentes** dos trajes sem `_glowmask`. Ela desenha o modelo de novo com uma máscara vazia. Agora só os trajes com máscara (Gojo e Cid) têm essa camada. Efeito colateral bom: os farrapos transparentes da manga do Hollow e dos trajes do Ichigo (Shikai, Bankai, Mugetsu) passam a ser recortados de verdade. **Conferir** se algum desses trajes mudou de cara.

Prévia: `previas/hollow_buraco.mp4` (`./gradlew runClientDemo -Pdemo=hollow_perto`).
