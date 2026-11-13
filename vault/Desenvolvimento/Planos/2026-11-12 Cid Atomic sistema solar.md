# I Am Atomic, o recuo que revela o sistema solar: plano

> **Para quem executa:** execução nativa (superpowers:executing-plans), tarefa por tarefa, com TDD. Passos com `- [ ]`.

**Objetivo:** no recuo do estouro, a Terra de baixo vira uma bola, depois um ponto entre os planetas do sistema solar, com o ponto roxo pulsando nela e abrindo até o branco.

**Arquitetura:** uma cena virtual em `core/` (`SistemaSolar`): os astros com posição, uma câmera virtual que se afasta da Terra e as contas de direção e tamanho aparente de cada astro, tudo na base da cutscene (lado, cima, frente). O céu do espaço (`EspacoCliente.Ceu`) desenha essa cena na casca de 100 blocos quando a cutscene `CLARAO` está em curso; senão, desenha o céu de hoje. O estouro passa de 70 para 130 ticks.

**Ferramentas:** Java 21, NeoForge 21.1.265, JUnit; Python para o som (`ferramentas/gerar_sfx_cid.py`).

**Spec:** [[2026-11-12 Cid Atomic sistema solar]] (`vault/Desenvolvimento/Specs/`).

## Restrições globais
- `core/` sem classes do Minecraft. O desenho fica em `client/`. O código comum não importa `client/`.
- Ao fim: `./gradlew build` verde e `./gradlew runGameTestServer` com "All N required tests passed". Nunca dois Gradle ao mesmo tempo. No Bash, Gradle com `dangerouslyDisableSandbox: true`.
- O que muda: só o trecho do estouro. A subida, o espaço antes do "atomic", o dano, a volta e a onda de choque continuam iguais (só acontecem 60 ticks depois).
- Só quem vê a cutscene `CLARAO` vê o recuo. Com `cutscenes = false`, ou para o alvo, o céu fica como hoje.
- Assets gerados (sons) só pelo gerador: muda o `.py` e gera de novo.
- Commits em português, terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Foco da revisão
1. **Cutscenes desligadas:** com `cutscenes = false`, o céu fica exatamente o de hoje, e a esfera roxa do mundo não se apaga. Conferir no código (`emCurso` falso) e em Pendências.
2. **Troca sem salto em t = 10:** no começo do recuo, a Terra nova tem de coincidir com o planeta de hoje (direção para baixo, 62°). Coberto em `aTerraComecaComoOChaoDeHoje`.
3. **Tudo cabe na tela no fim:** nenhum astro fica fora do quadro da câmera final. Coberto em `noFimTodosOsAstrosCabemNoQuadro`.
4. **O rumo do Cid inclinado:** a base usa o rumo só no plano horizontal, como a cutscene. Se o Cid olhava para cima, o sistema não pode sair torto.
5. **A luz vista do chão ainda brilha quando os dois voltam:** `TICKS_DA_LUZ` acompanha o clarão. Coberto em `aLuzAindaBrilhaNaVolta`.

---

### Tarefa 1: a cena em `core/` e os tempos novos

**Arquivos:**
- Criar: `src/main/java/com/minerwk/core/personagens/cid/SistemaSolar.java`
- Mudar: `core/personagens/cid/AtomicFinal.java` (clarão, luz, esfera), `core/personagens/cid/CutsceneAtomic.java` (`doClarao`), `core/personagens/cid/Cid.java` (o tremor do `atomic`)
- Testes: criar `src/test/java/com/minerwk/core/personagens/cid/SistemaSolarTest.java`; ajustar `CidAtomicTest.java` se algum número fixo quebrar

**Interfaces que nascem aqui:**
- `AtomicFinal.TICKS_DO_CLARAO = 130`; `AtomicFinal.TICKS_DA_LUZ = TICKS_DO_CLARAO + 50` (o `DEPOIS_DA_VOLTA` continua 50); `AtomicFinal.TICKS_DA_ESFERA = 70`: `raioDoClarao(t)` passa a crescer em 70 ticks e fica em 1 depois.
- `CutsceneAtomic.LONGE`: `public static final Vetor`, o ponto final da câmera em `doClarao`, que passa a ser `(40, 55, -100)` (de cima e de lado).
- `SistemaSolar` (tempos em ticks desde o "atomic", vetores na base (lado, cima, frente), a Terra no centro da cena):
  - `INICIO = 10`, `TERRA_INTEIRA = 45`, `SISTEMA = 95`, `PULSO = 95`, `FIM_DO_PULSO = 120`
  - `record Astro(String nome, double raio, double orbita, double fase, int cor, boolean anel)`; `List<Astro> ASTROS`, com `SOL` e `TERRA` como constantes públicas
  - `Vetor OLHAR`: a direção para onde a câmera final olha, `normalize(centro − LONGE)`, com `centro = (0, 0, AO_LADO/2)`
  - `double recuo(double t)`: de 0 a 1
  - `double distancia(double t)`: a distância da câmera virtual à Terra, em raios da Terra
  - `Vetor camera(double t)`, `Vetor posicao(Astro a, double t)`, `Vetor direcao(Astro a, double t)` (unitário, da câmera virtual ao astro)
  - `double raioAparente(Astro a, double t)` (meio ângulo, em radianos, nunca menor que `MINIMO = Math.toRadians(.35)`)
  - `double esferaNoMundo(double t)`, `double pontoRoxo(double t)`, `double raioDoPonto(double t)` (radianos), `double astrosAntigos(double t)`
  - `Vetor PONTO`: onde o clarão fica na Terra, no topo dela (`(0, 1.02, 0)`)

**As decisões da cena (o executor ajusta os números na prévia):**
- **Raios em raios da Terra:** Sol 12; Mercúrio .4, Vênus .95, Terra 1, Marte .55, Júpiter 6, Saturno 5 (com anel), Urano 2.5, Netuno 2.4.
- **Órbitas em volta do Sol, no plano (lado, frente):** 25, 40, 60, 80, 140, 190, 240, 280. Ponha a fase da Terra de modo que o Sol fique de lado no quadro. A Terra fica parada no centro da cena, e o Sol e os outros se posicionam a partir dela. Os outros giram devagar, mais depressa os de dentro (`velocidade ∝ orbita^-1.5`, uns .004 rad/tick na órbita 60).
- **A câmera virtual:** ela sai de `(0, 1/sin 62°, 0)`, com a Terra ocupando a metade de baixo como hoje. A direção vai de `cima` para `−OLHAR` (interpolação normalizada pelo `recuo`). A distância vai de `1/sin 62°` a 4 entre `INICIO` e `TERRA_INTEIRA`, a Terra inteira com uns 14°. Depois cresce em exponencial até 650 em `SISTEMA` e para.
- **O ponto roxo:** `pontoRoxo` vai de 0 em `INICIO` a 1 em `TERRA_INTEIRA` e pulsa (1 ± .25) até `PULSO`. `raioDoPonto` cresce acelerando de 1,5° em `PULSO` até 100° em `FIM_DO_PULSO`. `esferaNoMundo` vai de 1 em `INICIO` a 0 em `TERRA_INTEIRA`. `astrosAntigos` (a lua e os dois planetas de hoje) vai de 1 a 0 no primeiro terço do recuo.

- [ ] **Passo 1: escrever os testes que falham** em `SistemaSolarTest`:

```java
@Test void aTerraComecaComoOChaoDeHoje() {
    assertVetor(new Vetor(0, -1, 0), SistemaSolar.direcao(SistemaSolar.TERRA, 0), 1e-6);
    assertEquals(Math.toRadians(62), SistemaSolar.raioAparente(SistemaSolar.TERRA, 0), Math.toRadians(.5));
    assertEquals(SistemaSolar.raioAparente(SistemaSolar.TERRA, 0), SistemaSolar.raioAparente(SistemaSolar.TERRA, SistemaSolar.INICIO), 1e-9);
}
@Test void aTerraViraUmaBolaInteira() {
    double r = Math.toDegrees(SistemaSolar.raioAparente(SistemaSolar.TERRA, SistemaSolar.TERRA_INTEIRA));
    assertTrue(r > 10 && r < 20, "a Terra inteira na tela: " + r);
}
@Test void noFimTodosOsAstrosCabemNoQuadro() {
    for (var a : SistemaSolar.ASTROS) {
        double ang = Math.toDegrees(Math.acos(SistemaSolar.direcao(a, SistemaSolar.SISTEMA).ponto(SistemaSolar.OLHAR)));
        assertTrue(ang < 30, a.nome() + " fora do quadro: " + ang);
    }
}
@Test void aTerraNoFimEhUmPontoQueAindaSeVe() {
    assertEquals(SistemaSolar.MINIMO, SistemaSolar.raioAparente(SistemaSolar.TERRA, SistemaSolar.FIM_DO_PULSO), 1e-9);
}
@Test void osPlanetasGiramSemSairDaOrbita() { /* Marte em 0 e em 60: posições diferentes, a mesma distância ao Sol */ }
@Test void aEsferaDoMundoPassaParaOCeu() {
    assertEquals(1, SistemaSolar.esferaNoMundo(SistemaSolar.INICIO), 1e-9);
    assertEquals(0, SistemaSolar.esferaNoMundo(SistemaSolar.TERRA_INTEIRA), 1e-9);
    assertEquals(0, SistemaSolar.pontoRoxo(SistemaSolar.INICIO), 1e-9);
    assertTrue(SistemaSolar.pontoRoxo(SistemaSolar.TERRA_INTEIRA) >= .75);
}
@Test void oPulsoFinalCobreATela() {
    assertTrue(SistemaSolar.raioDoPonto(100) < SistemaSolar.raioDoPonto(110));
    assertTrue(SistemaSolar.raioDoPonto(SistemaSolar.FIM_DO_PULSO) >= Math.toRadians(90));
}
@Test void oEstouroDura130EALuzAindaBrilhaNaVolta() {
    assertEquals(130, AtomicFinal.TICKS_DO_CLARAO);
    assertEquals(130, AtomicFinal.VOLTA - AtomicFinal.ATOMIC);
    assertTrue(AtomicFinal.luz(AtomicFinal.TICKS_DO_CLARAO + 20) > .5, "quem está no chão ainda vê a luz depois da volta");
    assertTrue(AtomicFinal.branco(SistemaSolar.FIM_DO_PULSO + 4) > .5, "o pulso final emenda no branco");
}
```

  (`Vetor.ponto` é o produto escalar. Se o nome for outro em `Vetor`, use o que existe. `assertVetor` é um auxiliar do próprio teste.)

- [ ] **Passo 2:** `./gradlew test --tests "*SistemaSolarTest*"`. Esperado: falha de compilação (a classe não existe).
- [ ] **Passo 3:** implementar `SistemaSolar` e os tempos novos em `AtomicFinal`. Em `CutsceneAtomic.doClarao`, usar `LONGE`. Em `Cid.Atomic.atomic(ctx)`, o tremor passa a durar 40 ticks (`ctx.tremor(2.5f, 40, 64)`): o recuo longo não treme.
- [ ] **Passo 4:** `./gradlew build`. Esperado: verde. Se um teste antigo de `CidAtomicTest` tinha número fixo do clarão de 70, trocar pela constante.
- [ ] **Passo 5:** `./gradlew runGameTestServer`. Esperado: "All N required tests passed" em `run/logs/latest.log`.
- [ ] **Passo 6:** commit "Atomic: a cena do sistema solar e o estouro de 130 ticks".

### Tarefa 2: o céu desenha o recuo

**Arquivos:** mudar `client/camera/CutsceneCliente.java`, `client/vfx/EspacoCliente.java`, `client/vfx/RenderVisuaisCid.java` (o `atomic`) e `client/vfx/DemoVfx.java` (`FIM_PREVIA` do `cid_final` de 410 para 470).

**Interfaces:**
- Usa: tudo de `SistemaSolar`, e `VisuaisAtivos.Visual.direcao()` (o rumo vem no visual `cid_atomic`).
- Cria: `CutsceneCliente.emCurso(String id): boolean`, verdadeiro enquanto a cutscene ativa é a desse id (guardar o id do roteiro ao receber).

- [ ] **Passo 1:** `EspacoCliente.aoVisual` guarda também o rumo do visual `cid_atomic` (só a parte horizontal, normalizada).
- [ ] **Passo 2:** em `Ceu.renderSky`, com `t = tempo desde atomicEm` e `CutsceneCliente.emCurso(CutsceneAtomic.CLARAO)` e `t ≥ 0`, desenhar o modo recuo:
  - a Terra como uma calota em volta de `direcao(TERRA, t)`, com meio ângulo `raioAparente`. Usar o mesmo `corDoPlaneta` com `a` reescalado de `[0, raio]` para `[0, BORDA]`, para em `t ≤ INICIO` ser idêntica ao planeta de hoje. A borda azul da atmosfera vai em volta da calota. O sol nascendo de hoje vai sumindo com `astrosAntigos`.
  - a lua e os dois planetas de hoje (`astros()`) com alfa `astrosAntigos(t)`.
  - os outros astros de `ASTROS`: discos em `direcao × R` com raio `R × tan(raioAparente)`. O Sol ganha halo (como o `disco` do sol de hoje), Saturno o anel (como o planeta laranja de hoje), e cada astro entra com alfa `1 − astrosAntigos(t)`.
  - as órbitas: um anel fino de pontos por órbita, projetado da cena, com alfa baixo (≈ .25) e o mesmo fade.
  - o ponto roxo em `PONTO`: um `disco` roxo/branco com raio `max(raioDoPonto, ângulo do PONTO)` e força `pontoRoxo`, com raios em cruz na fase do pulso. Ele também banha a calota da Terra de roxo (como o `banho` de hoje).
  - Para levar da base (lado, cima, frente) ao mundo, usar a mesma conta que a `CutsceneCliente` usa para os quadros, com o rumo guardado.
  - Sem cutscene em curso: o céu de hoje, sem mudança.
- [ ] **Passo 3:** em `RenderVisuaisCid.atomic`, quando `CutsceneCliente.emCurso(CLARAO)` e `t ≥ INICIO`, multiplicar o alfa por `SistemaSolar.esferaNoMundo(t)`.
- [ ] **Passo 4:** `./gradlew build` verde. Depois, `./gradlew runClientDemo -Pdemo=cid_final` e conferir os quadros do estouro em `run/demo/`:
  - nenhum salto em t = 10;
  - a Terra inteira por volta de t = 45;
  - o sistema inteiro no quadro em t = 95;
  - o pulso abrindo no branco.

  Ajustar os números da cena na Tarefa 1 se preciso, com os testes ainda verdes.
- [ ] **Passo 5:** regravar a prévia `previas/cid_parte6_atomic.mp4` do mesmo jeito que na parte 6 (ver o diário dela) e fazer o commit "Atomic: o céu mostra o sistema solar no recuo".

### Tarefa 3: o som acompanha

**Arquivos:** mudar `ferramentas/gerar_sfx_cid.py`, `src/test/java/com/minerwk/core/personagens/cid/SonsCidTest.java` e os sons gerados.

- [ ] **Passo 1: teste que falha** em `SonsCidTest`: `assertEquals(SistemaSolar.PULSO, marco(m, SonsCid.ATOMIC, "pulso"));`. O teste do marco `clarao` já existe e vai cobrar 130.
- [ ] **Passo 2:** no gerador, `CLARAO = 130 * T`, `LUZ = 180 * T`, e um marco `'pulso': ticks(PULSO)` com `PULSO = 95 * T` (comentado como `SistemaSolar.PULSO`). O ronco do `cid_atomic` se estica nos 6,5 s, mais grave e largo no meio do recuo, e ganha um último estouro curto e grave no pulso.
- [ ] **Passo 3:** rodar o gerador, `./gradlew build` verde e a prévia de som (como na parte 6). Commit "Atomic: o som do recuo e o pulso final".

### Tarefa 4: vault e fechamento

- [ ] **Passo 1:** escrever o diário `vault/Diário/2026-11-12 Cid Atomic sistema solar.md` com o índice em `Diário.md`. Marcar no Roadmap ("Onde estamos"). Em Pendências, anotar o teste no jogo: o recuo com cutscene, o céu de hoje sem cutscene, o tempo novo e o som.
- [ ] **Passo 2:** `./gradlew build` e `./gradlew runGameTestServer` verdes. Depois, um revisor novo lê o branch inteiro, e cada achado corrigido ganha um teste que falha antes.
- [ ] **Passo 3:** commit "Vault: o recuo do Atomic no diário, Roadmap e Pendências".
