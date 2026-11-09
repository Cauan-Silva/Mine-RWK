package com.minerwk.core.kit.teste;

import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Recurso;

/** Recurso do personagem de teste: enche rápido parado e devagar em movimento. */
public final class RecursoFolego implements Recurso {
    static final double MAXIMO = 100;
    static final double PARADO = 0.5;
    static final double ANDANDO = 0.05;

    private double atual = MAXIMO;

    @Override
    public String nome() {
        return "Fôlego";
    }

    @Override
    public int cor() {
        return 0x1D9E75;
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
        if (!pode(custo)) throw new IllegalStateException("fôlego insuficiente: " + atual + " < " + custo);
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
