---
tags: [desenvolvimento, spec]
estado: aguardando revisão do autor
---
# Spec: UI dos personagens

Voltar para [[Roadmap]]. Pedido do autor (12/11), antes do próximo personagem: cada personagem com a sua interface, com ícones nos golpes, a recarga visível e uma árvore de habilidades própria. Parte do que hoje existe vem de [[2026-11-09 HUD e cliente]] (HUD com letras) e de [[2026-11-10 Maestria e árvore de habilidades]] (árvore em grade de texto).

## Escolhas do autor (12/11)
1. **Árvore: visual e forma.** Cada personagem tem um tema próprio, e a árvore vira uma árvore de verdade, com galhos ligando os nós. **As regras de compra não mudam** (custo, nível mínimo, Despertar).
2. **HUD: tema completo.** Ícone em cada golpe, a recarga em varredura, e a moldura e a barra de recurso no estilo do personagem. A HUD fica onde está hoje, acima dos corações.
3. **Ícones: gerador Python.** `ferramentas/gerar_icones.py` desenha tudo. Os PNGs gerados não se editam à mão: para ajustar, muda o gerador.

## Onde ficam as coisas
Os dados ficam em `core/`, sem classes do Minecraft e com testes JUnit. As telas em `client/` só desenham o que os dados dizem. Personagem novo = preencher o tema, a forma da árvore e os ícones; nenhuma tela nova.

- **`core/hud/TemaUi`** (record): `corPrincipal`, `corSecundaria`, `corFundo`, `corBorda`, `moldura` (enum `Moldura`: `LISA`, `CORTADA`, `ORNADA`), `fundoArvore` (nome da textura). **`core/hud/Temas`**: `tema(personagem, forma)` devolve o tema; um personagem pode trocar de tema por forma; quem não tem tema recebe `Temas.GENERICO` (o cinza de hoje).
- **`core/hud/FormaArvore`**: a lista de galhos do personagem. `Galho(id, icone, depoisDoDespertar, List<String> nos)`, mais um mapa opcional `nó → Selo` (enum `Selo`: `FORCA`, `RECARGA`, `CARGA`, `DURACAO`, `RECURSO`) para os nós cujo selo não é o do tipo. Cada `ArvoreX` (Gojo, Ichigo, Cid) ganha a sua `FORMA`, ao lado da `ARVORE`.
- **`core/hud/LayoutArvore`** é refeito: em vez da grade, posiciona o tronco, os galhos e os nós a partir da `FormaArvore`, e devolve também as linhas (os galhos) entre eles. Continua com rolagem e com `indiceEm(x, y, rolagem)`.
- **`core/hud/LogicaHud`** ganha a geometria da varredura da recarga (o setor escuro como lista de pontos, a partir da fração) e o brilho de "ficou pronto" (intensidade por tick desde que a recarga acabou).
- **Ícones:** `textures/gui/icones/<id>.png`, 32×32, desenhados em 20×20 na HUD e em 24×24 na árvore.

## Os ícones
- **Um por golpe:** todo `id` de habilidade que aparece na HUD, em qualquer forma (Shikai, Bankai e Hollow no Ichigo; o R agachado do Cid conta como o próprio golpe). A lista sai das definições dos personagens, não é escrita à mão no gerador: um teste confere que nenhum falta.
- **Um por recurso:** Energia amaldiçoada, Reiatsu, Magia.
- **Um para o Despertar** de cada personagem.
- **Selos dos nós**, num canto do ícone do galho:
  - **O selo padrão vem do tipo do nó:** `MELHORIA` é força (uma seta para cima), `RECARGA` é um relógio, `CARGA` é um raio.
  - **A `FormaArvore` pode trocar o selo de um nó:** duração vira uma ampulheta; reserva e fluxo do recurso viram uma gota.
- **Estilo:** pixel art com contorno escuro, o símbolo do golpe no centro, nas cores do tema do personagem. Exemplos: o Azul é uma esfera azul com o redemoinho; o Getsuga é o arco de energia; a Prisão do Cid, as correntes. O autor aprova pela prévia e pede ajustes ao gerador.
- **Sem ícone:** a HUD mostra a letra da tecla, como hoje. Nunca quebra.

## Os temas
| Personagem | Cores | Moldura | Fundo da árvore |
|---|---|---|---|
| Gojo | azul claro e branco, toque roxo | `LISA`, fina e clara | céu escuro com o brilho azul do Infinito |
| Ichigo (Shikai e Bankai) | preto e vermelho do reiatsu | `CORTADA`, cantos em lâmina | preto com a lua do mundo interior |
| Ichigo (Hollow) | branco osso e vermelho | `CORTADA` | o mesmo, mais avermelhado |
| Cid | preto e roxo-violeta, toque dourado | `ORNADA`, cantos do Shadow Garden | noite roxa com estrelas |
| Os outros | o cinza de hoje | `LISA` | escuro liso |

Os fundos e as molduras também saem de `gerar_icones.py` (ou de um `gerar_ui.py` irmão, se o arquivo ficar grande).

## Parte 1: ícones, temas e HUD
- O gerador e todos os ícones dos três personagens.
- `TemaUi`, `Temas` e os temas da tabela.
- **HUD nova:**
  - Quadros de 20 px com a moldura do tema e o ícone.
  - A letra da tecla fica pequena num canto.
  - **Recarga:** uma varredura circular escura (como um relógio), com os segundos no centro.
  - **Pronto:** um brilho rápido na cor do tema quando a recarga termina.
  - **Em uso:** a borda acende na cor principal.
  - **Sem recurso:** o ícone fica dessaturado e o custo aparece em vermelho.
  - **Trancado:** continua o cadeado (agora sobre o ícone escurecido).
- **Barra de recurso:** o ícone do recurso à esquerda, a barra com o degradê das duas cores do tema, e o número `80/100`. O nível e o XP da maestria ficam como estão.
- O aviso de "sem poderes" do Ichigo continua.

## Parte 2: a árvore
- A `FormaArvore` dos três personagens (galhos abaixo).
- **Layout:**
  - O emblema do personagem fica na raiz, embaixo.
  - Do tronco saem os galhos de antes do Despertar.
  - O Despertar fica no tronco.
  - Acima dele saem os galhos de depois.
  - Cada galho tem o ícone dele na ponta e os seus nós em fila.
- **Estado de cada nó:**
  - **Comprado:** o galho e o nó acesos na cor do tema, com os pontos de nível cheios.
  - **Pode comprar:** pisca em dourado.
  - **Trancado:** apagado.
  - **Depois do Despertar, sem despertar:** o galho escurecido, e o cadeado no Despertar.
- O fundo e a moldura do tema.
- O detalhe do nó (nome, efeito por nível, custo, o que falta) num painel ao lado, quando o mouse passa.
- **Clique:** pede a compra ao servidor, como hoje (`ComprarNo`).
- **Telas pequenas:** a tela rola. O painel de detalhe vai para baixo se não couber ao lado.

### Os galhos
| Personagem | Antes do Despertar | Depois do Despertar |
|---|---|---|
| Gojo | Azul (alcance, recarga) · Infinito (duração, recarga) · Energia (reserva, fluxo) | Vermelho (força, carga) · Roxo (força, recarga, recarga do Roxo máximo) · Domínio (técnica reversa, duração) |
| Ichigo | Getsuga (força, recarga) · Shunpo (recarga) · Reiatsu (reserva, fluxo, fôlego do Bankai) | Getsuga Negro (força, duração da lâmina) · Máscara (duração, recarga) · Mugetsu (recarga, poderes voltam) |
| Cid | Redemoinho (força, recarga) · Pós-imagem (recarga) · Prisão (duração) · Magia (reserva, fluxo) | Espada Atômica (força, recarga) · Overdrive (recuo) · Atomic (força) |

## Testes
- **JUnit (`core/`):**
  - todo nó da `Arvore` aparece em exatamente um galho, e um galho de antes só tem nós de antes (e o mesmo para depois);
  - todo galho aponta para nós que existem;
  - todo personagem jogável tem tema e forma;
  - todo `id` de golpe e de recurso tem o PNG em `textures/gui/icones/`;
  - no layout, nenhuma célula se sobrepõe a outra e todas cabem na largura;
  - `indiceEm` acha a célula certa com a tela rolada;
  - a varredura vai de vazia a cheia;
  - o brilho dura o tempo certo e some.
- **Build e GameTests** ao fim de cada parte (a parte 2 mexe no envio ao servidor só pelo clique, que já existe).
- **Prévia** pelo `runClientDemo`:
  - modo `hud`, novo: os três personagens com uma recarga correndo, o brilho e o cadeado;
  - modo `arvore`, que já existe: a árvore de cada personagem com nós em estados diferentes.
- **Em Pendências:** o autor vê no jogo, em várias escalas de GUI.

## Fora deste spec
- Pré-requisitos entre nós e regras novas de maestria.
- A HUD em outra posição.
- Um JSON editável por modpack.
- A tela de escolha de personagem, que fica como está (já tem os emblemas).
