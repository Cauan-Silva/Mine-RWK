---
tags: [planejamento]
---
# Pendências

Voltar para [[00 Início]].

## Para conferir
- `vfxReduzido` ainda não é lido: ligar ao Photon quando houver efeitos. `LogicaHud.teclasVisiveis` não é usada (a sincronização já manda só as teclas existentes).
- O tremor continua decaindo com o jogo pausado (até 15 ticks): irrelevante por ora.
- Animações do Minezin estão travadas na Player Animation Library: refazer/suavizar no Blockbench na Fase 2 (Gojo).
- O som de teste (robô) e a falta de VFX nos golpes do `teste` são provisórios: somem com o Gojo.
- Teclas X e C conflitam com salvar/carregar barra do criativo (configurável).
- Ninguém chama `atordoar()` ainda: postura vem na Fase 4.
- Personagem salvo cujo id deixar de existir prende o jogador sem kit (só `/rwk trocar` resolve). Tratar antes de remover/renomear algum personagem.
- Recargas salvas como tick absoluto: em mundo resetado mantendo jogadores podem ficar enormes. Limitar ao carregar.
- Tela de escolha simples sem rolagem: com 15 personagens não cabe (resolver no 1.4). Lista vai como "id|nome|obra".
- `FakePlayer` de outros mods enche `APANHOU`/`CAUSOU_DANO` em `ServicoKits` (vazamento pequeno).
- Como sincronizar as animações da Player Animation Library para os outros jogadores.
- Texto da licença do Sonniss GDC Bundle no site oficial.

## Para testar no jogo
- HUD (09/11): funcionou no jogo. Depois das correções da revisão, conferir: HUD acima da armadura e das bolhas de ar (mergulhar com armadura), texto "Fôlego" à esquerda da barra, barra sem subir de 0 ao entrar no mundo.
- Núcleo dos kits (09/11): escolha pelo menu, Z, X segurando e recarga **funcionaram**. Falta: morrer segurando X (não pode disparar), usar como espectador (nada acontece), ver a animação em outro jogador (2 clientes), sair e entrar mantendo o personagem.
- A tela de escolha pode abrir por cima da tela de carregamento do mundo: conferir.
- `/rwk anim` com `gojo_vermelho`, `gojo_infinito`, `gojo_roxo`, `gojo_dominio` (o `gojo_azul` já foi confirmado em 09/11). `/rwk parar` interrompe.
- Animação em 1ª pessoa: a Player Animation Library não mostra; decidir depois se os golpes precisam de braços visíveis em 1ª pessoa.
- `/rwk som`: toca o som de teste e mostra a legenda.
- `/rwk fx <efeito>`: depois de criar um `.fx` no editor do Photon2, confirmar que aparece na posição do jogador; sem arquivo, deve responder "não encontrado".
