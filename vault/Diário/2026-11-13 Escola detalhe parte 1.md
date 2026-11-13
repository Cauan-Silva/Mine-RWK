---
tags: [diario, arena]
---
# 13/11/2026: a Escola Jujutsu mais detalhada (parte 1, a planta)

Voltar para [[Diário]]. Spec e plano: [[2026-11-13 Detalhe da Escola Jujutsu]]. Branch `escola-detalhe`.

## De onde veio
No teste de 12/11 o autor pediu mais detalhe nas arenas, começando pela Escola Jujutsu. Ele escolheu "tudo": os prédios por dentro, os prédios mais ricos por fora, um jardim no pátio e um horizonte no céu. A parte 1 é a planta; o horizonte é a parte 2.

## O que foi feito
- **Materiais novos (32):**
  - tatame, mosaico de bambu, shoji, grades de janela, velas, barril, estante, suporte de armas, mesa baixa, tapete, feno, alvo e quadro;
  - cerca escura, água, vitória-régia, cascalho, pedra com musgo, cerejeira (tronco e folha), bambu e sino;
  - escadas de madeira nas quatro direções.
  - Cada um tem o seu bloco em `ObrasArena.estado` e a sua cor na prévia; um teste confere que nenhum fica sem cor.
- **O salão é um templo:**
  - antessala com mesas baixas e almofadas, separada da nave por painéis de papel com uma passagem;
  - nave de tatame com linhas de bambu e pilares;
  - altar com degrau, mesa com velas, lanternas sobre cercas e a estante embutida na parede do fundo.
- **Os pavilhões:**
  - **o leste é a sala de aula:** 16 mesas com almofada viradas para o quadro, e estantes;
  - **o oeste é a sala de treino:** suporte de armas sobre barris, dois alvos sobre feno e um tapete de 5 × 5.
- **Por fora:**
  - a varanda de madeira em volta do salão, com guarda-corpo aberto na escada;
  - grades nas janelas entre os pilares;
  - cercas de pé nos cantos do beiral e a cumeeira escura;
  - a torre do sino a nordeste, o depósito a noroeste e um portão fechado atrás do pavilhão oeste.
- **O jardim:**
  - **lagoa a sudeste:** borda de musgo, 2 de fundo no meio, vitórias-régias e pedras de passo;
  - **jardim seco a sudoeste:** cascalho, borda baixa e três pedras;
  - **plantas:** 5 cerejeiras e bambu atrás dos pavilhões;
  - **acabamento:** bancos virados para o pátio, musgo sorteado na calçada (sempre o mesmo, por um hash da posição) e uma cerca baixa entre o pátio e a floresta.
- **Cerca e shoji se ligam aos vizinhos** na montagem, como o muro já fazia.
- **Total:** 48,8 mil blocos. As áreas novas tiram algumas árvores da floresta e compensam os blocos novos, bem abaixo do teto de 65 mil.

## Decisões
- **O teste dos prédios mudou:**
  - antes, a sala inteira tinha de estar vazia; agora, o meio livre até a viga e um caminho a pé da porta até ele;
  - o piso pode ser qualquer bloco sólido;
  - a lanterna também pode ficar sobre tatame ou cerca.
- **A estante do altar fica embutida na parede do fundo,** para a nave ficar livre até o altar.
- **A entrada da varanda usa a escada de pedra** que já existia; o guarda-corpo fica na borda da base.
- **A água vem por último na lista:** o fundo e a borda da lagoa entram antes, e um teste confere a ordem e que toda água tem vizinho sólido ou água.
- **Um GameTest novo** (`escolaMontaInteiraSemVazarAgua`) monta a Escola inteira e confere a água, o sino e o bambu no mundo.
- **O `vermelhoCongelaEArremessaZumbi` falhou 1 vez em 2 rodadas.** É a instabilidade que já existia; na segunda rodada passaram os 129.

## Prévia
`previas/escola_detalhe_planta.png`: as vistas de cima, de frente e de lado.
