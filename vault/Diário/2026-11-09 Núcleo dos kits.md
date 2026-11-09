---
tags: [diario]
---
# 2026-11-09 Núcleo dos kits

Voltar para [[Diário]].

- Decidido com o autor: recurso próprio por personagem, escolha única pelo menu, habilidades instantâneas ou de segurar, custo no disparo e punição de 1/4 na interrupção, habilidades como classes Java. Spec e plano em `Desenvolvimento/`.
- Núcleo puro em `core/kit/` (`Execucao`, `KitDoJogador`, `Recurso`, `Personagem`, `Personagens`) com 22 testes JUnit.
- Servidor em `kit/`: dados no jogador (Data Attachment), tick, `/rwk trocar` (operador), contexto que causa dano e manda apresentação.
- Rede em `rede/`: `UsarTecla`, `EscolherPersonagem`, `SincronizarKit`, `TocarApresentacao`; envio só para quem tem o canal.
- Cliente em `client/kit/`: teclas Z X C V G R (+ K para escolher) e tela simples de escolha.
- 4 GameTests (`./gradlew runGameTestServer`). A revisão final achou que a habilidade disparava depois de morrer e funcionava no espectador; corrigido com testes.
- Testado no jogo pelo autor: escolha, Z, X e recarga funcionam. Animação travada, som de robô e sem VFX são provisórios ([[Pendências]]).
