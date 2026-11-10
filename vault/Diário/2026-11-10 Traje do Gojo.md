---
tags: [diario]
---
# 10/11/2026: traje do Gojo

Voltar para [[Diário]]. Spec: [[2026-11-10 Traje do Gojo]] (aprovado em 10/11). Branch `gojo-traje`.

## Escolhas do autor
Traje automático ao escolher o Gojo, sem item; venda preta que sai no Vazio Infinito e no R; sem cabelo; Seis Olhos como está. As duas passivas já existiam, então não houve nada a programar nelas.

## Parte 0: o protótipo
Os três pontos de risco do spec foram provados antes do resto:
- **Traje sem item vestido:** funciona com o renderer de armadura do GeckoLib e um item escondido (`traje_gojo`), que ninguém recebe.
- **Armadura normal escondida:** funciona trocando a camada de armadura do jogador.
- **Seguir as animações:** funciona em terceira pessoa. Em primeira pessoa faltava a manga, resolvida na parte 1.

## Partes 1 a 3
- **Modelo e textura:** vêm do Minezin (`ferramentas/traje/gojo_base.*`). O gerador `ferramentas/gerar_traje_gojo.py` tira os óculos e põe a venda, com as duas pontas do nó atrás, e os olhos.
- **Venda que sai:** some, e os olhos azuis brilham, enquanto o jogador tiver um visual do Vazio Infinito ou do R (`TrajeGojo.vendaFora`). O brilho usa a máscara `gojo_glowmask.png`.
- **Barra do casaco:** os 20 segmentos balançam com a velocidade do jogador (`TrajeGojo.balanco`): correndo, a de trás abre e a da frente cola; caindo, todas sobem.
- **Primeira pessoa:** a manga aparece na mão parada (`RenderArmEvent`) e nos braços animados dos golpes.
- **Outros jogadores:** o servidor avisa o personagem de cada jogador a quem o vê (`PersonagemDe`), ao entrar no alcance e ao trocar.
- **Config do cliente:** `armaduraSobreTraje` (padrão desligado) e `olhosBrilhando` (padrão ligado).

## O que deu trabalho
- **Visibilidade por espaço:** o renderer do GeckoLib só mostra as peças do espaço de armadura pedido e reaplica isso ao desenhar. A solução foi sobrescrever essa regra para o traje seguir a visibilidade das partes do modelo do jogador.
- **Primeira pessoa com animação:** a Player Animation Library só deixa passar, em primeira pessoa, as camadas de armadura e de item na mão. Por isso o traje é desenhado pela própria camada de armadura (uma subclasse), e não por uma camada nova.
- **Lados da barra:** no modelo do Minezin os ossos "left" e "right" são de quem olha de frente; o "left" fica do lado direito do corpo.

## Diferenças em relação ao spec
- A venda some e reaparece sem o gesto de tirar; a animação do golpe não foi mexida.
- A venda sai quando o domínio abre, e não já na carga de meio segundo, porque a carga do G não tem visual próprio.

## Prévia
`previas/traje-gojo.mp4`, gravado com `./gradlew runClientDemo -Pdemo=traje`.

## Revisão
Revisão do branch por um subagente: nada crítico nem importante. Os cinco achados menores ficaram em [[Pendências]], sem correção.
