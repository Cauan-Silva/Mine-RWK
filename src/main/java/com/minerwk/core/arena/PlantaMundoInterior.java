package com.minerwk.core.arena;

import com.minerwk.core.arena.PlantaEscola.Bloco;
import com.minerwk.core.arena.PlantaEscola.Material;
import java.util.ArrayList;
import java.util.List;

/**
 * A planta da arena do Zangetsu interior: o mundo interior do Ichigo, com os prédios deitados de lado. A luta
 * acontece em cima da fachada de um arranha-céu caído, uma grade de concreto e vidro; em volta, outros
 * prédios deitados em alturas diferentes e um atravessado ao fundo. Posições relativas ao bloco do centro;
 * {@code dy} 0 é a altura dos pés, então a fachada fica em {@code dy} −1. Norte é dz negativo.
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

    private PlantaMundoInterior() {
    }

    /** Viga ou vidro, pela posição na grade (as duas coordenadas correm pela face). */
    private static Material grade(int a, int b) {
        return Math.floorMod(a, VAO_LARGURA) == 0 || Math.floorMod(b, VAO_COMPRIMENTO) == 0 ? Material.CONCRETO_CLARO
                : Material.VIDRO_AZUL;
    }

    /** Uma face horizontal de fachada em {@code dy}, com o fundo escuro por baixo dos vidros. */
    private static void fachada(List<Bloco> blocos, int x0, int x1, int z0, int z1, int dy) {
        for (int dx = x0; dx <= x1; dx++) {
            for (int dz = z0; dz <= z1; dz++) {
                // A beirada é sempre viga: o prédio tem quina.
                Material m = dx == x0 || dx == x1 ? Material.CONCRETO_CLARO : grade(dx - x0, dz);
                blocos.add(new Bloco(dx, dy, dz, m));
                blocos.add(new Bloco(dx, dy - 1, dz, m == Material.VIDRO_AZUL ? Material.FUNDO_ESCURO : Material.CONCRETO_CLARO));
            }
        }
    }

    /** Uma face vertical virada para leste ou oeste, em {@code dx}, de {@code y0} a {@code y1}. */
    private static void lateral(List<Bloco> blocos, int dx, int y0, int y1, int z0, int z1) {
        for (int dy = y0; dy <= y1; dy++) {
            for (int dz = z0; dz <= z1; dz++) blocos.add(new Bloco(dx, dy, dz, grade(dy - y0 + 1, dz)));
        }
    }

    /** Uma face vertical virada para norte ou sul, em {@code dz}. */
    private static void frente(List<Bloco> blocos, int dz, int x0, int x1, int y0, int y1) {
        for (int dy = y0; dy <= y1; dy++) {
            for (int dx = x0; dx <= x1; dx++) blocos.add(new Bloco(dx, dy, dz, grade(dx, dy - y0 + 1)));
        }
    }

    /** O cenário inteiro, na ordem em que é montado: primeiro o prédio da luta. */
    public static List<Bloco> cenario() {
        List<Bloco> blocos = new ArrayList<>(40000);
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
                blocos.add(new Bloco(dx, CRUZADO_BASE, dz, Material.CONCRETO_CLARO));
                blocos.add(new Bloco(dx, CRUZADO_TOPO, dz, Material.CONCRETO_CLARO));
            }
        }
        return blocos;
    }
}
