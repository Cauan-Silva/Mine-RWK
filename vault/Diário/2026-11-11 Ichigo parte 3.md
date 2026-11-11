---
tags: [diario]
---
# 11/11/2026: Ichigo, parte 3 (arma na mão e traje das duas formas, com pano)

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]]. Branch `ichigo-kit`. Vem depois de [[2026-11-11 Ichigo parte 2]].

## O que foi feito
Quem escolhe o [[Ichigo]] aparece de shihakushō com a Zangetsu na mão; no Bankai, de casaco longo com a Tensa Zangetsu. Nada disso é item: é desenhado por cima da skin, como o traje do Gojo. O autor pediu os trajes com pano animado, e o tecido do Minezin foi trazido para isso.

- **Modelos** (`ferramentas/gerar_traje_ichigo.py`): partem dos modelos do Minezin (mesmo autor). A espada entra no modelo do traje, presa ao braço direito (osso `arma`), então segue o braço em terceira e em primeira pessoa. No Minezin as faces da espada apontavam para um atlas grande de cores; aqui cada cor é pintada num canto livre da textura do traje, e o atlas não vem. A espada fica com 80% do tamanho do modelo original, com a ponta um pouco erguida e a lâmina meio de lado, para a face larga da Zangetsu aparecer em primeira pessoa.
- **Que traje vale** (`core/traje/Trajes`): por personagem e forma. `core/traje/TrajeIchigo` guarda os nomes dos trajes e dos ossos.
- **Quem vê o jogador sabe a forma dele:** o aviso `PersonagemDe` passou a levar a forma e se a espada está fora da mão. O servidor avisa o jogador e quem o vê quando isso muda (e de novo quando ele renasce). Era o que tinha ficado da parte 1.
- **Espada fora da mão:** no Arremesso pelo pano, a espada some do braço até voltar.
- **Pano** (`core/traje/tecido`, vindo do Minezin):
  - As costas do casaco do Bankai são uma malha de partículas presa na cintura (`MalhaTecido`); os lados, as quinas e as tiras da frente seguem as colunas de fora dela, então a saia do casaco é um pano só.
  - As mangas (do Bankai, em três segmentos, e do Shikai, uma peça de cada lado, acrescentada aqui) andam em molas amortecidas (`MolaManto`) e seguem o balanço dos braços.
  - A faixa da Zangetsu e a corrente da Tensa Zangetsu balançam com a mira, o braço e os golpes (`TecidoEspada`), e nunca ficam paradas de todo.
  - Ao entrar no Bankai o casaco abre de uma vez e assenta. O Shunpo é um teleporte: o pano recomeça em vez de esticar pelo caminho.
  - No cliente, `client/traje/TecidoCliente` roda um tick por tick do jogo para cada jogador de traje do Ichigo, e `CamadaTraje` gira os ossos de pano a partir do repouso do modelo.
- **`CamadaTraje`** passou a ter um renderer e um item escondido por traje (antes só existia o do Gojo). O traje do Gojo não mudou.
- **Prévia:** `./gradlew runClientDemo -Pdemo=ichigo_traje` (Shikai de frente com um Getsuga, corrida de costas, primeira pessoa com um ataque; Bankai: a entrada, corrida de costas, um giro e primeira pessoa). Vídeo em `previas/ichigo_traje.mp4`.

## Ficou para depois
- **A saia do casaco como uma folha lisa.** No Minezin os painéis do casaco são costurados numa superfície só, empurrada para fora das pernas (`SuperficieTecido`). Aqui eles são desenhados como blocos que giram; na prévia fica bom de longe, mas de perto pode aparecer fresta ou a perna atravessar. Entra se o autor achar que precisa.
- **O hakama (as pernas largas do Shikai)** não tem ossos de pano no modelo: só as mangas e a faixa da espada se mexem nessa forma.
- **Em primeira pessoa** a espada só aparece com a mão principal vazia (é quando o jogo desenha o braço). Com um item na mão, aparece o item.
- **A pose da espada** é a do braço do Minecraft; as animações de golpe entram na parte 4.
- **Emblema na tela de escolha:** ainda o genérico.
- O traje do Mugetsu e a máscara: parte 6.

## Revisão (subagente)
Um bug real, corrigido: a espada ficava escondida para sempre depois de um arremesso, e para todos os jogadores com o mesmo traje, porque o osso só era escondido e nunca mostrado de volta. Conferidos sem problema: o aviso da aparência em todos os casos (entrar, renascer, mudar de dimensão, trocar de personagem), a ordem dos campos do pacote, o traje do Gojo igual a antes, o tick do pano e a regra de `core/` sem Minecraft.

## Testes
- JUnit: `TrajeIchigoTest` (5 casos: o traje de cada forma; a espada nos dois modelos, presa ao braço direito; os ossos da faixa e da corrente; as mangas do Shikai se mexem; a faixa nunca para e não espalha valor inválido), mais os testes do tecido vindos do Minezin (`MalhaTecidoTest`, `TecidoIchigoTest`, 8 casos).
- `./gradlew build` passa e os 69 GameTests passam. A parte de desenho só dá para conferir no jogo: a prévia mostra traje, espada e pano funcionando nas duas formas.

## Falta
O teste do autor no jogo: ver [[Pendências]]. Próximo: parte 4 (VFX e animações), que troca as partículas provisórias do Minecraft pelos efeitos do mod.
