package com.minerwk.core.maestria;

/**
 * Regras da maestria de um personagem, sem classes do Minecraft: a curva de XP, o nível, os pontos de árvore
 * e quanto XP cada fonte dá. O que se guarda do jogador é só o XP total; o resto sai dele.
 */
public final class Maestria {
    public static final int NIVEL_MAX = 100;
    /** XP por abater um inimigo com um golpe do kit. */
    public static final int XP_ABATE = 25;
    /** Teto de XP por um golpe num inimigo. */
    public static final int XP_MAX_GOLPE = 20;
    /** XP total de quem chegou ao nível máximo. */
    public static final long XP_MAX = xpAte(NIVEL_MAX);

    private Maestria() {
    }

    /** XP para passar do {@code nivel} para o seguinte; 0 no nível máximo. */
    public static int xpParaProximo(int nivel) {
        if (nivel >= NIVEL_MAX) return 0;
        return 100 + 10 * (Math.max(1, nivel) - 1);
    }

    /** XP total de quem acabou de chegar ao {@code nivel}. */
    public static long xpAte(int nivel) {
        long soma = 0;
        for (int n = 1; n < Math.min(nivel, NIVEL_MAX); n++) soma += xpParaProximo(n);
        return soma;
    }

    public static int nivel(long xpTotal) {
        return progresso(xpTotal).nivel();
    }

    /** Onde o jogador está: nível, XP dentro do nível e quanto falta ao todo para o próximo. */
    public static Progresso progresso(long xpTotal) {
        long resto = Math.max(0, xpTotal);
        int nivel = 1;
        while (nivel < NIVEL_MAX && resto >= xpParaProximo(nivel)) {
            resto -= xpParaProximo(nivel);
            nivel++;
        }
        if (nivel == NIVEL_MAX) return new Progresso(nivel, 0, 0);
        return new Progresso(nivel, (int) resto, xpParaProximo(nivel));
    }

    /** Pontos de árvore ganhos até o {@code nivel}: um por nível subido. */
    public static int pontos(int nivel) {
        return Math.max(0, Math.min(nivel, NIVEL_MAX) - 1);
    }

    /** XP por {@code dano} causado com um golpe do kit: 1 por ponto, com teto; só inimigo dá XP. */
    public static int xpPorDano(float dano, boolean inimigo) {
        if (!inimigo || !(dano > 0)) return 0;
        return (int) Math.min(XP_MAX_GOLPE, Math.ceil(dano));
    }

    public static int xpPorAbate(boolean inimigo) {
        return inimigo ? XP_ABATE : 0;
    }

    /** O XP total depois de ganhar {@code ganho}, sem passar do teto e sem voltar atrás. */
    public static long somar(long xpTotal, long ganho) {
        return Math.min(XP_MAX, Math.max(0, xpTotal) + Math.max(0, ganho));
    }

    public record Progresso(int nivel, int xpNoNivel, int xpParaProximo) {
    }
}
