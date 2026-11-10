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

## Parte 2, no mesmo dia: som preso ao visual e som que para
- **`SonsVisuais`** (cliente) toca o som no ponto do visual, corta o som da carga quando o visual é encerrado antes da hora e toca o som de fim perto do término. Qual som vai com qual visual fica no núcleo (`SonsGojo`), com JUnit.
- **Carga do Vermelho:** agora tem som, e ele para quando a tecla é solta ou a carga é interrompida.
- **Carga do Roxo:** o som passou a sair do visual da carga, então para junto se o golpe for interrompido.
- **Domínio rachando:** começa 8 ticks antes de o domínio sumir, com o estilhaço caindo no tick em que ele some. É uma janela, não um tick exato, e toca uma vez só.
- **Domínio na carga:** se a carga de 10 ticks for interrompida, o som para. Depois da abertura ele soa até o fim.
- **Correção da parte 1:** a explosão do Vermelho nascia no cliente por outro caminho e ficava muda.
- A revisão apontou o tick exato do som de fim, um som guardado que podia ser cortado por uma parada que não era dele e o som que continuava ao trocar de dimensão. Os três foram corrigidos.

## O que ficou para a parte 3
Os sete sons do R. Até lá o R fica sem som (antes tocava o som de teste).

## Do mesmo dia: vídeo do R completo
A demo gravável ganhou o modo `-Pdemo=r`, que invoca um alvo longe do spawn, usa o R de verdade e grava a cutscene inteira. Ao gravar apareceram dois defeitos da explosão, já corrigidos:
- a casca de distorção do Photon saía meia esfera para o lado, por causa do pivô do modelo;
- centrada, ela cobria o brilho de dentro; o shader passou a cobrir só a borda e a linha de interseção.
