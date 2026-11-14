package com.minerwk.core.arena;

import com.minerwk.core.arena.PlantaEscola.Bloco;
import com.minerwk.core.arena.PlantaEscola.Material;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A planta da arena do Zangetsu interior: o mundo interior do Ichigo, com os prédios deitados de lado. A luta
 * acontece em cima da fachada de um arranha-céu caído, uma grade de concreto e vidro com algumas janelas acesas;
 * em volta, outros prédios deitados em alturas diferentes, com antenas, caixas d'água e letreiros saindo de lado,
 * e um atravessado ao fundo. Posições relativas ao bloco do centro; {@code dy} 0 é a altura dos pés, então a
 * fachada fica em {@code dy} −1. Norte é dz negativo.
 */
public final class PlantaMundoInterior {
    private static final int BORDA = CaixaArena.MEIA;
    private static final int FUNDO = CaixaArena.Y_FUNDO - CaixaArena.Y_PATIO;
    /** Meia largura do prédio da luta: de −22 a +22 são 45 blocos. Ele atravessa a caixa de norte a sul. */
    public static final int MEIO_PREDIO = 22;
    /** A grade da fachada: uma viga a cada 5 blocos na largura e a cada 4 no comprimento. */
    private static final int VAO_LARGURA = 5, VAO_COMPRIMENTO = 4;
    /** O prédio alto, a leste: de x 28 a 46, com a fachada 10 blocos acima da luta. */
    public static final int ALTO_DE = 28, ALTO_ATE = 46, ALTO_TOPO = 9;
    /** O prédio baixo, a oeste: de x −46 a −30, com a fachada 9 blocos abaixo da luta. */
    public static final int BAIXO_DE = -46, BAIXO_ATE = -30, BAIXO_TOPO = -10;
    /** O prédio atravessado ao norte, por cima dos outros: de z −48 a −38, de dy 10 a 22. */
    public static final int CRUZADO_DE = -48, CRUZADO_ATE = -38, CRUZADO_BASE = 10, CRUZADO_TOPO = 22;
    /** Quantas janelas ficam acesas (luz por baixo do vidro) e quantas têm vidro claro. */
    private static final double ACESAS = .2, CLARAS = .12;

    private PlantaMundoInterior() {
    }

    private static void por(Map<Long, Bloco> blocos, int dx, int dy, int dz, Material m) {
        blocos.put(((long) (dx + 512) << 40) | ((long) (dy + 512) << 20) | (dz + 512), new Bloco(dx, dy, dz, m));
    }

    /** Viga ou vidro, pela posição na grade (as duas coordenadas correm pela face). */
    private static Material grade(int a, int b) {
        return Math.floorMod(a, VAO_LARGURA) == 0 || Math.floorMod(b, VAO_COMPRIMENTO) == 0 ? Material.CONCRETO_CLARO
                : Material.VIDRO_AZUL;
    }

    /** O vidro de uma janela: às vezes claro. */
    private static Material vidro(int dx, int dy, int dz) {
        return PlantaOriana.acaso(dx, dy, dz, 21) < CLARAS ? Material.VIDRO_CLARO : Material.VIDRO_AZUL;
    }

    /**
     * Uma face horizontal de fachada em {@code dy}: a grade, e por baixo dos vidros o escuro da sala, ou a luz de
     * uma janela acesa. Cada janela (um vão da grade) acende inteira.
     */
    private static void fachada(Map<Long, Bloco> blocos, int x0, int x1, int z0, int z1, int dy) {
        for (int dx = x0; dx <= x1; dx++) {
            for (int dz = z0; dz <= z1; dz++) {
                // A beirada é sempre viga: o prédio tem quina.
                Material m = dx == x0 || dx == x1 ? Material.CONCRETO_CLARO : grade(dx - x0, dz);
                if (m == Material.CONCRETO_CLARO) {
                    por(blocos, dx, dy, dz, m);
                    por(blocos, dx, dy - 1, dz, m);
                    continue;
                }
                int janelaX = Math.floorDiv(dx - x0, VAO_LARGURA), janelaZ = Math.floorDiv(dz, VAO_COMPRIMENTO);
                boolean acesa = PlantaOriana.acaso(janelaX + x0, dy, janelaZ, 20) < ACESAS;
                por(blocos, dx, dy, dz, vidro(janelaX + x0, dy, janelaZ));
                por(blocos, dx, dy - 1, dz, acesa ? Material.JANELA_ACESA : Material.FUNDO_ESCURO);
            }
        }
    }

    /** Uma face vertical virada para leste ou oeste, em {@code dx}, de {@code y0} a {@code y1}. */
    private static void lateral(Map<Long, Bloco> blocos, int dx, int y0, int y1, int z0, int z1) {
        for (int dy = y0; dy <= y1; dy++) {
            for (int dz = z0; dz <= z1; dz++) {
                Material m = grade(dy - y0 + 1, dz);
                por(blocos, dx, dy, dz, m == Material.VIDRO_AZUL ? vidro(dx, dy / VAO_LARGURA, dz / VAO_COMPRIMENTO) : m);
            }
        }
    }

    /** Uma face vertical virada para norte ou sul, em {@code dz}; algumas janelas acesas. */
    private static void frente(Map<Long, Bloco> blocos, int dz, int x0, int x1, int y0, int y1) {
        for (int dy = y0; dy <= y1; dy++) {
            for (int dx = x0; dx <= x1; dx++) {
                Material m = grade(dx, dy - y0 + 1);
                if (m == Material.VIDRO_AZUL) {
                    int jx = Math.floorDiv(dx, VAO_LARGURA), jy = Math.floorDiv(dy - y0 + 1, VAO_COMPRIMENTO);
                    m = PlantaOriana.acaso(jx, jy, dz, 22) < ACESAS ? Material.JANELA_ACESA : vidro(jx, jy, dz);
                }
                por(blocos, dx, dy, dz, m);
            }
        }
    }

    /** O cenário inteiro, na ordem em que é montado: primeiro o prédio da luta. */
    public static List<Bloco> cenario() {
        Map<Long, Bloco> blocos = new LinkedHashMap<>(40000);
        // O prédio da luta: a fachada sob os pés e as duas laterais descendo até o fundo da caixa.
        fachada(blocos, -MEIO_PREDIO, MEIO_PREDIO, -BORDA, BORDA, -1);
        lateral(blocos, -MEIO_PREDIO, FUNDO, -3, -BORDA, BORDA);
        lateral(blocos, MEIO_PREDIO, FUNDO, -3, -BORDA, BORDA);
        // O alto, a leste: uma parede de janelas ao lado da luta.
        fachada(blocos, ALTO_DE, ALTO_ATE, -BORDA, BORDA, ALTO_TOPO);
        lateral(blocos, ALTO_DE, FUNDO, ALTO_TOPO - 2, -BORDA, BORDA);
        // O baixo, a oeste: dá para cair nele e continuar lutando.
        fachada(blocos, BAIXO_DE, BAIXO_ATE, -BORDA, BORDA, BAIXO_TOPO);
        lateral(blocos, BAIXO_ATE, FUNDO, BAIXO_TOPO - 2, -BORDA, BORDA);
        // O atravessado, ao norte e por cima: a face que se vê da luta, a de baixo e a de cima.
        frente(blocos, CRUZADO_ATE, -BORDA, BORDA, CRUZADO_BASE, CRUZADO_TOPO);
        for (int dx = -BORDA; dx <= BORDA; dx++) {
            for (int dz = CRUZADO_DE; dz < CRUZADO_ATE; dz++) {
                por(blocos, dx, CRUZADO_BASE, dz, Material.CONCRETO_CLARO);
                por(blocos, dx, CRUZADO_TOPO, dz, Material.CONCRETO_CLARO);
            }
        }
        detalhes(blocos);
        return new ArrayList<>(blocos.values());
    }

    /**
     * O que sai dos prédios deitados: no mundo normal ficaria no telhado e na calçada; aqui sai de lado, para cima da
     * fachada de cada um. Antenas, caixas d'água, letreiros e as marquises na beirada que dá para a luta.
     */
    private static void detalhes(Map<Long, Bloco> blocos) {
        // O alto (fachada em dy 9): antenas, duas caixas d'água e um letreiro, longe do atravessado (dz −48 a −38).
        for (int[] a : new int[][] {{34, -26, 7}, {42, -4, 5}, {31, 18, 8}, {44, 36, 6}}) antena(blocos, a[0], ALTO_TOPO + 1, a[1], a[2]);
        caixaDagua(blocos, 38, ALTO_TOPO + 1, 6);
        caixaDagua(blocos, 36, ALTO_TOPO + 1, 30);
        letreiro(blocos, 40, ALTO_TOPO + 1, -18);
        // O baixo (fachada em dy −10): o mesmo, mais embaixo, abaixo do chão da luta.
        for (int[] a : new int[][] {{-40, -22, 6}, {-34, 10, 5}, {-43, 32, 6}}) antena(blocos, a[0], BAIXO_TOPO + 1, a[1], a[2]);
        caixaDagua(blocos, -38, BAIXO_TOPO + 1, -6);
        letreiro(blocos, -42, BAIXO_TOPO + 1, 18);
        // O letreiro no atravessado, na face que se vê da luta.
        for (int dx = -3; dx <= 2; dx++) {
            for (int dy = 15; dy <= 16; dy++) {
                por(blocos, dx, dy, CRUZADO_ATE, Math.floorMod(dx, 2) == 0 ? Material.LETREIRO_VERMELHO : Material.LETREIRO_AMARELO);
            }
        }
        // As marquises: uma faixa de concreto, um bloco para fora, no alto das laterais que dão para a luta.
        for (int dz = -BORDA; dz <= BORDA; dz++) {
            por(blocos, ALTO_DE - 1, ALTO_TOPO - 1, dz, Material.CONCRETO_CLARO);
            por(blocos, BAIXO_ATE + 1, BAIXO_TOPO - 1, dz, Material.CONCRETO_CLARO);
        }
    }

    /** Uma antena: a coluna de grade saindo da fachada e o para-raios na ponta. */
    private static void antena(Map<Long, Bloco> blocos, int dx, int base, int dz, int comprimento) {
        for (int k = 0; k < comprimento; k++) por(blocos, dx, base + k, dz, Material.GRADE_FERRO);
        por(blocos, dx, base + comprimento, dz, Material.PARA_RAIOS);
    }

    /** Uma caixa d'água de tábuas, 3 × 3 × 3, sobre quatro pés de grade. */
    private static void caixaDagua(Map<Long, Bloco> blocos, int x, int base, int z) {
        for (int dx : new int[] {-1, 1}) {
            for (int dz : new int[] {-1, 1}) {
                for (int k = 0; k < 2; k++) por(blocos, x + dx, base + k, z + dz, Material.GRADE_FERRO);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int k = 2; k <= 4; k++) por(blocos, x + dx, base + k, z + dz, Material.ASSOALHO);
            }
        }
    }

    /** Um letreiro de loja em pé sobre a fachada: 2 de altura por 6 de comprimento, em faixas vermelhas e amarelas. */
    private static void letreiro(Map<Long, Bloco> blocos, int x, int base, int z) {
        for (int k = 0; k < 6; k++) {
            for (int h = 0; h < 2; h++) {
                por(blocos, x, base + h, z + k, (k + h) % 2 == 0 ? Material.LETREIRO_VERMELHO : Material.LETREIRO_AMARELO);
            }
        }
    }
}
