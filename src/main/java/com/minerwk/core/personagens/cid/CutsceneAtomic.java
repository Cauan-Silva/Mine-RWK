package com.minerwk.core.personagens.cid;

import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.camera.Coreografia.Quadro;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200.Palavra;
import java.util.List;

/**
 * A cutscene do R do Cid, em quatro trechos, cada um mandado pelo servidor na sua hora (o golpe muda de
 * dimensão no meio, e a sequência pode pular para o Atomic se o alvo morre): a subida a socos, o espaço com
 * as três palavras, o recuo da câmera com o clarão roxo e a luz no céu vista do chão. Posições na base do
 * Cid a partir dos olhos: (lado, cima, frente), a frente virada para o alvo.
 */
public final class CutsceneAtomic {
    public static final String SUBIDA = "cid_atomic_subida", ESPACO = "cid_atomic_espaco", CLARAO = "cid_atomic_clarao",
            CEU = "cid_atomic_ceu_visto";
    /** Quanto o último plano de um trecho espera o seguinte (a rede, a troca de dimensão). */
    private static final int ESPERA = 12;
    /** Do lado, longe: os dois pequenos, o chão sumindo embaixo. */
    private static final Vetor MEIO = new Vetor(0, 0, AtomicFinal.AO_LADO / 2);

    /** As três palavras, contadas da chegada ao espaço. */
    public static final List<Palavra> PALAVRAS = palavras();

    private CutsceneAtomic() {
    }

    private static List<Palavra> palavras() {
        int inicio = AtomicFinal.FALA - AtomicFinal.PASSAGEM, cada = AtomicFinal.TICKS_DA_FALA / 3;
        return List.of(new Palavra(inicio, inicio + cada, "cutscene.minerwk.cid_atomic.1"),
                new Palavra(inicio + cada, inicio + 2 * cada, "cutscene.minerwk.cid_atomic.2"),
                new Palavra(inicio + 2 * cada, inicio + 3 * cada, "cutscene.minerwk.cid_atomic.3"));
    }

    /** O primeiro soco de perto; depois de lado, de longe e um pouco de baixo, com os dois subindo. */
    public static Coreografia daSubida() {
        return new Coreografia(List.of(
                new Quadro(0, new Vetor(1.5, -.2, -.7), new Vetor(0, -.2, 2), .9, 4),
                new Quadro(3, new Vetor(1.3, -.3, -.4), new Vetor(0, .6, 2), .95, 2),
                new Quadro(4, new Vetor(9, -1, 1), MEIO.mais(new Vetor(0, 1.5, 0)), 1.1, 0, true),
                new Quadro(AtomicFinal.PASSAGEM, new Vetor(10, -4, 1), MEIO.mais(new Vetor(0, 2, 0)), 1.15, 0),
                new Quadro(AtomicFinal.PASSAGEM + ESPERA, new Vetor(10, -4, 1), MEIO.mais(new Vetor(0, 7, 0)), 1.15, 0)));
    }

    /**
     * A chegada em plano aberto, com o planeta embaixo; os socos de lado; o rosto no capuz nas três palavras,
     * visto de frente e de lado (o alvo está a um passo dele, e de frente taparia o quadro).
     */
    public static Coreografia doEspaco() {
        int fala = AtomicFinal.FALA - AtomicFinal.PASSAGEM, fim = AtomicFinal.ATOMIC - AtomicFinal.PASSAGEM;
        return new Coreografia(List.of(
                new Quadro(0, new Vetor(10, 5, -3), new Vetor(0, -.6, 1), 1.1, 0),
                new Quadro(fala - 2, new Vetor(6, 1.5, 1), new Vetor(0, -.4, 1), 1, 0),
                new Quadro(fala, new Vetor(1.9, .15, 1.3), new Vetor(0, -.1, -.3), .75, -3, true),
                new Quadro(fim, new Vetor(1.15, .05, .75), new Vetor(0, 0, -.2), .65, 0),
                new Quadro(fim + ESPERA, new Vetor(1.15, .05, .75), new Vetor(0, 0, -.2), .65, 0)));
    }

    /** A câmera se afasta de uma vez, até os dois virarem um ponto entre os planetas, e fica. */
    public static Coreografia doClarao() {
        Vetor centro = new Vetor(0, 0, AtomicFinal.AO_LADO / 2), longe = new Vetor(40, 25, -105);
        return new Coreografia(List.of(
                new Quadro(0, new Vetor(3, 1, -4), centro, .9, 0),
                new Quadro(7, longe, centro, 1, 0),
                new Quadro(AtomicFinal.TICKS_DO_CLARAO + ESPERA, longe, centro, 1, 0)));
    }

    /** De volta ao chão: o céu, onde a luz ainda brilha, e a câmera desce para trás dele (dentro da cabeça veria o capuz por dentro). */
    public static Coreografia doCeu() {
        int fim = AtomicFinal.DEPOIS_DA_VOLTA;
        return new Coreografia(List.of(
                new Quadro(0, new Vetor(0, .6, -3.2), new Vetor(0, 10, 3), 1, 0),
                new Quadro(fim - 6, new Vetor(0, .6, -3.2), new Vetor(0, 9, 4), 1, 0),
                new Quadro(fim, new Vetor(0, .4, -3), new Vetor(0, 0, 5), 1, 0)));
    }
}
