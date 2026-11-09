---
tags: [desenvolvimento]
---
# Do Minezin para o Mine-RWK

O que muda ao sair de Forge 1.20.1 para NeoForge 1.21.1. Voltar para [[00 Início]].

## Ponto de partida (Minezin em 08/11/2026)
- Forge 1.20.1, ForgeGradle 6, cerca de 464 arquivos Java, quase todos em `core/` (cliente 69, vfx 66, rpg 35, e um pacote por personagem).
- Dependências: Photon 1.1.17, LDLib 1.0.42, GeckoLib 4.4.9, player-animation-lib (KosmX) 1.0.2.

## Mudanças principais
| Área | Forge 1.20.1 (Minezin) | NeoForge 1.21.1 (Mine-RWK) |
| --- | --- | --- |
| Build | ForgeGradle (`fg.deobf`) | ModDevGradle, sem deobf |
| Java | 17 | 21 |
| Rede | `SimpleChannel` | `CustomPacketPayload` + `StreamCodec`, registrado em `RegisterPayloadHandlersEvent` |
| Dados no jogador | Capabilities | Data Attachments (`AttachmentType`) |
| Itens | NBT | Data Components |
| Eventos | `MinecraftForge.EVENT_BUS` | `NeoForge.EVENT_BUS` e o bus do mod |
| Recursos | `new ResourceLocation(ns, path)` | `ResourceLocation.fromNamespaceAndPath(ns, path)` |
| VFX | Photon 1 | Photon2 + LDLib2 |
| Animação do jogador | KosmX playerAnimator | Player Animation Library (ZigyTheBird) |

## O que dá para reaproveitar quase direto
- Lógica dos kits (tempos, danos, máquinas de estado), desde que separada da API do Forge.
- Texturas, modelos do Blockbench, sons e geradores em Python (`tools/`).
- Testes JUnit da lógica pura.
- Notas do vault do Minezin como referência.
