---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec: o mundo interior do mangá

Voltar para [[Roadmap]]. É a terceira e última arena do pedido "mais detalhe nas arenas" do teste de 12/11. Antes vieram a Escola ([[2026-11-13 Detalhe da Escola Jujutsu]]) e Oriana ([[2026-11-13 Oriana sob ataque]]). A arena de hoje está em [[2026-11-12 Ichigo parte 7]] e no código `core/arena/PlantaMundoInterior`.

## Por quê
O mundo interior é a fachada de um arranha-céu deitado com três prédios em volta, sob o pôr do sol de todas as arenas, e acaba no vazio. No mangá, ele é um céu azul limpo e prédios deitados até onde a vista alcança. Quando o Ichigo perde a vontade de lutar, chove lá dentro.

## Escolhas do autor (13/11)
1. **O mundo calmo do mangá:** céu azul de dia, nuvens passando, os prédios deitados até o horizonte e mais detalhe nas fachadas.
2. **Na fase 2,** quando a máscara aparece, o céu escurece e começa a chover.
3. **A abordagem A:** o mesmo caminho de Oriana, mais a troca de fase pelo cenário.

## Regras que continuam valendo
- **A fachada da luta fica livre:** de dx −22 a +22 e por todo o comprimento, há chão em `dy` −1 e nada em cima. Nenhum detalhe novo vai em cima dela, como o `PlantaMundoInteriorTest` exige.
- **As entradas** do jogador e do Zangetsu não mudam.
- **A queda no prédio baixo** continua possível, e a regra da queda da luta não muda.
- **A arena é indestrutível.**
- **A caixa:** 101 × 101. Nenhum bloco sai dela.
- **Teto:** 60 mil blocos.
- **O clima de verdade não muda:** a chuva é só do cliente, de quem está nessa luta. As outras arenas e o mundo normal não molham.

## Parte 1: a planta (`core/arena/PlantaMundoInterior`)
Os materiais novos entram no enum `Material` e ganham o seu bloco no servidor:

| Material | Bloco | Uso |
| --- | --- | --- |
| `JANELA_ACESA` | luz-de-sapo ocre | a luz por trás de algumas janelas |
| `VIDRO_CLARO` | vidro cinza-claro | janelas variadas |
| `GRADE_FERRO` | barras de ferro | antenas e grades |
| `PARA_RAIOS` | para-raios | ponta das antenas |
| `LETREIRO_VERMELHO`, `LETREIRO_AMARELO` | concreto vermelho e amarelo | letreiros |

O `ASSOALHO` (tábuas de abeto) já existe e é reaproveitado.

- **As janelas:**
  - em todas as fachadas, uma parte das janelas fica acesa (luz por baixo do vidro, no lugar do fundo escuro);
  - outra parte ganha vidro claro;
  - na da luta, a mesma regra, e o chão continua inteiro.
- **No prédio alto (leste) e no baixo (oeste),** espalhados pela fachada deles:
  - duas ou três antenas de grade com para-raios na ponta;
  - caixas d'água de tábuas sobre pés de grade;
  - um letreiro grande em cada um, como uma placa de loja.
  - Tudo deitado como os prédios: o que no mundo normal ficaria no telhado, aqui sai de lado.
- **As quinas:** uma marquise de concreto na beirada das fachadas que dão para a luta, como o friso de um prédio.
- **O prédio atravessado:** a face que se vê da luta ganha as janelas acesas e um letreiro.

## Parte 2: o céu, o horizonte e a chuva (cliente)
- **O cenário na fase 2:** quando o Zangetsu muda para a fase 2, o servidor manda `CenarioArena("interior_chuva")` a quem está na luta, com uma constante nova `CenarioArena.INTERIOR_CHUVA`. `ArenasServidor` ganha uma forma pública de trocar o cenário de quem está numa arena, e a vaga guarda o cenário novo: quem sair e entrar de volta no jogo continua na chuva.
- **O céu (`CeuInterior`):** com o cenário `interior` ou `interior_chuva`, uma cúpula por cima do pôr do sol, como a noite de Oriana.
  - **O azul:** um degradê do zênite `#3A7BD5` até o horizonte `#CFE8FF`.
  - **As nuvens:** umas 30, brancas e achatadas, que andam devagar de leste para oeste.
  - **Na chuva:** em uns 3 s, o céu passa a cinza (`#4E5862` no zênite, `#8A949C` no horizonte) e as nuvens escurecem.
  - **A névoa** acompanha a cor do horizonte.
- **Nuvens do jogo:** a `CeuArena` passa a pular as nuvens do jogo também no mundo interior, porque elas ficariam com a cor do pôr do sol.
- **O horizonte:** `ferramentas/gerar_horizonte_interior.py` gera quatro faixas no mesmo formato da Escola e de Oriana.
  - **O que mostram:** prédios deitados de vários tamanhos, com a grade de janelas, em três camadas cada vez mais claras e azuladas com a distância. Uns mais altos, uns mais baixos, uns atravessados.
  - **Na chuva,** o anel é desenhado mais escuro.
  - O `HorizonteCliente` usa as faixas `interior` para os dois cenários.
- **A chuva:** no cenário `interior_chuva`, o cliente sobe aos poucos o nível de chuva do mundo do próprio cliente, e o jogo desenha a chuva, os respingos e o som. Ao sair do cenário, o nível volta a zero. O clima do servidor não muda.

## Testes
- **JUnit (`PlantaMundoInteriorTest`, ampliado):**
  - a fachada da luta continua livre e com chão, as entradas continuam, a caixa e o teto também;
  - há janelas acesas na fachada da luta e nos outros prédios;
  - nenhum detalhe novo (antena, caixa d'água, letreiro) sobre o campo da luta;
  - toda antena tem apoio.
- **GameTest:** quando o Zangetsu passa à fase 2, o jogador recebe o cenário `interior_chuva`. Na volta da arena, o vazio. Isso vai em `TestesZangetsu`, ao lado do teste de fase 2 que já existe.
- **A prévia `interior`** (`runClientDemo -Pdemo=interior`), como a de Oriana:
  - os prédios, o horizonte e o céu azul;
  - depois a troca para a chuva, por um comando da prévia ou forçando a fase 2;
  - e o mesmo com o VFX reduzido.

## Fora deste spec
- A água subindo e cobrindo os prédios.
- As cores invertidas do mundo do Hollow.
- Escurecer de verdade a luz dos blocos.
- Um som próprio de chuva, além do som da chuva do jogo.
