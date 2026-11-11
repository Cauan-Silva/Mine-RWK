---
tags: [diario]
---
# 10/11/2026: Arena do Toji, partes 2 e 3 (regras e cenário)

Voltar para [[Diário]]. Spec e plano: [[2026-11-10 Arena do Toji]]. Branch `arena-toji`. Antes: [[2026-11-10 Arena parte 1]].

O autor pediu, no meio da parte 2, que eu fizesse tudo até o fim e trouxesse o relatório: as partes 2 e 3 foram feitas em sequência, sem o teste dele entre elas.

## Parte 2: regras
- **Barreira em caixa:** paredes e teto da caixa de 81 × 81, até 56 blocos acima do pátio. No cliente ela vem só da posição (`CaixaArena.daPosicao`), sem pacote; no servidor (`RegrasArena`) o jogador é puxado de volta se passar 1,5 bloco (pérola, fruta do coro, empurrão) e o Toji, na hora. Espectador passa.
- **Queda no vazio:** quem cai abaixo do fundo da caixa volta ao centro do pátio, com o 5 × 5 do centro refeito, e leva 4 de dano. Vale para o jogador e para o Toji.
- **Blocos:** à mão só no criativo. Os golpes continuam destruindo, mas só dentro da caixa da vaga (`ArenasServidor.podeMexer`, usado no `apagarCasca`, no `arrancarBlocos` e no `apagarBlocos`).
- **Sai do Toji:** a regra dos 64 blocos (`Fim.LONGE`) e a do ajudante (o revide em outro jogador). O alcance de visão dele passou a 96, que cobre a diagonal da caixa.

## Parte 3: cenário
- **`PlantaEscola.cenario()`** (20691 blocos, montados em uns 6 ticks depois dos 21 da limpeza):
  - pátio de 61 × 61 de tijolo de pedra, com a faixa central de 5 e a moldura de andesito polido, e 3 camadas de pedra por baixo;
  - ao norte, escadaria de 21 de largura subindo 8 blocos até um patamar, com o torii grande (vão de 9 × 9) no fundo;
  - dois torii sobre o caminho, em dz −12 e +12 (vão livre de 7 × 7; só os 4 pilares ficam dentro do pátio);
  - muro de 2 de altura a leste, oeste e sul (e ao norte dos dois lados da escadaria), com uma lanterna a cada 6 blocos;
  - doze árvores em posições fixas na faixa de fora.
- **Prévia sem abrir o jogo:** `./gradlew test --tests "*PlantaEscola*"` e `python ferramentas/previa_arena.py` desenham `previas/arena.png` (de cima, de frente e de lado).
- **Tela escurecendo (`rede/EscurecerTela`, `client/arena/Escurecer`):** escurece em meio segundo ao invocar, fica preta durante a montagem e o teleporte, e clareia ao chegar. Na volta, escurece no último meio segundo antes do teleporte. A opção `cutscenes` do cliente não a desliga.

## Decisões
- **Um anel de chunks a mais fica carregado em volta da caixa.** No chunk da borda, o Toji empurrado para fora da parede saía do mundo antes de a barreira agir, e a luta acabava. Custo: 28 chunks a mais carregados por luta.
- **A proteção de blocos deixa passar a consulta dos golpes.** Os golpes perguntam "posso quebrar?" com o mesmo evento da quebra à mão (para respeitar mods de proteção); sem distinguir os dois, nenhum golpe destruía nada na arena. Achado pelos testes.
- **A moldura do pátio é de andesito polido, não de laje** (o plano dizia laje): laje no piso deixaria um degrau de meio bloco na borda da luta.
- **A volta depois da vitória continua aos 5 s**, com a tela escurecendo no último meio segundo (o plano somava o escurecimento: 5,5 s).
- **Sem vitória e com o jogador vivo na arena** (o Toji sumiu aos 10 minutos), a volta espera o meio segundo do escurecimento.

## Testes
- JUnit: `PlantaEscolaTest` (8, com a planta inteira conferida: caixa, pátio livre, entradas, escadaria, torii, muro, lanternas, árvores), `CurvaEscurecerTest` (4), `TojiBossTest` adaptado.
- GameTest: 7 novos (fora da caixa volta para dentro; o Toji não sai da caixa; queda no vazio; espectador passa; não quebra nem põe à mão; apagar e arrancar não passam da caixa). Os 58 passam, em duas rodadas seguidas.
- **Prova num mundo real** (`runClient -Pmundo="New World" -Pprova_arena`): três rodadas. Na primeira, o Toji matou o jogador parado em 9 s (ele tinha ficado desperto de vez na prova da parte 1, e desperto não tem a queda que o protege): a arena fechou e liberou a vaga como devia, mas a prova não pôde conferir a volta. Na segunda, a prova renasceu o jogador à força e a volta caiu no lugar exato, mas o cliente ficou dessincronizado (tela de morte) e o jogador apareceu fora da caixa; isso é efeito do renascimento forçado da ferramenta, não do jogo. Na terceira, com o jogador vivo e imune a dano: chegou à entrada (256.5, 64, 280.5); posto 20 blocos para fora da parede leste, a barreira o trouxe para x = 296,1 (a parede fica em 297); lutou com o Toji a 2,4 blocos; 3 s depois da vitória ainda estava na arena; 7 s depois, de volta ao lugar exato. Sem erros no log. A primeira rodada revelou um caso sem tratamento, já corrigido e com teste: quem entra no jogo morto dentro da arena não é teleportado (renascer já o tira de lá) e não leva um ponto de retorno velho.

## Revisão final (revisor separado, 10/11 à noite)
Um crítico e quatro importantes. O crítico e três importantes foram corrigidos, cada um com um teste que falhou antes e passa agora; os 62 GameTests passam em duas rodadas e a prova num mundo real passou de novo depois das correções.

**Corrigidos**
- **Crítico: morrer na arena e fechar o jogo antes de renascer perdia todos os itens e o XP.** A cópia do inventário ficava só na memória do servidor. Agora os itens voltam para o inventário do próprio morto (é ele que vai para o disco) e quem renasce copia do morto. Teste: `morrerNaArenaMantemItensEXp`.
- **Ponto de retorno obstruído:** se construíram (ou escorreu lava) no lugar de onde o jogador saiu, ele vai para o renascimento normal em vez de voltar dentro do bloco. Água não conta. Teste: `retornoObstruidoVaiParaORenascimento`.
- **Chunks forçados para sempre:** se o servidor parava com uma luta em andamento, os chunks da vaga ficavam carregados em toda sessão seguinte. Ao ligar, todos são soltos. Teste: `aoLigarOServidorNenhumChunkDaArenaFicaForcado`.
- **Montado:** quem está numa montaria desmonta ao ser segurado pela barreira ou ao voltar do vazio (antes a montaria o levava de volta e o dano se repetia). Teste: `quemCaiNoVazioMontadoDesmonta`.

**Não corrigido (decisão)**
- **Mods que mexem nos itens da morte** (Curios, mochilas, túmulos): a arena cancela os itens caídos inteiros, então o que outro mod acrescenta ali seria destruído, e um mod de túmulo poderia duplicar. Só dá para saber com esses mods instalados; fica anotado para quando o mod entrar num modpack.

**Menores, deixados para depois**
- A volta acontece 6,5 s depois do golpe final, não 5: os 5 s contam de quando o corpo do Toji some (1,5 s depois).
- Uma fila só para montar e apagar: com três obras na frente (vários jogadores invocando juntos) a espera passa de 4 s e a tela clareia antes do teleporte.
- Ao chegar, a arena tira Lentidão e Resistência de qualquer origem (poção do jogador).
- Se outro mod cancelar a troca de dimensão, o Toji nasceria no mundo normal nas coordenadas da vaga.
- A montagem não tira itens que ficaram numa vaga depois de uma queda do servidor.
- A proteção de blocos não cobre baldes nem isqueiro.
- Sem a dimensão, a mensagem é "você já está na arena" (o log avisa o motivo certo ao abrir o mundo).
- A planta do cenário é recalculada a cada luta.
- A dimensão herda a chuva do mundo normal: o céu pode escurecer (o spec diz "sem clima").
- A ferramenta `ProvaArena` continua no código (só liga com `-Pprova_arena`).

**O revisor não pôde julgar (só rodando):** travada na montagem (carga dos chunks e luz de 20 mil blocos), tremor do Toji encostado na parede, aviso de "configurações experimentais" ao abrir o mundo, e se a tela preta cobre de fato o carregamento.

## Falta
- **O teste do autor no jogo** (roteiro em [[Pendências]]). Nada do que é visual foi visto por alguém: o cenário, a tela preta, a barreira.
- Juntar o `arena-toji` na `main` e enviar ao GitHub (o branch está só neste PC).
- Depois: o spec do balanceamento de todos os golpes, que era o motivo da arena.
