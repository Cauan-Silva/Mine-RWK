package com.minerwk.core.personagens.gojo;

import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Recurso;

/** Recurso do Gojo. Os Seis Olhos quase não desperdiçam energia, então ela volta rápido. */
public final class EnergiaAmaldicoada implements Recurso {
    static final double MAXIMO = 200;
    static final double PARADO = 1.0;
    static final double ANDANDO = 0.4;

    private double atual = MAXIMO;

    @Override
    public String nome() {
        return "Energia Amaldiçoada";
    }

    @Override
    public int cor() {
        return 0x3C8CFF;
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
        if (!pode(custo)) throw new IllegalStateException("energia insuficiente: " + atual + " < " + custo);
        atual = Math.max(0, atual - custo);
    }

    @Override
    public void tick(EstadoDoJogador estado) {
        atual = Math.min(MAXIMO, atual + (estado.parado() ? PARADO : ANDANDO));
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
