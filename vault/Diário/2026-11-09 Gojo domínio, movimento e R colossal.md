---
tags: [diário]
---
# 09/11/2026: domínio fechado, animações livres e R colossal

Voltar para [[Diário]]. Spec: [[2026-11-09 Gojo domínio, movimento e R colossal]].

- No teste das animações, o autor achou tudo duro: Z, X, C e V parados enquanto anda, o R sem impacto ("ir bem alto e fazer o Roxo colossal destruindo em um AOE bem grande") e o G sem barreira, com o chão ainda caindo.
- **Domínio:** o chão deixou de ser feito de blocos e virou uma regra de movimento (`core/kit/Barreira`). O servidor aplica a regra a todas as entidades, e o cliente ao próprio jogador, a partir do visual do domínio, então não há tranco. A barreira prende quem estava dentro e barra quem estava fora. Um access transformer zera o contador que chutava quem fica mais de 4 s no ar.
- **Animações:** Z, X, C e V não mexem mais as pernas nem o corpo, e os braços balançam enquanto seguram a pose.
- **R:**
  - O cliente sobe o jogador 20 blocos durante o encantamento. O servidor só tira o dano de queda e o chute por voo.
  - A massa ficou com raio 7 e explode onde bate, num raio de 24.
  - O volume grande é apagado aos poucos (`ApagadorServidor`, 4.000 blocos por tick, sem atualizar vizinhos).
  - A câmera da cutscene sobe junto.
- **GameTests:** passaram de 15 para 17. Entraram o zumbi preso no ar, o zumbi de fora barrado e as cascas da explosão. Um bug apareceu no teste: a primeira casca pulava o bloco do centro.
