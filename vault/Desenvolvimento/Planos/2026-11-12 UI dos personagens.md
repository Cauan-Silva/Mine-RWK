# UI dos personagens: plano de implementação

> **Para agentes:** use superpowers:executing-plans (ou subagent-driven-development), tarefa por tarefa. Os passos usam `- [ ]`.

**Objetivo:** dar ao Gojo, ao Ichigo e ao Cid os seus próprios elementos de interface:
- ícones gerados em Python;
- uma HUD com o tema do personagem e a recarga em varredura;
- a árvore de habilidades com galhos.

**Arquitetura:** o tema (`TemaUi`), a forma da árvore (`FormaArvore`) e a geometria (`LogicaHud`, `LayoutArvore`) ficam em `core/hud`, sem classes do Minecraft e com JUnit. `HudKit` e `TelaArvore` em `client/kit` só desenham o que esses dados dizem. Os PNGs saem de `ferramentas/gerar_icones.py`.

**Tecnologia:** NeoForge 21.1.265, MC 1.21.1, Java 21, JUnit 5, Python 3 com Pillow e numpy.

**Spec:** [[2026-11-12 UI dos personagens]] (`vault/Desenvolvimento/Specs/2026-11-12 UI dos personagens.md`).

## Restrições gerais
- **Separação de pacotes:** código de `core/` não importa `net.minecraft.*`. Código comum nunca importa `client/`.
- **As regras de compra da maestria não mudam.** `Arvore.podeComprar` e `ComprarNo` ficam intocados.
- **Assets gerados não se editam à mão:**
  - os ícones ficam em `src/main/resources/assets/minerwk/textures/gui/icones/`, todos com 32×32;
  - os fundos ficam em `.../textures/gui/fundos/`.
- **Tamanhos na tela:** os ícones são desenhados em 20×20 na HUD e em 24×24 na árvore.
- **Fallbacks:**
  - golpe sem ícone mostra a letra da tecla;
  - personagem sem tema usa `Temas.GENERICO`, o cinza de hoje.
- **Comandos:**
  - nunca rodar dois processos do Gradle ao mesmo tempo;
  - JUnit: `./gradlew test --tests <classe>`;
  - fim de parte: `./gradlew build`, `./gradlew runGameTestServer` ("All N required tests passed" no `run/logs/latest.log`) e `./gradlew runClientDemo -Pdemo=<modo>`.
- **Texto:** commits, comentários e notas em português. Os commits terminam com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Fim de cada parte:** marcar no Roadmap, escrever o diário (com o índice em `Diário.md`) e anotar em Pendências o que falta testar no jogo.

## Onde o olho do revisor deve ir
1. **GUI em escala grande (largura 320, altura 240):** a árvore do Cid (4 galhos antes, 3 depois) cabe na largura sem sobrepor nada, a tela rola, e o painel de detalhe sai do lado e vira caixa junto ao mouse. *Teste na Tarefa 6.*
2. **Relógio do cliente atrasado:** a fração da recarga pode chegar fora de [0, 1]. A varredura limita o valor e nunca desenha mais que o quadro inteiro. *Teste na Tarefa 2.*
3. **Brilho no login ou na troca de personagem:** golpes que já estavam prontos não piscam. O brilho só acontece quando o cliente viu a recarga terminar. *Teste na Tarefa 2 (`Pronto`).*
4. **Ichigo entrando e saindo do Hollow:** o tema troca com a forma e volta ao normal ao sair. Forma desconhecida usa o tema base do personagem. *Teste na Tarefa 1.*
5. **Golpe novo sem PNG:** o `IconesTest` falha no build. Em jogo, a HUD cai para a letra. *Teste na Tarefa 3. O fallback da Tarefa 4 se confere pela prévia.*

---

## Parte 1: ícones, temas e HUD

### Tarefa 1: tema por personagem e por forma

**Arquivos:**
- Criar: `src/main/java/com/minerwk/core/hud/TemaUi.java` e `Temas.java`.
- Teste: `src/test/java/com/minerwk/core/hud/TemasTest.java`.

**Interfaces:**
- Produz:
  - `record TemaUi(int corPrincipal, int corSecundaria, int corFundo, int corBorda, Moldura moldura, String fundoArvore)`, com `enum Moldura { LISA, CORTADA, ORNADA }` aninhado. As cores são ARGB.
  - `Temas.tema(String personagem, String forma) -> TemaUi`.
  - `Temas.GENERICO`.
  - `Temas.temTema(String personagem) -> boolean`.

- [ ] **Teste:** `TemasTest` deve garantir:
  - `Temas.tema("gojo", Forma.BASE)` tem moldura `LISA` e `fundoArvore` `"gojo"`;
  - Ichigo: `tema("ichigo", BASE)` tem moldura `CORTADA`, e o Bankai tem o mesmo tema da base;
  - `tema("ichigo", <id da forma Hollow em Ichigo.java>)` tem `corPrincipal` diferente da base e `fundoArvore` `"ichigo_hollow"`;
  - `tema("cid", BASE)` tem moldura `ORNADA`;
  - `tema("asta", BASE) == Temas.GENERICO`;
  - forma desconhecida (`tema("ichigo", "xyz")`) devolve o tema base do Ichigo;
  - para cada personagem de `Gojo.criar()`, `Ichigo.criar()` e `Cid.criar()`, `temTema(p.id())` é verdadeiro.
- [ ] **Rodar** `./gradlew test --tests com.minerwk.core.hud.TemasTest`. Esperado: falha de compilação (a classe não existe).
- [ ] **Implementar.** As cores vêm da tabela do spec:

  | Tema | Principal | Secundária | Fundo | Borda |
  |---|---|---|---|---|
  | Gojo | `0xFF7FD8FF` | `0xFFFFFFFF` | `0xC0101828` | `0xFFB48CFF` |
  | Ichigo | `0xFFE03030` | `0xFF1A1A1A` | `0xC0100808` | `0xFF802020` |
  | Ichigo Hollow | `0xFFF0EDE0` | `0xFFE03030` | `0xC0180808` | `0xFFB02020` |
  | Cid | `0xFF9A6BFF` | `0xFFE8B84A` | `0xC00C0818` | `0xFF5A3A9A` |
  | Genérico | `0xFFAAAAAA` | `0xFF555555` | `0xAA000000` | `0xFF555555` |

  O `fundoArvore` do genérico é `"generico"`.
- [ ] **Rodar de novo.** Esperado: PASS.
- [ ] **Commit:** "UI parte 1: temas por personagem e forma".

### Tarefa 2: varredura da recarga e brilho de pronto

**Arquivos:**
- Modificar: `src/main/java/com/minerwk/core/hud/LogicaHud.java`.
- Criar: `src/main/java/com/minerwk/core/hud/Pronto.java`.
- Teste: `LogicaHudTest.java`, que já existe, e `ProntoTest.java`, novo.

**Interfaces:**
- Produz:
  - `LogicaHud.varredura(float restante, int lado) -> float[]`: o polígono escuro como pares `x, y`, relativos ao canto do quadro. O primeiro ponto é o centro. A varredura começa no topo e o escuro cobre o fim do giro horário: o claro cresce no sentido horário a partir do topo. `restante <= 0` devolve um array vazio; `restante` acima de 1 vira 1.
  - `LogicaHud.brilho(long ticksDesdePronta) -> float`: 1 no tick 0, cai em linha reta até 0 em `LogicaHud.BRILHO_TICKS = 8`, e é 0 depois disso e para valores negativos.
  - `Pronto` (por cliente) com `void ver(String id, boolean emRecarga, long agora)` e `long prontaEm(String id)`. `prontaEm` devolve -1 enquanto o cliente não viu a transição. `limpar()` zera tudo, para a troca de personagem.

- [ ] **Teste:** a área é calculada com a fórmula do laço (shoelace).
  - `varreduraVaziaSemRecarga`: `varredura(0, 20).length == 0`.
  - `varreduraCheia`: a área de `varredura(1, 20)` é `400 ± 0.5`.
  - `varreduraMetade`: a área de `varredura(.5f, 20)` é `200 ± 0.5`, e todos os pontos têm `x <= 10.001` (o escuro fica na metade esquerda).
  - `varreduraLimitada`: `varredura(1.7f, 20)` tem a mesma área de `varredura(1, 20)`.
  - `brilho`: `brilho(0) == 1`, `brilho(4) == .5 ± 1e-6`, `brilho(8) == 0`, `brilho(-1) == 0`.
  - `ProntoTest.naoPiscaNoLogin`: `ver("a", false, 100)` e depois `prontaEm("a") == -1`.
  - `ProntoTest.piscaNaTransicao`: `ver("a", true, 100)`, `ver("a", false, 140)` e depois `prontaEm("a") == 140`.
  - `ProntoTest.limpar`: depois de `limpar()`, `prontaEm == -1`.
- [ ] **Rodar** `./gradlew test --tests "com.minerwk.core.hud.*"`. Esperado: falha de compilação.
- [ ] **Implementar.** A varredura percorre o perímetro do quadrado: o setor vai do centro até o ponto do topo no ângulo `360·(1−restante)` e segue pelos cantos até voltar ao topo. Inclua os cantos que ficam dentro do setor.
- [ ] **Rodar de novo.** Esperado: PASS.
- [ ] **Commit:** "UI parte 1: varredura e brilho da recarga".

### Tarefa 3: gerador de ícones e fundos

**Arquivos:**
- Criar: `ferramentas/gerar_icones.py`. Se passar de cerca de 600 linhas, separe os fundos em `gerar_ui.py`.
- Gerar:
  - `textures/gui/icones/*.png`;
  - `textures/gui/fundos/{gojo,ichigo,ichigo_hollow,cid,generico}.png`, com 256×256, desenhados repetidos (tile).
- Teste: `src/test/java/com/minerwk/core/hud/IconesTest.java`.

**Interfaces:**
- Produz os nomes de arquivo:
  - `<id da habilidade>.png`, para todo `h.id()` de `p.todasAsHabilidades()` do Gojo, do Ichigo e do Cid;
  - `<personagem>_recurso.png`;
  - `<id do nó Despertar>.png`;
  - `selo_{forca,recarga,carga,duracao,recurso}.png`, com 32×32 e o símbolo num canto de 12×12 (o resto transparente).

- [ ] **Teste:** `IconesTest`, no estilo do `GravacoesCc0Test` (lendo `src/main/resources/...`), deve garantir:
  - para cada personagem dos três `criar()`, cada id de habilidade, `<id>_recurso` e o nó `DESPERTAR` da árvore tem um PNG de 32×32 (leia com `javax.imageio.ImageIO`);
  - os cinco selos existem;
  - os cinco fundos existem e têm 256×256;
  - mensagem de falha: "falta o ícone <nome>: rode ferramentas/gerar_icones.py".
- [ ] **Rodar** `./gradlew test --tests com.minerwk.core.hud.IconesTest`. Esperado: FAIL listando os ícones que faltam.
- [ ] **Escrever o gerador.**
  - **Desenho:** pixel art em 32×32, com contorno escuro de 1 px, o símbolo do golpe no centro e as cores do tema (as mesmas da Tarefa 1).
  - **Símbolos:** um por id, e as variações de forma podem compartilhar o desenho com outra cor. Exemplos do spec: o Azul é uma esfera azul com redemoinho; o Getsuga, o arco de energia; a Prisão, correntes.
  - **Determinismo:** o gerador falha com a lista dos ids sem desenho, para que um id esquecido não vire um PNG vazio.
  
  *Decisão:* o spec pede que a lista de ícones saia das definições dos personagens. Como o Python não lê o Java, o gerador tem um desenho por id e o `IconesTest` garante que a lista está completa.
- [ ] **Rodar** `python ferramentas/gerar_icones.py` e depois o teste. Esperado: PASS.
- [ ] **Montar** uma folha de contato em `previas/icones.png` (todos os ícones em 4×, com o nome embaixo) e olhar antes de seguir. Refazer os que não lerem como o golpe.
- [ ] **Commit:** "UI parte 1: gerador de ícones e fundos".

### Tarefa 4: HUD com tema, prévia e fechamento da parte 1

**Arquivos:**
- Modificar:
  - `src/main/java/com/minerwk/client/kit/HudKit.java`;
  - `src/main/java/com/minerwk/client/vfx/DemoVfx.java` (modo `hud`).
- Vault: Roadmap, diário `2026-11-12 UI parte 1.md`, `Diário.md` e Pendências.

**Interfaces:**
- Consome:
  - `Temas.tema(s.personagem(), s.forma())`;
  - `LogicaHud.varredura`, `LogicaHud.brilho` e `Pronto`;
  - as texturas `minerwk:textures/gui/icones/<id>.png`.

- [ ] **Quadros:**
  - `LADO = 20`, com a moldura do tema;
  - **`LISA`:** borda de 1 px;
  - **`CORTADA`:** borda de 1 px sem os pixels dos cantos, mais um risco diagonal de 2 px no canto superior direito;
  - **`ORNADA`:** borda de 1 px mais um L de 3 px na cor secundária em cada canto.
- [ ] **Ícone e estados:**
  - ícone 20×20 com `g.blit(textura, x, y, 0, 0, 20, 20, 20, 20)`, depois de conferir com `mc.getResourceManager().getResource(textura).isPresent()` (guarde o resultado num cache por id). Sem ícone, a letra, como hoje;
  - letra da tecla em escala 0,5 no canto inferior direito;
  - **recarga:** o polígono de `varredura` desenhado em triângulos (leque a partir do centro) com `SOMBRA_RECARGA`, por meio do `VertexConsumer` de `RenderType.gui()`, e os segundos no centro;
  - **brilho:** um quadro na `corPrincipal` com alfa `brilho(agora − pronto.prontaEm(id)) × 0xA0`, por cima do ícone;
  - **em uso:** a borda em `corPrincipal`;
  - **sem recurso:** um véu cinza `0x80404040` sobre o ícone e o custo em vermelho no lugar dos segundos;
  - **trancado:** o ícone, o véu escuro e o cadeado de hoje.
- [ ] **Chamar** `Pronto.ver` para cada habilidade no `aoTick`, e `Pronto.limpar()` na troca de personagem, onde hoje se zera `recursoMostrado`.
- [ ] **Barra de recurso:**
  - o ícone `<personagem>_recurso` em 9×9 à esquerda;
  - o preenchimento com `fillGradient` da cor principal para a secundária, com o nível e o XP como hoje;
  - o texto vira só `80/100`.
- [ ] **Prévia:** criar o modo `hud` no `DemoVfx`. Ele faz Gojo, depois Ichigo no Bankai, depois Ichigo no Hollow e depois Cid. Para cada um:
  - usa um golpe para mostrar a varredura;
  - espera a recarga acabar, para mostrar o brilho;
  - mostra um golpe trancado (o Cid antes do Despertar já tem).
- [ ] **Verificar**, nesta ordem:
  - `./gradlew build`, verde;
  - `./gradlew runGameTestServer`, com "All N required tests passed";
  - `./gradlew runClientDemo -Pdemo=hud`, olhando os quadros em `run/demo/` e montando `previas/ui_parte1_hud.mp4`.
- [ ] **Revisão** de leitura por um subagente sonnet nos arquivos desta parte. Corrigir o que for Crítico ou Importante.
- [ ] **Vault e commit:**
  - no Roadmap, a parte 1 feita;
  - o diário;
  - em Pendências: "ver a HUD nova no jogo em escala de GUI 2, 3 e 4, e no Hollow";
  - commit "UI parte 1: HUD com tema, ícones e recarga em varredura".

## Parte 2: a árvore com galhos

### Tarefa 5: a forma da árvore

**Arquivos:**
- Criar: `src/main/java/com/minerwk/core/hud/FormaArvore.java` e `FormasArvore.java`.
- Modificar: `ArvoreGojo.java`, `ArvoreIchigo.java` e `ArvoreCid.java`, cada um com um `public static final FormaArvore FORMA`.
- Teste: `src/test/java/com/minerwk/core/hud/FormaArvoreTest.java`.

**Interfaces:**
- Produz:
  - `record FormaArvore(List<Galho> galhos)` com:
    - `record Galho(String id, String icone, boolean depoisDoDespertar, List<String> nos, Map<String, Selo> selos)`;
    - `enum Selo { FORCA, RECARGA, CARGA, DURACAO, RECURSO }`;
    - `List<Galho> antes()` e `List<Galho> depois()`;
    - `Selo selo(No no)`, que usa o mapa do galho e, se o nó não estiver nele, o tipo do nó: `MELHORIA` vira `FORCA`, `RECARGA` vira `RECARGA`, `CARGA` vira `CARGA`.
  - `FormasArvore.de(String personagem) -> Optional<FormaArvore>`.
  - O campo `icone` é o id de um PNG da Tarefa 3: o id do golpe ou `<personagem>_recurso`.

- [ ] **Teste:** `FormaArvoreTest`, parametrizado pelos três personagens, deve garantir:
  - todo nó da `p.arvore()`, menos o Despertar, aparece em exatamente um galho;
  - nenhum galho cita um nó que não existe;
  - um galho de antes só tem nós com `depoisDoDespertar == false`, e o contrário para os de depois;
  - todo `icone` tem um PNG;
  - `selo` de `cid_prisao_duracao` é `DURACAO` e `selo` de `cid_magia_reserva` é `RECURSO`;
  - `selo` de `cid_redemoinho_recarga` é `RECARGA`, pelo tipo.
- [ ] **Rodar** `./gradlew test --tests com.minerwk.core.hud.FormaArvoreTest`. Esperado: falha de compilação.
- [ ] **Implementar.** Os galhos são os da tabela do spec, na mesma ordem.
  - **Selos de duração:** os nós de duração. No Gojo é o Infinito; no Ichigo, a lâmina e a máscara; no Cid, a Prisão.
  - **Selos de recurso:** os nós de reserva e fluxo, e o fôlego do Bankai.
  - **Técnica reversa do Gojo:** selo `FORCA`, pelo tipo.
  - **Ícones dos galhos:** o golpe principal de cada galho. O galho de recurso usa `<personagem>_recurso`, e o Domínio usa `gojo_vazio_infinito`.
- [ ] **Rodar de novo.** Esperado: PASS.
- [ ] **Commit:** "UI parte 2: forma da árvore dos três personagens".

### Tarefa 6: layout da árvore com galhos

**Arquivos:**
- Reescrever: `src/main/java/com/minerwk/core/hud/LayoutArvore.java`.
- Teste: `LayoutArvoreTest.java`, que já existe e será reescrito.

**Interfaces:**
- Consome: `FormaArvore`, com os galhos de antes e de depois e quantos nós cada um tem.
- Produz:
  - `LayoutArvore(int largura, int altura, List<Integer> nosAntes, List<Integer> nosDepois)`;
  - `Retangulo raiz()`, `Retangulo despertar()`, `Retangulo ponta(int galho)` e `Retangulo no(int galho, int i)`, com os galhos numerados de 0 em diante, os de antes primeiro;
  - `List<Linha> linhas()`, com `record Linha(int x1, int y1, int x2, int y2, int galho)`, onde `galho = -1` marca o tronco;
  - `Optional<Retangulo> painel()`;
  - `int rolagemMaxima()`;
  - `Alvo alvoEm(double x, double y, int rolagem)`, com `record Alvo(int galho, int no)`. `no = -1` é a ponta; `galho = -2` é o Despertar; `null` quer dizer nada.
- **Geometria:**
  - **Constantes:** `NO = 24`, `PASSO_X = 30`, `PASSO_Y = 36`, `TOPO = 42` (o cabeçalho), `MARGEM = 10` e `PAINEL_L = 120`.
  - **Tronco:** vertical em `cx`, que é o centro da área livre (a largura menos o painel, quando ele cabe).
  - **Raiz:** embaixo.
  - **Galhos de antes:** de baixo para cima, alternando esquerda e direita, dois por altura.
  - **Despertar:** no tronco, `PASSO_Y` acima da última altura dos galhos de antes.
  - **Galhos de depois:** acima do Despertar, com a mesma regra.
  - **Cada galho:** a ponta a `PASSO_X` do tronco e os nós seguindo para fora, de `PASSO_X` em `PASSO_X`.
  - **Painel:** à direita, se `largura >= 2·alcance + NO + PAINEL_L + 3·MARGEM`, onde `alcance = PASSO_X·(1 + nós do maior galho)`. Se não couber, `painel()` vem vazio e a tela desenha o detalhe junto ao mouse.
  - **Rolagem:** o conteúdo cresce para cima. A posição inicial de rolagem é `rolagemMaxima()`, que mostra a raiz.

- [ ] **Teste:** `LayoutArvoreTest`, com as formas do Cid (`[2,1,1,2]` e `[2,1,1]`) e do Ichigo (`[2,1,3]` e `[2,2,2]`), em 320×240 e em 854×480, deve garantir:
  - não há dois retângulos (raiz, Despertar, pontas e nós) que se sobreponham;
  - tudo fica em `[MARGEM, largura − MARGEM]` na horizontal;
  - `linhas().size() == galhos + soma dos nós + 2`;
  - `alvoEm` no centro de `no(1, 0)`, com `rolagem = 17`, devolve `Alvo(1, 0)` (o y testado já descontando a rolagem);
  - `alvoEm` no centro do Despertar devolve `galho == -2`;
  - `alvoEm(0, 0, 0) == null`;
  - em 320×240, `painel()` vem vazio e `rolagemMaxima() > 0`;
  - em 854×480, `painel()` existe e não se sobrepõe a nenhum nó.
- [ ] **Rodar** `./gradlew test --tests com.minerwk.core.hud.LayoutArvoreTest`. Esperado: FAIL ou falha de compilação.
- [ ] **Implementar.**
- [ ] **Rodar de novo.** Esperado: PASS.
- [ ] **Commit:** "UI parte 2: layout da árvore com galhos".

### Tarefa 7: tela da árvore, prévia e fechamento da parte 2

**Arquivos:**
- Modificar:
  - `src/main/java/com/minerwk/client/kit/TelaArvore.java`;
  - `DemoVfx.java` (o modo `arvore` passa a mostrar os três personagens).
- Vault: Roadmap, diário `2026-11-12 UI parte 2.md`, `Diário.md` e Pendências.

**Interfaces:**
- Consome: `FormasArvore.de`, `Temas.tema`, `LayoutArvore` e `FormaArvore.selo`.

- [ ] **Abertura:** a tela monta o layout a partir de `FormasArvore.de(personagem.id())`. `abrir()` só abre se houver forma, trocando a checagem de hoje pela do Despertar.
- [ ] **Fundo e linhas:**
  - o fundo é a textura `fundos/<tema.fundoArvore()>.png` repetida, com `corFundo` por cima;
  - as linhas têm 2 px: na `corPrincipal` se o nó de destino tem nível > 0, em `0xFF4A4A55` se não;
  - o tronco acende acima do Despertar só depois de despertar.
- [ ] **Raiz:** o emblema de `textures/gui/personagens/<id>.png`.
- [ ] **Pontas:** o ícone do galho.
- [ ] **Nós:**
  - o ícone do galho com o selo por cima, em 24×24, e a moldura do tema (reaproveitar o desenho da moldura da Tarefa 4 numa classe `client/kit/Molduras`);
  - abaixo, os pontos de nível (3 pontos de 2 px, cheios na `corPrincipal`);
  - `Arvore.Resultado` que pode comprar: a borda pisca em `0xFFE8B84A` (seno do tempo);
  - nó trancado: véu `0xA0000000`;
  - galho de depois sem despertar: véu e cadeado no Despertar.
- [ ] **Detalhe e clique:**
  - o detalhe aparece no `painel()`, ou junto ao mouse quando não há painel, com os textos de tradução que a tela já usa (nome, efeito, custo, falta);
  - o clique em `alvoEm` manda `ComprarNo` como hoje.
- [ ] **Prévia:** o modo `arvore` mostra Gojo, Ichigo e Cid, cada um em três momentos (sem pontos, com compras e desperto). Incluir uma passada em escala de GUI 4, para o caso sem painel.
- [ ] **Verificar**, nesta ordem:
  - `./gradlew build`, verde;
  - `./gradlew runGameTestServer`, com "All N required tests passed";
  - `./gradlew runClientDemo -Pdemo=arvore`, montando `previas/ui_parte2_arvore.mp4`.
- [ ] **Revisão** de leitura por um subagente sonnet. Corrigir o que for Crítico ou Importante.
- [ ] **Vault e commit:**
  - no Roadmap, a parte 2 e o spec feitos;
  - o diário;
  - em Pendências: "ver a árvore nova no jogo nas três escalas e comprar nós";
  - commit "UI parte 2: árvore com galhos e tema".
