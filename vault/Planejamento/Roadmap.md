---
tags: [planejamento]
atualizado: 2026-11-08
---
# Roadmap

Voltar para [[00 Início]]. Marque `[x]` ao concluir e registre no [[Diário]]. Decisões novas vão em [[Decisões]]; o que precisa de teste no jogo vai em [[Pendências]].

**Onde estamos:** Fase 2 (Gojo), mecânica das seis habilidades pronta (apresentação provisória); próximo: parte visual (VFX, animações, cutscene da ultimate) e o pipeline de áudio (parte 3 do 1.4, adiada para cá). As partes 1 (HUD e cliente) e 2 (tela de escolha) do 1.4 estão prontas. O 1.2 ainda tem o `.fx` de teste e o Iris pendentes.

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
- [ ] Pipeline de áudio: pasta `tools/` com o gerador em Python adaptado do Minezin e teste que confere ticks do som contra o Java
- [ ] `THIRD_PARTY_NOTICES.md` para gravações CC0 e Sonniss
- [x] Configuração do cliente (`minerwk-client.toml`: tremor, VFX reduzido, sons, HUD)
- [x] Tremor de tela próprio (`core/camera/Tremor`, pacote `Tremer`)
- [x] HUD de recargas e recurso (os ícones vêm com o Gojo)
- [x] Tela de seleção de personagem: grade de emblemas com rolagem e painel com golpes (`core/hud/LayoutEscolha`, `client/kit/TelaEscolha`)

## Fase 2: Gojo completo (primeiro personagem, vira o modelo dos outros)
- [x] Pesquisa canônica na nota do Gojo seguindo [[Kits Canônicos]] e [[Modelo de Personagem]]
- [x] Kit aprovado (tecla, dano, recarga) antes de programar: ver [[Gojo]]
- [x] Habilidades: Azul, Vermelho, Infinito, Roxo, Vazio Infinito (domínio), Roxo 200% (o teleporte ficou de fora do kit). Mecânica no servidor com apresentação provisória; ver [[2026-11-09 Gojo mecânica]]
- [ ] VFX no Photon2 de cada habilidade; Veil só se o domínio precisar de pós-processamento
- [ ] SFX de cada habilidade
- [ ] Animações do jogador de cada golpe
- [ ] Cutscene cinemática da ultimate (Roxo 200%), com sistema de câmera reutilizável pelas outras ultimates
- [ ] Traje (GeckoLib) com óculos, e passivas (Seis Olhos, Técnica Reversa)
- [ ] Teste no jogo de ponta a ponta, anotado em [[Pendências]]
- [ ] Escrever em `vault/Desenvolvimento/` o passo a passo de "como portar um personagem", com base no que o Gojo ensinou

## Fase 3: demais personagens
Um por vez, sempre: pesquisa canônica → kit aprovado → código → VFX → SFX → animações → traje → teste no jogo. Ordem sugerida (começa pelos que reaproveitam mais coisa do Gojo e do núcleo, deixa invocações e formas para depois):

| # | Personagem | Por que nessa posição |
| --- | --- | --- |
| 1 | Toji | Mesma obra do Gojo; corpo a corpo e armas |
| 2 | Ichigo | Espada + forma (Bankai/Mugetsu) |
| 3 | Tanjiro | Espada + respirações, efeitos de água/fogo |
| 4 | Asta | Espadas + forma demônio |
| 5 | Killua | Rápido, elétrico, sem invocações |
| 6 | Douma | Gelo, área |
| 7 | Frieren | Magias de projétil e área |
| 8 | Rudeus | Magias elementais |
| 9 | Homura | Tempo parado (sistema de tempo) |
| 10 | Dio | Stand + tempo parado (reaproveita a Homura) |
| 11 | Cid | Espada + ultimate de área |
| 12 | Guts | Arma enorme + armadura berserker (forma) |
| 13 | Jin-Woo | Invocações (Igris, Beru) com GeckoLib |
| 14 | Subaru e Beatrice | Contrato, forma e magias da Beatrice |

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
