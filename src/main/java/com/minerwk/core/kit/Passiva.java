package com.minerwk.core.kit;

/** Habilidade sem tecla que roda todo tick enquanto o jogador tem o personagem. */
@FunctionalInterface
public interface Passiva {
    void tick(Contexto contexto, EstadoDoJogador estado);
}
