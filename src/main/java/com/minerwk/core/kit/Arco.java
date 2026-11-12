package com.minerwk.core.kit;

/** O arco de um corte horizontal, sem classes do Minecraft: quem ele pega. */
public final class Arco {
    private Arco() {
    }

    /**
     * Se {@code ponto} está no arco: a até {@code alcance} blocos de {@code origem} e dentro de {@code graus}
     * (o arco inteiro, metade para cada lado de {@code direcao}), os dois medidos no chão, e a até
     * {@code altura} blocos acima ou abaixo. Quem está colado na origem conta; sem direção no chão
     * (olhando reto para cima ou para baixo), vale a volta toda.
     */
    public static boolean contem(Vetor origem, Vetor direcao, Vetor ponto, double alcance, double graus, double altura) {
        double dx = ponto.x() - origem.x(), dz = ponto.z() - origem.z();
        if (Math.abs(ponto.y() - origem.y()) > altura) return false;
        double dist = Math.hypot(dx, dz);
        if (dist > alcance) return false;
        double frente = Math.hypot(direcao.x(), direcao.z());
        if (dist < 1e-6 || frente < 1e-6) return true;
        double cos = (dx * direcao.x() + dz * direcao.z()) / (dist * frente);
        return cos >= Math.cos(Math.toRadians(graus / 2)) - 1e-9;
    }
}
