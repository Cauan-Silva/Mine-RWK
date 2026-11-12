package com.minerwk.core.personagens.cid;

import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Recurso;

/**
 * Recurso do Cid. Volta sempre, e mais rápido para quem passa um tempo sem apanhar; no Overdrive não volta.
 */
public final class Magia implements Recurso {
    public static final double MAXIMO = 100;
    /** Por tick: 4 por segundo. */
    public static final double GANHO = 4.0 / 20;
    /** Por tick depois de {@link #CALMA} ticks sem apanhar: 8 por segundo. */
    public static final double GANHO_CALMO = 8.0 / 20;
    /** Ticks sem apanhar para o ganho dobrar: 4 segundos. */
    public static final int CALMA = 80;
    /** Sinal: a Magia não volta por esses ticks (o Overdrive). Zero acaba a pausa. */
    public static final String SEM_GANHO = "sem_ganho";
    /** A pausa mais longa que um sinal pode pedir: 60 segundos. */
    static final int PAUSA_MAXIMA = 1200;
    static final int COR = 0x7A5CFF;

    private double atual = MAXIMO;
    /** Ticks desde o último golpe recebido; começa calmo. */
    private int semApanhar = CALMA;
    /** Ticks que faltam da pausa do ganho. */
    private int semGanho;

    @Override
    public String nome() {
        return "Magia";
    }

    @Override
    public int cor() {
        return COR;
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
        if (!pode(custo)) throw new IllegalStateException("magia insuficiente: " + atual + " < " + custo);
        atual = Math.max(0, atual - custo);
    }

    @Override
    public void sinal(String nome, int ticks) {
        if (SEM_GANHO.equals(nome)) semGanho = Math.max(0, Math.min(PAUSA_MAXIMA, ticks));
    }

    @Override
    public void tick(EstadoDoJogador estado) {
        // O ganho deste tick usa a calma de antes dele: os 4 s de espera ainda são no ganho normal.
        boolean calmo = semApanhar >= CALMA && !estado.apanhou();
        semApanhar = estado.apanhou() ? 0 : Math.min(CALMA, semApanhar + 1);
        if (semGanho > 0) {
            semGanho--;
            return;
        }
        atual = Math.min(MAXIMO, atual + (calmo ? GANHO_CALMO : GANHO));
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
