package com.minerwk.core.vfx;

import com.minerwk.core.kit.Vetor;

/** Azul: núcleo pulsante e filamentos em espiral que terminam no núcleo. Coordenadas relativas ao centro. */
public final class FluxoAzulGojo {
    private static final double OURO = 2.399963; // ângulo áureo, espalha as faixas sem repetir

    private FluxoAzulGojo() {
    }

    public static double raio(double t) {
        return .72 * (.94 + .06 * Math.sin(t * .3));
    }

    /** Ponto do filamento {@code faixa}; {@code s} vai de 0 (borda do campo) a 1 (núcleo). */
    public static Vetor fluxo(int faixa, double s, double t) {
        double a = faixa * OURO + s * 5.6 - t * .035;
        double r = .75 + 4.5 * Math.pow(1 - s, 1.45);
        double inclinacao = Math.sin(faixa * 1.91) * .65;
        return new Vetor(Math.cos(a) * r, Math.sin(a + faixa) * r * inclinacao, Math.sin(a) * r);
    }

    /** Brilho que corre pelo filamento de fora para dentro. */
    public static double pulso(int faixa, double s, double t) {
        double onda = .5 + .5 * Math.cos((s - t * .045 - faixa * .17) * Math.PI * 2);
        return (.16 + .84 * Math.pow(onda, 12)) * PerfilVfxGojo.suave(s / .12);
    }

    /** Ticks para um anel de sucção nascer largo e se fechar no núcleo. */
    public static final int CICLO_ANEL = 8;

    /**
     * Espiral de vento {@code faixa} (0 a 3): de {@code s} 0 (fora, raio 4,2) a 1 (o núcleo), apertando a
     * cada volta e girando com o tempo.
     */
    public static Vetor espiral(int faixa, double s, double t) {
        double nucleo = raio(t);
        double r = nucleo + (4.2 - nucleo) * Math.pow(1 - s, 1.6);
        double a = faixa * Math.PI / 2 + s * Math.PI * 5 - t * .25;
        double y = (1 - s) * Math.sin(faixa * 1.3 + t * .05) * 1.2;
        return new Vetor(Math.cos(a) * r, y, Math.sin(a) * r);
    }

    /** Raio do anel de sucção {@code k} (0 a 2, defasados) no tick {@code t}. */
    public static double anelSuccao(int k, double t) {
        double fase = ((t / CICLO_ANEL + k / 3.0) % 1 + 1) % 1;
        return raio(t) + .2 + 4 * Math.pow(1 - fase, 1.5);
    }

    /** Transparência do anel de sucção: aparece, fica forte perto do núcleo e some ao entrar. */
    public static double alphaAnel(int k, double t) {
        double fase = ((t / CICLO_ANEL + k / 3.0) % 1 + 1) % 1;
        return Math.sin(Math.PI * fase);
    }

    /** Arcos inclinados girando em volta do núcleo. */
    public static Vetor arco(int faixa, double s, double t) {
        double a = (s * .82 + faixa * .31) * Math.PI * 2 + t * (faixa % 2 == 0 ? .045 : -.037);
        double r = raio(t) * (1.3 + faixa * .23);
        double y = Math.sin(a) * r;
        double inclinacao = .45 + faixa * .8;
        return new Vetor(Math.cos(a) * r, y * Math.cos(inclinacao), y * Math.sin(inclinacao));
    }
}
