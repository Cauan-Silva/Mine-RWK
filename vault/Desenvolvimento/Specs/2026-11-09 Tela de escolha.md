---
tags: [desenvolvimento, spec]
estado: aguardando revisão
---
# Spec e plano: tela de escolha (Roadmap 1.4, parte 2)

Voltar para [[Roadmap]]. Desenho aprovado com o autor em 09/11/2026. Spec e plano no mesmo arquivo para economizar (parte pequena).

## Objetivo
Trocar a lista de botões por uma tela com grade de emblemas e painel de detalhes. Pronto quando a tela abre com o `teste` (emblema genérico) e com personagens falsos de teste mostra grade com rolagem, e os testes passam.

## Decisões
- Emblemas do Minezin como estão (autoria própria, gerados por `build_retratos_personagens.py`).
- Layout A: grade à esquerda, painel à direita, "Escolher" abre a confirmação atual.

## Desenho
- **Arquivos de arte:** `assets/minerwk/textures/gui/personagens/<id>.png` (96×128) e `_generico.png`; script em `tools/build_retratos_personagens.py` com saída ajustada para `minerwk`, nota em `tools/README.md`. Emblema ausente → genérico.
- **Rede:** em `SincronizarKit`, `List<String> escolhiveis` vira `List<PersonagemEscolha>`; `record PersonagemEscolha(String id, String nome, String obra, List<GolpeEscolha> golpes)` e `record GolpeEscolha(Tecla tecla, String id)`.
- **Textos dos golpes:** chaves de tradução `habilidade.minerwk.<id>` e `habilidade.minerwk.<id>.desc` (pt_br e en_us). Para o `teste`: "Golpe de teste" / "Golpe instantâneo em área curta." e "Carga de teste" / "Segure para carregar; mais força, mais dano e tremor.".
- **`LayoutEscolha`** (`core/hud/`, puro): dado largura e altura da tela e número de personagens, calcula colunas da grade (emblema 32×43, espaço 6), linhas visíveis, rolagem máxima, retângulo do painel (direita; abaixo da grade se a largura < 360) e `int indiceEm(x, y, rolagem)` (−1 fora).
- **`TelaEscolha`:** grade à esquerda com destaque ao passar o mouse e moldura no selecionado; rolagem com a roda do mouse; painel com emblema grande (96×128, metade se não couber), nome, obra, golpes Z…R (nome + descrição em uma linha, cortada com "…" se passar da largura), botão "Escolher" → confirmação atual. Abre com o primeiro selecionado.

## Plano (execução nativa, branch `tela-escolha`)
1. **Arte e script:** copiar os 15 PNGs e o script; ajustar caminho de saída; `tools/README.md`. Verificação: os arquivos existem.
2. **`LayoutEscolha` (TDD):** testes `colunasCabemNaLargura` (largura 400 → colunas que cabem na metade esquerda), `rolagemSoQuandoNaoCabe` (3 personagens → rolagem 0; 40 → > 0), `indiceEmAcertaCelula` (clique no centro da 2ª célula → 1; no espaço entre células → −1; com rolagem de uma linha → índice + colunas), `painelVaiParaBaixoEmTelaEstreita` (largura 300). Depois implementar.
3. **Rede:** `PersonagemEscolha`/`GolpeEscolha` com codecs; `Sincronizacao` preenche; `RecepcaoKit` e `TelaEscolha` passam a usar. Verificação: `./gradlew build` e `runGameTestServer`.
4. **Tela:** reescrever `TelaEscolha` com `LayoutEscolha`, emblemas e textos de tradução. Verificação: build; no jogo, mundo novo.
5. **Revisão final** (modelo barato), correções, vault (Roadmap, diário, Pendências).

## Fora desta parte
Emblemas novos ou com rosto; animação na tela; som de seleção.
