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
        // "I" e "am" com um silêncio depois de cada uma; "atomic" só depois da carga, até o estouro.
        int inicio = AtomicFinal.FALA - AtomicFinal.PASSAGEM, dura = AtomicFinal.TICKS_DA_PALAVRA;
        int segunda = inicio + AtomicFinal.ENTRE_PALAVRAS, terceira = AtomicFinal.TERCEIRA - AtomicFinal.PASSAGEM;
        return List.of(new Palavra(inicio, inicio + dura, "cutscene.minerwk.cid_atomic.1"),
                new Palavra(segunda, segunda + dura, "cutscene.minerwk.cid_atomic.2"),
                new Palavra(terceira, terceira + dura, "cutscene.minerwk.cid_atomic.3"));
    }

    /** O primeiro soco de perto; depois de lado, de longe e um pouco de baixo, com os dois subindo. */
    public static Coreografia daSubida() {
        return new Coreografia(List.of(
                new Quadro(0, new Vetor(1.5, -.2, -.7), new Vetor(0, -.2, 2), .9, 4),
                // A parada do primeiro soco de perto; quando o alvo sai voando a câmera já está longe.
                new Quadro(AtomicFinal.PARADA + 1, new Vetor(1.8, -.1, -1), new Vetor(0, .4, 2), .95, 2),
                new Quadro(AtomicFinal.PARADA + 2, new Vetor(6.5, -.6, 1), MEIO.mais(new Vetor(0, 1.5, 0)), 1.1, 0, true),
                new Quadro(AtomicFinal.PASSAGEM, new Vetor(7.5, -3, 1), MEIO.mais(new Vetor(0, 2, 0)), 1.15, 0),
                new Quadro(AtomicFinal.PASSAGEM + ESPERA, new Vetor(7.5, -3, 1), MEIO.mais(new Vetor(0, 7, 0)), 1.15, 0)));
    }

    /**
     * A chegada em plano aberto, com o planeta embaixo; os socos de lado; o rosto no capuz nas três palavras,
     * visto de frente e de lado (o alvo está a um passo dele, e de frente taparia o quadro).
     */
    public static Coreografia doEspaco() {
        int socos = AtomicFinal.TICKS_DA_CHEGADA, fala = AtomicFinal.FALA - AtomicFinal.PASSAGEM;
        int carga = AtomicFinal.CARGA - AtomicFinal.PASSAGEM, fim = AtomicFinal.ATOMIC - AtomicFinal.PASSAGEM;
        return new Coreografia(List.of(
                // A chegada: o espaço inteiro, parado, antes do primeiro soco.
                new Quadro(0, new Vetor(11, 6, -4), new Vetor(0, -.8, 1), 1.15, 0),
                new Quadro(socos, new Vetor(9, 4, -2), new Vetor(0, -.5, 1), 1.1, 0),
                new Quadro(fala - 2, new Vetor(6, 1.5, 1), new Vetor(0, -.4, 2), 1, 0),
                // "I" e "am": o rosto de três quartos, a câmera chegando devagar.
                new Quadro(fala, new Vetor(1.9, .15, 1.3), new Vetor(0, -.1, -.3), .75, -3, true),
                new Quadro(carga - 1, new Vetor(1.6, .12, 1.1), new Vetor(0, 0, -.2), .7, 0),
                // A carga e "atomic": de frente e de baixo (o alvo foi arremessado para longe e não tapa mais).
                new Quadro(carga, new Vetor(.25, -.75, 2.6), new Vetor(0, .15, 0), .8, 2, true),
                new Quadro(fim, new Vetor(.1, -.45, 1.5), new Vetor(0, .1, 0), .6, 0),
                new Quadro(fim + ESPERA, new Vetor(.1, -.45, 1.5), new Vetor(0, .1, 0), .6, 0)));
    }

    /** A câmera se afasta de uma vez, até os dois virarem um ponto entre os planetas, e fica. */
    public static Coreografia doClarao() {
        Vetor centro = new Vetor(0, 0, AtomicFinal.AO_LADO / 2), longe = new Vetor(40, 25, -105);
        return new Coreografia(List.of(
                new Quadro(0, new Vetor(3, 1, -4), centro, .9, 0),
                new Quadro(4, new Vetor(5, 2, -8), centro, .95, 0),
                new Quadro(18, longe, centro, 1, 0),
                new Quadro(AtomicFinal.TICKS_DO_CLARAO + ESPERA, longe.vezes(1.08), centro, 1, 0)));
    }

    /** De volta ao chão: o céu, onde a luz ainda brilha, e a câmera desce para trás dele (dentro da cabeça veria o capuz por dentro). */
    public static Coreografia doCeu() {
        int fim = AtomicFinal.DEPOIS_DA_VOLTA, onda = AtomicFinal.ATRASO_DA_ONDA;
        return new Coreografia(List.of(
                new Quadro(0, new Vetor(0, .6, -3.2), new Vetor(0, 10, 3), 1, 0),
                new Quadro(onda - 1, new Vetor(0, .6, -3.2), new Vetor(0, 9.5, 3.5), 1, 0),
                // A onda de choque chega ao chão: de cima e de frente, com o aro abrindo em volta dele.
                new Quadro(onda, new Vetor(5, 5, 9), new Vetor(0, -1.4, 0), 1.05, 0, true),
                new Quadro(fim - 8, new Vetor(6, 6.5, 11), new Vetor(0, -1, 0), 1.05, 0),
                new Quadro(fim, new Vetor(0, .4, -3), new Vetor(0, 0, 5), 1, 0)));
    }
}
