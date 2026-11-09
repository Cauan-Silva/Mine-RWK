---
tags: [desenvolvimento, spec]
estado: aguardando revisão
---
# Spec: HUD e cliente (Roadmap 1.4, parte 1)

Voltar para [[Roadmap]]. Desenho aprovado com o autor em 09/11/2026. O 1.4 foi dividido em três partes: **1) HUD e cliente** (esta), 2) tela de escolha bonita, 3) pipeline de áudio (no começo da Fase 2).

## Objetivo
Mostrar recurso e recargas na tela, dar ao jogador opções de cliente e ter um tremor de tela próprio que os golpes possam pedir. Pronto quando, com o personagem `teste`, o HUD mostra as recargas contando, o recurso enchendo e o X faz a tela tremer conforme a força, e os testes passam.

## HUD
- Posição: centralizado acima da barra de itens, por cima dos corações e da fome. Linha de quadradinhos (Z, X, C, V, G, R) e embaixo a barra do recurso.
- Só aparecem as teclas que o personagem tem. Sem personagem, nada aparece. Escondido com `mostrarHud = false` ou com a interface oculta (F1).
- Quadradinho:
  - Pronto: letra da tecla.
  - Em recarga: escurecido de cima para baixo pela fração restante e os segundos restantes (a partir de 1 s: inteiro arredondado para cima; abaixo de 1 s: uma casa arredondada para baixo, mínimo `0.1`).
  - Sem recurso para o custo: letra vermelha.
  - Em uso (carga ou disparo): borda clara.
- Barra do recurso: cor do personagem e texto pequeno "Nome atual/máximo" (ex.: "Fôlego 70/100", inteiros).
- Suavidade: a recarga é calculada no cliente a partir do tick em que fica pronta e do tempo do mundo do cliente; a barra desliza até o valor novo em ~4 ticks.

## Mudanças no núcleo e na rede
- `Recurso` ganha `String nome()` e `int cor()` (RGB). `RecursoFolego`: "Fôlego", verde `0x1D9E75`.
- `SincronizarKit` passa a levar também: nome e cor do recurso, a duração total de cada recarga (para a fração), o custo de cada habilidade, a tecla de cada habilidade e qual habilidade está em uso (vazio = nenhuma). Enviado também quando a habilidade em uso muda.
- `Contexto` ganha `tremor(float forca, int duracao, double raio)`.
- Pacote novo `Tremer(float forca, int duracao)` servidor → clientes. O servidor manda aos jogadores a até `raio` blocos, com força reduzida linearmente pela distância (força × (1 − distância/raio)).

## Lógica pura (JUnit)
- `LogicaHud` (`core/hud/`, sem Minecraft): fração restante da recarga, texto dos segundos, se mostra vermelho (`recurso < custo`), lista de teclas a mostrar na ordem Z X C V G R.
- `Tremor` (`core/hud/` ou `core/camera/`): soma de tremores ativos com decaimento suave até zero no fim da duração; limite máximo da soma; multiplicado pela intensidade da configuração; dá deslocamentos de guinada, inclinação e rotação a partir de senos de frequências diferentes (sem aleatório, para ser testável).

## Configuração do cliente
`ModConfigSpec` do tipo CLIENT, arquivo `minerwk-client.toml`, editável no menu de Mods:
| Opção | Padrão | Efeito agora |
| --- | --- | --- |
| `intensidadeTremor` | 1.0 (0 a 1) | Multiplica o tremor; 0 desliga |
| `vfxReduzido` | false | Só fica salva; o Photon passa a respeitar quando houver efeitos |
| `sfxAnime` | true | Desligado, os sons do `TocarApresentacao` não tocam |
| `mostrarHud` | true | Liga e desliga o HUD |

## Personagem de teste
`teste_carga` (X) chama `tremor(0.3 + 0.7 × força, 15, 12)` no disparo.

## Testes
- JUnit: `LogicaHud` (fração, segundos: 61 ticks → "4", 19 ticks → "0.9", 7 ticks → "0.3", 1 tick → "0.1", 0 → pronto; vermelho; teclas na ordem e só as existentes), `Tremor` (decai a zero, soma limitada, intensidade 0 → 0, dois tremores somam), `RecursoFolego` nome e cor.
- GameTest: o golpe X do `teste` não falha ao pedir tremor (jogador falso sem canal).
- No jogo: HUD aparece só depois de escolher; recarga conta; letra fica vermelha sem fôlego; X treme a tela; opção 0 tira o tremor; `mostrarHud` esconde.

## Fora desta parte
Ícones das habilidades (com o Gojo), retratos e tela de escolha (parte 2), áudio (parte 3), HUD de alvo/boss.
