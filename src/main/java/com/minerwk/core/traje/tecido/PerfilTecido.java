package com.minerwk.core.traje.tecido;

/**
 * Como o pano de um traje se comporta. Cada campo multiplica o comportamento afinado (1 = sem mudança):
 * {@code peso} é a gravidade nas partículas (mais pesado cai mais reto e balança menos); {@code rigidez},
 * o quanto os elos seguram o comprimento; {@code arrasto}, o ar (mais alto assenta mais rápido);
 * {@code inercia}, o quanto o movimento de quem veste joga o pano; {@code mola}, a resposta das peças de
 * mola (mangas, faixas). Os valores são presos a faixas seguras.
 */
public record PerfilTecido(double peso, double rigidez, double arrasto, double inercia, double mola) {
    public static final PerfilTecido PADRAO = new PerfilTecido(1, 1, 1, 1, 1);

    public PerfilTecido {
        peso = limitar(peso, .3, 3);
        rigidez = limitar(rigidez, .2, 1);
        arrasto = limitar(arrasto, .2, 4);
        inercia = limitar(inercia, 0, 3);
        mola = limitar(mola, .5, 2);
    }

    private static double limitar(double v, double a, double b) {
        return Double.isFinite(v) ? Math.max(a, Math.min(b, v)) : 1;
    }
}
