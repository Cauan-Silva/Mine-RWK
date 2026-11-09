---
tags: [diário]
---
# 09/11/2026: cutscene do Vazio Roxo máximo e mais animações do R

Voltar para [[Diário]]. Spec: [[2026-11-09 Cutscene do Vazio Roxo máximo]].

- No teste do Vazio Roxo máximo, o autor achou que "melhorou bastante", mas a cutscene não mostrava a explosão de longe e faltavam animações. Ele escolheu:
  - a explosão em plano aberto;
  - a cutscene cobrindo a sequência inteira;
  - animações novas no encantamento e no ar;
  - a cutscene vista também pelo alvo.
- **Cutscene em duas partes:**
  - **Parte 1, o encantamento:** termina no disparo e segura o último plano até a parte 2 chegar.
  - **Parte 2:** o servidor manda no disparo (`CutsceneRoxoMaximo`). Ela é montada pelo avanço, que vai no id. Os planos são:
    - o avanço atrás do ombro, com o FOV abrindo;
    - o uppercut de baixo;
    - a subida de lado;
    - as esferas de frente;
    - o close com o núcleo atrás;
    - um corte seco para o plano aberto a ~70 blocos, parado olhando a explosão;
    - a volta suave aos olhos.
  - O cliente emenda as duas partes sem soltar a câmera.
  - A `Coreografia` ganhou o corte seco (`Quadro.corte`).
- **Alvo jogador:** recebe a parte 2 no uppercut, entrando no meio, com a câmera na base do Gojo.
- **Fim da cutscene:**
  - se o golpe for descartado (troca de personagem, morte), o servidor manda `!fim`;
  - se o dono morrer, o cliente também encerra.
- **Animações:**
  - **Encantamento:** um gesto por palavra (mãos abrindo, braços cruzando, corpo arqueando, mãos se aproximando tremendo), com o corpo respirando entre eles.
  - **No ar:** o Gojo flutua com uma perna dobrada e reage quando a explosão passa por ele.
  - **Descida:** nova, em loop.
- **Voo:** acaba quando o Gojo pousa. O servidor para a pose de descida, no lugar dos 30 s fixos.
- **GameTests:** passaram de 18 para 21: o voo que acaba ao pousar, a descida no chão sem voo e o voo novo no meio da descida.
- **Revisão (subagente):** não achou nada crítico. Foram corrigidos:
  - um R novo antes de pousar herdava a descida e perdia o voo;
  - a pose de descida podia ficar em loop (teto de 30 s, água, escada, elytra). Agora para em todos esses casos;
  - ao pousar, o servidor cortava a animação de outro golpe. Agora só para a pose se ela ainda for a do Gojo; no R sem alvo, no chão, nem toca a pose;
  - as barras de cinema fechavam esperando a parte 2;
  - o plano aberto andava junto com a descida do Gojo. Agora fica parado.
