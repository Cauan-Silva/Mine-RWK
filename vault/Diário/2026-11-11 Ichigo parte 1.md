---
tags: [diario]
---
# 11/11/2026: Ichigo, parte 1 (formas no núcleo e o Reiatsu)

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]] (aprovado em 11/11). Branch `ichigo-kit`. Vem depois de [[2026-11-11 Ichigo pesquisa]].

## O que foi feito
O [[Ichigo]] é o primeiro personagem de transformação, e o núcleo só sabia de um conjunto de golpes por personagem. Esta parte prepara o núcleo; o Ichigo ainda não aparece no jogo.

- **Formas** (`core/kit/Forma`): uma transformação é um id e outro conjunto de golpes nas mesmas teclas. A forma base continua sendo as habilidades do próprio `Personagem` (id `base`); `Personagem` ganhou a lista `transformacoes`. Quem não tem transformação (o Gojo) não mudou.
- **O kit do jogador** (`KitDoJogador`) guarda a forma atual e as teclas valem para os golpes dela.
  - `mudarForma` troca a forma. Um golpe ainda em carga é interrompido sem recarga de punição; um que já disparou segue até o fim (é o caso do próprio golpe que faz a troca).
  - Morrer ou sair volta à forma base. A forma não é salva.
- **Recargas** já eram por id de golpe: o Z de uma forma não trava o Z da outra, e sair e voltar não zera nada. Agora há teste disso.
- **Trancas por golpe** (`Trancas`): o que exige o despertar passa a ser uma lista de ids de golpes, e não de teclas. Assim o V do Shikai fica livre e o V do Bankai trancado. As do Gojo foram convertidas (`Trancas.dasTeclas`) e são as mesmas: X, V, G, R e a passiva.
- **Golpe que transforma:** `Contexto.mudarForma(id)` e `Contexto.forma()`, implementados no servidor.
- **Recurso:** `Recurso.forma(id)` avisa o recurso da troca (o Reiatsu muda de regra e de cor).
- **Sincronização:** o pacote do kit leva a forma e só os golpes dela, e é reenviado quando a forma muda. O HUD mostra os golpes da forma sem mudar nada nele.
- **Tela de escolha:** abaixo dos golpes da forma base aparece o nome de cada transformação (tradução `forma.minerwk.<id>`) e os golpes dela.
- **Reiatsu** (`core/personagens/ichigo/Reiatsu`): máximo 200. No Shikai volta 1,5 por segundo, e 3 por segundo depois de 4 s sem apanhar. No Bankai não volta e drena 2 por segundo (200 dão 100 s). `esgotado()` diz quando o Bankai tem de acabar; quem o encerra é o kit, na parte 2. Barra azul-clara no Shikai e vermelho-escura no Bankai.
- **Personagem de teste:** ganhou o G, que entra e sai de uma "forma de teste" em que o Z é outro golpe. Serve para ver as formas no jogo antes do Ichigo existir.

## Ficou para depois
- **Quem vê o jogador saber a forma dele** (para a arma e o traje trocarem): entra na parte 3, que é onde isso é usado.
- O Ichigo registrado como personagem e os golpes: parte 2.

## Testes
- JUnit: `FormasTest` (13 casos: começa na base; trocar muda os golpes e avisa o recurso; forma desconhecida ou igual não muda; recarga por golpe; tranca por golpe; conversão das teclas em ids; troca interrompe a carga sem punir; golpe disparado não é interrompido; soltar a tecla depois da troca; mudar de dimensão mantém a forma; morrer volta à base; formas no personagem e ids repetidos; personagem sem transformação) e `ReiatsuTest` (9 casos).
- GameTest novo em `kit/TestesKit`: o G transforma pelo caminho do servidor, o Z vira o da forma, tecla sem golpe na forma não faz nada, e morrer volta à base. Os 63 GameTests passam.
- `./gradlew build` passa, com os testes do Gojo inalterados.

## Revisão (subagente)
Nenhum achado crítico. Conferido sem problema: as trancas do Gojo (as mesmas de antes), a ordem dos campos dos pacotes, o reenvio no mesmo tick da troca, recargas e salvamento, e as contas do Reiatsu.

Corrigido:
- **Mudar de dimensão tirava a transformação.** Entrar ou sair da arena, um portal ou o End passavam pelo mesmo caminho da morte, que volta à forma base. Agora a troca de dimensão só acaba com o golpe e os efeitos (`descartarMantendoAForma`); a forma fica. Com teste.
- Um teste dizia conferir "soltar a tecla depois da troca" e não fazia troca nenhuma. Foi refeito: a troca chega com a tecla segurada, a carga acaba e nada fica preso.

Anotado para a parte 2:
- Quando o Bankai acabar por falta de reiatsu, a troca vem de fora do golpe: conferir a animação do golpe que estiver em carga nessa hora.
- Quem morre no Bankai renasce no Shikai com o reiatsu que tinha (pode ser quase zero). Decidir se o renascimento enche a barra.

## Falta
O teste do autor no jogo: ver [[Pendências]].
