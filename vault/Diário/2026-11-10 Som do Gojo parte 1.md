---
tags: [diario]
---
# 10/11/2026: som do Gojo, parte 1

Voltar para [[Diário]]. Spec: [[2026-11-10 Som do Gojo]] (aprovado em 10/11, com síntese própria, sem voz e uppercut quase mudo no impact frame). Branch `gojo-som`.

## O que foi feito
- **Motor:** `ferramentas/sfx_motor.py`, portado do Minezin (mesmo autor). Sintetiza cada som em camadas, sem áudio de terceiros, e grava três variações por som.
- **Gerador:** `ferramentas/gerar_sfx_gojo.py` escreve os `.ogg` em `assets/minerwk/sounds/gojo/`, as entradas do `sounds.json`, as legendas em português e inglês e o manifesto `ferramentas/sfx_gojo_manifesto.json`.
- **Teste de ticks:** o `SonsGojoTest` confere os marcos do manifesto contra o kit (fim do Azul, carga cheia do Vermelho, disparo do Roxo, abertura do domínio). Isso fecha o item de áudio do 1.4.
- **11 sons:** Azul; carga, disparo e explosão do Vermelho; Infinito ligando, desligando e o tic; carga e disparo do Roxo; domínio e o domínio rachando.
- **Ligação nos golpes:** os ids ficam em `SonsGojo` (núcleo). Os sons de fase (Azul, explosão do Vermelho, tic do Infinito) tocam no cliente quando o visual chega, no ponto do visual. Os de gesto saem pelo `ctx.apresentacao`.
- A prévia em áudio foi para o autor ouvir.

## O que ficou para as próximas partes
- **Parte 2:** cortar o som quando a carga é interrompida. Por isso a carga do Vermelho (que o jogador solta quando quer) e o domínio rachando ainda não tocam no jogo, embora os arquivos existam.
- **Parte 3:** os sete sons do R. Até lá o R fica sem som (antes tocava o som de teste).

## Do mesmo dia: vídeo do R completo
A demo gravável ganhou o modo `-Pdemo=r`, que invoca um alvo longe do spawn, usa o R de verdade e grava a cutscene inteira. Ao gravar apareceram dois defeitos da explosão, já corrigidos:
- a casca de distorção do Photon saía meia esfera para o lado, por causa do pivô do modelo;
- centrada, ela cobria o brilho de dentro; o shader passou a cobrir só a borda e a linha de interseção.
