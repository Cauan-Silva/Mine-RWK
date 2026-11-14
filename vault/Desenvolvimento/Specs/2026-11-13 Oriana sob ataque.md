---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec: Oriana sob ataque

Voltar para [[Roadmap]]. É a segunda arena do pedido do autor no teste de 12/11, "mais detalhe nas arenas". A primeira foi a Escola: [[2026-11-13 Detalhe da Escola Jujutsu]]. A arena de hoje está em [[2026-11-12 Cid parte 7]] e no código `core/arena/PlantaOriana`.

## Por quê
A arena do Ragnarok é um gramado limpo: uma estrada, a muralha inteira, quatro torres, oito árvores e o portal no céu. Nada nela diz que a capital está sendo destruída. Na obra, o Mordred invoca o Ragnarok pela Rosa Negra para arrasar Oriana (LN vol. 4, cap. 3; anime, 2ª temporada).

## Escolhas do autor (13/11)
1. **A capital sob ataque**, e não intacta: fogo e fumaça atrás da muralha, a muralha rachada, crateras e destroços no campo, a noite avermelhada.
2. **A abordagem A:** blocos na planta da arena, mais a cidade e o céu no cliente, pelo mesmo caminho da Escola.

## Regras que continuam valendo
- **O meio fica livre:** de −30 a +30 nos dois eixos e até a altura do voo do Ragnarok, nada acima do chão, como o `PlantaOrianaTest` exige. No chão (`dy` −1) pode haver marcas, mas nunca buraco.
- **Entradas:** a do jogador (16 ao sul) e a do boss (8 ao norte) não mudam.
- **A arena é indestrutível** ([[2026-11-12 Balanceamento]]).
- **A caixa:** 101 × 101. Nenhum bloco sai dela.
- **Teto:** 60 mil blocos (o limite do teste de hoje).
- **O portal da Rosa Negra** continua no mesmo lugar, acima das torres.
- **Ninguém sai da caixa:** brechas e portão mostram o que há atrás, mas não deixam passar.

## Parte 1: a planta (`core/arena/PlantaOriana`)
Os materiais novos entram no enum `Material` e ganham o seu bloco no servidor, como os da Escola:

| Material | Bloco | Uso |
| --- | --- | --- |
| `NETHERRACK` | netherrack | base do fogo, que queima para sempre |
| `FOGO` | fogo | chamas |
| `FOGUEIRA` | fogueira acesa | fogo que não se espalha |
| `TERRA_QUEIMADA` | terra grossa | chão queimado |
| `PEDRA_NEGRA` | blackstone | marcas de queimado |
| `BRASA` | bloco de magma | o miolo das crateras |
| `TIJOLO_RACHADO` | tijolo de pedra rachado | muralha e torres |
| `TIJOLO_MUSGO` | tijolo de pedra com musgo | muralha e torres |
| `FENO` | fardo de feno | destroços e carroças |
| `LA_VERMELHA` | lã vermelha | estandartes |

Os da Escola que servem aqui são reaproveitados: `CERCA_ESCURA`, `BARRIL`, `CASCALHO` e `MADEIRA_ESCURA`.

### A muralha e as torres
- **Tijolos misturados:** rachados e com musgo, uns 30%, e manchas pretas de queimado (`PEDRA_NEGRA`) na face de dentro.
- **Ameias faltando** em trechos.
- **Duas brechas em V**, cortadas do topo até a altura 4. Por elas se vê a cidade queimando, mas não dá para passar.
- **Entulho** de pedra caída no pé de cada brecha.
- **O portão arrombado:**
  - uma folha caída no chão, à frente do vão;
  - a outra pendurada torta;
  - atrás do vão, uma barricada de entulho e madeira que fecha a passagem.
- **Torres:**
  - uma, a de x −20, com o topo destruído: sem o telhado, com a borda quebrada e fogo em cima;
  - as outras com estandartes vermelhos rasgados pendurados, de comprimentos diferentes.

### O campo (fora do meio livre)
- **Crateras:**
  - quatro a seis, só no chão (`dy` −1): um miolo de brasa, um anel de pedra negra e uma borda de terra queimada;
  - elas podem entrar no meio livre, porque são só chão;
  - as maiores, com terra erguida em volta, ficam nas beiradas.
- **Chão queimado:** manchas de terra queimada espalhadas pelo gramado e pela estrada.
- **A estrada:** com falhas de cascalho e de terra.
- **Fogo:**
  - oito a doze focos nas beiradas, em netherrack, e fogueiras perto dos destroços;
  - nenhum foco a menos de 3 blocos de um bloco que pega fogo (madeira, folha, feno, lã, cerca, barril), na horizontal, nem a menos de 5 na vertical;
  - assim o fogo da vanilla não se espalha nem queima a arena.
- **Destroços:**
  - **duas ou três carroças viradas:** madeira, cerca como roda, feno e barris espalhados;
  - **barricadas** de madeira e cerca, perto do portão;
  - **pedras caídas** da muralha, até uns 6 blocos dela.
- **Árvores:**
  - seis das oito ficam queimadas: tronco escuro sem folhas, galhos quebrados;
  - duas, ao sul, ainda com folhas.
- **Postes:** um dos quatro tombado, com a lanterna no chão.

## Parte 2: a cidade e a noite (cliente)
- **O horizonte de Oriana:** `ferramentas/gerar_horizonte_oriana.py` gera quatro faixas (`textures/ceu/horizonte_oriana_<n>.png`) no mesmo formato e com a mesma regra de emenda do gerador da Escola.
  - **Ao norte, atrás da muralha, a capital:**
    - telhados apinhados;
    - o castelo com torres no meio;
    - incêndios em laranja com o brilho subindo;
    - colunas de fumaça escura.
  - **Nas outras direções:** o campo à noite, com morros escuros e vilas pequenas pegando fogo ao longe.
  - **A paleta:** noite, com o laranja do fogo como a única luz forte.
- **O anel:** o `HorizonteCliente` passa a escolher as faixas pelo cenário, `escola` ou `oriana`. O anel (`AnelHorizonte`), a saia e a névoa são os mesmos.
- **A noite:** com o cenário `oriana`, o cliente cobre o céu do pôr do sol com uma noite avermelhada:
  - uma cúpula escura com algumas estrelas e um brilho laranja baixo ao norte;
  - a névoa no mesmo tom.
  - **Limite que fica:** os blocos continuam com a luz do pôr do sol, porque a hora da dimensão é uma só para todas as arenas. Escurecer de verdade fica fora.
- **Fumaça e fagulhas:** o cliente solta partículas em pontos fixos atrás da muralha e sobre a torre destruída:
  - colunas de fumaça que sobem e se desfazem;
  - fagulhas laranja.
  - Com o "VFX reduzido", só a metade.

## Testes
- **JUnit (`PlantaOrianaTest`, ampliado):**
  - o meio livre, as entradas, a caixa, o teto de 60 mil e o portal continuam;
  - **o fogo não se espalha:** nenhum `FOGO` sem netherrack embaixo, e nenhum bloco que pega fogo perto dele, nas distâncias acima;
  - **as brechas não deixam passar:** na faixa da muralha, cada coluna tem bloco do chão até a altura 3;
  - **o portão fechado:** a barricada atrás do vão fecha a passagem;
  - **as crateras:** todo o chão continua com bloco em `dy` −1.
- **JUnit do horizonte:** o gerador confere a emenda das faixas, como o da Escola. O `AnelHorizonte` já é testado.
- **GameTests:**
  - a arena de Oriana monta inteira dentro do tempo;
  - **o fogo fica:** depois de alguns segundos de tick, nenhum bloco da planta sumiu;
  - o cenário `oriana` chega ao cliente (já existe).
- **Prévia:** `runClientDemo -Pdemo=ragnarok` (ou uma `-Pdemo=oriana` sem a luta), olhando o portão, uma brecha, a torre queimada e o campo de dentro.

## Fora deste spec
- **Iluminação noturna de verdade** nos blocos (a hora é uma só para todas as arenas).
- **Som ambiente:** fogo estalando ao longe, sinos, gritos.
- **O mundo interior do Ichigo**, a próxima arena.
- **Destruição durante a luta:** o Ragnarok quebrar coisas. A arena continua indestrutível.
