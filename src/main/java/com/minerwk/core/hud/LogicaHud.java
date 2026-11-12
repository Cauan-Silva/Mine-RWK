package com.minerwk.core.hud;

import com.minerwk.core.kit.Tecla;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Contas do HUD de recargas e recurso, sem Minecraft (testadas com JUnit). */
public final class LogicaHud {
    private LogicaHud() {
    }

    /** Quanto da recarga ainda falta, de 1 (acabou de começar) a 0 (pronta). Sempre em [0, 1]. */
    public static float fracao(long prontaNoTick, int duracao, long agora) {
        if (duracao <= 0) return 0f;
        float f = (prontaNoTick - agora) / (float) duracao;
        return Math.max(0f, Math.min(1f, f));
    }

    /**
     * Texto dos segundos restantes: a partir de 1 s, inteiro arredondado para cima;
     * abaixo de 1 s, uma casa arredondada para baixo (mínimo 0.1). Vazio se pronta.
     */
    public static String segundos(long restanteTicks) {
        if (restanteTicks <= 0) return "";
        if (restanteTicks >= 20) return Long.toString((restanteTicks + 19) / 20);
        long decimos = Math.max(1, restanteTicks / 2);
        return "0." + decimos;
    }

    /** Quanto da barra de XP está cheia, em [0, 1]; cheia no nível máximo (que não pede mais XP). */
    public static double fracaoXp(int xpNoNivel, int xpProximo) {
        if (xpProximo <= 0) return 1;
        return Math.max(0, Math.min(1, xpNoNivel / (double) xpProximo));
    }

    public static boolean semRecurso(double atual, double custo) {
        return atual + 1e-9 < custo;
    }

    /** As teclas que o personagem tem, na ordem Z X C V G R. */
    public static List<Tecla> teclasVisiveis(Set<Tecla> doPersonagem) {
        return Arrays.stream(Tecla.values()).filter(doPersonagem::contains).toList();
    }

    /** Recarga a partir da qual todos os golpes parados juntos contam como perda de poderes: 10 segundos. */
    static final long LONGA = 200;

    /**
     * Se o jogador está sem poderes (o custo do Mugetsu do Ichigo): sem recurso e com todos os golpes da forma
     * em recarga longa. Devolve os ticks que faltam para o último voltar, ou 0 se não é o caso.
     */
    public static long semPoderes(double recurso, List<Long> restantes) {
        if (recurso > 1e-9 || restantes.isEmpty()) return 0;
        long maior = 0;
        for (long r : restantes) {
            if (r < LONGA) return 0;
            maior = Math.max(maior, r);
        }
        return maior;
    }
}
