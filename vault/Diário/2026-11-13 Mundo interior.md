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

## Revisão final (revisor separado)
Nenhum achado crítico, um importante e nove menores. Corrigidos:
- **Importante: no céu escuro do Mugetsu e do Roxo Máximo, o horizonte e a chuva continuavam claros por cima.**
  - **Também na Escola e em Oriana:** o horizonte já tinha esse problema lá.
  - **A correção:** o anel, a saia e a chuva escurecem junto com o céu do golpe (`HorizonteCliente.escuroDosGolpes`).
  - **Sem teste automático,** porque é desenho do cliente. Está em Pendências.
- **Menores, corrigidos junto:**
  - **o comentário e a Pendência prometiam a chuva depois de sair e voltar:** sair do jogo encerra a luta, então isso não acontece. Os textos dizem agora o que acontece de fato;
  - **a chuva acompanhava a câmera ao pular e cair:** agora fica presa ao mundo;
  - **a saia do horizonte trocava de cor de uma vez:** agora escurece junto com o resto, em 3 s;
  - **o VFX reduzido não valia para a chuva:** agora ela cai num raio menor, com metade dos respingos e o som a cada 20 ticks;
  - **o vidro claro das laterais saía cortado no meio de uma janela:** agora o sorteio é por janela.
- **Menores, deixados para depois:**
  - um teste de jogo com a arena de verdade (chuva na fase 2 e o vazio na volta);
  - o teste dos detalhes poderia vigiar o vão entre os prédios e as marquises;
  - o despejo da planta para a prévia roda em todo build;
  - a caixa d'água e o letreiro do prédio baixo servem de esconderijo dos golpes do Zangetsu. O autor julga jogando.

## Prévia
`previas/mundo_interior.gif` e a folha `previas/mundo_interior.png` (`./gradlew runClientDemo -Pdemo=interior`). A prévia força a fase 2 no meio e volta ao mundo sem chuva. A planta vista de cima: `python ferramentas/previa_arena.py interior`.

## Falta
O autor ver e testar no jogo (roteiro em [[Pendências]]). Com isso, o pedido das arenas do teste de 12/11 está completo.
