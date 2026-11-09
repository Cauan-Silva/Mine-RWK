---
tags: [diário]
---
# Gojo impacto

Spec: [[2026-11-09 Gojo impacto]]. O autor testou o VFX e achou os golpes simples e sem peso; escolheu câmera, detritos e efeitos maiores (som fica para o pipeline de áudio).

- `ImpactoGojo` (núcleo): tabela de tremor, clarão e FOV por momento, caindo com a distância; o cliente dispara a partir dos mesmos eventos de visual, sem pacote novo.
- Clarão é uma camada de GUI acima de `CAMERA_OVERLAYS`; o FOV respeita a opção "Efeitos de FOV" do jogo.
- Detritos: o servidor solta pedaços do bloco real ao apagar (máx. 12 por chamada); o resto (explosão, Azul, rastro do Roxo, poeira do domínio) é partícula do cliente.
- Brilho falso (modo 42 do shader) no lugar do bloom, que brigaria com o Iris.
- Armadilha: heredoc grande no bash quebra e às vezes roda pela metade. Escrever arquivos com o Write e scripts Python no scratchpad.
