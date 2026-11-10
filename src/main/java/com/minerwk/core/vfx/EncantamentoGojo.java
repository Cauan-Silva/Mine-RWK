package com.minerwk.core.vfx;

import com.minerwk.core.kit.Vetor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Formas do encantamento do R, sem classes do Minecraft: o quanto o céu escurece durante a carga e as
 * rachaduras que crescem no chão a partir dos pés do Gojo.
 */
public final class EncantamentoGojo {
    /** Escuro do céu no fim da carga (o escuro cheio, 1, fica para a fusão e a explosão). */
    public static final double CEU_CARGA = .6;
    /** Até quantos blocos do Gojo o céu de quem vê escurece. */
    public static final double ALCANCE_CEU = 48;
    public static final int RACHADURAS = 7;
    public static final double ALCANCE_RACHADURA = 6;
    private static final double PASSO = .5;

    private EncantamentoGojo() {
    }

    /** Alvo do escuro do céu no tick {@code t} de uma carga de {@code duracao} ticks. */
    public static double ceuCarga(double t, double duracao) {
        return CEU_CARGA * PerfilVfxGojo.suave(t / Math.max(1, duracao));
    }

    /** Quanto do escuro chega a quem está a {@code distancia} blocos: cheio perto, nada no alcance. */
    public static double perto(double distancia) {
        return Math.max(0, Math.min(1, 1 - distancia / ALCANCE_CEU));
    }

    /**
     * Pontos da rachadura {@code i} no chão (y = 0), em relação aos pés, com a carga na fração {@code k}.
     * A parte já aberta não muda de lugar enquanto ela cresce; a {@code semente} muda o desenho a cada uso.
     */
    public static List<Vetor> rachadura(long semente, int i, double k) {
        Random rnd = new Random(semente * 31 + i);
        double total = ALCANCE_RACHADURA * (.55 + .45 * rnd.nextDouble()) * Math.max(0, Math.min(1, k));
        double angulo = 2 * Math.PI * (i + (rnd.nextDouble() - .5) * .6) / RACHADURAS;
        List<Vetor> pts = new ArrayList<>();
        Vetor p = Vetor.ZERO;
        pts.add(p);
        for (double feito = 0; feito < total - 1e-9; feito += PASSO) {
            angulo += (rnd.nextDouble() - .5) * 1.1;
            double passo = Math.min(PASSO, total - feito);
            p = new Vetor(p.x() + Math.cos(angulo) * passo, 0, p.z() + Math.sin(angulo) * passo);
            pts.add(p);
        }
        return pts;
    }
}
