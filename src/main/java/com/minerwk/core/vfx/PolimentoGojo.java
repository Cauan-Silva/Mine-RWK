package com.minerwk.core.vfx;

import com.minerwk.core.kit.Vetor;

/** Detalhes do visual do Gojo: detritos do Azul, ondulação do Infinito e estrelas do domínio. */
public final class PolimentoGojo {
    private static final double OURO = 2.399963;

    private PolimentoGojo() {
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    /** Progresso de 0 (borda do campo) a 1 (engolido pelo núcleo) do detrito {@code i}. */
    public static double progressoDetrito(int i, double t) {
        return frac(t * .022 + i * .618034);
    }

    /** Detrito puxado em espiral até o núcleo do Azul, relativo ao centro. */
    public static Vetor detrito(int i, double t) {
        double u = progressoDetrito(i, t);
        double r = .6 + 5.4 * Math.pow(1 - u, 1.3);
        double a = i * OURO + u * 9.5;
        double y = -1.6 * (1 - u) * (.6 + .4 * Math.sin(i * 1.7)) + Math.sin(u * Math.PI * 3 + i) * .25 * (1 - u);
        return new Vetor(Math.cos(a) * r, y, Math.sin(a) * r);
    }

    public static double tamanhoDetrito(int i, double t) {
        return (.1 + .08 * (i % 3)) * (1 - .7 * progressoDetrito(i, t));
    }

    public static double alphaDetrito(int i, double t) {
        double u = progressoDetrito(i, t);
        return PerfilVfxGojo.suave(u / .15) * PerfilVfxGojo.suave((1 - u) / .1);
    }

    /** Ondulação do Infinito: três anéis que nascem e somem em ciclo curto. */
    public static double raioTic(int anel, double t) {
        return .12 + .42 * frac(t * .09 + anel / 3.0);
    }

    public static double alphaTic(int anel, double t) {
        double f = frac(t * .09 + anel / 3.0);
        return Math.sin(f * Math.PI) * (1 - f * .5);
    }

    /** Estrela na casca interna do domínio, relativa ao centro. */
    public static Vetor estrela(int i, double raio) {
        double y = 1 - 2 * frac(i * .381966 + .13);
        double r = Math.sqrt(Math.max(0, 1 - y * y));
        double a = i * OURO;
        double k = raio * (.55 + .4 * frac(i * .7548));
        return new Vetor(Math.cos(a) * r * k, Math.abs(y) * k * .8 + .4, Math.sin(a) * r * k);
    }

    public static double brilhoEstrela(int i, double t) {
        return .45 + .55 * Math.pow(.5 + .5 * Math.sin(t * .21 + i * 1.93), 3);
    }
}
