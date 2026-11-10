---
tags: [diario]
---
# 10/11/2026: VFX pesado e nova ordem do R

Voltar para [[Diário]]. Specs: [[2026-11-10 Preparação e VFX pesado do Vazio Roxo máximo]] e [[2026-11-10 Ordem do R e cutscene do soco]].

## VFX pesado
- Estudamos os tutoriais do Photon2 e de animação ([[Tutoriais de VFX e animação]]).
- O Photon passou a ser montado em código (`FxCodigo`), sem editor. O bloom vem da cor HDR, e a casca da explosão usa um shader com fresnel e distorção (`casca_roxo`).
- O ritmo do R ficou cerca de 2x mais lento, com um silêncio de 4 ticks antes do estouro.
- A fusão ficou em quatro tempos (`FusaoRoxoGojo`): dança em espiral, gota, toque e núcleo.
- A explosão ficou em camadas: fitas, onda de choque, fumaça, detritos e partículas pairando.
- O `/rwk previa` e a run `clientDemo` gravam a prévia quadro a quadro.

## Nova ordem do R (teste do autor: "quase lá")
- O rosto do alvo bugava porque a câmera entrava na cabeça dele.
  - Agora todo plano depois do soco fica a pelo menos 2 blocos do alvo (teste JUnit).
  - O close da fusão passou a ser de três quartos pela esquerda.
  - A troca do plano de lado para o da esquerda é um corte seco, para não atravessar o alvo.
- O soco ganhou uma cutscene própria:
  - corrida com a câmera baixa atrás do Gojo;
  - corte seco para um plano de lado e de baixo no uppercut;
  - **impact frame** de 3 ticks em preto e branco com toque de roxo, piscando invertido (pós-processamento `shaders/post/impacto`, decisão em `ImpactFrameGojo`).
- O Azul e o Vermelho só aparecem no céu. O tempo das esferas continua 30 ticks: 8 nascendo nas mãos, 10 de viagem e 12 de dança.
- O encantamento não tem mais esferas nas mãos. No lugar entraram uma aura de fitas azuis e vermelhas subindo em espiral e os olhos brilhando.
