# 2026-11-12 Sons híbridos em todos os personagens

Depois do piloto do Ragnarok ([[2026-11-12 Sons híbridos]]), o autor pediu o mesmo para os outros. Os 70 sons do Cid, do Ichigo (e do Zangetsu, que usa os dele), do Gojo e do Toji agora têm gravações CC0 por cima da síntese.

## Como ficou
- **Módulo comum** `ferramentas/gravacoes.py`: as gravações em grupos (murros, baques, explosões, pedras, whooshes, lâminas, correntes, vidro, slime, gongo, choque elétrico...) e o `hibrido()`, que soma a síntese de antes (a 60%) com as camadas gravadas e um pouco de sala.
- **Cada gerador** ganhou uma função `gravado(nome, s)` com as camadas reais nos mesmos tempos da síntese. A síntese antiga ficou intacta como `sintetico`, então os tempos conferidos contra o Java não mudaram (os testes de som passam).
- **O que ganhou gravação:**
  - **Cid:** a lâmina e o slime (gravações de slime e lama), as correntes de verdade, os socos do Atomic, e explosão e trovão no I Am Atomic.
  - **Ichigo:** o aço, o ar do Getsuga e a explosão no fim do voo, a terra da onda, o osso da máscara e o vidro quando ela quebra, a garganta do Hollow e o Cero.
  - **Gojo:** só o que é físico (o ar, os golpes do R, as explosões, o gongo do domínio e do encantamento, o vidro quando o domínio racha). Os tons do Azul, do Vermelho, do Roxo e do Infinito continuam sintetizados, porque são a cara deles.
  - **Toji:** quase tudo, porque ele é todo físico (lâmina, corrente, murros, a Nuvem batendo).
- **Sem gravação** (só síntese, de propósito): a aura do I Am Atomic (o silêncio), o reiatsu indo e voltando, o Infinito, as esferas do R e o despertar.
- **20 gravações novas** do BigSoundBank (correntes, vidro, espelho, faca, choque, solda, ossos, gelo, gongo, lama, respingo, whooshes) e mais arquivos dos pacotes do Kenney (metal, vidro, madeira, picareta, slime, laser, pano, sacar a faca). Duas o site recusou (pedra de afiar, tambor).
- **`ferramentas/conferir_gravacoes.py`:** roda todos os geradores e lista as gravações sem uso e as que faltam no `fontes.json`. Três sobravam e saíram (bigorna, chicote, solda curta).

## Para o autor ouvir
`previas/sfx_cid_hibrido.wav`, `sfx_ichigo_hibrido.wav`, `sfx_gojo_hibrido.wav`, `sfx_toji_hibrido.wav` e `sfx_ragnarok_hibrido.wav`.

## Revisão (sonnet, só leitura)
Nada crítico nem importante: nenhuma colisão de nomes entre as constantes de tempo dos geradores e as listas de gravações, nenhum início fora da duração do som, e os créditos cobrem todos os arquivos.
