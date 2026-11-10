---
tags: [diario]
---
# 10/11/2026: som do Gojo (partes 1, 2 e 3)

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

## Parte 3, no mesmo dia: os sete sons do R
| Som | Quando toca | Ideia |
| --- | --- | --- |
| `gojo_r_encantamento` | com a carga (80 ticks); cortado se a carga for interrompida | hum grave crescendo, com uma batida e um toque vítreo em cada palavra |
| `gojo_r_avanco` | no disparo | baque nos pés e o ar rasgando |
| `gojo_r_uppercut` | no tick do soco, no ponto do soco | quase mudo nos 3 ticks do impact frame; depois o impacto seco e a cauda |
| `gojo_r_subida` | 3 ticks depois do soco | vento subindo de tom |
| `gojo_r_esferas` | quando as esferas nascem (30 ticks) | dois tics nas mãos, Azul grave e Vermelho agudo se aproximando, tic do toque e o afundar |
| `gojo_r_fusao` | quando o núcleo nasce (40 ticks) | zumbido batendo cada vez mais rápido, com 4 ticks de silêncio no fim |
| `gojo_r_explosao` | no estouro (60 + 50 ticks) | estalo enorme, sub distorcido, corpo rasgando por 3 s e cauda |

- Uppercut, subida e fusão tocam no meio de um visual, numa janela de ticks e uma vez só (`SonsGojo.noMeio`, com JUnit).
- O `SonsGojoTest` confere os marcos contra o Java: as quatro palavras, o impact frame, o toque das esferas, o silêncio antes do estouro e a duração da explosão.
- Prévia: `previas/r-completo-com-som.mp4`, o vídeo da demo com a trilha montada por fora, cada som no tick da sua fase.

Com isso o Gojo tem 18 sons, três variações de cada, 1,2 MB no total.

O autor aprovou os sons pela prévia (10/11).

## Do mesmo dia: vídeo do R completo
A demo gravável ganhou o modo `-Pdemo=r`, que invoca um alvo longe do spawn, usa o R de verdade e grava a cutscene inteira. Ao gravar apareceram dois defeitos da explosão, já corrigidos:
- a casca de distorção do Photon saía meia esfera para o lado, por causa do pivô do modelo;
- centrada, ela cobria o brilho de dentro; o shader passou a cobrir só a borda e a linha de interseção.
