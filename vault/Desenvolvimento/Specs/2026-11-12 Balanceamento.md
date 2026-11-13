---
tags: [desenvolvimento, spec]
estado: aguardando revisão
---
# Spec: balanceamento depois do primeiro teste no jogo

Voltar para [[Roadmap]]. O autor testou o [[Gojo]], o [[Ichigo]] e o [[Cid]] em 12/11. A queixa central: os personagens apanham demais de mobs do jogo ("estou tomando gap para um Warden"), e os golpes do Gojo quase não ferem o [[Toji]].

## O que o autor pediu (12/11)
- Todos os personagens precisam de mais dano ou mais resistência. **Régua: um personagem vence um Warden com folga.**
- Gojo:
  - as técnicas quase não ferem o Toji;
  - o clique (M1) não dá dano;
  - a arena de boss não pode ser destrutível;
  - o Vermelho empurra demais.
- Ichigo: balancear o boss (Zangetsu interior); as skills estão boas, mas gastam reiatsu demais.
- Cid: o dano está bom e o moveset também; balancear o boss (Ragnarok). O final da ultimate precisa de mais impacto.
- Mais detalhe nas outras arenas, como a da Escola Jujutsu.

## Divisão em três trabalhos
1. **Este spec:** o balanceamento.
2. **Depois:** o impacto do final do I Am Atomic (visual e som).
3. **Depois:** mais detalhe nas arenas, começando pela Escola Jujutsu.

O 2 e o 3 não dependem dos números e terão spec próprio.

## Abordagens consideradas
- **A. Uma camada de poder comum, com ajustes pontuais por cima.** Escolhida pelo autor em 12/11. Resolve o Warden de uma vez, vale automaticamente para os próximos personagens e é ajustável pela config.
- B. Refazer cada número de cada golpe. Controle fino, mas um ajuste geral depois vira dezenas de mudanças.
- C. Só multiplicar o dano dos golpes. Não resolve: o Warden continua matando em 1 ou 2 golpes.

## A camada de poder
Vale para quem tem um personagem escolhido. Os números ficam na **config do servidor** (`minerwk-server.toml`, novo) e podem ser mudados sem recompilar.

| Item | Padrão | O que faz |
| --- | --- | --- |
| `vidaExtra` | 20 | Soma à vida máxima: 20 vira 40 |
| `reducaoDeDano` | 0,5 | Corta o dano recebido de qualquer fonte, exceto `/kill` e o vazio. O golpe do Warden (30) vira 15 |
| `multiplicadorDosGolpes` | 3 | Multiplica o dano de todos os golpes do kit (não o do clique, que tem o dele) |
| `vidaDosBosses` | 2 | Multiplica a vida dos três bosses |
| `danoDosBosses` | 2 | Multiplica o dano dos três bosses. Com a redução de 50%, chega ao jogador o mesmo que hoje |

- **A vida extra** é um modificador de atributo: entra ao escolher o personagem e sai ao ficar sem personagem. Ao entrar, o jogador não ganha vida de graça; a barra só aumenta.
- **A redução** entra depois da armadura (`LivingDamageEvent.Pre`), para somar com ela e não ser contada duas vezes.
- **O multiplicador dos golpes** entra num ponto só: os métodos de dano do `ContextoServidor`. Nenhum golpe muda de número.
- **Os bosses** leem os dois multiplicadores ao nascer: a vida no atributo, e o dano no `ferir` de cada um.
- **Conta contra o Warden (Normal):**
  - com o Gojo, o Roxo (30 × 3 = 90) e o Vermelho cheio (14 × 3 = 42) matam em 7 a 8 golpes, uns 30 s;
  - o jogador aguenta 5 golpes do Warden, e o Infinito do Gojo ainda bloqueia o corpo a corpo.

## Ajustes pontuais
- **Clique do Gojo:** ganha arma própria, "punho", com 6 de dano e a velocidade do punho (4). Antes caía no soco da mão (1).
- **Vermelho:** o arremesso do alvo passa de `2,5 + 1,5 × força` para `1,2 + 0,8 × força`. O dano não muda.
- **Ichigo, reiatsu:**

  | O quê | Hoje | Novo |
  | --- | --- | --- |
  | Dreno do Bankai | 2/s | 1/s |
  | Regeneração | 1,5/s | 2,5/s |
  | Regeneração sem apanhar | 3/s | 5/s |
  | Getsuga Tenshō | 20 | 12 |
  | Shunpo | 15 | 10 |
  | Getsuga no chão | 30 | 20 |
  | Arremesso | 20 | 12 |
  | Bankai | 50 | 40 |
  | Getsuga negro | 15 | 10 |
  | Shunpo do Bankai | 10 | 6 |
  | Getsuga na lâmina | 30 | 20 |

  A máscara (40) e o Mugetsu (100, gasta tudo) não mudam: são os golpes caros de propósito.
- **Arena indestrutível:** na dimensão das arenas nenhum golpe apaga ou arranca blocos (`ArenasServidor.podeMexer` passa a negar tudo lá). Troca a regra do spec [[2026-11-10 Arena do Toji]], que deixava destruir dentro da caixa. Os visuais continuam iguais; só os blocos ficam. A regra da queda no vazio fica, para o caso de alguém chegar abaixo do fundo por outro caminho.

## Bosses
O pedido "balancear" o Zangetsu e o Ragnarok não diz se estão fáceis ou difíceis. Este spec só os ajusta à camada nova (vida ×2 e dano ×2), para continuarem na mesma proporção em relação ao jogador. O ajuste fino de cada boss sai do próximo teste do autor, mudando os dois multiplicadores na config ou os números de cada golpe.

## Testes
- **JUnit:**
  - a conta da redução e da vida extra;
  - os custos novos do Ichigo e o dreno de 1/s;
  - o arremesso menor do Vermelho;
  - a arena negando blocos.
- **GameTests:**
  - um zumbi perde exatamente 3 vezes o dano do golpe;
  - um Warden invocado leva 15 de um golpe com o jogador sem armadura;
  - os bosses nascem com o dobro da vida.
- Os GameTests antigos que conferem um dano exato passam a ler o multiplicador da config.

## Fora deste spec
- O impacto do final do I Am Atomic e as arenas, como dito acima.
- Dificuldade do jogo: a camada não muda conforme Fácil, Normal ou Difícil. O Warden no Difícil tira 45, que vira 22,5.
