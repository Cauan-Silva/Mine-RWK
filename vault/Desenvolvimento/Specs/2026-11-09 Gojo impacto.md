---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec e plano: impacto dos golpes do Gojo (Fase 2, parte 2b)

Voltar para [[Roadmap]]. Vem do teste de 09/11/2026: o VFX funciona, mas os golpes parecem simples e sem peso. Hoje só existe o desenho do efeito. A câmera não reage, nada sai do chão e falta brilho. O autor escolheu três frentes: **a câmera reage**, **detritos e partículas**, **efeitos maiores e brilhantes**. O som fica para o pipeline de áudio.

## Objetivo
Pronto quando:
- os JUnit novos passam e o build passa;
- no jogo, cada golpe forte se sente: a tela treme e pisca, o campo de visão dá um tranco, voam pedaços dos blocos de verdade, e as esferas brilham em volta;
- quem está longe sente menos, e quem está fora do raio não sente nada;
- o tremor obedece a `intensidadeTremor` da config, e o `VFX reduzido` corta pela metade as partículas e o brilho.

## Desenho

### 1. A câmera reage (só no cliente, sem pacote novo)
O cliente já recebe o começo de cada visual e a explosão do Vermelho. A reação da câmera nasce desses mesmos eventos, conforme a distância da câmera até o ponto. A tabela fica numa função pura, `core/vfx/ImpactoGojo`, com JUnit. Para cada momento ela devolve: tremor (força e ticks), clarão (cor, alpha e ticks) e soco de FOV (% e ticks). Tudo cai linearmente até zero no raio.

| Momento | Tremor | Clarão | FOV | Raio |
| --- | --- | --- | --- | --- |
| Azul abre | 0,25 · 10 | ciano 0,15 · 3 | −5% · 10 (puxa) | 12 |
| Vermelho explode | 0,8 · 8 | branco avermelhado 0,35 · 4 | +6% · 6 | 24 |
| Roxo é lançado | 1,0 · 12 | violeta 0,45 · 5 | +8% · 8 | 30 |
| Roxo 200% carga | sobe de 0,1 a 0,6 até o fim da carga | — | — | 16 |
| Roxo 200% é lançado | 1,6 · 20 | branco 0,7 · 6 | +12% · 12 | 48 |
| Vazio Infinito abre | 0,5 · 10 | branco azulado 0,8 · 3 | −8% · 20 | 16 |

- **Tremor:** chama o `TremorCamera` que já existe, com a força já reduzida pela distância.
- **Clarão:** uma camada de GUI de tela cheia (`RegisterGuiLayersEvent`) que pinta a cor com o alpha e se apaga rápido.
- **FOV:** `ViewportEvent.ComputeFov` multiplica o FOV pelo soco, que volta suave ao normal.
- **Infinito:** não reage, por ser um golpe sutil.

### 2. Detritos e partículas
- **Roxo (servidor):** `ContextoServidor.apagarBlocos` solta partículas de bloco (`BlockParticleOption`) com o estado real de cada bloco apagado. São no máximo 12 blocos sorteados por chamada, 4 partículas cada, espalhadas pela área. Isso serve para qualquer personagem futuro que apague blocos.
- **Vermelho (cliente), na explosão:**
  - um anel de 24 partículas com pedaços do chão embaixo do ponto, voando para fora e para cima;
  - 20 faíscas vermelhas (`DustParticleOptions`), 10 de fumaça (`POOF`) e um `EXPLOSION`.
- **Azul (cliente):** a cada tick, 3 pedaços do chão num raio de 4 são puxados para o núcleo.
- **Roxo e Roxo 200% (cliente):** 4 faíscas violeta por tick no rastro da massa (o dobro no 200%).
- **Vazio Infinito (cliente):** na abertura, um anel de poeira (`CLOUD`) se espalha pelo chão até o raio 12.
- **Distância:** nada disso aparece a mais de 64 blocos da câmera.
- **VFX reduzido:** metade das partículas.

### 3. Maiores e brilhantes
- **Brilho falso, no lugar do bloom:** um modo novo no shader, o **6**. É um quadrado virado para a câmera, aditivo, com queda radial suave. `Pincel.brilho(centro, raio, cor, alpha)`.
- **Cada esfera ganha um brilho de 3 a 4 vezes o raio:**
  - Azul: ciano;
  - Vermelho: carmesim, e a explosão ganha um brilho grande que some em 8 ticks;
  - Roxo: violeta, com uma pulsação.
- **Tamanhos:**
  - esfera do Vermelho em voo: de 0,35 + 0,15·força para 0,55 + 0,25·força;
  - carga do Vermelho: 1,4×;
  - onda da explosão: 1,5×;
  - Azul: 1,3×;
  - massa do Roxo: 2,1 e 3,5, antes 1,9 e 3,2;
  - fitas: 1,5× mais largas.
- **Fica fora o bloom de verdade** (pós-processamento): briga com o Iris e custa FPS, e o brilho falso cobre quase todo o efeito.

### Testes (JUnit, `ImpactoGojoTest`)
- A força cai com a distância, é zero no raio e fora dele, e é a cheia em cima do ponto.
- O Roxo 200% é o mais forte dos momentos.
- O alpha do clarão fica entre 0 e 1 e chega a 0 depois dos ticks.
- O soco de FOV volta a 1.
- O tremor da carga do Roxo 200% sobe ao longo da carga.

## Plano
1. `ImpactoGojo` com testes (vermelho → verde).
2. Cliente: `client/vfx/ImpactoCamera` (clarão, FOV e chamadas ao tremor), ligado ao `VisuaisAtivos.receber`/`encerrar` e registrado no `MineRWKClient`.
3. Partículas: detritos no `ContextoServidor.apagarBlocos`, e `client/vfx/DetritosGojo` chamado no tick para os visuais ativos.
4. Shader modo 6, `Pincel.brilho`, brilho nas esferas e tamanhos novos.
5. Build, GameTests, revisão com sonnet, vault, depois o teste do autor.
