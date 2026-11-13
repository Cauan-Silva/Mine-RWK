---
tags: [diario]
---
# 12/11/2026: balanceamento depois do primeiro teste

Voltar para [[Diário]]. Spec e plano: [[2026-11-12 Balanceamento]]. Branch `balanceamento`.

## De onde veio
O autor testou o [[Gojo]], o [[Ichigo]] e o [[Cid]] e apanhou demais de um Warden. Pediu mais dano ou mais resistência para todos, com a régua "um personagem vence um Warden com folga", e ajustes no Gojo, no Ichigo e nos bosses. Escolheu a abordagem A: uma camada de poder comum, com ajustes pontuais por cima.

## O que foi feito
- **Config do servidor** (`saves/<mundo>/serverconfig/minerwk-server.toml`, seção `poder`):

  | Valor | Padrão |
  | --- | --- |
  | `vidaExtra` | 20 (a vida vai a 40) |
  | `reducaoDeDano` | 0,5 |
  | `multiplicadorDosGolpes` | 3 |
  | `vidaDosBosses` | 2 |
  | `danoDosBosses` | 2 |

- **Camada no jogador (`kit/PoderServidor`):** quem tem personagem ganha a vida extra (modificador de atributo, conferido a cada segundo, ao entrar e ao renascer) e recebe o dano reduzido depois da armadura. `/kill` e o vazio passam inteiros.
- **Golpes:** todo método de dano do `ContextoServidor` passa pelo multiplicador. O clique não.
- **Bosses:** o Toji, o Zangetsu interior e o Ragnarok leem os multiplicadores ao começar a luta (vida a partir da constante, então começar de novo não multiplica duas vezes), e todo dano deles passa pelo multiplicador, inclusive o calor do Ragnarok.
- **Gojo:** o clique é o `PUNHO` (6 de dano), posto por uma passiva que não é trancada. Antes caía no soco da mão (1). O Vermelho arremessa `1,2 + 0,8 × força` (era `2,5 + 1,5 × força`).
- **Ichigo:** golpes um terço mais baratos (ver a tabela do spec), o Bankai drena 1/s (eram 2), o reiatsu volta a 2,5/s, ou 5/s sem apanhar. Um Bankai cheio dura 200 s sem golpes. A máscara e o Mugetsu não mudam.
- **Arenas indestrutíveis:** na dimensão das arenas nenhum golpe apaga nem arranca blocos.

## Decisões
- **Os GameTests de mecânica rodam sem a camada.** Eles conferem o número de cada golpe (o Getsuga tira 12, o soco do Toji tira 6). Em vez de reescrever 29 deles, o jogador de teste é marcado "sem camada" (`PoderServidor.semCamada`), e os bosses invocados por ele ficam neutros também. A camada é testada só em `TestesPoder`. O plano dizia para os testes antigos lerem o multiplicador.
- **`Passiva.trancavel()`:** as passivas do Gojo só rodam depois do despertar; o punho precisa valer antes, então uma passiva pode dizer que não obedece às trancas.
- Dois GameTests do Ichigo diziam que "o Gojo não tem arma do kit"; agora ele tem o punho, e os dois foram atualizados.

## Testes
- JUnit: `PoderTest` (6), dois novos em `GojoTest`, um em `IchigoTest`; os testes de reiatsu passaram aos ritmos novos.
- GameTest: `TestesPoder` (8): golpe do kit ×3, vida extra e a volta sem personagem, renascido com a vida extra, golpe do Warden pela metade, sem personagem apanha inteiro, `/kill`, bosses com o dobro da vida (uma vez só), golpe do boss multiplicado e reduzido. As arenas: `apagarNaArenaNaoMexe` e `arrancarNaArenaNaoMexe`. Os 124 passam.

## Revisão final (revisor separado)
Nenhum crítico, dois importantes e sete menores. Corrigidos com um teste que falhou antes e passa agora (os GameTests foram a 128):
- **Importante: sair e entrar no mundo cortava a vida em 20.** O modificador de vida não era salvo; agora é (`addPermanentModifier`). Teste: `sairEEntrarMantemAVida`.
- **Importante: o despertar das lutas do Toji e do Zangetsu disparava cedo.** A queda decide antes da redução e via o golpe dobrado inteiro: um soco que deixaria 2 de vida derrubava. Agora ela mede o dano que de fato chega (`PoderServidor.recebidoPor`). Teste: `socoDoTojiNaoDerrubaQuemSobrevive`.
- **O traje de slime do Cid passou a disparar só com golpes de 16.** Ele media o golpe já reduzido. Agora a ordem é fixa: o traje (prioridade mais alta) mede antes da camada, a camada reduz, e a forma Hollow decide o golpe letal já com a redução. Teste: `trajeDoCidContinuaComOGolpeForteDeAntes`.
- **Explosões do jogo (creeper, TNT) ainda quebravam a arena.** Agora não. Teste: `explosaoNaArenaNaoQuebraBlocos`.
- **A queda no vazio da arena** passou a usar o dano do vazio, que a camada não corta (continua 4, como no spec da arena).
- **A vida dos bosses vai até ×3 na config:** o jogo limita a vida de qualquer criatura a 1024.

Registrado sem mudança:
- Na forma Hollow as duas reduções se multiplicam: o dano chega a 25%.
- O traje do Cid também: quando ele dispara, o golpe chega a 25%.
- O punho do Gojo (6, velocidade 4) dá uns 24 de dano por segundo com o clique: dá para matar um Warden só no soco em uns 20 s. Está dentro do spec; o autor vê no jogo.

## Falta
O teste do autor no jogo (roteiro em [[Pendências]]). Depois, os dois trabalhos seguintes: o final do I Am Atomic do Cid e o detalhe nas arenas.
