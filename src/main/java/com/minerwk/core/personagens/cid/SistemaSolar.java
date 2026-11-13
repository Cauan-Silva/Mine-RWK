package com.minerwk.core.personagens.cid;

import com.minerwk.core.kit.Vetor;
import java.util.List;

/**
 * A cena do recuo do Atomic: o sistema solar visto por uma câmera virtual que se afasta da Terra. A Terra fica
 * parada no centro da cena, com raio 1; tudo está na base da cutscene (lado, cima, frente), e os tempos são em
 * ticks desde o "atomic". No começo a câmera está colada na Terra, que toma a metade de baixo do céu como o
 * planeta de sempre; no fim ela está longe (500 raios da Terra), atrás da câmera da cutscene, e o sistema inteiro cabe no quadro.
 * Sem classes do Minecraft: o céu do espaço (no cliente) desenha o que estas contas dizem.
 */
public final class SistemaSolar {
    /** O recuo começa depois do estouro; a Terra vira uma bola inteira; o sistema inteiro aparece e o plano para. */
    public static final int INICIO = 10, TERRA_INTEIRA = 45, SISTEMA = 95;
    /** O último pulso do ponto roxo, que se abre até tomar a tela e emenda no branco. */
    public static final int PULSO = 95, FIM_DO_PULSO = 120;
    /** O menor tamanho com que um astro é desenhado (meio ângulo): longe, a Terra ainda é um ponto visível. */
    public static final double MINIMO = Math.toRadians(.5);
    /** Até onde o planeta de hoje sobe, medido do ponto bem embaixo (o mesmo do céu do espaço). */
    public static final double BORDA = Math.toRadians(62);
    /** Onde o clarão está: no topo da Terra, onde os dois estavam. */
    public static final Vetor PONTO = new Vetor(0, 1.02, 0);

    /** Um astro: raio (em raios da Terra), órbita em volta do Sol, fase inicial, cor e se tem anel. */
    public record Astro(String nome, double raio, double orbita, double fase, int cor, boolean anel) {
    }

    public static final Astro SOL = new Astro("sol", 12, 0, 0, 0xFFE6A8, false);
    public static final Astro TERRA = new Astro("terra", 1, 60, 0, 0x2F6FD0, false);
    public static final List<Astro> ASTROS = List.of(SOL,
            new Astro("mercurio", .4, 25, 2.0, 0xA89E94, false),
            new Astro("venus", .95, 40, 4.0, 0xE3C98F, false),
            TERRA,
            new Astro("marte", .55, 80, 1.0, 0xC8635A, false),
            new Astro("jupiter", 6, 140, 3.6, 0xD9B48A, false),
            new Astro("saturno", 5, 190, 5.2, 0xE8D1A0, true),
            new Astro("urano", 2.5, 240, 2.6, 0x9FD8E0, false),
            new Astro("netuno", 2.4, 280, 4.6, 0x4D72D8, false));

    /** O Sol fica de lado da Terra, à distância da órbita dela. */
    private static final Vetor CENTRO_DO_SOL = new Vetor(-TERRA.orbita(), 0, 0);
    /** Para onde a câmera da cutscene olha no fim do recuo. */
    public static final Vetor OLHAR = new Vetor(0, 0, AtomicFinal.AO_LADO / 2).menos(CutsceneAtomic.LONGE).normalizado();
    private static final Vetor CIMA = new Vetor(0, 1, 0);
    /** A distância da câmera virtual em que a Terra toma o céu até {@link #BORDA}; a da Terra inteira; a do fim. */
    private static final double PERTO = 1 / Math.sin(BORDA), INTEIRA = 4, LONGE = 500;
    /** A velocidade de uma órbita como a da Terra, em radianos por tick (as de dentro giram mais depressa). */
    private static final double GIRO = .004;

    private SistemaSolar() {
    }

    private static double suave(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    /**
     * O quanto a câmera já virou do alto da Terra para trás da câmera da cutscene, de 0 a 1. Vira junto com a Terra
     * encolhendo: quando ela é uma bola inteira, já está no meio do quadro (a câmera da cutscene chega lá no tick 18).
     */
    public static double recuo(double t) {
        return suave((t - INICIO) / (TERRA_INTEIRA - INICIO));
    }

    /** A distância da câmera virtual ao centro da Terra, em raios da Terra. */
    public static double distancia(double t) {
        if (t <= TERRA_INTEIRA) return PERTO + (INTEIRA - PERTO) * suave((t - INICIO) / (TERRA_INTEIRA - INICIO));
        return INTEIRA * Math.pow(LONGE / INTEIRA, suave((t - TERRA_INTEIRA) / (SISTEMA - TERRA_INTEIRA)));
    }

    /** Onde está a câmera virtual: sobe do alto da Terra e vai para trás dela, na linha do olhar da cutscene. */
    public static Vetor camera(double t) {
        double r = recuo(t);
        Vetor rumo = CIMA.vezes(1 - r).mais(OLHAR.vezes(-r)).normalizado();
        return rumo.vezes(distancia(t));
    }

    /** O centro do astro: o Sol parado ao lado da Terra, a Terra no centro e os outros girando devagar. */
    public static Vetor posicao(Astro a, double t) {
        if (a == SOL) return CENTRO_DO_SOL;
        if (a == TERRA) return Vetor.ZERO;
        double angulo = a.fase() + t * GIRO * Math.pow(TERRA.orbita() / a.orbita(), 1.5);
        return CENTRO_DO_SOL.mais(new Vetor(Math.cos(angulo), 0, Math.sin(angulo)).vezes(a.orbita()));
    }

    /** A direção da câmera virtual até o astro (comprimento 1). */
    public static Vetor direcao(Astro a, double t) {
        return posicao(a, t).menos(camera(t)).normalizado();
    }

    /** A direção da câmera virtual até um ponto qualquer da cena (o clarão, um ponto de uma órbita). */
    public static Vetor direcao(Vetor ponto, double t) {
        return ponto.menos(camera(t)).normalizado();
    }

    /** Meio ângulo do astro visto da câmera virtual, em radianos, nunca menor que {@link #MINIMO}. */
    public static double raioAparente(Astro a, double t) {
        double d = posicao(a, t).distancia(camera(t));
        return Math.max(MINIMO, Math.asin(Math.min(1, a.raio() / d)));
    }

    /** A esfera roxa do mundo, que se apaga enquanto o ponto roxo do céu assume (de 1 a 0). */
    public static double esferaNoMundo(double t) {
        return 1 - suave((t - INICIO) / (TERRA_INTEIRA - INICIO));
    }

    /** A força do ponto roxo na Terra: acende enquanto a esfera do mundo se apaga e pulsa até o fim. */
    public static double pontoRoxo(double t) {
        double acende = suave((t - INICIO) / (TERRA_INTEIRA - INICIO));
        if (t <= TERRA_INTEIRA) return acende;
        return 1 + .25 * Math.sin((t - TERRA_INTEIRA) * .35);
    }

    /** O meio ângulo do ponto roxo: pequeno até o pulso final, que se abre acelerando até tomar a tela. */
    public static double raioDoPonto(double t) {
        double u = Math.max(0, Math.min(1, (t - PULSO) / (FIM_DO_PULSO - PULSO)));
        return Math.toRadians(1.5 + 98.5 * Math.pow(u, 2.2));
    }

    /** A lua e os dois planetas do céu de hoje, que somem no começo do recuo (de 1 a 0). */
    public static double astrosAntigos(double t) {
        return 1 - suave((t - INICIO) / (TERRA_INTEIRA - INICIO));
    }
}
