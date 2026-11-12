---
tags: [diario]
---
# 11/11/2026: Ichigo, parte 6, etapa 1 (mecânica da máscara, do Mugetsu e da perda dos poderes)

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]]. Branch `ichigo-kit`. Vem depois de [[2026-11-11 Ichigo parte 5]].

A parte 6 é grande, então foi dividida em duas etapas. Esta é a **mecânica**, com apresentação provisória: os golpes funcionam no servidor e mandam os visuais, mas o cliente ainda não desenha nada deles. A etapa 2 é a apresentação.

## O que foi feito
- **Máscara Hollow (V do Bankai, trancada):** 8 ticks de gesto, depois 11 s de máscara. Enquanto dura: todo dano causado é 30% maior (golpes e ataque básico, inclusive com a lâmina carregada), o Getsuga negro fica 50% mais largo e o Bankai para de drenar. Acaba antes se ele sair do Bankai. Custo 40, recarga 40 s.
- **Mugetsu (R do Bankai, trancado):** pede 100 de reiatsu. A carga de 6 s é a cutscene; durante ela o reiatsu não escorre. No corte gasta **todo** o reiatsu e solta o véu: 9 blocos de largura, avança 40 blocos em 1 s, 40 de dano sem contar a armadura, uma vez por inimigo. Atravessa paredes e não quebra nada. Um golpe de 6 ou mais de dano durante a carga corta o Mugetsu sem gastar nada, como no R do [[Gojo]]. Recarga de 180 s contada do corte.
- **Perda dos poderes:** no corte o Ichigo volta ao Shikai, o reiatsu vai a zero e fica **selado** por 60 s (não regenera), e os cinco golpes do Shikai entram em recarga de 60 s. Sobra o ataque básico da Zangetsu. O HUD já mostra o tempo que falta em cada golpe, pelas recargas.
- **Trancas:** a máscara e o Mugetsu só começam para quem despertou. Até o boss existir (parte 7), `/rwk despertar <jogador> sim`.

## Como foi feito
- **A máscara é um efeito**, não uma terceira forma: os golpes perguntam ao contexto se o efeito está ativo (`Contexto.efeitoAtivo`). Assim o Bankai continua com uma lista só de golpes e nenhum id se repete.
- **Sinais ao recurso** (`Recurso.sinal`, `Contexto.sinalAoRecurso`): o kit avisa o Reiatsu do [[Ichigo]] com um nome e um tempo. `sem_dreno` pausa o dreno do Bankai; `selar` zera e tranca o reiatsu.
- **O selo é salvo:** o valor que vai para o disco é o reiatsu ou, selado, os ticks que faltam com o sinal trocado. Sair e entrar no jogo não devolve os poderes. As recargas dos golpes já eram salvas.
- **Dano que ignora armadura:** tipo de dano novo `minerwk:direto`, na tag `bypasses_armor` do jogo, e `Contexto.danoDiretoUmaVez`.
- **`Contexto.golpeEmAndamento`:** a passiva que encerra o Bankai sem reiatsu não age no meio do Mugetsu (ele gasta tudo e ele mesmo tira do Bankai).

## Decisões
- **A perda dos poderes usa as recargas e o selo, não uma forma "sem poderes".** O spec pede o HUD com os golpes apagados e o tempo que falta: as recargas já fazem isso e já são salvas.
- **O véu atravessa paredes.** O spec diz que não quebra blocos; não diz se para neles. É o golpe final e custa os poderes: esconder-se atrás de um muro não salva.
- **O dano do véu é medido no corte**, antes de os poderes irem embora: com a máscara no rosto são 52.
- **A pausa do dreno vale desde o tick em que é pedida** (o reiatsu devolve o que drenou naquele tick). Sem isso, quem usasse o Mugetsu com 100 exatos perderia o corte por 0,1 de reiatsu.
- **O gesto da máscara tem 8 ticks** (o spec não dava o tempo).
- **Sem animação por enquanto** nos dois golpes novos: elas entram com a apresentação.

## Testes
- JUnit: 17 novos em `IchigoTest` (trancas; duração, dreno, dano e largura da máscara; sair do Bankai desfaz a máscara; os 100 de reiatsu; o véu de 9 por 40, quem fica fora dele, atravessar paredes; a carga sem dreno e o gasto total; os 60 s sem poderes e a volta; a recarga de 180 s; reiatsu exato; golpe forte corta e golpe fraco não; Mugetsu com a máscara) e 7 em `ReiatsuTest` (pausa do dreno, selo, selo salvo, valor absurdo). 372 passam.
- GameTest: 4 novos em `TestesIchigo`. O mais importante: um golem com armadura 30 absorve quase todo o Getsuga negro e perde exatamente 40 para o Mugetsu. 73 passam.

## Falta (etapa 2: apresentação)
- A máscara no rosto (modelo e textura) e o gesto.
- A forma final do Mugetsu (cabelo longo, faixas, reiatsu preto) e a cutscene com "Mugetsu" (無月) na tela.
- O véu preto subindo do chão e o céu escurecendo.
- O aviso de "sem poderes" no HUD (hoje só as recargas aparecem).
- As animações do jogador e os sons (máscara, Mugetsu, perda e volta dos poderes).
- A prévia em vídeo.
