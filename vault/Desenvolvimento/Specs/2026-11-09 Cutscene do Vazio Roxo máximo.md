---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec e plano: cutscene do Vazio Roxo máximo e mais animações do R (Fase 2, parte 7)

Voltar para [[Roadmap]]. Vem do teste de 09/11/2026 (depois de [[2026-11-09 Vazio Roxo máximo e VFX do Azul e do Infinito]]). O autor achou que "melhorou bastante", mas:
- o R não tem uma cutscene correta: falta a câmera de longe mostrando a explosão;
- ainda faltam animações.

Escolhas do autor (09/11):
- explosão em **plano aberto de longe**;
- a cutscene **cobre a sequência inteira**, em planos;
- animações novas do **Gojo no ar** e do **encantamento**;
- a cutscene é vista pelo **Gojo e pelo alvo**, quando o alvo é um jogador.

## Objetivo
Pronto quando:
- o R tem uma cutscene do começo ao fim: o encantamento, o avanço, o uppercut, as esferas, a fusão, a explosão vista de longe e a volta ao jogador;
- o encantamento e a parte no ar têm mais animação;
- os testes passam e o autor aprova no jogo.

## 1. Cutscene em duas partes
O sistema de câmera continua o mesmo (`Coreografia`, quadros-chave na base do Gojo). Muda assim:

- **Parte 1, encantamento (0 a 80 ticks):** os planos e as palavras de hoje. O plano do disparo atrás do ombro (82 a 110) sai, e a parte 1 termina no disparo, sem voltar para os olhos.
- **Parte 2, sequência:** começa no disparo e emenda na parte 1 sem corte.
  - O servidor manda a parte 2 no disparo, já sabendo o avanço A.
  - A coreografia é montada pelo A: `CutsceneRoxoMaximo.coreografia(avanco)` e `semAlvo()`, em `core/`, com JUnit.

**Planos da parte 2, com alvo (ticks a partir do disparo):**

| Plano | Ticks | Câmera |
| --- | --- | --- |
| Avanço | 0 até A | Atrás do ombro, baixa, seguindo o Gojo; FOV abre (sensação de velocidade) |
| Uppercut | A até A+6 | De baixo e de lado, perto do soco; tranco de FOV e leve inclinação |
| Subida | A+6 até A+15 | Sobe junto, de lado, mostrando o Gojo e o alvo subindo |
| Esferas | A+15 até A+25 | De frente para o Gojo, um pouco acima, com as mãos abrindo e as esferas indo para trás dele |
| Fusão | A+25 até A+35 | Close no rosto com o núcleo roxo crescendo atrás, desfocado pelo brilho |
| Explosão | A+35 até A+75 | **Plano aberto:** corta para ~70 blocos de distância, de lado e um pouco acima, parado, olhando para o centro da explosão. A esfera nasce atrás do Gojo e engole tudo; segura até a esfera sumir |
| Volta | A+75 até A+90 | Volta suave para os olhos do jogador |

**Sem alvo:**
- atrás do ombro, seguindo as esferas até a mira (20 ticks);
- a fusão vista do Gojo (10 ticks);
- o mesmo plano aberto da explosão, agora centrado no ponto da mira;
- a volta.

**O alvo também vê, se for jogador:**
- Ele recebe a parte 2 a partir do uppercut, com a câmera na base do Gojo (o mesmo enquadramento que o Gojo vê).
- Enquanto isso, ele não controla a câmera nem o movimento: já está preso no ar.
- A cutscene dele acaba junto com a do Gojo. A opção `cutscenes = false` também vale para ele.

**Corte:**
- Um golpe forte na carga corta a parte 1, como hoje.
- Se o Gojo morrer ou trocar de personagem no meio da parte 2, a cutscene acaba na hora, para os dois.

## 2. Animações
**Encantamento (0 a 4 s), mais gestos.** Cada palavra ganha um gesto próprio, com transições e a mão segurando a força:
1. **Nove Cordas:** as duas mãos abrem devagar para os lados, palmas para cima, o Azul na esquerda e o Vermelho na direita.
2. **Luz Polarizada:** os braços cruzam na frente do peito, e o tronco gira de um lado para o outro.
3. **Corvo e Declaração:** cabeça para trás, braços abertos para cima, o corpo se arqueando.
4. **Entre a Frente e o Verso:** as mãos voltam para a frente, quase se tocando, tremendo, e o olhar vai para o alvo.

Entre os gestos, o corpo respira e balança, sem parar duro.

**Gojo no ar:**
- **Flutuando:** entre o uppercut e a explosão, uma perna dobrada, a outra solta e o corpo balançando de leve. Hoje ele fica parado.
- **Reação à explosão:** quando a esfera passa por ele, o tronco vai um pouco para a frente, a cabeça baixa e os braços abrem, como quem segura o vento. Depois volta ao punho estendido.
- **Descida:**
  - Pose própria em loop: braços meio abertos, pernas soltas e cabeça olhando para baixo.
  - Ela para quando o Gojo pousa. O servidor vê o jogador no chão, para a animação e encerra o voo na hora.
  - Isso substitui os 30 s fixos de voo, mas o teto de 30 s continua.

## 3. Teste
- **JUnit:**
  - os planos da parte 2 nos ticks certos para A = 0, 10 e 20, e para o caso sem alvo;
  - o plano aberto a pelo menos 60 blocos do centro da explosão e olhando para ele;
  - a parte 1 termina no tick 80;
  - as durações das animações novas;
  - os ossos das animações do ar sem `body`.
- **`GojoTest`:**
  - a parte 2 é enviada no disparo, para o Gojo e para o alvo;
  - a animação de descida começa no fim da sequência.
- **GameTest:** o voo do Gojo acaba quando ele pousa.

## Fora deste spec
Continuam na fila: VFX pesado dos outros golpes e som.
