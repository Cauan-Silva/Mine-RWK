---
tags: [diario]
---
# 12/11/2026: Mugetsu fino e longo

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]]. Branch `ichigo-kit`. Vem depois de [[2026-11-11 Ichigo parte 6 apresentação]].

## O que foi feito
O autor viu a prévia do Mugetsu e pediu o véu "fininho, mas muito longo e comprido: uns 40 blocos de altura e uns 150 de distância".

- **Medidas** (`core/vfx/MugetsuIchigo`): 3 blocos de largura (eram 9), 40 de altura (eram 13) e 150 de alcance (eram 40). A largura de 3 foi escolha minha para "fininho"; é um número só, fácil de mudar.
- **Tempo:** o véu continua levando 1 s (20 ticks) para chegar ao fim, agora a 7,5 blocos por tick. Assim o som do Mugetsu, a forma final e a cutscene continuam encaixados sem regravar nada.
- **Dano** (`Ichigo.Veu`): acompanha o desenho. Cada tick cobre o trecho andado em 5 fatias, dos pés aos 40 de altura, então pega quem está no alto e não deixa vão com o passo maior. Continua 40 de dano, uma vez, ignorando armadura e atravessando paredes.
- **Desenho:** as mesmas cortinas de escuridão, agora uma a cada 3 blocos e com 3 colunas; a frente que avança ficou mais alta.
- **Cutscene:** o plano aberto do corte ficou mais longe e mais alto para o véu caber.

## Testes
- JUnit: os casos do véu foram refeitos para as medidas novas, e entraram dois (pega quem está a 31 de altura; não pega a 44). `./gradlew build` passa.
- Os 73 GameTests passam.
- Prévia gravada (`./gradlew runClientDemo -Pdemo=ichigo_mugetsu`) e conferida por quadros: uma parede preta fina, alta e comprida, com o céu escuro. Vídeo em `previas/ichigo_mugetsu.mp4`.

## Falta
O autor ver no jogo: ver [[Pendências]]. Próximo no Ichigo: a parte 7, o boss Zangetsu interior.
