---
tags: [diario]
---
# 12/11/2026: Ichigo parte 7

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]]. Branch `ichigo-kit`. Vem depois de [[2026-11-12 Mugetsu fino e longo]].

## O que foi feito
A parte 7 do spec, em três pedaços.

### Árvore e Despertar
- `core/personagens/ichigo/ArvoreIchigo`: 13 nós. Antes do despertar: força e recarga do Getsuga, recarga do Shunpo, reserva e fluxo do reiatsu, fôlego do Bankai e o nó **Despertar** (5 pontos, maestria 20). Depois: força do Getsuga negro, duração da lâmina carregada e da máscara, recarga da máscara e do Mugetsu, e a volta mais rápida dos poderes.
- O `Ichigo` e o `Reiatsu` leem as melhorias; textos em pt e en.

### Boss Zangetsu interior
- `core/boss/ZangetsuBoss` (regras puras): 260 de vida, seis golpes no `MotorBoss` do Toji (corte, Getsuga, Getsuga em três, Shunpo, arremesso, Cero), fase 2 abaixo de 40% (máscara, ritmo 0,65, passo maior), danos, alcances e tempos.
- `boss/ZangetsuEntity` (GeckoLib): a luta. Getsuga branco em meias-luas que voam e param em parede; Shunpo que surge atrás com a sombra de aviso; a espada arremessada (ele fica sem ela até voltar); Cero com carga e raio. Os visuais são os do Ichigo em branco (`RenderVisuaisIchigo` ganhou a versão branca, a carga e o raio do Cero).
- **A queda:** o que mataria o jogador durante a luta o derruba (uma vez); `core/boss/CutsceneMascara` mostra a máscara se formando e ele se levanta de vida cheia, em Bankai, de máscara, com os golpes trancados liberados só nesta luta. Vencer libera de vez e dá 500 de XP; perder, sair ou deixar o tempo acabar tranca de novo.
- `boss/MascaraRachada`: o item (receita sem forma: osso, pérola do Ender, espada de ferro), que exige ser Ichigo com o nó Despertar. `/rwk zangetsu <jogador>` pula os requisitos.
- Modelo, textura (o traje do Bankai com claro e escuro trocados), máscara, animações e o ícone do item saem de `ferramentas/gerar_boss_zangetsu.py`; nenhum asset de terceiros.

### Mundo interior
- `core/arena/PlantaMundoInterior`: o chão é a fachada de um prédio deitado (concreto claro e janelas de vidro azul), com três prédios em volta, um deles atravessado no alto. Uns 27 mil blocos, na dimensão das arenas; `ArenasServidor.entrar` passou a aceitar o cenário de quem chama.

## Decisões minhas (mudam com uma palavra do autor)
- O **corte de perto**, que o spec não lista: sem ele bastava ficar colado.
- A queda acontece **no golpe que mataria**, não por tempo ou vida do boss.
- Os 13 nós e os números de cada um.
- O céu é o **entardecer** da dimensão das arenas (o spec pede azul; pede dimensão própria).
- Ele espera **2 s** depois da chegada antes da entrada, para o jogador terminar de carregar o cenário.
- O Cero usa os sons da carga do Bankai e da onda; som próprio fica para a parte 8.
- O `ZangetsuEntity` repete ajudas do `TojiEntity` em vez de eu refatorar o Toji agora.

## Testes
- JUnit: `ArvoreIchigo` no `IchigoTest` e `ReiatsuTest`, `ZangetsuBossTest`, `PlantaMundoInteriorTest`. `./gradlew build` passa.
- GameTests: 12 novos em `TestesZangetsu` (entrada na arena, espera da entrada, cada golpe, a queda e a máscara, qualquer dano mortal vira queda, fase 2, vitória). **Os 85 passam.**
- Os testes acharam um erro de verdade: a meia-lua que derrubava o jogador limpava a lista de meias-luas no meio do laço e derrubava o servidor. Corrigido.
- Prévia: `./gradlew runClientDemo -Pdemo=zangetsu`, conferida por quadros (arena, modelo, Getsuga, Cero, cutscene da máscara, fase 2, vitória e volta). Vídeo em `previas/ichigo_parte7_zangetsu.mp4`. A entrada dele não aparece na prévia: a tela de carregamento a cobriu.

## Revisão (subagente)
Nenhum achado crítico. Corrigido:
- Os golpes dele zeram o tempo de invulnerabilidade do alvo (como o Toji), senão um dos três Getsugas podia ser engolido.
- Cair no vão entre os prédios fere sem ser golpe dele e podia matar antes da máscara: agora qualquer dano mortal na luta vira a queda. O conserto do centro da arena só repõe o que foi destruído, sem trocar a fachada por tijolo.
- A Máscara Rachada volta ao inventário se o Zangetsu não conseguir entrar.
- A paralisia não pega nele.

Ficou como está: a queda compara o dano bruto, antes da armadura (igual ao Toji); matar o boss por comando conta como vitória; quem perde de máscara fica com ela no rosto até acabar (em [[Pendências]]).

## Falta
O autor ver a prévia e testar no jogo: ver [[Pendências]]. Próximo no Ichigo: a parte 8, a forma Hollow completa.
