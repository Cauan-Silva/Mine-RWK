---
tags: [diario]
---
# 11/11/2026: a arena vira a Escola Jujutsu de verdade

Voltar para [[Diário]]. Spec: [[2026-11-10 Arena do Toji]] (seção do cenário refeita). Branch `arena-escola`. Vem depois de [[2026-11-10 Arena partes 2 e 3]] e de [[2026-11-11 Queda do despertar]].

## Por quê
O autor quis finalizar a arena antes do [[Ichigo]]: a ideia era ela ser a Escola Jujutsu, e o cenário que existia era um pátio cinza com muro, lanternas e doze árvores, sem nenhum prédio. A prévia (`previas/arena_atual.mp4`) também mostrou o céu azul de dia em vez do fim de tarde.

## Referência
Jujutsu Kaisen, caps. 70 a 72 e ep. 27 (wiki do Fandom, páginas "Satoru Gojo vs. Toji Fushiguro" e "Tokyo Metropolitan Curse Technical College", lidas em 11/11). O grupo sobe uma escadaria longa com torii paralelos no sopé do monte, chega ao topo já dentro da barreira da Escola, e é ali que o Toji ataca. A Escola é um conjunto de prédios em estilo japonês tradicional, disfarçada de templo budista, no meio de muitas árvores. Na luta o Toji salta para um telhado, é jogado para dentro de um prédio, e o Azul máximo arrasa os prédios em volta.

## O que foi feito
- **Planta nova** (`core/arena/PlantaEscola.cenario`):
  - escadaria ao sul, de 11 de largura, descendo 14 blocos (um degrau e um patamar por nível), com cinco torii em fila e o torii grande na cabeça;
  - pátio de 41 × 41 com o caminho central de andesito, livre para a luta (só os dois pilares de um torii);
  - salão principal ao norte: base de 2 blocos com escada, paredes brancas (quartzo liso) com pilares e viga de carvalho escuro, porta para o pátio e dois telhados de ardósia profunda;
  - dois pavilhões, a leste e a oeste, com um telhado;
  - telhados de quatro águas com beiral de 2 blocos, subindo meio bloco por faixa (laje, bloco), com os cantos do beiral erguidos;
  - prédios ocos, com lanternas por dentro; lanternas de pedra na borda do pátio;
  - floresta de pinheiros de 8 a 12 blocos em volta e pela encosta, em posições fixas.
- **Caixa maior** (`CaixaArena`): 101 × 101 (era 81), fundo 20 blocos abaixo do pátio (era 8) para caber a encosta. O jogador entra a 16 blocos do centro e o Toji no centro.
- **Céu:** hora fixa da dimensão de 11500 para 12400 (pôr do sol).
- **Montagem:** a limpeza olha 40 mil posições por tick (a caixa tem 775 mil). O cenário tem 49 mil blocos: cerca de 13 ticks para pôr. A primeira versão tinha 73 mil; a floresta ficou um pouco menos densa e a pedra sob a grama passou de 3 camadas para 1.
- **Prévia da planta:** `ferramentas/previa_arena.py` atualizado (materiais novos, caixa de 101).

## Testes
- JUnit (`PlantaEscolaTest`): cabe na caixa e não repete posição; a caixa inteira tem chão sem buracos; pátio com piso inteiro e 4 camadas; pátio livre; entradas com chão e espaço, dentro do pátio; a escadaria desce com os torii em fila e dá para andar nela; prédios ocos, com porta e telhado sem furo, e o salão mais alto que os pavilhões; a floresta não entra no pátio, na escadaria nem nos prédios; lanternas com apoio.
- `CaixaArenaTest` e os GameTests de `TestesArena` passaram a usar as medidas da caixa em vez de números soltos.
- `./gradlew build` e os 62 GameTests passam.
- Prévia no jogo: `previas/arena_escola.mp4`.

## Conhecido
- **Espera na entrada:** na gravação a tela de carregamento do jogo ("Loading terrain") ficou mais longa que com o cenário antigo. A gravação tira uma foto por tick e varia muito de uma rodada para outra, então não deu para medir direito: fica para o teste do autor.
- A escadaria acaba na borda da caixa: do último patamar para o sul é a barreira e, atrás das árvores, o vazio.
- Os prédios não têm móveis nem divisórias.

## Revisão (subagente)
Nenhum achado crítico nem importante. Conferido sem problema: medidas antigas da caixa em outros arquivos, queda no vazio na base da escadaria, chunks forçados, orientação das escadas, apoio das lanternas e frestas no telhado.

Corrigido:
- sobras da pose forçada da queda (import e duas chamadas que desfaziam uma pose que ninguém mais força);
- o teste das lanternas agora exige poste de pedra ou assoalho embaixo, e não qualquer bloco.

Ficou anotado, sem mudar:
- **O Toji pode surgir em cima de um telhado** se o jogador estiver no ar perto de um beiral (o Desaparecer e a Execução aceitam qualquer chão firme até 2 blocos abaixo). Ele desce sozinho. Se atrapalhar no teste, a regra pode exigir chão a no máximo 1 bloco.
- **Cabeça ainda mexendo** por até 1 s se a luta acabar logo depois de ele se levantar (a animação da queda dura mais 25 ticks). Só visual.
- O teste da animação da queda confere a rotação do corpo e a duração, não a posição.

## Falta
O teste do autor no jogo: ver [[Pendências]].
