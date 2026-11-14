---
tags: [diario, arena]
---
# 13/11/2026: o mundo interior do mangá

Voltar para [[Diário]]. Spec e plano: [[2026-11-13 Mundo interior]]. Branch `mundo-interior`.

## De onde veio
É a terceira e última arena do pedido "mais detalhe nas arenas" do teste de 12/11, depois da Escola e de Oriana ([[2026-11-13 Oriana sob ataque]]). O autor escolheu o mundo calmo do mangá, com chuva quando a máscara aparece, e a abordagem A (o mesmo caminho de Oriana, mais a troca de fase pelo cenário).

## O que foi feito
- **Os prédios:**
  - uma parte das janelas acende (luz por baixo do vidro) e outra tem vidro claro, em todas as fachadas;
  - os prédios alto e baixo ganham antenas com para-raios, caixas d'água sobre pés de grade e um letreiro vermelho e amarelo, tudo saindo de lado, como os prédios;
  - o atravessado ganha um letreiro na face que se vê da luta, e as laterais uma marquise.
  - A fachada da luta continua inteira e livre.
- **A fase 2:** quando o Zangetsu veste a máscara, o servidor troca o cenário de quem luta para `interior_chuva` (`ArenasServidor.trocarCenario`). A vaga guarda o cenário novo.
- **No cliente (`CeuInterior`):**
  - o céu azul com 30 nuvens andando devagar de leste para oeste, por cima do pôr do sol;
  - na fase 2, o céu passa a cinza em 3 s e chove, com riscos de chuva, respingos e o som;
  - o horizonte com prédios deitados flutuando até onde a vista alcança (`ferramentas/gerar_horizonte_interior.py`), mais escuro na chuva.
  - As nuvens do jogo somem nesta arena, como em Oriana.

## Decisões
- **A chuva é desenhada por nós:** o bioma das arenas é o vazio, que não tem chuva, então subir o nível de chuva do cliente não mostraria nada. O gancho de chuva da dimensão das arenas desenha os riscos, os respingos são partículas e o som é o da chuva do jogo, tudo só no cliente. Ela é mais simples que a do jogo: não tem a neblina de chuva e não escurece a luz.
- **A planta virou um mapa por posição,** como a de Oriana, para os detalhes poderem trocar blocos sem repetir posição.
- **A verificação de emenda do horizonte** compara só a volta completa (a faixa 3 com a 0). As outras emendas são o mesmo panorama cortado, e a grade densa de janelas fazia a régua antiga falhar sem costura nenhuma.
- **O teste antigo da grade** passou a aceitar a luz no lugar do fundo escuro.

## Testes
- JUnit: janelas acesas em todos os prédios, detalhes fora do campo da luta, antenas com apoio e a fachada da luta inteira.
- GameTest: a máscara manda o cenário da chuva.
- Os 131 passam. Na tarefa 1, duas rodadas caíram por instabilidade de fora: o Vermelho uma vez e o Toji com o GeckoLib outra.

## Prévia
`previas/mundo_interior.gif` e a folha `previas/mundo_interior.png` (`./gradlew runClientDemo -Pdemo=interior`). A prévia força a fase 2 no meio e volta ao mundo sem chuva. A planta vista de cima: `python ferramentas/previa_arena.py interior`.

## Falta
O autor ver e testar no jogo (roteiro em [[Pendências]]). Com isso, o pedido das arenas do teste de 12/11 está completo.
