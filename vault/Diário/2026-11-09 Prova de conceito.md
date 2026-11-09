---
tags: [diario]
---
# 2026-11-09 Prova de conceito

Voltar para [[Diário]].

- Comandos de teste no cliente: `/rwk fx <efeito>`, `/rwk anim <animação>`, `/rwk parar`, `/rwk som` (`client/comando/ComandosTeste`). Sem namespace, o id vira `minerwk:`.
- Animações do Gojo do Minezin copiadas para `assets/minerwk/player_animations/gojo.json` (formato GeckoLib, que a Player Animation Library lê).
- Som de teste `minerwk:teste` registrado (`registro/Sons`) com um áudio CC0 da Kenney; créditos em `THIRD_PARTY_NOTICES.md`.
- O Minezin não tinha `.fx` (usava o Photon pelo código), então os efeitos serão feitos do zero no editor do Photon2.
- Testado no jogo: `/rwk anim gojo_azul` toca em 3ª pessoa (em 1ª pessoa não aparece, por isso o primeiro teste pareceu falhar); `/rwk som` toca; `/rwk fx` sem arquivo responde "não encontrado".
- Falta: criar um `.fx` de teste e conferir com Iris. Testes no jogo em [[Pendências]].
