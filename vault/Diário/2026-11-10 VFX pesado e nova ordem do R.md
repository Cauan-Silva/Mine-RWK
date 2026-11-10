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

## Segundo teste do autor (10/11)
O autor viu o Azul e o Vermelho serem jogados para baixo, sem a fusão atrás do Gojo, e achou a explosão "muito simples e rápida".
- **Esferas para baixo:** causa provável, não confirmada no jogo. O servidor calculava o centro da fusão com a posição que ele tinha do Gojo, mas quem move o Gojo na subida é o cliente.
  - O servidor agora tira o centro da mesma conta do movimento (`deslocamentoGojo` no fim da subida).
  - O cliente desenha a fusão sempre atrás do Gojo de verdade, na horizontal, e as esferas nascem nas mãos abertas para os lados, como na animação.
- **Explosão:** cresce por 3 s em vez de 1,5 s, estourando rápido e depois devagar (`fracaoExplosao`), e o que sobra fica mais 2,5 s no ar (`TICKS_RESTO`).
  - Camadas novas: miolo escuro, relâmpagos correndo na casca, levas de fitas a cada 12 ticks, coluna de luz, três ondas de choque em sequência e chuva de faíscas.
  - O plano aberto da cutscene passou de 40 para 80 ticks, e as animações foram regeradas.
## Terceiro teste do autor (10/11)
"Quase perfeito": a fumaça saía junto com o estouro e cobria a explosão. Agora a fumaça da cratera só sai quando a casca termina de crescer (3 s depois do estouro).

## Enquanto o autor almoçava (10/11)
O autor achou que "melhorou bastante" e deixou quatro tópicos rodando.
- **Céu e chão no encantamento** (do spec do VFX pesado):
  - o céu escurece aos poucos na carga, fica cheio na fusão e na explosão e volta devagar (`CeuRoxo`, um véu desenhado depois do céu e antes do terreno);
  - rachaduras de luz crescem no chão a partir dos pés (`EncantamentoGojo`, com JUnit);
  - pedras e faíscas sobem devagar em volta do Gojo (Photon);
  - a prévia gravável ganhou o modo `-Pdemo=encantamento`, e o vídeo foi para o autor.
- **`/rwk recarregar [jogador]`:** enche a energia e zera as recargas, para testar sem trocar de kit.
- **Guia:** primeira versão de [[Como portar um personagem]].
- **Som:** [[2026-11-10 Som do Gojo]], aguardando aprovação. O Minezin já tem um motor de síntese do próprio autor, então a proposta é portá-lo, sem áudio de terceiros.
- **Próximo personagem:** pesquisa e proposta de kit do [[Toji]], aguardando aprovação. A wiki da obra recusou a leitura automática; parte das armas veio só de fontes secundárias e está marcada na nota.
