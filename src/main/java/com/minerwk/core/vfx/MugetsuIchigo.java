package com.minerwk.core.vfx;

/**
 * As formas do Mugetsu, sem classes do Minecraft: o véu de escuridão que sobe do chão e avança, o céu que
 * escurece com ele e a fumaça de reiatsu preto da transformação. O servidor usa as medidas para o dano
 * ({@code Ichigo.Veu}); o cliente desenha a partir das curvas.
 */
public final class MugetsuIchigo {
    /** A carga (a cutscene) em ticks: a forma final aparece, tudo se cala, o corte. */
    public static final int TRANSFORMA = 30, SILENCIO = 84, CARGA = 120;
    /** O véu: uma lâmina de escuridão de 3 blocos de largura que avança 150 em 1 s (7,5 por tick). */
    public static final double ALCANCE = 150, MEIA_LARGURA = 1.5, PASSO = 7.5;
    /** Ticks do avanço, e os do véu se desfazendo e do céu voltando ao normal. */
    public static final int TICKS = (int) (ALCANCE / PASSO), RESTO = 30;
    /** A altura do véu no meio, e em quantos blocos atrás da frente ele termina de subir. */
    public static final double ALTURA = 40, SUBIDA = 12;
    /** O céu fica todo escuro nesses ticks depois do corte. */
    public static final int CEU_ESCURECE = 4;
    /** Quanto o céu já escurece durante a carga, da transformação em diante. */
    public static final double CEU_DA_CARGA = .45;
    /** O que sobra da fumaça quando tudo se cala. */
    public static final double FUMACA_NO_SILENCIO = .12;

    private MugetsuIchigo() {
    }

    private static double suave(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    /** A quantos blocos da saída a frente do véu está no tick {@code t} (pode ser fracionário). */
    public static double frente(double t) {
        return Math.max(0, Math.min(ALCANCE, t * PASSO));
    }

    /**
     * A altura do véu no ponto a {@code z} blocos da saída e na posição lateral {@code u} (−1 e 1 são as
     * bordas), no tick {@code t}. Zero adiante da frente; logo atrás dela ele ainda está subindo do chão. O
     * topo é irregular, como fumaça, e mais baixo nas bordas.
     */
    public static double altura(double u, double z, double t) {
        double atras = frente(t) - z;
        if (atras <= 0 || z < 0) return 0;
        double perfil = 1 - .42 * u * u;
        double onda = .86 + .14 * Math.sin(z * 1.7 + u * 5.1 + t * .55) * Math.cos(z * .63 - t * .31);
        return Math.min(ALTURA, ALTURA * perfil * onda * suave(atras / SUBIDA));
    }

    /** Quanto o véu ainda cobre: cheio enquanto avança, depois se desfaz até sumir. */
    public static double opacidade(double t) {
        if (t <= TICKS) return 1;
        return 1 - suave((t - TICKS) / RESTO);
    }

    /** Quanto o céu está escuro no tick {@code t} do corte: escurece na hora e volta com o véu se desfazendo. */
    public static double ceu(double t) {
        if (t <= 0) return 0;
        if (t < CEU_ESCURECE) return t / CEU_ESCURECE;
        if (t <= TICKS) return 1;
        return 1 - suave((t - TICKS) / RESTO);
    }

    /**
     * A força da fumaça de reiatsu preto durante a carga: incha até a forma final aparecer
     * ({@code transforma}), murcha até o silêncio e fica um fio parado até o corte.
     */
    public static double fumaca(double t, int transforma, int silencio) {
        if (t <= 0) return 0;
        if (t <= transforma) return suave(t / transforma);
        if (t >= silencio) return FUMACA_NO_SILENCIO;
        return 1 - (1 - FUMACA_NO_SILENCIO) * suave((t - transforma) / (silencio - transforma));
    }

    /** O céu durante a carga: escurece em parte até a transformação e fica assim até o corte. */
    public static double ceuDaCarga(double t, int transforma) {
        return CEU_DA_CARGA * suave(t / transforma);
    }
}
