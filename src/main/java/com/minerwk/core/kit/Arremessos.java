package com.minerwk.core.kit;

/**
 * Reação do alvo a um golpe forte: para no ar por alguns ticks, sai voando e gira até tocar o chão.
 * Só a conta; o servidor aplica nas entidades.
 */
public final class Arremessos {
    /** Ticks máximos de giro depois do lançamento. */
    public static final int GIRO_MAX = 20;
    /** Ticks logo depois do lançamento em que ainda gira mesmo encostado no chão (ainda não saiu do lugar). */
    private static final int DECOLAGEM = 3;

    public enum Passo { CONGELAR, LANCAR, GIRAR, FIM }

    private Arremessos() {
    }

    /**
     * Velocidade de saída: para longe do centro com {@code forca} blocos por tick, mais um pouco para cima.
     * Com {@code eixo} (não nulo), ignora a parte ao longo do eixo: o alvo sai para o lado do caminho.
     */
    public static Vetor velocidade(Vetor centro, Vetor alvo, double forca, Vetor eixo) {
        Vetor d = alvo.menos(centro);
        if (eixo != null && eixo.comprimento() > 1e-9) {
            Vetor e = eixo.normalizado();
            double ao = d.x() * e.x() + d.y() * e.y() + d.z() * e.z();
            d = d.menos(e.vezes(ao));
        }
        Vetor dir = d.comprimento() < 1e-6 ? new Vetor(0, 1, 0) : d.normalizado();
        return dir.vezes(forca).mais(new Vetor(0, Math.min(1, .25 * forca), 0));
    }

    /** Um alvo sendo arremessado. Chame {@link #tick} uma vez por tick. */
    public static final class Estado {
        private final Vetor velocidade;
        private final int congelar;
        private int t;

        public Estado(Vetor velocidade, int congelar) {
            this.velocidade = velocidade;
            this.congelar = Math.max(0, congelar);
        }

        public Vetor velocidade() {
            return velocidade;
        }

        public Passo tick(boolean noChao) {
            Passo p;
            if (t < congelar) p = Passo.CONGELAR;
            else if (t == congelar) p = Passo.LANCAR;
            else {
                int voo = t - congelar;
                p = voo <= GIRO_MAX && (voo <= DECOLAGEM || !noChao) ? Passo.GIRAR : Passo.FIM;
            }
            t++;
            return p;
        }
    }
}
