# Oriana sob ataque: plano

> **Para quem executa:** execução nativa (superpowers:executing-plans), tarefa por tarefa, com TDD. Passos com `- [ ]`.

**Objetivo:** a arena do Ragnarok vira a capital de Oriana sob ataque: muralha rachada, campo queimado com fogo e destroços, a cidade em chamas no horizonte e uma noite avermelhada no céu.

**Arquitetura:**
- **A planta:** `core/arena/PlantaOriana` ganha os detalhes em blocos. O enum `PlantaEscola.Material` ganha materiais novos, e `arena/ObrasArena.estado` os traduz.
- **O horizonte:** o da Escola (`client/arena/HorizonteCliente` + `core/arena/AnelHorizonte`) passa a escolher as faixas pelo cenário.
- **Novo no cliente:** um gerador Python para as faixas de Oriana, uma cúpula de noite e as partículas de fumaça.

**Ferramentas:** Java 21, NeoForge 21.1.265, JUnit, GameTests, Python + Pillow.

**Spec:** [[2026-11-13 Oriana sob ataque]] (`vault/Desenvolvimento/Specs/`).

## Restrições globais
- **O meio livre:** de −30 a +30 em dx e dz, de `dy` 0 até `RagnarokBoss.ALTURA + ALTURA_DO_VOO`, sem nada. O chão (`dy` −1) inteiro com bloco. As entradas não mudam: o jogador em (0, −1, 16) na estrada, o boss em −8.
- **A caixa:** nada fora de ±`CaixaArena.MEIA` nem da faixa de altura. No máximo 60 000 blocos. Nenhuma posição repetida.
- **O portal:** no mesmo lugar e acima das torres.
- **Ninguém sai da caixa:** na faixa da muralha (dz de −48 a −46), toda coluna tem bloco de `dy` 0 a 3.
- **O fogo:** todo `FOGO` tem `NETHERRACK` embaixo, e nenhum material que pega fogo a menos de 3 na horizontal (Chebyshev) e de 5 na vertical.
  - **Materiais que pegam fogo:** `MADEIRA_ESCURA`, `TRONCO`, `FOLHA`, `FENO`, `LA_VERMELHA`, `CERCA_ESCURA`, `BARRIL`, `TRONCO_QUEIMADO`.
- **Lados:** o cliente não entra no comum. Nada de `client/` fora de `client/`.
- **Ao fim de cada tarefa:** `./gradlew build` verde. Nas tarefas que mexem em servidor ou arena, também `./gradlew runGameTestServer` com "All N required tests passed". Nunca dois Gradle ao mesmo tempo, e no Bash com `dangerouslyDisableSandbox: true`.
- **Assets gerados** (as faixas) só pelo gerador.
- **Commits:** em português, terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Foco da revisão
1. **O fogo da vanilla não pode comer a arena nem crescer:** coberto pelo teste de distância e pelo GameTest `orianaNaoPerdeBlocosComOFogo`.
2. **Uma brecha ou o portão não podem virar saída:** coberto por `aMuralhaNaoDeixaPassar`.
3. **O Ragnarok voando e a investida** não batem em nada novo: o teste do meio livre continua, e as crateras são só chão.
4. **O horizonte e a noite de Oriana** não aparecem na Escola, no mundo interior nem fora das arenas, e vice-versa. Isso se confere na prévia e em Pendências.
5. **Desempenho:** no máximo 60 000 blocos, e as partículas com teto por tick e metade no VFX reduzido.

---

### Tarefa 1: materiais e a muralha destruída

**Arquivos:**
- Mudar: `core/arena/PlantaEscola.java` (enum `Material`), `arena/ObrasArena.java` (`estado`), `core/arena/PlantaOriana.java`.
- Teste: `src/test/java/com/minerwk/core/arena/PlantaOrianaTest.java`.

**Interfaces que nascem aqui:**
- **Materiais novos:** `NETHERRACK`, `FOGO`, `FOGUEIRA`, `TERRA_QUEIMADA`, `PEDRA_NEGRA`, `BRASA`, `TIJOLO_RACHADO`, `TIJOLO_MUSGO`, `LA_VERMELHA`, `TRONCO_QUEIMADO`.
- **Os blocos de cada um:** netherrack; fogo (`Blocks.FIRE`); fogueira acesa; terra grossa; blackstone; magma; tijolo de pedra rachado; tijolo de pedra com musgo; lã vermelha; tronco de carvalho escuro.
- **Em `PlantaOriana`:**
  - `BRECHAS = {-32, 30}` (o meio de cada brecha em dx);
  - `MEIA_BRECHA = 3`;
  - `FUNDO_DA_BRECHA = 4` (a brecha desce do topo até `dy` 4);
  - `TORRE_DESTRUIDA = -20`;
  - `public static final java.util.Set<Material> INFLAMAVEIS`.

- [ ] **Passo 1: os testes que falham** em `PlantaOrianaTest`:
  - **`aMuralhaNaoDeixaPassar`:** para todo dx em ±MEIA e dz de `MURALHA_DE` a `MURALHA_ATE`, há bloco em `dy` 0, 1, 2 e 3.
  - **`aMuralhaTemBrechasEmV`:** para cada x de `BRECHAS`, não há bloco em (x, `MURALHA_TOPO`, `MURALHA_ATE`), e há bloco em (x, `FUNDO_DA_BRECHA` − 1, `MURALHA_ATE`).
  - **`aMuralhaEstaRachada`:** entre 15% e 45% dos blocos da muralha são `TIJOLO_RACHADO` ou `TIJOLO_MUSGO`, e há pelo menos 20 `PEDRA_NEGRA` na face de dentro (dz = `MURALHA_ATE`).
  - **`aTorreDestruidaPerdeuOTelhado`:** nenhuma `TELHA` em volta de x = `TORRE_DESTRUIDA`; há `FOGO` em cima dela. As outras três torres têm `LA_VERMELHA` pendurada.
  - **`oPortaoArrombadoContinuaFechado`:**
    - há `MADEIRA_ESCURA` no chão à frente do vão (`dy` 0, dz = `MURALHA_ATE` + 1 a + 3, |dx| ≤ 2);
    - para |dx| ≤ `MEIA_ESTRADA` e `dy` de 0 a 3, há bloco em dz = `MURALHA_ATE` − 1 (a barricada).
  - Mudar `aMuralhaFechaONorteEOPortalFicaNoCeu`:
    - a "sem brecha" só vale em `dy` 3, e não mais em 6;
    - o portão deixa de exigir `MADEIRA_ESCURA` em `0,2`: passa a exigir um bloco qualquer lá.
- [ ] **Passo 2:** `./gradlew test --tests "*PlantaOrianaTest*"`. Esperado: falha de compilação ou de asserção.
- [ ] **Passo 3:** implementar:
  - os materiais e o `estado`;
  - em `muralha`, o rachado e o musgo por um acaso determinístico da posição (como as outras plantas fazem), o queimado na face de dentro, as ameias faltando, as brechas em V e o entulho no pé delas (pedra e cascalho até 3 de altura, do lado de dentro);
  - o portão arrombado: uma folha deitada à frente e a barricada atrás do vão;
  - a torre destruída: sem telhado, com a borda irregular e `NETHERRACK` + `FOGO` em cima;
  - os estandartes de `LA_VERMELHA` (colunas de 3 a 6, rasgadas) nas outras.
  - `INFLAMAVEIS` como nas restrições globais.
- [ ] **Passo 4:** `./gradlew build` verde. Os testes antigos de `PlantaOrianaTest` continuam passando.
- [ ] **Passo 5:** commit "Oriana: a muralha rachada, as brechas, o portão arrombado e a torre em chamas".

### Tarefa 2: o campo queimado, o fogo e os destroços

**Arquivos:** mudar `core/arena/PlantaOriana.java`, `PlantaOrianaTest.java` e `arena/TestesArena.java` (ou o arquivo de GameTests do Ragnarok, onde a arena de Oriana já é usada).

**Interfaces:** usa `INFLAMAVEIS` e os materiais da Tarefa 1.

- [ ] **Passo 1: os testes que falham:**
  - **`oFogoNaoSeEspalha`:**
    - todo `FOGO` tem `NETHERRACK` logo embaixo;
    - nenhum `INFLAMAVEIS` com |Δx| ≤ 3, |Δz| ≤ 3 e −1 ≤ Δy ≤ 5 de um `FOGO`;
    - há de 8 a 16 focos de `FOGO` no campo (fora da muralha e das torres).
  - **`oCampoTemCraterasEDestrocos`:**
    - pelo menos 4 `BRASA` em `dy` −1;
    - pelo menos 60 `TERRA_QUEIMADA` ou `PEDRA_NEGRA` em `dy` −1;
    - pelo menos 2 `FOGUEIRA`;
    - `FENO` e `BARRIL` fora do meio livre;
    - pelo menos 6 colunas de `TRONCO_QUEIMADO` sem `FOLHA` em volta, e ainda 2 árvores com `FOLHA` ao sul (dz > 0).
  - **GameTest `orianaNaoPerdeBlocosComOFogo`:** montar a planta de Oriana numa vaga como o `escolaMontaInteiraSemVazarAgua` faz com a Escola, esperar 200 ticks e conferir que todo bloco da planta continua no lugar (o `FOGO` e a `FOGUEIRA` como estão; o resto exatamente igual).
- [ ] **Passo 2:** `./gradlew test --tests "*PlantaOrianaTest*"`. Esperado: falha.
- [ ] **Passo 3:** implementar em `PlantaOriana.cenario()`:
  - **as crateras:** de 4 a 6, de raio 2 a 4, com miolo de `BRASA`, anel de `PEDRA_NEGRA` e borda de `TERRA_QUEIMADA`, só em `dy` −1. As das beiradas têm a borda erguida em `dy` 0.
  - **as manchas** de chão queimado e as falhas da estrada (`CASCALHO`, `TERRA_QUEIMADA`).
  - **os focos de fogo** em `NETHERRACK` na faixa de beirada (|dx| ou |dz| > 32) e as `FOGUEIRA` perto dos destroços, respeitando a distância.
  - **duas ou três carroças viradas** (`MADEIRA_ESCURA`, `CERCA_ESCURA` como roda, `FENO`, `BARRIL`) e as barricadas perto do portão.
  - **as pedras caídas** até 6 blocos da muralha.
  - **as árvores queimadas** com `TRONCO_QUEIMADO` (tronco mais galhos curtos, sem folha), mantendo duas com folha ao sul.
  - **um poste tombado:** o muro deitado, com a lanterna no chão.
- [ ] **Passo 4:** `./gradlew build` e `./gradlew runGameTestServer` verdes. Depois a prévia `previa_arena.py`, se ela desenha Oriana, ou um quadro da prévia `ragnarok` para conferir de olho.
- [ ] **Passo 5:** commit "Oriana: crateras, fogo, destroços e árvores queimadas".

### Tarefa 3: a cidade em chamas no horizonte e a noite

**Arquivos:**
- Criar `ferramentas/gerar_horizonte_oriana.py` e as faixas `assets/minerwk/textures/ceu/horizonte_oriana_<0..3>.png`.
- Criar `client/arena/NoiteOriana.java` e `client/arena/FumacaOriana.java`.
- Mudar `client/arena/HorizonteCliente.java` e o registro dos ouvintes em `client/MineRWKClient`.
- Mudar `client/vfx/DemoVfx.java`, com o modo novo `oriana`.

**Interfaces:**
- Usa `CenarioArena.ORIANA`, `CenarioCliente.atual()`, `AnelHorizonte`, `ConfigCliente.vfxReduzido()` e `HorizonteCliente.reduzidoNaPrevia`.
- **O horizonte por cenário:** `HorizonteCliente` guarda um `Map<String, ResourceLocation[]>` (`escola` → `horizonte_escola_<n>`, `oriana` → `horizonte_oriana_<n>`) e desenha as faixas do cenário atual. A cor da saia (`CHAO`) também passa a ser por cenário, e a de Oriana é a do pé escuro da faixa.

- [ ] **Passo 1: o gerador.**
  - **O formato:** o mesmo da Escola. Quatro faixas de 1024 × 768, três linhas de 256 (a de cima é a mais longe), o pátio na altura 171 de cada linha, a faixa 0 centrada no sul e as seguintes para oeste, norte e leste. O panorama inteiro é feito com ruído periódico e cortado em quatro.
  - **Ao norte (a faixa 2), a capital:**
    - telhados apinhados em silhueta;
    - o castelo com três torres no meio;
    - incêndios em laranja e amarelo, com brilho que sobe;
    - colunas de fumaça escura.
  - **Nas outras faixas:** morros escuros com algumas vilas pequenas pegando fogo.
  - **O céu da textura** é transparente acima das silhuetas, como na Escola.
  - **O gerador confere a emenda** (diferença média < 8, como o da Escola).
- [ ] **Passo 2:** rodar o gerador e conferir os quatro PNGs de olho.
- [ ] **Passo 3: o `HorizonteCliente` por cenário,** com o mapa acima. A Escola continua igual.
- [ ] **Passo 4: `NoiteOriana`.**
  - **Quando:** no `RenderLevelStageEvent` `AFTER_SKY`, com o cenário `oriana` e na dimensão das arenas, antes do horizonte.
  - **A cúpula:** uma esfera grande centrada na câmera, com degradê do zênite `#0B0712` até o horizonte `#3A1410`, umas 300 estrelas tênues fixas e um brilho laranja baixo, centrado no norte.
  - **A névoa:** no `ViewportEvent.ComputeFogColor`, a mesma cor do horizonte.
  - **A rotação:** como no horizonte, a da câmera vai em cada vértice.
  - **A ordem:** registrar a noite antes do horizonte, para o anel ficar por cima.
- [ ] **Passo 5: `FumacaOriana`.**
  - **O que solta:** a cada tick, com o cenário `oriana`, umas 6 partículas em pontos fixos atrás da muralha (dz de −49 a −50 em relação ao centro da arena, a altura da muralha + 2) e sobre a torre destruída: `CAMPFIRE_COSY_SMOKE` e fagulhas (`LAVA` ou `FLAME` subindo).
  - **O VFX reduzido** solta a metade.
  - **O centro da arena** vem do cliente: o ponto em que o jogador entrou. Se o cliente não sabe o centro, use a vaga mais perto (`CaixaArena`).
- [ ] **Passo 6: a prévia `oriana`.**
  - **Igual à da Escola:** o jogador entra na luta e vira espectador, e o boss fica invisível.
  - **A câmera:** olha o portão, uma brecha, a torre em chamas, o campo do sul para o norte, o céu e o mesmo com o VFX reduzido.
  - **Gravar:** `./gradlew runClientDemo -Pdemo=oriana` e conferir os quadros em `run/demo/`.
  - **Sem ffmpeg neste computador:** GIF e folha de quadros em `previas/oriana_ataque.*`, como no recuo do Atomic.
- [ ] **Passo 7:** `./gradlew build` verde e commit "Oriana: a cidade em chamas no horizonte, a noite e a fumaça".

### Tarefa 4: vault e fechamento

- [ ] **Passo 1:** escrever o diário `vault/Diário/2026-11-13 Oriana sob ataque.md` com o índice. No Roadmap ("Onde estamos"), o mundo interior fica como a próxima arena.
- [ ] **Passo 2:** anotar em Pendências o teste no jogo:
  - andar pela beirada sem se queimar à toa;
  - o fogo continua igual depois de minutos;
  - o Ragnarok voa sem bater em nada;
  - a noite e a cidade só aparecem em Oriana.
- [ ] **Passo 3:** `./gradlew build` e `./gradlew runGameTestServer` verdes. Depois, um revisor novo lê o branch inteiro, e cada achado crítico ou importante corrigido ganha um teste que falha antes.
- [ ] **Passo 4:** commit "Vault: Oriana sob ataque no diário, Roadmap e Pendências".
