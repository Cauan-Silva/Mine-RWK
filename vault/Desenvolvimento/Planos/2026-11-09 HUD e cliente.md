---
tags: [desenvolvimento, plano]
---
# HUD e cliente: plano de implementação

> Execução nativa (decisão do autor no 1.3), em branch próprio, TDD nas partes puras, revisão final por um revisor separado.

**Objetivo:** HUD de recurso e recargas, configuração do cliente e tremor de tela (Roadmap 1.4, parte 1).
**Spec:** [[2026-11-09 HUD e cliente]] (em `vault/Desenvolvimento/Specs/`).

## Restrições globais
- Português em nomes, comentários e mensagens; commits com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- `core/` sem Minecraft; renderização só em `client/`.
- Verificação: `./gradlew build` e `./gradlew runGameTestServer`.

## Foco de revisão
1. Recarga que já acabou no servidor mas o relógio do cliente está um pouco atrás → nunca mostrar fração negativa nem acima de 1.
2. Recarga de punição (1/4) → a fração usa a duração daquela recarga, não a normal.
3. Personagem sem algumas teclas → HUD centralizado só com as existentes.
4. Tremor pedido com o jogador fora do mundo do cliente (outra dimensão) → ignorado.
5. Configuração lida antes de carregar (tela de título) → valores padrão, sem erro.

---

### Tarefa 1: núcleo do HUD
**Arquivos:** `core/kit/Recurso.java`, `core/kit/teste/RecursoFolego.java`, `core/kit/Recargas.java`; criar `core/hud/LogicaHud.java`; testes `core/hud/LogicaHudTest.java`, acréscimos em `RecursoFolegoTest` e `RecargasTest`.
- `Recurso`: `String nome()`, `int cor()`. `RecursoFolego`: "Fôlego", `0x1D9E75`.
- `Recargas`: guarda também a duração de cada recarga iniciada; `int duracao(String habilidade)` (0 se nunca iniciou); `salvar()` continua só com os ticks de pronta (duração não é salva; ao carregar, duração = restante na primeira vez que o cliente vê).
- `LogicaHud`: `static float fracao(long prontaNoTick, int duracao, long agora)` em [0, 1]; `static String segundos(long restanteTicks)` ("" se 0); `static boolean semRecurso(double atual, double custo)`; `static List<Tecla> teclasVisiveis(Set<Tecla> doPersonagem)` na ordem do enum.
- Testes: os valores do spec (61 → "4", 19 → "0.9", 7 → "0.3", 1 → "0.1", 0 → ""), fração com relógio atrasado/adiantado limitada a [0, 1], punição de 1/4 com duração própria, teclas na ordem.

### Tarefa 2: `Tremor`
**Arquivos:** criar `core/camera/Tremor.java`; teste `TremorTest.java`.
- `void adicionar(float forca, int duracao)`; `void tick()`; `float forcaAtual()` = soma de `forca × (1 − t/duração)²` dos ativos, limitada a 1,5; `Deslocamento deslocamento(float tempo, float intensidade)` com `record Deslocamento(float guinada, float inclinacao, float rotacao)` vindos de senos de frequências 23, 31 e 17 Hz × `forcaAtual × intensidade` × 2°.
- Testes: decai a zero após a duração (e o tremor é removido), dois tremores somam, soma limitada, intensidade 0 → deslocamento zero.

### Tarefa 3: rede e servidor
**Arquivos:** `core/kit/Contexto.java`, `core/kit/teste/PersonagemTeste.java`, `kit/ContextoServidor.java`, `kit/Sincronizacao.java`, `rede/SincronizarKit.java`, criar `rede/Tremer.java`, `rede/Pacotes.java`.
- `Contexto.tremor(float forca, int duracao, double raio)`; `ContextoServidor` manda `Tremer` (com `Envio.para`) a cada jogador da mesma dimensão a até `raio`, força × (1 − d/raio).
- `SincronizarKit` troca o mapa de recargas por uma lista de `HabilidadeHud(Tecla tecla, String id, double custo, long prontaNoTick, int duracao)` e ganha `String nomeRecurso`, `int corRecurso`, `String emUso`. `Sincronizacao` também envia quando `emUso` muda.
- `teste_carga.aoDisparar` chama `tremor(0.3f + 0.7f * forca, 15, 12)`.
- `Pacotes.aoTremer` receptor estático como os outros.

### Tarefa 4: cliente
**Arquivos:** criar `client/ConfigCliente.java`, `client/kit/HudKit.java`, `client/camera/TremorCamera.java`; modificar `MineRWKClient.java`, `client/kit/RecepcaoKit.java`, lang.
- `ConfigCliente`: `ModConfigSpec` CLIENT com as 4 opções do spec; registrado com `modContainer.registerConfig(ModConfig.Type.CLIENT, ...)` e tela de configuração do NeoForge (`IConfigScreenFactory` / `ConfigurationScreen`).
- `HudKit`: camada registrada em `RegisterGuiLayersEvent` acima da barra de vida/fome; usa `LogicaHud` e o último `SincronizarKit`; barra desliza até o valor novo (interpolação de ~4 ticks).
- `TremorCamera`: `Tremor` do cliente; `ClientTickEvent.Post` → `tick()`; `ViewportEvent.ComputeCameraAngles` soma o deslocamento × `intensidadeTremor`; ignora se não há mundo.
- `RecepcaoKit`: não toca som se `sfxAnime = false`; `aoTremer` adiciona ao `TremorCamera`.

### Tarefa 5: testes no servidor, jogo e vault
- GameTest `cargaComTremorNaoFalha`: jogador falso com `teste`, segura X por 20 ticks, solta; recurso = 70 e nenhum erro.
- `./gradlew runClient`: checklist do spec. Roadmap, diário e Pendências.
