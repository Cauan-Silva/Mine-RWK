# Mine-RWK

Mod de Minecraft com kits de personagens de anime, sucessor do Minezin (repositório Cauan-Silva/Minezin, Forge 1.20.1), refeito em NeoForge. O autor fala português: responda e escreva notas, commits e comentários em português.

## Onde está cada coisa
- **Plano e estado atual:** `vault/Planejamento/Roadmap.md` (diz em que item estamos). Leia antes de começar.
- Decisões: `vault/Planejamento/Decisões.md` · pendências e testes no jogo: `vault/Planejamento/Pendências.md`.
- Bibliotecas escolhidas e por quê: `vault/Pesquisa/Bibliotecas.md`.
- Regra dos kits fiéis à obra: `vault/Pesquisa/Kits Canônicos.md` e o modelo `vault/Personagens/Modelo de Personagem.md`.
- Build, comandos e problemas conhecidos: `vault/Desenvolvimento/Ambiente e Build.md`.
- Forge → NeoForge: `vault/Desenvolvimento/Do Minezin para o Mine-RWK.md`.

## Ambiente
- **Loader:** NeoForge 21.1.265, com o ModDevGradle 2.0.148 (`net.neoforged.moddev`).
- **Minecraft** 1.21.1 e **Java** 21. O Gradle baixa o JDK pelo toolchain (foojay).
- **Mapeamentos:** os nomes oficiais da Mojang mais o Parchment 2024.11.17 (para 1.21.1), que dá nomes aos parâmetros.
- **Mod:** `mod_id=minerwk`, pacote base `com.minerwk` (`MineRWK.MOD_ID`).
- **Bibliotecas:**
  - Photon2 2.2.11 e LDLib2 2.2.46.a: VFX.
  - GeckoLib 4.9.3: bosses e trajes.
  - Player Animation Library 1.1.6: animações do jogador.
- Todas as versões ficam no `gradle.properties`.
- **Pacotes:**
  - `core/`: regras puras, sem classes do Minecraft, com testes JUnit.
  - `kit/`: os kits no servidor e os GameTests.
  - `boss/` e `arena/`: as entidades e as dimensões.
  - `rede/`: os pacotes de rede.
  - `registro/`: os DeferredRegister.
  - `client/`: tudo o que é visual.
- **Ferramentas:** `ferramentas/*.py` geram modelos, texturas, animações e sons. Assets gerados não se editam à mão: muda o gerador e gera de novo.

## Comandos
- `./gradlew build`: compila e roda os testes JUnit.
- `./gradlew runGameTestServer`: os GameTests num servidor sem tela. O resultado sai em `run/logs/latest.log` ("All N required tests passed").
- `./gradlew runClient`: abre o jogo.
- `./gradlew runClientDemo -Pdemo=<nome>`: grava os quadros de uma prévia em `run/demo/`.
- `./gradlew runData`: roda a DataGen. A saída vai para `src/generated/resources/`, que já entra nos recursos.
- Nunca rode dois processos do Gradle ao mesmo tempo.

## Regras do projeto
- Toda habilidade nova começa pela pesquisa canônica com fontes, e o kit é aprovado pelo autor antes de programar.
- Lógica dos kits em `core/` sem classes do Minecraft sempre que der, com testes JUnit.
- Ao terminar algo: marcar no Roadmap, registrar no diário (`vault/Diário/AAAA-MM-DD Título.md` e o índice `Diário.md`) e anotar em Pendências o que precisa ser testado no jogo.
- Não usar assets de terceiros sem licença compatível; registrar créditos em `THIRD_PARTY_NOTICES.md`. Gravações de som: só CC0, em `art/audio/cc0/`, com a fonte em `fontes.json` (o `GravacoesCc0Test` confere).

## Servidor e cliente
O mod precisa rodar num servidor dedicado, onde as classes de cliente não existem.
- **Tudo o que desenha, toca som local, lê teclado ou mexe na câmera fica em `client/`.** Photon, LDLib2, Player Animation Library, renderers do GeckoLib, telas, HUD, `Minecraft.getInstance()`, `RenderType` e `PoseStack`, e também os eventos de cliente (`RenderPlayerEvent`, `RenderArmEvent`, `RenderLevelStageEvent` e outros).
- **Código comum e de servidor nunca importa nada de `client/`** nem `net.minecraft.client.*`. O cliente entra só pela classe `@Mod(value = MineRWK.MOD_ID, dist = Dist.CLIENT)` (`client/MineRWKClient`), que registra os ouvintes.
- **O servidor avisa o cliente por pacote** (`rede/`, `CustomPacketPayload`), por exemplo `MostrarVisual` e `Cutscene`. O lado do cliente trata o pacote dentro de `client/`. Nunca decidir dano, cura ou estado de jogo no cliente.
- **Uma classe comum que precisa de algo do cliente** recebe um gancho registrado pelo cliente, como um `Runnable` ou uma interface em `core/`. Não use `DistExecutor` nem um `if (level.isClientSide)` que carregue uma classe de cliente.
- **Para conferir:** os GameTests rodam num servidor sem tela. Se uma classe de cliente vazar para o código comum, o `runGameTestServer` quebra.

## Dados: DataGen
- **Receitas, loot tables, tags, blockstates e modelos de item/bloco novos são gerados por DataGen**, com providers no evento `GatherDataEvent` e `./gradlew runData`. Não escreva esses JSONs à mão.
- O que a DataGen do NeoForge não cobre bem fica em JSON: dimensões e tipos de dimensão, tipos de dano, estruturas `.nbt`, e os assets que os geradores Python fazem (modelos GeckoLib, animações, sons).
- **Hoje ainda não há DataGen:** as três receitas, os tipos de dano e a tag `bypasses_armor` em `src/main/resources/data/` foram escritos à mão. Antes de criar o primeiro dado gerado, monte o provider e migre esses arquivos junto. Peça ao autor antes de migrar.

## Antes de encerrar uma tarefa
- **Depois de criar ou mudar um sistema, rode `./gradlew build` e leia o log.** Corrija todo erro de compilação ou teste antes de encerrar. Nunca diga que está pronto sem um build verde nesta sessão.
- **Se mexeu em servidor, rede, entidades, kits ou bosses, rode também `./gradlew runGameTestServer`** e confira "All N required tests passed" no `run/logs/latest.log`.
- **Mudanças visuais** se conferem por uma prévia (`runClientDemo`), olhando os quadros. O que só dá para provar jogando vai para Pendências.
