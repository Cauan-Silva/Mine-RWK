---
tags: [diario]
---
# 2026-11-09 Gojo mecânica

Voltar para [[Diário]].

- Pesquisa canônica do Gojo com fontes (Fandom lida pelo navegador do app, porque bloqueia leitura automática) e kit aprovado: ver [[Gojo]].
- Decisões: sem transformações; golpes que duram viram **efeitos soltos** (o jogador segue usando outras teclas); Roxo apaga blocos com a gamerule `minerwkApagarBlocos`; domínio paralisa mobs e jogadores (com PvP). O autor pediu cutscene cinemática na ultimate, para a parte visual.
- Núcleo: `Vetor`, `Efeito`, `Passiva` (por jogador, via fábrica), interrupção por dano na carga. Gojo em `core/personagens/gojo` com 12 testes JUnit.
- Servidor: `ContextoServidor` com puxar, empurrar, colisão, apagar blocos, paralisia e Infinito; `ProtecoesServidor` cancela dano de quem está paralisado e o dano físico no Infinito. 5 GameTests novos.
- Achado nos GameTests: o `ServerPlayer` recém-criado fica 60 ticks imune a dano, então testes de dano passavam sem testar nada; agora o contador é zerado no teste.
- Revisão final (modelo barato) sem críticos; corrigidos 4 menores com GameTests (12 no total): dimensão, pets e montaria, Paralisia e ícone. O resto ficou em [[Pendências]].
- O `teste` deixou de aparecer na tela de escolha (só aparece quando é o único personagem).
