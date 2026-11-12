---
tags: [diario]
---
# 12/11/2026: Ichigo parte 8

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]]. Branch `ichigo-kit`. Vem depois de [[2026-11-12 Ichigo parte 7]]. Com ela o [[Ichigo]] fecha as 8 partes do spec.

## O que foi feito
A forma Hollow completa: a passiva trancada que salva o Ichigo do golpe que mataria no Bankai.

### Regras (núcleo, com JUnit)
- `core/personagens/ichigo/Ichigo`: a forma `hollow` (sem golpes em tecla), `podeVirarHollow`, `virarHollow` (1 de vida, cura 10, recarga de 5 min) e o efeito `FormaHollow`, de 10 s: a cada 30 ticks uma garra (8) em quem está a até 4 blocos ou o Sonído para junto do alvo (até 24); aos 6 s o Cero (20 ticks de carga, feixe de 24 blocos, 18 de dano), uma vez. Ao acabar: Shikai, 6 de vida e o G em recarga de 20 s.
- `Reiatsu`: parado durante a forma. `Contexto` ganhou `vida` e `inimigoProximo`.
- A forma não aparece na tela de escolha (`PersonagemEscolha` pula formas sem golpe).

### Servidor
- `kit/HollowServidor`: o golpe fatal vira a forma (só no Bankai, desperto de vez, fora de arena de boss, com a passiva pronta); na forma, metade do dano, não morre, e clique, item, bloco e entidade não respondem. `/kill` e o vazio continuam matando.
- `ContextoServidor.inimigoProximo`: o inimigo mais perto à vista (monstros, quem mira ou feriu o Ichigo, jogadores com PvP).
- Três GameTests novos (88 no total).

### Apresentação
- **Modelo** (`ferramentas/gerar_traje_hollow.py`): 30 cubos nos ossos de armadura, textura 64×64 pintada no script: máscara com os dois chifres para a frente, cabelo laranja até a cintura, o buraco no peito, tufos vermelhos, garras e o hakama rasgado. Nenhum asset de terceiros.
- **VFX** (`RenderVisuaisIchigo`): o reiatsu preto e vermelho em volta do corpo com o estouro inicial, os três riscos da garra, o Sonído e o Cero violeta (o do Zangetsu interior continua vermelho).
- **Animações do jogador:** a transformação, a garra e o Cero. **Sons** (sintetizados): rugido, garra, carga do Cero e Cero; o Cero do boss passou a usá-los.
- Prévia: `previas/ichigo_parte8_hollow.mp4` (`./gradlew runClientDemo -Pdemo=hollow`).

## Revisão (subagente) e o que mudou por ela
- O golpe fatal passou a ser medido pelo **dano já reduzido** (`LivingDamageEvent.Pre`): antes um golpe forte que a armadura seguraria gastava a forma.
- A forma só vai atrás de **inimigo de verdade** (antes, qualquer mob: vaca, aldeão, animal de outro jogador).
- Na forma também não se usa **item, bloco nem entidade** (antes só o ataque era barrado).
- Forma interrompida (troca de dimensão, saída do jogo) **acaba com o mesmo custo** (antes saía de graça).
- O Cero pega quem está **colado** na boca do feixe.
- Ficou como está: paralisado ou caído, a forma ainda age (ela é quem manda no corpo); `/kill` no meio da forma não encerra o visual no cliente antes do tempo.

## Decisões minhas (mudam com uma palavra do autor)
- Quem a forma ataca (acima) e que o Cero fere quem estiver na linha, inimigo ou não.
- A primeira ação aos 0,75 s e a pausa em volta do Cero (10 ticks antes, 12 depois): dá seis garras e um Cero nos 10 s.
- A forma vem antes do Totem da Imortalidade.
- Não há aviso na tela da recarga de 5 minutos.

## Estado
`./gradlew build` e os 88 GameTests passam. A prévia foi gravada antes da regra nova de alvos (nela a forma ataca golens e slimes); o modo `hollow` já usa husks, sem regravar. O que testar no jogo está em [[Pendências]].
