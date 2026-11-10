---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec: o boss Toji e o despertar do Gojo

Voltar para [[Roadmap]]. Fecha o item "[[Toji]] como boss (Nêmesis do Gojo) e o despertar". Depende da maestria e da árvore, já feitas ([[2026-11-10 Maestria e árvore de habilidades]]). Aprovado pelo autor em 10/11; parte 1 feita.

## Escolhas do autor (10/11)
1. **O que desperta:** como na obra. O Gojo começa com Azul e Infinito; o despertar libera a Técnica Reversa, o Vermelho, o Roxo, o Vazio Infinito (G) e o Vazio Roxo máximo (R).
2. **Requisito:** maestria 20 e o nó **Despertar** comprado na árvore.
3. **Como começa:** um item de invocação, feito numa bancada com itens comuns, que só funciona para quem cumpre o requisito. Dá para repetir a luta.
4. **O despertar acontece no meio da luta.** O Toji derruba o Gojo com a lança; em vez de morrer, ele desperta e termina a luta com os golpes novos.
5. **Só fica se vencer.** Durante a luta os golpes novos funcionam; perdendo, o jogador invoca de novo e desperta outra vez no meio.
6. **Qualquer um ajuda.** Outros jogadores podem bater e apanhar, mas só quem invocou desperta.
7. **Som:** sintetizado aqui, como o do Gojo. Modelo, textura e animações vêm do Minezin.
8. **Tamanho:** em partes, começando pelo essencial.

## Golpes trancados
- Cada personagem diz quais habilidades e passivas exigem o despertar. No Gojo: **X** (Vermelho), **V** (Roxo), **G**, **R** e a passiva **Técnica Reversa**. O **Z** (Azul), o **C** (Infinito) e os Seis Olhos ficam livres.
- Apertar a tecla de um golpe trancado não faz nada. No HUD o quadro dele aparece escuro, com um cadeado.
- **Quem já joga de Gojo** passa a ter esses golpes trancados quando esta parte entrar. Para testes, `/rwk despertar <jogador> sim` continua valendo.
- Existem dois estados: **despertou** (definitivo, salvo, vem da vitória) e **desperto na luta** (temporário, só enquanto o Toji daquele jogador existe).

## O item: Contrato do Matador de Feiticeiros
- **Receita (sem forma):** 1 papel, 1 lingote de ouro e 1 espada de ferro.
- **Uso:** clicar com o botão direito no chão. Exige ser Gojo, ter o nó Despertar e não ter outro Toji vivo. Sem isso, o item não é gasto e uma mensagem diz o que falta.
- O Toji aparece a 8 blocos, na direção do olhar, e o item é consumido.
- Quem já despertou também pode invocar, para lutar de novo.

## O Toji
- **Modelo:** entidade do GeckoLib com o modelo, a textura e as 14 animações do Minezin (`nemesis/1`: idle, walk, entrada, fase, investida, estocada, giro, arremesso, sumir, execucao, atordoado, morte e duas de conjuração). Crédito em `THIRD_PARTY_NOTICES.md` como vindo do Minezin.
- **Vida:** 240 (12 barras de coração), com barra de boss no topo da tela.
- **Sem energia amaldiçoada:** não tem aura nem brilho, e **o Vazio Infinito não o paralisa**. O Azul o puxa com metade da força.
- **Alvo:** quem o invocou; passa a atacar também outro jogador que o ferir.
- **Não solta itens.** A recompensa é o despertar e 500 de XP de maestria para quem invocou.

### Golpes da parte essencial
Todo golpe tem um **aviso** (a pose da animação e uma marca no chão) antes da janela de dano, e uma pausa depois, em que o Toji fica aberto.

| Golpe | Aviso | Efeito |
| --- | --- | --- |
| Investida Celeste | 18 ticks, linha de 12 blocos no chão | avança pela linha; 8 de dano e empurrão |
| Lança Invertida do Céu | 14 ticks, brilho na ponta | estocada de 6 blocos; 10 de dano. **Atravessa o Infinito**, desliga-o e o põe em recarga |
| Corrente de Mil Milhas | 12 ticks, a corrente estala | puxa o alvo de até 14 blocos e emenda um soco de 6 |

Os números do Minezin (18, 22 e 14 de dano) foram reduzidos, porque aqui o jogador tem 20 de vida e nenhuma armadura do kit.

### O despertar no meio da luta
Acontece na primeira destas três condições:
- o Toji perde 20% da vida;
- passam 45 segundos de luta;
- um golpe do Toji mataria o jogador.

O que acontece:
1. O Toji acerta a Lança, que atravessa o Infinito. O jogador cai e fica 3 segundos no chão, sem poder agir e sem poder morrer.
2. Ele se levanta com a vida e a energia cheias, as recargas zeradas e os golpes trancados liberados (**desperto na luta**).
3. O Toji passa para a fase 2 da luta.

Depois do despertar a luta é de verdade: o jogador pode morrer. Na obra o Gojo acaba com o Toji usando o Roxo; aqui qualquer golpe serve, e o R funciona nele normalmente.

Para quem **já despertou** e invoca de novo, não há queda: a luta começa direto na fase 2.

### Como a luta acaba
| Situação | Resultado |
| --- | --- |
| O Toji morre | quem invocou ganha o **despertou** definitivo e 500 de XP; os nós de depois do despertar abrem na árvore |
| Quem invocou morre, sai do jogo, troca de dimensão ou se afasta mais de 64 blocos | o Toji some; o "desperto na luta" acaba e os golpes voltam a trancar |
| Passam 10 minutos | o Toji some, como acima |

Um golpe em andamento quando o Toji some termina normalmente.

## Técnica
- **Motor de golpes em `core/boss/`, sem classes do Minecraft, com JUnit:** cada golpe tem aviso, janela de dano, pausa, recarga, alcance e peso; o motor escolhe o próximo golpe pela distância e pelas recargas. Serve aos próximos bosses.
- **Regras do despertar em `core/`:** quais habilidades um personagem tranca, e quando o despertar no meio da luta dispara.
- **Entidade e item** registrados no mod; o desenho do Toji fica só no cliente.
- **Dados salvos:** o "despertou" já existe. O "desperto na luta" não é salvo: vive com o Toji.
- **Rede:** o pacote de sincronização passa a dizer se cada habilidade está trancada.

### Parte 0: provar antes de construir
1. O modelo e as animações do Minezin (Forge 1.20.1) abrem no GeckoLib daqui (4.9.3, NeoForge 1.21.1) como entidade.
2. O Infinito pode ser atravessado e desligado por um golpe específico, sem mexer nos outros danos.

Se algum falhar, eu paro e trago as alternativas.

## Partes
1. **Golpes trancados:** a regra em `core/`, o cadeado no HUD e o estado "desperto na luta".
2. **O boss essencial:** a parte 0, a entidade, o motor de golpes, os três golpes, a barra de boss, o item e a receita, o despertar no meio (versão simples, sem cutscene) e a vitória.
3. **O resto da luta:** Nuvem Brincalhona, Desaparecer e Execução do Assassino, as três fases, VFX, som, a entrada do Toji e a cutscene do despertar.

Cada parte termina com build, testes e uma prévia gravada; a revisão por subagente fica para o fim.

## Pontos que eu assumi
Mude o que quiser antes de aprovar:
- **O Vazio Infinito não paralisa o Toji.** Segue a obra (ele não tem energia amaldiçoada) e impede que o G decida a luta sozinho.
- **Quando o despertar dispara:** 20% de vida do Toji, 45 segundos ou golpe mortal.
- **240 de vida e os danos 8, 10 e 6.** Ponto de partida para o teste no jogo.
- **A receita** (papel, lingote de ouro e espada de ferro) e o nome do item.
- **500 de XP** pela vitória, cerca de dois níveis perto do 20.
- **Sem fala do Toji** nas partes 1 e 2; a entrada e a fala ("Nada pessoal... é só trabalho", do Minezin) ficam para a parte 3.
- **O "Exige o despertar"** da árvore continua genérico.

## Fontes
A pesquisa canônica está na nota [[Toji]]. Conferido de novo em 10/11:
- A Lança Invertida do Céu atravessa o Gojo no capítulo 71 ("Hidden Inventory, Part 7"): página do capítulo na Jujutsu Kaisen Wiki, https://jujutsu-kaisen.fandom.com/wiki/Chapter_71 (vista pelo resumo da busca; a leitura automática da wiki continua recusada).
- A Corrente de Mil Milhas e o Roxo aparecem no capítulo 75: resumo de fã, https://daddyjim.ai/jujutsu-kaisen/manga-chapter/Chapter-75 (fonte secundária).
- **Sem fonte conferida:** o domínio não afetar quem não tem energia amaldiçoada. Vem da memória da obra (Maki contra o Naoya); conferir antes de tratar como regra canônica.

## Fora deste spec
- Outros bosses, as Fendas do Vazio e o loot de boss.
- O Toji de Shibuya (com a Nuvem Brincalhona contra o Dagon) como luta separada.
