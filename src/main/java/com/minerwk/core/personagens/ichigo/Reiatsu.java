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
    /** Id da forma Hollow completa: nela o reiatsu não sobe nem desce. */
    public static final String HOLLOW = "hollow";
    public static final double MAXIMO = 200;
    /** Por tick no Shikai: 2,5 por segundo (era 1,5 até o balanceamento de 12/11). */
    public static final double GANHO = 2.5 / 20;
    /** Por tick no Shikai depois de {@link #CALMA} ticks sem apanhar: 5 por segundo. */
    public static final double GANHO_CALMO = 5.0 / 20;
    /** Ticks sem apanhar para o ganho dobrar: 4 segundos. */
    public static final int CALMA = 80;
    /** Por tick no Bankai: 1 por segundo (eram 2). */
    public static final double DRENO = 1.0 / 20;
    static final int COR_SHIKAI = 0x8FD8FF;
    static final int COR_BANKAI = 0x8E1B1B;

    /** Sinal: o Bankai para de drenar por esses ticks (a máscara Hollow, a carga do Mugetsu). Zero acaba a pausa. */
    public static final String SEM_DRENO = "sem_dreno";
    /** Sinal: o reiatsu vai a zero e fica selado por esses ticks, sem regenerar (a perda dos poderes). */
    public static final String SELAR = "selar";
    /** O selo mais longo que existe: 60 segundos. Um valor salvo nunca sela por mais do que isso. */
    public static final int SELO_MAXIMO = 1200;

    private double atual = MAXIMO;
    /** Da árvore: a reserva maior, o ganho mais rápido no Shikai e o Bankai que escorre menos. */
    private double fatorMaximo = 1, fatorFluxo = 1, fatorDreno = 1;
    private boolean bankai;
    /** Ticks que faltam da pausa do dreno; só vale dentro do Bankai. */
    private int semDreno;
    /** Ticks que faltam do selo. */
    private int selado;
    private boolean hollow;
    /** O que o Bankai drenou no último tick: uma pausa pedida nesse tick devolve isso. */
    private double ultimoDreno;
    /** Ticks desde o último golpe recebido; começa calmo. */
    private int semApanhar = CALMA;

    @Override
    public void melhorias(com.minerwk.core.maestria.Melhorias melhorias) {
        fatorMaximo = melhorias.mais(ArvoreIchigo.REIATSU_RESERVA);
        fatorFluxo = melhorias.mais(ArvoreIchigo.REIATSU_FLUXO);
        fatorDreno = melhorias.menos(ArvoreIchigo.BANKAI_FOLEGO);
        atual = Math.min(atual, maximo());
    }

    @Override
    public void forma(String forma) {
        bankai = BANKAI.equals(forma);
        hollow = HOLLOW.equals(forma);
        // A pausa é de quem está no Bankai: sair dele a desfaz.
        if (!bankai) semDreno = 0;
        ultimoDreno = 0;
    }

    @Override
    public void sinal(String nome, int ticks) {
        if (SEM_DRENO.equals(nome)) {
            if (!bankai) return;
            semDreno = Math.max(0, ticks);
            // O kit avisa depois de o tick do recurso já ter rodado: a pausa vale desde este tick.
            if (semDreno > 0) atual = Math.min(maximo(), atual + ultimoDreno);
            ultimoDreno = 0;
        } else if (SELAR.equals(nome)) {
            selado = Math.max(0, Math.min(SELO_MAXIMO, ticks));
            if (selado > 0) atual = 0;
        }
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
        return Math.round(MAXIMO * fatorMaximo);
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
        ultimoDreno = 0;
        if (hollow) return;
        if (selado > 0) {
            selado--;
            atual = 0;
        } else if (bankai) {
            if (semDreno > 0) {
                semDreno--;
            } else {
                ultimoDreno = Math.min(atual, DRENO * fatorDreno);
                atual -= ultimoDreno;
            }
        } else {
            atual = Math.min(maximo(), atual + (semApanhar >= CALMA ? GANHO_CALMO : GANHO) * fatorFluxo);
        }
    }

    /** O valor salvo: o reiatsu, ou os ticks de selo que faltam com o sinal trocado. */
    @Override
    public double salvar() {
        return selado > 0 ? -selado : atual;
    }

    @Override
    public void carregar(double valor) {
        selado = 0;
        ultimoDreno = 0;
        if (!Double.isFinite(valor)) {
            atual = 0;
        } else if (valor < 0) {
            selado = (int) Math.min(SELO_MAXIMO, Math.ceil(-valor));
            atual = 0;
        } else {
            atual = Math.min(maximo(), valor);
        }
    }
}
