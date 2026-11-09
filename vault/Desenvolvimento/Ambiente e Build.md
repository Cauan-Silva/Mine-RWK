---
tags: [desenvolvimento]
---
# Ambiente e Build

Voltar para [[00 Início]].

## Requisitos
- Nada além do Java 17 que já está instalado: o Gradle baixa o **JDK 21** sozinho (plugin foojay) para `~/.gradle/jdks`.
- Gradle 8.14.3 pelo wrapper (`gradlew`), plugin **ModDevGradle 2.0.148**.

## Comandos
| Comando | O que faz |
| --- | --- |
| `./gradlew build` | Compila, roda os testes JUnit e gera `build/libs/minerwk-<versão>.jar` |
| `./gradlew runClient` | Abre o Minecraft com o mod e as bibliotecas |
| `./gradlew runServer` | Servidor dedicado |
| `./gradlew runGameTestServer` | Servidor sem tela que roda os GameTests (`kit/TestesKit`) e fecha |
| `./gradlew runData` | Gera recursos em `src/generated/resources` |

## Estrutura
- `src/main/java/com/minerwk/`: `MineRWK` (comum), `client/MineRWKClient` (cliente), `core/` (lógica dos kits, testável sem o jogo).
- `src/main/templates/META-INF/neoforge.mods.toml`: preenchido com as versões do `gradle.properties`.
- Versões de tudo ficam no `gradle.properties`.

## Armadilhas
- **Acento no nome do usuário do Windows (`Usuário`)**: o Java falha ao abrir sockets locais com "Unable to establish loopback connection" (o Gradle, o NeoFormRuntime e o próprio jogo). Correção: `-Djdk.net.unixdomain.tmpdir=C:/Users/Public`, já colocada no `gradlew`, no `gradlew.bat` (via `JAVA_TOOL_OPTIONS`, que vale para os processos filhos) e no `org.gradle.jvmargs`. Ao rodar pelo IntelliJ, se o erro aparecer, adicionar a mesma variável `JAVA_TOOL_OPTIONS` na configuração.
- **Conflito de versões com o Minecraft**: LDLib2 e Player Animation Library pedem fastutil, gson, slf4j e netty mais novos do que os que o Minecraft 1.21.1 trava. O `build.gradle` exclui esses grupos (`libsDoJogo`) e usa os do jogo.
- Avisos esperados no log: `geckolib.refmap.json could not be read` (só em desenvolvimento), `ldlib2:sync_bench` sem modelo e `graph_wire` sem `iTime` (vêm do LDLib2).
