---
tags: [desenvolvimento, spec]
estado: aguardando revisão
---
# Spec: traje do Gojo e passivas (Fase 2, parte 10)

Voltar para [[Roadmap]]. Fecha o item "Traje (GeckoLib) com óculos, e passivas (Seis Olhos, Técnica Reversa)". Nada foi programado: é para o autor aprovar ou mudar.

## Escolhas do autor (10/11)
1. **Vestir:** automático. Quem escolhe o Gojo aparece com o traje, sem item. Os espaços de armadura ficam livres.
2. **Olhos:** venda preta, que **sai no Vazio Infinito e no R**, mostrando os olhos azuis brilhando, e volta depois.
3. **Cabelo:** sem cabelo. Fica a cabeça da skin do jogador, com a venda por cima.
4. **Seis Olhos:** fica como está.

## Passivas: nada a programar
As duas já existem e continuam iguais:
- **Técnica Reversa:** depois de 5 s sem levar dano, cura 1 coração a cada 4 s (`TecnicaReversa`).
- **Seis Olhos:** a energia enche rápido, 1,0 por tick parado e 0,4 andando (`EnergiaAmaldicoada`).

Este spec só as marca como feitas no Roadmap. Quando o despertar do [[Toji]] entrar, a Técnica Reversa passa a ser liberada pela vitória; isso é do spec dele.

## O traje
Uniforme da Escola Técnica de Jujutsu, como o do Minezin, que é do mesmo autor:
- **Casaco:** preto-azulado, gola alta em anel até o queixo, fecho escondido, mangas largas até o pulso.
- **Barra do casaco:** em segmentos, balançando com o movimento (o tecido do Minezin).
- **Calça** larga e **sapatos** pretos.
- **Venda:** faixa preta em volta da cabeça, cobrindo os olhos da skin. Substitui os óculos redondos do Minezin.

Cores do Minezin: base `#1b1f2a`, sombra `#0f1117`, meio-tom `#262b38`, luz `#333a4a`.

### A venda que sai
| Momento | Venda | Olhos |
| --- | --- | --- |
| Normal, e nos golpes Z, X, C e V | no rosto | cobertos |
| Vazio Infinito (G), da carga até o domínio fechar | fora | azuis, brilhando |
| R, do encantamento até o fim da explosão | fora | azuis, brilhando |

- A venda não cai no chão: ela some do rosto com um gesto curto da animação do golpe e reaparece no fim.
- Os olhos são uma camada que brilha no escuro, desenhada sobre a skin, no lugar dos olhos de uma skin padrão. Em skins com o rosto em outra posição o brilho pode não cair em cima dos olhos; a config permite desligar.
- O brilho que o encantamento do R já desenha na frente do rosto passa a sair desses olhos.

## Como aparece para os outros
Hoje cada cliente só sabe o próprio personagem. Para todos verem o traje, o servidor passa a avisar qual personagem cada jogador escolheu a quem o vê (ao entrar no alcance e ao trocar). O momento da venda sai dos visuais que o cliente já recebe, então não precisa de pacote novo.

## Armadura normal
Com o traje automático, os espaços de armadura ficam livres. A armadura vestida continua protegendo, mas **não é desenhada** em quem está com o traje, para não cobrir o uniforme. Uma opção na config do cliente (`mostrar armadura sobre o traje`) inverte isso.

## Técnica e reaproveitamento
- **Modelos e textura:** reaproveitar do Minezin `geo/armaduras/gojo*.geo.json` e `textures/armor/trajes/gojo.png` (128 × 128), trocando os óculos pela venda e tirando o que for cabelo. O gerador (`tools/build_gojo_cloth.py` e `build_gojo_rework.py`) vem para `ferramentas/`. Registrar em `THIRD_PARTY_NOTICES.md` como vindo do Minezin.
- **Desenho:** GeckoLib, numa camada do jogador só no cliente, que segue os ossos do modelo (e portanto as animações dos golpes).
- **Tecido da barra:** a lógica do balanço fica em `core/`, com JUnit, como no `TecidoGojo` do Minezin.

### Parte 0: provar antes de construir
Três pontos podem não funcionar como imaginado, então viram um protótipo curto antes do resto:
1. Desenhar um modelo do GeckoLib sobre o jogador **sem item de armadura** vestido. Se não der de forma limpa, o plano B são peças de armadura invisíveis ao jogador, que o mod veste e tira sozinho.
2. Esconder a armadura normal de quem está com o traje.
3. O traje acompanhando as animações da Player Animation Library (braços, corpo flutuando no R) e aparecendo em primeira pessoa nos braços.

Se algum falhar, eu paro e trago as alternativas antes de seguir.

## Divisão em partes
0. Protótipo dos três pontos acima.
1. Modelos, textura e gerador portados, com a venda; traje desenhado no próprio jogador.
2. Sincronizar o personagem para quem vê; armadura normal escondida; config.
3. Venda que sai e olhos brilhando no G e no R; barra do casaco com tecido.

## Testes
- **JUnit:**
  - quando a venda está fora, a partir dos visuais ativos do jogador (G e R sim; Z, X, C e V não);
  - o tecido da barra (mesma hierarquia de ossos do modelo, balanço limitado);
  - os arquivos do traje (ossos esperados, textura no tamanho do modelo).
- **Prévia:** um modo da demo gravável em terceira pessoa, girando em volta do jogador, com a venda saindo num G. Eu gravo e mando o vídeo antes do teste no jogo.
- **No jogo:** o traje em 1ª e 3ª pessoa, com armadura vestida, com skins diferentes (clássica e de braço fino), outro jogador vendo, trocar de personagem tirando o traje, e a venda no G e no R.

## Perguntas em aberto
1. **Braço fino:** as skins de braço fino (modelo Alex) ficam com a manga do traje um pixel mais larga. Aceito isso, ou faço as duas larguras?
2. **Sem cabelo e a venda:** a venda dá a volta na cabeça da skin, por cima do cabelo que a skin tiver. Tudo bem?

## Fora deste spec
O traje dos outros personagens, o boss [[Toji]] e o despertar, ícones das habilidades e retrato na tela de escolha.
