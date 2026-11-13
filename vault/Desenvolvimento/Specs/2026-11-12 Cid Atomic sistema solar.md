---
tags: [spec, cid]
estado: aprovado
---
# I Am Atomic: o recuo revela o sistema solar

Voltar para [[Cid]]. Vem do teste do autor em 12/11, depois do [[2026-11-12 Balanceamento]].

## Por quê
O autor achou que o final do I Am Atomic ainda tinha pouco impacto. O que faltava, nas palavras dele: "mostrar mais o sistema solar", no recuo da câmera do estouro.

Hoje o recuo não revela nada. O céu do espaço (`EspacoCliente.Ceu`) é desenhado numa casca a 100 blocos que anda com a câmera. A câmera se afasta 105 blocos, mas o planeta continua tomando a metade de baixo da tela, e a lua e os dois planetas continuam pequenos no mesmo lugar.

## Objetivo
No recuo do estouro, a câmera parece sair de verdade. A Terra embaixo vira uma bola inteira com o ponto roxo nela, e então aparece o sistema solar inteiro: o sol, os planetas e as órbitas, com o clarão roxo visível até de lá.

**Fora do escopo:** o espaço antes do "atomic" (a chegada, os socos, as palavras) fica como está. A subida, a volta, a onda de choque, o dano e as regras do golpe não mudam.

**O que faria falhar:** um corte seco para uma imagem 2D (parece slide); o sistema solar aparecer antes do estouro; a tela tremer ou piscar por causa de câmera longe demais.

## Abordagens
- **A, escolhida pelo autor: o céu encolhe durante o recuo.** O desenho do céu ganha uma escala que anda com o tempo depois do "atomic". Os astros viram objetos com posição própria, desenhados a partir de uma câmera virtual que se afasta, enquanto a câmera do jogo só recua um pouco. Tudo fica no cliente, sem limite do motor.
- **B, planetas de verdade no mundo:** esferas enormes na dimensão e a câmera voando milhares de blocos. Esbarra na distância de renderização e na precisão da câmera.
- **C, um corte para uma cena 2D:** é a mais simples, mas perde o recuo contínuo.

## O recuo, quadro a quadro
Em ticks desde o "atomic" (`t`), com o clarão passando de 70 para **130 ticks** (6,5 s; o golpe todo vai de 17 para 20 s):

| t | O que se vê |
| --- | --- |
| 0–10 | Como hoje: o quadro branco do estouro, a esfera roxa nascendo em volta dos dois, a câmera começando a se afastar. |
| 10–45 | A Terra sai de baixo da tela e vira uma bola inteira, com a borda azul e o lado da noite. A esfera roxa do mundo encolhe e passa a ser um ponto roxo brilhante na Terra, que acende a face dela de roxo. |
| 45–95 | A Terra encolhe até ficar do tamanho de um planeta entre os outros. Entram o sol, grande e amarelado, e os outros planetas, cada um na sua órbita (linhas finas e apagadas), girando devagar. O ponto roxo pulsa como uma estrela ao lado da Terra, com raios em cruz. |
| 95–120 | O plano para no sistema inteiro. O ponto roxo dá o último pulso e se abre até tomar a tela. |
| 120–150 | A tela vai ao branco (como hoje, só mais tarde), e os dois voltam por baixo dele. |

## As decisões que eu tomei (o autor confere)
- **O sistema solar parece o nosso, sem ser em escala:** o sol, quatro planetas pequenos de rocha (um deles a Terra), dois gigantes, um com anel, e dois azuis mais longe, todos com as distâncias apertadas para caberem na tela. Ele é visto de cima e de lado, com as órbitas em elipse.
- **Só quem está vendo a cutscene vê o recuo.** Com `cutscenes = false` (ou para quem está no espaço sem ver a cena), o céu fica como hoje: em primeira pessoa, o mundo encolhendo à sua volta ficaria estranho.
- **A esfera roxa do mundo (`RenderVisuaisCid`) se apaga** quando o ponto roxo do céu assume, para os dois não aparecerem juntos com tamanhos que não batem.
- **O som do Atomic acompanha os 130 ticks:** o gerador (`ferramentas/gerar_sfx_cid.py`) estica o ronco e põe um último estouro no pulso final.
- **A luz no céu vista do chão** e a onda de choque não mudam; só começam 60 ticks depois, junto com a volta.

## O que fica para quem constrói
- A conta da escala, das órbitas e do ponto roxo fica em `core/` (sem Minecraft, com JUnit), como as outras curvas de `AtomicFinal`. O desenho fica em `client/`.
- A forma de cada astro (discos com sombra, como os de hoje, ou esferas com gomos) e as cores.
- O ajuste fino dos tempos, conferido na prévia (`runClientDemo`).
