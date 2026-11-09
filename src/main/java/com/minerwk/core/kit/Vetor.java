package com.minerwk.core.kit;

/** Ponto ou direção no mundo, sem classes do Minecraft. */
public record Vetor(double x, double y, double z) {
    public static final Vetor ZERO = new Vetor(0, 0, 0);

    public Vetor mais(Vetor o) {
        return new Vetor(x + o.x, y + o.y, z + o.z);
    }

    public Vetor menos(Vetor o) {
        return new Vetor(x - o.x, y - o.y, z - o.z);
    }

    public Vetor vezes(double k) {
        return new Vetor(x * k, y * k, z * k);
    }

    public double comprimento() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double distancia(Vetor o) {
        return menos(o).comprimento();
    }

    /** Mesmo sentido com comprimento 1; o zero continua zero. */
    public Vetor normalizado() {
        double c = comprimento();
        return c < 1e-9 ? ZERO : vezes(1 / c);
    }
}
