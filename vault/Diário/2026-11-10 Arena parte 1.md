---
tags: [diario]
---
# 10/11/2026: Arena do Toji, parte 1 (entrada e saída)

Voltar para [[Diário]]. Spec e plano: [[2026-11-10 Arena do Toji]]. Branch `arena-toji`. Antes: [[2026-11-10 Arena parte 0]].

## O que foi feito
- **A luta do [[Toji]] acontece na arena.** O Contrato e o `/rwk toji` reservam uma vaga, montam a arena em fatias (por enquanto uma plataforma lisa de 61 × 61, com 4 camadas) e levam o jogador para a entrada sul. O Toji nasce 16 blocos ao norte, de frente para ele, com a entrada de sempre.
- **Montagem e limpeza em fatias (`arena/ObrasArena`):** no máximo 4000 blocos mexidos e 20000 posições olhadas por tick. A caixa é limpa antes de montar e depois da luta; os chunks da vaga ficam forçados nesse meio tempo.
- **Ciclo (`arena/ArenasServidor`):** montando → lutando → voltando. Na vitória o jogador volta 5 s depois; nos outros fins, no tick seguinte. Depois a arena é apagada e a vaga, liberada.
- **Fins de luta tratados:** vitória; morte (renasce no lugar normal); sair do jogo no meio (ao entrar de novo, volta ao ponto de retorno); sair da dimensão por comando (fica onde o comando o pôs); sair durante a montagem (a entrada é desfeita e a vaga liberada); entrar no jogo dentro da dimensão sem luta (volta ao ponto de retorno, ou ao renascimento se o ponto não vale mais).
- **Morrer na arena não perde itens nem XP (`arena/RegrasArena`):** o inventário e o XP são guardados na morte e devolvidos ao renascer; nada cai na arena.
- **Sai:** o comando de prova `/rwk arena`. O Toji não nasce mais a 8 blocos no mundo aberto.

## Decisões
- **O fim da luta nunca age na hora, só no tick seguinte.** O Toji avisa de dentro da própria remoção (inclusive quando o chunk descarrega ou o servidor fecha); teleportar e apagar entidades ali deu erro ao fechar o servidor de testes.
- **Durante a montagem o jogador recebe Lentidão e Resistência** por até 4 s (acabam sozinhos), em vez de ficar marcado como invulnerável, o que ficaria salvo se o servidor caísse.
- **Nova resposta `NA_ARENA`** ("Você já está indo para a arena ou dentro dela") para quem tenta invocar com a arena reservada ou de dentro dela.
- **A prova automática continua** como ferramenta de desenvolvimento (`arena/ProvaArena`, só com `-Pprova_arena`): invoca o Toji, mata-o e confere a volta. Não faz nada num jogo normal.

## Testes
- JUnit: `PlantaEscolaTest` (2) e mais um caso em `VagasArenaTest`.
- GameTest: 10 novos em `TestesArena` (montar e apagar; invocar leva para a arena com o Toji; vitória volta em 5 s; Toji some e volta na hora; sair na montagem libera a vaga; duas lutas em vagas diferentes; entrar no jogo na arena sem luta; morrer na arena mantém itens e XP; fora dela derruba; morrer depois da vitória não teleporta). `contratoExigeONoDespertar` foi adaptado. Os 51 passam.
- O `vermelhoCongelaEArremessaZumbi` falhou em 2 de 6 rodadas, como já estava anotado: é instável e não tem a ver com a arena.
- **Prova num mundo real** (`runClient -Pmundo="New World" -Pprova_arena`): invocar = OK; 10 s depois o jogador estava na vaga 0 com o Toji a 2 blocos (a luta estava acontecendo); 3 s depois da vitória ainda na arena; 7 s depois, de volta ao lugar exato (dimensão, posição e olhar). Sem erros no log.

## Falta
- O teste do autor no jogo (roteiro em [[Pendências]]): a prova automática não vê a tela, não morre e não sai do jogo.
- Parte 2: barreira em caixa, queda no vazio, blocos protegidos, destruição só dentro da caixa, e o fim da regra dos 64 blocos e do ajudante. **Até lá dá para cair da plataforma e o Toji some a 64 blocos.**
