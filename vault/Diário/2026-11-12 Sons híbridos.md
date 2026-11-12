# 2026-11-12 Sons híbridos: gravações CC0 + síntese (piloto no Ragnarok)

O autor achou todos os sons crus (só síntese) e escolheu o caminho híbrido: gravações reais de domínio público dão o corpo, e a síntese reforça os graves e faz a magia. O piloto refez os onze sons do Ragnarok; os outros personagens seguem depois que o autor aprovar o resultado.

## O que entrou
- **51 trechos de gravação** em `art/audio/cc0/`: 29 do BigSoundBank (Joseph Sardin, CC0) e 22 do Kenney (Impact Sounds, RPG Audio e Sci-Fi Sounds, CC0). Rosnados de zumbi, mugido e rugido de gato (descidos de tom viram a garganta de um demônio), fogo de gasolina e de galhos, explosões, trovão, pedras caindo, socos, whooshes, vento, bandeira e pombo (as asas), espada e faca. `fontes.json` guarda de onde veio cada um, o trecho cortado e a licença.
- **O motor já tinha a camada** `Som.amostra` (veio do Minezin e nunca tinha sido usada aqui): pega a gravação, muda o tom, corta e mistura. As três variações de cada som sorteiam gravações diferentes.
- **Os sons do Ragnarok refeitos** (`ferramentas/gerar_sfx_ragnarok.py`): o rugido é um rosnado gravado bem grave com um mugido por baixo; o soco, o pisão e a morte usam explosão, baque e pedras de verdade; o sopro é o jato de um propulsor com a chama e a garganta; as asas são pano batendo; o corte é a espada e a faca gravadas.
- **Teste** `GravacoesCc0Test`: toda gravação tem fonte e licença CC0 no `fontes.json`, e cada autor aparece no `THIRD_PARTY_NOTICES.md`.

## Decisões minhas, para o autor conferir
- **Fontes só sem conta:** Kenney e BigSoundBank. O Freesound tem muito mais, mas exige login.
- **Seis gravações o BigSoundBank recusou baixar** (maçarico, bandeira longa, porcos, respiração de cavalo). Ficaram de fora, sem insistir.
- **Só os trechos usados entram no repositório** (1,1 MB), não os pacotes inteiros.

Prévia para ouvir: `previas/sfx_ragnarok_hibrido.wav` (a anterior, só síntese, é `previas/sfx_ragnarok.wav`).

## Revisão (sonnet, só leitura)
Nada crítico nem importante. Corrigido: uma gravação (a espada cortando o ar) estava nos créditos sem ser usada; agora entra no corte da asa. Menor, anotado: o teste confere licença e créditos, mas não que o gerador use cada gravação.
