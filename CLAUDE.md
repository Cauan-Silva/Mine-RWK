# Mine-RWK

Mod de Minecraft com kits de personagens de anime, sucessor do Minezin (repositório Cauan-Silva/Minezin, Forge 1.20.1), refeito em NeoForge. O autor fala português: responda e escreva notas, commits e comentários em português.

## Onde está cada coisa
- **Plano e estado atual:** `vault/Planejamento/Roadmap.md` (diz em que item estamos). Leia antes de começar.
- Decisões: `vault/Planejamento/Decisões.md` · pendências e testes no jogo: `vault/Planejamento/Pendências.md`.
- Bibliotecas escolhidas e por quê: `vault/Pesquisa/Bibliotecas.md`.
- Regra dos kits fiéis à obra: `vault/Pesquisa/Kits Canônicos.md` e o modelo `vault/Personagens/Modelo de Personagem.md`.
- Build, comandos e problemas conhecidos: `vault/Desenvolvimento/Ambiente e Build.md`.
- Forge → NeoForge: `vault/Desenvolvimento/Do Minezin para o Mine-RWK.md`.

## Stack
Minecraft 1.21.1 · NeoForge 21.1.265 · ModDevGradle · Java 21 (o Gradle baixa) · Photon2 + LDLib2 (VFX) · GeckoLib (entidades/trajes) · Player Animation Library (jogador). Versões no `gradle.properties`.

## Comandos
- `./gradlew build`: compila e roda os testes JUnit.
- `./gradlew runClient`: abre o jogo. `./gradlew runGameTestServer`: servidor sem tela.

## Regras do projeto
- Toda habilidade nova começa pela pesquisa canônica com fontes, e o kit é aprovado pelo autor antes de programar.
- Lógica dos kits em `core/` sem classes do Minecraft sempre que der, com testes JUnit.
- Photon e Player Animation Library só no cliente (`client/`).
- Ao terminar algo: marcar no Roadmap, registrar no diário (`vault/Diário/AAAA-MM-DD Título.md` e o índice `Diário.md`) e anotar em Pendências o que precisa ser testado no jogo.
- Não usar assets de terceiros sem licença compatível; registrar créditos em `THIRD_PARTY_NOTICES.md`.
