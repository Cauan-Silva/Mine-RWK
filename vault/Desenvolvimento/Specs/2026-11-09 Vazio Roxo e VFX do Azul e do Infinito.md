---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec e plano: R vira o Vazio Roxo máximo explosivo; VFX do Azul e do Infinito (Fase 2, parte 6)

Voltar para [[Roadmap]]. Vem do teste de 09/11/2026 (depois de [[2026-11-09 Gojo domínio, movimento e R colossal]]):
- **Aprovados:** as animações melhoraram, e o Vermelho e o Roxo (V) estão bons. O V não muda.
- **Azul e Infinito:** ainda "muito básicos".
- **R:** "Gojo joga o azul e o vermelho, eles mesclam no ar gerando o vazio roxo máximo, explodindo e destruindo tudo o que tem em volta, sem lançar o vazio roxo." O autor tinha escrito V e corrigiu: **é o R** (o Vazio Roxo máximo).

**Base no cânone:** já está em [[Gojo]]. No fim da luta contra o Sukuna (cap. 235), o Roxo sai como explosão em todas as direções, sem avançar como feixe. A segunda referência em vídeo (caps. 234 e 235) mostra a mesma cena.

## Objetivo
Pronto quando:
- o R, depois do encantamento lá no alto, lança o Azul e o Vermelho, que se encontram no ar, se fundem e explodem em roxo no lugar, sem feixe;
- o Azul e o Infinito têm visual mais rico (lista abaixo);
- os testes passam e o autor aprova no jogo.

## 1. R: Vazio Roxo máximo com uppercut
Escolhas do autor (09/11):
- Gojo avança e dá o uppercut no alvo, que é jogado pra cima.
- Gojo sobe junto. O Azul e o Vermelho se fundem **atrás do Gojo**, e o Roxo explode e se expande, engolindo o Gojo e o alvo. O Gojo não leva dano do próprio golpe.
- A explosão tem **raio 40**.
- Sem ninguém na mira, as esferas vão até onde a mira bate e explodem ali, sem uppercut.

Continua igual: o encantamento de 4 s com a cutscene, a energia cheia, a recarga de 5 min e o corte por golpe forte na carga.

**Muda:**
- O Gojo **não sobe mais durante o encantamento**. Fica no chão recitando e só sobe junto com o alvo.
- **Sai o feixe colossal.**

**Sequência depois do encantamento (ticks a partir do disparo):**

| Fase | Ticks | O que acontece |
| --- | --- | --- |
| Mira | 0 | Alvo: a criatura ou o jogador mais perto da linha da mira, a até **60 blocos**, ficando no máximo a 4 blocos da linha. Ele fica marcado. |
| Avanço | 0 até A | O Gojo dispara pela linha até 1,5 bloco do alvo, a **3 blocos/tick**: A = distância ÷ 3, no máximo 20. Atravessa o ar sem quebrar nada e para se bater em bloco. |
| Uppercut | A | Soco para cima: **10 de dano** e o alvo sobe **25 blocos** em 15 ticks (suave, segurado pelo servidor, como o congelamento do arremesso). O Gojo sobe junto, ao lado dele. |
| Esferas | A+15 até A+25 | Lá no alto, o Azul sai da mão esquerda e o Vermelho da direita, em arcos **para trás**, e se encontram **4 blocos atrás do Gojo** (do lado oposto ao alvo). O alvo continua parado no ar à frente. |
| Fusão | A+25 até A+35 | O núcleo roxo pulsa atrás do Gojo e cresce, e o mundo escurece. Não puxa ninguém: o alvo fica segurado no ar. |
| Explosão | A+35 até A+51 | O Roxo explode **atrás do Gojo** e se expande até o **raio 40** em 16 ticks, engolindo o Gojo e o alvo. **60 de dano** uma vez por alvo (o alvo marcado sempre leva, e o Gojo nunca) e arremesso forte para longe do centro. Os blocos são apagados aos poucos pelo `ApagadorServidor` (no ar quase não há bloco; no chão abaixo, o que a esfera alcançar). |
| Queda | depois | O alvo é solto e cai, levando o dano de queda normal. O Gojo desce devagar, sem dano de queda (como hoje). |

- **Sem alvo:** não há avanço nem uppercut, e é a escolha do autor (explode onde mira). As esferas vão do Gojo até onde a mira bate (no máximo 60 blocos) em 20 ticks, se fundem ali e explodem com raio 40.
- **Alvo some no meio** (morre, sai ou muda de dimensão): a sequência continua no último ponto onde ele estava.
- **Movimento do Gojo:** como na subida de hoje, o cliente move o próprio jogador a partir do visual (avanço e subida), e o servidor só tira o dano de queda e o chute por voo. O alvo é movido pelo servidor. Se for jogador, por teleporte a cada tick, como o congelamento.
- **Núcleo testável:** uma classe `core/personagens/gojo/RoxoMaximo` com as fases e as funções `posicaoGojo(t)` e `alturaAlvo(t)`, usadas pelo servidor e pelo cliente.
- **Custo no servidor:** uma esfera de raio 40 tem ~268 mil posições. A maior parte é ar no alto, mas, perto do chão, apagar tudo leva alguns segundos a 4.000 blocos por tick. O limite por tick continua o mesmo.
- **Visual:**
  - o rastro do avanço;
  - o clarão e o tremor no uppercut;
  - as duas esferas em arco para trás, cada uma com o seu rastro;
  - no encontro atrás do Gojo, um clarão branco e anéis girando;
  - o núcleo roxo pulsando e crescendo às costas dele;
  - a explosão roxa que já existe, com raio 40, nascendo atrás e passando por cima do Gojo e do alvo;
  - tremor e clarão na câmera de quem está perto.
- **Animação:** no disparo do R, avanço com o punho para trás e uppercut com o braço direito subindo. Lá no alto, as duas mãos se abrem para os lados e para trás (esquerda e depois direita), e o Gojo fica de frente para o alvo. Na explosão, o braço fica estendido com o punho fechado.
- **Cutscene:** por enquanto acaba como hoje, logo depois do disparo. A cutscene nova (próxima parte) vai cobrir o avanço, o uppercut e a explosão.
- **Teste:**
  - **JUnit do `RoxoMaximo`:** as fases nos ticks certos, o avanço que para a 1,5 bloco, a subida de 25 em 15 ticks e o caso sem alvo.
  - **`GojoTest`:** uppercut com 10 de dano; esferas, fusão e explosão com cascas até o raio 40, com o centro 4 blocos atrás do Gojo; o alvo marcado leva o dano; nenhum feixe.
  - **GameTest:** zumbi na mira é jogado 25 blocos pra cima e leva o dano.

## 2. Azul mais rico (só visual)
- **Espirais de vento:** fitas brancas e azuis girando para dentro do núcleo, apertando a cada volta.
- **Anéis de sucção:** círculos que nascem largos e se fecham no núcleo, várias vezes por segundo.
- **Núcleo:** pulsa e "engole a luz", com o centro escuro e a borda ciano forte.
- **Partículas:** poeira e folhas do chão sendo sugadas, além dos blocos que já orbitam.
- **Ar distorcido:** um anel em volta do núcleo que ondula, simulando o espaço dobrando.

## 3. Infinito mais rico (só visual)
- **Ondas na bolha:** quando algo encosta (projétil, mob ou golpe parado), a bolha mostra ondas saindo do ponto do toque.
- **Objetos parados:** um projétil parado pelo Infinito fica tremendo no ar com um halo azul, antes de cair.
- **Superfície viva:** brilho que corre pela bolha e um padrão sutil de hexágonos que aparece e some.
- **Partículas:** poeira desacelerando em volta, como se o tempo ficasse lento perto do Gojo.

Com isso, o servidor passa a avisar o cliente quando o Infinito para algo (ponto e direção do toque), por um visual curto `gojo_infinito_toque`.

## Fora deste spec
Continuam na fila, nessa ordem:
1. cutscene nova do R (agora com o avanço, o uppercut, as duas esferas e a explosão);
2. VFX pesado dos outros golpes;
3. som.
