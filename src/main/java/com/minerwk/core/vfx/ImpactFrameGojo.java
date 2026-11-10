package com.minerwk.core.vfx;

/**
 * Impact frame do uppercut do R: três ticks em preto e branco de alto contraste com um toque de roxo,
 * piscando invertido como nos animes, junto com o hit-stop. Sem classes do Minecraft.
 */
public final class ImpactFrameGojo {
    public static final int TICKS = 3;
    /** Até quantos blocos do soco a tela de quem vê pisca. */
    public static final double ALCANCE = 48;

    private ImpactFrameGojo() {
    }

    /** Dispara no tick do soco ({@code idade} do visual igual ao {@code avanco}), para quem está perto. */
    public static boolean dispara(long idade, int avanco, double distancia) {
        return idade == avanco && distancia <= ALCANCE;
    }

    /** Quanto a tela vira preto e branco, {@code i} ticks depois do soco: cheio e sumindo no último tick. */
    public static double forca(double i) {
        if (i < 0 || i >= TICKS) return 0;
        return Math.min(1, TICKS - i);
    }

    /** Se o tick {@code i} é o invertido (silhuetas claras sobre o fundo escuro, e vice-versa). */
    public static boolean invertido(double i) {
        return ((int) Math.floor(i)) % 2 == 1;
    }
}
