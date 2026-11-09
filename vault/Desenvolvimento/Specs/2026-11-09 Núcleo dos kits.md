---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec: núcleo dos kits (Roadmap 1.3)

Voltar para [[Roadmap]]. Decisões tomadas com o autor em 09/11/2026 (registradas em [[Decisões]]).

## Objetivo
Base que todos os 15 personagens usam: personagem, habilidades por tecla, recurso, recargas, ciclo de carga/disparo/fim, interrupção, dados do jogador, rede e uma tela simples de escolha. Pronto quando um personagem de teste usa as duas habilidades no jogo, com recurso e recargas funcionando, e os testes passam.

## Decisões
| Assunto | Decisão |
| --- | --- |
| Recurso | Cada personagem tem um recurso próprio com regras próprias (ex.: fôlego do Tanjiro enche parado, fúria do Guts enche apanhando) |
| Escolha | Uma vez só, **pelo menu**. Trocar: item raro (fase futura) ou comando de operador |
| Teclas | Cada habilidade é instantânea (carga fixa) ou de segurar (solta para disparar, força proporcional, carga máxima) |
| Interrupção | Custo só é cobrado no disparo; interrompida na carga → recarga de punição = 1/4 da recarga normal |
| Definição | Uma classe Java por habilidade, números agrupados num registro `Numeros` (facilita levar para config depois) |
| Fora do 1.3 | Sistema de postura (Fase 4), item de troca, tela bonita (1.4), HUD (1.4) |

## Parte 1: núcleo puro (`core/kit/`, sem Minecraft, JUnit)
- `Personagem`: id, nome, obra, `Supplier<Recurso>`, `Map<Tecla, Habilidade>`.
- `Tecla`: enum `Z, X, C, V, G, R` (R = ultimate).
- `Habilidade`: id, `Numeros numeros()`, ganchos `aoIniciarCarga(Contexto)`, `aoDisparar(Contexto, float forca)`, `aoTerminar(Contexto)` (padrão: nada).
- `Numeros` (record): `cargaMin`, `cargaMax`, `disparo`, `fim` (ticks), `segurar` (bool), `custo`, `recarga` (ticks). Instantânea: `cargaMin == cargaMax`.
- `Contexto`: interface que a camada do Minecraft implementa (posição, olhar, dano em área, tocar apresentação). Cresce conforme os personagens pedirem.
- `Recurso`: `atual()`, `maximo()`, `pode(custo)`, `gastar(custo)`, `tick(EstadoDoJogador)`, `salvar()`/`carregar(double)`.
- `EstadoDoJogador` (record): parado, apanhou neste tick, causou dano neste tick.
- `Execucao`: máquina de estados CARGA → DISPARO → FIM → livre, um passo por tick. Entradas: `soltar()`, `interromper()`. Saídas (eventos): `Disparou(forca)`, `Terminou`, `Interrompida`. Força = `(tempo de carga − cargaMin) / (cargaMax − cargaMin)` limitada a [0, 1]; instantânea = 1. Na habilidade de segurar, chegar em `cargaMax` dispara sozinho; soltar antes de `cargaMin` espera até `cargaMin`.
- `KitDoJogador`: personagem + recurso + `Recargas` + execução atual. `apertar(Tecla, agora)`, `soltar(Tecla)`, `atordoar()`, `tick(agora, estado, contexto)`. Regras: uma execução por vez; só começa se recarga pronta e `recurso.pode(custo)`; cobra o custo no disparo (se não puder mais pagar no disparo, vira interrupção); recarga normal começa no disparo; interrupção na carga → recarga de `recarga / 4`.

## Parte 2: servidor (`kit/`)
- Data Attachment `DadosKit` (salvo, copiado na morte): id do personagem (vazio até escolher), valor do recurso, recargas. A execução em andamento não é salva.
- `Personagens`: mapa id → `Personagem`, preenchido no início do mod.
- `ServicoKits`: tick do servidor por jogador com kit; monta `EstadoDoJogador` (parado pela velocidade; apanhou/causou dano pelos eventos de dano do NeoForge).
- `ContextoServidor`: implementa `Contexto` com o mínimo.
- Comando de operador `/rwk trocar <jogador> <personagem>` (administração e testes).
- Personagem `teste`: habilidade instantânea (dano em área curta) e uma de segurar (dano proporcional à força); recurso que enche parado. Escondido da tela quando houver personagens reais.

## Parte 3: rede e cliente
- Teclas Z, X, C, V, G, R configuráveis (`RegisterKeyMappingsEvent`); o cliente só manda apertou/soltou.
- Payloads: `UsarTecla` (C→S: tecla, apertou), `EscolherPersonagem` (C→S: id; servidor recusa se já tiver personagem), `SincronizarKit` (S→jogador: personagem, recurso, recargas; só quando muda), `TocarApresentacao` (S→próximos: jogador, animação, efeito, som).
- Tela de escolha simples: lista de botões (nome e obra) + confirmar com aviso de escolha definitiva. Abre ao entrar sem personagem e por uma tecla enquanto não escolheu.

## Testes
- JUnit: `Execucao` (instantânea, segurar, carga máxima, soltar cedo, interrupção), `KitDoJogador` (custo no disparo, recarga, punição 1/4, uma por vez, sem recurso), recurso do personagem de teste.
- GameTest: jogador falso com o personagem `teste` usa a habilidade; recurso diminui e recarga começa.
- No jogo (anotar em [[Pendências]]): escolher pelo menu, usar as duas habilidades, ver a animação em outro jogador.
