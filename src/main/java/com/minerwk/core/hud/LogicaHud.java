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

    /** Quantos ticks dura o brilho de "ficou pronto". */
    public static final int BRILHO_TICKS = 8;

    /**
     * A parte escura da recarga num quadro de {@code lado} pixels, como um relógio: o claro cresce no sentido
     * horário a partir do topo e o escuro é o resto do giro. Devolve o polígono em pares x, y relativos ao canto do
     * quadro, começando no centro (para desenhar em leque); vazio sem recarga. {@code restante} acima de 1 conta 1.
     */
    public static float[] varredura(float restante, int lado) {
        if (!(restante > 0)) return new float[0];
        float r = Math.min(1f, restante);
        double inicio = 360 * (1 - r);
        float meio = lado / 2f;
        List<float[]> pontos = new java.util.ArrayList<>();
        pontos.add(new float[] {meio, meio});
        pontos.add(noQuadrado(inicio, meio));
        for (double canto : new double[] {45, 135, 225, 315}) {
            if (canto > inicio) pontos.add(noQuadrado(canto, meio));
        }
        pontos.add(new float[] {meio, 0});
        float[] saida = new float[pontos.size() * 2];
        for (int i = 0; i < pontos.size(); i++) {
            saida[2 * i] = pontos.get(i)[0];
            saida[2 * i + 1] = pontos.get(i)[1];
        }
        return saida;
    }

    /** O ponto da borda do quadrado no ângulo dado, em graus no sentido horário a partir do topo. */
    private static float[] noQuadrado(double graus, float meio) {
        double a = Math.toRadians(graus);
        double dx = Math.sin(a), dy = -Math.cos(a);
        double escala = meio / Math.max(Math.abs(dx), Math.abs(dy));
        return new float[] {(float) (meio + dx * escala), (float) (meio + dy * escala)};
    }

    /** A força do brilho de "ficou pronto": 1 no tick em que ficou pronto, caindo em linha reta até 0. */
    public static float brilho(long ticksDesdePronta) {
        if (ticksDesdePronta < 0 || ticksDesdePronta >= BRILHO_TICKS) return 0;
        return 1 - ticksDesdePronta / (float) BRILHO_TICKS;
    }
}
