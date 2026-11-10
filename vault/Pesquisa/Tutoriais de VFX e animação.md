---
tags: [pesquisa, vfx, animação]
---
# Tutoriais de VFX e animação (estudados em 10/11/2026)

Voltar para [[Bibliotecas]]. O autor mandou dois vídeos. Eles foram transcritos com faster-whisper e vistos em folhas de quadros.

## 1. "Photon2 Minecraft VFX Tutorial I" (KilaBash, autor do Photon, 26 min)

### O que é o Photon2
- É uma cópia do sistema de partículas do Unity: segundo o autor, cobre mais de 90% do Particle System e do Trail Renderer e parte do Line Renderer. Tutoriais de Unity servem quase direto.
- O Photon **não cuida de quando, onde e por quanto tempo** um efeito toca. Prender o efeito na entidade e controlar o ciclo de vida é com o nosso código, que é o que `EfeitosVisuais` e o servidor já fazem.
- Tem GPU instancing, shaders próprios e dados de partícula customizados na GPU. Só não faz o que exigir compute shader.

### Fluxo de trabalho
- `/photon editor` em um mundo criativo abre o editor, com hierarquia, recursos, cena e inspetor, parecido com o Unity.
- O projeto (`File > FX project`) é salvo e depois **exportado como `.fx`**. O jogo só toca o `.fx`, que fica em `assets/minerwk/fx/`. No nosso caso essa pasta ainda está vazia.
- Os comandos do Photon servem só para testar: prender o efeito num bloco ou entidade, limpar e recarregar.

### Módulos mostrados (os mesmos do Unity)
| Módulo | Uso no tutorial | Uso possível no R |
| --- | --- | --- |
| Shape (sphere, cone, dot, **mesh**) | fogos saindo de um ponto; partículas formando um texto 3D | partículas nascendo na superfície das esferas, do núcleo ou de um anel |
| Emission e **Bursts** | taxa 0 e uma rajada de 150 no tempo 0 | estouro da explosão; faíscas no toque da fusão |
| Physics (gravidade, colisão, remover ao colidir) | fogos quicando no chão | detritos e poeira caindo e parando no chão |
| **Trails** (duração, largura, cor ao longo do rastro) | rastro brilhante dos fogos | a hélice dupla da dança das esferas; fitas da aura |
| Color over Lifetime / Size over Lifetime (gradiente, curva) | amarelo a vermelho; aparecer e sumir com alfa | o filete ciano, violeta e carmesim; pulsos |
| **Noise** com *position offset* em curva | partículas que se juntam no texto e depois se espalham (curva alta, zero, alta) | **sucção**: começa espalhado e converge para o núcleo; na explosão, espalha |
| **Sub Emitters** (ao morrer, probabilidade 1) | foguete que sobe e explode em outro efeito | detrito que solta poeira ao cair; faísca que solta clarão |
| Renderer em modo **Model** | uma esfera `.obj` como escudo, com `useBlockUV` desligado e pivô 0,5 | cascas e anéis em malha com shader próprio |

### Bloom
- No Photon2 não há opção de bloom. **Qualquer cor HDR acima de 1 brilha sozinha**, e a força e o raio ficam na configuração.
- Os materiais têm *HDR color* e modo de mistura (aditivo, multiplicativo).
- **Ponto importante para nós:** o VFX do Gojo hoje é desenhado à mão (`RenderVisuaisGojo` com o shader `gojo_energy`) e por isso **não passa pelo bloom do Photon**. O brilho de verdade só vem se o desenho passar pelo pipeline HDR do Photon.

### Shaders próprios (a parte avançada)
- Usa um JSON de shader do jogo, com os arquivos em `assets/<ns>/shaders/core/`. O vertex shader faz `#moj_import <photon:particle.glsl>` e lê `ParticleData` (posição, cor, UV, luz, normal), o que funciona com e sem instancing.
- Todo uniform declarado aparece no editor para ajustar ao vivo. Se o nome tiver `HDR`, ganha um seletor de cor HDR.
- O Photon e o LDLib2 dão uniforms e samplers extras: **inversa da projeção, textura de profundidade (`SamplerSceneDepth`) e cor da cena**. Também aceitam geometry shader.
- **Fresnel:** `pow(1 - dot(normal, direçãoDaVista), power)` deixa a borda brilhando. O `gojo_energy` já faz algo parecido (`rim`).
- **Brilho na interseção:** compara a profundidade do fragmento com a da cena e acende onde a casca encosta no terreno. É o que falta para a casca da explosão "cortar" o chão com uma linha de luz.
- Com a **cor da cena** também dá para fazer **distorção** (refração, ar quente): a casca roxa ondulando o que está atrás dela.

### Animação de partículas formando malhas
Um `.obj` feito no Blender e carregado por um JSON de modelo do NeoForge serve de Shape. É preciso o ajuste de `usemtl none` no `.obj` e `newmtl none` no `.mtl`. Com Noise em curva, as partículas "montam" a forma. Serve para símbolos ou letras de energia, se um dia fizer sentido no kit.

## 2. "ANIMA a tu PERSONAJE como quieras" (MCreator, 9 min, espanhol)
- Usa o modelo de jogador do Blockbench com os ossos `body`, `head`, `torso`, `left_arm`, `right_arm`, `left_leg` e `right_leg`. **Os nomes precisam ser exatos**, senão a animação quebra. Exporta para `assets/<ns>/player_animations/` e toca pelo nome.
- É o mesmo fluxo da Player Animation Library que já usamos. Nós geramos as animações por script (`ferramentas/gerar_animacoes_gojo.py`), mas **o autor também pode animar à mão no Blockbench** e jogar o arquivo na mesma pasta, desde que respeite os nomes dos ossos.
- Uma animação de 1 quadro sem fim deixa o jogador preso na pose. Por isso nossas poses longas têm fim ou loop explícito.

## Conclusões para o projeto
1. **Partículas em quantidade** (poeira, detritos, faíscas, fumaça, rastros) são o forte do Photon: são baratas, têm física e colisão, rastros e sub-emissores. Fazer isso à mão é retrabalho.
2. **Formas exatas e coreografadas** (a dança das esferas, a deformação em gota, o núcleo e os tempos casados com a cutscene) continuam no nosso código, com testes em `core/vfx`.
3. **Bloom e distorção** pedem o pipeline do Photon (HDR, profundidade, cor da cena). Falta descobrir se dá para usar esse pipeline no nosso desenho à mão ou se as cascas precisam virar efeitos do Photon com o nosso shader.
4. **Quem cria os `.fx`:** o editor é visual, dentro do jogo. Antes de prometer efeitos `.fx` é preciso ver se o formato pode ser gerado por script ou se o autor (ou eu, controlando a tela) monta no editor.
