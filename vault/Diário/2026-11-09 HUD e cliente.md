---
tags: [diario]
---
# 2026-11-09 HUD e cliente

Voltar para [[Diário]].

- O 1.4 foi dividido em três partes: HUD e cliente (feita), tela de escolha bonita, áudio (começo da Fase 2).
- HUD acima da pilha vanilla: quadrados por tecla com recarga escurecendo e segundos, letra vermelha sem recurso, borda branca em uso, barra do recurso com nome e cor do personagem. Contas em `core/hud/LogicaHud` (JUnit).
- Tremor de tela próprio: `core/camera/Tremor` (JUnit), pacote `Tremer`, aplicado em `ViewportEvent.ComputeCameraAngles`. O X do personagem de teste treme conforme a força.
- Configuração do cliente `minerwk-client.toml` com tela no menu de Mods.
- Esboços de posição do HUD feitos direto na conversa (o autor pediu para economizar uso). Revisão final com modelo mais barato achou colisão com armadura/ar e a barra subindo de 0 ao entrar; corrigido.
- Testado no jogo pelo autor: funcionando.
