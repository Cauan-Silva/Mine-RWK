---
tags: [desenvolvimento, spec]
estado: aguardando revisão
---
# Spec: maestria e árvore de habilidades

Voltar para [[Roadmap]]. Adianta o item 1 da Fase 4 ("RPG e Despertar"), porque o boss [[Toji]] depende dele. Nada foi programado: é para o autor aprovar ou mudar.

## Escolhas do autor (10/11)
1. **Ordem:** a maestria e a árvore vêm primeiro, num spec só delas. O despertar e o Toji ficam no spec seguinte.
2. **O que libera o Toji:** nível de maestria mais o nó **Despertar**, comprado na árvore.
3. **O que a árvore compra:** melhorias e passivas. Os golpes não são comprados: o Gojo começa com Azul e Infinito e ganha o resto no despertar.
4. **Nível:** só dá pontos para a árvore, sem atributos automáticos.
5. **Luta do Toji (para o próximo spec):** o despertar acontece no meio da luta, como na obra, e libera o kit inteiro, com o G e o R. O item de invocação sai de uma receita de bancada.

## Maestria
Cada jogador tem uma maestria **por personagem**. Trocar de personagem não apaga a do anterior.

### Como se ganha XP
| Fonte | XP |
| --- | --- |
| Dano causado com um golpe do kit num inimigo | 1 por ponto de dano, no máximo 20 por golpe |
| Abater um inimigo com o kit | 25 |

- **Inimigo** é mob hostil ou boss. Animais, aldeões e outros jogadores não dão XP nesta versão (o PvP entra na Fase 4).
- Usar um golpe no ar não dá XP, para ninguém subir de nível parado.
- Morrer não tira XP nem nível.

### Níveis
- Nível 1 a 100.
- XP para passar do nível `n` para o seguinte: `100 + 10 × (n − 1)`.
- Cada nível ganho dá **1 ponto** de árvore. No nível 100 são 99 pontos.

| Marco | XP somado | Estimativa de jogo |
| --- | --- | --- |
| Nível 10 | 1.260 | uns 30 inimigos |
| Nível 20 (requisito do Despertar) | 3.610 | uns 80 inimigos, cerca de 2 horas |
| Nível 50 | 16.660 | uns 370 inimigos |
| Nível 100 | 58.410 | uns 1.300 inimigos |

A estimativa supõe cerca de 45 XP por inimigo abatido com golpes. Os números ficam num lugar só, para ajustar depois do teste no jogo.

### O que o jogador vê
- **HUD:** o nível ao lado do nome do recurso e uma barra fina de XP embaixo dele.
- **Subir de nível:** som, partículas e a mensagem "Maestria 12: +1 ponto".

## Árvore
A tecla **H** abre a árvore do personagem atual. Cada nó tem até 3 níveis, que custam **1, 2 e 3 pontos** (6 no total). O servidor confere tudo antes de descontar: personagem, pontos, pré-requisito e nível de maestria.

### Árvore do Gojo
**Antes do despertar** (disponível desde o começo):

| Nó | Efeito por nível |
| --- | --- |
| Azul: alcance | +10% no raio de atração |
| Azul: recarga | −8% na recarga |
| Infinito: duração | +15% na duração |
| Infinito: recarga | −8% na recarga |
| Energia: reserva | +10% no máximo de energia |
| Energia: fluxo | +10% na regeneração |
| **Despertar** | nível único, 5 pontos, exige maestria 20. Libera o desafio do Toji |

**Depois do despertar** (aparecem com cadeado até vencer o Toji):

| Nó | Efeito por nível |
| --- | --- |
| Vermelho: força | +8% no dano |
| Vermelho: carga | −10% no tempo de carga |
| Roxo: força | +8% no dano |
| Roxo: recarga | −8% na recarga |
| Técnica Reversa | −10% no intervalo entre as curas |
| Vazio Infinito: duração | +10% na duração do domínio |
| Vazio Roxo máximo: recarga | −8% na recarga |

A árvore inteira custa 83 pontos, então no nível 100 sobram 16. No nível 20 o jogador tem 19 pontos: 5 vão para o Despertar e 14 para as melhorias, o que já obriga a escolher.

### A tela
- Dois blocos, um acima do outro: os nós de antes do despertar e, embaixo, os de depois, com o Despertar no meio ligando os dois.
- Cada nó mostra o nome, o nível (`2/3`) e o custo do próximo. Passar o mouse mostra o efeito atual e o próximo.
- Nó sem pontos ou sem requisito fica apagado, com o motivo no texto ("Maestria 20", "Vença o Toji").
- No topo: personagem, nível de maestria, barra de XP e pontos livres.

## O nó Despertar e o estado "despertou"
- Comprar o nó marca o jogador como **pronto para o desafio**. Neste spec isso ainda não faz mais nada: o item de invocação e a luta são do spec do Toji.
- O jogador também ganha o estado **despertou** (sim ou não, por personagem), que abre os nós de depois do despertar. Enquanto o Toji não existir, ele só muda por comando.
- **Os golpes continuam todos liberados por enquanto.** Trancar o Vermelho, o Roxo, a Técnica Reversa, o G e o R até a vitória é do spec do Toji, para o jogo não ficar com golpes trancados sem ter como abrir.

## Comandos de teste (permissão 2)
- `/rwk maestria <jogador> nivel <n>` e `/rwk maestria <jogador> xp <quantia>`.
- `/rwk maestria <jogador> zerar`: devolve todos os pontos e zera os nós.
- `/rwk despertar <jogador> <sim|nao>`.

## Técnica
- **Regras em `core/maestria/`, sem classes do Minecraft, com JUnit:** a curva de XP, o ganho por dano e por abate, a definição dos nós (custo, níveis, pré-requisito, nível mínimo) e a compra.
- **Dados salvos:** o `DadosKit` ganha, por personagem, o XP total, os níveis dos nós e o "despertou". Quem já tem mundo salvo começa do zero, sem perder o personagem.
- **Como a melhoria chega no golpe:** a habilidade pergunta ao contexto o nível de um nó pelo id (`gojo_azul_alcance`) e aplica o efeito. Recarga e custo são ajustados no kit, num ponto só, para todos os golpes.
- **Rede:** o estado da maestria vai no pacote de sincronização que já existe; a compra de um nó é um pacote novo do cliente, com só o id do nó.
- **Outros personagens:** a árvore é declarada por personagem, do mesmo jeito que as habilidades. O guia [[Como portar um personagem]] ganha esse passo.

## Partes
1. **Maestria:** regras, ganho de XP, dados salvos, HUD e comandos.
2. **Árvore por dentro:** nós do Gojo, compra validada no servidor e o efeito de cada melhoria nos golpes.
3. **Tela da árvore:** a tecla H, o nó Despertar e o estado "despertou".

Cada parte termina com build, testes e uma prévia; a revisão por subagente fica para o fim.

## Pontos que eu assumi
Mude o que quiser antes de aprovar:
- **Despertar no nível 20** de 100, com cerca de 2 horas de jogo. No Minezin era no 50.
- **XP só por dano e abate** em mobs hostis, sem XP por usar o golpe no ar e sem XP em jogadores.
- **Custo 1, 2 e 3** por nível de nó, e o Despertar por 5 pontos.
- **Sem redistribuir pontos** no survival por enquanto; só pelo comando de zerar.
- **Sem mana nova:** o recurso continua sendo a energia de cada personagem.
- **Os números das melhorias** (8%, 10%, 15%) são um ponto de partida para o teste no jogo.

## Fora deste spec
- O boss Toji, o item de invocação, a luta com o despertar no meio e o trancamento dos golpes.
- Rank, títulos, atributos gerais (vida, dano) e árvores de outros personagens.
