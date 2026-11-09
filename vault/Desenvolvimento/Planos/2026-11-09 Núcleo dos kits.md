---
tags: [desenvolvimento, plano]
---
# Núcleo dos kits: plano de implementação

> **Para agentes:** usar superpowers:subagent-driven-development ou superpowers:executing-plans, tarefa por tarefa. Passos com `- [ ]`.

**Objetivo:** base comum dos personagens (Roadmap 1.3): habilidades com carga/disparo/fim, recurso próprio, recargas, interrupção, dados do jogador, rede, teclas e tela simples de escolha, provada com um personagem `teste`.

**Arquitetura:** lógica pura em `core/kit/` (JUnit, sem Minecraft); ponte no servidor em `kit/` (attachment, tick, contexto, comando); cliente em `client/kit/` (teclas, tela, recepção dos payloads); payloads em `rede/`.

**Stack:** Java 21, NeoForge 21.1.265 (Data Attachments, `CustomPacketPayload`, `RegisterKeyMappingsEvent`, GameTest), JUnit 5, Player Animation Library e Photon2 (já existentes em `client/`).

**Spec:** [[2026-11-09 Núcleo dos kits]] (em `vault/Desenvolvimento/Specs/`).

## Restrições globais
- Comentários, nomes e mensagens em português; commits terminam com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Nada de classes do Minecraft em `core/`. Photon e Player Animation Library só em `client/`.
- Tempos em ticks (20 por segundo). Recarga de punição = `recarga / 4` (divisão inteira).
- O servidor decide tudo; o cliente só envia apertou/soltou e o pedido de escolha.
- Verificação: `./gradlew build` (compila e roda JUnit).

## Foco de revisão
1. Tecla de outra habilidade apertada durante uma execução → ignorada (não troca nem enfileira). Teste na Tarefa 3.
2. Jogador sai ou morre no meio da carga → execução descartada sem cobrar custo nem punir. Teste na Tarefa 3 (`descartar()`).
3. `EscolherPersonagem` com id inexistente ou quando já tem personagem → recusado sem erro. GameTest na Tarefa 7.
4. Tecla sem habilidade no personagem (ex.: R no `teste`) → nada acontece. Teste na Tarefa 3.
5. Valor salvo do recurso fora de [0, máximo] (arquivo editado ou antigo) → limitado ao carregar. Teste na Tarefa 2.

---

### Tarefa 1: tipos base e `Execucao`
**Arquivos:** criar em `src/main/java/com/minerwk/core/kit/`: `Tecla.java`, `Numeros.java`, `Contexto.java`, `Habilidade.java`, `Execucao.java`; teste `src/test/java/com/minerwk/core/kit/ExecucaoTest.java`.

**Produz:**
- `enum Tecla { Z, X, C, V, G, R }`
- `record Numeros(int cargaMin, int cargaMax, int disparo, int fim, boolean segurar, double custo, int recarga)` com validação no construtor (nada negativo, `cargaMax >= cargaMin`) e `static Numeros instantanea(int carga, int disparo, int fim, double custo, int recarga)` (`cargaMin = cargaMax = carga`, `segurar = false`).
- `interface Contexto {}` (vazia; cresce na Tarefa 6).
- `interface Habilidade { String id(); Numeros numeros(); default void aoIniciarCarga(Contexto c){} default void aoDisparar(Contexto c, float forca){} default void aoTerminar(Contexto c){} }`
- `final class Execucao`: `Execucao(Habilidade h)`, `Fase fase()` (`enum Fase { CARGA, DISPARO, FIM, ACABOU }`), `Habilidade habilidade()`, `void soltar()`, `Evento tick()`. `sealed interface Evento` com `Evento.NADA`, `Evento.TERMINOU` e `record Disparou(float forca)`. Interromper é papel do `KitDoJogador`.

**Regras do tick** (contador `t` de ticks na fase):
- CARGA: incrementa `t`. Dispara (vai a DISPARO e devolve `Disparou`) quando `t >= cargaMax`, ou quando `segurar`, soltou e `t >= cargaMin`. Força: não segurar → 1; segurar → `clamp((t - cargaMin) / (float)(cargaMax - cargaMin), 0, 1)` (1 se `cargaMax == cargaMin`).
- DISPARO dura `disparo` ticks, depois FIM dura `fim` ticks, depois ACABOU devolvendo `TERMINOU`.

- [ ] **Passo 1: testes** em `ExecucaoTest`:
  - `instantaneaDisparaNoTickDaCargaComForca1`: `instantanea(3,2,2,0,0)`; ticks 1–2 → `NADA`; tick 3 → `Disparou(1f)`.
  - `instantaneaTerminaDepoisDeDisparoEFim`: após o disparo, o 4º tick devolve `TERMINOU` e `fase()==ACABOU`.
  - `segurarSoltandoNoMeioDaForcaProporcional`: `Numeros(10,30,0,0,true,0,0)`; 20 ticks, `soltar()`, tick → `Disparou` com força 0,5 (tolerância 0,06).
  - `segurarChegandoNaCargaMaximaDisparaSozinho`: sem soltar, o 30º tick → `Disparou(1f)`.
  - `soltarAntesDaCargaMinimaEsperaAMinima`: solta no tick 2; até o 9º → `NADA`; 10º → `Disparou(0f)`.
  - `numerosInvalidosLancamExcecao`: `cargaMax < cargaMin` e custo negativo → `IllegalArgumentException`.
- [ ] **Passo 2:** `./gradlew test --tests "*ExecucaoTest"` → falha (classes não existem).
- [ ] **Passo 3:** implementar os tipos.
- [ ] **Passo 4:** mesmo comando → passa.
- [ ] **Passo 5:** commit "Núcleo: Execucao e tipos base das habilidades".

### Tarefa 2: `Recurso`, `EstadoDoJogador` e `Personagem`
**Arquivos:** criar `core/kit/Recurso.java`, `EstadoDoJogador.java`, `Personagem.java`, `core/kit/teste/RecursoFolego.java`; modificar `core/kit/Recargas.java`; testes `core/kit/teste/RecursoFolegoTest.java` e acréscimo em `RecargasTest.java`.

**Produz:**
- `interface Recurso { double atual(); double maximo(); boolean pode(double custo); void gastar(double custo); void tick(EstadoDoJogador e); double salvar(); void carregar(double valor); }` (`carregar` limita a [0, máximo]).
- `record EstadoDoJogador(boolean parado, boolean apanhou, boolean causouDano)`.
- `record Personagem(String id, String nome, String obra, Supplier<Recurso> recurso, Map<Tecla, Habilidade> habilidades)` com `Optional<Habilidade> naTecla(Tecla t)`; o construtor copia o mapa.
- `Recargas`: `Map<String, Long> salvar()` (cópia) e `void carregar(Map<String, Long>)`.
- `RecursoFolego`: máximo 100, começa cheio; `tick` soma 0,5 parado e 0,05 andando; `gastar` lança `IllegalStateException` se `!pode`.

- [ ] **Passo 1: testes:** `encheParadoMaisRapido` (de 50: 10 ticks parado → 55; 10 andando → 50,5), `naoPassaDoMaximo`, `carregarLimitaValor` (−5 → 0; 999 → 100), `gastarSemTerLanca`; em `RecargasTest`: `salvarECarregarPreservaTicks`.
- [ ] **Passo 2:** `./gradlew test` → falha. **Passo 3:** implementar. **Passo 4:** passa.
- [ ] **Passo 5:** commit "Núcleo: Recurso, Personagem e salvar recargas".

### Tarefa 3: `KitDoJogador`
**Arquivos:** criar `core/kit/KitDoJogador.java`; teste `KitDoJogadorTest.java` (habilidades falsas que contam chamadas dos ganchos e um `Recurso` falso).

**Produz:**
- `KitDoJogador(Personagem p)` (recurso por `p.recurso().get()`); `personagem()`, `recurso()`, `recargas()`, `Optional<Execucao> execucao()`.
- `boolean apertar(Tecla t, long agora)`: começa se não há execução, a tecla tem habilidade, a recarga está pronta e `recurso.pode(custo)`; chama `aoIniciarCarga`.
- `void soltar(Tecla t)`: repassa só se a execução for da habilidade dessa tecla.
- `void atordoar(long agora)`: se em CARGA, descarta e inicia recarga `recarga / 4`; fora da carga, nada.
- `void descartar()`: descarta sem cobrar nem punir.
- `List<Evento> tick(long agora, EstadoDoJogador e, Contexto c)`: `recurso.tick(e)`; avança a execução; em `Disparou`: se `!recurso.pode(custo)` → `atordoar(agora)`; senão `gastar`, `recargas.iniciar(id, agora, recarga)` e `aoDisparar(c, forca)`; em `TERMINOU`: `aoTerminar(c)` e limpa.

- [ ] **Passo 1: testes:** `custoCobradoSoNoDisparo`, `recargaComecaNoDisparo`, `naoComecaEmRecargaOuSemRecurso`, `atordoarNaCargaDaPunicaoDeUmQuarto` (recarga 40 → pronta só em `agora + 10`, custo intacto), `atordoarDepoisDoDisparoNaoFazNada`, `outraTeclaDuranteExecucaoEhIgnorada`, `teclaSemHabilidadeNaoFazNada`, `descartarNaoCobraNemPune`, `semRecursoNoDisparoViraInterrupcao`, `ganchosChamadosNaOrdem`.
- [ ] **Passos 2–4:** falha → implementar → passa.
- [ ] **Passo 5:** commit "Núcleo: KitDoJogador com custo, recargas e interrupção".

### Tarefa 4: personagem `teste` e registro
**Arquivos:** criar `core/kit/teste/PersonagemTeste.java`, `core/kit/Personagens.java`; teste `PersonagemTesteTest.java`.

**Produz:**
- `Personagens`: `registrar(Personagem)`, `Optional<Personagem> porId(String)`, `Collection<Personagem> todos()` (ordem de registro), `Collection<Personagem> escolhiveis()` (esconde `teste` quando há outro).
- `PersonagemTeste.criar()`: id `teste`, nome "Personagem de teste", obra "Mine-RWK", recurso `RecursoFolego`. Z = `teste_golpe` (`instantanea(5, 2, 5, 20, 40)`); X = `teste_carga` (`Numeros(10, 40, 2, 10, true, 30, 100)`). Os ganchos ganham corpo na Tarefa 6.

- [ ] **Passo 1:** testes `personagemTesteTemZeX`, `escolhiveisEscondeTesteQuandoHaOutro`, `idDesconhecidoDevolveVazio`.
- [ ] **Passos 2–4:** falha → implementar → passa.
- [ ] **Passo 5:** commit "Núcleo: personagem de teste e registro de personagens".

### Tarefa 5: dados do jogador e serviço no servidor
**Arquivos:** criar `kit/DadosKit.java`, `kit/Anexos.java`, `kit/ServicoKits.java`, `kit/ComandoTrocar.java`; modificar `MineRWK.java`.

**Produz:**
- `DadosKit`: `String personagem` (`""` = nenhum), `double recurso`, `Map<String, Long> recargas`; `Codec<DadosKit> CODEC`.
- `Anexos.DADOS_KIT`: `AttachmentType<DadosKit>` com `.serialize(CODEC).copyOnDeath()`, registrado por `DeferredRegister` de `NeoForgeRegistries.Keys.ATTACHMENT_TYPES`.
- `ServicoKits`: `Optional<KitDoJogador> kit(ServerPlayer)` (cria do attachment na primeira chamada; cache por UUID); `void definirPersonagem(ServerPlayer, Personagem)` (zera recurso e recargas, salva, sincroniza); ouvintes de `PlayerTickEvent.Post` (só servidor; `parado` = velocidade horizontal < 0,01; `apanhou/causouDano` marcados em `LivingDamageEvent.Post`; chama `kit.tick`; salva no attachment a cada 20 ticks e ao sair), `PlayerLoggedOutEvent` e `PlayerEvent.Clone` (`descartar()` e limpa o cache). **Correção da revisão:** morto ou espectador não usa habilidade e a execução é descartada no tick (`podeAgir`).
- `/rwk trocar <jogador> <personagem>` (nível 2, sugere ids de `Personagens.todos()`), em `RegisterCommandsEvent`.
- `MineRWK`: registra `Anexos.REGISTRO`, `Personagens.registrar(PersonagemTeste.criar())` e os ouvintes.

- [ ] **Passo 1:** `./gradlew build` → passa.
- [ ] **Passo 2:** commit "Servidor: dados do kit no jogador, tick e comando de troca".

### Tarefa 6: `Contexto` no servidor e rede
**Arquivos:** modificar `core/kit/Contexto.java` e `PersonagemTeste.java`; criar `kit/ContextoServidor.java`, `rede/Pacotes.java`, `rede/UsarTecla.java`, `rede/EscolherPersonagem.java`, `rede/SincronizarKit.java`, `rede/TocarApresentacao.java`.

**Produz:**
- `Contexto`: `void danoEmArea(double raio, float dano)` (em volta do jogador, exceto ele) e `void apresentacao(String animacao, String efeito, String som)` (ids do mod ou vazio).
- `ContextoServidor(ServerPlayer)`; `apresentacao` usa `PacketDistributor.sendToPlayersTrackingEntityAndSelf`.
- Payloads (`record` + `StreamCodec`, `RegisterPayloadHandlersEvent`, versão `"1"`):
  - `UsarTecla(Tecla tecla, boolean apertou)` C→S → `apertar`/`soltar`.
  - `EscolherPersonagem(String id)` C→S → aceita só se o jogador não tem personagem e o id está em `escolhiveis()`; senão ignora (log em debug).
  - `SincronizarKit(String personagem, double recurso, double maximo, Map<String, Long> recargas, List<String> escolhiveis)` S→jogador; `escolhiveis` como `"id|nome|obra"`, preenchido só sem personagem. Enviado ao entrar, ao escolher/trocar, quando uma recarga começa e a cada 10 ticks se o recurso mudou.
  - `TocarApresentacao(int entidade, String animacao, String efeito, String som)` S→cliente.
- Ganchos do `teste`: `teste_golpe` → `danoEmArea(3, 4)` + `apresentacao("gojo_azul", "", "teste")`; `teste_carga` → `danoEmArea(5, 4 + 8 * forca)` + `apresentacao("gojo_roxo", "", "teste")`.

- [ ] **Passo 1:** `./gradlew build` → passa.
- [ ] **Passo 2:** commit "Rede: payloads do kit e contexto do servidor".

### Tarefa 7: cliente (teclas, recepção, tela de escolha) e GameTests
**Arquivos:** criar `client/kit/TeclasKit.java`, `client/kit/RecepcaoKit.java`, `client/kit/TelaEscolha.java`, `kit/TestesKit.java`; modificar `MineRWKClient.java`, `lang/pt_br.json`, `lang/en_us.json`.

**Produz:**
- `TeclasKit`: `KeyMapping` `key.minerwk.z` … `key.minerwk.r` (padrão Z X C V G R) e `key.minerwk.escolher` (padrão K), categoria `key.categories.minerwk`. No `ClientTickEvent.Post`, envia `UsarTecla` quando `isDown()` muda. A tecla de escolha abre a `TelaEscolha` só se ainda não há personagem.
- `RecepcaoKit`: guarda o último `SincronizarKit` (para o HUD do 1.4) e abre a `TelaEscolha` no primeiro com personagem vazio. `TocarApresentacao` → `AnimacoesJogador.tocar`, `EfeitosVisuais.naEntidade` e som local, ignorando campos vazios.
- `TelaEscolha extends Screen`: um botão "Nome — Obra" por escolhível; clicar abre a confirmação "A escolha é definitiva. Confirmar?" (Sim envia `EscolherPersonagem` e fecha; Não volta).
- GameTests: `usarHabilidadeGastaRecursoEIniciaRecarga` (jogador falso com `teste`, `apertar(Z)`, 5 ticks → recurso 80 e recarga de `teste_golpe` > 0); `escolhaRecusadaQuandoJaTemPersonagem` (com `teste`, processa `EscolherPersonagem("outro")` → continua `teste`).

- [ ] **Passo 1:** `./gradlew build` e `./gradlew runGameTestServer` → passam.
- [ ] **Passo 2:** commit "Cliente: teclas, tela de escolha e GameTests do kit".

### Tarefa 8: teste no jogo e vault
- [ ] `./gradlew runClient`: entrar sem personagem → a tela abre; escolher `teste`; Z e X (segurando) causam dano e tocam animação e som; a recarga impede o reuso; ficar parado enche o recurso.
- [ ] Marcar o 1.3 no Roadmap, criar o diário e anotar em Pendências (ver a animação em outro jogador; teste com 2 clientes).
- [ ] Commit "Vault: núcleo dos kits concluído".
