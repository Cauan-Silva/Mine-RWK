package com.minerwk.core.kit;

/** Habilidade sem tecla que roda todo tick enquanto o jogador tem o personagem. */
public interface Passiva {
    void tick(Contexto contexto, EstadoDoJogador estado);

    /**
     * Se a passiva obedece às trancas do personagem (só roda depois do despertar quando ele tranca as
     * passivas). O ataque básico, por exemplo, nunca fica trancado.
     */
    default boolean trancavel() {
        return true;
    }
}
