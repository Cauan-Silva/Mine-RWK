---
tags: [pesquisa]
data: 2026-11-08
---
# Bibliotecas (pesquisa de 08/11/2026)

Objetivo: escolher, para NeoForge, as melhores bibliotecas de VFX, SFX e animação. Voltar para [[00 Início]].

Versões tiradas de CurseForge, Modrinth e Mavens. Itens marcados com **(confirmar)** vieram de uma única fonte ou de site de terceiros e precisam ser conferidos na hora de montar o `build.gradle`.

## Resumo da recomendação

Versões em uso no `gradle.properties` (conferidas nos Mavens e já rodando no jogo em 08/11/2026):

| Área | Escolha | Versão em uso | Papel |
| --- | --- | --- | --- |
| Minecraft / loader | 1.21.1 + NeoForge | 21.1.265 | Ecossistema maior e estável |
| VFX principal | **Photon2** | 2.2.11 | Partículas, editor visual, efeitos dos golpes |
| Base do Photon | **LDLib2** | 2.2.46.a | Dependência obrigatória (o Photon 2.2.11 pede ≥ 2.2.45) |
| Pós-processamento | **Veil** (opcional, fora do build) | 4.5.2 | Shaders de tela, distorção, luz dinâmica (domínios, ultimates) |
| Modelos / entidades | **GeckoLib** | 4.9.3 | Bosses, invocações, trajes, armas 3D |
| Animação do jogador | **Player Animation Library** (ZigyTheBird) | 1.1.6+mc.1.21.1 | Poses e golpes do jogador |
| SFX | Sistema de som do jogo | — | `sounds.json` + gerador em Python, como no Minezin |
| Shaders do jogador | Iris (compatibilidade) | 1.8.12 / 1.8.14-beta | Testar que os efeitos aparecem com shaderpack |

Coordenadas Maven: ver `build.gradle` e [[Ambiente e Build]].

## Versão do Minecraft: 1.21.1 (aprovada em 08/11/2026)

- **Correção:** a primeira versão desta nota dizia que o Photon2 só existia para 1.21.1. Errado: o Maven da LowDragMC também tem Photon e LDLib2 para **26.1, 26.2 e 26.3**, com as mesmas versões e atualizados no mesmo dia.
- Comparação feita: em 26.3 também existem GeckoLib 5.5.7, Player Animation Library 1.2.9, Iris 1.11.7 e Sound Physics; **não existe Veil**.
- Escolhemos 1.21.1 porque é onde está a maior parte dos mods e modpacks NeoForge, a API é estável, tem mais documentação e tem o Veil. O Photon continua atualizado para ela.
- Migrar para 26.x no futuro é possível: as bibliotecas principais existem lá.

## VFX

### Photon2 + LDLib2 (escolhido)
- Continuação do Photon 1.x usado no Minezin (`photon-forge-1.20.1:1.1.17`). A 2.2.0 (21/07/2026) virou "toolkit completo de VFX em tempo real", não só editor de partículas.
- Requer LDLib2 (o POM da 2.2.11 aponta 2.2.45); o KilaGraph vem embutido no jar `:all`.
- API: `FXHelper.getFX(id)` + `BlockEffectExecutor` / `EntityEffectExecutor` (só no cliente). Atalhos em `client/vfx/EfeitosVisuais`.
- Documentação: https://low-drag-mc.github.io/LowDragMC-Doc/en/photon2/java-api/
- **Risco:** é uma versão maior. Os efeitos do Minezin (Photon 1) provavelmente precisam ser reexportados ou refeitos. Testar com um efeito antes de portar o resto.

### Veil (complemento, opcional)
- Biblioteca de renderização da FoundryMC: pipeline de shaders GLSL, framebuffers e pós-processamento em JSON, luzes dinâmicas.
- Útil para o que partícula não resolve: distorção de tela no Infinito do Gojo, mudança de cor do mundo em domínios, bloom localizado.
- **Atenção com Iris:** os efeitos do Veil ficam fora do G-buffer do shaderpack e podem sumir. Existe o "Veil Iris Lights Compat" (Veil ≥ 4.0.0, NeoForge ≥ 21.1.228, Iris ≥ 1.8.1).
- Decisão: só entra quando um efeito precisar, não na base.

### Alternativas avaliadas
| Biblioteca | Por que não como principal |
| --- | --- |
| **Lodestone** 1.8.2 | Partículas muito configuráveis, screenshake e world events. Sobrepõe o Photon; dá para pegar só a ideia do screenshake e fazer o nosso. |
| **AAA Particles** (Effekseer) 2.2.x | Efeitos de altíssima qualidade feitos no editor Effekseer. Seria um segundo pipeline de VFX paralelo ao Photon. Fica como reserva se o Photon2 não der conta de algum efeito. |
| **Shine: VFX** | Bloom e luz colorida sem shader. É um mod de usuário, não uma API para nós. |

## Animação

### GeckoLib
- Já usado no Minezin (4.4.9 Forge). No NeoForge 1.21.1 há 4.7.x, 4.8 e possivelmente 4.9.2 **(confirmar a última)**.
- Usar para bosses, invocações (Igris, Beru…), trajes e armas 3D.

### Player Animation Library (ZigyTheBird)
- Substitui o `player-animation-lib` do KosmX usado no Minezin. Feito para animar o modelo do jogador sem conflitar com outros sistemas de animação; Fabric e NeoForge.
- Em uso: 1.1.6+mc.1.21.1. Documentação em https://docs.zigythebird.com/pal/intro
- Lê animações JSON do Blockbench e do Blender (formato GeckoLib/Bedrock) e também o formato do PlayerAnimator antigo. Arquivos em `assets/minerwk/player_animations/`.
- A camada é registrada no `FMLClientSetupEvent` dentro de `enqueueWork` (obrigatório no NeoForge). Atalhos em `client/animacao/AnimacoesJogador`.
- **Confirmar:** como sincronizar a animação para os outros jogadores verem (provavelmente um pacote nosso que manda tocar em cada cliente).

## SFX

Não há biblioteca de "SFX" que valha a dependência: o sistema de som do jogo (`SoundEvent` + `sounds.json`) basta, e o Minezin já tem um gerador em Python que monta as variações e sincroniza os ticks com o Java. O ganho está nas **fontes de áudio**:

| Fonte | Licença | Uso |
| --- | --- | --- |
| Freesound (filtro CC0) | CC0, sem atribuição | O que o Minezin já usa; manter o registro em `THIRD_PARTY_NOTICES.md` |
| Sonniss GDC Game Audio Bundle (2015–2026) | Royalty-free, uso comercial, sem atribuição **(ler o texto da licença no site da Sonniss)** | Banco grande e profissional: impactos, magia, vento, explosões |

Compatibilidade: testar com **Sound Physics Remastered** 1.5.0 (só cliente), que muda reverb e abafamento dos sons de mods.

## Build
- **ModDevGradle** (`net.neoforged.moddev`), o plugin recomendado hoje pelo NeoForge, no lugar do ForgeGradle do Minezin.
- Parchment para nomes de parâmetros; Java 21.

## Fontes
- Photon (CurseForge): https://www.curseforge.com/minecraft/mc-mods/photon
- Photon 2.2.0 NeoForge: https://www.curseforge.com/minecraft/mc-mods/photon/files/8480501
- Veil (GitHub): https://github.com/foundrymc/veil
- Veil NeoForge 1.21.1 (Maven): https://maven.blamejared.com/foundry/veil/veil-neoforge-1.21.1/4.0.0/veil-neoforge-1.21.1-4.0.0.pom
- Iris Veil Compat: https://modrinth.com/mod/iris-veil-compat/version/1.21.1+0.1.0-alpha?dep=1
- AAA Particles NeoForge 1.21.1: https://modrinth.com/mod/aaa-particles/version/neoforge-1.21.1-2.1.0?dep=1
- Lodestone: https://modrinth.com/mod/lodestonelib
- GeckoLib NeoForge 1.21.1: https://www.curseforge.com/minecraft/mc-mods/geckolib/files/8350073 · https://moddex.gg/mod/geckolib
- Player Animation Library: https://modrinth.com/mod/player-animation-library · https://www.curseforge.com/minecraft/mc-mods/player-animation-library/files/7611367
- Shine: VFX: https://www.curseforge.com/minecraft/mc-mods/shine-vfx/files/8150560
- Iris NeoForge 1.21.1: https://www.curseforge.com/minecraft/mc-mods/irisshaders/files/5910090
- Sound Physics Remastered 1.21.1: https://modrinth.com/mod/sound-physics-remastered/version/neoforge-1.21.1-1.5.0
- Sonniss GDC 2026: https://rekkerd.org/sonniss-releases-gdc-2026-game-audio-bundle/ · https://bedroomproducersblog.com/2026/03/16/sonniss-gdc-2026-bundle/
