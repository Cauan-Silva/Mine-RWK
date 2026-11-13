---
tags: [desenvolvimento, spec]
estado: aguardando revisão do autor
---
# Spec: detalhe da Escola Jujutsu

Voltar para [[Roadmap]]. Pedido do autor no teste de 12/11 ([[2026-11-12 Balanceamento]]): mais detalhe nas arenas, começando pela Escola Jujutsu. A arena de hoje está em [[2026-11-11 Arena da Escola Jujutsu]] e no spec [[2026-11-10 Arena do Toji]].

## Escolhas do autor (13/11)
1. **Tudo:** os prédios por dentro, os prédios mais ricos por fora, o pátio com o jardim e o que fica além da caixa.
2. **O horizonte é um pano de fundo no céu**, desenhado pelo cliente, e não blocos de verdade.
3. **Design aprovado em 13/11:**
   - o salão vira templo com altar;
   - os pavilhões viram uma sala de aula e uma sala de treino;
   - a cidade fica ao sul, na direção da escadaria;
   - o teto é de 65 mil blocos.

## Regras que continuam valendo
- **Pátio livre:** o caminho central e a área de luta continuam livres, como o `PlantaEscolaTest` exige hoje. As entradas do jogador e do Toji não mudam.
- **Arena indestrutível:** ninguém mexe nos blocos ([[2026-11-12 Balanceamento]]).
- **Caixa:** 101 × 101 (`CaixaArena`). Nenhum bloco sai dela.
- **Montagem:** a planta continua posta em fatias por tick. Hoje são 49 mil blocos e cerca de 13 ticks.
  - **Teto:** 65 mil blocos, cerca de 18 ticks.
  - **Se passar:** a floresta da encosta fica menos densa, antes de tirar detalhe dos prédios.

## Parte 1: a planta (`core/arena/PlantaEscola`)
Novos materiais no enum `Material`. Cada um ganha o seu bloco no lado do servidor, onde os materiais de hoje já são traduzidos:
- `TATAME` (bloco de palha);
- `BAMBU_PISO` (mosaico de bambu);
- `SHOJI` (painel de vidro branco);
- `GRADE` (alçapão de abeto);
- `CERCA_ESCURA`;
- `AGUA`;
- `VITORIA_REGIA`;
- `CASCALHO`;
- `PEDRA_MUSGO`;
- `FOLHA_CEREJEIRA`, `TRONCO_CEREJEIRA`;
- `BAMBU` (a planta);
- `SINO`;
- `VELA`;
- `BARRIL`;
- `ESTANTE`;
- `SUPORTE_ARMADURA` (bloco que lembra um suporte de armas: grade de alçapão).

Blocos com orientação (escadas, alçapões, cercas) seguem o mesmo esquema de direção que as escadas de pedra já usam.

### Salão principal (templo), por dentro
- **Piso:** tatame em faixas, separadas por linhas de mosaico de bambu.
- **Divisórias:** painéis de papel dividem uma antessala, logo depois da porta, da nave.
- **Altar ao fundo:** degraus de madeira escura, uma mesa com velas, lanternas dos lados e uma estante atrás.
- **Pilares internos** em fila, nas duas laterais da nave.
- **Móveis:** mesas baixas (laje de madeira) e almofadas (tapete) perto da entrada.

### Pavilhões, por dentro
- **Leste, a sala de aula:** fileiras de mesas baixas com almofadas, e uma estante e um quadro (bloco escuro) na parede do fundo.
- **Oeste, a sala de treino:**
  - piso de madeira;
  - um suporte de armas na parede, com grade e barris;
  - dois alvos (feno com bloco de alvo);
  - tapete de treino no meio.

### Prédios, por fora
- **Varanda (engawa)** de madeira elevada, de 1 bloco, em volta do salão, com guarda-corpo de cerca escura e degraus nas entradas.
- **Janelas** com grade (alçapão) nas paredes brancas, uma a cada vão entre pilares.
- **Ornamento** nos cantos erguidos do beiral, e uma cumeeira de madeira escura.
- **Construções novas,** fora do pátio de luta e dentro da área de 41 × 41 ou na encosta:
  - **torre do sino (shōrō):** quatro pilares, um telhado pequeno e o sino pendurado no meio;
  - **depósito:** prédio pequeno de madeira, fechado, com porta;
  - **portão lateral:** um portão coberto no muro leste ou oeste, sem passagem para fora da caixa.

### Pátio e jardim
- **Lagoa:** num canto do pátio, fora da área de luta. Tem borda de pedra, água de 1 a 2 blocos de fundo, vitórias-régias e pedras de passo. A água não pode vazar: toda água tem pedra embaixo e dos lados.
- **Jardim seco:** no canto oposto, cascalho com três pedras grandes (de musgo e andesito) e uma borda.
- **Vegetação:**
  - quatro a seis cerejeiras, de 6 a 8 blocos, nas bordas do pátio e perto do salão;
  - touceiras de bambu atrás dos pavilhões.
- **Acabamento:**
  - bancos (escada de madeira) virados para o pátio;
  - pedra com musgo misturada no piso das bordas;
  - uma cerca baixa de bambu (cerca escura) entre o pátio e a floresta.

## Parte 2: o horizonte (cliente)
- **O pano de fundo:**
  - **Gerado em Python:** `ferramentas/gerar_horizonte_escola.py` gera as faixas do horizonte (PNG em `textures/ceu/horizonte_escola_<n>.png`, quatro faixas de 90°, repetíveis na emenda).
  - **Montanhas:** três camadas em degradê de fim de tarde, cada uma mais clara e mais azul com a distância.
  - **Floresta:** escura na base.
  - **Cidade ao sul:** prédios em silhueta com luzes acesas e uma torre vermelha e branca que lembra uma torre de rádio, sem copiar nenhum prédio real.
- **Desenho:**
  - **Anel:** um cilindro de quadros em volta do jogador, desenhado depois do céu e antes do terreno (`RenderLevelStageEvent`, depois do céu), com o raio perto da distância de renderização.
  - **Sempre centrado no jogador**, para nunca chegar perto. A base fica abaixo do pátio, para esconder o vazio em volta da caixa.
  - **Névoa:** um pouco de névoa na cor do céu na base, para a borda sumir.
- **O cliente precisa saber em que cenário o jogador está:**
  - **Por quê:** a dimensão das arenas é uma só, e a Escola, o mundo interior e Oriana usam vagas diferentes.
  - **Pacote:** o servidor manda um pacote novo, `CenarioArena(String cenario)`, ao entrar numa arena (`"escola"`, `"interior"`, `"oriana"`) e ao sair (`""`).
  - **Cliente:** guarda o cenário e só desenha o horizonte quando ele for `"escola"`.
  - **Depois:** as outras arenas podem ganhar o seu horizonte mais tarde, com o mesmo pacote.
- **Configuração:** o horizonte respeita o "VFX reduzido" da `minerwk-client.toml`, que desenha só a camada mais próxima.

## Testes
- **JUnit (`PlantaEscolaTest`, ampliado):**
  - o pátio livre e as entradas continuam como hoje;
  - nada sai da caixa, e nenhuma posição se repete;
  - **total de blocos:** no máximo 65 mil;
  - **lagoa:** toda água tem um bloco sólido embaixo e nos quatro lados, ou outra água;
  - **prédios:** cada um continua com a porta livre e um caminho da porta até o meio da sala;
  - **cerejeiras e bambu:** não entram no pátio de luta nem nos prédios;
  - **torre do sino:** o sino tem apoio.
- **JUnit da geometria do horizonte** (em `core/`): o anel fecha os 360°, as faixas emendam, e a base fica abaixo do pátio.
- **GameTests:** a arena da Escola monta inteira dentro do tempo, e o pacote `CenarioArena` sai ao entrar e ao sair.
- **Build e prévias:**
  - `runClientDemo` com a câmera dentro do salão, na sala de treino, no jardim e no alto da escadaria, olhando o horizonte ao sul;
  - `ferramentas/previa_arena.py` atualizado com os materiais novos.

## Fora deste spec
- O horizonte das outras arenas (o mundo interior e Oriana).
- Objetos com que se interage (o sino tocar, portas que abrem).
- Mudar o tamanho da caixa.
