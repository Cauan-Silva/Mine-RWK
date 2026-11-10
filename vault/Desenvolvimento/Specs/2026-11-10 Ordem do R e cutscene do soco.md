---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec e plano: nova ordem do R, cutscene da corrida e do uppercut, impact frame (Fase 2, parte 8b)

Voltar para [[Roadmap]]. Vem do teste de 10/11/2026 de [[2026-11-10 Preparação e VFX pesado do Vazio Roxo máximo]]. O autor achou que está "quase lá", mas:
- "o rosto do npc acabou bugando, e não deu para ver direito": a câmera entrou na cabeça do alvo;
- "o soco deveria ter uma cutscene também, tipo correndo e dando um uppercut";
- "o vermelho e o azul só devem aparecer quando ele estiver no céu, e lá ele deve conjurar os dois e realizar a fusão explodindo tudo".

Escolhas do autor (10/11):
- câmera baixa atrás do Gojo na corrida e, no soco, um corte para um plano de lado e de baixo, vendo o alvo ser lançado;
- o uppercut tem **impact frame**;
- no céu, o Azul nasce na mão esquerda e o Vermelho na direita, e os dois voam para trás dele para a dança e a fusão.

## Causa do rosto bugado
A base da cutscene são os olhos do Gojo. O alvo fica suspenso a 1,5 bloco na frente dele, na mesma altura. O close da fusão põe a câmera a 1,6 bloco na frente do rosto do Gojo, ou seja, dentro da cabeça do alvo. O plano das esferas, a 4 blocos na frente e 1 acima, também atravessa o alvo.

## Nova ordem
| Fase | O que acontece | Ticks |
| --- | --- | --- |
| Encantamento (parte 1) | os quatro gestos, com **aura** de fitas azuis e vermelhas subindo em volta do Gojo e os olhos brilhando, **sem as esferas nas mãos** | 80 + 20 |
| Corrida | o Gojo corre até o alvo; câmera baixa atrás dele | até 20 |
| Uppercut | corte seco para um plano de lado e de baixo, **impact frame** e o alvo lançado | — |
| Subida | os dois sobem juntos | 30 |
| **Conjurar** | lá em cima ele abre os braços; o Azul nasce na mão esquerda e o Vermelho na direita, crescendo | 8 |
| Viagem | as duas voam das mãos para trás dele | 10 |
| Dança e toque | como no spec anterior | 12 |
| Fusão, silêncio, explosão | como no spec anterior | 40 + 30 |

O tempo das esferas continua 30: 8 para conjurar, 10 de viagem e 12 de dança. O núcleo e a explosão não mudam.

## Cutscene da corrida e do uppercut
- **Corrida:** câmera baixa, a uns 0,5 bloco do chão, atrás e um pouco ao lado do Gojo, com o FOV abrindo com a velocidade.
- **Uppercut:** corte seco no instante do soco para um plano de lado e de baixo, a uns 4 blocos, mostrando o Gojo e o alvo inteiros. A câmera fica parada enquanto o alvo sobe, e depois sobe junto.
- **Impact frame no soco:** 3 ticks em preto e branco de alto contraste, com as silhuetas em branco sobre o fundo escuro e um toque de roxo, como nos animes. Junta com o hit-stop que já existe. É um pós-processamento de tela só no cliente, para quem está na cutscene ou perto.

## Câmera longe do alvo
- Todos os planos depois do uppercut ficam a pelo menos 2 blocos do alvo.
- O close da fusão passa a ser de três quartos, do lado esquerdo e um pouco acima, vendo o rosto do Gojo e o núcleo atrás.
- O plano das esferas passa para o lado, para ver o Gojo abrindo os braços sem o alvo na frente.
- Teste em JUnit: em toda a coreografia com alvo, a câmera nunca chega a menos de 2 blocos da posição do alvo em relação ao Gojo.

## Teste
- **JUnit:**
  - `RoxoMaximo`: as fases novas (conjurar 8, viagem 10, dança 12);
  - `CutsceneRoxoMaximo`: corte seco no uppercut, câmera baixa na corrida e distância mínima do alvo;
  - `FusaoRoxoGojo`: o tamanho das esferas nascendo nas mãos.
- **Animação:** braços abrindo para conjurar, com o tempo vindo das constantes.
- **No jogo:** o encantamento sem esferas, a corrida, o uppercut com impact frame, a conjuração lá em cima e o rosto do alvo sem bug.

## Fora deste spec
Céu escurecendo e chão rachando no encantamento (continuam do spec anterior); som.
