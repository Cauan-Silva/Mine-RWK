package com.minerwk.core.vfx;

import com.minerwk.core.kit.Vetor;

/**
 * Vermelho: energia comprimida na mão durante a carga e ondas de repulsão no impacto.
 * No Mine-RWK a esfera viaja (ver {@link TrajetoriaGojo}); o cone de descarga do Minezin saiu.
 * Coordenadas: (lateral, vertical, frente) relativas ao foco.
 */
public final class DescargaVermelhoGojo {
    private static final double OURO = 2.399963;
    /** Ticks até a carga atingir o tamanho cheio. */
    public static final int CARGA_CHEIA = 30;

    private DescargaVermelhoGojo() {
    }

    public static double raioCarga(double t) {
        return .1 + .32 * PerfilVfxGojo.suave(t / CARGA_CHEIA);
    }

    /** Fios girando em volta da energia na mão. */
    public static Vetor carga(int faixa, double s, double t) {
        double a = s * Math.PI * 1.65 + faixa * OURO + t * .11;
        double r = raioCarga(t) * (1.35 + .3 * Math.sin(faixa * 2.1));
        double incl = .4 + faixa * .6;
        double y = Math.sin(a) * r;
        return new Vetor(Math.cos(a) * r, y * Math.cos(incl), y * Math.sin(incl));
    }

    /** Raio da onda de choque {@code i} ticks depois do impacto. */
    public static double raioOnda(double i) {
        return i < 0 ? 0 : .6 + i * 1.15;
    }

    public static double alphaOnda(double i) {
        return i < 0 ? 0 : Math.exp(-i / 5);
    }

    /** Anéis da repulsão, abrindo a partir do ponto de impacto. */
    public static Vetor onda(int faixa, double s, double i) {
        double a = s * Math.PI * 2;
        double r = (1 + faixa * 1.3) * (1 + Math.max(0, i) * .17);
        r *= 1 + .025 * Math.sin(a * 9 - i * .5 + faixa);
        return new Vetor(Math.cos(a) * r, Math.sin(a) * r, faixa * .4 + Math.sin(a * 5 + i * .3) * .12);
    }
}
