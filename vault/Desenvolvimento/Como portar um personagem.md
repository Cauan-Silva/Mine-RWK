---
tags: [desenvolvimento, guia]
estado: primeira versão (10/11/2026)
---
# Como portar um personagem

Voltar para [[00 Início]]. Passo a passo tirado do que o [[Gojo]] ensinou. É a primeira versão: o Gojo ainda não tem som nem traje, então essas duas partes estão só esboçadas e serão completadas quando ele as tiver.

A ordem geral é a do [[Roadmap]]: pesquisa canônica → kit aprovado → código → VFX → SFX → animações → traje → teste no jogo. Cada parte grande segue o mesmo ciclo: perguntas, spec em `Specs/`, aprovação do autor, branch, testes primeiro, revisão, vault e teste no jogo.

## 1. Pesquisa e kit (sem código)
1. Copiar o [[Modelo de Personagem]] para `vault/Personagens/<Nome>.md`.
2. Preencher o kit canônico seguindo [[Kits Canônicos]]: cada habilidade com nome original, o que faz na obra, onde aparece e a fonte.
3. Anotar aparência e som de cada golpe com a cena de referência. É disso que o VFX e o SFX vão sair depois.
4. Só então adaptar para o jogo: tecla, dano, recarga, alcance e custo. Comparar com o kit do Minezin.
5. **Parar aqui e pedir a aprovação do autor.** Atualizar as tabelas de [[Personagens]] e de [[Kits Canônicos]].

## 2. Mecânica no núcleo (`core/`, com JUnit)
Tudo em `core/personagens/<nome>/`, sem classes do Minecraft.

| O que | Onde olhar no Gojo |
| --- | --- |
| O personagem: id, nome, obra, recurso, teclas e passivas | `Gojo.criar()` |
| O recurso (energia, fôlego, mana) | `EnergiaAmaldicoada`, que implementa `Recurso` |
| Cada golpe: uma `Habilidade` com os seus `Numeros` | classes internas de `Gojo` (`Azul`, `Vermelho`...) |
| O que dura vários ticks depois do disparo | um `Efeito` criado com `ctx.criarEfeito(...)`, como `FeixeRoxo` |
| Passivas | `TecnicaReversa`, que implementa `Passiva` |
| Golpe com muitas fases e constantes de tempo | uma classe só de números e contas, como `RoxoMaximo` |

**Personagem de transformação** (o [[Ichigo]]): cada transformação é uma `Forma` (id e golpes por tecla) na lista `transformacoes` do `Personagem`; a forma base são as habilidades de sempre. O golpe que transforma chama `ctx.mudarForma(id)`. Recargas e trancas valem por id de golpe, então a mesma tecla pode estar livre numa forma e trancada em outra. O recurso fica sabendo da troca por `Recurso.forma(id)`. Morrer volta à base, e a forma não é salva. O nome da transformação na tela de escolha vem da tradução `forma.minerwk.<id>`. Ver [[2026-11-11 Ichigo parte 1]].

Pontos que custaram caro no Gojo:
- **`Numeros`** guarda carga mínima e máxima, disparo, fim, se segura a tecla, custo e recarga. Golpe que dispara sozinho usa `Numeros.instantanea`.
- **O golpe só fala com o mundo pelo `Contexto`** (`ferir`, `danoUmaVez`, `puxar`, `arremessar`, `suspender`, `apagarCasca`, `visual`, `apresentacao`, `tremor`, `cutscene`...). Se faltar uma ação, ela entra em `Contexto` com corpo vazio e é implementada em `kit/ContextoServidor`.
- **Tempos em constantes, nunca soltos.** Animações, cutscene, VFX e testes leem as mesmas constantes. Quando o ritmo do R mudou, quem tinha número copiado quebrou.
- **Quem move o jogador é o cliente.** O servidor pode estar atrasado, então posições que dependem do movimento saem da mesma conta nos dois lados (ver `RoxoMaximo.deslocamentoGojo`), e não da posição que o servidor tem naquele tick.
- **Testes:** um teste por regra do golpe com o contexto falso de `GojoTest`, que anota o que foi chamado. Fases e curvas têm teste próprio (`RoxoMaximoTest`).

Registrar o personagem em `MineRWK` com `Personagens.registrar(...)` e pôr os textos em `assets/minerwk/lang/pt_br.json` e `en_us.json` (`habilidade.minerwk.<id>` e `.desc`). O emblema da tela de escolha fica em `textures/personagens/`.

Para o que só existe com o servidor rodando (blocos, entidades, barreiras), há GameTests em `kit/TestesGojo`.

## 3. VFX
Dividido em duas metades, como no Gojo:

| Metade | Onde | O que vai lá |
| --- | --- | --- |
| Formas e curvas | `core/vfx/` (JUnit) | trajetórias, raios, tempos, cores: `FusaoRoxoGojo`, `FluxoAzulGojo`, `EncantamentoGojo` |
| Desenho | `client/vfx/` | `RenderVisuaisGojo` (desenho à mão com o `Pincel`), `PhotonRoxoMaximo` (partículas do Photon montadas em código com `FxCodigo`), `DetritosGojo` (o que dispara por tick) |

- O servidor manda `ctx.visual(tipo, origem, direcao, forca, duracao)` e o cliente desenha pelo `tipo`. O campo `forca` leva um número livre para o cliente (raio, distância, ticks).
- Photon e Player Animation Library ficam só em `client/`.
- **Peso vem em camadas**, não de um efeito só: clarão, corpo, casca, fitas, onda de choque, detritos, fumaça depois, e o que fica no ar. A explosão do R só ficou boa na terceira rodada, quando ganhou tempo (3 s) e camadas.
- **Bloom:** cor HDR acima de 1 nas partículas do Photon. O desenho do `Pincel` não passa pelo bloom.
- Reações de tela ficam em `ImpactoCamera` (tremor, clarão, FOV), `ImpactFrame` (pós-processamento) e `CeuRoxo` (céu).
- Respeitar o `VFX reduzido` da config (`FxCodigo` já divide a quantidade por 2).
- **Prévia sem jogar:** `/rwk previa` e a run `clientDemo`, que grava um quadro por tick em `run/demo/` (`-Pdemo=encantamento` grava a carga). Vale adicionar um modo por personagem.

## 4. Animações do jogador
- As animações do Gojo saem de um gerador em Python, `ferramentas/gerar_animacoes_gojo.py`, que escreve `assets/minerwk/player_animations/gojo.json`. Um gerador por personagem.
- A animação do golpe tem o mesmo id do golpe e começa na carga (`ctx.apresentacao(id, "", "")`).
- Os tempos do gerador repetem as constantes do núcleo, e `AnimacoesGojoTest` confere que batem. Mudou o tempo no Java, mudar no gerador e gerar de novo.
- Conferir em 1ª pessoa e com F5. Ângulo torto se ajusta no gerador ou no Blockbench.

## 5. Cutscene (só para a ultimate)
- A câmera é uma `Coreografia` de quadros (`core/camera`), na base dos olhos do personagem: ver `CutsceneRoxoMaximo`.
- Corte seco usa o `corte` do quadro. Entre dois planos em lados opostos do alvo, sempre corte: o deslize atravessa o alvo.
- Teste obrigatório: a câmera nunca chega a menos de 2 blocos do alvo. Foi isso que bugava o rosto do alvo no R.

## 6. Som
Tudo sintetizado aqui, sem áudio de terceiros. Ver [[2026-11-10 Som do Gojo parte 1]].
- **Ids no núcleo:** uma classe `Sons<Personagem>` em `core/` com os ids e a lista `TODOS` (`SonsGojo`, `SonsToji`). `registro/Sons` registra a lista.
- **Gerador:** um `ferramentas/gerar_sfx_<personagem>.py` por personagem, sobre o motor `ferramentas/sfx_motor.py`. Cada som é montado em camadas (transiente, corpo, sub, cauda) e sai em 3 variações. O gerador escreve os `.ogg` em `assets/minerwk/sounds/<personagem>/`, as entradas do `sounds.json`, as legendas em português e inglês e o manifesto `ferramentas/sfx_<personagem>_manifesto.json`. `--preview arquivo.wav` grava todos em sequência para ouvir.
- **Tempos:** os marcos dentro de um som (a batida em cada palavra, o estouro) repetem os ticks do Java e ficam em `MARCOS`. Um teste JUnit (`SonsGojoTest`, `SonsTojiTest`) confere o manifesto contra o kit e contra a lista de ids. Mudou um tempo no Java, mudar no gerador e gerar de novo.
- **Quem toca:**
  - Som de gesto (o disparo) sai pelo terceiro argumento de `ctx.apresentacao`.
  - Som que acompanha um visual (carga, campo, explosão) é tocado no cliente por `SonsVisuais`, no ponto do visual. Assim ele é cortado se o visual for encerrado antes da hora, e o som de fim cai na hora certa. Qual som vai com qual visual fica no núcleo (`SonsGojo.doVisual`), com JUnit.
  - Entidade (boss) toca pelo servidor, com `level().playSound`.
- **Caráter:** combinar com o personagem. Os do Gojo têm tom e zumbido de energia; os do Toji, que não tem energia amaldiçoada, são secos (impacto, metal, ar).
- Créditos em `THIRD_PARTY_NOTICES.md`, mesmo sendo sintetizado (a linha diz qual gerador fez).

## 7. Traje
GeckoLib, automático ao escolher o personagem, sem item no inventário. Ver [[2026-11-10 Traje do Gojo]].
- **Modelo e textura:** os arquivos base ficam em `ferramentas/traje/` e um gerador (`gerar_traje_gojo.py`) aplica as mudanças e escreve os assets. Se vier do Minezin, registrar em `THIRD_PARTY_NOTICES.md`.
- **Como é desenhado:** pelo renderer de armadura do GeckoLib com um item escondido (`traje_gojo`), dentro de uma subclasse da camada de armadura do jogador. Tem de ser essa camada: em primeira pessoa a Player Animation Library só deixa passar a camada de armadura e a do item na mão.
- **Visibilidade das peças:** o renderer do GeckoLib mostra só o espaço de armadura pedido; o traje sobrescreve isso para seguir as partes do modelo do jogador.
- **Primeira pessoa:** a manga aparece na mão parada (`RenderArmEvent`) e nos braços animados.
- **Peças que mudam com o golpe** (a venda do Gojo): a regra fica no núcleo (`TrajeGojo.vendaFora`) e olha os visuais ativos do jogador. Brilho por máscara (`*_glowmask.png`).
- **Outros jogadores:** o servidor manda o personagem de cada jogador a quem o vê (`PersonagemDe`); sem isso só o dono veria o traje.
- **Cuidado com os ossos:** no modelo do Minezin "left" e "right" são de quem olha de frente.
- Prévia: um modo novo em `DemoVfx` (`./gradlew runClientDemo -Pdemo=traje`).

## 8. Maestria, árvore e Nêmesis
Só para personagem que tem despertar. Ver [[2026-11-10 Maestria e árvore de habilidades]] e [[2026-11-10 Toji e o despertar do Gojo]].
- **Árvore:** os nós do personagem ficam no núcleo (`ArvoreGojo`); a tela (tecla H) e a compra são genéricas.
- **Golpes trancados:** o kit diz quais golpes exigem o despertar; o HUD mostra o cadeado. `/rwk despertar <jogador> sim|nao` para testar.
- **Boss:** os golpes são dados (`core/boss/Golpe`: aviso, janela, pausa, recarga, alcance, peso) e o `MotorBoss` escolhe e marca o tempo; a entidade só faz o que cada golpe causa no mundo. As regras e os números ficam numa classe do núcleo (`TojiBoss`), com JUnit.
- **Todo golpe de boss avisa:** marca no chão mandada como `MostrarVisual` e desenhada no cliente (`RenderVisuaisToji`), que enche enquanto o aviso corre.
- **Teleporte de boss:** sempre conferir chão, parede e líquido antes (`cabeEm`); morte por queda no vazio contaria como vitória.
- **GameTests de boss:** um lote por teste (`batch`), chunks vizinhos forçados, jogador com conexão de mentira e no chão, esperas "até acontecer". Detalhes em [[2026-11-10 Toji parte 2]].

## 9. Fechar cada parte
1. `./gradlew build` verde.
2. Revisão final por um subagente barato.
3. Marcar no [[Roadmap]], escrever o diário em `vault/Diário/` e o índice [[Diário]], e listar em [[Pendências]] o que precisa ser visto no jogo.
4. Abrir o jogo para o autor testar. `/rwk trocar @s <personagem>` troca de kit e `/rwk recarregar` enche a energia e zera as recargas.
5. Mesclar só quando o autor pedir.
