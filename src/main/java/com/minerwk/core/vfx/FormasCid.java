package com.minerwk.core.vfx;

/**
 * As formas e os tempos dos visuais do Cid, sem classes do Minecraft: o cliente só desenha. Os tempos
 * repetem os do kit ({@code core/personagens/cid/Cid}); o {@code FormasCidTest} confere que batem.
 */
public final class FormasCid {
    /** Z: três cortes, um a cada 5 ticks; cada arco vive 7 e dá quase uma volta. */
    public static final int CORTES = 3, ENTRE_CORTES = 5, VIDA_DO_CORTE = 7;
    /** Quanto o visual do Redemoinho dura: até o último corte acabar de sumir (mais que o golpe). */
    public static final int REDEMOINHO = (CORTES - 1) * ENTRE_CORTES + VIDA_DO_CORTE;
    /** O comprimento da cauda do arco e o quanto o plano de cada corte tomba. */
    public static final double CAUDA = Math.PI * 1.1, TOMBO = .16;
    /** X: quanto duram os riscos da partida e o caminho que eles mostram. */
    public static final int RISCOS = 9;
    public static final double CAMINHO = 9;
    /** C: o passo do slime por tick; as correntes, quantas são, de onde saem e em quantos ticks fecham. */
    public static final double PASSO_DO_SLIME = 1.6, ALCANCE_DO_SLIME = 16, RAIO_DA_PRISAO = 1.7, MEIO_DO_ALVO = .9;
    /** O traje de slime amortecendo um golpe: quanto o respingo dura. */
    public static final int TRAJE = 12;
    public static final int CORRENTES = 4, FECHAR = 4, SOLTAR = 6;
    /** G: o estouro ao ligar, os fios em volta do corpo e a saída. */
    public static final int ESTOURO = 8, FIOS = 12, SAIDA_DA_AURA = 10;

    private FormasCid() {
    }

    /** A opacidade de um corte do Redemoinho com {@code idade} ticks: entra de uma vez e some até o fim. */
    public static double corte(double idade) {
        if (idade < 0 || idade >= VIDA_DO_CORTE) return 0;
        return Math.min(1, idade * 2 + .4) * (1 - Math.pow(idade / VIDA_DO_CORTE, 2));
    }

    /** O ângulo que a ponta do corte já varreu: rápido no começo, freando no fim (quase uma volta). */
    public static double angulo(double idade) {
        double u = Math.max(0, Math.min(1, idade / VIDA_DO_CORTE));
        return Math.PI * 1.9 * (1 - Math.pow(1 - u, 2.2));
    }

    /** A opacidade do vulto da Pós-imagem: firme no começo, piscando e sumindo no último terço. */
    public static double vulto(double t, int duracao) {
        if (t < 0 || t >= duracao) return 0;
        double u = t / duracao;
        if (u < .66) return 1;
        double some = 1 - (u - .66) / .34;
        return some * (.65 + .35 * Math.sin(t * 2.4));
    }

    /** Quanto o slime já andou: para no alcance. */
    public static double voo(double t) {
        return Math.min(ALCANCE_DO_SLIME, Math.max(0, t) * PASSO_DO_SLIME);
    }

    /** A opacidade do slime em voo: some no último bloco do alcance, se não bateu em nada. */
    public static double slime(double t) {
        return Math.max(0, Math.min(1, ALCANCE_DO_SLIME - Math.max(0, t) * PASSO_DO_SLIME + 1));
    }

    /** A opacidade das correntes: fecham em {@link #FECHAR} ticks e soltam nos últimos {@link #SOLTAR}. */
    public static double correntes(double t, int duracao) {
        if (t < 0 || t >= duracao) return 0;
        return Math.min(1, t / 2 + .3) * Math.min(1, (duracao - t) / SOLTAR);
    }

    /** A opacidade da aura do Overdrive. */
    public static double aura(double t, int duracao) {
        if (t < 0 || t >= duracao) return 0;
        return Math.min(1, t / 3) * Math.min(1, (duracao - t) / SAIDA_DA_AURA);
    }

    public static double raioDoEstouro(double t) {
        return .6 + 3.4 * (1 - Math.pow(1 - Math.min(1, Math.max(0, t) / ESTOURO), 2));
    }

    /** A altura (0 a 1) do fio {@code k} da aura no tick {@code t}: cada um sobe no seu passo e recomeça. */
    public static double fio(int k, double t) {
        double x = Math.sin(k * 127.1 + 311.7) * 43758.5453;
        double fase = x - Math.floor(x);
        double v = t / (11 + 5 * fase) + fase;
        return v - Math.floor(v);
    }
}
