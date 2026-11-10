package com.minerwk.core.vfx;

import com.minerwk.core.kit.Vetor;

/** Formas da bolha do Infinito: ondas saindo do ponto do toque e hexágonos que aparecem e somem. */
public final class BolhaInfinitoGojo {
    /** Raio da bolha, em blocos, em volta do centro do Gojo. */
    public static final double RAIO = 1.4;
    /** Ticks de uma onda de toque. */
    public static final int TICKS_ONDA = 16;
    /** Ondas por toque, uma atrás da outra. */
    public static final int ONDAS = 3;
    public static final int HEXAGONOS = 14;
    private static final double OURO = 2.399963;

    private BolhaInfinitoGojo() {
    }

    /** Ângulo (a partir do ponto do toque) da onda {@code k} no tick {@code t}: abre rápido e para no meio da bolha. */
    public static double angulo(int k, double t) {
        double x = Math.max(0, t - k * 3) / TICKS_ONDA;
        return Math.PI / 2 * (1 - Math.pow(1 - Math.min(1, x), 2));
    }

    /** Transparência da onda {@code k}: forte no começo e some até o fim. */
    public static double alphaOnda(int k, double t) {
        double x = (t - k * 3) / TICKS_ONDA;
        if (x <= 0 || x >= 1) return 0;
        return (1 - x) * (1 - k * .25);
    }

    /** Direção do hexágono {@code i}, espalhados por igual na esfera (espiral de Fibonacci). */
    public static Vetor direcaoHexagono(int i) {
        double y = 1 - 2 * (i + .5) / HEXAGONOS;
        double r = Math.sqrt(1 - y * y);
        double a = i * OURO;
        return new Vetor(Math.cos(a) * r, y, Math.sin(a) * r);
    }

    /** Brilho do hexágono {@code i} no tempo {@code tempo} (segundos): cada um acende e apaga na sua vez. */
    public static double brilhoHexagono(int i, double tempo) {
        return Math.pow(Math.max(0, Math.sin(tempo * 1.6 + i * 1.7)), 6);
    }
}
