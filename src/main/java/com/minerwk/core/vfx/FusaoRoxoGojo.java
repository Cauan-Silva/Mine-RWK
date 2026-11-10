package com.minerwk.core.vfx;

import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.RoxoMaximo;

/**
 * Formas da fusão do R (Vazio Roxo máximo), sem classes do Minecraft. Depois da viagem em arco, o Azul e
 * o Vermelho dançam numa espiral que fecha, esticam em gota, encostam e param, afundam um no outro e o
 * núcleo roxo nasce e carrega até o silêncio antes do estouro. Tempos em ticks; posições em relação ao
 * centro da fusão, na base (lado, cima, frente).
 */
public final class FusaoRoxoGojo {
    public static final double RAIO_ESFERA = .45;
    /** Distância de cada esfera ao centro quando a dança começa (onde o arco termina). */
    public static final double ORBITA = 1.6;
    public static final double VOLTAS = 2.5;
    /** Ticks paradas e coladas no toque, e ticks afundando uma na outra. */
    public static final int TOQUE = 2, AFUNDA = 2;
    public static final double GOTA_MAX = 1.8;
    /** Inclinação do plano da órbita (os rastros se trançam em hélice, não num círculo chato). */
    private static final double INCLINACAO = Math.toRadians(35);
    /** Depois de encostar, as duas ainda giram coladas devagar (radianos por tick). */
    private static final double GIRO_COLADAS = .6;

    public static final int CIANO = 0x3CC8FF, VIOLETA = 0xB060FF, CARMESIM = 0xFF2A28;

    public static final double NUCLEO_NASCE = .4, NUCLEO_MAX = 3.5;
    public static final double RAIO_CHOQUE = 60;

    private FusaoRoxoGojo() {
    }

    /** Tick da dança em que as esferas encostam. */
    public static int encosta() {
        return RoxoMaximo.TICKS_DANCA - TOQUE - AFUNDA;
    }

    /** Distância entre os centros das esferas no tick {@code d} da dança. */
    public static double separacao(double d) {
        int e = encosta();
        double colada = 2 * RAIO_ESFERA;
        if (d <= e) return colada + (2 * ORBITA - colada) * (1 - PerfilVfxGojo.suave(d / e));
        if (d <= e + TOQUE) return colada;
        return colada * Math.max(0, 1 - (d - e - TOQUE) / AFUNDA);
    }

    /** Ângulo da órbita: duas voltas e meia acelerando até encostar, depois girando devagar coladas. */
    public static double angulo(double d) {
        int e = encosta();
        if (d <= e) return 2 * Math.PI * VOLTAS * Math.pow(Math.max(0, d) / e, 1.6);
        return 2 * Math.PI * VOLTAS + (d - e) * GIRO_COLADAS;
    }

    /** Onde está a esfera do {@code lado} (-1 o Azul, 1 o Vermelho) no tick {@code d} da dança. */
    public static Vetor posicao(int lado, double d) {
        double r = lado * separacao(d) / 2, a = angulo(d);
        // + 0.0 evita -0.0 nas componentes zeradas.
        return new Vetor(r * Math.cos(a) + 0.0, r * Math.sin(a) * Math.sin(INCLINACAO) + 0.0,
                r * Math.sin(a) * Math.cos(INCLINACAO) + 0.0);
    }

    /** Quanto a esfera estica na direção da outra: 1 longe, {@link #GOTA_MAX} encostadas. */
    public static double alongamento(double separacao) {
        double colada = 2 * RAIO_ESFERA;
        double k = Math.max(0, Math.min(1, (2 * ORBITA - separacao) / (2 * ORBITA - colada)));
        return 1 + (GOTA_MAX - 1) * k * k;
    }

    /** Cor do filete que liga as duas, de {@code u} 0 (Azul) a 1 (Vermelho), passando pelo violeta. */
    public static int corFilete(double u) {
        u = Math.max(0, Math.min(1, u));
        return u < .5 ? misturar(CIANO, VIOLETA, u * 2) : misturar(VIOLETA, CARMESIM, (u - .5) * 2);
    }

    private static int misturar(int a, int b, double k) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = (int) Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return r << 16 | g << 8 | bl;
    }

    private static int inicioSilencio() {
        return RoxoMaximo.TICKS_FUSAO - RoxoMaximo.TICKS_SILENCIO;
    }

    /** 0 durante a carga, sobe até 1 no fim do silêncio antes do estouro ({@code f}: tick da fusão). */
    public static double silencio(double f) {
        return Math.max(0, Math.min(1, (f - inicioSilencio()) / RoxoMaximo.TICKS_SILENCIO));
    }

    /** Raio do núcleo: nasce pequeno, cresce carregando e encolhe no silêncio. */
    public static double raioNucleo(double f) {
        int s = inicioSilencio();
        if (f <= s) return NUCLEO_NASCE + (NUCLEO_MAX - NUCLEO_NASCE) * Math.pow(Math.max(0, f) / s, .8);
        return NUCLEO_MAX * (1 - .65 * silencio(f));
    }

    /** Frequência do pulso (radianos por tick), cada vez mais rápida. */
    public static double frequencia(double f) {
        return .5 + .04 * f;
    }

    /** Fator de escala do pulso; 1 (parado) no silêncio. */
    public static double pulso(double f) {
        if (f >= inicioSilencio()) return 1;
        return 1 + .18 * Math.sin(.5 * f + .02 * f * f);
    }

    /** Raio da onda de choque no chão, {@code i} ticks depois do estouro; sai na frente da casca. */
    public static double raioChoque(double i) {
        double s = Math.max(0, Math.min(1, i / RoxoMaximo.TICKS_EXPLOSAO));
        return RAIO_CHOQUE * (1 - (1 - s) * (1 - s));
    }

    /** Um tick do escuro do céu (0 normal, 1 o roxo é a luz principal): escurece rápido e volta devagar. */
    public static double ceu(double atual, double alvo) {
        return alvo > atual ? atual + (alvo - atual) * .15 : atual - (atual - alvo) * .03;
    }
}
