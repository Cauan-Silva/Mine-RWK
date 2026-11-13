---
tags: [desenvolvimento, plano]
estado: aguardando revisão do autor
---
# Plano: detalhe da Escola Jujutsu

> **Para quem executa:** use superpowers:executing-plans (nativo) ou superpowers:subagent-driven-development, tarefa por tarefa. Os passos usam caixas (`- [ ]`).

**Objetivo:** a Escola Jujutsu com os prédios mobiliados por dentro, mais ricos por fora, um jardim no pátio e um horizonte de montanhas e cidade no céu.

**Arquitetura:**
- **Parte 1 (servidor e `core/`):** a planta continua só em `core/arena/PlantaEscola`, que ganha materiais e métodos novos; `arena/ObrasArena.estado` traduz cada material para um bloco.
- **Parte 2 (cliente e rede):**
  - o servidor manda `CenarioArena` ao entrar e ao sair;
  - o cliente guarda o cenário e desenha um anel de faixas PNG em volta do jogador, depois do céu;
  - a geometria do anel fica em `core/` e tem teste.

**Tecnologia:** NeoForge 21.1.265, MC 1.21.1, Java 21, JUnit 5, GameTests, Python (Pillow) para as texturas.

**Spec:** [[2026-11-13 Detalhe da Escola Jujutsu]] (`vault/Desenvolvimento/Specs/2026-11-13 Detalhe da Escola Jujutsu.md`).

## Restrições gerais
- **Pátio livre:** `patioFicaLivreParaALuta`, `entradasTemChaoEEspaco` e a escadaria continuam passando sem mudar esses testes.
- **Caixa:** nada sai de 101 × 101, nada se repete (`cenarioCabeNaCaixaSemRepetirEMontaRapido`).
- **Limite de blocos:** **65 000** (hoje são cerca de 49 mil). Se passar, primeiro afrouxar a floresta (`PASSO_DA_FLORESTA`) e só depois tirar detalhe dos prédios.
- **Classes de cliente:** `core/` não importa `net.minecraft`, e o código comum não importa `client/`. O horizonte e o estado do cenário ficam em `client/arena/`.
- **Apoio:** tudo que depende de apoio (lanterna, vela, sino, vitória-régia, tapete, bambu) é posto **depois** do apoio, por `Obra.por`, que já reordena.
- **Orientação:** blocos com orientação viram constantes separadas por direção (como `ESCADA_PEDRA` é sempre norte), nunca um estado guardado fora do enum.
- **Texturas:** os PNGs do horizonte saem só de `ferramentas/gerar_horizonte_escola.py`. Não se editam à mão.
- **Rede:** a versão do `PayloadRegistrar` sobe de `"6"` para `"7"`.
- **Ao fim de cada parte:**
  - `./gradlew build` e `./gradlew runGameTestServer` verdes;
  - uma prévia;
  - Roadmap, diário e Pendências.

## Onde o olho do revisor deve ir
1. **Água que vaza** ao montar fatia por fatia: a água é posta antes da borda e escorre por um tick. A borda e o fundo da lagoa têm que vir **antes** da água na lista. Há teste de ordem.
2. **Planta que cai:** bambu, vitória-régia e tapete sem o bloco certo embaixo quebram na montagem. Há teste de apoio para os três.
3. **O cenário que fica preso no cliente:** se o jogador morre, desloga ou é trazido de volta por `fechar`, o cliente tem que receber `""`. Um `CenarioArena` esquecido desenha montanhas no mundo normal. Há GameTest para sair por `aoSair` e por `fechar`, e o cliente também limpa no `LoggingOut`.
4. **O horizonte atravessando o terreno:** o anel tem que ficar além da caixa (raio > 51 × √2 ≈ 73) e a base abaixo do pé da escadaria. Há teste de geometria.
5. **Custo por quadro:** o anel é desenhado todo quadro. Os vértices são calculados uma vez (cache por raio e camada), e o VFX reduzido desenha só uma camada.

---

## Parte 1: a planta

### Tarefa 1: materiais novos e o bloco de cada um

**Arquivos:**
- Modificar: `src/main/java/com/minerwk/core/arena/PlantaEscola.java` (enum `Material`)
- Modificar: `src/main/java/com/minerwk/arena/ObrasArena.java:156` (`estado`)
- Modificar: `ferramentas/previa_arena.py` (`CORES`)
- Teste: `src/test/java/com/minerwk/core/arena/PlantaEscolaTest.java`

**Interfaces:**
- **Produz:** os materiais abaixo, usados nas tarefas 2 a 5.
- **Além da lista do spec** (decisão do plano, porque o próprio spec cita esses móveis): `LAJE_MADEIRA`, `TAPETE`, `FENO`, `ALVO`, `QUADRO` e as variantes com direção.

| Material | Bloco |
|---|---|
| `TATAME` | `HAY_BLOCK` |
| `BAMBU_PISO` | `BAMBU_MOSAIC` |
| `SHOJI` | `WHITE_STAINED_GLASS_PANE` |
| `GRADE_NS`, `GRADE_LO` | `SPRUCE_TRAPDOOR` aberto, de frente para o norte ou para o leste |
| `CERCA_ESCURA` | `DARK_OAK_FENCE` |
| `AGUA` | `WATER` |
| `VITORIA_REGIA` | `LILY_PAD` |
| `CASCALHO` | `GRAVEL` |
| `PEDRA_MUSGO` | `MOSSY_STONE_BRICKS` |
| `TRONCO_CEREJEIRA` | `CHERRY_LOG` |
| `FOLHA_CEREJEIRA` | `CHERRY_LEAVES`, com `PERSISTENT` |
| `BAMBU` | `BAMBOO` |
| `SINO` | `BELL`, preso ao teto (`BellAttachType.CEILING`) |
| `VELA` | `CANDLE`, 3 velas, acesa |
| `BARRIL` | `BARREL` |
| `ESTANTE` | `BOOKSHELF` |
| `SUPORTE_ARMADURA` | `SPRUCE_TRAPDOOR` fechado na metade de cima |
| `LAJE_MADEIRA` | `DARK_OAK_SLAB` |
| `TAPETE` | `RED_CARPET` |
| `FENO` | `HAY_BLOCK`, em pé |
| `ALVO` | `TARGET` |
| `QUADRO` | `BLACK_CONCRETE` |
| `ESCADA_MADEIRA_N/S/L/O` | `DARK_OAK_STAIRS`, com `FACING` |

- [ ] **Passo 1: teste que falha.** `todoMaterialTemCorNaPrevia`: lê `ferramentas/previa_arena.py` e confere que todo `Material.name()` aparece em `CORES`.
  - Ele protege a prévia: o script hoje quebra com um material sem cor.
  - O `switch` de `ObrasArena.estado` já é exaustivo, então o compilador cobre o bloco de cada material.
- [ ] **Passo 2:** `./gradlew test --tests "*PlantaEscolaTest*"`. Esperado: FALHA, com "TATAME sem cor".
- [ ] **Passo 3:** os materiais no enum (com javadoc agrupando: interior, jardim, móveis), os `case` no `estado` e as cores no script.
- [ ] **Passo 4:** `./gradlew build`. Esperado: verde.
- [ ] **Passo 5:** commit `Escola: materiais novos da planta e os blocos de cada um`.

### Tarefa 2: o salão por dentro (templo)

**Arquivos:**
- Modificar: `PlantaEscola.java`: novo `private static void interiorSalao(Obra o)`, chamado em `cenario()` logo depois de `predio(o, SALAO)`
- Teste: `PlantaEscolaTest.java`

**Interfaces:**
- **Consome:** os materiais da Tarefa 1, `SALAO` (x −12..12, z −39..−27, pódio 2, parede 6, porta ao sul).
- **Produz:** `public static boolean temCaminho(Map<String, Material> m, Predio p, int destinoX, int destinoZ)` no teste (auxiliar). É uma busca em largura sobre as posições onde os pés e a cabeça (`podio`, `podio + 1`) estão livres ou têm tapete. A Tarefa 3 reusa essa função.

**Desenho** (z cresce para o sul; a porta fica em z = −27):
- **Antessala**, nas fileiras z −29..−28: fileira de `SHOJI` em z = −30, com uma abertura de 3 no meio (x −1..1).
- **Nave**, em z −38..−31:
  - **piso:** `TATAME` em faixas de 2, separadas por uma fileira de `BAMBU_PISO` (o piso fica em `podio − 1`, por cima do `ASSOALHO`);
  - **pilares:** `PILAR` nos dois lados, em x = ±9, a cada 3 fileiras, até a viga.
- **Altar**, em z −38..−37 e x −3..3:
  - um degrau de `LAJE_MADEIRA` na fileira −37;
  - a mesa de `MADEIRA_ESCURA` na fileira −38, com `VELA` em cima;
  - `ESTANTE` na parede de trás (z −38, encostada à parede);
  - `LANTERNA` sobre uma `CERCA_ESCURA` em x = ±4.
- **Antessala, os móveis:** duas `LAJE_MADEIRA` (mesas baixas) com `TAPETE` ao lado (almofadas), longe da porta (x = ±8).

- [ ] **Passo 1: testes que falham:**
  - `salaoTemAltarEVelasComApoio`: existe `VELA` com um bloco sólido embaixo, e existe `ESTANTE`;
  - `salaoTemShojiComPassagem`: existe `SHOJI` e `temCaminho(m, SALAO, 0, -36)` é verdadeiro.
- [ ] **Passo 2:** rodar `PlantaEscolaTest`. Esperado: FALHA.
- [ ] **Passo 3:** `interiorSalao`.
  - O teste antigo `prediosSaoOcos...` exige ar de `podio + 1` até o topo dentro de todo prédio.
  - Ele passa a valer só para **a coluna da porta até o meio** (x = meio do prédio), porque os móveis agora ocupam o resto.
  - Os demais limites desse teste ficam: assoalho, telhado fechado, porta, parede e beiral.
  - Registrar essa mudança no commit.
- [ ] **Passo 4:** rodar `PlantaEscolaTest`. Esperado: verde.
- [ ] **Passo 5:** commit `Escola: o salão por dentro, com altar, shoji e tatame`.

### Tarefa 3: os pavilhões por dentro

**Arquivos:**
- Modificar: `PlantaEscola.java`: `interiorSalaDeAula(Obra o)` (o `PAVILHAO_LESTE`) e `interiorSalaDeTreino(Obra o)` (o `PAVILHAO_OESTE`)
- Teste: `PlantaEscolaTest.java`

**Interfaces:**
- **Consome:** `temCaminho` (Tarefa 2) e os materiais da Tarefa 1.

**Desenho:**
- **Sala de aula** (leste, porta a oeste):
  - fileiras de `LAJE_MADEIRA` com `TAPETE` atrás, em 3 colunas, deixando o corredor do meio (z −1..1) livre;
  - na parede do fundo (x = 38): `QUADRO` 5 × 2 no meio e `ESTANTE` dos lados.
- **Sala de treino** (oeste, porta a leste):
  - o piso continua `ASSOALHO`;
  - na parede norte (z = −9): `SUPORTE_ARMADURA` com `BARRIL` embaixo;
  - na parede do fundo (x = −38): dois alvos, cada um com `FENO` embaixo e `ALVO` em cima;
  - no meio: um quadrado de `TAPETE` 5 × 5.

- [ ] **Passo 1: teste que falha.** `pavilhoesMobiliadosComCaminho`:
  - no leste há `QUADRO` e pelo menos 6 `LAJE_MADEIRA`;
  - no oeste há 2 `ALVO` sobre `FENO`, e `BARRIL`;
  - `temCaminho` vai da porta ao meio de cada pavilhão;
  - todo `TAPETE` tem um bloco sólido embaixo.
- [ ] **Passo 2:** rodar. Esperado: FALHA.
- [ ] **Passo 3:** os dois métodos, chamados em `cenario()` depois de `predio(...)`.
- [ ] **Passo 4:** rodar `PlantaEscolaTest`. Esperado: verde.
- [ ] **Passo 5:** commit `Escola: sala de aula e sala de treino nos pavilhões`.

### Tarefa 4: os prédios por fora e as construções novas

**Arquivos:**
- Modificar: `PlantaEscola.java`: `engawa(Obra o)`, `janelas(Obra o, Predio p)`, `ornamentos(Obra o, Predio p)`, `torreDoSino(Obra o)`, `deposito(Obra o)` e `portaoLateral(Obra o)`
- Teste: `PlantaEscolaTest.java`

**Interfaces:**
- **Produz:**
  - `public static final int[] SINO = {x, y, z}`, a posição do sino, para o teste;
  - `public static final Predio DEPOSITO`, que **não** entra em `PREDIOS`, para não herdar a regra da porta de 5.

**Desenho:**
- **Engawa:**
  - um anel de `ASSOALHO` em volta do salão, 1 bloco para fora das paredes, na altura `podio − 1`;
  - `CERCA_ESCURA` na borda de fora, menos na frente da porta (x −2..2) e nos degraus;
  - `ESCADA_MADEIRA_S` descendo na frente da porta.
- **Janelas:** em cada vão de parede branca entre dois pilares, um `GRADE_NS`/`GRADE_LO` na altura `podio + 2`. Nunca na porta.
- **Ornamentos:**
  - `CERCA_ESCURA` de pé sobre os 4 cantos do beiral;
  - uma fileira de `MADEIRA_ESCURA` na cumeeira de cada telhado.
- **Torre do sino:**
  - 4 `PILAR` de 5 de altura em x 22..24, z −24..−22, ao nordeste, fora do pátio de luta e longe do beiral do pavilhão leste;
  - telhado de `LAJE_TELHA`;
  - o `SINO` pendurado sob o centro do teto.
- **Depósito:**
  - 5 × 5 de `MADEIRA_ESCURA` em x −26..−22, z −25..−21, ao noroeste;
  - teto de `LAJE_TELHA`;
  - porta de 1 × 2 (ar) no lado sul.
- **Portão lateral:**
  - no muro oeste, em z ≈ 0, atrás do pavilhão oeste, se o muro chegar lá;
  - senão, na borda oeste da área de grama;
  - dois `PILAR` com uma viga de `MADEIRA_VERMELHA` e telha por cima;
  - fechado por `CERCA_ESCURA` (sem passagem para fora).

- [ ] **Passo 1: testes que falham:**
  - `sinoTemApoio`: o bloco acima de `SINO` não é nulo;
  - `engawaCercaOSalaoComEntrada`: há `CERCA_ESCURA` na linha z = −26 e ar na frente da porta;
  - `janelasNaoFicamNaPorta`: nenhuma `GRADE_*` está nas 5 colunas da porta de cada prédio;
  - `depositoTemPorta`.
- [ ] **Passo 2:** rodar. Esperado: FALHA.
- [ ] **Passo 3:** os métodos.
  - Conferir que a torre e o depósito passam em `livreParaArvore` (a floresta não pode nascer dentro deles).
  - Se não passarem, estender `livreParaArvore` com essas duas áreas.
- [ ] **Passo 4:** rodar `PlantaEscolaTest`. Esperado: verde, com `patioFicaLivreParaALuta` intocado.
- [ ] **Passo 5:** commit `Escola: engawa, janelas, torre do sino, depósito e portão`.

### Tarefa 5: o jardim, o limite de blocos e o fim da Parte 1

**Arquivos:**
- Modificar: `PlantaEscola.java`: `lagoa(Obra o)`, `jardimSeco(Obra o)`, `cerejeira(Obra o, int x, int z, int altura)`, `bambuzal(Obra o)` e `acabamento(Obra o)`
- Teste: `PlantaEscolaTest.java`, `TestesArena.java` (GameTest de tempo)

**Interfaces:**
- **Produz:** `public static final int LAGOA_X0, LAGOA_X1, LAGOA_Z0, LAGOA_Z1`, a área da lagoa, para o teste.

**Desenho:**
- **Lagoa:** ao sudeste, em x 22..28 e z 14..20, fora da luta (|x| > 20) e longe do beiral do pavilhão (z > 12) e da escadaria.
  - A borda é de `PEDRA_MUSGO`.
  - A água tem 2 de fundo no meio e 1 nas bordas, com o fundo de `PEDRA`.
  - Há 3 `VITORIA_REGIA` sobre a água e 3 pedras de passo de `LAJE_PEDRA`.
  - **Ordem:** o fundo e a borda vêm antes da água, na lista.
- **Jardim seco:** ao sudoeste, espelhado (x −28..−22).
  - `CASCALHO` no chão e uma borda de `LAJE_PEDRA`.
  - Três pedras: `PEDRA_MUSGO` 2 × 2 × 2, `ANDESITO_POLIDO` 1 × 2 × 1 e `PEDRA_MUSGO` 1 × 1 × 1.
- **Cerejeiras:** 5 delas, de 6 a 8 blocos.
  - **Posições:** 2 ladeando o salão (x = ±16, z ≈ −30), 1 perto da lagoa, 1 perto do jardim seco e 1 atrás do depósito.
  - **Forma:** a copa é uma esfera achatada de `FOLHA_CEREJEIRA`.
  - **Onde não:** nenhuma cerejeira dentro de |x|, |z| ≤ 20, nem sob o beiral (`Predio.contem` com folga).
- **Bambuzal:** touceiras de `BAMBU`, de 4 a 7 de altura, sobre `GRAMA`, atrás dos dois pavilhões (|x| 41..44, a cada 2 em z).
- **Acabamento:**
  - bancos de `ESCADA_MADEIRA_*` virados para o pátio nas bordas leste e oeste, fora da luta;
  - `PEDRA_MUSGO` sorteada (hash fixo de x e z, sem `Random`) em cerca de 15 % do piso das bordas do pátio, onde 20 < |x| ou |z| ≤ 30;
  - uma fileira de `CERCA_ESCURA` entre a grama do pátio e a floresta.
- **Limite de blocos:** baixar o limite do teste de 80 000 para 65 000. Se passar, subir `PASSO_DA_FLORESTA` de 7 para 8 ou 9.

- [ ] **Passo 1: testes que falham:**
  - `aguaNaoVaza`: para toda `AGUA`, os blocos de baixo e dos 4 lados são sólidos ou `AGUA`, e o índice de cada vizinho sólido na lista é **menor** que o da água;
  - `plantasTemApoio`: `BAMBU` sobre `GRAMA` ou `BAMBU`; `VITORIA_REGIA` sobre `AGUA`; `TAPETE` sobre um sólido;
  - `cerejeirasForaDaLutaEDosPredios`;
  - `cenarioCabeNaCaixaSemRepetirEMontaRapido` com `<= 65000`.
- [ ] **Passo 2:** rodar. Esperado: FALHA.
- [ ] **Passo 3:** os métodos, chamados em `cenario()` **antes** de `floresta(o)`, e a floresta respeitando as áreas novas.
- [ ] **Passo 4:** rodar `PlantaEscolaTest`. Esperado: verde. Anotar o total de blocos.
- [ ] **Passo 5:** GameTest `escolaMontaDentroDoTempo` em `TestesArena`: entra na Escola e confere que a obra termina (`ObrasArena.ocioso()`) em até 20 ticks.
- [ ] **Passo 6:** `./gradlew build` e depois `./gradlew runGameTestServer`. Esperado: "All N required tests passed".
- [ ] **Passo 7: prévia.**
  - O teste `despejaAPlantaParaAPrevia` grava `build/arena.txt`; rodar `python ferramentas/previa_arena.py`.
  - Gerar `previas/escola_detalhe_planta.png` e conferir a vista de cima.
  - Para dentro dos prédios, ver a Tarefa 8, que grava a prévia no jogo das duas partes juntas.
- [ ] **Passo 8:** Roadmap, diário `vault/Diário/2026-11-13 Escola detalhe parte 1.md` e Pendências (andar pelos prédios no jogo). Commit `Escola: jardim, cerejeiras e bambu; fim da parte 1`.

---

## Parte 2: o horizonte

### Tarefa 6: o pacote `CenarioArena`

**Arquivos:**
- Criar: `src/main/java/com/minerwk/rede/CenarioArena.java`
- Modificar: `rede/Pacotes.java`: o registro (versão `"7"`) e `public static volatile Consumer<CenarioArena> aoCenarioArena`
- Modificar: `arena/ArenasServidor.java`: os `entrar` (linhas 151, 156 e 166), `fechar` (218) e `aoSair` (257)
- Modificar: `boss/LutasRagnarok.java:108` e `boss/LutasZangetsu.java:113`
- Teste: `arena/TestesArena.java`

**Interfaces:**
- **Produz:**
  - `public record CenarioArena(String cenario) implements CustomPacketPayload`, com `TIPO` e `CODEC` (`ByteBufCodecs.STRING_UTF8`);
  - as constantes `ESCOLA = "escola"`, `INTERIOR = "interior"`, `ORIANA = "oriana"`, `NENHUM = ""`;
  - `ArenasServidor.entrar(ServerPlayer, String cenario, List<PlantaEscola.Bloco>, ...)`: o nome entra como 2º parâmetro nos overloads que recebem a planta;
  - `static String cenarioEnviado(UUID)`: o último cenário mandado a esse jogador, para os GameTests.

- [ ] **Passo 1: GameTests que falham:**
  - `cenarioEscolaAoEntrarEVazioAoSair`: entra na Escola, `cenarioEnviado == "escola"`; `aoSair`, `""`;
  - `cenarioVazioAoFechar`: entra, `fechar`, `""`.
- [ ] **Passo 2:** `./gradlew runGameTestServer`. Esperado: falha de compilação ou dos testes novos.
- [ ] **Passo 3:**
  - o record e o registro;
  - um `private static void mandarCenario(ServerPlayer, String)` que faz `PacketDistributor.sendToPlayer` e grava no mapa;
  - a chamada ao chegar (com o nome) e em todo caminho de saída;
  - os dois chamadores dos bosses passam `INTERIOR` e `ORIANA`.
- [ ] **Passo 4:** `./gradlew build` e `./gradlew runGameTestServer`. Esperado: tudo verde.
- [ ] **Passo 5:** commit `Rede: CenarioArena avisa o cliente em que arena o jogador está`.

### Tarefa 7: as faixas do horizonte e a geometria do anel

**Arquivos:**
- Criar: `ferramentas/gerar_horizonte_escola.py`
- Criar (gerados): `src/main/resources/assets/minerwk/textures/ceu/horizonte_escola_0.png` … `_3.png`
- Criar: `src/main/java/com/minerwk/core/arena/AnelHorizonte.java`
- Teste: `src/test/java/com/minerwk/core/arena/AnelHorizonteTest.java`

**Interfaces:**
- **Produz:**
  - `public record Quad(double x0, double z0, double x1, double z1, double yBase, double yTopo, float u0, float u1, int faixa)`;
  - `public static List<Quad> anel(double raio, double yBase, double yTopo, int segmentosPorFaixa)`: as 4 faixas de 90°, com a faixa 0 começando no **sul** (+z), para a cidade ficar ao sul;
  - `public static final int FAIXAS = 4`;
  - `public static final double RAIO_MINIMO = 80`.

**Imagens:** cada PNG tem 1024 × 768, em três linhas de 256, uma por camada (a de cima é a mais longe). Há transparência acima das montanhas.
- **Três camadas de montanhas:**
  - perfil de ruído 1D periódico, com soma de senos de períodos inteiros, para emendar;
  - degradê de fim de tarde, cada camada mais clara e mais azul.
- **Floresta:** escura na base.
- **Faixa 0 (sul), a cidade:**
  - prédios em retângulos com janelas acesas;
  - uma torre treliçada vermelha e branca, genérica.
- **Emenda:** a coluna 1023 da faixa *n* casa com a coluna 0 da faixa *n + 1* (mod 4).
- **Determinismo:** `random.seed` fixo.

- [ ] **Passo 1: teste que falha.** `AnelHorizonteTest`:
  - `fechaOs360Graus`: a soma dos ângulos é 2π, e o fim de cada quad é o começo do próximo;
  - `faixasEmendam`: `u1` do último quad de uma faixa é 1, e `u0` do primeiro da seguinte é 0, no mesmo ponto;
  - `ficaForaDaCaixa`: toda distância ao centro é ≥ `RAIO_MINIMO` > `CaixaArena.MEIA * √2`;
  - `faixaZeroOlhaParaOSul`: o meio da faixa 0 tem z > 0 e x ≈ 0.
- [ ] **Passo 2:** `./gradlew test --tests "*AnelHorizonteTest*"`. Esperado: FALHA (a classe não existe).
- [ ] **Passo 3:** `AnelHorizonte`, sem nenhum `net.minecraft`.
- [ ] **Passo 4:** rodar. Esperado: verde.
- [ ] **Passo 5:** o gerador.
  - Rodar `python ferramentas/gerar_horizonte_escola.py`.
  - Conferir a emenda: o script falha com `assert` se a diferença média entre a borda de uma faixa e a da seguinte passar de 8 por canal.
  - Ver uma montagem pequena, convertida para JPEG.
- [ ] **Passo 6:** commit `Horizonte da Escola: gerador das faixas e geometria do anel`.

### Tarefa 8: o cliente desenha o horizonte e o fim da Parte 2

**Arquivos:**
- Criar: `src/main/java/com/minerwk/client/arena/CenarioCliente.java`, que guarda o cenário: `receber(CenarioArena)`, `atual()` e `limpar()`
- Criar: `src/main/java/com/minerwk/client/arena/HorizonteCliente.java`, ouvinte de `RenderLevelStageEvent`
- Modificar: `client/MineRWKClient.java`:
  - `Pacotes.aoCenarioArena = CenarioCliente::receber`;
  - o ouvinte do horizonte;
  - `LoggingOut` → `CenarioCliente.limpar()`
- Modificar: `client/vfx/DemoVfx.java`: o modo `escola`
- Teste: GameTests (nada de cliente vaza) e a prévia

**Interfaces:**
- **Consome:** `AnelHorizonte.anel`, `CenarioArena.ESCOLA`, `ConfigCliente.vfxReduzido()`.

**Desenho:**
- **Estágio:** `RenderLevelStageEvent.Stage.AFTER_SKY`. Só desenha se `CenarioCliente.atual()` for `"escola"` e o nível for `minerwk:arena`.
- **Posição:** centrado na câmera em x e z, com o y fixo no mundo (a base em y do pátio − 30, o topo em + 60). Assim ele não sobe junto quando se pula.
- **Camadas:** 3 anéis (raios 160, 220 e 300; o mais longe é desenhado primeiro), cada um com a sua linha de textura.
  - Cada camada usa a sua linha do PNG da Tarefa 7 (v de 0 a ⅓, de ⅓ a ⅔ e de ⅔ a 1).
  - Com `vfxReduzido`, desenha só o raio 160.
- **Cor:** `POSITION_TEX_COLOR` com blend. A cor do vértice da base mistura com a cor da névoa (`RenderSystem.getShaderFogColor()`), para sumir embaixo.
- **Cache:** a lista de quads é calculada uma vez por camada.

- [ ] **Passo 1:** `./gradlew build` e `./gradlew runGameTestServer`. Esperado: verde, e nenhuma classe de `client/` puxada pelo servidor.
- [ ] **Passo 2: o modo `escola` no `DemoVfx`:**
  - entra na Escola;
  - grava quadros em 4 posições: dentro do salão, na sala de treino, no jardim (a lagoa) e no alto da escadaria olhando para o sul;
  - grava mais um quadro com o VFX reduzido.
- [ ] **Passo 3:** `./gradlew runClientDemo -Pdemo=escola`.
  - Converter os quadros para `previas/escola_detalhe.mp4` e ver os JPEG pequenos.
  - **O que procurar:**
    - a emenda sem costura;
    - a base sem vão aparecendo;
    - a cidade ao sul;
    - o horizonte some ao voltar para o mundo normal.
- [ ] **Passo 4:**
  - Roadmap e diário `vault/Diário/2026-11-13 Escola detalhe parte 2.md`;
  - Pendências: ver no jogo com distâncias de renderização 8 e 16, e com o VFX reduzido;
  - Commit `Horizonte da Escola no céu; fim da parte 2`.

---

## Fim
Uma revisão do ramo inteiro (subagente de leitura), depois PR para `main`.
