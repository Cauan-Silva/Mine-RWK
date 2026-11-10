---
tags: [desenvolvimento, spec]
estado: aguardando revisão
---
# Spec e plano: preparação, ritmo e VFX pesado do Vazio Roxo máximo (Fase 2, parte 8)

Voltar para [[Roadmap]]. Vem do teste de 10/11/2026 (depois de [[2026-11-09 Cutscene do Vazio Roxo máximo]]). O autor achou que a cutscene melhorou, mas o golpe "ainda está cru no quesito VFX, não existe uma preparação, ocorre tudo muito rápido".

> **Escolhas propostas (a confirmar):** o autor pediu para seguir sem as perguntas. As opções abaixo são as minhas recomendações e estão marcadas com (proposto); basta dizer o que trocar.

## Objetivo
Pronto quando:
- o R tem um crescendo visível do encantamento até o estouro: energia juntando, céu escurecendo, a fusão carregando e um instante de silêncio antes de explodir;
- a sequência depois do disparo dura cerca do dobro;
- a explosão tem camadas, onda de choque e rastro;
- os testes passam e o autor aprova no jogo.

## 1. Ritmo, cerca de 2x mais lento (proposto)
Os tempos ficam em `RoxoMaximo`. A cutscene e as animações se ajustam sozinhas, porque já leem essas constantes.

| Fase | Hoje | Novo |
| --- | --- | --- |
| Avanço | até 20 ticks a 3 b/t | igual (é o "tranco") |
| Subida com o alvo | 15 | 30 |
| Esferas (mãos até atrás do Gojo) | 10 | 30 |
| Fusão (carregando) | 10 | 40 |
| Explosão (até o raio 40) | 16 | 30 |
| Total do disparo ao fim da explosão | ~51 | ~130 (6,5 s) |

- Sem alvo: as esferas viajam 30 ticks e a fusão leva 40.
- A animação do R e a cutscene esticam junto: o plano aberto ganha mais 20 ticks para mostrar o crescimento.

## 2. Preparação (proposto: tudo)
**Encantamento (4 s), a energia juntando:**
- **Aura:** fitas azuis e vermelhas sobem em espiral em volta do Gojo e ficam mais fortes a cada palavra. Os olhos brilham, com um brilho azul na altura da cabeça.
- **Chão:** rachaduras que crescem a partir dos pés. Pedras e terra flutuam e giram devagar (só visual, sem tirar blocos).
- **Céu:** escurece aos poucos para todos perto, até o roxo ser a luz principal na explosão. Volta ao normal devagar depois.

**A fusão das esferas (pedido do autor, 10/11: "devem se fundir de uma forma mais bonita"):**
Hoje as duas esferas chegam em arco e trocam de repente pelo núcleo roxo. A proposta é um encontro em quatro tempos:
1. **Dança:** ao chegar atrás do Gojo, o Azul e o Vermelho não se batem. Eles giram um em volta do outro numa espiral que vai fechando (duas voltas e meia), cada um deixando o seu rastro. Os rastros se trançam como uma hélice dupla.
2. **Atração:** quanto mais perto, mais as esferas se deformam uma na direção da outra, esticando como gotas. Um filete de energia liga as duas, e a cor dele vai do ciano ao carmesim passando pelo violeta. O espaço entre elas ondula.
3. **Toque:** quando se encostam, as duas param por 2 ticks, giram coladas e as bordas misturam as cores. Sai um anel fino de luz branca no plano da órbita.
4. **Nascimento do roxo:** as duas afundam uma na outra e somem num ponto. Depois de um clarão curto, o núcleo roxo nasce pequeno e escuro, com borda violeta, e começa a carregar (o resto da fusão).

O tempo das esferas (30 ticks) se divide assim: 18 de viagem em arco e 12 de dança e toque. A forma da espiral e da deformação fica no `core/vfx`, com testes.

**Fusão carregando (40 ticks):**
- o núcleo suga ar, poeira e detritos de volta para ele;
- pulsa cada vez mais rápido;
- solta anéis de choque pequenos e relâmpagos roxos curtos.
- **Antes do estouro:** 4 ticks com o núcleo encolhendo, a tela clareando para branco e o tremor parando, como um silêncio.

**Câmera lenta:** fica de fora. O Minecraft não deixa desacelerar o mundo só para um jogador; o peso vem do ritmo novo e da pausa antes do estouro.

## 3. Explosão pesada (proposto: tudo)
**Camadas:**
- núcleo branco ofuscante;
- casca roxa com borda ondulando, como distorção;
- fitas de energia saindo em raios.

**Onda de choque:**
- um anel horizontal que corre pelo chão até o raio 60, levantando poeira;
- um segundo anel no ar.

**Detritos:**
- pedaços de bloco voando para fora, no lugar dos que somem, só visual e em quantidade limitada;
- poeira grossa.

**Rastro:**
- a cratera solta fumaça por 5 s;
- partículas roxas pairam no ar;
- o céu volta devagar.

**Tremor:** longo e grave para quem está perto. O som fica para a parte de som.

## 4. Como fazer: o que os tutoriais mudaram (proposto)
Fonte: [[Tutoriais de VFX e animação]]. Hoje todo o VFX do Gojo é desenhado à mão e a pasta `fx/` está vazia. Os tutoriais mostram que o Photon2 dá de graça boa parte do que este spec pede. A proposta é dividir assim:

| Camada | Quem faz | Por quê |
| --- | --- | --- |
| Esferas, dança em hélice, gota, toque, núcleo, casca e tempos | **nosso código** (`core/vfx` + `RenderVisuaisGojo`) | forma exata, casada com a cutscene e testada em JUnit |
| Rastros trançados da dança, fitas da aura | **Trails** do Photon presos às esferas | rastro suave sem escrever um renderizador de fita |
| Sucção (poeira e ar convergindo), faíscas do toque, relâmpagos curtos | **Photon**: Shape em esfera, Noise com *position offset* em curva (espalhado para zero), Bursts | o tutorial faz exatamente "juntar e espalhar" assim |
| Detritos, poeira grossa, fumaça da cratera | **Photon**: Physics com colisão, Sub Emitter ao morrer (detrito que cai solta poeira) | quantidade e física baratas |
| Clarão, céu escurecendo e tela clareando no silêncio | **faixa de pós-processamento** na linha do tempo do FX (`PostProcessTrack`) ou o nosso código, o que for mais simples no teste | a parte de tela fica num lugar só |

**Brilho de verdade (bloom):**
- No Photon2, cor HDR acima de 1 brilha sozinha. O nosso desenho à mão não passa por esse pipeline, por isso o roxo hoje "não brilha".
- Proposta: os efeitos do Photon usam materiais HDR, e as cascas e o núcleo ganham uma versão HDR (o Photon expõe `HDRTarget` e um pós-processamento de bloom).

**Shader da casca da explosão**, com as técnicas do tutorial do escudo:
- **fresnel** (já temos o `rim`);
- **linha de luz onde a casca corta o terreno**, comparando com a profundidade da cena;
- **distorção** do que está atrás, usando a cor da cena. É o "ondulando, como distorção" da seção 3.

**Quem cria os `.fx`:** o arquivo é um NBT. O caminho mais barato é montar **um** efeito-base no editor (`/photon editor`), seja pelo autor ou por mim controlando a tela, e daí em diante gerar ou ajustar as variações por script em `ferramentas/`.

### Parte 0: teste rápido antes de tudo (meio dia)
Antes de programar o resto, provar no jogo três coisas, com código descartável:
1. um `.fx` simples (rajada com rastro e cor HDR) tocando por `EfeitosVisuais` e brilhando;
2. o nosso desenho do núcleo com cor HDR passando pelo bloom do Photon;
3. o shader lendo a profundidade e a cor da cena (linha de interseção e distorção).

Se o item 2 ou o 3 falhar, as cascas ficam como estão e só as partículas (seções 2 e 3) vão para o Photon. Se o item 1 falhar, tudo fica à mão, como no plano original.

## Desempenho
- Tudo é desenhado no cliente, com limite de partículas e o `vfxReduzido` da configuração. Os efeitos do Photon também respeitam o `vfxReduzido`: com ele ligado, tocam uma versão com menos partículas ou nem tocam.
- O servidor só ganha visuais novos curtos: a aura da carga e o silêncio antes do estouro.
- O apagamento de blocos continua o mesmo.

## Teste
- **JUnit:**
  - as fases novas no `RoxoMaximo` e no `RoxoMaximoTest`;
  - a cutscene acompanhando os tempos novos;
  - as formas puras novas (aura, anel de choque, pulso da fusão acelerando, curva do céu) em `core/vfx`.
- **`GojoTest`:** o silêncio antes do estouro e a explosão no tempo novo.
- **GameTest:** o de sempre (zumbi preso no ar pelo tempo novo).
- **No jogo:** o crescendo e a explosão de longe; o brilho (bloom) com e sem shaders; o `vfxReduzido` ligado.
- **Parte 0:** os três itens do teste rápido, com print de cada um.

## Fora deste spec
Som (próxima parte); VFX pesado dos outros golpes.
