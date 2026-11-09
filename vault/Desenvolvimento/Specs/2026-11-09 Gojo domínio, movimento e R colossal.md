---
tags: [desenvolvimento, spec]
estado: proposto
---
# Spec e plano: domínio fechado, animações livres ao andar e Roxo 200% colossal (Fase 2, parte 5)

Voltar para [[Roadmap]]. Vem do teste das animações em 09/11/2026 ([[2026-11-09 Gojo animações]]). O autor achou:
- Z, X, C e V parados enquanto o personagem anda;
- o R sem impacto: "a ideia é ele ir bem alto e fazer o Roxo colossal destruindo em um AOE bem grande";
- o G simples: o chão continua caindo, dá para sair andando e não há barreira.

A direção foi aprovada. Este spec traz os números para aprovar. São três partes num branch só, nesta ordem: domínio, animações e R.

## Objetivo
Pronto quando:
- **Domínio:** no G, ninguém entra nem sai do domo por 8 s, e ninguém cai dentro dele, mesmo sobre buraco, água ou penhasco.
- **Animações:** andando durante Z, X, C ou V, as pernas fazem a passada normal e os braços não ficam congelados.
- **R:** o Gojo sobe uns 20 blocos durante o encantamento, dispara um Roxo três vezes mais largo e termina numa explosão de raio 24, sem travar o servidor.
- **Testes:** os JUnit e os GameTests novos passam, e o autor aprova no jogo.

## 1. Domínio fechado (G)
- **Barreira:** é uma esfera de raio 12 em volta do ponto onde o G foi usado, e vale por 8 s.
  - Quem estava dentro no começo, o Gojo incluído, não sai. Quem estava fora não entra.
  - Quem encosta na borda volta para 0,6 bloco dela, e a velocidade para fora é zerada.
  - Vale para mobs, jogadores, flechas e outros projéteis.
- **Chão:** fica no nível dos pés do Gojo no momento do G.
  - Ninguém que está dentro desce abaixo dele. Quem está abaixo é levantado até ele.
  - O chão zera a velocidade para baixo e a queda acumulada.
  - Vale sobre ar, água, lava ou buraco, porque é regra de movimento e não depende de blocos.
- **Sem tranco para jogadores:** a posição do jogador é decidida pelo cliente dele. Por isso a mesma regra roda nos dois lados.
  - O cliente aplica a barreira e o chão ao próprio jogador, a partir do visual do domínio que ele já recebe (origem e duração).
  - O servidor aplica as mesmas regras a todos.
- **Sem expulsão por voo:** um jogador que fica mais de 4 s sem bloco embaixo é chutado pelo servidor ("Flying is not enabled"). Durante o domínio (e o voo do R), o contador de tempo no ar é zerado a cada tick. Para isso entra um access transformer no `aboveGroundTickCount`.
- **O bloco `piso_dominio` sai de uso.** Ele continua registrado só para os mundos antigos, onde ainda pode existir algum com o tick agendado para sumir.
- **Lógica no núcleo:** a regra é uma função pura em `core/kit/Barreira`. Ela recebe o centro, o raio, o chão, a posição, a velocidade e se a entidade estava dentro, e devolve a posição e a velocidade corrigidas. Tem testes JUnit.

## 2. Animações livres ao andar (Z, X, C, V)
- **Pernas e corpo livres:** as animações de Z, X, C e V deixam de mexer pernas e posição do corpo. A passada normal do Minecraft fica visível.
- **Cabeça:** move pouco, para não brigar com o olhar.
- **Braços nunca parados:** em toda pose segurada, os braços balançam devagar (uns 4°, a cada ~0,6 s) e o tronco "respira". Os golpes que já tremem continuam tremendo.
- **G e R mantêm as pernas.** O G é uma pose parada. No R o Gojo está no ar, com as pernas soltas.
- **Teste:** o JUnit passa a exigir que Z, X, C e V não tenham `right_leg`, `left_leg` nem `body`.

## 3. Roxo 200% colossal (R)
Muda números do kit aprovado.

| | Hoje | Proposta |
| --- | --- | --- |
| Encantamento | 4 s parado | 4 s, **subindo 20 blocos** nos primeiros 3 s (subida suave) e pairando no último |
| Massa roxa | raio 2,5 · 2 blocos/tick · 120 blocos · dano 60 | **raio 7** · 2 blocos/tick · 120 blocos · dano 60 uma vez por alvo |
| Fim | a massa some | **explosão de raio 24** no ponto onde a massa parou (ou bateu em bloco inquebrável), crescendo em 12 ticks · dano 60 uma vez por alvo · arremesso forte |
| Depois | — | desce com queda lenta e sem dano de queda até tocar o chão (no máximo 10 s) |
| Custo, recarga, corte por golpe forte | energia cheia · 5 min · dano ≥ 6 corta | sem mudança |

- **Subida:**
  - O cliente sobe o próprio jogador, igual à regra do domínio.
  - Para no teto se houver bloco acima.
  - O servidor zera o tempo no ar e a queda acumulada.
  - O disparo sai dos olhos lá no alto, na direção para onde o jogador olhava no começo.
- **Custo no servidor:** hoje cada bloco apagado avisa os vizinhos (`UPDATE_ALL`). No R colossal, só o cliente é avisado (`UPDATE_CLIENTS`), sem atualizar vizinhos.
  - A explosão apaga em cascas (raio de 2 em 2 por tick) para não pesar num tick só.
  - Limite de **4.000 blocos por tick**: o que passar fica para o tick seguinte.
  - A regra `minerwkApagarBlocos` continua desligando tudo.
  - Os detritos visuais seguem limitados a 12 por chamada.
- **Visual:**
  - A esfera roxa acompanha o raio novo.
  - A explosão final reusa o anel e o clarão do Vermelho, em roxo e bem maior.
  - Tremor e clarão em raio de 64 blocos.
  - O VFX pesado de verdade fica para a parte de VFX.
- **Cutscene:** a câmera passa a seguir os olhos do Gojo enquanto ele sobe, e não mais o ponto onde ele começou. A cutscene refeita pelas referências (cortes secos, olhos, mundo sem cor) continua sendo a parte seguinte.

## Testes
- **JUnit:**
  - `Barreira`: quem está dentro não sai, quem está fora não entra, o chão segura e levanta, e a velocidade para fora é zerada;
  - a altura da subida do R ao longo do tempo;
  - as cascas da explosão: cada bloco do raio é apagado uma vez só e o limite por tick é respeitado;
  - os números novos do R no `GojoTest`;
  - as animações sem pernas em Z, X, C e V.
- **GameTests:**
  - um zumbi dentro do domínio não cai sobre o vazio e não passa da borda;
  - um zumbi fora não entra;
  - o R colossal apaga um volume grande e fere um alvo a 20 blocos do fim.
- **No jogo (autor):**
  - o G sobre um penhasco e sobre a água;
  - tentar sair andando e entrar de fora;
  - Z, X, C e V andando;
  - o R completo, vendo a TPS (F3) na explosão.
