package com.minerwk.core.vfx;

/**
 * Tempos e formas dos outros visuais do Ichigo: o Shunpo, a onda do Getsuga no chão, a Zangetsu
 * arremessada, a entrada do Bankai e o Getsuga na lâmina. Só aparência, menos os números marcados como
 * do golpe, que o kit usa também.
 */
public final class FormasIchigo {
    /** Do golpe: ticks de ida da Zangetsu arremessada (a volta leva o mesmo) e até onde ela vai. */
    public static final int ARREMESSO_IDA = 8;
    public static final double ARREMESSO_ALCANCE = 10, ARREMESSO_PASSO = ARREMESSO_ALCANCE / ARREMESSO_IDA;
    /** Do golpe: raio da onda do Getsuga no chão. */
    public static final double ONDA_RAIO = 5;
    /** Quanto cada visual fica na tela, em ticks. */
    public static final int SHUNPO = 9, ONDA = 16, BANKAI = 34, SAIR = 14, ARREMESSO = ARREMESSO_IDA * 2;
    /** O rastro do clique da espada: ticks na tela, ticks que a lâmina leva para varrer o arco e meia abertura. */
    public static final int CORTE = 7;
    private static final double CORTE_VARRE = 2.5;
    public static final double CORTE_ABERTURA = Math.toRadians(80);
    /** Ticks até a onda chegar ao raio cheio, e até o pilar do Bankai subir. */
    private static final double ONDA_ABRE = 6, PILAR_SOBE = 4;

    private FormasIchigo() {
    }

    private static double suave(double t) {
        return PerfilVfxGojo.suave(t);
    }

    /** Até onde a Zangetsu vai com um bloco a {@code bloqueio} blocos: ela anda em passos inteiros. */
    public static double alcanceDoArremesso(double bloqueio) {
        return Math.min(ARREMESSO_ALCANCE, Math.floor(Math.max(0, bloqueio) / ARREMESSO_PASSO + 1e-9) * ARREMESSO_PASSO);
    }

    /** Distância da Zangetsu à saída no tick {@code t}: vai até {@code alcance}, para e volta até a mão. */
    public static double arremesso(double t, double alcance) {
        if (t <= 0) return 0;
        if (t <= ARREMESSO_IDA) return Math.min(alcance, ARREMESSO_PASSO * t);
        return alcance * Math.max(0, 1 - (t - ARREMESSO_IDA) / ARREMESSO_IDA);
    }

    /** Ângulo do giro da espada no ar (radianos): quase uma volta a cada 3 ticks. */
    public static double giro(double t) {
        return t * 1.9;
    }

    /** Raio da onda no chão: abre depressa e freia ao chegar ao raio do golpe. */
    public static double raioDaOnda(double t) {
        double x = Math.max(0, Math.min(1, t / ONDA_ABRE));
        return ONDA_RAIO * (1 - (1 - x) * (1 - x));
    }

    /** Altura da parede da onda: sobe com ela e baixa enquanto some. */
    public static double alturaDaOnda(double t) {
        return 1.9 * suave(t / 3) * (1 - suave((t - ONDA_ABRE) / (ONDA - ONDA_ABRE)));
    }

    /** Raio do pilar de reiatsu da entrada do Bankai: abre de uma vez e afina até sumir. */
    public static double raioDoPilar(double t) {
        return 1.7 * suave(t / 2) * (1 - suave((t - 10) / (BANKAI - 14.0)));
    }

    public static double alturaDoPilar(double t) {
        return 16 * suave(t / PILAR_SOBE);
    }

    /** Raio do anel de poeira que corre pelo chão na entrada do Bankai. */
    public static double anelDoBankai(double t) {
        double x = Math.max(0, Math.min(1, t / 10));
        return 1 + 6 * (1 - (1 - x) * (1 - x));
    }

    /** Raio do rastro do clique: o alcance da espada no Shikai, no Bankai e com o Getsuga na lâmina. */
    public static double raioDoCorte(int forma) {
        return forma >= 2 ? 4.6 : forma == 1 ? 2.3 : 2.1;
    }

    /** Até que ângulo a lâmina já varreu no tick {@code t}: de -abertura a +abertura. */
    public static double frenteDoCorte(double t) {
        return -CORTE_ABERTURA + 2 * CORTE_ABERTURA * suave(t / CORTE_VARRE);
    }

    /** Opacidade do rastro no ângulo {@code a}: cheio logo atrás da lâmina, sumindo para trás e com o tempo. */
    public static double corte(double a, double t) {
        double frente = frenteDoCorte(t);
        if (t < 0 || a > frente + 1e-9) return 0;
        double atras = (frente - a) / (2 * CORTE_ABERTURA);
        return Math.max(0, 1 - atras * .8) * Math.max(0, 1 - t / CORTE);
    }

    /** Opacidade da pós-imagem {@code k} (0 é a da partida) do Shunpo: as de trás somem primeiro. */
    public static double posImagem(int k, double t) {
        return t < 0 ? 0 : .55 * Math.max(0, 1 - t / (4 + 2 * k));
    }
}
