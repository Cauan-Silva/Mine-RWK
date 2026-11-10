---
tags: [planejamento]
---
# Decisões

Voltar para [[00 Início]]. Registro do que foi decidido, quando e por quê.

| Data | Decisão | Motivo | Estado |
| --- | --- | --- | --- |
| 08/11/2026 | Projeto novo em NeoForge, sucessor do Minezin | Pedido do autor | decidido |
| 08/11/2026 | Kits seguem a obra original, com fonte anotada | Pedido do autor; regra em [[Kits Canônicos]] | decidido |
| 08/11/2026 | Vault do Obsidian dentro do repositório (`vault/`) | Mesmo formato do Minezin | decidido |
| 08/11/2026 | Minecraft 1.21.1 + NeoForge 21.1.x | Ecossistema maior e estável, tem Veil. 26.3 também tem Photon (a pesquisa inicial errou), mas foi descartada | aprovado |
| 08/11/2026 | Photon2 como VFX principal; Veil só se precisar | Continuidade com o Minezin; ver [[Bibliotecas]] | aprovado |
| 08/11/2026 | GeckoLib + Player Animation Library | Ver [[Bibliotecas]] | aprovado |
| 08/11/2026 | SFX sem biblioteca extra; fontes CC0 e Sonniss | Ver [[Bibliotecas]] | aprovado |
| 08/11/2026 | Gojo como primeiro personagem | Mais completo no Minezin | aprovado |
| 08/11/2026 | Pacote `com.minerwk`, mod id `minerwk` | Nome do projeto | decidido |
| 09/11/2026 | Recurso próprio por personagem, com regras próprias | Mais fiel e interessante | aprovado |
| 09/11/2026 | Personagem escolhido uma vez, só pelo menu; troca por item raro ou operador | Pedido do autor | aprovado |
| 09/11/2026 | Habilidades instantâneas ou de segurar (força pela carga) | Pedido do autor | aprovado |
| 09/11/2026 | Custo cobrado no disparo; interrupção dá recarga de 1/4 | Pedido do autor | aprovado |
| 09/11/2026 | Habilidades como classes Java, números num record `Numeros` | Obras com habilidades muito diferentes | aprovado |
| 09/11/2026 | Sem transformações (o Minezin tinha); kit inteiro ao escolher | Mais simples; o autor não vê problema | aprovado |
| 09/11/2026 | Golpes que duram viram efeitos soltos; o jogador segue usando outras teclas | Infinito e domínio não podem travar o kit | aprovado |
| 09/11/2026 | Roxo apaga blocos por padrão, com gamerule `minerwkApagarBlocos` | Fiel à obra, desligável no servidor | aprovado |
| 09/11/2026 | Ultimate (R) de cada personagem tem cutscene cinemática; sistema de câmera reutilizável | Pedido do autor | feito: `core/camera/Coreografia` + `client/camera/CutsceneCliente` (Roxo 200% primeiro) |
| 09/11/2026 | Peso dos golpes vem de animação, reação do alvo, som e efeitos desenhados, nessa ordem; referência: Gojo x Sukuna no mangá | O autor achou os golpes simples mesmo com tremor e brilho; código geométrico sozinho não dá impacto | aprovado |
| 10/11/2026 | O Toji não é personagem jogável: é o Nêmesis do Gojo, o boss do despertar. O Gojo começa com Azul e Infinito; vencer o Toji, invocado com um item, libera a Técnica Reversa, o Vermelho e o Roxo. Fazer depois do traje do Gojo | Correção do autor: como no Minezin, cada personagem tem o seu boss, e o despertar segue a obra (arco do Inventário Oculto) | decidido; spec a escrever. Ver [[Toji]] |
| 10/11/2026 | Maestria por personagem e árvore de habilidades vêm antes do Toji. O nível só dá pontos; a árvore compra melhorias e passivas, não golpes. O Toji exige o nível de maestria e o nó Despertar; o despertar acontece no meio da luta e libera o kit inteiro, com o G e o R; o item de invocação sai de uma receita | Pedido do autor: "deve se ter um sistema de maestria e árvore de habilidades, a boss fight deve se iniciar depois de um tempo" | spec aguardando revisão: [[2026-11-10 Maestria e árvore de habilidades]] |
