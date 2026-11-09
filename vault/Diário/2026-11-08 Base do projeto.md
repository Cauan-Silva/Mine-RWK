---
tags: [diario]
---
# 08/11/2026: base do projeto

- Base técnica aprovada. Ao montar o build, descobri que o Photon também existe para 26.x (a pesquisa tinha dito que não); comparamos e ficamos com **1.21.1**. Ver [[Bibliotecas]].
- Projeto montado com ModDevGradle 2.0.148, NeoForge 21.1.265, Parchment, JDK 21 baixado pelo Gradle.
- Bibliotecas no build: Photon2 2.2.11, LDLib2 2.2.46.a, GeckoLib 4.9.3, Player Animation Library 1.1.6.
- Código inicial: `MineRWK`, `MineRWKClient` (registra a camada de animação do jogador), `EfeitosVisuais` (atalhos do Photon2), `AnimacoesJogador` e `core/kit/Recargas` com 5 testes.
- Testado: `build` com testes passando; servidor de GameTest e cliente abrem com todas as bibliotecas carregadas.
- Problemas resolvidos (detalhes em [[Ambiente e Build]]): acento em `Usuário` quebrava os sockets locais do Java; conflito de versões de fastutil/gson/slf4j/netty com o Minecraft.
- Próximo passo: prova de conceito com um efeito do Photon2 e uma animação do jogador.
