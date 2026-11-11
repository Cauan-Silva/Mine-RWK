package com.minerwk.core.arena;

/** A opacidade da tela preta que esconde a ida e a volta da arena. */
public final class CurvaEscurecer {
    private CurvaEscurecer() {
    }

    /**
     * De 0 (clara) a 1 (preta) no {@code tick}: sobe em linha reta por {@code escurecendo} ticks, fica preta
     * por {@code preto} e desce em linha reta por {@code clareando}. Fora desse tempo, 0.
     */
    public static double opacidade(double tick, int escurecendo, int preto, int clareando) {
        if (tick < 0) return 0;
        if (tick < escurecendo) return tick / escurecendo;
        double depois = tick - escurecendo - preto;
        if (depois <= 0) return 1;
        return depois >= clareando ? 0 : 1 - depois / clareando;
    }

    /** Quantos ticks o efeito dura. */
    public static int duracao(int escurecendo, int preto, int clareando) {
        return escurecendo + preto + clareando;
    }
}
