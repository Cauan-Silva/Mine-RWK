---
tags: [planejamento]
---
# Pendências

Voltar para [[00 Início]].

## Para conferir
- `vfxReduzido` já reduz faixas e segmentos do VFX do Gojo; os emissores de brilho do Photon (`GolpesGojoPhoton` do Minezin) ficaram de fora: a API do Photon2 mudou e o shader já carrega o visual. Retomar se faltar brilho. `LogicaHud.teclasVisiveis` não é usada (a sincronização já manda só as teclas existentes).
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

- Gojo (revisão 09/11, menores que ficaram): o Roxo pode derrubar plantas vizinhas (o `BreakEvent` para mods de proteção entrou em 09/11, no Gojo peso); o Vermelho atravessa parede fina encostada no rosto; bola de fogo e crânio de Wither voltam a acelerar no Infinito (o dano é cancelado mesmo assim); TNT sem dono passa pelo Infinito; sobram até 2 ticks de Infinito e 5 de Paralisia depois de morrer. Corrigidos na revisão: efeitos acabam ao mudar de dimensão; golpes não atingem pets, montaria nem projéteis do próprio Gojo; Paralisia não cancela Espinhos nem dano em si mesmo; ícone da Paralisia escondido (falta textura).

## Para testar no jogo
- Gojo (09/11): **as seis teclas funcionaram no jogo sem erro** (autor). Ainda vale conferir com calma, mundo novo escolhendo o Gojo: Z puxa mobs para o ponto à frente; X segurando lança a esfera que explode e empurra; C faz flechas pararem no ar e mobs não encostarem (some depois de ~6 s ou sem energia); V abre um túnel de 40 blocos; G paralisa mobs num raio de 12 por 8 s; R (energia cheia) carrega 4 s e abre um túnel de 120 blocos, e um golpe forte na carga cancela. Técnica Reversa: vida volta sozinha depois de 5 s sem dano. Conferir `/gamerule minerwkApagarBlocos false`. Ver também a TPS com o Roxo 200%.
- Gojo VFX (09/11), conferir no jogo: Z esfera escura com filamentos ciano puxando; X carga vermelha na mão, esfera viajando e explosão em anéis no ponto do impacto; C bolha azulada em volta do jogador; V e R duas esferas (azul e vermelha) se juntando à frente e a massa roxa com coroa e rastro seguindo o túnel; G domo escuro de raio 12 com chão estrelado, linhas coloridas e neblina escura só dentro. Ver de outro jogador também (os visuais vão para quem vê). Conferir FPS, o `VFX reduzido` na config e com Iris ligado. Se o shader não carregar, o log avisa e o desenho sai só com cor.
- Gojo impacto (09/11), conferir no jogo: tela treme e pisca e o FOV dá um tranco no Vermelho (explosão), Roxo, Roxo 200% (o chão já treme na carga) e domínio; Azul puxa o FOV. Pedaços dos blocos voam no Roxo e na explosão do Vermelho; Azul suga pedaços do chão; poeira em anel na abertura do domínio. Brilho em volta das esferas. Longe (fora do raio) não sente nada. A opção do jogo "Efeitos de FOV" zera o tranco. Ver FPS e rede com o Roxo 200% abrindo túnel.
- Gojo peso (09/11), conferir no jogo: Azul dura 3 s e arranca terra, grama e flores (não pedra) que giram em volta do núcleo e caem quando acaba, deixando uma marca no chão; Vermelho voa mais devagar, o mob para no ar um instante e sai voando girando, e sobra fumaça com brasas; Roxo anda devagar (2 s, Roxo 200% 3 s), quem sobrevive é jogado para o lado, o túnel brilha e solta fumaça por 3 s; ao acertar, a tela de quem bateu trava um instante; o domínio racha no fim. Correções do teste do autor (09/11): a marca do Azul aparece desde o começo; os blocos arrancados flutuam e orbitam o núcleo e só caem no fim; o chão do domínio é sólido (bloco invisível `piso_dominio` que some sozinho por tick agendado), dá para ficar em pé no ar.
- Roxo 200% cutscene (09/11), conferir no jogo: com energia cheia, R abre a cutscene (barras, câmera girando, mãos, de baixo, rosto, atrás do ombro no disparo, volta suave); as quatro palavras em japonês e português; levar golpe forte na carga corta e devolve a câmera; outro jogador vê as palavras girando em volta do Gojo e o céu escurecer; `cutscenes = false` na config deixa só as palavras. Conferir F5, Esc e o chat durante a cutscene, e que o Roxo sai para onde o jogador olhava no início. Conferir jogador arremessado por PvP (o congelamento segura a posição por teleporte). Menores da revisão que ficaram: o Roxo também congela quem é invulnerável no raio (criativo) e pode recongelar quem já terminou o arremesso; o túnel do Roxo 200% é desenhado inteiro mesmo longe.
- Gojo domínio, movimento e R colossal (09/11), conferir no jogo: G sobre um penhasco e sobre a água (ninguém cai; o chão fica no nível dos pés de quando usou), tentar sair andando e pular contra a borda, mob de fora não entra, flecha de dentro não sai; ficar 8 s no ar sem ser chutado. Z, X, C e V andando: as pernas fazem a passada normal e os braços balançam. R: sobe 20 blocos no encantamento (para no teto), o Roxo sai lá de cima bem mais largo e explode onde bate num raio de 24; desce devagar sem dano de queda. Ver a TPS (F3) na explosão e com a regra `minerwkApagarBlocos false`. O R usado por outro jogador: ver de fora. Menores da revisão que ficaram: quem está na borda do domínio no instante em que ele abre pode ser decidido "dentro" no cliente e "fora" no servidor (o servidor puxa de volta se passar de 1,5 bloco); o GameTest `vermelhoCongelaEArremessaZumbi` falhou 1 vez em 4 rodadas (provável interferência de teste vizinho; barreiras dos testes encurtadas); com `UPDATE_CLIENTS` a explosão deixa água parada e tochas/areia soltas no ar.
- Gojo animações (09/11), conferir no jogo, em 1ª pessoa e com F5: cada golpe tem a sua animação desde a carga (Azul puxa o braço e empurra a mão tremendo; Vermelho aponta com a outra mão no pulso, pende para trás e dá um coice no disparo, também soltando cedo; Infinito mão aberta à frente; Roxo abre os braços, junta e estica no tranco; Roxo 200% quatro gestos, corpo flutuando 0,6 bloco e tranco no disparo, visível pela câmera da cutscene; domínio mão no rosto). Em 1ª pessoa os braços animados aparecem. Golpe forte na carga do R para a animação. Outro jogador vê tudo. Se algum ângulo sair torto (sinal trocado de braço ou perna), ajustar em `ferramentas/gerar_animacoes_gojo.py` ou no Blockbench.
- Vazio Roxo máximo e VFX do Azul e do Infinito (09/11), conferir no jogo:
  - **R com um mob na mira** (até 60 blocos): o Gojo fica no chão no encantamento, dispara até o alvo, dá o uppercut e os dois sobem 25 blocos. O Azul e o Vermelho saem das mãos em arcos e se fundem atrás do Gojo, e a explosão roxa cresce até o raio 40 engolindo os dois. O Gojo não leva dano e desce devagar; o alvo cai com dano de queda.
  - **R sem ninguém na mira:** as esferas vão até onde a mira bate e explodem lá.
  - **R em outro jogador (PvP):** ele fica preso no ar sem tranco e não é chutado por voo.
  - **R perto de parede:** o avanço para no bloco.
  - **Animações do R** em F5 e em 1ª pessoa: punho para trás no avanço, uppercut, mãos abrindo e punho estendido.
  - **Azul:** espirais, anéis se fechando, centro escuro, anel ondulando, poeira e folhas sugadas.
  - **Infinito:** brilho correndo, hexágonos, poeira lenta; flecha ou mob batendo faz ondas na bolha, e a flecha parada treme com halo.
  - Ver a TPS na explosão de raio 40 perto do chão (leva alguns segundos para apagar tudo).
  - **Menores da revisão que ficaram:**
    - com ping alto, o uppercut fere antes de o Gojo chegar na tela dele;
    - o GameTest `vermelhoCongelaEArremessaZumbi` falhou 2 vezes em 6 rodadas.
- Cutscene do Vazio Roxo máximo (09/11), conferir no jogo:
  - **Emenda:** o encantamento emenda na parte 2 sem a câmera voltar ao jogador no meio.
  - **Planos com alvo:** atrás do ombro no avanço (FOV abrindo), uppercut de baixo, subida de lado, esferas de frente, close com o núcleo atrás e o corte para o plano aberto de longe mostrando a explosão. Depois a câmera volta suave.
  - **Sem alvo:** a câmera segue as esferas até a mira e corta para o plano aberto.
  - **Outro jogador como alvo:** vê a cutscene a partir do uppercut e não fica preso nela. Testar com o Gojo morrendo ou trocando de personagem no meio.
  - **F5:** volta ao que estava.
  - **Animações:** os quatro gestos do encantamento, o Gojo flutuando e reagindo à explosão, e a pose de descida até pousar (na água também).
  - **Terreno:** ver se o plano aberto fica dentro de um morro em terreno alto.
  - **Menores da revisão que ficaram:**
    - a passagem do close no rosto (fim do encantamento) para atrás do ombro (avanço) é um corte, não um deslize;
    - se a parte 2 chegar com mais de 1 s de atraso, a câmera volta ao jogador e a cutscene recomeça;
    - um Gojo atingido pelo R de outro Gojo perde a própria cutscene;
    - com o dono fora do alcance de rastreamento, o alvo vê a câmera a partir dos olhos do início.
- VFX pesado e nova ordem do R (10/11), conferir no jogo:
  - **Encantamento:** sem esferas nas mãos; aura de fitas azuis e vermelhas subindo em espiral, cada vez mais forte, e os olhos brilhando.
  - **Corrida:** câmera baixa atrás do Gojo, perto do chão, com o FOV abrindo.
  - **Uppercut:** corte seco para o plano de lado e de baixo, e o **impact frame** (3 ticks em preto e branco com toque de roxo, piscando invertido) junto com o hit-stop. Se o shader não carregar, o log avisa "Impact frame desligado" e o golpe segue sem ele.
  - **No céu:** o Azul nasce na mão esquerda e o Vermelho na direita, voam para trás dele, dançam, encostam e fundem; silêncio, estouro e a explosão em camadas com bloom.
  - **Rosto do alvo:** a câmera nunca entra nele.
  - Ver o FPS na explosão (Photon e casca) e o `VFX reduzido`.
  - **Depois do segundo teste (10/11):** conferir se o Azul e o Vermelho vão para trás do Gojo lá no alto (antes desciam) e se a explosão agora tem peso: 3 s crescendo, miolo escuro, relâmpagos na casca, coluna de luz, três ondas de choque e o resto sumindo por 2,5 s. Se as esferas ainda descerem, a causa é outra e precisa de log no jogo.
  - **Depois do terceiro teste (10/11, "quase perfeito"):** a fumaça da cratera agora só sai quando a explosão termina de crescer; conferir que ela não cobre mais a casca.
  - Com teto baixo sobre o Gojo, a explosão sai um pouco acima dele (o centro vem da conta do movimento, não de onde ele parou).
- HUD (09/11): funcionou no jogo. Depois das correções da revisão, conferir: HUD acima da armadura e das bolhas de ar (mergulhar com armadura), texto "Fôlego" à esquerda da barra, barra sem subir de 0 ao entrar no mundo.
- Núcleo dos kits (09/11): escolha pelo menu, Z, X segurando e recarga **funcionaram**. Falta: morrer segurando X (não pode disparar), usar como espectador (nada acontece), ver a animação em outro jogador (2 clientes), sair e entrar mantendo o personagem.
- Tela de escolha (09/11): **funcionou no jogo** (autor). Conferido: mundo novo abre a tela com o `teste` (emblema genérico), golpes Z e X no painel, "Escolher" → confirmação. `/rwk tela 40` mostra a grade com rolagem pela roda do mouse e emblemas do Minezin; o 15º falso usa o genérico. Testar também com a janela estreita (painel embaixo) e com escala de interface grande.
- A tela de escolha pode abrir por cima da tela de carregamento do mundo: conferir.
- `/rwk anim` com `gojo_vermelho`, `gojo_infinito`, `gojo_roxo`, `gojo_roxo_200`, `gojo_vazio_infinito` (o `gojo_dominio` virou `gojo_vazio_infinito`) (o `gojo_azul` já foi confirmado em 09/11). `/rwk parar` interrompe.
- `/rwk som`: toca o som de teste e mostra a legenda.
- `/rwk fx <efeito>`: depois de criar um `.fx` no editor do Photon2, confirmar que aparece na posição do jogador; sem arquivo, deve responder "não encontrado".
