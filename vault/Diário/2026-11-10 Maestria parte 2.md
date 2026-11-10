---
tags: [diario]
---
# 10/11/2026: maestria, parte 2 (a árvore por dentro)

Voltar para [[Diário]]. Spec: [[2026-11-10 Maestria e árvore de habilidades]]. Branch `maestria-arvore`. Parte anterior: [[2026-11-10 Maestria parte 1]].

## O que foi feito
- **Regras (`core/maestria/`):** `No` (níveis, custo 1, 2 e 3, nível mínimo, se é de depois do despertar), `Arvore` (compra com o motivo da recusa, pontos gastos e livres) e `Melhorias` (os nós comprados vistos como fatores).
- **Árvore do Gojo (`ArvoreGojo`):** os 14 nós do spec, 83 pontos no total. O `Personagem` passou a carregar a árvore dele.
- **Compra no servidor:** `ServicoKits.comprarNo` confere personagem, nó, nível máximo, despertar, maestria e pontos. O comando `/rwk maestria <jogador> comprar <nó>` usa a mesma regra; `zerar` devolve os pontos.
- **Efeito nos golpes:**
  - Recarga e carga são ajustadas no kit, num ponto só (`KitDoJogador.numeros`).
  - Os outros nós são lidos pelo golpe no contexto (`ctx.melhorias()`): alcance do Azul, duração do Infinito e do domínio, dano do Vermelho e do Roxo, intervalo da Técnica Reversa.
  - A reserva e o fluxo de energia são lidos pelo próprio recurso.

## Testes
JUnit: `ArvoreTest` e casos novos em `KitDoJogadorTest` e `GojoTest`. GameTest novo: `compraDeNoValeNoServidorEChegaNoKit`. Os 23 GameTests passam.

## O que ficou de fora ou diferente
- **Visual:** o alcance maior do Azul e a carga mais curta do Vermelho mudam a mecânica, mas o efeito visual e a animação continuam do tamanho original.
- **Reserva de energia:** comprar o nó sobe o máximo sem encher a barra.
- **Pacote de compra do cliente e os nós no pacote de sincronização:** ficam para a parte 3, junto com a tela.

## Falta
A parte 3: a tela da árvore (tecla H), o nó Despertar e o estado "despertou".
