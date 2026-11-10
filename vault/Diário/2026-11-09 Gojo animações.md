---
tags: [diário]
---
# 09/11/2026: animações dos golpes do Gojo

Voltar para [[Diário]]. Spec: [[2026-11-09 Gojo animações]].

- O autor achou a cutscene dura e os golpes simples, e mandou dois vídeos de referência (anotados no spec). A ordem aprovada é: animações, cutscene nova do R, VFX mais pesado e som.
- As animações do Minezin mexiam só braços e tronco e não apareciam em primeira pessoa. Foram trocadas por seis novas com o corpo inteiro (cabeça, pernas, posição), antecipação, exagero e suavização variada. O Roxo 200% ganhou a dele, com quatro gestos e o corpo flutuando.
- A animação começa na carga e para quando a carga é cortada (`Contexto.pararAnimacao`). O Vermelho pula para a pose do coice ao disparar, porque a carga dele tem tamanho variável.
- Primeira pessoa: `FirstPersonMode.THIRD_PERSON_MODEL` da Player Animation Library mostra os braços animados.
- O JSON é gerado por `ferramentas/gerar_animacoes_gojo.py` e abre no Blockbench. Um teste JUnit confere ids, duração (carga + golpe) e ossos.
- Os sinais dos ângulos seguem as animações antigas. Só o teste no jogo confirma se algum braço ou perna vai para o lado errado.
