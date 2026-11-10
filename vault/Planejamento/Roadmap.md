---
tags: [planejamento]
atualizado: 2026-11-08
---
# Roadmap

Voltar para [[00 Início]]. Marque `[x]` ao concluir e registre no [[Diário]]. Decisões novas vão em [[Decisões]]; o que precisa de teste no jogo vai em [[Pendências]].

**Onde estamos:** Fase 2 (Gojo), mecânica das seis habilidades pronta (apresentação provisória); VFX dos seis golpes feito (shader portado do Minezin, falta o teste no jogo); impacto de câmera, detritos e brilho feito; o autor ainda acha os golpes simples. Reação do alvo, ritmo e a cutscene do Roxo 200% feitos (falta o teste no jogo). Animações, domínio fechado e R colossal feitos; o R virou o Vazio Roxo máximo com uppercut, e o Azul e o Infinito ganharam VFX mais rico (falta o teste no jogo). A cutscene completa do R e as animações novas estão feitas (falta o teste no jogo). VFX pesado e nova ordem do R feitos e testados pelo autor ("melhorou bastante"); céu e chão no encantamento feitos (falta o teste no jogo). Som do Gojo feito, as três partes (falta o teste no jogo). Traje do Gojo feito (falta o teste no jogo). Próximo no Gojo: o boss [[Toji]] e o despertar (a maestria e a árvore de habilidades estão feitas, falta o teste no jogo). O [[Toji]] não é jogável: é o boss do despertar do Gojo, a fazer depois do traje. Próximo, na ordem aprovada em 09/11: som; depois e o pipeline de áudio (parte 3 do 1.4, adiada para cá). As partes 1 (HUD e cliente) e 2 (tela de escolha) do 1.4 estão prontas. O 1.2 ainda tem o `.fx` de teste e o Iris pendentes.

## Como trabalhar em outro PC
1. `git clone https://github.com/Cauan-Silva/Mine-RWK.git` (ou `git pull` se já tiver).
2. Abra a pasta `vault/` no Obsidian.
3. `./gradlew build` para conferir que compila (o Gradle baixa o JDK 21 sozinho). Detalhes e problemas conhecidos em [[Ambiente e Build]].
4. Para o Minezin de referência: `git pull` na pasta do Minezin (a `main` dele tem o estado mais novo dos kits).
5. No Claude Code, abra a pasta do Mine-RWK: o `CLAUDE.md` da raiz já explica o projeto e aponta para este roadmap.

---

## Fase 0: pesquisa ✅
- [x] Pesquisa de bibliotecas: [[Bibliotecas]]
- [x] Base técnica aprovada ([[Decisões]])
- [x] Versões confirmadas no build

## Fase 1: base do projeto (atual)

### 1.1 Projeto ✅
- [x] ModDevGradle + NeoForge 21.1.265 + Parchment + JUnit ([[Ambiente e Build]])
- [x] Photon2, LDLib2, GeckoLib e Player Animation Library carregando no cliente e no servidor

### 1.2 Prova de conceito
- [x] Trazer um efeito `.fx` do Minezin: **não existe nenhum**; o Minezin usava o Photon 1 pelo código. Os efeitos serão feitos do zero no editor do Photon2 (anotado em [[Bibliotecas]])
- [x] Comando de teste `/rwk fx <efeito>` que toca o efeito na posição do jogador
- [x] Uma animação JSON do Blockbench em `assets/minerwk/player_animations/` tocando com `/rwk anim <animação>` (`gojo.json` do Minezin: `gojo_azul`, `gojo_vermelho`, `gojo_infinito`, `gojo_roxo`, `gojo_dominio`)
- [x] Um som de teste registrado (`sounds.json`) tocando junto (`/rwk som`, e toca junto com `/rwk anim`)
- [ ] Criar um `.fx` de teste no editor do Photon2 em `assets/minerwk/fx/` e tocar com `/rwk fx`
- [ ] Conferir com Iris + um shaderpack que o efeito continua visível

### 1.3 Núcleo dos kits ✅
Spec e plano: [[2026-11-09 Núcleo dos kits]] (em `Desenvolvimento/Specs` e `Desenvolvimento/Planos`).
- [x] Registro de personagem (`Personagem` com id, nome, obra, habilidades)
- [x] Habilidade com tempo em ticks (carga, disparo, fim), recarga (`core/kit/Recargas` já existe) e custo
- [x] Teclas Z, X, C, V, G (e R para ultimate) configuráveis
- [x] Rede: payloads (`CustomPacketPayload`) para usar habilidade, sincronizar recargas e mandar tocar VFX/animação/som nos outros jogadores
- [x] Dados do jogador com Data Attachments (personagem escolhido, recargas, recurso)
- [x] Interrupção (atordoar cancela golpe em carga, recarga de 1/4). A postura em si fica para a Fase 4; ninguém chama `atordoar()` ainda
- [x] Testes JUnit da lógica de habilidade e um GameTest de uso de habilidade no servidor

### 1.4 Base visual e de som
- [x] Pipeline de áudio: `ferramentas/sfx_motor.py` e `gerar_sfx_gojo.py`, portados do Minezin, e o `SonsGojoTest`, que confere os ticks do som contra o Java; ver [[2026-11-10 Som do Gojo parte 1]]
- [ ] `THIRD_PARTY_NOTICES.md` para gravações CC0 e Sonniss
- [x] Configuração do cliente (`minerwk-client.toml`: tremor, VFX reduzido, sons, HUD)
- [x] Tremor de tela próprio (`core/camera/Tremor`, pacote `Tremer`)
- [x] HUD de recargas e recurso (os ícones vêm com o Gojo)
- [x] Tela de seleção de personagem: grade de emblemas com rolagem e painel com golpes (`core/hud/LayoutEscolha`, `client/kit/TelaEscolha`)

## Fase 2: Gojo completo (primeiro personagem, vira o modelo dos outros)
- [x] Pesquisa canônica na nota do Gojo seguindo [[Kits Canônicos]] e [[Modelo de Personagem]]
- [x] Kit aprovado (tecla, dano, recarga) antes de programar: ver [[Gojo]]
- [x] Habilidades: Azul, Vermelho, Infinito, Roxo, Vazio Infinito (domínio), Roxo 200% (o teleporte ficou de fora do kit). Mecânica no servidor com apresentação provisória; ver [[2026-11-09 Gojo mecânica]]
- [x] VFX dos seis golpes (shader `gojo_energy` + geometria, sem Photon por enquanto); ver [[2026-11-09 Gojo VFX]]
- [x] Impacto dos golpes: câmera reage (tremor, clarão, FOV), detritos e brilho; ver [[2026-11-09 Gojo impacto]]
- [x] Reação do alvo e ritmo: golpes mais longos, arremesso com congelamento, Azul arranca blocos, tela de quem acerta trava, rastros no mundo; ver [[2026-11-09 Gojo peso]]
- [x] Roxo 200%: cutscene (sistema de câmera reutilizável) e encantamento visível; ver [[2026-11-09 Roxo 200 cutscene]]
- [ ] VFX no Photon2 de cada habilidade; Veil só se o domínio precisar de pós-processamento
- [x] SFX de cada habilidade: 18 sons sintetizados, presos ao visual e cortados na interrupção, incluindo os sete do R; ver [[2026-11-10 Som do Gojo parte 1]] (falta o teste no jogo)
- [x] Animações do jogador de cada golpe: corpo inteiro, primeira pessoa, começam na carga; ver [[2026-11-09 Gojo animações]]
- [x] Domínio fechado (barreira e chão por regra de movimento), animações livres ao andar e Roxo 200% colossal (sobe 20 blocos, massa de raio 7, explosão de raio 24); ver [[2026-11-09 Gojo domínio, movimento e R colossal]]
- [x] R vira o Vazio Roxo máximo (avanço, uppercut, fusão atrás do Gojo, explosão de raio 40) e VFX mais rico do Azul e do Infinito; ver [[2026-11-09 Vazio Roxo máximo e VFX do Azul e do Infinito]]
- [x] Cutscene cinemática da ultimate (Vazio Roxo máximo): encantamento, avanço, uppercut, esferas, fusão e a explosão em plano aberto, vista pelo Gojo e pelo alvo; mais animações do R; ver [[2026-11-09 Cutscene do Vazio Roxo máximo]]
- [x] VFX pesado do R (Photon em código, bloom, casca com shader, fusão em quatro tempos, explosão em camadas) e nova ordem do R (conjurar no céu, cutscene da corrida e do uppercut com impact frame, câmera longe do alvo); ver [[2026-11-10 VFX pesado e nova ordem do R]]
- [x] Traje (GeckoLib) com a venda que sai no domínio e no R, automático ao escolher o Gojo, e passivas (Seis Olhos e Técnica Reversa, que já existiam); ver [[2026-11-10 Traje do Gojo]] (falta o teste no jogo)
- [x] Maestria e árvore de habilidades, adiantadas da Fase 4 porque o Toji depende delas; spec [[2026-11-10 Maestria e árvore de habilidades]] (aprovado). As três partes feitas, ver [[2026-11-10 Maestria parte 1]], [[2026-11-10 Maestria parte 2]] e [[2026-11-10 Maestria parte 3]] (falta o teste no jogo)
- [ ] [[Toji]] como boss (Nêmesis do Gojo) e o despertar: o Gojo começa com Azul e Infinito; vencer o Toji, invocado com um item, libera a Técnica Reversa, o Vermelho e o Roxo. Vem depois do traje (decisão de 10/11). Spec [[2026-11-10 Toji e o despertar do Gojo]] (aprovado). Parte 1 (golpes trancados) feita, ver [[2026-11-10 Toji parte 1]]; faltam o boss essencial e o resto da luta
- [ ] Teste no jogo de ponta a ponta, anotado em [[Pendências]]
- [ ] Escrever em `vault/Desenvolvimento/` o passo a passo de "como portar um personagem", com base no que o Gojo ensinou (primeira versão em [[Como portar um personagem]]; falta completar som e traje)

## Fase 3: demais personagens
Um por vez, sempre: pesquisa canônica → kit aprovado → código → VFX → SFX → animações → traje → teste no jogo. Ordem sugerida (começa pelos que reaproveitam mais coisa do Gojo e do núcleo, deixa invocações e formas para depois):

| # | Personagem | Por que nessa posição |
| --- | --- | --- |
| 1 | Ichigo | Espada + forma (Bankai/Mugetsu) |
| 2 | Tanjiro | Espada + respirações, efeitos de água/fogo |
| 3 | Asta | Espadas + forma demônio |
| 4 | Killua | Rápido, elétrico, sem invocações |
| 5 | Douma | Gelo, área |
| 6 | Frieren | Magias de projétil e área |
| 7 | Rudeus | Magias elementais |
| 8 | Homura | Tempo parado (sistema de tempo) |
| 9 | Dio | Stand + tempo parado (reaproveita a Homura) |
| 10 | Cid | Espada + ultimate de área |
| 11 | Guts | Arma enorme + armadura berserker (forma) |
| 12 | Jin-Woo | Invocações (Igris, Beru) com GeckoLib |
| 13 | Subaru e Beatrice | Contrato, forma e magias da Beatrice |

Atualizar a coluna "Portado" em [[Personagens]] a cada um.

## Fase 4: sistemas do Minezin
Na ordem (cada um ganha nota em `vault/Sistemas/` quando portado):
1. [ ] RPG e Despertar, Rank, Passivas de todos
2. [ ] Bosses Nemesis e rework dos bosses (máquina de estados por golpe, postura, fases)
3. [ ] Fendas do Vazio, Lua do Vazio, World Bosses
4. [ ] Raids, Torre
5. [ ] Party, PvP, Leaderboard, Títulos
6. [ ] Vila, Missões, Guilda, Códice
7. [ ] Interface e menus finais

## Fase 5: lançamento
- [ ] Balanceamento geral e teste com 2+ jogadores em servidor dedicado
- [ ] Teste com Iris + shaderpacks populares e com Sound Physics Remastered
- [ ] Página do mod, changelog e versão 1.0.0
