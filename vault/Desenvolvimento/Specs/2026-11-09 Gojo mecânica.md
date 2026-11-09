---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec e plano: mecânica do Gojo (Fase 2, parte 1)

Voltar para [[Roadmap]]. Kit aprovado em [[Gojo]] (09/11/2026). Decisões desta conversa: efeitos soltos, Roxo apaga blocos com gamerule, domínio paralisa mobs e jogadores.

## Objetivo
O Gojo escolhível com as seis habilidades e as duas passivas funcionando no servidor, com os números aprovados. A apresentação continua provisória (animações `gojo_*` que já existem, som de teste, partículas vanilla para enxergar os efeitos). VFX, sons, animações novas, traje e o texto do encantamento na tela ficam para as próximas partes.

Pronto quando: JUnit de cada habilidade e da energia passam; GameTests de Azul, Roxo (com e sem a regra), domínio e Infinito passam; no jogo, o autor escolhe o Gojo e usa as seis teclas.

## Desenho

### Núcleo (`core/`, sem Minecraft)
- **`Vetor`** (record x, y, z, com soma, escala, distância, normalizar).
- **`Efeito`**: algo que a habilidade deixa no mundo e que dura sozinho. `int duracao()`, `void tick(Contexto ctx, int t)`, `boolean encerrado()` (padrão falso), `void aoAcabar(Contexto ctx)`. O `KitDoJogador` guarda a lista, roda um tick de cada e remove quando acaba. Enquanto um efeito dura, o jogador pode usar outras teclas. Morrer, sair ou trocar de personagem encerra todos (`descartar`).
- **`Passiva`**: `void tick(Contexto ctx, EstadoDoJogador estado)`; o `Personagem` ganha uma lista de passivas (vazia nos que já existem).
- **`Habilidade.danoQueInterrompe()`**: padrão sem limite; se o jogador levar um golpe com pelo menos esse dano durante a carga, a execução é descartada (sem punição extra além da recarga já prevista em `atordoar`).
- **`Contexto` ganha** (o servidor implementa; tudo em volta de um ponto, sem afetar o próprio jogador):
  - `Vetor olhos()`, `Vetor olhar()`, `Vetor alvoAte(double distancia)` (primeiro bloco ou o limite).
  - `puxar(centro, raio, forca)` e `empurrar(centro, raio, forca)`: mobs, jogadores, itens e projéteis.
  - `danoEm(centro, raio, dano)`.
  - `boolean colide(de, ate)`: bloco sólido ou criatura no segmento.
  - `apagarBlocos(centro, raio)`: obedece a gamerule.
  - `paralisar(centro, raio, ticks)`.
  - `infinito(ticks)`, `curar(vida)`, `boolean gastarRecurso(qtd)`, `criarEfeito(Efeito)`, `particulas(tipo, ponto, quantidade)` (provisório).

### Gojo (`core/personagens/gojo/`)
- **`EnergiaAmaldicoada`**: "Energia Amaldiçoada", cor `0x3C8CFF`, máximo 200; enche 1,0 por tick parado e 0,4 andando.
- **Z `gojo_azul`** (instantâneo, custo 20, recarga 120): efeito de 30 ticks no ponto `alvoAte(6)`; puxa raio 5 com força 0,25 por tick; 2 de dano em raio 2 a cada 10 ticks.
- **X `gojo_vermelho`** (carga 5–30, custo 30, recarga 200): esfera que anda 1,5 bloco por tick por até 30 blocos; ao colidir (ou no fim), dano `6 + 8·força` em raio 3, empurrão `1,5 + 1,5·força` em raio 4, tremor.
- **C `gojo_infinito`** (instantâneo, custo 40, recarga 500): 120 ticks de Infinito; gasta 1,5 por tick enquanto liga e acaba se a energia acabar; empurra criaturas a até 2 blocos.
  *Ajuste ao aprovado:* a nota dizia "2 por tick", o que esgotaria os 200 de energia antes dos 6 s. Ficou 1,5 por tick; com o ganho de energia, o Infinito dura os 6 s parado e uns 4,5 s andando.
- **V `gojo_roxo`** (carga fixa de 30 ticks, custo 90, recarga 900): avança 2 blocos por tick por 40 blocos; em cada passo, apaga blocos em raio 1,5 e causa 30 de dano em raio 1,5.
- **G `gojo_vazio_infinito`** (instantâneo, custo 200, recarga 3600): domínio fixo no ponto de uso, raio 12, 160 ticks; renova a paralisia de quem está dentro a cada tick.
- **R `gojo_roxo_200`** (carga fixa de 80 ticks, custo 200, recarga 6000, `danoQueInterrompe` 6): como o Roxo, com 4 blocos por tick por 120 blocos, raio 2,5 e 60 de dano.
- **Passiva Técnica Reversa:** depois de 100 ticks sem levar dano, cura 2 de vida a cada 80 ticks.
- A passiva Seis Olhos é o próprio ganho rápido da energia.

### Servidor
- **Gamerule `minerwkApagarBlocos`** (padrão ligada). Não apaga blocos inquebráveis (dureza negativa), blocos com conteúdo (baús, fornalhas…) nem fluidos; não deixa itens.
- **Paralisia**: efeito de poção do mod (velocidade −100%, sem pulo). Com o efeito, o alvo não ataca (o dano que ele causaria é cancelado) e, se for jogador com kit, não usa teclas. Jogadores só são afetados se o PvP do servidor estiver ligado.
- **Infinito**: estado no servidor com o tick de fim. Enquanto ativo, cancela dano corpo a corpo e de projétil; projéteis que chegam a 1,5 bloco param (velocidade zero e caem).
- O `teste` continua escondido; o Gojo entra em `Personagens` e aparece na tela de escolha com o emblema `gojo.png`.
- Nomes e descrições dos golpes nas traduções (pt_br e en_us).

## Plano (execução nativa, branch `gojo-mecanica`)
1. **Núcleo:** `Vetor`, `Efeito`, `Passiva`, `danoQueInterrompe`, novos métodos do `Contexto`; `KitDoJogador` roda efeitos e passivas. JUnit: efeito dura o tempo certo e roda junto com outra habilidade; `descartar` encerra efeitos; dano na carga acima do limite interrompe e abaixo não. Verificação: `./gradlew build`.
2. **Gojo no núcleo:** energia, seis habilidades e Técnica Reversa. JUnit com um `Contexto` falso que anota as chamadas: Azul puxa 30 vezes e dá dano 3 vezes; Vermelho explode ao colidir e no limite, com dano conforme a força; Infinito acaba sem energia; Roxo e Roxo 200% dão 20 e 30 passos; domínio paralisa 160 ticks; Técnica Reversa só cura depois de 100 ticks sem dano; energia enche 1,0 e 0,4. Verificação: `./gradlew build`.
3. **Servidor:** `ContextoServidor` com os métodos novos, gamerule, Paralisia, Infinito, interrupção por dano no evento de dano, registro do Gojo e traduções. GameTests: Azul puxa um zumbi; Roxo apaga uma parede e, com a regra desligada, não apaga; domínio paralisa um zumbi; Infinito impede o dano de uma flecha. Verificação: `./gradlew build` e `runGameTestServer`.
4. **Revisão final** (modelo barato), correções, vault (Roadmap, diário, Pendências), teste do autor no jogo.

## Fora desta parte
VFX (Photon), sons, animações novas, traje, texto do encantamento na tela, teleporte. **Cutscene cinemática do Roxo 200%** (pedido do autor em 09/11): sistema de câmera reutilizável para as ultimates, desenhado na parte visual; a mecânica da carga de 80 ticks já é o tempo da cena.
