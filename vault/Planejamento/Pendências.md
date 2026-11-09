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
- `FakePlayer` de outros mods enche `APANHOU`/`CAUSOU_DANO` em `ServicoKits` (vazamento pequeno).
- Como sincronizar as animações da Player Animation Library para os outros jogadores.
- Texto da licença do Sonniss GDC Bundle no site oficial.

- Tela de escolha (revisão 09/11, menores): em janela muito baixa (≈300×120 com escala grande) o botão Escolher sobrepõe o painel ou sai da tela; o cache de emblemas não limpa no F3+T; a roda rola a grade mesmo com o mouse no painel.

- Gojo (revisão 09/11, menores que ficaram): o Roxo não avisa mods de proteção de terreno (sem `BreakEvent`; só respeita a proteção do spawn) e pode derrubar plantas vizinhas; o Vermelho atravessa parede fina encostada no rosto; bola de fogo e crânio de Wither voltam a acelerar no Infinito (o dano é cancelado mesmo assim); TNT sem dono passa pelo Infinito; sobram até 2 ticks de Infinito e 5 de Paralisia depois de morrer. Corrigidos na revisão: efeitos acabam ao mudar de dimensão; golpes não atingem pets, montaria nem projéteis do próprio Gojo; Paralisia não cancela Espinhos nem dano em si mesmo; ícone da Paralisia escondido (falta textura).

## Para testar no jogo
- Gojo (09/11), mundo novo escolhendo o Gojo: Z puxa mobs para o ponto à frente; X segurando lança a esfera que explode e empurra; C faz flechas pararem no ar e mobs não encostarem (some depois de ~6 s ou sem energia); V abre um túnel de 40 blocos; G paralisa mobs num raio de 12 por 8 s; R (energia cheia) carrega 4 s e abre um túnel de 120 blocos, e um golpe forte na carga cancela. Técnica Reversa: vida volta sozinha depois de 5 s sem dano. Conferir `/gamerule minerwkApagarBlocos false`. Ver também a TPS com o Roxo 200%.
- HUD (09/11): funcionou no jogo. Depois das correções da revisão, conferir: HUD acima da armadura e das bolhas de ar (mergulhar com armadura), texto "Fôlego" à esquerda da barra, barra sem subir de 0 ao entrar no mundo.
- Núcleo dos kits (09/11): escolha pelo menu, Z, X segurando e recarga **funcionaram**. Falta: morrer segurando X (não pode disparar), usar como espectador (nada acontece), ver a animação em outro jogador (2 clientes), sair e entrar mantendo o personagem.
- Tela de escolha (09/11): **funcionou no jogo** (autor). Conferido: mundo novo abre a tela com o `teste` (emblema genérico), golpes Z e X no painel, "Escolher" → confirmação. `/rwk tela 40` mostra a grade com rolagem pela roda do mouse e emblemas do Minezin; o 15º falso usa o genérico. Testar também com a janela estreita (painel embaixo) e com escala de interface grande.
- A tela de escolha pode abrir por cima da tela de carregamento do mundo: conferir.
- `/rwk anim` com `gojo_vermelho`, `gojo_infinito`, `gojo_roxo`, `gojo_dominio` (o `gojo_azul` já foi confirmado em 09/11). `/rwk parar` interrompe.
- Animação em 1ª pessoa: a Player Animation Library não mostra; decidir depois se os golpes precisam de braços visíveis em 1ª pessoa.
- `/rwk som`: toca o som de teste e mostra a legenda.
- `/rwk fx <efeito>`: depois de criar um `.fx` no editor do Photon2, confirmar que aparece na posição do jogador; sem arquivo, deve responder "não encontrado".
