package com.minerwk.core.boss;

import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.camera.Coreografia.Quadro;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200.Palavra;
import java.util.List;

/**
 * A cutscene do despertar no meio da luta contra o Toji: os 3 segundos da queda vistos de cima, fechando
 * no Gojo caído, e depois dele se levantar, de baixo e por trás, com o Toji à frente. Posições na base de
 * quem caiu, a partir dos olhos: (lado, cima, frente), com a frente apontando para o Toji.
 */
public final class CutsceneDespertar {
    public static final String ID = "toji_despertar";
    /** Tick em que ele se levanta: igual a {@link TojiBoss#QUEDA}. */
    public static final int LEVANTA = TojiBoss.QUEDA;
    /** Ticks de cutscene depois de se levantar; o Toji espera esse tempo antes de voltar a atacar. */
    public static final int DEPOIS = 25;

    public static final Coreografia COREOGRAFIA = new Coreografia(List.of(
            // De cima, quase a pino, o corpo no chão.
            new Quadro(0, new Vetor(.3, 4.2, -.6), new Vetor(0, 0, .2), 1, 8),
            new Quadro(30, new Vetor(1.4, 1.6, -1.6), new Vetor(0, 0, .1), .95, 0),
            // Fechando no rosto.
            new Quadro(LEVANTA - 2, new Vetor(.7, .5, -1.1), new Vetor(0, .05, 0), .8, -4),
            // Corte: de pé, de baixo e por trás, o Toji à frente.
            new Quadro(LEVANTA, new Vetor(.9, -1.1, -2.4), new Vetor(0, .1, 3), 1.1, 0, true),
            new Quadro(LEVANTA + DEPOIS, new Vetor(.5, -.7, -1.7), new Vetor(0, .15, 3), 1, 0)));

    public static final List<Palavra> PALAVRAS = List.of(
            new Palavra(LEVANTA + 2, LEVANTA + DEPOIS - 2, "cutscene.minerwk.toji_despertar.1"));

    private CutsceneDespertar() {
    }
}
