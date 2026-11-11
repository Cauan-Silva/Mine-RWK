---
tags: [diario]
---
# 11/11/2026: Ichigo, parte 2 (kit do Shikai e do Bankai)

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]]. Branch `ichigo-kit`. Vem depois de [[2026-11-11 Ichigo parte 1]].

## O que foi feito
O [[Ichigo]] passa a existir no jogo (`/rwk trocar <jogador> ichigo` ou a tela de escolha), com os golpes das duas formas. A apresentação é provisória: partículas do Minecraft, sem modelo de espada, traje, animação nem som (partes 3, 4 e 5). Na tela de escolha ele aparece com o emblema genérico.

**Shikai** (`core/personagens/ichigo/Ichigo`):
- **Z Getsuga Tenshō:** segura até 10 ticks; a meia-lua anda 24 blocos a 1,6 por tick, atravessa os inimigos ferindo cada um uma vez (8 a 12, pela carga) e para em bloco sólido. Recarga 4 s, custo 20.
- **X Shunpo:** até 8 blocos em linha reta na mira, parando antes de parede. Recarga 7 s, custo 15.
- **C Getsuga no chão:** 8 ticks de gesto; raio 5, 6 de dano, empurra e desfaz os projéteis dos outros. Recarga 12 s, custo 30.
- **V Arremesso pelo pano:** 6 ticks de giro; a espada vai até 10 blocos em 8 ticks (10 de dano, uma vez por inimigo) e volta em outros 8. Nesses 16 ticks não há outro golpe e o clique é o soco de mão vazia. Recarga 10 s, custo 20.
- **G Bankai:** 30 ticks de entrada, empurra quem está a até 6 blocos sem ferir e troca a forma. Custo 50.

**Bankai:**
- **Z Getsuga negro:** sai em 2 ticks, 2,4 blocos por tick, 28 blocos, 10 de dano. Recarga 1,5 s, custo 15.
- **X Shunpo do Bankai:** até 14 blocos; com um inimigo na mira (até 14 blocos, 1,5 de folga) surge 1,5 bloco atrás dele, já olhando para ele. Recarga 2 s, custo 10.
- **C Getsuga na lâmina:** por 6 s o ataque básico alcança 5 blocos e ganha +6 de dano. Acaba antes se sair do Bankai. Recarga 14 s, custo 30.
- **G Selar o Bankai:** volta ao Shikai; o Bankai fica 5 s em recarga.
- **Reiatsu em zero:** o Bankai acaba sozinho e o G fica 10 s em recarga. Quem faz isso é a passiva `Zangetsu`.

**Ataque básico da Zangetsu** (o clique): 7 de dano no Shikai (1,4 ataque por segundo) e 6 no Bankai (2,2 por segundo). Vale no lugar do item que o jogador segura, como diz o spec.
- No núcleo é o registro `core/kit/Arma` (dano, velocidade, alcance). O kit pede a arma todo tick (`Contexto.arma`); o último pedido do tick ganha, e é assim que o arremesso tira a espada e o Getsuga na lâmina a fortalece.
- No servidor (`kit/ArmaServidor`): o dano do clique é trocado pelo da arma, mantendo a proporção da recarga do ataque (golpe apressado é fraco) e do crítico; velocidade e alcance viram modificadores de atributo, descontando o que o item na mão soma. Sem pedido no tick (outro personagem), tudo volta ao normal. O dano dos golpes do kit não passa por aqui.

**Ações novas do `Contexto`** (todas com corpo vazio no núcleo e feitas no `ContextoServidor`): `recurso()`, `recarga(id, ticks)`, `arma(arma)`, `deslocar(direcao, distancia, encarar)`, `colideBloco(de, ate)` e `desfazerProjeteis(centro, raio)`.
- O `deslocar` anda em passos de meio bloco e para no primeiro que não cabe. Quem olha um pouco para baixo desliza rente ao chão, e um degrau de 1 bloco é subido.

## Decisões desta parte
- **Sem animação por enquanto.** Os golpes não chamam `apresentacao` com animações que ainda não existem; entram na parte 4.
- **O Bankai não tem recarga própria ao entrar:** a recarga do G é posta na saída (5 s por vontade, 10 s por falta de reiatsu).
- **Morrer no Bankai:** renasce no Shikai com o reiatsu que tinha, como o Gojo com a energia. No Shikai ele volta sozinho, então não foi criada regra nova.
- **Bankai acabando no meio de um golpe:** o que está em carga é interrompido sem punição; o que já disparou termina.

## Testes
- JUnit: `IchigoTest`, 17 casos (teclas de cada forma; Getsuga com carga cheia e curta, atravessando e parando na parede; Shunpo das duas formas, com e sem alvo; Getsuga no chão; arremesso e os 16 ticks sem espada; arma de cada forma; entrada, saída e fim do Bankai por reiatsu; Getsuga na lâmina e o fim dele ao sair).
- GameTests novos em `kit/TestesIchigo` (6): entra no Bankai, a arma e a velocidade de ataque trocam, e sem reiatsu ele acaba; o Getsuga atravessa dois zumbis; o clique tira 7 (e a metade se apressado; o soco do Gojo não muda); o Getsuga no chão desfaz a flecha do zumbi e não a do Ichigo; o Shunpo para antes da parede; o Shunpo do Bankai surge atrás do zumbi olhando para ele. Os 69 GameTests passam.
- `./gradlew build` passa.

## Revisão (subagente)
Nenhum achado crítico. Conferidos sem problema: os números contra o spec, a conta dos modificadores de atributo (não oscila nem acumula), o que o evento do clique pega, a rotação do Shunpo e a ordem dos pedidos de arma no tick.

Corrigido:
- **O Getsuga nascia do outro lado de uma parede colada no jogador** (ele sai 1 bloco à frente dos olhos). Agora, com parede nesse bloco, sai dos próprios olhos e para nela. Com teste.
- **O Shunpo sem alvo reescrevia a rotação do jogador**, o que podia dar um tranco na câmera de quem estivesse girando. Agora só a posição muda.

Ficou como está, anotado:
- O Shunpo do Bankai com alvo anda até 15,5 blocos (os 14 da mira mais 1,5 para trás do alvo). Com alvo muito baixo ou muito alto, o Ichigo chega um pouco acima ou abaixo do chão dele.
- Encantamento de dano do item na mão ainda entra na proporção do clique (no máximo 1,5 vez); Força não soma.
- O alcance de 5 do Getsuga na lâmina vale também para interagir com entidades.
- Entrar no Bankai com pouco reiatsu dá um Bankai curto e 10 s de recarga: é a regra.
- Nenhum GameTest cobre o teleporte com conexão de verdade (o jogador dos testes não tem): só o teste no jogo confere.

## Falta
O teste do autor no jogo: ver [[Pendências]]. Próximo: parte 3 (arma na mão e traje das duas formas).
