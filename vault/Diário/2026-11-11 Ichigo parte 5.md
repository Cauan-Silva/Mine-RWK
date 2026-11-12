---
tags: [diario]
---
# 11/11/2026: Ichigo, parte 5 (som)

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]]. Branch `ichigo-kit`. Vem depois de [[2026-11-11 Ichigo parte 4]].

## O que foi feito
Os golpes do [[Ichigo]] ganharam som: 11 sons sintetizados aqui, com 3 variações cada (33 arquivos em `assets/minerwk/sounds/ichigo/`), sem áudio de terceiros e sem os sons do Ichigo do Minezin.

O som do Ichigo é de lâmina e de pressão: ar cortado, aço, e o reiatsu como um peso grave que empurra. Nada dos tons de vidro e dos zumbidos do [[Gojo]]. O Shikai é mais aberto; o Bankai, mais grave, seco e rápido.

| Som | Quando toca | O que é |
| --- | --- | --- |
| `ichigo_getsuga_carga` | segurando o Z no Shikai | o reiatsu subindo pela lâmina, crescendo até a carga cheia; para se soltar cedo ou for interrompido |
| `ichigo_getsuga` | Z do Shikai | a saída do corte, o voo rasgando o ar e o estouro quando ele se desfaz (tick 15), com o chão crepitando |
| `ichigo_getsuga_negro` | Z do Bankai | o mesmo, mais grave e mais rápido (estouro no tick 12) |
| `ichigo_shunpo` | X das duas formas | baque seco nos pés, o ar sugado e um estalo onde ele surge |
| `ichigo_onda` | C do Shikai | a espada cravando no chão, a onda abrindo e as pedras |
| `ichigo_arremesso` | V do Shikai | a lâmina girando na ida, a volta mais rápida (tick 8) e o baque do aço na mão (tick 16) |
| `ichigo_bankai_carga` | carga do G | a pressão se juntando; para se a liberação for cortada |
| `ichigo_bankai` | G do Shikai | o estouro de pressão, o vento que varre e o aço da Tensa Zangetsu |
| `ichigo_lamina` | C do Bankai | o Getsuga aceso na lâmina pelos 6 s; apaga se sair do Bankai |
| `ichigo_sair` | saindo do Bankai | a pressão se soltando |
| `ichigo_corte` | clique da espada | só a lâmina passando pelo ar, curto |

## Como foi ligado
- **Todo som do Ichigo sai de um visual** (`core/personagens/ichigo/SonsIchigo.doVisual`): toca no ponto do visual, junto com o que se vê, e as duas cargas e a lâmina param quando o visual é encerrado antes da hora. Nenhum golpe pede som pela apresentação (tocaria duas vezes); há teste para isso.
- **`core/personagens/SonsDosVisuais`** é novo: o cliente pergunta a ele qual som vai com um visual, e ele pergunta ao Gojo e ao Ichigo. Os próximos personagens entram aí.
- **O rastro do clique da espada** é um visual criado no próprio cliente; `VisuaisAtivos.local` passou a tocar o som do visual também, então todo mundo que vê o Ichigo ouve o corte.
- **Gerador:** `ferramentas/gerar_sfx_ichigo.py`, sobre o `sfx_motor.py`. Os tempos repetem os ticks do Java e ficam em `MARCOS`.

## Decisões
- **Só os sons do que já existe.** O spec lista também máscara, Mugetsu, Cero, garra e a perda e a volta dos poderes; esses golpes são das partes 6 e 8 e ainda não têm tempos para o som seguir. Os sons deles entram junto com cada parte, no mesmo gerador.
- **Um som só para o Shunpo das duas formas.**
- **O arremesso é um som só** (ida, volta e o baque na mão), com os tempos presos aos do visual, em vez de três sons soltos.
- **O Getsuga que bate numa parede continua soando** o estouro: o som não é cortado com o visual.

## Testes
- JUnit: `SonsIchigoTest` (8): o manifesto tem exatamente os sons do kit; cada som tem 3 variações, entrada no `sounds.json` e legenda em português e inglês; cada visual tem o seu som e todo som é usado; só as cargas e a lâmina param com o visual; o cliente acha o som de qualquer personagem; os marcos batem com os ticks do Java; nenhum golpe pede som pela apresentação. 348 testes de unidade e 69 GameTests passam.
- **Medição do áudio** (não dá para ouvir daqui): o estouro do Getsuga cai no tick do fim do voo (o volume sobe de 0,04 para 0,17 ali; no negro, de 0,05 para 0,18); as duas cargas crescem até o fim; a lâmina fica estável nos 6 s. O baque da espada na mão estava fraco por baixo do giro (0,05 contra 0,13 antes dele) e foi reforçado (0,25 contra 0,09).

## Prévia
`previas/sfx_ichigo.wav`: os 33 arquivos em fila (cada som nas 3 variações), para ouvir sem abrir o jogo. Gerar de novo: `python ferramentas/gerar_sfx_ichigo.py --preview previas/sfx_ichigo.wav`.

## Falta
- **O autor ouvir**: a prévia e depois no jogo (ver [[Pendências]]). Ninguém ouviu estes sons ainda; os ajustes de timbre saem daí.
- Próximo: parte 6 (máscara Hollow e o Mugetsu com cutscene e a perda dos poderes), com os sons deles.
