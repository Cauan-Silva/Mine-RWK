---
tags: [diario]
---
# 10/11/2026: maestria, parte 3 (a tela da árvore)

Voltar para [[Diário]]. Spec: [[2026-11-10 Maestria e árvore de habilidades]]. Branch `maestria-arvore`. Partes anteriores: [[2026-11-10 Maestria parte 1]] e [[2026-11-10 Maestria parte 2]].

## O que foi feito
- **Tela (`client/kit/TelaArvore`):** a tecla **H** abre e fecha. Em cima os nós de antes do despertar, o Despertar no meio e os de depois embaixo. Cada nó mostra o nome, o nível (`2/3`) e o custo ou o motivo de estar trancado; passar o mouse mostra o efeito de agora e o do próximo nível. No topo, o personagem, a maestria, os pontos livres e a barra de XP.
- **Posições (`core/hud/LayoutArvore`):** até quatro colunas conforme a largura, linhas centradas e rolagem quando não cabe. A árvore do Gojo cabe na janela padrão sem rolar.
- **Compra pelo cliente:** clicar manda o pacote `ComprarNo` com só o id; o servidor confere tudo. A tela só mostra o estado que o servidor mandou.
- **Sincronização:** o pacote do kit passou a levar os nós comprados e o "despertou".
- **Despertou:** `ServicoKits.definirDespertou` e o comando `/rwk despertar <jogador> <sim|nao>`. Comprar o nó Despertar mostra "Pronto para o desafio"; a luta que usa isso é do spec do [[Toji]].
- **Traduções:** nome e descrição dos 14 nós em português e inglês, com um teste que confere se nenhuma falta.

## Prévia
`./gradlew runClientDemo -Pdemo=arvore` grava a tela em três momentos; os quadros ficam em `previas/arvore-*.jpg`. A demo zera a maestria do Gojo no mundo de testes ao terminar.

## O que ficou diferente do spec
- Os golpes continuam todos liberados, como o spec previa: o trancamento é do spec do Toji.
- O motivo do nó trancado diz "Exige o despertar", sem citar o Toji, para servir a outros personagens.

## Revisão
Revisão das três partes por um subagente: nada crítico nem importante. Corrigido: baixar o nível por comando abaixo dos pontos já gastos agora devolve os nós. Ficaram anotados em [[Pendências]]:
- `zerar` não desfaz o "despertou" (de propósito: há o comando `despertar`).
- O Roxo e o R leem o tempo de carga sem as melhorias; só importa se um dia ganharem nó de carga.
- Comprar um nó no meio da carga de um golpe aplica a recarga nova com o tempo de carga antigo.
