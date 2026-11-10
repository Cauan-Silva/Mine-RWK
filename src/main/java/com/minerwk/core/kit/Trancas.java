package com.minerwk.core.kit;

import java.util.Set;

/**
 * O que um personagem só pode usar depois de despertar.
 *
 * @param teclas   as habilidades trancadas, pela tecla
 * @param passivas se as passivas também só rodam depois do despertar
 */
public record Trancas(Set<Tecla> teclas, boolean passivas) {
    public static final Trancas NENHUMA = new Trancas(Set.of(), false);

    public Trancas {
        teclas = Set.copyOf(teclas);
    }
}
