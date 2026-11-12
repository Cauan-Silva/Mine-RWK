---
tags: [diario]
---
# 11/11/2026: Ichigo, parte 6, etapa 2 (apresentação da máscara, do Mugetsu e dos poderes)

Voltar para [[Diário]]. Spec: [[2026-11-11 Ichigo]]. Branch `ichigo-kit`. Vem depois de [[2026-11-11 Ichigo parte 6 mecânica]]. Com esta etapa a parte 6 está completa.

## O que foi feito
- **Sons (6 novos, 17 no total):** a máscara se formando (osso e uma voz oca com eco) e quebrando; a carga do Mugetsu (o reiatsu preto sobe, a forma final fecha com um baque e tudo murcha até o silêncio completo); o corte (um estalo de lâmina, um grave enorme e a escuridão rolando); o reiatsu indo embora e voltando.
- **Máscara Hollow no rosto:** a forma e a textura são as do Minezin (mesmo autor), presas à cabeça. Aparece no fim do gesto e fica até quebrar; lascas de osso convergem quando ela se forma e cacos caem quando se desfaz.
- **Forma final do Mugetsu:** traje `ichigo_mugetsu` (modelo e textura do Minezin), sem espada. Entra na transformação, aos 1,5 s da carga, e fica até o véu se desfazer.
- **Visuais do Mugetsu:** fumaça de reiatsu preto que incha até a transformação e murcha até o silêncio (tinta opaca, sem luz); o véu, uma parede de escuridão de 9 blocos que sobe do chão e avança os 40 blocos, desenhada em cortinas com a frente e as laterais fechadas; o céu meio escuro desde a transformação e todo escuro no corte.
- **Cutscene** (`core/personagens/ichigo/CutsceneMugetsu`, no sistema da do Gojo), uma parte só, 8,1 s: a volta em torno dele com a fumaça subindo; corte seco para baixo quando a forma final aparece; o rosto, fechando, com "O Getsuga Tenshō Final" (最後の月牙天衝); no silêncio, atrás do ombro, com "Mugetsu" (無月); no corte, plano aberto de lado e do alto para ver o véu avançar; e a volta ao Ichigo.
- **Animações:** `ichigo_mascara` (a mão livre passa pelo rosto) e `ichigo_mugetsu` (o corpo se abre na subida do reiatsu, fica reto na forma final, o braço sobe devagar no silêncio e desce de uma vez no corte).
- **HUD:** o aviso "Sem poderes: N s" em cima dos quadros enquanto não há reiatsu e todos os golpes estão em recarga longa.
- **Perda e volta dos poderes:** um efeito avisa o reiatsu indo embora 1,5 s depois do corte e voltando aos 60 s, com visual e som onde o Ichigo estiver.
- **Na cutscene o nome do jogador não aparece** mais por cima do rosto (vale também para as do [[Gojo]]).

## Como foi conferido
Além dos testes, a prévia gravada (`./gradlew runClientDemo -Pdemo=ichigo_mugetsu`) foi rodada três vezes e os quadros foram olhados um a um: a máscara no rosto, a fumaça, a forma final, os planos da cutscene com as palavras, o véu no plano aberto, o céu escuro, o HUD com os golpes em 60 s e o aviso. Folhas de quadros em `previas/ichigo_mugetsu.png` e `previas/ichigo_mugetsu_cutscene.png`. Não há vídeo: este PC não tem o ffmpeg.

O que a primeira gravação mostrou e foi corrigido: o véu parecia vidro escuro (ficou mais opaco e com as cortinas mais juntas) e o nome "Dev" cobria o rosto nos closes.

## Decisões
- **A forma final vem dos visuais do golpe**, não de uma forma do kit: dura só a carga e o corte, e o kit continua com Shikai e Bankai.
- **A cutscene é uma parte só** e quem a corta é o próprio golpe, se for interrompido. O R do Gojo usa duas partes porque o alvo decide o caminho; aqui não há alvo.
- **O plano do véu fica de lado e do alto, fora da faixa:** de dentro do véu não se vê nada.
- **Um som só para a carga inteira do Mugetsu**, com os três tempos dentro (transformação, silêncio, corte), cortado se o golpe for interrompido.
- **O aviso de "sem poderes" sai do estado do kit** (sem recurso e todos os golpes com mais de 10 s de recarga), então continua valendo depois de sair e entrar no jogo.

## Testes
- JUnit novos: `MugetsuIchigoTest` (7), `CutsceneMugetsuTest` (5), 3 em `TrajeIchigoTest`, 4 em `IchigoTest`, 1 em `LogicaHudTest` e os de `SonsIchigoTest` ampliados para os 17 sons. GameTests: 73 passam.
- As curvas do véu e a cutscene foram escritas junto com os testes delas, não antes: os testes passaram na primeira rodada. Os demais falharam antes do código.

## Falta
- **O autor ver e ouvir** (roteiro em [[Pendências]]): nada disto foi visto em movimento por alguém, só em quadros parados; e ninguém ouviu os sons.
- O cabelo longo da forma final quase não aparece: o modelo do Minezin cobre o pescoço e o corpo, e a cabeça continua a da skin do jogador.
- As faixas da forma final (`mg_faixa_*`) ficam paradas: o tecido só anima os ossos `bk_*`.
- O aviso "Sem poderes" encosta nas mensagens que o jogo põe em cima da barra de itens.
- A lâmina preta na mão não é desenhada (só a fumaça).
- Próximo no Ichigo: parte 7, o boss Zangetsu interior, que libera a máscara e o Mugetsu sem o comando.
