---
tags: [diario]
---
# 2026-11-09 Tela de escolha

Voltar para [[Diário]].

- Parte 2 do 1.4. Emblemas do Minezin (96×128, autoria própria) e o script gerador vieram para `textures/gui/personagens/` e `tools/`.
- Layout: grade de emblemas à esquerda, painel à direita (embaixo se a tela tiver menos de 360 de largura). Contas em `core/hud/LayoutEscolha` (JUnit).
- A lista de personagens agora vai pela rede com nome, obra e golpes (`PersonagemEscolha`), no lugar do texto "id|nome|obra". Nomes e descrições dos golpes ficam nas traduções (`habilidade.minerwk.<id>` e `.desc`).
- Comando `/rwk tela <quantos>` abre a tela com personagens falsos, para ver a rolagem.
- Esboço da tela feito direto na conversa. Revisão final com modelo mais barato.
