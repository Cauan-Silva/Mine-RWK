package com.minerwk.core.boss;

import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.camera.Coreografia.Quadro;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200.Palavra;
import java.util.List;

/**
 * A cena da asa, quando o Ragnarok passa da metade da vida: a câmera abre nele, o Shadow passa num risco preto
 * e a asa esquerda cai; depois um plano baixo de frente, com ele urrando. Quem invocou assiste; a base é a dos
 * olhos do Ragnarok: (lado, cima, frente), com a frente apontando para quem ele encara; o lado negativo é o da
 * asa esquerda dele (que, para quem o encara, fica à direita). Dura {@link RagnarokBoss#TROCA_DE_FASE}, o tempo em que ele não ataca.
 */
public final class CutsceneAsa {
    public static final String ID = "ragnarok_asa";
    /** Tick em que o corte passa pela asa. */
    public static final int CORTE = 8;
    /** Tick do plano de frente, com o urro. */
    public static final int URRO = 34;
    /** Onde a asa esquerda fica em relação aos pés dele, em blocos: (lado, cima, frente). */
    public static final Vetor ASA = new Vetor(-2.1, 4.9, -.7);
    /** Tick em que a asa chega ao chão e some do corpo. */
    public static final int ASA_NO_CHAO = RagnarokBoss.ASA_CAI;

    public static final Coreografia COREOGRAFIA = new Coreografia(List.of(
            // De frente e do lado da asa, longe: o corpo inteiro no quadro.
            new Quadro(0, new Vetor(-6.5, -1.5, 10.5), new Vetor(-1.5, -1.5, 0), 1, 0),
            new Quadro(CORTE - 2, new Vetor(-5.5, -1, 9), new Vetor(-1.8, -1, -.5), .9, 0),
            // O corte: um tranco.
            new Quadro(CORTE + 1, new Vetor(-5.2, -1.1, 8.6), new Vetor(-1.9, -1.2, -.5), .86, 4),
            // Acompanha a asa caindo.
            new Quadro(ASA_NO_CHAO + 4, new Vetor(-6, -3.2, 8.5), new Vetor(-2.6, -4, 0), .92, 0),
            // Corte: baixo e de frente, ele urrando por cima da câmera.
            new Quadro(URRO, new Vetor(3.5, -5, 7), new Vetor(0, -.2, 0), 1, -3, true),
            new Quadro(RagnarokBoss.TROCA_DE_FASE, new Vetor(3.2, -4.8, 9.5), new Vetor(0, .4, 0), 1.06, 0)));

    public static final List<Palavra> PALAVRAS = List.of(
            new Palavra(URRO + 2, RagnarokBoss.TROCA_DE_FASE - 2, "cutscene.minerwk.ragnarok_asa.1"));

    private CutsceneAsa() {
    }
}
