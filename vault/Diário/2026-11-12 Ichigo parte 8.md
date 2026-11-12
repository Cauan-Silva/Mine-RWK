---
tags: [diario]
---
# 12/11/2026: Ichigo parte 8

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]]. Branch `ichigo-kit`. Vem depois de [[2026-11-12 Ichigo parte 7]]. Com ela o [[Ichigo]] fecha as 8 partes do spec.

## O que foi feito
A forma Hollow completa: a passiva trancada que salva o Ichigo do golpe que mataria no Bankai.

### Regras (núcleo, com JUnit)
- `core/personagens/ichigo/Ichigo`: a forma `hollow` (sem golpes em tecla), `podeVirarHollow`, `virarHollow` (1 de vida, cura 10, recarga de 5 min) e o efeito `FormaHollow`, de 10 s: a cada 30 ticks uma garra (8) em quem está a até 4 blocos ou o Sonído para junto do alvo (até 24); aos 6 s o Cero (20 ticks de carga, feixe de 24 blocos, 18 de dano), uma vez. Ao acabar: Shikai, 6 de vida e o G em recarga de 20 s.
- `Reiatsu`: parado durante a forma. `Contexto` ganhou `vida` e `inimigoProximo`.
- A forma não aparece na tela de escolha (`PersonagemEscolha` pula formas sem golpe).

### Servidor
- `kit/HollowServidor`: o golpe fatal vira a forma (só no Bankai, desperto de vez, fora de arena de boss, com a passiva pronta); na forma, metade do dano, não morre, e clique, item, bloco e entidade não respondem. `/kill` e o vazio continuam matando.
- `ContextoServidor.inimigoProximo`: o inimigo mais perto à vista (monstros, quem mira ou feriu o Ichigo, jogadores com PvP).
- Três GameTests novos (88 no total).

### Apresentação
- **Modelo** (`ferramentas/gerar_traje_hollow.py`): cubos nos ossos de armadura, textura pintada no script (refeito no mesmo dia, ver abaixo). Nenhum asset de terceiros.
- **VFX** (`RenderVisuaisIchigo`): o reiatsu preto e vermelho em volta do corpo com o estouro inicial, os três riscos da garra, o Sonído e o Cero violeta (o do Zangetsu interior continua vermelho).
- **Animações do jogador:** a transformação, a garra e o Cero. **Sons** (sintetizados): rugido, garra, carga do Cero e Cero; o Cero do boss passou a usá-los.
- Prévia: `previas/ichigo_parte8_hollow.mp4` (`./gradlew runClientDemo -Pdemo=hollow`).

## Revisão (subagente) e o que mudou por ela
- O golpe fatal passou a ser medido pelo **dano já reduzido** (`LivingDamageEvent.Pre`): antes um golpe forte que a armadura seguraria gastava a forma.
- A forma só vai atrás de **inimigo de verdade** (antes, qualquer mob: vaca, aldeão, animal de outro jogador).
- Na forma também não se usa **item, bloco nem entidade** (antes só o ataque era barrado).
- Forma interrompida (troca de dimensão, saída do jogo) **acaba com o mesmo custo** (antes saía de graça).
- O Cero pega quem está **colado** na boca do feixe.
- Ficou como está: paralisado ou caído, a forma ainda age (ela é quem manda no corpo); `/kill` no meio da forma não encerra o visual no cliente antes do tempo.

## Decisões minhas (mudam com uma palavra do autor)
- Quem a forma ataca (acima) e que o Cero fere quem estiver na linha, inimigo ou não.
- A primeira ação aos 0,75 s e a pausa em volta do Cero (10 ticks antes, 12 depois): dá seis garras e um Cero nos 10 s.
- A forma vem antes do Totem da Imortalidade.
- Não há aviso na tela da recarga de 5 minutos.

## Modelo refeito pelas referências do autor (mesmo dia)
O autor mandou três imagens do Vasto Lorde e pediu **sem o cabelo longo**. O `gerar_traje_hollow.py` foi reescrito: textura 128×128. Mudou: chifres que saem para os lados e dobram para a frente e para baixo; listras pretas na máscara; gola de pelo vermelho; marcas do buraco até os ombros; manga preta rasgada só no braço direito; chamas menores nos pulsos e tornozelos; faixa branca com laço; hakama mais curto na perna esquerda; pés com garras; e a **Tensa Zangetsu na mão direita** (a mesma do traje do Bankai, no osso `arma`). As imagens serviram só de referência; nada delas entrou no mod.

Depois, ainda a pedido: **chifres mais finos**, **olhos e boca maiores** na máscara, e **pano animado**. O hakama deixou de ser um cubo em cada perna e virou uma saia de tiras presa na cintura (inteira atrás e à direita, curta e rasgada à esquerda); a manga rasgada tem três segmentos e as pontas da faixa também. Os ossos usam os nomes do casaco do Bankai (`bk_capa_*`, `bk_manga_*`), que o `TecidoIchigo` já balança: nenhum código Java novo. Por fim o autor achou o resultado **feio**, e o modelo foi refeito de novo: textura 256×256 com as faces pintadas a quatro vezes a resolução do cubo (máscara 32×32 de olhos rasgados e presas, com a testa e o focinho em relevo; peito com o abdômen desenhado), chifres em curva suave de oito segmentos girados, e a gola e as chamas dos pulsos e tornozelos em tufos com volume no lugar dos planos recortados. São 138 ossos (114 de pano) e 329 cubos. O movimento do pano não deu para conferir pelos quadros da prévia (o jogador fica pequeno): fica para o teste do autor.

Ferramenta nova: `ferramentas/previa_traje.py <traje>` desenha qualquer traje fora do jogo em quatro vistas (`previas/traje_<nome>.png`), com a conta de giros do GeckoLib. Serve para os próximos modelos.

## Estado
`./gradlew build` e os 88 GameTests passam. A prévia `previas/ichigo_parte8_hollow.mp4` foi regravada com o modelo novo e os husks. O que testar no jogo está em [[Pendências]].
