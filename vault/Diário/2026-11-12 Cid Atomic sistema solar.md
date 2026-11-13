---
tags: [diario, cid]
---
# 12/11/2026: o recuo do I Am Atomic revela o sistema solar

Voltar para [[Diário]]. Spec e plano: [[2026-11-12 Cid Atomic sistema solar]]. Branch `atomic-sistema-solar`.

## De onde veio
No teste depois do balanceamento, o autor achou que o final do I Am Atomic ainda tinha pouco impacto e disse o que faltava: "mostrar mais o sistema solar", no recuo da câmera do estouro. O motivo estava no céu do espaço: ele é desenhado numa casca que anda com a câmera, então o recuo de 105 blocos não mudava nada. O autor escolheu a abordagem A: uma cena virtual que o céu desenha durante o recuo.

## O que foi feito
- **`SistemaSolar` (core):** o Sol, os oito planetas (Saturno com anel) com as distâncias apertadas, e a Terra parada no centro da cena. A câmera virtual sai colada na Terra (que toma a metade de baixo do céu, igual ao planeta de sempre) e recua até ficar atrás da câmera da cutscene, a 500 raios da Terra. Também calcula o ponto roxo na Terra, que pulsa e no fim se abre até tomar a tela, e a esfera do mundo que se apaga quando ele assume.
- **O estouro passou de 70 para 130 ticks** (o golpe vai de 17 para 20 s). A esfera do mundo cresce no ritmo de antes (`TICKS_DA_ESFERA = 70`), e a luz vista do chão dura o clarão mais 2,5 s. A câmera final ficou mais alta, em `(40, 55, -100)`, para as órbitas aparecerem em elipse. O tremor fica só nos 2 s do estouro.
- **O céu do espaço:** com a cutscene `CLARAO` na tela, desenha a cena. A Terra é a calota do planeta de sempre reescalada, com a atmosfera em volta. A lua e os dois planetas de sempre somem, e entram o Sol com halo, os planetas com a face de trás na sombra, as órbitas e o ponto roxo com raios em cruz. Sem a cutscene (config desligada, ou o alvo), o céu é o de sempre.
- **O som:** o `cid_atomic` estica para os 6,5 s, ganha um grave largo que cresce no recuo e um último estouro no pulso (marco `pulso`, conferido pelo `SonsCidTest`). O `cid_atomic_luz` acompanha a luz mais longa.

## Decisões
- **`Vetor.ponto`** (produto escalar) entrou no núcleo para a conta da câmera.
- **O som foi feito antes do céu:** o teste dos marcos já cobrava os 130 ticks.
- **Dois GameTests do Atomic** tinham o tempo do clarão fixo (70 e 76) e passaram a usar a constante.
- **Só os sons do Atomic entram no commit:** o gerador regrava todos, e os outros saíram iguais.
- **Ajustes vistos na prévia:** a câmera virtual vira até o olhar da cutscene junto com a Terra encolhendo (até t = 45), e não até t = 95, porque senão a Terra saía do quadro e a tela ficava preta por 1,5 s. O plano final ficou mais perto (500 raios da Terra, não 650), e o tamanho mínimo dos astros foi de 0,35° para 0,5°: o sistema ficava pequeno demais.
- **A prévia `cid_final`:** o husk ganhou 400 de vida (com os golpes ×3 ele morria nos socos e o golpe pulava para o Atomic), e a prévia começa sempre no mesmo campo de neve (o fim de cada prévia move o jogador, e da última vez ele começou no meio das árvores, sem ver o alvo).
- **Sem ffmpeg neste computador:** a prévia saiu em GIF e numa folha de quadros.
- **Os GameTests falharam duas vezes seguidas e passaram nas duas seguintes.** As falhas foram o Toji derrubando o servidor de testes com um pacote de animação do GeckoLib, e o `vermelhoCongelaEArremessaZumbi` uma vez. Nenhum dos dois foi mexido aqui: parece instabilidade que já existia.
- **A Terra como bola** mostra um desenho em flor, porque a textura do planeta de sempre é polar, pensada para ser vista de dentro. Dá para refinar depois.
- **A base do céu usa a direção da cutscene**, a mesma da câmera, e não a do visual: as duas são o rumo do R, que é sempre horizontal.

## Testes
JUnit: `SistemaSolarTest` (15), cobrindo a Terra que começa igual ao planeta de hoje, a bola inteira em t = 45, a Terra que não sai do quadro durante o recuo, tudo no quadro em t = 95 (45° para os lados, 22° para cima e para baixo), a Terra no meio, o Sol maior, as órbitas, a esfera passando para o céu, o pulso cobrindo a tela e a luz depois da volta. Também o `CidAtomicTest` com a volta em 352 e o marco do pulso no `SonsCidTest`. GameTests: os 128 passam.

## Falta
O autor ver a prévia `previas/cid_atomic_recuo.gif` (a folha de quadros em `previas/cid_atomic_recuo.png`), ouvir `previas/sfx_cid_atomic_recuo.wav` e testar no jogo (roteiro em [[Pendências]]).
