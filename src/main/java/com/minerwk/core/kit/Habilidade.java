package com.minerwk.core.kit;

/** Uma habilidade de um personagem. Os ganchos rodam no servidor. */
public interface Habilidade {
    String id();

    Numeros numeros();

    default void aoIniciarCarga(Contexto contexto) {
    }

    /** @param forca de 0 a 1; habilidades instantâneas sempre recebem 1 */
    default void aoDisparar(Contexto contexto, float forca) {
    }

    default void aoTerminar(Contexto contexto) {
    }
}
