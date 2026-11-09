package com.minerwk.core.personagens.gojo;

import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Passiva;

/** Passiva: depois de 100 ticks sem levar dano, cura 2 de vida (1 coração) a cada 80 ticks. */
public final class TecnicaReversa implements Passiva {
    static final int ESPERA = 100;
    static final int INTERVALO = 80;
    static final float CURA = 2f;

    private int semDano;

    @Override
    public void tick(Contexto contexto, EstadoDoJogador estado) {
        if (estado.apanhou()) {
            semDano = 0;
            return;
        }
        semDano++;
        if (semDano >= ESPERA && (semDano - ESPERA) % INTERVALO == 0) contexto.curar(CURA);
    }
}
