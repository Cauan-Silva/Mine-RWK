---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec: a arena da luta do Toji

Voltar para [[Roadmap]]. Muda onde acontece a luta de [[2026-11-10 Toji e o despertar do Gojo]]: hoje o [[Toji]] aparece onde o jogador está, no mundo aberto. O autor testou no jogo em 10/11 e pediu uma arena, para depois balancear todos os golpes num terreno sempre igual.

## Escolhas do autor (10/11)
1. **Para quê:** a luta do Toji. Não é arena de treino.
2. **Onde:** uma dimensão própria do mod. Cada luta tem a sua arena, construída no começo e apagada no fim.
3. **Cenário:** a entrada da Escola Jujutsu, como na luta do anime: pátio de pedra, escadaria, portões torii e árvores.
4. **Quem entra:** só quem invocou. A luta do despertar é solo.
5. **Depois:** o balanceamento dos golpes do Gojo e do Toji, com spec próprio (fora deste).

## Entrada
- O gatilho não muda: o Contrato do Matador de Feiticeiros ou `/rwk toji <jogador>`, com as mesmas exigências de hoje.
- O mod guarda o **ponto de retorno** do jogador (dimensão, posição e para onde olhava) e reserva uma **vaga** na dimensão da arena.
- A arena é montada na vaga, em fatias, ao longo de até 2 segundos. Nesse tempo o jogador fica parado onde está, com a tela escurecendo. Quando a arena fica pronta, ele é levado para lá.
- O jogador aparece no lado sul do pátio, no topo da escadaria, olhando para o norte. O Toji entra 16 blocos à frente dele, com a animação de chegada, o baque e a fala que já existem, e só ataca 2 segundos depois, como hoje.
- Montaria, passageiros e animais de estimação não vão junto. Quem está montado desmonta antes.
- Só entra quem invocou. Não é possível invocar de dentro da arena.

## A dimensão
- Nome: `minerwk:arena`. Vazia (só ar), sem mobs, sem clima, sem ciclo de dia: a hora fica fixa no pôr do sol (12400).
- Dividida em **vagas** numa grade, a 512 blocos uma da outra. Duas lutas ao mesmo tempo ficam em vagas diferentes e não se veem.
- Uma vaga fica ocupada do começo da montagem até o fim da limpeza. A próxima luta pega a primeira vaga livre.
- As vagas não são salvas: quando o servidor liga, todas estão livres. A arena é limpa antes de ser montada, então sobras de uma queda do servidor não aparecem.

## O cenário: entrada da Escola Jujutsu
> **Refeito em 11/11** a pedido do autor, para ficar fiel à obra (caps. 70 a 72, ep. 27): o grupo sobe uma escadaria longa com torii em fila, chega ao topo já dentro da Escola, e a luta acontece ali, entre prédios de templo que o Azul máximo arrasa. A versão anterior (pátio de 61 × 61 com muro e doze árvores, caixa de 81) saiu. Ver [[2026-11-11 Arena da Escola Jujutsu]].

Montado por código, com blocos do Minecraft. Tudo cabe numa caixa de 101 × 76 × 101 blocos por vaga. Norte é para onde o jogador olha ao entrar.

- **Sul, a chegada:** uma escadaria de 11 blocos de largura que desce 14 blocos encosta abaixo, com cinco torii vermelhos em fila e um torii grande na cabeça dela. O jogador entra no topo, de costas para a escadaria.
- **Pátio:** 41 × 41 blocos, plano, de tijolo de pedra com a faixa central e a moldura de andesito polido e 3 camadas de pedra por baixo. Só os dois pilares de um torii ficam dentro dele. Lanternas de pedra na borda.
- **Salão principal (norte):** base de pedra de 2 blocos com escada de frente, paredes brancas com pilares e viga escuros, porta de 5 × 4 para o pátio e dois telhados escuros de beiral largo.
- **Pavilhões (leste e oeste):** o mesmo estilo, menores, com um telhado e a porta para o pátio.
- **Prédios ocos e destrutíveis:** dá para entrar e subir nos telhados, e os golpes os destroem.
- **Floresta:** pinheiros altos em volta de tudo e pela encosta.
- **Sem itens soltos, baús ou mobs.**
- A planta (`core/arena/PlantaEscola`) fica separada do código que põe os blocos no mundo; `python ferramentas/previa_arena.py` desenha a planta sem abrir o jogo.

## Regras dentro da arena
- **Barreira:** uma caixa invisível nas bordas da vaga (101 × 101, de 20 blocos abaixo do pátio até 56 acima). Nem o jogador nem o Toji passam. Usa a mesma regra de movimento do domínio do Gojo, estendida para caixa.
- **Queda no vazio:** se o chão for todo destruído e alguém cair abaixo do fundo da caixa, volta para o centro do pátio (o chão do centro é refeito num quadrado de 5 × 5) e leva 4 de dano. Vale para o jogador e para o Toji.
- **Blocos:** o jogador não quebra nem coloca blocos à mão (em sobrevivência). Os golpes destroem normalmente.
- **Destruição limitada à caixa:** o Roxo, o R e o Azul não apagam nem arrancam blocos fora da caixa da vaga. A explosão de raio 40 do R continua com o mesmo visual e o mesmo dano; só os blocos apagados é que param na borda.
- **Subida do R:** os 25 blocos da subida cabem na caixa (56 acima do pátio).
- **Sai do código:** a regra "afastar 64 blocos faz o Toji sumir" e a regra do ajudante (alvo por 5 segundos, não desperta).
- **Fica como está:** o despertar no meio da luta, as três fases, os golpes, a cutscene e o XP.

## Como a luta acaba
- **Vitória:** o jogador fica 5 segundos na arena e é levado ao ponto de retorno. Os golpes ficam liberados e vêm os 500 de XP, como hoje.
- **Derrota (morte):** o jogador **não perde itens nem XP** ao morrer na arena, e renasce no ponto de renascimento normal dele. Os cadeados voltam, como hoje.
- **Sair do jogo no meio:** conta como derrota. Ao entrar de novo, o jogador aparece no ponto de retorno.
- **Sair da dimensão por comando** (`/tp`, `/execute in`): conta como derrota; ele fica onde o comando o pôs.
- **Queda do servidor:** ao entrar, quem estiver na dimensão da arena sem luta em andamento é levado ao ponto de retorno. Sem ponto salvo, vai para o renascimento normal.
- Em todos os casos o Toji some, a arena é apagada em fatias e a vaga é liberada.
- Se o ponto de retorno estiver obstruído ou a dimensão dele não existir mais, o jogador vai para o renascimento normal.

## Técnica
- **Dimensão:** arquivos de dados do mod (`dimension` e `dimension_type` em `data/minerwk/`), com gerador plano sem camadas e o bioma `the_void`.
- **Lógica pura em `core/arena/`**, com testes JUnit:
  - `VagasArena`: reservar, liberar e a posição de cada vaga;
  - `PlantaEscola`: a lista de blocos do cenário (posição relativa e material);
  - `CaixaArena`: os limites da vaga (dentro ou fora, ponto de entrada do jogador e do Toji).
- **Servidor, em `com.minerwk.arena`:**
  - `ArenasServidor`: monta e apaga em fatias, leva e traz o jogador, e trata saída, morte e entrada no jogo;
  - o ponto de retorno é um Data Attachment novo do jogador, salvo em disco.
- **Mudanças no que existe:**
  - `LutasToji.invocar` passa a pedir a arena e só cria o Toji quando ela está pronta;
  - a barreira em caixa fica em `CaixaArena` (a caixa depende só da posição, então cliente e servidor a aplicam sem pacote novo; o `BarreirasServidor` do domínio não muda);
  - `ApagadorServidor` e o arranque de blocos do Azul respeitam o limite da caixa quando o golpe acontece na dimensão da arena;
  - `TojiEntity` perde a regra dos 64 blocos e a do ajudante.
- **GameTests:** entrar e voltar ao lugar exato; vitória; morte sem perder itens; sair no meio e voltar ao ponto de retorno; duas lutas em vagas diferentes; o R não apaga blocos fora da caixa.

## Partes
0. **Prova:** a dimensão carrega no cliente e no servidor dedicado, e um comando de teste leva o jogador a uma plataforma lisa e o traz de volta ao lugar exato. Se a dimensão por arquivos de dados não servir, paro e mostro as opções.
1. **Entrada e saída:** vagas, ponto de retorno, a luta do Toji acontecendo na plataforma lisa, e todos os fins de luta (vitória, morte, sair, comando, queda do servidor).
2. **Regras:** barreira em caixa, queda no vazio, blocos protegidos, destruição limitada, e a retirada das regras dos 64 blocos e do ajudante.
3. **Cenário:** a planta da entrada da Escola no lugar da plataforma, com a montagem e a limpeza em fatias e a tela escurecendo na ida e na volta.

Cada parte termina com `./gradlew build` passando, diário, Roadmap e Pendências atualizados, e o seu teste no jogo.

## Pontos que eu assumi
- **Não perder itens na derrota.** A arena é apagada no fim, então os itens caídos sumiriam. Se preferir que a morte custe algo, dá para cobrar de outro jeito (o Contrato já é gasto).
- **Tamanho do pátio (61 × 61)** e da caixa (81 × 81 × 64). O Desaparecer e a Execução do Toji usam poucos blocos; o que pede espaço são o R e o Roxo de 40 blocos.
- **Hora fixa no fim de tarde,** para o céu escurecendo do R e o domínio aparecerem bem. Meio-dia ou noite são trocas de um número.
- **Sem música** na arena por enquanto.
- **O Contrato é gasto na entrada,** como hoje, e não é devolvido na derrota.
- **Modo criativo:** quem está em criativo pode quebrar e pôr blocos na arena (útil para testes).
- O `/rwk toji` também leva para a arena. Não fica um jeito de lutar no mundo aberto.

## Fora deste spec
- O balanceamento dos golpes do Gojo e do Toji e o comando de medição de dano (próximo spec).
- Arena de treino, plateia e luta com mais jogadores.
- Outros bosses usando a arena (o sistema de vagas e a caixa ficam prontos para isso, mas cada boss terá o seu cenário).
