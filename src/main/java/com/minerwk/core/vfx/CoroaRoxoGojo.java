package com.minerwk.core.vfx;

import com.minerwk.core.kit.Vetor;

/**
 * Roxo: Azul e Vermelho se aproximam durante a carga e se fundem no primeiro ponto da massa;
 * depois, coroa de lâminas de energia e esteira helicoidal. Só aparência.
 * Coordenadas (lateral, vertical, frente) relativas ao centro da massa.
 */
public final class CoroaRoxoGojo {
    private static final double OURO = 2.399963;
    private static final double SEPARACAO_INICIAL = 3.2;

    private CoroaRoxoGojo() {
    }

    /** Distância entre as esferas azul e vermelha no tick {@code t} de uma carga de {@code carga} ticks. */
    public static double separacao(double t, int carga) {
        return SEPARACAO_INICIAL * (1 - PerfilVfxGojo.suave(t / carga));
    }

    /** Onde as esferas se fundem: o primeiro ponto do caminho da massa, para não haver salto no disparo. */
    public static Vetor centroFusao(Vetor olhos, Vetor direcao, double passo) {
        return TrajetoriaGojo.roxo(olhos, direcao, passo, 0);
    }

    /** O núcleo violeta aparece no último terço da carga. */
    public static double formacao(double t, int carga) {
        return PerfilVfxGojo.suave((t - carga * .7) / (carga * .3));
    }

    /** Lâmina inclinada em volta da massa. */
    public static Vetor ponto(int faixa, double s, double t, double raio) {
        double a = s * Math.PI * 2 + t * .045 * (faixa % 2 == 0 ? 1 : -1) + faixa * OURO;
        double r = raio * (1.07 + .035 * Math.sin(a * 5 - t * .13 + faixa));
        double x = Math.cos(a) * r;
        double y = Math.sin(a) * r;
        double z = Math.sin(a * 3 + faixa) * raio * .045;
        double inclinacao = .55 + faixa * .95;
        double co = Math.cos(inclinacao);
        double si = Math.sin(inclinacao);
        return new Vetor(x, y * co - z * si, y * si + z * co);
    }

    /** Arco de energia ligando as duas esferas durante a carga. */
    public static Vetor ponte(int faixa, double s, double t, int carga) {
        return new Vetor(separacao(t, carga) * (2 * s - 1), Math.sin(s * Math.PI) * Math.sin(t * .12 + faixa) * .6, 0);
    }

    /** Esteira helicoidal atrás da massa; {@code i} = ticks desde o disparo. */
    public static Vetor rastro(int faixa, int total, double s, double i, double raio) {
        double a = faixa * Math.PI * 2 / total + s * 9 - i * .12;
        double r = raio * (1.2 + s * .3);
        return new Vetor(Math.cos(a) * r, Math.sin(a) * r, -s * Math.min(12, Math.max(0, i) * 1.6));
    }
}
