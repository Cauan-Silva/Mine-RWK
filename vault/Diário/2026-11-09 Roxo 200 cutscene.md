---
tags: [diário]
---
# Roxo 200% cutscene

Spec: [[2026-11-09 Roxo 200 cutscene]]. O autor pediu encantamento visível e a cutscene já agora; pesquisa da cena (cap. 223) em [[Gojo]].

- Sistema de câmera reutilizável: `Coreografia` (núcleo, JUnit) com quadros-chave na base de quem usa o golpe; uma cutscene nova é só uma lista nova. A do Roxo 200% fica em `CutsceneRoxo200`, com as palavras.
- Cliente: um `Marker` só do cliente vira a entidade-câmera; HUD, mão e andar ficam bloqueados e o olhar do jogador preso (o golpe sai para onde ele olhava). O corte da carga (`EncerrarVisual`) encerra a cutscene.
- Quem está perto vê as palavras em 3D girando em volta do Gojo e o céu escurecer (mesma neblina do domínio). Config `cutscenes`.
- Protocolo de rede foi para 4. Também nesta rodada: chão sólido do domínio (`piso_dominio`), blocos do Azul orbitando e a marca do Azul desde o começo.
