package com.minerwk.core.personagens.gojo;

import com.minerwk.core.kit.Vetor;

/**
 * Sequência do R depois do encantamento (Vazio Roxo máximo): avanço até o alvo, uppercut, os dois
 * sobem, o Azul e o Vermelho se fundem atrás do Gojo e o Roxo explode engolindo os dois. Os tempos
 * são em ticks desde o disparo; servidor e cliente usam as mesmas contas.
 */
public final class RoxoMaximo {
    public static final double ALCANCE = 60;
    /** Distância máxima da linha da mira para alguém virar o alvo. */
    public static final double LARGURA = 4;
    public static final double VEL_AVANCO = 3;
    /** O Gojo para a essa distância do alvo. */
    public static final double PARADA = 1.5;
    public static final int AVANCO_MAX = 20;
    public static final double SUBIDA = 25;
    public static final int TICKS_SUBIDA = 30;
    /** As esferas: {@link #TICKS_VIAGEM} em arco das mãos até atrás do Gojo e {@link #TICKS_DANCA} de dança e toque. */
    public static final int TICKS_VIAGEM = 18;
    public static final int TICKS_DANCA = 12;
    public static final int TICKS_ESFERAS = TICKS_VIAGEM + TICKS_DANCA;
    /** O núcleo roxo carregando; os últimos {@link #TICKS_SILENCIO} são o silêncio antes do estouro. */
    public static final int TICKS_FUSAO = 40;
    public static final int TICKS_SILENCIO = 4;
    public static final double ATRAS = 4;
    public static final double RAIO_EXPLOSAO = 40;
    public static final int TICKS_EXPLOSAO = 30;
    public static final float DANO_UPPERCUT = 10;
    public static final float DANO = 60;
    /** Sem alvo: as esferas vão até a mira nesse tempo. */
    public static final int TICKS_ESFERAS_SEM_ALVO = TICKS_ESFERAS;

    private RoxoMaximo() {
    }

    /** Ticks do avanço até o alvo a {@code distancia} dos olhos. */
    public static int avanco(double distancia) {
        return (int) Math.min(AVANCO_MAX, Math.ceil(Math.max(0, distancia - PARADA) / VEL_AVANCO));
    }

    public static int inicioEsferas(int avanco) {
        return avanco + TICKS_SUBIDA;
    }

    public static int inicioFusao(int avanco) {
        return inicioEsferas(avanco) + TICKS_ESFERAS;
    }

    public static int inicioSilencio(int avanco) {
        return inicioExplosao(avanco) - TICKS_SILENCIO;
    }

    public static int inicioExplosao(int avanco) {
        return inicioFusao(avanco) + TICKS_FUSAO;
    }

    public static int fim(int avanco) {
        return inicioExplosao(avanco) + TICKS_EXPLOSAO;
    }

    /** Quanto o alvo (e o Gojo) subiram {@code t} ticks depois do uppercut. */
    public static double alturaAlvo(double t) {
        double x = Math.max(0, Math.min(1, t / TICKS_SUBIDA));
        return SUBIDA * x * x * (3 - 2 * x);
    }

    /** Onde o Gojo está no tick {@code t}, em relação aos olhos no disparo, indo na direção {@code dir}. */
    public static Vetor deslocamentoGojo(double t, Vetor dir, double distancia) {
        int a = avanco(distancia);
        double fracao = a == 0 ? 1 : Math.min(1, Math.max(0, t / a));
        Vetor frente = dir.normalizado().vezes(fracao * Math.max(0, distancia - PARADA));
        return frente.mais(new Vetor(0, alturaAlvo(t - a), 0));
    }

    /** Centro da fusão: {@link #ATRAS} blocos atrás dos olhos, na horizontal (ou acima, se ele olha reto para baixo). */
    public static Vetor centroFusao(Vetor olhos, Vetor dir) {
        double h = Math.sqrt(dir.x() * dir.x() + dir.z() * dir.z());
        if (h < 1e-6) return olhos.mais(new Vetor(0, ATRAS, 0));
        return olhos.menos(new Vetor(dir.x() / h, 0, dir.z() / h).vezes(ATRAS));
    }
}
