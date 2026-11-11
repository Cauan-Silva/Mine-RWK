---
tags: [diario]
---
# 11/11/2026: Ichigo, parte 4 (VFX dos golpes e animações do jogador)

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]]. Branch `ichigo-kit`. Vem depois de [[2026-11-11 Ichigo parte 3]].

## O que foi feito
As partículas do Minecraft que seguravam o lugar saíram do kit do [[Ichigo]]: cada golpe agora tem visual próprio e animação do jogador. O reiatsu do Shikai é azul-claro e soma luz; o do Bankai é tinta preta com a borda vermelha.

- **Getsuga Tenshō (Z):** enquanto a tecla está segurada o reiatsu sobe pelo corpo; no corte sai uma meia-lua em volume (fio afiado na frente, dorso grosso com chamas), com duas pós-imagens atrás. Ela cresce ao sair da lâmina, voa junto com o dano e se parte em lascas ao parar (no alcance ou na parede). A forma vem do Minezin (`core/vfx/GetsugaIchigo`), com o passo e o alcance daqui; o tamanho bate com o raio do dano, e a carga cheia deixa a do Shikai maior. No Bankai ela é preta, de contorno vermelho.
- **Shunpo (X):** riscos de velocidade pelo caminho andado de verdade, três vultos que ficam para trás e poeira na partida e na chegada (pretos e vermelhos no Bankai).
- **Getsuga no chão (C do Shikai):** uma parede de reiatsu abre em volta até o raio do golpe, com rachaduras no chão, pedras e um clarão.
- **Arremesso (V do Shikai):** a Zangetsu gira no ar presa à mão pelo pano, vai até o alcance (ou até o bloco) e volta. No traje ela some do braço enquanto isso (parte 3).
- **Entrada do Bankai (G):** na carga o reiatsu azul é puxado para o corpo e escurece; no estouro sobe um pilar de tinta com veios vermelhos, um anel de poeira corre pelo chão e ficam chamas negras em volta. O pilar esconde a troca do traje.
- **Getsuga na lâmina (C do Bankai):** chamas de tinta e vermelho ao longo da espada e fiapos pelo corpo, pelos 6 s.
- **Sair do Bankai** (por vontade ou sem reiatsu): lascas de tinta sobem do corpo e o azul volta num brilho.
- **Animações** (`ferramentas/gerar_animacoes_ichigo.py` → `player_animations/ichigo.json`): nove, só braços, tronco e cabeça (as pernas seguem a passada). Carga e corte do Getsuga, corte seco do Bankai, mergulho do Shunpo, cravar a espada, girar e lançar, a pose da liberação, baixar a espada e erguer a lâmina. Golpe interrompido para a animação e apaga a carga.
- **No código:** as formas e os tempos em `core/vfx/GetsugaIchigo` e `core/vfx/FormasIchigo` (o kit usa os mesmos números de passo, alcance e raio); o desenho em `client/vfx/RenderVisuaisIchigo`, em cor chapada com luz por vértice, sem textura e sem shader novo. Os visuais chegam pelo mesmo `MostrarVisual` do Gojo, com o tipo começando em `ichigo_`.
- **Prévia:** `./gradlew runClientDemo -Pdemo=ichigo` (câmera fixa de lado: os quatro golpes do Shikai, a entrada do Bankai de frente, os golpes do Bankai e a saída). Vídeo em `previas/ichigo.mp4`.

## Ficou para depois
- **Som:** parte 5. Os golpes ainda são mudos.
- **O clique da espada** (ataque básico) não ganhou rastro de corte, nem o alcance maior do Getsuga na lâmina tem visual próprio além das chamas.
- **O rasgo no chão** por onde o Getsuga passa (existe no Minezin) não veio.
- **Em primeira pessoa** as chamas do Getsuga na lâmina não aparecem na espada da tela, só os fiapos em volta do corpo.
- **Onda, pilar e saída** nascem a 1,62 abaixo dos olhos: agachado, ficam um pouco enterrados.
- As pós-imagens do Getsuga negro ficam acinzentadas sobre a grama clara; dá para escurecer se incomodar.

## Revisão (subagente)
- **Corrigido:** a meia-lua que batia num bloco era desenhada um passo dentro da parede, e as lascas nasciam escondidas. O cliente agora usa o ponto que o servidor manda.
- **Corrigido:** com dois Getsugas do mesmo jogador no ar, o aviso de parada encerrava o mais novo; agora encerra o que saiu primeiro.
- **Corrigido:** quem morria ou trocava de personagem com o Getsuga na lâmina deixava as chamas por até 6 s; agora elas só aparecem em quem está no Bankai.
- Conferidos sem problema: o estado de render (todo lote abre e fecha, e um erro no meio não vaza), os índices das malhas, a sincronia do arremesso e da onda com o servidor, as cargas (somem no disparo e em toda interrupção), as nove animações no arquivo e a regra de `core/` sem Minecraft.

## Testes
- JUnit: `FormasIchigoTest` (10 casos: a meia-lua anda com o golpe, para no ponto do servidor, tem o tamanho do dano, é grossa no meio; o arremesso vai, para e volta; a onda chega ao raio; o pilar sobe e some) e seis casos novos em `IchigoTest` (os visuais e as animações de cada golpe, a carga interrompida, e toda animação pedida existe no arquivo).
- `./gradlew build` passa e os 69 GameTests passam. O desenho só dá para conferir no jogo: a prévia mostra todos os visuais.

## Falta
O teste do autor no jogo: ver [[Pendências]]. Próximo: parte 5 (som).
