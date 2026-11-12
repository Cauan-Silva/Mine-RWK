# Avisos de terceiros

## Sons
| Arquivo | Origem | Licença |
| --- | --- | --- |
| `assets/minerwk/sounds/teste/teste.ogg` (era `computerNoise_000.ogg`) | Sci-Fi Sounds 1.0, Kenney (www.kenney.nl) | CC0 1.0 |
| `art/audio/cc0/bigsoundbank/*.ogg` (trechos; fonte e trecho de cada um em `art/audio/cc0/fontes.json`) | BigSoundBank.com, Joseph Sardin (https://bigsoundbank.com) | CC0 1.0 |
| `art/audio/cc0/kenney/impact*.ogg` | Impact Sounds 1.0, Kenney (www.kenney.nl) | CC0 1.0 |
| `art/audio/cc0/kenney/knifeSlice*.ogg`, `chop.ogg`, `drawKnife*.ogg`, `cloth*.ogg` | RPG Audio 1.0, Kenney (www.kenney.nl) | CC0 1.0 |
| `art/audio/cc0/kenney/explosionCrunch*`, `laserLarge*`, `laserSmall*`, `lowFrequency_explosion*`, `forceField*`, `thrusterFire*`, `spaceEngineLow*`, `slime*` | Sci-Fi Sounds 1.0, Kenney (www.kenney.nl) | CC0 1.0 |
| `assets/minerwk/sounds/{cid,gojo,ichigo,toji,ragnarok}/*.ogg` | Gerados por `ferramentas/gerar_sfx_*.py` com o motor `ferramentas/sfx_motor.py` (portado do Minezin, mesmo autor): síntese própria com as gravações CC0 acima por cima, mudadas de tom, cortadas e mixadas (`ferramentas/gravacoes.py`) | do projeto, sobre CC0 |

## Animações
| Arquivo | Origem |
| --- | --- |
| `assets/minerwk/player_animations/gojo.json` | Minezin (mesmo autor) |
| `assets/minerwk/geo/traje/ichigo_mugetsu.geo.json`, `textures/traje/ichigo_mugetsu.png` | Minezin (mesmo autor): a forma final do Mugetsu (`geo/armaduras/ichigo_mugetsu.geo.json` e a textura), sem mudança; passam por `ferramentas/gerar_traje_ichigo.py` |
| `assets/minerwk/textures/traje/ichigo_mascara.png` e a forma da máscara em `client/traje/CamadaTraje` | Minezin (mesmo autor): `textures/ichigo/mascara.png` e a geometria de `CamadaHollow` |
| `assets/minerwk/player_animations/ichigo.json` | Gerado por `ferramentas/gerar_animacoes_ichigo.py`; as poses do Getsuga, da onda e da entrada do Bankai partem das do Minezin (mesmo autor), `player_animation/ichigo.json` |
| `assets/minerwk/animations/boss/toji.animation.json` | Minezin (mesmo autor): as 14 animações do Toji (`animations/nemesis/1.animation.json`), sem mudança |

## Shaders
| Arquivo | Origem |
| --- | --- |
| `assets/minerwk/shaders/core/gojo_energy*` | Minezin (mesmo autor) |

## Modelos e texturas
| Arquivo | Origem |
| --- | --- |
| `ferramentas/traje/gojo_base.geo.json`, `ferramentas/traje/gojo_base.png` | Minezin (mesmo autor): o uniforme do Gojo |
| `assets/minerwk/geo/traje/gojo.geo.json`, `assets/minerwk/textures/traje/gojo*.png` | Gerados por `ferramentas/gerar_traje_gojo.py` a partir da base acima, com a venda e os olhos |
| `ferramentas/traje/ichigo_base.*`, `ichigo_bankai_base.*`, `zangetsu_base.geo.json`, `tensa_zangetsu_base.geo.json` | Minezin (mesmo autor): o shihakushō, o casaco do Bankai e as duas espadas (`geo/armaduras/ichigo*.geo.json`, `textures/armor/trajes/ichigo*.png`, `geo/ichigo/*.geo.json`), feitos lá por `tools/build_ichigo_assets.py` e `build_ichigo_traje.py`, sem arte de terceiros |
| `assets/minerwk/geo/traje/ichigo*.geo.json`, `assets/minerwk/textures/traje/ichigo*.png` | Gerados por `ferramentas/gerar_traje_ichigo.py` a partir das bases acima: a espada entra no traje, presa ao braço direito, com as cores dela pintadas num canto livre da textura (o atlas de cores do Minezin não é usado) |
| `assets/minerwk/geo/boss/toji.geo.json`, `assets/minerwk/textures/boss/toji.png`, `toji_glowmask.png` | Minezin (mesmo autor): o modelo e a textura do Toji (`geo/nemesis/1.geo.json`, `textures/bosses/toji_skin*.png`), sem mudança |
| `assets/minerwk/textures/item/contrato_toji.png` | Gerada por `ferramentas/gerar_item_contrato.py` |

## Código
| Arquivo | Origem |
| --- | --- |
| `core/traje/tecido/MolaManto.java`, `MalhaTecido.java`, `TecidoIchigo.java`, `TecidoEspada.java` e os testes deles | Minezin (mesmo autor): o tecido dos trajes (`MolaMantoCid`, `MalhaTecido`, `TecidoIchigo`, `TecidoEspadaIchigo`), com os nomes trocados e as mangas do Shikai acrescentadas |
| `core/vfx/GetsugaIchigo.java` | Minezin (mesmo autor): a forma da meia-lua do Getsuga (`core/vfx/GetsugaIchigo`), com os números do Mine-RWK |
