---
tags: [desenvolvimento, plano]
---
# Arena do Toji: plano de implementação

> Execução nativa (decisão do autor no 1.3), no branch `arena-toji`, TDD nas partes puras, revisão final por um revisor separado. Os passos usam `- [ ]` para acompanhar.

**Objetivo:** a luta do [[Toji]] passa a acontecer numa arena própria (dimensão `minerwk:arena`), com o cenário da entrada da Escola Jujutsu, e o jogador volta ao lugar de onde saiu.
**Arquitetura:** a geometria (vagas, caixa, planta do cenário) é lógica pura em `core/arena/`, testada com JUnit. O servidor (`com.minerwk.arena`) monta e apaga a arena em fatias, leva e traz o jogador e aplica as regras. A caixa de cada vaga é função só da posição, então cliente e servidor aplicam a barreira sem pacote novo.
**Stack:** NeoForge 21.1.265, dimensão por arquivos de dados, Data Attachments, GameTest, JUnit 5.
**Spec:** [[2026-11-10 Arena do Toji]] (em `vault/Desenvolvimento/Specs/`).

## Restrições globais
- Português em nomes, comentários e mensagens; commits terminam com `Co-Authored-By: Claude <noreply@anthropic.com>`.
- `core/` sem classes do Minecraft; renderização só em `client/`.
- Verificação de cada tarefa: `./gradlew build` e `./gradlew runGameTestServer`.
- Toda mensagem nova entra em `pt_br.json` e `en_us.json`.
- Números do spec, sem mudar: vagas a **512** blocos; caixa **81 × 64 × 81**; pátio **61 × 61** com **4** blocos de pedra por baixo; barreira até **56** blocos acima do pátio; Toji a **16** blocos do jogador; **2 s** de espera do Toji e até **2 s** de montagem; volta **5 s** depois da vitória; queda no vazio dá **4** de dano e refaz **5 × 5** no centro.
- Cada parte (0 a 3) termina com diário, Roadmap e Pendências atualizados e o teste do autor no jogo. Não começar a parte seguinte sem esse teste.

## Foco de revisão
1. O jogador sai do jogo **durante a montagem** (antes de ser levado) → a vaga é liberada, nada o prende, e ao voltar ele está onde estava. (Tarefa 4)
2. O jogador **morre nos 5 s depois da vitória** → continua desperto, não perde itens e não é teleportado depois de renascer. (Tarefa 5)
3. **Pérola do Ender, fruta do coro ou golpe que empurra** para fora da caixa → o servidor põe de volta dentro; o mesmo para o Toji. (Tarefa 6)
4. **Ponto de retorno inválido** (dimensão que não existe mais, lugar obstruído) → renascimento normal, sem erro. (Tarefa 4)
5. **Coordenadas negativas e bordas** em `CaixaArena.daPosicao` → a vaga certa, e o bloco da face da caixa conta como dentro. (Tarefa 1)

## Arquivos
| Arquivo | Responsabilidade |
| --- | --- |
| `core/arena/CaixaArena.java` (novo) | Limites de uma vaga, pontos de entrada, barreira em caixa |
| `core/arena/VagasArena.java` (novo) | Reservar e liberar vagas |
| `core/arena/PlantaEscola.java` (novo) | Lista de blocos da plataforma lisa e do cenário |
| `arena/PontoRetorno.java` (novo) | Registro salvo no jogador (dimensão, posição, olhar) |
| `arena/ArenasServidor.java` (novo) | Ciclo de vida: entrar, luta acabou, sair, entrar no jogo |
| `arena/ObrasArena.java` (novo) | Montar e apagar em fatias, chunks forçados |
| `arena/RegrasArena.java` (novo) | Barreira no servidor, queda no vazio, blocos protegidos, limite da destruição, itens na morte |
| `kit/Anexos.java` | Anexo `RETORNO_ARENA` |
| `boss/LutasToji.java`, `boss/TojiEntity.java`, `core/boss/TojiBoss.java` | Invocar pela arena; sem os 64 blocos e sem ajudante |
| `kit/ContextoServidor.java` | `apagarCasca` e `arrancarBlocos` respeitam a caixa |
| `client/kit/MovimentoCliente.java` | Barreira em caixa no cliente |
| `client/arena/Escurecer.java`, `rede/` (novo pacote `EscurecerTela`) | Tela escurecendo na ida e na volta |
| `data/minerwk/dimension/arena.json`, `dimension_type/arena.json` (novos) | A dimensão |
| `arena/TestesArena.java` (novo) | GameTests |

---

## Parte 0: prova

### Tarefa 1: geometria da vaga (`CaixaArena`, `VagasArena`)
**Arquivos:** criar `src/main/java/com/minerwk/core/arena/CaixaArena.java` e `VagasArena.java`; testes em `src/test/java/com/minerwk/core/arena/`.

**Interfaces (produz):**
```java
public record CaixaArena(int indiceX, int indiceZ) {
    public static final int LADO_VAGA = 512, LARGURA = 81, ALTURA = 64, MEIA = 40;
    public static final int Y_PATIO = 64, Y_FUNDO = 56, Y_TETO = 120;   // Y_PATIO = y dos pés no pátio
    public static final int VAGAS_POR_LINHA = 64;
    public static CaixaArena daVaga(int indice);              // indice 0.. → (indice % 64, indice / 64)
    public static CaixaArena daPosicao(double x, double z);   // floorDiv por 512
    public int centroX();  public int centroZ();              // 256 + 512 * indice
    public boolean contemBloco(int x, int y, int z);          // |x-centroX| <= 40, |z-centroZ| <= 40, Y_FUNDO <= y < Y_TETO
    public Vetor entradaJogador();                            // (centroX+.5, Y_PATIO, centroZ+24.5); olha para o norte (yaw 180)
    public Vetor entradaToji();                               // 16 blocos ao norte do jogador: (centroX+.5, Y_PATIO, centroZ+8.5)
    public Vetor centro();                                    // (centroX+.5, Y_PATIO, centroZ+.5)
    public boolean abaixoDoFundo(double y);                   // y < Y_FUNDO
    /** Paredes e teto; o fundo é aberto (quem cai é tratado pela queda no vazio). */
    public Barreira.Correcao corrigir(Vetor pes, Vetor velocidade, double meiaLargura, double altura);
}
public final class VagasArena {
    public int reservar();          // menor índice livre
    public void liberar(int indice);
    public boolean ocupada(int indice);
    public void limpar();
}
```

- [ ] **Passo 1: testes que falham.** `CaixaArenaTest`:
  - `daVaga(0)` tem centro (256, 256); `daVaga(1)` (768, 256); `daVaga(64)` (256, 768).
  - `daPosicao(256, 256)` = `daVaga(0)`; `daPosicao(511.9, 0)` = vaga (0,0); `daPosicao(512, 0)` = (1,0); `daPosicao(-0.1, -0.1)` = índices (-1,-1), sem exceção.
  - `contemBloco`: (216, 56, 216) e (296, 119, 296) dentro; (215, 64, 256), (297, 64, 256), (256, 55, 256) e (256, 120, 256) fora.
  - `entradaToji()` fica 16 blocos ao norte de `entradaJogador()`, os dois dentro do pátio (|dx|, |dz| ≤ 30 do centro).
  - `corrigir`: pés em (centroX + 39.9, 64, centroZ) com velocidade +x e meia largura 0.3 → x ≤ centroX + 40.5 − 0.3 − `Barreira.MARGEM` e velocidade x ≤ 0; pés no centro → posição e velocidade iguais; pés em y = 119 subindo com altura 1.8 → y ≤ 120 − 1.8 e velocidade y ≤ 0; pés em y = 50 (abaixo do fundo) → sem correção em y.
- [ ] `VagasArenaTest`: reservar três vezes dá 0, 1, 2; liberar(1) e reservar dá 1; `ocupada`; `limpar` libera tudo; liberar índice não reservado não lança.
- [ ] **Passo 2:** `./gradlew test --tests "*arena*"` → falha por classe inexistente.
- [ ] **Passo 3:** implementar as duas classes.
- [ ] **Passo 4:** `./gradlew test --tests "*arena*"` → passa.
- [ ] **Passo 5:** commit `Arena, tarefa 1: geometria da vaga e reserva de vagas`.

### Tarefa 2: a dimensão e o comando de prova
**Arquivos:** criar `src/main/resources/data/minerwk/dimension/arena.json`, `dimension_type/arena.json`, `arena/PontoRetorno.java`, `arena/ArenasServidor.java`, `arena/TestesArena.java`; modificar `kit/Anexos.java`, `kit/ComandoTrocar.java`, `MineRWK.java`, os dois `lang`.

**Interfaces (produz):**
```java
public record PontoRetorno(ResourceLocation dimensao, double x, double y, double z, float yaw, float pitch) {
    public static final Codec<PontoRetorno> CODEC;
    public static PontoRetorno de(ServerPlayer jogador);
}
// Anexos: Supplier<AttachmentType<Optional<PontoRetorno>>> RETORNO_ARENA — salvo no disco, copyOnDeath
public final class ArenasServidor {
    public static final ResourceKey<Level> DIMENSAO;          // minerwk:arena
    public static ServerLevel nivel(MinecraftServer servidor);
    public static boolean naArena(Entity e);                  // e.level().dimension() == DIMENSAO
    static void levar(ServerPlayer jogador, CaixaArena caixa);        // guarda o retorno (se não houver) e teleporta para entradaJogador, yaw 180
    static boolean trazer(ServerPlayer jogador);                      // teleporta ao retorno e o apaga; false se não havia ou é inválido
}
```
- Dimensão: `dimension_type` com `fixed_time` **11500**, `has_skylight` true, `has_ceiling` false, `natural` false, `bed_works` false, `respawn_anchor_works` false, `min_y` 0, `height` 256, `effects` `minecraft:overworld`. `dimension` com gerador `minecraft:flat`, `layers` vazio, bioma `minecraft:the_void`, sem estruturas.
- Comando de prova em `ComandoTrocar`: `/rwk arena ir` (monta uma plataforma de pedra 61 × 61 em `daVaga(0)`, y 63, e chama `levar`) e `/rwk arena voltar` (chama `trazer`). Nível de permissão 2, como os outros comandos de teste.
- `trazer` com retorno inválido (dimensão inexistente): leva ao ponto de renascimento do jogador (`getRespawnPosition`/spawn do mundo) e devolve false.

- [ ] **Passo 1: GameTests que falham** em `TestesArena` (`@GameTestHolder`, template `vazio`, como `TestesToji`):
  - `dimensaoExiste`: `ArenasServidor.nivel(servidor)` não é nulo e `dimensionType().fixedTime()` é 11500.
  - `levarETrazerVoltaAoLugarExato`: jogador de teste (`TestesGojo.gojo(helper)`) em posição conhecida; `levar` → `naArena` é true e a posição é `entradaJogador()`; `trazer` → dimensão, x, y, z, yaw e pitch iguais aos de antes (tolerância 0.01) e o anexo fica vazio.
  - `trazerSemRetornoNaoFazNada`: devolve false e o jogador não se move.
  - `retornoInvalidoVaiParaORenascimento`: anexo com dimensão `minerwk:nao_existe` → `trazer` devolve false e o jogador fica no mundo normal.
- [ ] **Passo 2:** `./gradlew runGameTestServer` → os quatro falham.
- [ ] **Passo 3:** implementar. Se o jogador de teste do GameTest não conseguir trocar de dimensão, **parar e avisar o autor**: nesse caso os testes de ida e volta desta e das próximas tarefas viram roteiro manual em [[Pendências]], e o plano segue.
- [ ] **Passo 4:** `./gradlew build` e `./gradlew runGameTestServer` → passam. `./gradlew runServer` sobe sem erro de dimensão no log (servidor dedicado).
- [ ] **Passo 5:** commit `Arena, tarefa 2: dimensão minerwk:arena, ponto de retorno e comando de prova`.
- [ ] **Passo 6: fim da parte 0.** Diário `2026-11-10 Arena parte 0`, Roadmap e Pendências (roteiro: `/rwk arena ir`, olhar o céu de fim de tarde e o vazio em volta, `/rwk arena voltar` de vários lugares, inclusive do Nether). **Teste do autor.**

---

## Parte 1: entrada e saída

### Tarefa 3: montar e apagar em fatias (`PlantaEscola.plataforma`, `ObrasArena`)
**Arquivos:** criar `core/arena/PlantaEscola.java`, `arena/ObrasArena.java`; testes `PlantaEscolaTest`, GameTests em `TestesArena`.

**Interfaces (produz):**
```java
public final class PlantaEscola {
    public enum Material { PEDRA, TIJOLO_PEDRA, LAJE_PEDRA, ANDESITO_POLIDO, ESCADA_PEDRA, MADEIRA_ESCURA, MADEIRA_VERMELHA, TRONCO, FOLHA, LANTERNA, MURO }
    public record Bloco(int dx, int dy, int dz, Material material) {}   // relativo ao centro; dy 0 = y dos pés no pátio
    public static List<Bloco> plataforma();   // 61 × 61, dy −1 TIJOLO_PEDRA, dy −2..−4 PEDRA
    public static List<Bloco> cenario();      // tarefa 8
}
public final class ObrasArena {
    static final int POR_TICK = 4000;            // blocos postos ou apagados por tick
    static final int OLHADOS_POR_TICK = 20000;   // posições olhadas por tick na limpeza (a caixa tem ~420 mil: ~21 ticks)
    static void registrarEventos();
    /** Limpa a caixa, põe os blocos e chama aoTerminar. Mantém os chunks da vaga forçados até liberar(). */
    static void montar(ServerLevel nivel, CaixaArena caixa, List<PlantaEscola.Bloco> blocos, Runnable aoTerminar);
    /** Remove entidades que não são jogadores, apaga todos os blocos da caixa e solta os chunks. */
    static void apagar(ServerLevel nivel, CaixaArena caixa, Runnable aoTerminar);
    static boolean ocioso();
}
```
- Sem atualizar vizinhos nem derrubar itens (flags de bloco como as do `ApagadorServidor`). Folhas com `persistent = true`.

- [ ] **Passo 1: testes que falham.** `PlantaEscolaTest`: `plataforma()` tem 61·61·4 = **14884** blocos; todos com |dx|, |dz| ≤ 30 e dy em [−4, −1]; sem posição repetida. GameTests: `montarPoePlataformaEApagarLimpa` (depois de `montar`, o bloco em `centro` −1 é tijolo de pedra e `aoTerminar` rodou em até 40 ticks; um bloco de terra posto antes dentro da caixa sumiu; depois de `apagar`, a caixa só tem ar e não há entidades de item).
- [ ] **Passo 2:** rodar e ver falhar.
- [ ] **Passo 3:** implementar.
- [ ] **Passo 4:** `./gradlew build` e `runGameTestServer` → passam.
- [ ] **Passo 5:** commit `Arena, tarefa 3: planta da plataforma e obras em fatias`.

### Tarefa 4: ciclo de vida da arena e a luta do Toji dentro dela
**Arquivos:** modificar `arena/ArenasServidor.java`, `boss/LutasToji.java`, `boss/TojiEntity.java`, `boss/ContratoToji.java`, `core/boss/TojiBoss.java`, `kit/ComandoTrocar.java`, `MineRWK.java`, `lang`; remover o comando de prova `/rwk arena`.

**Interfaces (produz):**
```java
// TojiBoss
public enum Invocacao { OK, OUTRO_PERSONAGEM, SEM_NO, JA_EXISTE, NA_ARENA }
// ArenasServidor
public static void registrarEventos();
/** Reserva a vaga, guarda o retorno, monta e, pronta a arena, leva o jogador e chama aoChegar. false se ele já tem arena ou está na dimensão. */
public static boolean entrar(ServerPlayer jogador, Consumer<CaixaArena> aoChegar);
/** Chamado pelo Toji quando a luta termina. Vitória: volta em 100 ticks. Senão: volta já, se ainda estiver na arena. Depois apaga e libera a vaga. */
public static void lutaAcabou(UUID jogador, boolean venceu);
public static Optional<CaixaArena> de(UUID jogador);
```
- `LutasToji.invocar`: com o jogador na dimensão da arena ou com arena reservada → `NA_ARENA` (mensagem `mensagem.minerwk.toji.na_arena`: "Você já está na arena"). Senão, as checagens de hoje e `ArenasServidor.entrar(jogador, caixa -> criar o Toji em caixa.entradaToji(), virado para o jogador, e comecarLuta)`. Devolve `OK` logo que a entrada começa (o Contrato é gasto aí).
- Durante a montagem o jogador recebe lentidão máxima e fica invulnerável; os dois saem ao chegar. Se desmontar for preciso, `stopRiding()` antes de levar.
- `TojiEntity.encerrarLuta()` chama `ArenasServidor.lutaAcabou(invocadorId, venceu)`.
- Eventos em `ArenasServidor`: `PlayerLoggedOutEvent` (com arena reservada → cancela a entrada ou deixa o Toji encerrar; a vaga é liberada); `PlayerLoggedInEvent` (na dimensão da arena e sem arena ativa → `trazer`; se falhar, renascimento normal); `ServerStoppedEvent` (limpa os mapas e `VagasArena`).
- Jogador que troca de dimensão por comando no meio da luta: o Toji já some pela regra de hoje (`dono.level() != level()`); `lutaAcabou(false)` não o teleporta (ele não está na arena) e apaga o retorno.

- [ ] **Passo 1: testes que falham.** JUnit `TojiBossTest`: `podeInvocar` continua igual (o `NA_ARENA` é decidido no servidor). GameTests:
  - `invocarLevaParaAArenaComOToji`: depois de `LutasToji.invocar(jogador, false)` e até 60 ticks, o jogador está em `entradaJogador()` da vaga 0 e `LutasToji.de(uuid)` tem um Toji a 16 blocos.
  - `invocarDeDentroDaArenaERecusado`: segunda chamada devolve `NA_ARENA`.
  - `vitoriaTrazDeVoltaEm100Ticks`: matar o Toji → aos 90 ticks o jogador ainda está na arena; aos 110 está no ponto de origem; `despertou` é true; a vaga 0 fica livre depois da limpeza.
  - `tojiSomeTrazDeVoltaNaHora`: `toji.discard()` → jogador de volta no tick seguinte, `despertou` false.
  - `sairDuranteAMontagemLiberaAVaga` (foco 1): `entrar` e, no tick seguinte, simular a saída (o método que o evento chama) → `VagasArena` sem ocupadas depois da limpeza, jogador nunca foi levado, anexo de retorno vazio.
  - `duasLutasFicamEmVagasDiferentes`: dois jogadores de teste invocam → caixas com índices 0 e 1.
  - `entrarNoJogoNaArenaSemLutaVolta` (foco 4): jogador posto à mão na dimensão com retorno salvo → o método do login o traz; com retorno inválido → mundo normal.
- [ ] **Passo 2:** rodar e ver falhar.
- [ ] **Passo 3:** implementar.
- [ ] **Passo 4:** `./gradlew build` e `runGameTestServer` → passam (os GameTests antigos do Toji continuam passando; os que dependiam de o Toji nascer a 8 blocos no mundo aberto passam a criar o Toji direto, como `TestesToji.toji(...)` já faz).
- [ ] **Passo 5:** commit `Arena, tarefa 4: a luta do Toji acontece na arena`.

### Tarefa 5: morrer na arena não perde itens
**Arquivos:** criar `arena/RegrasArena.java`; modificar `MineRWK.java`.

**Interfaces (produz):** `RegrasArena.registrarEventos()`.
- `LivingDeathEvent` de jogador na arena: guarda cópia do inventário e do XP (nível, progresso, total) por UUID. `LivingDropsEvent` e `LivingExperienceDropEvent` desse jogador: cancelados. `PlayerEvent.Clone` com morte: devolve o inventário e o XP guardados e esquece. `ServerStoppedEvent`: limpa.

- [ ] **Passo 1: GameTests que falham:** `morrerNaArenaMantemItensEXp` (jogador na arena com 3 diamantes e 10 níveis; `kill`; depois do renascimento simulado pelo evento Clone ele tem os 3 diamantes e 10 níveis, e não há item caído na caixa); `morrerForaDaArenaDerrubaItens` (o comportamento normal não muda); `morrerDepoisDaVitoriaNaoTeleporta` (foco 2: matar o Toji, matar o jogador aos 40 ticks → aos 110 ticks ele não foi teleportado para o retorno, `despertou` é true e o anexo de retorno está vazio).
- [ ] **Passo 2 a 4:** falhar, implementar (em `lutaAcabou`, a volta agendada é cancelada se o jogador morrer), passar.
- [ ] **Passo 5:** commit `Arena, tarefa 5: morte na arena sem perder itens`.
- [ ] **Passo 6: fim da parte 1.** Diário, Roadmap, Pendências (roteiro: Contrato e `/rwk toji`; vencer e voltar; morrer e conferir os itens; sair do jogo no meio e entrar; `/tp` para fora no meio; dois jogadores ao mesmo tempo, se der). **Teste do autor.**

---

## Parte 2: regras

### Tarefa 6: barreira em caixa e queda no vazio
**Arquivos:** modificar `arena/RegrasArena.java`, `client/kit/MovimentoCliente.java`.

- **Cliente:** em `MovimentoCliente.aoTickJogador`, se a dimensão do jogador é `minerwk:arena`, aplicar `CaixaArena.daPosicao(x, z).corrigir(pes, velocidade, meiaLargura, altura)` ao próprio jogador (posição, velocidade e `onGround` como o domínio já faz).
- **Servidor** (`RegrasArena`, a cada tick, para jogadores com arena ativa e o Toji deles): fora da caixa da **sua** vaga por mais de 1,5 bloco (mesma tolerância do domínio) ou em outra vaga → de volta ao ponto mais próximo dentro; para o Toji, sem tolerância. `abaixoDoFundo(y)` → refazer o 5 × 5 de tijolo de pedra em `centro` −1, levar para `centro()`, zerar a queda e dar **4** de dano genérico.
- Jogadores em modo espectador não são corrigidos.

- [ ] **Passo 1: GameTests que falham:** `foraDaCaixaVoltaParaDentro` (foco 3: jogador teleportado para centroX + 60 → no tick seguinte está com x ≤ centroX + 40.5); `tojiNaoSaiDaCaixa` (Toji movido para fora → volta); `quedaNoVazioVoltaAoCentro` (apagar o chão, pôr o jogador em y = 50 → está em `centro()`, com 4 a menos de vida e com chão embaixo); `espectadorPassa`.
- [ ] **Passo 2 a 4:** falhar, implementar, passar.
- [ ] **Passo 5:** commit `Arena, tarefa 6: barreira em caixa e queda no vazio`.

### Tarefa 7: blocos protegidos, destruição limitada, fim dos 64 blocos e do ajudante
**Arquivos:** modificar `arena/RegrasArena.java`, `arena/ArenasServidor.java`, `kit/ContextoServidor.java`, `core/boss/TojiBoss.java`, `boss/TojiEntity.java`, testes.

**Interfaces (produz):**
```java
// ArenasServidor
/** Fora da dimensão da arena: sempre true. Dentro: só dentro da caixa da vaga daquela posição. */
public static boolean podeMexer(Level nivel, BlockPos pos);
// TojiBoss
public enum Fim { CONTINUA, INVOCADOR_FORA, TEMPO }
public static Fim fim(boolean invocadorPresente, int ticksDeLuta);   // sem distância
```
- `RegrasArena`: `BlockEvent.BreakEvent` e `BlockEvent.EntityPlaceEvent` de jogador na arena que não está em criativo → cancelados.
- `ContextoServidor.apagarCasca` e `arrancarBlocos`: o predicado ganha `&& ArenasServidor.podeMexer(nivel, pos)`.
- `TojiBoss`: remover `DISTANCIA_MAXIMA` e `Fim.LONGE`; o `FOLLOW_RANGE` do Toji passa a **96** (cobre a diagonal da caixa). `TojiEntity`: remover `revide`, `revideAte`, `TICKS_DE_REVIDE` e o desvio de alvo em `hurt` e no tick (linhas ~253 a 257 e ~758 a 761). A mensagem `toji.sumiu` continua.

- [ ] **Passo 1: testes que falham.** JUnit `TojiBossTest`: `fim(true, 100)` = CONTINUA; `fim(false, 0)` = INVOCADOR_FORA; `fim(true, TEMPO_LIMITE)` = TEMPO (apagar os casos de `LONGE`). GameTests: `jogadorNaoQuebraNemPoeNaArena` (sobrevivência: eventos cancelados; criativo: não); `apagarNaoPassaDaCaixa` (blocos postos em centroX + 40 e centroX + 41, y 70; `ctx.apagarCasca(centro deslocado, 0, 6)` cobrindo os dois → o de dentro some, o de fora fica); `arrancarNaoPassaDaCaixa` (o mesmo com `arrancarBlocos`); `foraDaArenaApagarNaoMuda` (os GameTests antigos do Roxo continuam passando).
- [ ] **Passo 2 a 4:** falhar, implementar, passar.
- [ ] **Passo 5:** commit `Arena, tarefa 7: blocos protegidos, destruição na caixa, sem os 64 blocos e sem ajudante`.
- [ ] **Passo 6: fim da parte 2.** Diário, Roadmap, Pendências (roteiro: tentar sair andando, com pérola e com o impulso dos golpes; destruir o chão com o Roxo e cair; R no canto da arena; quebrar bloco à mão; o Toji não some mais ao se afastar). **Teste do autor.**

---

## Parte 3: cenário

### Tarefa 8: a planta da entrada da Escola
**Arquivos:** modificar `core/arena/PlantaEscola.java`, `arena/ObrasArena.java` (material → bloco), `arena/ArenasServidor.java` (usar `cenario()`); teste `PlantaEscolaTest`; script de prévia `ferramentas/previa_arena.py`.

- `cenario()` = `plataforma()` com o piso em faixas (tijolo de pedra, com uma faixa central de 5 de largura de andesito polido de norte a sul e bordas de laje) mais:
  - **Norte:** escadaria de 21 de largura subindo **8** blocos de dz −31 a −38, patamar em dz −39 e −40, torii grande (vão de 9, 9 de altura) no patamar.
  - **Caminho central:** dois torii (vão livre de **7** de altura e 7 de largura) em dz −12 e dz +12, de madeira vermelha com a viga de cima de madeira escura.
  - **Leste, oeste e sul:** muro de pedra de 2 de altura em |dx| = 31 e dz = +31; lanternas sobre o muro a cada 6 blocos; na faixa de 32 a 40, chão de pedra em dy −1 e 12 árvores (tronco de 5 a 7, copa de folhas de raio 2 a 3), em posições fixas (sem aleatório, para o teste e para as lutas serem iguais).
- Mapa de materiais em `ObrasArena`: PEDRA → `stone`, TIJOLO_PEDRA → `stone_bricks`, LAJE_PEDRA → `stone_brick_slab`, ANDESITO_POLIDO → `polished_andesite`, ESCADA_PEDRA → `stone_brick_stairs` (virada para o sul), MADEIRA_ESCURA → `dark_oak_planks`, MADEIRA_VERMELHA → `mangrove_planks`, TRONCO → `spruce_log`, FOLHA → `spruce_leaves` (persistente), LANTERNA → `lantern`, MURO → `stone_brick_wall`.

- [ ] **Passo 1: testes que falham** em `PlantaEscolaTest`: todo bloco de `cenario()` cabe na caixa (|dx|, |dz| ≤ 40, dy em [−8, 55]); sem posição repetida; total ≤ **40000** blocos (montagem em até 10 ticks); o pátio (|dx|, |dz| ≤ 30, dy 0 a 6) está livre, exceto as colunas dos dois torii do caminho; `entradaJogador` e `entradaToji` têm chão em dy −1 e ar em dy 0 e 1; existe escada em dz −31 e patamar em dy 7 no dz −39.
- [ ] **Passo 2 a 4:** falhar, implementar, passar.
- [ ] **Passo 5:** `ferramentas/previa_arena.py` desenha a planta vista de cima e de lado em `previas/arena.png` a partir de um despejo de texto da planta (um teste JUnit marcado para rodar à mão escreve `build/arena.txt`). Mostrar a imagem ao autor antes do teste no jogo.
- [ ] **Passo 6:** commit `Arena, tarefa 8: cenário da entrada da Escola Jujutsu`.

### Tarefa 9: tela escurecendo na ida e na volta
**Arquivos:** criar `rede/EscurecerTela.java` (pacote servidor → cliente: `int ticksEscurecendo, int ticksPreto, int ticksClareando`), `client/arena/Escurecer.java` (camada de GUI acima de tudo, como a da cutscene); modificar `rede/Pacotes.java`, `client/MineRWKClient.java`, `arena/ArenasServidor.java`.

- Ida: ao começar a montagem, `EscurecerTela(10, ticks da montagem, 15)`; o teleporte acontece com a tela preta. Volta (vitória e derrota com o jogador vivo): `EscurecerTela(10, 5, 15)` e o teleporte 10 ticks depois.
- A opção `cutscenes = false` da configuração do cliente **não** desliga o escurecimento (ele esconde o teleporte).
- Lógica pura da opacidade em `core/arena/CurvaEscurecer.opacidade(int tick, int a, int b, int c)` → 0..1.

- [ ] **Passo 1: teste que falha** `CurvaEscurecerTest`: `opacidade(0, 10, 20, 15)` = 0; `(10, …)` = 1; `(29, …)` = 1; `(45, …)` = 0; no meio das rampas é linear; tick negativo ou depois do fim = 0.
- [ ] **Passo 2 a 4:** falhar, implementar, passar. Ajustar `vitoriaTrazDeVoltaEm100Ticks` para 110 ticks (os 10 do escurecimento).
- [ ] **Passo 5:** commit `Arena, tarefa 9: tela escurecendo na ida e na volta`.
- [ ] **Passo 6: fim da parte 3.** Diário, Roadmap (marcar o item da arena), Pendências (roteiro: olhar o cenário inteiro, os torii não atrapalham o Desaparecer e a Execução, o R sobe sem bater em nada, FPS na montagem, a tela preta esconde o teleporte, Iris). Atualizar [[Toji]] e o spec ([[2026-11-10 Toji e o despertar do Gojo]]: escolha 6 "qualquer um ajuda" e a regra dos 64 blocos foram trocadas pela arena). **Teste do autor**, depois revisão final por um revisor separado e merge do `arena-toji` na `main`.
