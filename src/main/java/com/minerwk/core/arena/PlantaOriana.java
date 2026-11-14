package com.minerwk.core.arena;

import com.minerwk.core.arena.PlantaEscola.Bloco;
import com.minerwk.core.arena.PlantaEscola.Material;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A planta da arena do Ragnarok: os arredores da capital de Oriana sob ataque. Um campo aberto cortado pela
 * estrada que leva ao portão arrombado, a muralha rachada da cidade ao norte, com brechas e uma torre em chamas,
 * e no céu, por cima dela, o portal da Rosa Negra. O meio do campo fica sem obstáculos: o boss tem sete blocos
 * de altura e atravessa a arena voando. Posições relativas ao bloco do centro; {@code dy} 0 é a altura dos pés.
 * Norte é dz negativo.
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
    /** A torre que o Ragnarok já derrubou: sem telhado, com a borda quebrada e fogo em cima. */
    public static final int TORRE_DESTRUIDA = -20;
    /** As duas brechas em V na muralha: o meio de cada uma, a meia largura no topo e até onde descem. */
    public static final int[] BRECHAS = {-32, 30};
    public static final int MEIA_BRECHA = 3, FUNDO_DA_BRECHA = 4;
    /** O portal da Rosa Negra: um disco de pé no céu, por cima da muralha. */
    public static final int PORTAL_DY = 36, PORTAL_DZ = -44, PORTAL_RAIO = 12, PORTAL_MIOLO = 10;
    /** Onde o Ragnarok entra, ao norte do centro: o jogador entra 16 ao sul. */
    public static final int ENTRADA_DO_BOSS = -8;
    /** O que pega fogo: nenhum destes pode ficar perto de um foco. */
    public static final Set<Material> INFLAMAVEIS = EnumSet.of(Material.MADEIRA_ESCURA, Material.TRONCO, Material.FOLHA,
            Material.FENO, Material.LA_VERMELHA, Material.CERCA_ESCURA, Material.BARRIL, Material.TRONCO_QUEIMADO);

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

    private static long chave(int dx, int dy, int dz) {
        return ((long) (dx + 512) << 40) | ((long) (dy + 512) << 20) | (dz + 512);
    }

    private static void por(Map<Long, Bloco> blocos, int dx, int dy, int dz, Material m) {
        blocos.put(chave(dx, dy, dz), new Bloco(dx, dy, dz, m));
    }

    private static void tirar(Map<Long, Bloco> blocos, int dx, int dy, int dz) {
        blocos.remove(chave(dx, dy, dz));
    }

    /** Um sorteio fixo por posição, de 0 a 1: a mesma planta sai sempre igual. */
    static double acaso(int dx, int dy, int dz, int sal) {
        long h = dx * 73856093L ^ dy * 19349663L ^ dz * 83492791L ^ sal * 2654435761L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    /** O tijolo da muralha e das torres: rachado ou com musgo em quase um terço. */
    private static Material tijolo(int dx, int dy, int dz) {
        double r = acaso(dx, dy, dz, 1);
        return r < .18 ? Material.TIJOLO_RACHADO : r < .30 ? Material.TIJOLO_MUSGO : Material.TIJOLO_PEDRA;
    }

    /**
     * A muralha de pedra rachada: manchas de queimado na face de dentro, ameias faltando, duas brechas em V com o
     * entulho caído no pé, e o portão arrombado, com uma folha no chão e uma barricada atrás do vão.
     */
    private static void muralha(Map<Long, Bloco> blocos) {
        for (int dx = -BORDA; dx <= BORDA; dx++) {
            for (int dz = MURALHA_DE; dz <= MURALHA_ATE; dz++) {
                for (int dy = 0; dy <= MURALHA_TOPO; dy++) {
                    boolean queimado = dz == MURALHA_ATE && dy <= 7 && acaso(dx / 3, dy / 2, 0, 2) < .3 && acaso(dx, dy, dz, 3) < .75;
                    por(blocos, dx, dy, dz, queimado ? Material.PEDRA_NEGRA : tijolo(dx, dy, dz));
                }
            }
            if (Math.floorMod(dx, 2) == 0 && acaso(dx, 0, 0, 4) > .35) por(blocos, dx, MURALHA_TOPO + 1, MURALHA_ATE, tijolo(dx, 10, 0));
        }
        for (int x : BRECHAS) brecha(blocos, x);
        // O vão do portão, de 5 por 5, na face de dentro; as folhas foram arrombadas.
        for (int dx = -MEIA_ESTRADA; dx <= MEIA_ESTRADA; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                tirar(blocos, dx, dy, MURALHA_ATE);
                tirar(blocos, dx, dy, MURALHA_ATE - 1);
            }
            // A barricada atrás do vão: tábuas, cercas e pedra até a altura 3. Ninguém passa.
            for (int dy = 0; dy <= 3; dy++) {
                double r = acaso(dx, dy, 0, 5);
                Material m = dy == 3 && r < .5 ? Material.CERCA_ESCURA : r < .35 ? Material.PEDRA : Material.MADEIRA_ESCURA;
                por(blocos, dx, dy, MURALHA_ATE - 1, m);
            }
        }
        // Uma folha caída no chão à frente do vão, e a outra pendurada torta no canto.
        for (int dx = -MEIA_ESTRADA; dx <= 0; dx++) {
            for (int dz = MURALHA_ATE + 1; dz <= MURALHA_ATE + 3; dz++) por(blocos, dx, 0, dz, Material.MADEIRA_ESCURA);
        }
        for (int dy = 1; dy <= 4; dy++) por(blocos, MEIA_ESTRADA, dy, MURALHA_ATE, Material.MADEIRA_ESCURA);
    }

    /** Uma brecha em V, cortada do topo até {@link #FUNDO_DA_BRECHA}, com o entulho caído do lado de dentro. */
    private static void brecha(Map<Long, Bloco> blocos, int x) {
        int altura = MURALHA_TOPO + 1 - FUNDO_DA_BRECHA;
        for (int dy = FUNDO_DA_BRECHA; dy <= MURALHA_TOPO + 1; dy++) {
            int meia = MEIA_BRECHA * (dy - FUNDO_DA_BRECHA) / altura;
            for (int dx = x - meia; dx <= x + meia; dx++) {
                for (int dz = MURALHA_DE; dz <= MURALHA_ATE; dz++) tirar(blocos, dx, dy, dz);
            }
        }
        // O entulho: um monte de pedra e cascalho que desce da muralha para o campo.
        for (int dx = x - MEIA_BRECHA - 1; dx <= x + MEIA_BRECHA + 1; dx++) {
            for (int dz = MURALHA_ATE + 1; dz <= MURALHA_ATE + 4; dz++) {
                int alto = 3 - (dz - MURALHA_ATE - 1) - Math.abs(dx - x) / 2;
                for (int dy = 0; dy < alto; dy++) {
                    por(blocos, dx, dy, dz, acaso(dx, dy, dz, 6) < .4 ? Material.CASCALHO : tijolo(dx, dy, dz));
                }
            }
        }
    }

    private static void torre(Map<Long, Bloco> blocos, int x) {
        int meio = (MURALHA_DE + MURALHA_ATE) / 2;
        boolean destruida = x == TORRE_DESTRUIDA;
        for (int dx = Math.max(-BORDA, x - MEIA_TORRE); dx <= Math.min(BORDA, x + MEIA_TORRE); dx++) {
            for (int dz = Math.max(-BORDA, meio - MEIA_TORRE); dz <= meio + MEIA_TORRE; dz++) {
                boolean casca = Math.abs(dx - x) == MEIA_TORRE || Math.abs(dz - meio) == MEIA_TORRE
                        || dx == -BORDA || dx == BORDA || dz == -BORDA;
                // A destruída perdeu o alto: a borda para em alturas diferentes, de 4 a 9 abaixo do topo.
                int topo = destruida ? TORRE_TOPO - 4 - (int) (acaso(dx, 0, dz, 7) * 6) : TORRE_TOPO;
                for (int dy = 0; dy <= topo; dy++) {
                    if (casca) por(blocos, dx, dy, dz, tijolo(dx, dy, dz));
                }
                if (!destruida) por(blocos, dx, TORRE_TOPO + 1, dz, Material.TELHA);
            }
        }
        if (destruida) {
            // Por dentro, um piso de netherrack que queima para sempre; o fogo aparece por cima da borda quebrada.
            int piso = TORRE_TOPO - 7;
            for (int dx = x - MEIA_TORRE + 1; dx <= x + MEIA_TORRE - 1; dx++) {
                for (int dz = meio - MEIA_TORRE + 1; dz <= meio + MEIA_TORRE - 1; dz++) {
                    por(blocos, dx, piso, dz, Material.NETHERRACK);
                    if (acaso(dx, piso, dz, 8) < .6) por(blocos, dx, piso + 1, dz, Material.FOGO);
                }
            }
            return;
        }
        // Os estandartes vermelhos, rasgados, na face que dá para o campo.
        int face = meio + MEIA_TORRE + 1;
        for (int dx : new int[] {x - 1, x + 1}) {
            int comprimento = 3 + (int) (acaso(dx, 0, face, 9) * 4);
            for (int k = 0; k < comprimento; k++) por(blocos, dx, TORRE_TOPO - 1 - k, face, Material.LA_VERMELHA);
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
