---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec e plano: encantamento e cutscene do Roxo 200% (Fase 2, parte 3)

Voltar para [[Roadmap]]. Pedido do autor em 09/11/2026: o Roxo 200% ainda não está como ele imaginou. Escolheu **encantamento visível** e **cutscene já agora**. A pesquisa da cena (cap. 223) está em [[Gojo]]: as quatro palavras do encantamento, o gesto e o ataque surpresa de Shibuya a Shinjuku.

Esta parte também cria o **sistema de câmera reutilizável** decidido para as ultimates de todos os personagens.

## Objetivo
Pronto quando:
- os JUnit da coreografia passam, e o build e os GameTests passam;
- no jogo, quem usa o R vê uma cutscene:
  - durante os 4 s de carga, a câmera sai do jogador, dá a volta no Gojo e mostra as mãos;
  - as quatro palavras aparecem uma a uma, e o céu escurece;
  - no disparo, a câmera acompanha a massa por um instante e volta ao jogador;
- quem está perto vê as palavras flutuando em volta do Gojo e o céu escurecer, sem cutscene;
- um golpe forte durante a carga corta a cutscene na hora e devolve a câmera;
- dá para desligar as cutscenes na config do cliente.

## Desenho

### 1. Sistema de câmera (reutilizável)
- **Núcleo, `core/camera/Coreografia`:** uma lista de quadros-chave. Cada quadro tem:
  - o tick;
  - a posição da câmera na base de quem usa o golpe (lado, cima, frente);
  - o ponto para onde a câmera olha, na mesma base;
  - o FOV e a inclinação.
- **Interpolação:** suave entre quadros. A função `quadro(t)` devolve posição, alvo, FOV e inclinação, e tem JUnit. Uma coreografia nova para outro personagem é só uma lista nova de quadros.
- **Cliente, `client/camera/Cutscene`:** uma entidade-câmera só no cliente.
  - O jogo passa a renderizar por ela (`setCameraEntity`), e ela segue a coreografia a cada quadro.
  - Barras de cinema em cima e embaixo, e a HUD escondida.
  - Os comandos de andar e pular ficam ignorados enquanto dura.
  - No fim, ou se for cortada, a câmera volta ao jogador.
- **Rede:** o pacote `Cutscene(id, dono, origem, direcao, inicio)` vai só para quem usou o golpe. O fim antecipado reaproveita o `EncerrarVisual` da carga: quando a carga é interrompida, a cutscene acaba.
- **Config do cliente:** `cutscenes = true`. Desligada, o jogador vê só o encantamento na tela e o efeito normal.

### 2. Coreografia do Roxo 200% (80 ticks de carga + 30 depois do disparo)

| Ticks | Câmera | Na tela |
| --- | --- | --- |
| 0–20 | gira devagar em volta do Gojo, meio corpo, de lado para a frente | barras entram; «九綱» **Nove Cordas** |
| 20–40 | close nas mãos (o gesto) | «偏光» **Luz Polarizada** |
| 40–60 | de baixo para cima, as esferas azul e vermelha girando; o céu escurece | «烏と声明» **Corvo e Declaração** |
| 60–80 | de frente para o rosto, aproximando, a tela escurece nas bordas | «表裏の間» **Entre a Frente e o Verso** |
| 80–95 | atrás do ombro, seguindo a massa que sai | «虚式・茈» **Técnica Vazia: Roxo**, grande, com clarão |
| 95–110 | volta suave para a visão do jogador | barras saem |

- **As palavras:** o japonês aparece grande, e a tradução em português, menor, embaixo. Entram com um esmaecer curto e somem antes da próxima. As traduções vão nos arquivos de idioma, então o inglês também funciona.

### 3. Para quem está perto
- **Palavras flutuando:** quem está a até 48 blocos vê cada palavra no mundo, girando devagar em volta do Gojo (texto voltado para a câmera, no ritmo da tabela). Usa o `MostrarVisual` que já existe (`gojo_roxo_200_carga`); o cliente sabe o tempo de cada palavra pela idade do visual.
- **Céu escurecendo:** do tick 40 até o disparo, a cor da neblina e do céu escurece num raio de 32 em volta do Gojo, como no domínio, e volta depois do disparo.

### O que não muda
Os números, o dano e a mecânica do Roxo 200% continuam como estão. O jogador continua vulnerável durante a carga: a cutscene não dá imunidade, e um golpe forte a corta.

## Testes
- **JUnit (`CoreografiaTest`):**
  - em cima de um quadro, `quadro(t)` devolve exatamente aquele quadro;
  - entre dois quadros, fica entre eles e a curva é suave;
  - antes do primeiro quadro vale o primeiro, e depois do último vale o último;
  - a coreografia do Roxo 200% tem 110 ticks e termina na posição dos olhos (para a volta ao jogador não dar salto).
- **JUnit (`GojoTest`):** o R manda a cutscene ao começar a carga, e uma carga interrompida a encerra.
- **GameTest:** só o dono recebe o pacote da cutscene (contexto falso registrando os envios).

## Plano
1. `Coreografia` com testes; a coreografia do Roxo 200% como dado.
2. Primitiva `cutscene(id)` no `Contexto`, o pacote `Cutscene` e o envio no `Roxo200`, com testes.
3. Cliente: entidade-câmera, barras, HUD escondida, entrada bloqueada e o corte pela interrupção.
4. Encantamento na tela (cutscene) e no mundo (palavras flutuando), mais o céu escurecendo.
5. Config `cutscenes`, build, revisão com sonnet, vault e teste do autor.
