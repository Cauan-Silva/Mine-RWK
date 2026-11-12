package com.minerwk.core.boss;

import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.camera.Coreografia.Quadro;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200.Palavra;
import java.util.List;

/**
 * A cutscene do meio da luta contra o Zangetsu interior: o Ichigo cai, a câmera fecha no rosto e, quando ele
 * se levanta, a máscara Hollow já está nele. Posições na base de quem caiu, a partir dos olhos: (lado, cima,
 * frente), com a frente apontando para o Zangetsu.
 */
public final class CutsceneMascara {
    public static final String ID = "zangetsu_mascara";
    /** Tick em que ele se levanta: igual a {@link ZangetsuBoss#QUEDA}. */
    public static final int LEVANTA = ZangetsuBoss.QUEDA;
    /** Ticks de cutscene depois de se levantar; o Zangetsu espera esse tempo antes de voltar a atacar. */
    public static final int DEPOIS = 24;
    /** Animação de quem caiu (a mesma queda do despertar do Gojo) e a do gesto de vestir a máscara. */
    public static final String ANIM_QUEDA = CutsceneDespertar.ANIM_QUEDA, ANIM_MASCARA = "ichigo_mascara";

    public static final Coreografia COREOGRAFIA = new Coreografia(List.of(
            // De lado e de cima, o corpo no chão e o Zangetsu ao fundo.
            new Quadro(0, new Vetor(2.6, 2.8, -1.8), new Vetor(0, -.2, .8), 1, 6),
            new Quadro(34, new Vetor(1.2, .9, -1.2), new Vetor(0, 0, .1), .9, 0),
            // Fechando no rosto.
            new Quadro(LEVANTA - 2, new Vetor(.5, .3, 1.0), new Vetor(0, .05, 0), .75, -5),
            // Corte: de pé, de frente, a mão passando pelo rosto.
            new Quadro(LEVANTA, new Vetor(-.5, .05, 1.9), new Vetor(0, .05, 0), .85, 4, true),
            new Quadro(LEVANTA + DEPOIS, new Vetor(-.9, -.4, 2.6), new Vetor(0, .1, 0), 1, 0)));

    public static final List<Palavra> PALAVRAS = List.of(
            new Palavra(LEVANTA + 2, LEVANTA + DEPOIS - 2, "cutscene.minerwk.zangetsu_mascara.1"));

    private CutsceneMascara() {
    }
}
