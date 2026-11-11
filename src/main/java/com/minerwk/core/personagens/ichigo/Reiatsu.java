package com.minerwk.core.personagens.ichigo;

import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Recurso;

/**
 * Recurso do Ichigo. No Shikai volta sempre, e mais rápido para quem passa um tempo sem apanhar. No Bankai
 * não volta e escorre: é o que limita a forma. Quem encerra o Bankai quando ele acaba é o kit (ver
 * {@link #esgotado()}).
 */
public final class Reiatsu implements Recurso {
    /** Id da forma do Bankai (as habilidades dela entram com o kit). */
    public static final String BANKAI = "bankai";
    public static final double MAXIMO = 200;
    /** Por tick no Shikai: 1,5 por segundo. */
    public static final double GANHO = 1.5 / 20;
    /** Por tick no Shikai depois de {@link #CALMA} ticks sem apanhar: 3 por segundo. */
    public static final double GANHO_CALMO = 3.0 / 20;
    /** Ticks sem apanhar para o ganho dobrar: 4 segundos. */
    public static final int CALMA = 80;
    /** Por tick no Bankai: 2 por segundo. */
    public static final double DRENO = 2.0 / 20;
    static final int COR_SHIKAI = 0x8FD8FF;
    static final int COR_BANKAI = 0x8E1B1B;

    private double atual = MAXIMO;
    private boolean bankai;
    /** Ticks desde o último golpe recebido; começa calmo. */
    private int semApanhar = CALMA;

    @Override
    public void forma(String forma) {
        bankai = BANKAI.equals(forma);
    }

    /** No Bankai, sem reiatsu: a forma tem de acabar. */
    public boolean esgotado() {
        return bankai && atual <= 0;
    }

    @Override
    public String nome() {
        return "Reiatsu";
    }

    @Override
    public int cor() {
        return bankai ? COR_BANKAI : COR_SHIKAI;
    }

    @Override
    public double atual() {
        return atual;
    }

    @Override
    public double maximo() {
        return MAXIMO;
    }

    @Override
    public boolean pode(double custo) {
        return atual + 1e-9 >= custo;
    }

    @Override
    public void gastar(double custo) {
        if (!pode(custo)) throw new IllegalStateException("reiatsu insuficiente: " + atual + " < " + custo);
        atual = Math.max(0, atual - custo);
    }

    @Override
    public void tick(EstadoDoJogador estado) {
        semApanhar = estado.apanhou() ? 0 : Math.min(CALMA, semApanhar + 1);
        if (bankai) {
            atual = Math.max(0, atual - DRENO);
        } else {
            atual = Math.min(MAXIMO, atual + (semApanhar >= CALMA ? GANHO_CALMO : GANHO));
        }
    }

    @Override
    public double salvar() {
        return atual;
    }

    @Override
    public void carregar(double valor) {
        atual = Double.isFinite(valor) ? Math.max(0, Math.min(MAXIMO, valor)) : 0;
    }
}
