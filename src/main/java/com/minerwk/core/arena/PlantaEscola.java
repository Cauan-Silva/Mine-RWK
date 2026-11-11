package com.minerwk.core.arena;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A planta da arena: qual material vai em cada posição, relativa ao bloco do centro. {@code dy} 0 é a
 * altura dos pés no pátio, então o piso fica em {@code dy} −1. Separada do código que põe os blocos no
 * mundo para poder ser testada e ajustada sem abrir o jogo.
 */
public final class PlantaEscola {
    /** Meio lado do pátio: de −30 a +30 são 61 blocos. */
    public static final int MEIO_PATIO = 30;
    /** Camadas sob os pés: o piso e mais três de pedra. */
    public static final int CAMADAS = 4;

    public enum Material {
        PEDRA, TIJOLO_PEDRA, LAJE_PEDRA, ANDESITO_POLIDO, ESCADA_PEDRA, MADEIRA_ESCURA, MADEIRA_VERMELHA, TRONCO,
        FOLHA, LANTERNA, MURO
    }

    public record Bloco(int dx, int dy, int dz, Material material) {
    }

    private PlantaEscola() {
    }

    /** Pátio liso de 61 × 61: piso de tijolo de pedra e três camadas de pedra por baixo. */
    public static List<Bloco> plataforma() {
        List<Bloco> blocos = new ArrayList<>((2 * MEIO_PATIO + 1) * (2 * MEIO_PATIO + 1) * CAMADAS);
        for (int dy = -CAMADAS; dy <= -1; dy++) {
            Material m = dy == -1 ? Material.TIJOLO_PEDRA : Material.PEDRA;
            for (int dx = -MEIO_PATIO; dx <= MEIO_PATIO; dx++) {
                for (int dz = -MEIO_PATIO; dz <= MEIO_PATIO; dz++) blocos.add(new Bloco(dx, dy, dz, m));
            }
        }
        return blocos;
    }

    // --- O cenário: a entrada da Escola Jujutsu ---

    /** Meio lado da caixa: a faixa de fora do pátio vai de 31 a 40. */
    private static final int BORDA = 40;
    /** Onde ficam os muros: logo depois do pátio. */
    private static final int MURO_EM = MEIO_PATIO + 1;
    /** Meia largura da escadaria (21 blocos) e quantos degraus ela sobe. */
    private static final int MEIA_ESCADA = 10;
    private static final int DEGRAUS = 8;
    /** Os dois torii do caminho central: onde ficam, meio vão (7 livres) e altura livre. */
    private static final int[] TORII_DO_CAMINHO = {-12, 12};
    private static final int PILAR_DO_CAMINHO = 4;
    private static final int VAO_DO_CAMINHO = 7;
    /** O torii grande do patamar: meio vão (9 livres) e altura livre. */
    private static final int PILAR_GRANDE = 5;
    private static final int VAO_GRANDE = 9;
    /** De quanto em quanto há uma lanterna sobre o muro. */
    private static final int PASSO_DAS_LANTERNAS = 6;
    /** As doze árvores, em posições fixas: toda luta acontece no mesmo cenário. */
    private static final int[][] ARVORES = {
            {36, -24}, {36, -8}, {36, 8}, {36, 24},
            {-36, -24}, {-36, -8}, {-36, 8}, {-36, 24},
            {-24, 36}, {-8, 36}, {8, 36}, {24, 36}};

    /** Junta os blocos por posição: o que é posto depois substitui o que estava lá. */
    private static final class Obra {
        private final Map<Long, Bloco> blocos = new LinkedHashMap<>();

        void por(int dx, int dy, int dz, Material m) {
            long chave = ((long) (dx + 512) << 40) | ((long) (dy + 512) << 20) | (dz + 512);
            // Sai e entra de novo: assim o que depende de apoio (a lanterna) vem depois do apoio na lista.
            blocos.remove(chave);
            blocos.put(chave, new Bloco(dx, dy, dz, m));
        }
    }

    /**
     * A entrada da Escola: pátio de pedra com a faixa central de andesito, escadaria ao norte subindo até o
     * patamar do torii grande, dois torii sobre o caminho, e muro, lanternas e árvores nas outras bordas.
     */
    public static List<Bloco> cenario() {
        Obra o = new Obra();
        // Pátio: a faixa central de 5 e a moldura são de andesito polido; o resto, tijolo de pedra.
        for (int dx = -MEIO_PATIO; dx <= MEIO_PATIO; dx++) {
            for (int dz = -MEIO_PATIO; dz <= MEIO_PATIO; dz++) {
                boolean destaque = Math.abs(dx) <= 2 || Math.abs(dx) == MEIO_PATIO || Math.abs(dz) == MEIO_PATIO;
                o.por(dx, -1, dz, destaque ? Material.ANDESITO_POLIDO : Material.TIJOLO_PEDRA);
                for (int dy = -CAMADAS; dy <= -2; dy++) o.por(dx, dy, dz, Material.PEDRA);
            }
        }
        // Faixa de fora: uma camada de pedra em volta do pátio.
        for (int dx = -BORDA; dx <= BORDA; dx++) {
            for (int dz = -BORDA; dz <= BORDA; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) > MEIO_PATIO) o.por(dx, -1, dz, Material.PEDRA);
            }
        }
        // Escadaria ao norte: cada degrau um bloco mais alto, maciça por baixo.
        for (int dx = -MEIA_ESCADA; dx <= MEIA_ESCADA; dx++) {
            for (int k = 0; k < DEGRAUS; k++) {
                int dz = -MURO_EM - k;
                for (int dy = 0; dy < k; dy++) o.por(dx, dy, dz, Material.PEDRA);
                o.por(dx, k, dz, Material.ESCADA_PEDRA);
            }
            // Patamar: os dois últimos blocos da caixa, com o piso na altura do último degrau.
            for (int dz = -MURO_EM - DEGRAUS; dz >= -BORDA; dz--) {
                for (int dy = 0; dy < DEGRAUS - 1; dy++) o.por(dx, dy, dz, Material.PEDRA);
                o.por(dx, DEGRAUS - 1, dz, Math.abs(dx) <= 2 ? Material.ANDESITO_POLIDO : Material.TIJOLO_PEDRA);
            }
        }
        torii(o, -BORDA, DEGRAUS, PILAR_GRANDE, VAO_GRANDE);
        for (int dz : TORII_DO_CAMINHO) torii(o, dz, 0, PILAR_DO_CAMINHO, VAO_DO_CAMINHO);
        // Muro de 2 de altura a leste, a oeste e ao sul, e ao norte dos dois lados da escadaria.
        for (int i = -MURO_EM; i <= MURO_EM; i++) {
            for (int dy = 0; dy <= 1; dy++) {
                o.por(MURO_EM, dy, i, Material.MURO);
                o.por(-MURO_EM, dy, i, Material.MURO);
                o.por(i, dy, MURO_EM, Material.MURO);
                if (Math.abs(i) > MEIA_ESCADA) o.por(i, dy, -MURO_EM, Material.MURO);
            }
        }
        for (int i = -MEIO_PATIO; i <= MEIO_PATIO; i += PASSO_DAS_LANTERNAS) {
            o.por(MURO_EM, 2, i, Material.LANTERNA);
            o.por(-MURO_EM, 2, i, Material.LANTERNA);
            o.por(i, 2, MURO_EM, Material.LANTERNA);
        }
        for (int i = 0; i < ARVORES.length; i++) arvore(o, ARVORES[i][0], ARVORES[i][1], 5 + i % 3, 2 + i % 2);
        return new ArrayList<>(o.blocos.values());
    }

    /**
     * Um torii atravessado no eixo leste-oeste: dois pilares vermelhos, a travessa vermelha logo acima do
     * vão e a viga escura de cima, mais larga.
     */
    private static void torii(Obra o, int dz, int base, int pilar, int vao) {
        for (int dy = base; dy < base + vao; dy++) {
            o.por(pilar, dy, dz, Material.MADEIRA_VERMELHA);
            o.por(-pilar, dy, dz, Material.MADEIRA_VERMELHA);
        }
        for (int dx = -pilar - 1; dx <= pilar + 1; dx++) o.por(dx, base + vao, dz, Material.MADEIRA_VERMELHA);
        for (int dx = -pilar - 2; dx <= pilar + 2; dx++) o.por(dx, base + vao + 1, dz, Material.MADEIRA_ESCURA);
    }

    /** Tronco de {@code altura} blocos e copa redonda de raio {@code raio} em volta do topo dele. */
    private static void arvore(Obra o, int x, int z, int altura, int raio) {
        for (int dx = -raio; dx <= raio; dx++) {
            for (int dy = -raio; dy <= raio; dy++) {
                for (int dz = -raio; dz <= raio; dz++) {
                    if (dx * dx + dy * dy + dz * dz > raio * raio + 1) continue;
                    int fx = x + dx, fz = z + dz;
                    // A copa não passa da caixa nem entra por cima do muro e do pátio.
                    if (Math.max(Math.abs(fx), Math.abs(fz)) > BORDA || Math.max(Math.abs(fx), Math.abs(fz)) <= MURO_EM) continue;
                    o.por(fx, altura + dy, fz, Material.FOLHA);
                }
            }
        }
        for (int dy = 0; dy < altura; dy++) o.por(x, dy, z, Material.TRONCO);
    }
}
