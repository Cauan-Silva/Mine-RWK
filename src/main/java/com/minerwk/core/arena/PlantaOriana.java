package com.minerwk.core.arena;

import com.minerwk.core.arena.PlantaEscola.Bloco;
import com.minerwk.core.arena.PlantaEscola.Material;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A planta da arena do Ragnarok: os arredores da capital de Oriana. Um campo aberto cortado pela estrada que
 * leva ao portão, a muralha da cidade ao norte com as torres, e no céu, por cima dela, o portal da Rosa
 * Negra. O campo é largo e sem obstáculos no meio: o boss tem sete blocos de altura e atravessa a arena
 * voando. Posições relativas ao bloco do centro; {@code dy} 0 é a altura dos pés. Norte é dz negativo.
 */
public final class PlantaOriana {
    private static final int BORDA = CaixaArena.MEIA;
    /** Meia largura da estrada que vai do sul ao portão. */
    public static final int MEIA_ESTRADA = 2;
    /** A muralha: de dz −48 a −46, com 10 blocos de altura. */
    public static final int MURALHA_DE = -48, MURALHA_ATE = -46, MURALHA_TOPO = 9;
    /** As torres, quadradas, encostadas na muralha. */
    public static final int[] TORRES = {-44, -20, 20, 44};
    public static final int MEIA_TORRE = 3, TORRE_TOPO = 15;
    /** O portal da Rosa Negra: um disco de pé no céu, por cima da muralha. */
    public static final int PORTAL_DY = 36, PORTAL_DZ = -44, PORTAL_RAIO = 12, PORTAL_MIOLO = 10;
    /** Onde o Ragnarok entra, ao norte do centro: o jogador entra 16 ao sul. */
    public static final int ENTRADA_DO_BOSS = -8;

    private PlantaOriana() {
    }

    /** O cenário inteiro, na ordem em que é montado: primeiro o chão. Quem vem depois troca o que já estava. */
    public static List<Bloco> cenario() {
        Map<Long, Bloco> blocos = new LinkedHashMap<>(40000);
        for (int dx = -BORDA; dx <= BORDA; dx++) {
            for (int dz = -BORDA; dz <= BORDA; dz++) {
                por(blocos, dx, -1, dz, Math.abs(dx) <= MEIA_ESTRADA ? Material.ANDESITO_POLIDO : Material.GRAMA);
                por(blocos, dx, -2, dz, Material.PEDRA);
            }
        }
        muralha(blocos);
        for (int x : TORRES) torre(blocos, x);
        // Lanternas na beira da estrada, longe do meio da luta, e árvores só nas beiradas do campo.
        for (int dz : new int[] {-40, 40}) {
            for (int dx : new int[] {-6, 6}) poste(blocos, dx, dz);
        }
        for (int[] p : new int[][] {{-42, -22}, {42, -22}, {-45, 4}, {45, 4}, {-40, 30}, {40, 30}, {-28, 44}, {28, 44}}) {
            arvore(blocos, p[0], p[1]);
        }
        portal(blocos);
        return new ArrayList<>(blocos.values());
    }

    private static void por(Map<Long, Bloco> blocos, int dx, int dy, int dz, Material m) {
        blocos.put(((long) (dx + 512) << 40) | ((long) (dy + 512) << 20) | (dz + 512), new Bloco(dx, dy, dz, m));
    }

    private static void tirar(Map<Long, Bloco> blocos, int dx, int dy, int dz) {
        blocos.remove(((long) (dx + 512) << 40) | ((long) (dy + 512) << 20) | (dz + 512));
    }

    /** A muralha de pedra com ameias e o portão fechado no fim da estrada. */
    private static void muralha(Map<Long, Bloco> blocos) {
        for (int dx = -BORDA; dx <= BORDA; dx++) {
            for (int dz = MURALHA_DE; dz <= MURALHA_ATE; dz++) {
                for (int dy = 0; dy <= MURALHA_TOPO; dy++) por(blocos, dx, dy, dz, Material.TIJOLO_PEDRA);
            }
            if (Math.floorMod(dx, 2) == 0) por(blocos, dx, MURALHA_TOPO + 1, MURALHA_ATE, Material.TIJOLO_PEDRA);
        }
        // O portão: um vão de 5 por 5 na face de fora, com as folhas de madeira no fundo.
        for (int dx = -MEIA_ESTRADA; dx <= MEIA_ESTRADA; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                tirar(blocos, dx, dy, MURALHA_ATE);
                por(blocos, dx, dy, MURALHA_ATE - 1, Material.MADEIRA_ESCURA);
            }
        }
    }

    private static void torre(Map<Long, Bloco> blocos, int x) {
        int meio = (MURALHA_DE + MURALHA_ATE) / 2;
        for (int dx = Math.max(-BORDA, x - MEIA_TORRE); dx <= Math.min(BORDA, x + MEIA_TORRE); dx++) {
            for (int dz = Math.max(-BORDA, meio - MEIA_TORRE); dz <= meio + MEIA_TORRE; dz++) {
                boolean casca = Math.abs(dx - x) == MEIA_TORRE || Math.abs(dz - meio) == MEIA_TORRE
                        || dx == -BORDA || dx == BORDA || dz == -BORDA;
                for (int dy = 0; dy <= TORRE_TOPO; dy++) {
                    if (casca) por(blocos, dx, dy, dz, Material.TIJOLO_PEDRA);
                }
                por(blocos, dx, TORRE_TOPO + 1, dz, Material.TELHA);
            }
        }
    }

    private static void poste(Map<Long, Bloco> blocos, int dx, int dz) {
        for (int dy = 0; dy <= 2; dy++) por(blocos, dx, dy, dz, Material.MURO);
        por(blocos, dx, 3, dz, Material.LANTERNA);
    }

    private static void arvore(Map<Long, Bloco> blocos, int x, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 4; dy <= 7; dy++) {
                    int longe = Math.abs(dx) + Math.abs(dz) + (dy - 4);
                    if (longe <= 4 && Math.abs(x + dx) <= BORDA && Math.abs(z + dz) <= BORDA) {
                        por(blocos, x + dx, dy, z + dz, Material.FOLHA);
                    }
                }
            }
        }
        for (int dy = 0; dy <= 5; dy++) por(blocos, x, dy, z, Material.TRONCO);
    }

    /** O disco do portal: o miolo negro e a borda roxa que brilha. */
    private static void portal(Map<Long, Bloco> blocos) {
        for (int dx = -PORTAL_RAIO; dx <= PORTAL_RAIO; dx++) {
            for (int dy = -PORTAL_RAIO; dy <= PORTAL_RAIO; dy++) {
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > PORTAL_RAIO + .3) continue;
                por(blocos, dx, PORTAL_DY + dy, PORTAL_DZ, d <= PORTAL_MIOLO ? Material.PORTAL_NEGRO : Material.PORTAL_ROXO);
            }
        }
    }
}
