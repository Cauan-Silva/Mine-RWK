package com.minerwk.core.personagens.gojo;

import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Passiva;
import com.minerwk.core.maestria.Melhorias;

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
        int intervalo = Melhorias.escala(INTERVALO, contexto.melhorias().menos(ArvoreGojo.TECNICA_REVERSA));
        if (semDano >= ESPERA && (semDano - ESPERA) % intervalo == 0) contexto.curar(CURA);
    }
}
