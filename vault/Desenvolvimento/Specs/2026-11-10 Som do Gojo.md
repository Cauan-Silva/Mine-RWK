---
tags: [desenvolvimento, spec]
estado: aprovado
---
# Spec: som do Gojo e pipeline de áudio (Fase 2, parte 9; fecha o item de áudio do 1.4)

Voltar para [[Roadmap]]. Hoje todo golpe do Gojo toca o mesmo som de teste (`minerwk:teste`). Este spec troca isso pelos sons próprios de cada golpe e monta o pipeline que os próximos personagens vão reaproveitar. Nada foi programado: é para o autor aprovar ou mudar.

## Proposta em uma frase
Portar o motor de síntese do Minezin (`tools/sfx_motor.py`, do próprio autor) para `ferramentas/`, gerar todos os sons do Gojo por código com os tempos tirados das constantes do Java, e tocar cada som junto com o visual que ele acompanha.

## De onde vem o áudio
| Opção | Licença | Avaliação |
| --- | --- | --- |
| **Síntese própria (recomendada)** | Nossa: o motor e os sons são do autor, sem nada de terceiros | Já funciona no Minezin (12 sons do Gojo), os tempos saem do código e mudam junto com o golpe, e não há crédito a registrar |
| Bancos prontos (Freesound CC0, Sonniss) | CC0 ou royalty-free, com registro em `THIRD_PARTY_NOTICES.md` | Som mais "gravado", mas cada arquivo precisa ser escolhido, cortado e conferido à mão, e não acompanha mudança de tempo |
| Mistura | As duas | O motor já aceita amostras (`amostra`); dá para acrescentar uma camada gravada num som específico depois, sem mudar o resto |

O motor usa `numpy` e `soundfile`, que já estão instalados nesta máquina. Ele gera três variações de cada som (tom ±4% e outra semente de ruído) e as lista juntas no `sounds.json`, para o jogo sortear e o golpe repetido não soar igual.

## Lista de sons
Tempos em ticks, tirados das constantes atuais. "Onde" diz de onde o som sai.

| Golpe | Som | Quando | Duração | Onde | Ideia do som |
| --- | --- | --- | --- | --- | --- |
| Z Azul | `gojo_azul` | disparo | 60 (3 s) | no ponto de atração | sucção grave, vento girando, sub pulsando |
| X Vermelho | `gojo_vermelho_carga` | ao segurar | até 30, corta ao soltar | no Gojo | agudo subindo com estalos |
| | `gojo_vermelho_disparo` | disparo | curto | no Gojo | estalo seco e sopro |
| | `gojo_vermelho_explosao` | impacto | ~2,5 s | no ponto do impacto | boom grave com cauda |
| C Infinito | `gojo_infinito_ativa` / `_desativa` | liga e desliga | ~1 s | no Gojo | dois toques de vidro |
| | `gojo_infinito_tic` | golpe ou flecha parados | curto | no Gojo | "tic" vítreo |
| V Roxo | `gojo_roxo_carga` | carga | 30 | no Gojo | dois tons se aproximando e batendo |
| | `gojo_roxo_disparo` | disparo | ~5 s | no Gojo | explosão abafada, sub enorme, cauda ronronando |
| G Vazio Infinito | `gojo_dominio` | carga e abertura | 10 + ~5 s | no centro | drone crescendo, baque na abertura, brilho agudo |
| | `gojo_dominio_fim` | o domínio racha | ~1,5 s | no centro | vidro grande trincando |
| R encantamento | `gojo_r_encantamento` | carga | 80 | no Gojo | hum grave crescendo, com uma batida em cada palavra (ticks 2, 22, 42 e 62) |
| R corrida | `gojo_r_avanco` | disparo | até 20 | no Gojo | sopro de ar acelerando |
| R uppercut | `gojo_r_uppercut` | soco | ~1 s | no alvo | impacto seco; os 3 ticks do impact frame ficam quase mudos, e a cauda entra depois |
| R subida | `gojo_r_subida` | depois do soco | 30 | no Gojo | vento subindo de tom |
| R conjurar e dança | `gojo_r_esferas` | no céu | 30 (8 + 10 + 12) | no centro da fusão | Azul grave à esquerda e Vermelho agudo à direita, girando cada vez mais rápido até o toque |
| R fusão | `gojo_r_fusao` | núcleo | 40 | no centro | zumbido roxo pulsando cada vez mais rápido, com **4 ticks de silêncio** no fim |
| R explosão | `gojo_r_explosao` | estouro | 60 + 50 | no centro | estalo enorme, sub distorcido, corpo rasgando por 3 s e cauda de eco por 2,5 s |

São 17 sons. A base dos de Z, X, C, V e G é a síntese que já existe no Minezin, com os tempos novos. Os sete do R são novos, porque a ordem do R mudou.

## Mudanças no jogo
1. **Som preso ao visual.** Os sons de fase (Azul, explosões, domínio e todo o R) tocam no cliente quando o visual correspondente chega, no ponto do visual. Assim o som nunca sai fora do tempo do VFX, mesmo com atraso de rede, e a explosão longe soa de longe.
2. **Som que para.** Carga interrompida ou tecla solta cedo corta o som da carga (hoje o som toca até o fim). Vale para o Vermelho, o Roxo e o encantamento do R.
3. **Sons de gesto** (disparo do Vermelho, liga e desliga do Infinito) continuam saindo pelo `ctx.apresentacao`, como hoje.
4. `Sons` e o `sounds.json` passam a ser escritos pelo gerador, com legenda em português e inglês para cada som.
5. A opção `SFX de anime` da config continua ligando e desligando tudo.

## Pipeline e testes
- `ferramentas/sfx_motor.py`: o motor portado, sem mudança de comportamento.
- `ferramentas/gerar_sfx_gojo.py`: a lista de sons e a síntese de cada um. Escreve os `.ogg`, o `sounds.json`, as legendas e um manifesto com a duração e os marcos de cada som.
- **JUnit:** um teste lê o manifesto e confere os marcos contra as constantes do Java (palavras do encantamento, silêncio antes do estouro, duração da explosão), como o `AnimacoesGojoTest` faz com as animações. É o "teste que confere ticks do som contra o Java" do item 1.4.
- **Prévia:** `--preview` gera um WAV com todos os sons em fila. Eu gero e mando para o autor ouvir antes de testar no jogo.
- **No jogo:** cada golpe com o som no tempo do visual; carga cortada parando o som; explosão do R ouvida de longe; outro jogador ouvindo; e o Sound Physics Remastered, se der.

## Divisão em partes
1. Motor, gerador, manifesto e teste, com os sons de Z, X, C, V e G.
2. "Som preso ao visual" e "som que para".
3. Os sete sons do R.

## Escolhas do autor (10/11)
1. **Fonte:** síntese própria, com o motor portado do Minezin.
2. **Voz no encantamento:** sem voz; só o hum crescendo e uma batida em cada palavra.
3. **Impact frame:** os 3 ticks do soco ficam quase mudos e o impacto entra logo depois.

## Fora deste spec
Música, sons de passo ou de ambiente, e os sons dos outros personagens.
