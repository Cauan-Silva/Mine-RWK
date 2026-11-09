---
tags: [diário]
---
# 09/11/2026: Vazio Roxo máximo com uppercut e VFX do Azul e do Infinito

Voltar para [[Diário]]. Spec: [[2026-11-09 Vazio Roxo e VFX do Azul e do Infinito]].

- No teste anterior, o autor aprovou o Vermelho e o Roxo (V), achou o Azul e o Infinito básicos e explicou o R: o Azul e o Vermelho se fundem no ar e o Roxo explode em volta, sem feixe. Depois pediu o uppercut no alvo e a fusão **atrás do Gojo**, com a explosão engolindo os dois.
- **R (Vazio Roxo máximo):**
  - As contas ficam em `core/personagens/gojo/RoxoMaximo`: avanço a 3 blocos por tick (no máximo 20 ticks), subida de 25 blocos em 15 ticks, esferas, fusão e explosão. O servidor e o cliente usam as mesmas contas.
  - O `Contexto` ganhou `marcarAlvo`, `posicaoDe`, `suspender` e `ferir`. O alvo é a criatura mais perto da linha da mira (até 60 blocos, até 4 da linha, com linha de visão).
  - O servidor segura o alvo no ar (`SuspensosServidor`, por teleporte a cada tick, sem gravar NoAI). O cliente move o próprio Gojo a partir do visual `gojo_roxo_maximo`, e ele não sobe mais no encantamento.
  - A explosão nasce 4 blocos atrás do Gojo e cresce até o raio 40 em 16 ticks. O alvo leva 10 no uppercut e 60 na explosão, uma vez só. Sem alvo, as esferas vão até onde a mira bate e explodem lá.
  - O feixe colossal saiu, e o `FeixeRoxo` voltou a ser só do V.
  - Visuais novos: o rastro do avanço, o clarão do uppercut, o rastro do alvo subindo, o Azul e o Vermelho saindo das mãos em arcos, e a fusão com clarão, núcleo pulsando e anéis girando. A explosão recebe o raio do servidor.
  - Animações novas: o avanço com o punho para trás, `gojo_roxo_maximo` (uppercut, mãos abrindo para os lados e para trás, punho estendido na explosão) e a versão sem alvo.
- **Azul:** espirais de vento apertando até o núcleo, anéis de sucção se fechando, núcleo escuro pulsando, anel de espaço dobrando e poeira e folhas sugadas. As formas estão em `FluxoAzulGojo`.
- **Infinito:** brilho correndo pela bolha, hexágonos que acendem e apagam e poeira que entra devagar. O servidor manda `gojo_infinito_toque` quando o Infinito para algo: ondas saem do ponto do toque e o projétil parado treme com um halo. As formas estão em `BolhaInfinitoGojo`.
- **GameTests:** passaram de 17 para 18. Entrou o zumbi na mira do R, que é ferido e segurado 25 blocos acima.
- **Revisão (subagente):** não achou nada crítico. Foram corrigidos:
  - o Gojo podia levar dano de queda no fim do R, se a explosão abrisse uma cratera embaixo dele. A queda lenta e o voo agora duram até 30 s;
  - o uppercut acertava um alvo que tinha corrido para longe. Agora o alvo é segurado onde o Gojo bate (teste novo);
  - uma flecha parada pelo Infinito repetia o toque a cada 3 ticks. Agora ela é marcada na primeira parada;
  - os visuais que movem o jogador não são mais despejados quando a lista enche;
  - um alvo montado é desmontado antes de ser segurado;
  - o dragão e o Wither não viram alvo.
