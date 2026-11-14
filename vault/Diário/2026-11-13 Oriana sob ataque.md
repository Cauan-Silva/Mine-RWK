---
tags: [diario, arena]
---
# 13/11/2026: Oriana sob ataque

Voltar para [[Diário]]. Spec e plano: [[2026-11-13 Oriana sob ataque]]. Branch `oriana-ataque`.

## De onde veio
É a segunda arena do pedido "mais detalhe nas arenas" do teste de 12/11; a Escola veio antes ([[2026-11-13 Escola detalhe parte 2]]). O autor escolheu a capital sob ataque, como na obra, e a abordagem A: blocos na arena, mais a cidade e a noite no cliente, pelo mesmo caminho da Escola.

## O que foi feito
- **Materiais novos na planta:** netherrack, fogo, fogueira, terra queimada, blackstone, magma, tijolo rachado e com musgo, lã vermelha e tronco queimado.
- **A muralha:**
  - tijolos rachados e com musgo, manchas pretas na face de dentro e ameias faltando;
  - duas brechas em V (em dx −32 e 30) que descem até a altura 4, com entulho no pé;
  - o portão arrombado: uma folha caída no chão e uma barricada atrás do vão.
- **As torres:** a de x −20 perdeu o telhado e queima por dentro, e as outras têm estandartes vermelhos rasgados.
- **O campo:**
  - o chão queimado e a estrada falhada;
  - seis crateras, só no chão, com brasa no meio;
  - doze focos de fogo em netherrack nas beiradas e três fogueiras;
  - três carroças viradas com feno e barris, duas barricadas e pedras caídas da muralha;
  - seis árvores queimadas e as duas do sul ainda verdes;
  - um poste tombado.
- **No cliente, só em Oriana:**
  - o horizonte com a capital ao norte: telhados, o castelo de três torres e fogo, com o céu laranja e colunas de fumaça. Nas outras direções, vilas queimando ao longe. Gerador `ferramentas/gerar_horizonte_oriana.py`.
  - a noite: uma cúpula avermelhada com estrelas e o brilho do incêndio ao norte, e a névoa no mesmo tom (`NoiteOriana`).
  - fumaça e fagulhas subindo atrás da muralha e da torre, na metade com o VFX reduzido (`FumacaOriana`).

## Decisões
- **As nuvens:** ficavam brancas por cima da noite. A dimensão das arenas ganhou efeitos de céu próprios (`CeuArena`, igual ao do mundo normal) que pulam as nuvens só em Oriana. A Escola e o mundo interior ficam iguais.
- **O fogo não se espalha:** nenhum material que pega fogo a menos de 3 blocos de um foco, nem a menos de 5 na vertical. Um teste confere, e um GameTest deixa o fogo queimar 10 s e confere que nenhum bloco mudou.
- **A muralha que não deixa passar:** para cada coluna, uma das três fileiras fica cheia até a altura 3. O vão do portão é na face de dentro, e a barricada fica atrás.
- **As crateras do meio são só chão:** o meio de 61 × 61 continua livre para o Ragnarok voar.
- **A noite é só de céu:** os blocos continuam com a luz do pôr do sol, porque a hora é uma só para as três arenas.
- **As prévias:** a da Escola e a de Oriana viraram uma só função. O script de prévia das arenas aceita `oriana`.

## Testes
- JUnit no `PlantaOrianaTest`: a muralha não deixa passar, as brechas em V, a muralha rachada, a torre destruída, o portão fechado, o fogo que não se espalha e o campo com crateras e destroços.
- GameTest `orianaNaoPerdeBlocosComOFogo`. Os 131 passam.
- Na primeira rodada da tarefa 2, o servidor de testes caiu de novo por causa do Toji (pacote de animação do GeckoLib). Na segunda passou. É a mesma instabilidade de antes, sem relação com Oriana.

## Revisão final (revisor separado)
A primeira leitura disse "não está pronto": um achado crítico, um importante e seis menores. Corrigidos:
- **Crítico: dava para atravessar a brecha.**
  - **O furo:** o entulho subia até a altura 3 e o fundo do V ficava na 4. Um pulo levava para trás da muralha, onde o jogador ficava preso e a luta travava. O teste antigo olhava só até a altura 3.
  - **A correção:** o entulho baixou, com o topo na 2, e a camada de trás do V virou uma barreira invisível (material `BARREIRA`), que deixa ver a cidade mas segura até quem pula alto com um kit.
  - **O teste:** `ninguemChegaAtrasDaMuralhaAndandoEPulando` anda pela arena a partir da entrada, subindo um bloco e caindo de qualquer altura. Ele falhou antes, em (30, 4, −48), e passa agora.
- **Importante: a noite de Oriana cobria os efeitos dos golpes.**
  - **O que acontecia:** a cúpula pintava por cima do céu escuro do Roxo Máximo e do Mugetsu, e a névoa dela sobrescrevia a do domínio.
  - **A correção:** a noite agora roda antes, com prioridade alta, e não mexe na névoa debaixo d'água.
  - **Sem teste automático,** porque são eventos de desenho do cliente. Vai para Pendências.
- **Menores, corrigidos junto, no mesmo arquivo:** as fagulhas agora aparecem de longe, e a fumaça segue a opção de partículas do jogador.
- **Menores, deixados para depois:**
  - a cúpula é montada a cada quadro, mas poderia ser montada uma vez só;
  - o teste de jogo do fogo não pega fogo novo nascendo fora da planta (quem segura é o teste da distância);
  - o fim da investida do Ragnarok pode pousar dentro de destroços de 1 bloco nas beiradas.

## Depois da revisão: as brasas só nas beiradas
O autor decidiu (13/11) que a brasa, que machuca quem pisa, fica só nas crateras das beiradas. As quatro crateras do meio livre têm o miolo de pedra negra. Teste: `oCampoTemCraterasEDestrocos` falhou antes e passa agora.

## Prévia
`previas/oriana_ataque.gif` e a folha `previas/oriana_ataque.png` (`./gradlew runClientDemo -Pdemo=oriana`). A planta vista de cima: `python ferramentas/previa_arena.py oriana` gera `previas/arena_oriana.png`.

## Falta
O autor ver e testar no jogo (roteiro em [[Pendências]]). A próxima arena é o mundo interior do Ichigo.
