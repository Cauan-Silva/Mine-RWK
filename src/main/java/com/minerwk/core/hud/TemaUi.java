package com.minerwk.core.hud;

/**
 * O tema de interface de um personagem (ou de uma forma dele): as cores da HUD e da árvore, o estilo da moldura
 * dos quadros e o fundo da árvore ({@code textures/gui/fundos/<fundoArvore>.png}). Cores em ARGB.
 */
public record TemaUi(int corPrincipal, int corSecundaria, int corFundo, int corBorda, Moldura moldura,
                     String fundoArvore) {
    public enum Moldura {
        /** Borda fina e lisa. */
        LISA,
        /** Cantos cortados em lâmina. */
        CORTADA,
        /** Cantos com um ornamento na cor secundária. */
        ORNADA
    }
}
