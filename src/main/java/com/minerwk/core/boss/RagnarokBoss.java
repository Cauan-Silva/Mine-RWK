package com.minerwk.core.boss;

import java.util.List;

/**
 * Os números e as regras do Ragnarok (o demônio do 4º Reino, Nêmesis do Cid), sem classes do Minecraft. O que
 * cada golpe faz no mundo fica na entidade; aqui ficam os tempos, os danos e as contas.
 */
public final class RagnarokBoss {
    public static final String ID = "ragnarok";
    /** 16 barras de coração. */
    public static final float VIDA = 320;
    public static final int XP_VITORIA = 500;
    /** Colossal: uns 7 blocos de altura. */
    public static final float LARGURA = 3.2f, ALTURA = 7f;

    /** Um murro que abre cratera no ponto marcado; "soco" dura 30 ticks. */
    public static final Golpe SOCO = new Golpe("soco", "soco", 16, 2, 12, 50, 0, 10, 4);
    /** Quatro socos seguidos, cada um mirado de novo; "barragem" dura 64 ticks. */
    public static final Golpe BARRAGEM = new Golpe("barragem", "barragem", 14, 34, 16, 120, 0, 10, 3);
    /** Um leque de cinco projéteis de magia saído das asas; "rajada" dura 46 ticks. */
    public static final Golpe RAJADA = new Golpe("rajada", "rajada", 18, 18, 10, 100, 8, 44, 3);
    /** Sobe, marca uma linha no chão e atravessa a arena; "investida" dura 60 ticks. */
    public static final Golpe INVESTIDA = new Golpe("investida", "investida", 30, 14, 16, 170, 8, 44, 2);
    /** 2 s de carga na boca e um feixe que varre devagar para o lado; "sopro" dura 100 ticks. */
    public static final Golpe SOPRO = new Golpe("sopro", "sopro", 40, 40, 20, 220, 6, 44, 2);
    /** Fase 2: um pisão que manda uma onda pelo chão, pulável; "pisao" dura 46 ticks. */
    public static final Golpe PISAO = new Golpe("pisao", "pisao", 18, 16, 12, 120, 0, 14, 3);

    public static final List<Golpe> GOLPES = List.of(SOCO, BARRAGEM, RAJADA, INVESTIDA, SOPRO);
    /** Sem uma asa ele não voa mais: sai a investida, entra o pisão. */
    public static final List<Golpe> GOLPES_SEM_ASA = List.of(SOCO, BARRAGEM, RAJADA, PISAO, SOPRO);

    public static List<Golpe> golpes(int fase) {
        return fase <= 1 ? GOLPES : GOLPES_SEM_ASA;
    }

    // --- Fases ---
    /** Abaixo de 50% de vida o Shadow corta uma asa. */
    public static final double VIDA_DA_ASA = .5;
    /** Sem a asa as recargas e a folga ficam com 70% do tempo. */
    public static final double RITMO_SEM_ASA = .7;
    public static final double PASSO_NORMAL = 1.0, PASSO_SEM_ASA = 1.25;
    /** Ticks da cena da asa cortada, em que ele não ataca, e quando a asa some do corpo. */
    public static final int TROCA_DE_FASE = 60, ASA_CAI = 20;
    /** Ticks da entrada (o rugido), antes do primeiro golpe, e a espera pelo cenário de quem acabou de chegar. */
    public static final int ENTRADA = 50, CHEGADA = 40;
    /** Folga entre dois golpes. */
    public static final int INTERVALO = 22;
    /** A luta acaba sozinha aos 10 minutos. */
    public static final int TEMPO_LIMITE = 10 * 60 * 20;
    public static final double ALCANCE_DE_VISAO = 96;

    public static int fase(double fracaoDeVida) {
        return fracaoDeVida <= VIDA_DA_ASA ? 2 : 1;
    }

    public static double ritmo(int fase) {
        return fase <= 1 ? 1 : RITMO_SEM_ASA;
    }

    // --- Calor ---
    /** O corpo em chamas fere quem fica colado: uma vez por segundo, medido da beirada do corpo. */
    public static final int CALOR_A_CADA = 20;

    public static double raioDoCalor(int fase) {
        return fase <= 1 ? 2 : 3;
    }

    public static float danoDoCalor(int fase) {
        return fase <= 1 ? 1 : 2;
    }

    /** Se o calor alcança quem está a {@code distancia} do centro dele (no plano) e {@code acima} dos pés dele. */
    public static boolean noCalor(double distancia, double acima, int fase) {
        return distancia - LARGURA / 2 <= raioDoCalor(fase) && acima > -2 && acima < ALTURA + 1;
    }

    // --- Soco em chamas ---
    public static final float DANO_SOCO = 10;
    public static final double RAIO_SOCO = 3;
    /** Até onde o braço alcança, do centro dele: o ponto marcado nunca fica mais longe que isso. */
    public static final double ALCANCE_DO_BRACO = 7;

    /** A que distância dele o murro cai, para um alvo a {@code distancia}: no alvo, até onde o braço dá. */
    public static double pontoDoSoco(double distancia) {
        return Math.max(LARGURA / 2 + .5, Math.min(ALCANCE_DO_BRACO, distancia));
    }

    // --- Barragem ---
    public static final float DANO_BARRAGEM = 7;
    public static final double RAIO_BARRAGEM = 2.5;
    /** Um soco a cada 10 ticks da janela; cada um é mirado de novo 6 ticks antes, quando a marca aparece. */
    public static final int SOCOS_DA_BARRAGEM = 4, ENTRE_SOCOS = 10, MIRA_DA_BARRAGEM = 6;

    public static boolean socoDaBarragem(int tickNaJanela) {
        return tickNaJanela >= 0 && tickNaJanela % ENTRE_SOCOS == 0 && tickNaJanela / ENTRE_SOCOS < SOCOS_DA_BARRAGEM;
    }

    /** Se neste tick da janela ele mira o soco seguinte (o primeiro é mirado no aviso). */
    public static boolean miraDaBarragem(int tickNaJanela) {
        return tickNaJanela >= 0 && tickNaJanela % ENTRE_SOCOS == ENTRE_SOCOS - MIRA_DA_BARRAGEM
                && tickNaJanela / ENTRE_SOCOS + 1 < SOCOS_DA_BARRAGEM;
    }

    // --- Rajada das asas ---
    public static final float DANO_RAJADA = 6;
    public static final int PROJETEIS = 5;
    /** O leque inteiro, em graus, e o que cada projétil anda por tick; todos saem juntos quando a janela abre. */
    public static final double LEQUE = 60, PASSO_RAJADA = 1.4, ALCANCE_RAJADA = 40, RAIO_RAJADA = .9;

    /** O desvio do projétil {@code i} (0 a 4) em relação à mira, em graus: de −30 a +30. */
    public static double desvioDaRajada(int i) {
        return -LEQUE / 2 + LEQUE * i / (PROJETEIS - 1);
    }

    // --- Investida voando ---
    public static final float DANO_INVESTIDA = 12;
    public static final double COMPRIMENTO_INVESTIDA = 34, RAIO_INVESTIDA = 2.6, ALTURA_DO_VOO = 5;

    /**
     * Quanto dá para andar de ({@code x}, {@code z}), medidos do centro de um quadrado de meio lado
     * {@code meia}, no rumo ({@code dx}, {@code dz}) de comprimento 1, sem sair dele. Zero se já está fora.
     */
    public static double ateABeirada(double x, double z, double dx, double dz, double meia) {
        double ate = Double.MAX_VALUE;
        if (dx > 1e-9) ate = Math.min(ate, (meia - x) / dx);
        if (dx < -1e-9) ate = Math.min(ate, (-meia - x) / dx);
        if (dz > 1e-9) ate = Math.min(ate, (meia - z) / dz);
        if (dz < -1e-9) ate = Math.min(ate, (-meia - z) / dz);
        return Math.max(0, ate);
    }

    /** Quanto da linha ele já atravessou no tick {@code tickNaJanela} (0 a 1): sai devagar e acelera. */
    public static double avancoDaInvestida(int tickNaJanela) {
        double x = Math.max(0, Math.min(1, (tickNaJanela + 1) / (double) INVESTIDA.janela()));
        return x * x * (3 - 2 * x);
    }

    /** A altura dele sobre o chão no tick {@code t} do golpe: sobe no aviso, rasante na janela, pousa na pausa. */
    public static double alturaNaInvestida(int t) {
        int aviso = INVESTIDA.aviso(), janela = INVESTIDA.janela();
        if (t < aviso) return ALTURA_DO_VOO * Math.min(1, t / (aviso * .6));
        if (t < aviso + janela) return ALTURA_DO_VOO * Math.max(0, 1 - (t - aviso + 1) / (janela * .35));
        return 0;
    }

    // --- Sopro do Demônio ---
    public static final float DANO_SOPRO = 16;
    public static final double ALCANCE_SOPRO = 30, RAIO_SOPRO = 1.3;
    /** O feixe varre este ângulo durante a janela, de um lado ao outro da mira travada. */
    public static final double VARREDURA = 50;
    /** A mira trava estes ticks antes do feixe: dá para sair do caminho. */
    public static final int SOPRO_TRAVA = 10;

    /** O desvio do feixe em graus no tick {@code tickNaJanela}; {@code sentido} é +1 ou −1 (para que lado varre). */
    public static double desvioDoSopro(int tickNaJanela, int sentido) {
        double x = Math.max(0, Math.min(1, tickNaJanela / (double) (SOPRO.janela() - 1)));
        return sentido * VARREDURA * (x - .5);
    }

    // --- Pisão ---
    public static final float DANO_PISAO = 8;
    public static final double RAIO_PISAO = 8, LARGURA_DA_ONDA = 1.2;
    /** Quem está mais alto que isto acima do chão dele passa por cima da onda. */
    public static final double PULO = .6;

    /** O raio da onda no tick {@code tickNaJanela}: de 1 a 8 ao longo da janela. */
    public static double raioDaOnda(int tickNaJanela) {
        double x = Math.max(0, Math.min(1, (tickNaJanela + 1) / (double) PISAO.janela()));
        return 1 + (RAIO_PISAO - 1) * x;
    }

    /** Se a onda pega quem está a {@code distancia} do centro e {@code acima} do chão dele neste tick. */
    public static boolean naOnda(int tickNaJanela, double distancia, double acima) {
        return acima <= PULO && Math.abs(distancia - raioDaOnda(tickNaJanela)) <= LARGURA_DA_ONDA;
    }

    private RagnarokBoss() {
    }

    /** @param invocadorPresente vivo, conectado e na mesma dimensão */
    public static boolean acabou(boolean invocadorPresente, int ticksDeLuta) {
        return !invocadorPresente || ticksDeLuta >= TEMPO_LIMITE;
    }
}
