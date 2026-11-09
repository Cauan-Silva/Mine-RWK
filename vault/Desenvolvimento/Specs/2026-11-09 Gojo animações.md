---
tags: [desenvolvimento, spec]
estado: proposto
---
# Spec e plano: animações dos golpes do Gojo (Fase 2, parte 4)

Voltar para [[Roadmap]]. Vem do teste de 09/11/2026. O autor achou a cutscene "dura" e as animações de todos os ataques simples. A ordem que ele aprovou é: **animações → cutscene nova do R → VFX mais pesado → som**. Este spec cobre só as animações. As referências estão no fim.

## O que existe hoje e por que parece simples
- **Arquivo:** são 5 animações herdadas do Minezin em `player_animations/gojo.json`: `gojo_azul`, `gojo_vermelho`, `gojo_infinito`, `gojo_roxo` e `gojo_dominio`.
- **Só três ossos:** elas mexem os dois braços e o tronco. Cabeça, pernas e a posição do corpo ficam paradas, então não há peso, inclinação nem flutuação.
- **Só em terceira pessoa:** em primeira pessoa (como o autor joga) não se vê nada.
- **Duração errada:** depois do ritmo novo, os golpes duram mais que as animações. O Vermelho, por exemplo, tem 1,4 s de animação contra até 3 s de golpe. O Roxo 200% usa a mesma animação do Roxo normal.
- **Movimento linear:** os quadros-chave vão em linha reta, sem antecipação nem exagero, com a mesma velocidade do começo ao fim.

## Objetivo
Pronto quando:
- cada golpe tem a sua animação, do começo da carga até o fim do golpe;
- **em primeira pessoa**, os braços animados aparecem na tela;
- as animações mexem o corpo inteiro (cabeça, tronco, braços, pernas e posição), com suavização variada;
- os outros jogadores veem a animação (já funciona pela `apresentacao`);
- os testes JUnit novos passam e o autor aprova no jogo.

## 1. Primeira pessoa
- A Player Animation Library 1.1.6 tem o modo `FirstPersonMode.THIRD_PERSON_MODEL`, que desenha os braços do modelo animado no lugar da mão normal. Ele vai ligado na camada do kit (`AnimacoesJogador`).
- A configuração mostra os dois braços e os itens na mão. O braço segue a câmera (`setFirstPersonFollowsCamera`).
- **Limite honesto:** o modelo do Minecraft não tem dedos. Os gestos do mangá (indicador e mindinho, a pinça) viram poses de braço e de mão (pulso girado, braço à frente do rosto). Dedos de verdade exigiriam um modelo de mão próprio no GeckoLib, fora deste spec.

## 2. As animações (todas novas, num arquivo só)
São 6 animações, uma por golpe. O nome é o mesmo id do golpe (o Roxo 200% ganha a dele). A duração cobre a carga mais o golpe.

| Golpe | Duração | Movimento |
| --- | --- | --- |
| Azul | 3,4 s | Antecipação: puxa o braço direito para trás e gira o tronco. Depois estica a mão aberta à frente com um tranco e fica tremendo de leve enquanto o campo puxa as coisas. Cabeça inclinada, olhando o núcleo. Volta suave. |
| Vermelho | 3,0 s | Na carga, o braço direito sobe com o indicador à frente (pulso girado) e o esquerdo segura o pulso. O corpo se inclina para trás conforme carrega. No disparo, o braço dá um coice: tranco rápido para trás, tronco recuando e um passo de perna. |
| Infinito | 1,2 s | A mão esquerda sobe à frente do peito, de palma aberta, com o corpo ereto e a cabeça levemente erguida. Pose calma, sem tranco. |
| Roxo | 3,6 s | Os dois braços se abrem para os lados (o Azul numa mão, o Vermelho na outra) e se juntam devagar à frente. No disparo, o braço direito estica com um tranco forte, o corpo avança e as pernas abrem em base. |
| Roxo 200% | 7,0 s | Começa como no Vermelho e no Azul, com uma mão de cada vez e a cabeça baixa. A cada palavra do encantamento, uma pose de gesto (são 4). O corpo **sobe 0,6 bloco** (flutua, como na referência) ao longo da carga. No disparo, o braço estica no tranco mais forte de todos e desce suave. |
| Vazio Infinito | 2,5 s | A mão direita sobe à frente do rosto, com indicador e médio cruzados (pulso girado). A cabeça abaixa e depois ergue. A pose fica até o fim e solta devagar. |

Princípios em todas:
- **Antecipação** antes de todo movimento forte (puxar antes de empurrar).
- **Exagero e volta:** passa um pouco do ponto e volta.
- **Suavização variada:** a carga é lenta e o disparo é rápido (1 a 2 ticks), com saída suave.
- **Corpo inteiro:** os braços nunca se mexem sozinhos.

**Como ficam as animações:** escrevo o JSON no formato do Blockbench (Bedrock/GeckoLib), com as curvas de suavização que a biblioteca lê. Ele abre no Blockbench, então o autor (ou um animador) pode ajustar depois sem mexer em código.

## 3. Ligar cada animação ao golpe certo
- **Hoje:** a `apresentacao` toca a animação só no disparo, e o Roxo 200% usa `gojo_roxo`.
- **Muda:**
  - A animação passa a começar **no início da carga** (onde a carga existe) e a durar até o fim do golpe.
  - Se a carga for cortada (golpe forte, morte), a animação para.
  - O Roxo 200% passa a usar `gojo_roxo_200`.
- **Velocidade:** a carga do Vermelho tem tamanho variável (5 a 30 ticks). A animação foi feita para a carga máxima. Se o jogador soltar antes, ela salta para a pose do disparo.

## 4. Testes
- **JUnit (núcleo):** um teste lê `gojo.json` e confere:
  - que existe uma animação para cada golpe do Gojo, com o mesmo id;
  - que a duração de cada uma é pelo menos a carga máxima mais a duração do golpe;
  - que todo osso usado existe no modelo do jogador.
- **Teste no jogo pelo autor:** cada golpe em primeira e em terceira pessoa (F5), e um segundo jogador vendo de fora.

## Fora deste spec (próximas partes, nessa ordem)
1. **Cutscene nova do R:**
   - planos curtos com corte seco e tremor de câmera;
   - close nos olhos;
   - plano do alvo;
   - mundo sem cor com só a técnica colorida;
   - tela preta com o Azul e o Vermelho se juntando;
   - quadro de impacto no disparo.
2. **VFX mais pesado:** riscos de velocidade, quadros de impacto, mais partículas.
3. **Som.**
4. **Chão do domínio:** espera o autor dizer como testou.

## Referências
- Vídeo 1 ("Gojo vs. Sukuna – Vazio Roxo", 8 s): planos de ~0,8 s alternando o Gojo e o Sukuna, braço esticado com o Azul escorrendo, tela preta com as duas esferas se juntando e o disparo com riscos.
- Vídeo 2 (mangá animado, caps. 234 e 235, ~105 s): mundo preto e branco com só a técnica colorida, close nos olhos brilhando, o Gojo flutuando sobre a cidade destruída e o Vermelho saindo da ponta dos dedos com um rastro de luz.
- O gesto e o encantamento do cap. 223 estão em [[Gojo]].
