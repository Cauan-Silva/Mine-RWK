# O mundo interior do mangá: plano

> **Para quem executa:** execução nativa (superpowers:executing-plans), tarefa por tarefa, com TDD. Passos com `- [ ]`.

**Objetivo:** a arena do Zangetsu vira o mundo interior do mangá: prédios deitados mais detalhados, o céu azul com nuvens, os prédios até o horizonte, e o céu cinza com chuva quando a máscara aparece.

**Arquitetura:**
- **A planta:** `core/arena/PlantaMundoInterior` ganha os detalhes, com materiais novos no enum `PlantaEscola.Material` e em `ObrasArena.estado`.
- **O cliente:** repete o caminho de Oriana. Uma cúpula própria (`CeuInterior`), o horizonte por cenário e a `CeuArena` sem nuvens.
- **A fase 2:** o servidor troca o cenário para `interior_chuva` quando o Zangetsu veste a máscara. O cliente escurece o céu e liga a chuva só no próprio mundo dele.

**Ferramentas:** Java 21, NeoForge 21.1.265, JUnit, GameTests, Python + Pillow.

**Spec:** [[2026-11-13 Mundo interior]] (`vault/Desenvolvimento/Specs/`).

## Restrições globais
- **A fachada da luta:** de dx −22 a +22, com chão em `dy` −1 por todo o comprimento e nada acima de `dy` −1 nesse campo (o `osOutrosPrediosFicamForaDoCampoDaLuta` já exige). As entradas não mudam.
- **A caixa:** nada fora de ±`CaixaArena.MEIA` nem da faixa de altura. No máximo 60 000 blocos. Nenhuma posição repetida.
- **O clima do servidor nunca muda:** a chuva é só do cliente.
- **Lados:** nada de `client/` fora de `client/`.
- **Ao fim de cada tarefa:** `./gradlew build` verde. Nas tarefas que mexem em servidor ou arena, também `./gradlew runGameTestServer` com "All N required tests passed". Nunca dois Gradle ao mesmo tempo, e no Bash com `dangerouslyDisableSandbox: true`.
- **Assets gerados** só pelo gerador. O `ferramentas/previa_arena.py` precisa da cor de todo material novo (o `PlantaEscolaTest` exige).
- **Commits:** em português, terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Foco da revisão
1. **A chuva não pode vazar:** nem para as outras arenas, nem para o mundo normal, nem depois de morrer ou sair do jogo na arena. O nível de chuva volta a 0 sempre que o cenário deixa de ser `interior_chuva`. Isso se confere na prévia, com a volta ao mundo, e em Pendências.
2. **A fase 2 sem arena** (o Zangetsu dos GameTests, invocado fora da arena) não pode quebrar: `trocarCenario` só manda o pacote e só atualiza a vaga se ela existir. Coberto pelo GameTest da Tarefa 2.
3. **Os céus dos golpes:** a cúpula do mundo interior também roda com prioridade alta, para o Roxo Máximo e o Mugetsu ficarem por cima, como em Oriana.
4. **Os detalhes não podem virar chão no campo da luta:** coberto pelo teste existente e pelo novo `detalhesForaDoCampoDaLuta`.
5. **Desempenho:** a contagem de blocos e as nuvens da cúpula, que são poucas (30) e desenhadas sem `pow` dentro de laços.

---

### Tarefa 1: os prédios mais detalhados

**Arquivos:** mudar `core/arena/PlantaEscola.java` (enum), `arena/ObrasArena.java` (`estado`), `core/arena/PlantaMundoInterior.java`, `ferramentas/previa_arena.py` (as cores) e `src/test/java/com/minerwk/core/arena/PlantaMundoInteriorTest.java`.

**Interfaces que nascem aqui:**
- **Materiais novos:** `JANELA_ACESA`, `VIDRO_CLARO`, `GRADE_FERRO`, `PARA_RAIOS`, `LETREIRO_VERMELHO` e `LETREIRO_AMARELO`.
- **Os blocos de cada um:** luz-de-sapo ocre, vidro cinza-claro, barras de ferro, para-raios, concreto vermelho e concreto amarelo.

- [ ] **Passo 1: os testes que falham** em `PlantaMundoInteriorTest`:
  - **`haJanelasAcesasEmTodosOsPredios`:** há `JANELA_ACESA` com |dx| ≤ 22 (embaixo do vidro da luta, em `dy` −2), com dx ≥ `ALTO_DE` e com dx ≤ `BAIXO_ATE`. Há também `VIDRO_CLARO` na fachada da luta.
  - **`detalhesForaDoCampoDaLuta`:** nenhum `GRADE_FERRO`, `PARA_RAIOS`, `LETREIRO_*` ou `ASSOALHO` com |dx| ≤ `MEIO_PREDIO` e `dy` ≥ 0. Há pelo menos 4 `PARA_RAIOS` e pelo menos 2 blocos de cada cor de letreiro.
  - **`asAntenasTemApoio`:** todo `PARA_RAIOS` tem um `GRADE_FERRO` vizinho (em qualquer dos seis lados). Toda coluna de antena encosta na fachada do seu prédio.
  - **`aFachadaDaLutaContinuaInteira`:** para |dx| ≤ 22 e todo dz, há bloco em `dy` −1, e ele é concreto, vidro azul ou vidro claro. Nunca a luz no lugar do vidro.
- [ ] **Passo 2:** `./gradlew test --tests "*PlantaMundoInteriorTest*"`. Esperado: falha.
- [ ] **Passo 3:** implementar:
  - **as janelas:** por um sorteio fixo da posição, como o `acaso` de `PlantaOriana`, uns 20% das janelas ganham `JANELA_ACESA` no lugar do `FUNDO_ESCURO` embaixo, e uns 12% trocam o vidro azul por `VIDRO_CLARO`;
  - **as antenas:** no prédio alto, colunas de `GRADE_FERRO` saindo da fachada para cima, com 5 a 8 de comprimento e um `PARA_RAIOS` na ponta. No baixo, as antenas saem para cima da fachada dele, abaixo do campo da luta;
  - **as caixas d'água:** de `ASSOALHO`, 3 × 3 × 3, sobre quatro pés de `GRADE_FERRO`;
  - **os letreiros:** placas de 2 × 6 com faixas de `LETREIRO_VERMELHO` e `LETREIRO_AMARELO` na fachada do alto, na do baixo e na frente do atravessado;
  - **a marquise:** uma faixa de concreto claro, um bloco para fora, na beirada das laterais do alto e do baixo que dão para a luta.
  - **As cores** dos materiais novos em `previa_arena.py`, e um despejo `build/arena_interior.txt` no teste, como o de Oriana, para `python ferramentas/previa_arena.py interior`.
- [ ] **Passo 4:** `./gradlew build` e `./gradlew runGameTestServer` verdes. Conferir de olho a prévia `previas/arena_interior.png`.
- [ ] **Passo 5:** commit "Mundo interior: janelas acesas, antenas, caixas d'água, letreiros e marquises".

### Tarefa 2: a fase 2 troca o cenário para a chuva

**Arquivos:** mudar `rede/CenarioArena.java` (constante), `arena/ArenasServidor.java` e `boss/ZangetsuEntity.java` (`vestirAMascara`). Teste em `kit/TestesZangetsu.java`.

**Interfaces que nascem aqui:**
- `CenarioArena.INTERIOR_CHUVA = "interior_chuva"`.
- `ArenasServidor.trocarCenario(ServerPlayer jogador, String cenario)`: se o jogador tem uma vaga, guarda o cenário nela, e sempre manda o pacote.
- `ArenasServidor.cenarioEnviado(UUID)` passa a ser `public`.

- [ ] **Passo 1: o teste que falha.** Em `abaixoDeQuarentaPorCentoEleVesteAMascara`, depois de "a máscara aparece no rosto", conferir `CenarioArena.INTERIOR_CHUVA.equals(ArenasServidor.cenarioEnviado(jogador.getUUID()))`, com a mensagem "a fase 2 manda a chuva".
- [ ] **Passo 2:** `./gradlew runGameTestServer`. Esperado: esse teste falha (não compila antes da constante).
- [ ] **Passo 3:** implementar `trocarCenario`. Em `vestirAMascara(dono, …)`, chamar `ArenasServidor.trocarCenario(dono, CenarioArena.INTERIOR_CHUVA)` quando `dono != null`.
- [ ] **Passo 4:** `./gradlew build` e `./gradlew runGameTestServer` verdes (o Toji derruba o servidor de testes de vez em quando; rodar de novo se a queda for dele).
- [ ] **Passo 5:** commit "Mundo interior: a máscara traz o cenário da chuva".

### Tarefa 3: o céu azul, o horizonte e a chuva

**Arquivos:**
- Criar `ferramentas/gerar_horizonte_interior.py`, as faixas `textures/ceu/horizonte_interior_<0..3>.png` e `client/arena/CeuInterior.java`.
- Mudar `client/arena/HorizonteCliente.java`, `client/arena/CeuArena.java`, `client/MineRWKClient.java` e `client/vfx/DemoVfx.java`.

**Interfaces:** usa `CenarioArena.INTERIOR`, `INTERIOR_CHUVA`, `CenarioCliente.atual()`, `AnelHorizonte` e o padrão de `NoiteOriana`.

- [ ] **Passo 1: o gerador.**
  - **O formato:** o mesmo de Oriana (quatro faixas de 1024 × 768, três linhas de 256, o pátio na altura 171, ruído periódico e a emenda conferida).
  - **As três camadas:** prédios deitados, retângulos compridos na horizontal com a grade de janelas, de várias alturas, alguns atravessados na diagonal.
  - **As cores:** concreto claro e vidro azul, cada camada mais clara e mais azul (a névoa da distância).
  - **O céu da textura** é transparente.
- [ ] **Passo 2: o `HorizonteCliente`.**
  - **Os cenários:** `interior` e `interior_chuva` usam as faixas `horizonte_interior_<n>`, e a cor da saia é a do pé da camada perto.
  - **Na chuva,** o anel é desenhado com a cor dos vértices em 55% (mais escuro).
- [ ] **Passo 3: `CeuInterior`.**
  - **Quando:** com o cenário `interior` ou `interior_chuva` e na dimensão das arenas. Registrar com `EventPriority.HIGH` como a `NoiteOriana`.
  - **A cúpula:** a mesma geometria da `NoiteOriana`. O degradê vai de `#3A7BD5` (zênite) a `#CFE8FF` (horizonte), e na chuva de `#4E5862` a `#8A949C`. A mistura entre os dois anda 1/60 por tick rumo ao alvo, então leva 3 s.
  - **As nuvens:** 30, em direções fixas acima de 15°, achatadas (três discos sobrepostos), brancas com alfa .85. Na chuva, cinza-escuro.
  - **O movimento:** elas giram em volta do eixo vertical a 0,02°/tick, de leste para oeste.
  - **A névoa:** no `ComputeFogColor`, com prioridade alta e fora de líquido, a cor do horizonte.
  - **A chuva:** no tick do cliente, com o cenário `interior_chuva`, o nível de chuva do mundo do cliente sobe 0,02 por tick até 1. Em qualquer outro cenário dentro da dimensão das arenas, ele desce até 0. Ao sair da dimensão, vai a 0.
- [ ] **Passo 4:** a `CeuArena.renderClouds` também pula as nuvens com `CeuInterior.ativa()`.
- [ ] **Passo 5: a prévia `interior`.**
  - **Igual à de Oriana**, com `rwk zangetsu @s` e `minerwk:zangetsu`. Os pontos:
    - de cima da fachada olhando o prédio alto;
    - o prédio baixo de cima;
    - o atravessado;
    - o céu.
  - **Na metade,** a prévia força a fase 2: `data merge entity @e[type=minerwk:zangetsu,limit=1] {Health:...}` com 39% da vida, ou o comando de teste que existir. Conferir a chuva chegando.
  - **O último ponto** sai com o VFX reduzido.
  - **Na volta ao mundo,** conferir que não chove.
  - **Saída:** GIF e folha em `previas/mundo_interior.*`.
- [ ] **Passo 6:** `./gradlew build` verde, a prévia conferida e commit "Mundo interior: céu azul, prédios até o horizonte e a chuva da máscara".

### Tarefa 4: vault e fechamento

- [ ] **Passo 1:** escrever o diário `vault/Diário/2026-11-13 Mundo interior.md` com o índice. No Roadmap ("Onde estamos"), o pedido das arenas fica fechado. Em Pendências, o teste no jogo:
  - a chuva chega com a máscara;
  - ela não vaza depois da vitória, da morte ou de sair do jogo;
  - quem sai e volta no meio da fase 2;
  - o céu azul e o horizonte com distância de renderização 8 e 16.
- [ ] **Passo 2:** `./gradlew build` e `./gradlew runGameTestServer` verdes. Depois, um revisor novo lê o branch inteiro, e cada achado crítico ou importante corrigido ganha um teste que falha antes.
- [ ] **Passo 3:** commit "Vault: o mundo interior no diário, Roadmap e Pendências".
