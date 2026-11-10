---
tags: [diario]
---
# 10/11/2026: maestria, parte 1

Voltar para [[Diário]]. Spec: [[2026-11-10 Maestria e árvore de habilidades]] (aprovado em 10/11). Branch `maestria-arvore`.

## Por que agora
O autor pediu maestria e árvore de habilidades antes do [[Toji]]: a luta só fica disponível depois de um tempo de jogo. O item 1 da Fase 4 foi adiantado.

## O que foi feito
- **Regras (`core/maestria/Maestria`):** nível 1 a 100, `100 + 10 × (n − 1)` de XP por nível, 1 ponto por nível, XP por dano (1 por ponto, teto de 20 por golpe) e por abate (25), só em inimigos.
- **Dados salvos:** o `DadosKit` guarda a maestria de cada personagem (`DadosMaestria`: XP, nós e "despertou"). Trocar de personagem não apaga.
- **Ganho de XP:** só o dano causado pelos golpes do kit conta. O `ContextoServidor` marca o dano como "golpe do kit"; soco e espada não dão XP.
- **Subir de nível:** mensagem na barra de ação, som e partículas.
- **HUD:** o nível à direita da barra do recurso e uma linha fina de XP embaixo dela.
- **Comando:** `/rwk maestria <jogador> nivel <n> | xp <quantia> | zerar`.

## Testes
JUnit em `MaestriaTest` e `LogicaHudTest`; um GameTest novo (`soGolpeDoKitEmInimigoDaXpDeMaestria`). Os 22 GameTests passam.

## O que ficou diferente do spec
- O teto de 20 de XP vale por inimigo atingido, e não pelo golpe inteiro: um golpe em área que acerta cinco inimigos pode dar até 100.

## Falta
As partes 2 (árvore por dentro) e 3 (tela da árvore). O HUD ainda não foi visto no jogo.
