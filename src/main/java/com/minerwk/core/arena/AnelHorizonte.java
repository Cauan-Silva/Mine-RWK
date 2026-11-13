package com.minerwk.core.arena;

import java.util.ArrayList;
import java.util.List;

/**
 * A geometria do horizonte da Escola: um anel de quadros em volta do jogador, em quatro faixas de 90°, cada
 * uma com a sua textura. A faixa 0 fica centrada no sul (+z), onde está a cidade, e as seguintes vêm à
 * direita de quem olha para ela (oeste, norte, leste), para a imagem não sair espelhada.
 */
public final class AnelHorizonte {
    public static final int FAIXAS = 4;
    /** Além do canto da caixa (50 × √2 ≈ 71): o anel nunca atravessa o cenário. */
    public static final double RAIO_MINIMO = 80;
    /** A base e o topo, em blocos relativos ao pátio: a base fica bem abaixo do pé da escadaria. */
    public static final double Y_BASE = -30;
    public static final double Y_TOPO = 60;

    /** Um quadro em pé, de (x0, z0) a (x1, z1), e a parte da textura da {@code faixa} que ele mostra. */
    public record Quad(double x0, double z0, double x1, double z1, double yBase, double yTopo, float u0, float u1,
                       int faixa) {
    }

    private AnelHorizonte() {
    }

    public static List<Quad> anel(double raio, double yBase, double yTopo, int segmentosPorFaixa) {
        if (raio < RAIO_MINIMO) throw new IllegalArgumentException("raio menor que " + RAIO_MINIMO + ": " + raio);
        if (segmentosPorFaixa < 1) throw new IllegalArgumentException("segmentos: " + segmentosPorFaixa);
        List<Quad> quads = new ArrayList<>(FAIXAS * segmentosPorFaixa);
        int total = FAIXAS * segmentosPorFaixa;
        for (int i = 0; i < total; i++) {
            double a0 = angulo(i, total), a1 = angulo(i + 1, total);
            int faixa = i / segmentosPorFaixa, k = i % segmentosPorFaixa;
            quads.add(new Quad(-Math.sin(a0) * raio, Math.cos(a0) * raio, -Math.sin(a1) * raio, Math.cos(a1) * raio,
                    yBase, yTopo, (float) k / segmentosPorFaixa, (float) (k + 1) / segmentosPorFaixa, faixa));
        }
        return quads;
    }

    /** O ângulo do i-ésimo ponto, a partir do sul, começando meia faixa antes dele. */
    private static double angulo(int i, int total) {
        // O último ponto é o primeiro: o anel fecha exatamente.
        if (i == total) i = 0;
        return 2 * Math.PI * i / total - Math.PI / FAIXAS;
    }
}
