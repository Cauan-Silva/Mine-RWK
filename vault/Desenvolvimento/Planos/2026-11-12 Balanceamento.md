---
tags: [desenvolvimento, plano]
---
# Balanceamento: plano de implementação

> Execução nativa (como nas partes anteriores), no branch `balanceamento`, TDD, revisão final por um revisor separado. Os passos usam `- [ ]`.

**Objetivo:** a camada de poder comum (vida, redução de dano, multiplicadores) com os números na config do servidor, e os ajustes pontuais do Gojo, do Ichigo e da arena.
**Arquitetura:** as contas ficam em `core/kit/Poder` (puro, JUnit). A config fica em `ConfigServidor` (comum, tipo SERVER). O servidor aplica: a vida e a redução nos eventos do jogador, o multiplicador nos métodos de dano do `ContextoServidor`, e o dos bosses no nascimento e no `ferir` de cada um.
**Stack:** NeoForge 21.1.265, `ModConfigSpec`, JUnit 5, GameTest.
**Spec:** [[2026-11-12 Balanceamento]] (em `vault/Desenvolvimento/Specs/`).

## Restrições globais
- Padrões da config, exatos do spec: `vidaExtra` 20, `reducaoDeDano` 0,5, `multiplicadorDosGolpes` 3, `vidaDosBosses` 2, `danoDosBosses` 2.
- A redução não vale para `/kill` nem para o vazio (a tag `BYPASSES_INVULNERABILITY`).
- O multiplicador vale só para os golpes do kit, não para o clique.
- Nada de `client/` no código comum (CLAUDE.md). Build verde e `runGameTestServer` verde ao fim de cada tarefa.
- Commits terminam com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Foco de revisão
1. **Trocar para "sem personagem" ou para outro personagem:** a vida extra sai e a vida atual não fica acima do novo máximo. (Tarefa 2)
2. **Morrer e renascer:** o renascido tem os 40 de vida máxima de novo. (Tarefa 2)
3. **Config fora da faixa** (redução 1,5, multiplicador negativo): limitada pela própria config, sem dano negativo nem cura. (Tarefa 1)
4. **Jogador sem personagem** apanha normal. (Tarefa 2)
5. **Boss que já existia** quando o servidor carregou o mundo (salvo no disco) fica com a vida multiplicada só uma vez. (Tarefa 3)

---

### Tarefa 1: as contas e a config
**Arquivos:** criar `src/main/java/com/minerwk/core/kit/Poder.java`, `src/main/java/com/minerwk/ConfigServidor.java`, `src/test/java/com/minerwk/core/kit/PoderTest.java`; registrar a config em `MineRWK.java` (`container.registerConfig(ModConfig.Type.SERVER, ConfigServidor.SPEC)`; o construtor passa a receber `ModContainer`).

**Interfaces (produz):**
```java
public record Poder(double vidaExtra, double reducaoDeDano, double multiplicadorDosGolpes, double vidaDosBosses, double danoDosBosses) {
    public static final Poder PADRAO = new Poder(20, .5, 3, 2, 2);
    public float recebido(float dano);            // dano * (1 - reducao), nunca negativo
    public float golpe(float dano);               // dano * multiplicadorDosGolpes
    public float vidaDoBoss(float vida);          // vida * vidaDosBosses
    public float danoDoBoss(float dano);          // dano * danoDosBosses
}
// ConfigServidor: ModConfigSpec com os cinco valores; static Poder poder() lê os valores
// (com a config ainda não carregada, devolve Poder.PADRAO).
```
Faixas na config: `vidaExtra` 0 a 200; `reducaoDeDano` 0 a 0,95; os três multiplicadores 0,1 a 20.

- [ ] **Passo 1: `PoderTest`**: `PADRAO.recebido(30)` = 15; `PADRAO.golpe(30)` = 90; `PADRAO.vidaDoBoss(240)` = 480; `PADRAO.danoDoBoss(10)` = 20; `new Poder(0, 0, 1, 1, 1).recebido(7)` = 7; `recebido(-3)` = 0.
- [ ] **Passo 2:** `./gradlew test --tests "*PoderTest*"` falha (classe não existe).
- [ ] **Passo 3:** implementar `Poder` e `ConfigServidor`.
- [ ] **Passo 4:** `./gradlew build` verde.
- [ ] **Passo 5:** commit `Balanceamento, tarefa 1: as contas da camada de poder e a config do servidor`.

### Tarefa 2: a camada no jogador e nos golpes
**Arquivos:** criar `src/main/java/com/minerwk/kit/PoderServidor.java`; modificar `kit/ServicoKits.java` (ao definir personagem), `kit/ContextoServidor.java` (métodos de dano), `MineRWK.java` (registrar eventos); GameTests em `kit/TestesPoder.java` (novo).

**Interfaces (produz):**
```java
public final class PoderServidor {
    public static void registrarEventos();
    /** Põe ou tira o modificador de vida conforme o jogador tenha personagem; a vida atual nunca passa do novo máximo. */
    public static void aplicarVida(ServerPlayer jogador);
}
```
- Vida: modificador `minerwk:vida_do_personagem` (`ADD_VALUE`) em `MAX_HEALTH`. `aplicarVida` é chamado em `ServicoKits.definirPersonagem` (com ou sem personagem), ao entrar no jogo e no `PlayerEvent.Clone`.
- Redução: `LivingDamageEvent.Pre`, jogador com personagem, fonte sem `DamageTypeTags.BYPASSES_INVULNERABILITY` → `setNewDamage(poder.recebido(...))`.
- Golpes: `danoEmArea`, `danoSeguidoEmArea`, `danoEm`, `danoUmaVez`, `danoDiretoUmaVez`, `ferir` e `danoEmArco` do `ContextoServidor` multiplicam o dano por `poder.golpe` antes de aplicar. O clique (`ArmaServidor`) não muda.

- [ ] **Passo 1: GameTests que falham** em `TestesPoder`:
  - `golpeDoKitTiraTresVezes`: zumbi sem armadura com 40 de vida (`MAX_HEALTH` 40); `new ContextoServidor(jogador).danoEm(centro do zumbi, 1, 4)` → o zumbi fica com 28.
  - `personagemTemQuarentaDeVida`: Gojo → `getMaxHealth()` = 40; trocar para nenhum personagem → 20 e a vida atual ≤ 20 (foco 1).
  - `renascidoMantemAVidaExtra`: `restoreFrom` como em `TestesArena.renascer`, depois o evento → 40 (foco 2).
  - `golpeDoWardenTiraQuinze`: jogador Gojo sem armadura, vida 40; `jogador.hurt(warden.damageSources().mobAttack(warden), 30)` → 25.
  - `semPersonagemApanhaNormal`: o mesmo sem personagem → 30 a menos (foco 4).
  - `killIgnoraARedução`: `jogador.kill()` mata.
- [ ] **Passo 2:** `./gradlew runGameTestServer` → os novos falham.
- [ ] **Passo 3:** implementar.
- [ ] **Passo 4:** os GameTests antigos que conferem um dano exato de golpe passam a usar `ConfigServidor.poder().golpe(x)` no esperado. `./gradlew build` e `runGameTestServer` verdes.
- [ ] **Passo 5:** commit `Balanceamento, tarefa 2: vida extra, redução de dano e multiplicador dos golpes`.

### Tarefa 3: os bosses
**Arquivos:** modificar `boss/TojiEntity.java`, `boss/ZangetsuEntity.java`, `boss/RagnarokEntity.java`; GameTests em `kit/TestesPoder.java`.

- Vida: no `comecarLuta` (o ponto comum aos três), `getAttribute(MAX_HEALTH).setBaseValue(poder.vidaDoBoss(VIDA))` e `setHealth(getMaxHealth())`. Como o valor vem da constante e não do atributo atual, aplicar duas vezes dá o mesmo (foco 5).
- Dano: o `ferir` privado de cada um passa `poder.danoDoBoss(dano)` ao `hurt`.
- [ ] **Passo 1: GameTests que falham:** `bossesNascemComODobroDaVida` (os três, depois de `comecarLuta`, `getMaxHealth()` = 2 × `VIDA`, e chamar `comecarLuta` de novo não muda); `golpeDoTojiChegaComoHoje` (soco do Toji, 6 × 2 × 0,5 = 6 no jogador sem armadura).
- [ ] **Passo 2 a 4:** falhar, implementar, build e GameTests verdes (os GameTests dos bosses que contam vida ou dano exatos passam a usar a config).
- [ ] **Passo 5:** commit `Balanceamento, tarefa 3: vida e dano dos bosses pela config`.

### Tarefa 4: Gojo
**Arquivos:** modificar `core/personagens/gojo/Gojo.java`, `core/personagens/gojo/TecnicaReversa.java` (ou uma passiva nova `Punho` na lista de passivas do Gojo); testes em `src/test/java/com/minerwk/core/personagens/gojo/`.

- `public static final Arma PUNHO = new Arma(6, 4, 0)`, posta por uma passiva (`ctx.arma(PUNHO)`) a cada tick, como a Zangetsu do Ichigo. A passiva não pode ficar trancada: as passivas do Gojo são trancadas até o despertar, então o punho entra numa passiva que a tranca não alcança, ou o kit passa a separar as duas. Decisão do executor, registrada no diário.
- Vermelho: `ctx.arremessar(posicao, 4, 1.2 + .8 * forca, 3, null)`.
- [ ] **Passo 1: JUnit que falha:** `cliqueDoGojoTemOPunho` (depois de um tick, sem despertar, a arma pedida é `PUNHO` com 6 de dano); `vermelhoEmpurraMenos` (forca 1 → arremesso 2,0; forca 0 → 1,2).
- [ ] **Passo 2 a 4:** falhar, implementar, build verde.
- [ ] **Passo 5:** commit `Balanceamento, tarefa 4: punho do Gojo e Vermelho mais leve`.

### Tarefa 5: Ichigo
**Arquivos:** modificar `core/personagens/ichigo/Ichigo.java` e `core/personagens/ichigo/Reiatsu.java`; ajustar `IchigoTest` e `ReiatsuTest`.

| Constante / golpe | Novo |
| --- | --- |
| `Reiatsu.DRENO` | 1/20 por tick |
| `Reiatsu.GANHO` | 2,5/20 |
| `Reiatsu.GANHO_CALMO` | 5/20 |
| Getsuga Tenshō | 12 |
| Shunpo | 10 |
| Getsuga no chão | 20 |
| Arremesso | 12 |
| Bankai | 40 |
| Getsuga negro | 10 |
| Shunpo do Bankai | 6 |
| Getsuga na lâmina | 20 |

Máscara (40) e Mugetsu (100) não mudam.
- [ ] **Passo 1:** um teste `custosDoReiatsuDepoisDoTeste` com a tabela acima (custo de cada id) e os três ritmos do `Reiatsu`; os testes antigos que contam reiatsu passam a usar as constantes.
- [ ] **Passo 2 a 4:** falhar, implementar, build e GameTests verdes.
- [ ] **Passo 5:** commit `Balanceamento, tarefa 5: Ichigo gasta menos reiatsu`.

### Tarefa 6: arena indestrutível
**Arquivos:** modificar `arena/ArenasServidor.java` (`podeMexer` devolve `false` em toda a dimensão das arenas), `arena/TestesArena.java`.
- [ ] **Passo 1:** os testes `apagarNaoPassaDaCaixa` e `arrancarNaoPassaDaCaixa` viram `apagarNaArenaNaoMexe` e `arrancarNaArenaNaoMexe`: o bloco de dentro da caixa também fica. Fora da dimensão nada muda (`explosaoRoxaApagaAsCascas` continua passando).
- [ ] **Passo 2 a 4:** falhar, implementar, GameTests verdes.
- [ ] **Passo 5:** commit `Balanceamento, tarefa 6: arenas indestrutíveis`.

### Tarefa 7: fechar
- [ ] Diário `2026-11-12 Balanceamento`, Roadmap, Pendências (roteiro: Warden no Normal e no Difícil; os três bosses; o clique do Gojo; o Vermelho; o Ichigo gastando reiatsu; a config `minerwk-server.toml` em `saves/<mundo>/serverconfig/`), spec marcado como aprovado.
- [ ] `./gradlew build` e `runGameTestServer` verdes; revisão final por um revisor separado; commit.
