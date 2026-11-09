package com.minerwk.core.kit;

/**
 * Regra de movimento das barreiras esféricas (o domínio do Gojo) e do voo do Roxo 200%. Roda igual no
 * servidor (todas as entidades) e no cliente (o próprio jogador), então não dá tranco.
 * Posições são dos pés.
 */
public final class Barreira {
    /** Distância da borda onde quem encosta fica. */
    public static final double MARGEM = 0.6;
    /** Quanto o Gojo sobe no encantamento do Roxo 200% e em quantos ticks. */
    public static final double ALTURA_ROXO_200 = 20;
    public static final int TICKS_SUBIDA_ROXO_200 = 60;

    public record Correcao(Vetor posicao, Vetor velocidade, boolean noChao) {
    }

    private Barreira() {
    }

    /** Lado em que a entidade está quando a barreira aparece (ou quando é vista pela primeira vez). */
    public static boolean dentro(Vetor centro, double raio, Vetor pes) {
        return pes.distancia(centro) < raio;
    }

    /**
     * Corrige posição e velocidade: quem estava {@code dentro} não passa de {@code raio - MARGEM} e não
     * desce abaixo de {@code chao}; quem estava fora não chega a menos de {@code raio + MARGEM}.
     */
    public static Correcao corrigir(Vetor centro, double raio, double chao, boolean dentro, Vetor pes, Vetor vel) {
        Vetor p = pes;
        Vetor v = vel;
        boolean noChao = false;
        if (dentro) {
            if (p.y() < chao) {
                p = new Vetor(p.x(), chao, p.z());
                if (v.y() < 0) v = new Vetor(v.x(), 0, v.z());
                noChao = true;
            }
            Vetor d = p.menos(centro);
            double dist = d.comprimento();
            double limite = raio - MARGEM;
            if (dist > limite && dist > 1e-9) {
                Vetor n = d.vezes(1 / dist);
                p = centro.mais(n.vezes(limite));
                v = semComponente(v, n, true);
            }
        } else {
            Vetor d = p.menos(centro);
            double dist = d.comprimento();
            double limite = raio + MARGEM;
            if (dist < limite && dist > 1e-9) {
                Vetor n = d.vezes(1 / dist);
                p = centro.mais(n.vezes(limite));
                v = semComponente(v, n, false);
            }
        }
        return new Correcao(p, v, noChao);
    }

    /** Tira de {@code v} a parte ao longo de {@code n}: a que vai para fora ({@code paraFora}) ou para dentro. */
    private static Vetor semComponente(Vetor v, Vetor n, boolean paraFora) {
        double k = v.x() * n.x() + v.y() * n.y() + v.z() * n.z();
        if (paraFora ? k <= 0 : k >= 0) return v;
        return v.menos(n.vezes(k));
    }

    /** Altura acima do ponto de partida no tick {@code t} do encantamento do Roxo 200% (sobe suave e para). */
    public static double subidaRoxo200(double t) {
        double x = Math.max(0, Math.min(1, t / TICKS_SUBIDA_ROXO_200));
        return ALTURA_ROXO_200 * x * x * (3 - 2 * x);
    }
}
