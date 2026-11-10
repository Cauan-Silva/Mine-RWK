package com.minerwk.core.arena;

import java.util.BitSet;

/** Quais vagas da dimensão da arena estão ocupadas. Não é salvo: ao ligar o servidor todas estão livres. */
public final class VagasArena {
    private final BitSet ocupadas = new BitSet();

    /** Ocupa e devolve a menor vaga livre. */
    public int reservar() {
        int indice = ocupadas.nextClearBit(0);
        ocupadas.set(indice);
        return indice;
    }

    public void liberar(int indice) {
        if (indice >= 0) ocupadas.clear(indice);
    }

    public boolean ocupada(int indice) {
        return indice >= 0 && ocupadas.get(indice);
    }

    public void limpar() {
        ocupadas.clear();
    }
}
