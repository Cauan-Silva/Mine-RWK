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
    /** Meio lado da plataforma lisa: de −30 a +30 são 61 blocos. */
    public static final int MEIO_PATIO = 30;
    /** Camadas sob os pés: o piso e mais três de pedra. */
    public static final int CAMADAS = 4;

    public enum Material {
        PEDRA, TIJOLO_PEDRA, LAJE_PEDRA, ANDESITO_POLIDO, ESCADA_PEDRA, MADEIRA_ESCURA, MADEIRA_VERMELHA, TRONCO,
        FOLHA, LANTERNA, MURO, GRAMA, PAREDE_BRANCA, PILAR, ASSOALHO, TELHA, LAJE_TELHA,
        /** Do mundo interior do Ichigo: as vigas, as janelas e o escuro por trás delas. */
        CONCRETO_CLARO, VIDRO_AZUL, FUNDO_ESCURO,
        /** De Oriana: o miolo e a borda do portal da Rosa Negra. */
        PORTAL_NEGRO, PORTAL_ROXO,
        /** Dentro dos prédios: o piso do templo, as divisórias de papel, as janelas e os móveis. */
        TATAME, BAMBU_PISO, SHOJI, GRADE_NS, GRADE_LO, VELA, BARRIL, ESTANTE, SUPORTE_ARMADURA, LAJE_MADEIRA, TAPETE,
        FENO, ALVO, QUADRO,
        /** Por fora e no jardim. */
        CERCA_ESCURA, AGUA, VITORIA_REGIA, CASCALHO, PEDRA_MUSGO, FOLHA_CEREJEIRA, TRONCO_CEREJEIRA, BAMBU, SINO,
        /** Bancos e degraus de madeira, pelo lado para onde sobem. */
        ESCADA_MADEIRA_N, ESCADA_MADEIRA_S, ESCADA_MADEIRA_L, ESCADA_MADEIRA_O
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
    // Norte é dz negativo. Do sul vem a escadaria longa com os torii em fila; no topo dela, o pátio; em
    // volta do pátio, o salão principal (norte) e dois pavilhões (leste e oeste); em volta de tudo, a floresta.

    /** Meio lado da caixa. */
    private static final int BORDA = CaixaArena.MEIA;
    /** Meio lado do pátio da luta: de −20 a +20 são 41 blocos. */
    public static final int MEIO_PATIO_ESCOLA = 20;
    /** Até onde vai o calçamento em volta do pátio, antes da grama. */
    private static final int CALCADA = 24;
    /** Primeira fileira da escadaria (logo ao sul do pátio) e quantos degraus ela desce. */
    public static final int ESCADA_EM = MEIO_PATIO_ESCOLA + 1;
    public static final int DEGRAUS = 14;
    /** Meia largura da escadaria (11 blocos). */
    public static final int MEIA_ESCADA = 5;
    /** Em quais degraus há um torii; os pilares ficam em ±6 e o vão tem 6 de altura. */
    public static final int[] TORII_DA_ESCADA = {1, 4, 7, 10, 13};
    public static final int PILAR_DA_ESCADA = MEIA_ESCADA + 1;
    private static final int VAO_DA_ESCADA = 6;
    /** O torii grande, na cabeça da escadaria: pilares em ±6, vão de 9 de altura. */
    public static final int TORII_GRANDE_EM = MEIO_PATIO_ESCOLA;
    public static final int PILAR_GRANDE = 6;
    public static final int VAO_GRANDE = 9;
    /** O torii do caminho, entre o centro e o salão: pilares em ±4, vão de 7 de altura. */
    public static final int TORII_DO_CAMINHO = -12;
    public static final int PILAR_DO_CAMINHO = 4;
    public static final int VAO_DO_CAMINHO = 7;

    /** Um prédio: a planta das paredes, a altura da base e das paredes, o lado da porta e se tem dois telhados. */
    public record Predio(int x0, int x1, int z0, int z1, int podio, int parede, char porta, boolean doisNiveis) {
        /** Quanto o beiral passa das paredes. */
        public static final int BEIRAL = 2;

        boolean perto(int x, int z, int folga) {
            return x >= x0 - BEIRAL - folga && x <= x1 + BEIRAL + folga && z >= z0 - BEIRAL - folga && z <= z1 + BEIRAL + folga;
        }
    }

    /** O salão principal, ao norte, de frente para o pátio. */
    public static final Predio SALAO = new Predio(-12, 12, -39, -27, 2, 6, 'S', true);
    /** Os pavilhões dos lados, com a porta para o pátio. */
    public static final Predio PAVILHAO_LESTE = new Predio(27, 39, -10, 10, 1, 5, 'O', false);
    public static final Predio PAVILHAO_OESTE = new Predio(-39, -27, -10, 10, 1, 5, 'L', false);
    public static final List<Predio> PREDIOS = List.of(SALAO, PAVILHAO_LESTE, PAVILHAO_OESTE);

    /** De quanto em quanto a floresta tenta uma árvore. */
    private static final int PASSO_DA_FLORESTA = 7;

    /** Junta os blocos por posição: o que é posto depois substitui o que estava lá. */
    private static final class Obra {
        private final Map<Long, Bloco> blocos = new LinkedHashMap<>();

        private static long chave(int dx, int dy, int dz) {
            return ((long) (dx + 512) << 40) | ((long) (dy + 512) << 20) | (dz + 512);
        }

        /** Fora da caixa nada é posto (a copa de uma árvore da borda é cortada). */
        void por(int dx, int dy, int dz, Material m) {
            if (Math.abs(dx) > BORDA || Math.abs(dz) > BORDA) return;
            long chave = chave(dx, dy, dz);
            // Sai e entra de novo: assim o que depende de apoio (a lanterna) vem depois do apoio na lista.
            blocos.remove(chave);
            blocos.put(chave, new Bloco(dx, dy, dz, m));
        }

        void tirar(int dx, int dy, int dz) {
            blocos.remove(chave(dx, dy, dz));
        }

        boolean tem(int dx, int dy, int dz) {
            return blocos.containsKey(chave(dx, dy, dz));
        }
    }

    /** A fileira é a de um degrau da escadaria (e não a de um patamar)? */
    private static boolean fileiraDeDegrau(int dz) {
        return dz >= ESCADA_EM && (dz - ESCADA_EM) % 2 == 0 && (dz - ESCADA_EM) / 2 < DEGRAUS;
    }

    /**
     * Altura (dy) do bloco do chão numa fileira: −1 do pátio para o norte; ao sul a encosta desce um bloco
     * a cada dois, junto com a escadaria (uma fileira de degrau, uma de patamar).
     */
    public static int chao(int dz) {
        if (dz < ESCADA_EM) return -1;
        int k = Math.min((dz - ESCADA_EM) / 2, DEGRAUS - 1);
        return fileiraDeDegrau(dz) ? -1 - k : -2 - k;
    }

    /** A entrada da Escola: ver o comentário do começo desta parte. */
    public static List<Bloco> cenario() {
        Obra o = new Obra();
        terreno(o);
        torii(o, TORII_GRANDE_EM, 0, PILAR_GRANDE, VAO_GRANDE);
        torii(o, TORII_DO_CAMINHO, 0, PILAR_DO_CAMINHO, VAO_DO_CAMINHO);
        for (int k : TORII_DA_ESCADA) torii(o, ESCADA_EM + 1 + 2 * k, -1 - k, PILAR_DA_ESCADA, VAO_DA_ESCADA);
        for (Predio p : PREDIOS) predio(o, p);
        interiorSalao(o);
        // Lanternas de pedra na borda do pátio, fora da frente das portas e do caminho.
        for (int i : new int[] {-16, -8, 8, 16}) {
            lanterna(o, MEIO_PATIO_ESCOLA, i);
            lanterna(o, -MEIO_PATIO_ESCOLA, i);
            lanterna(o, i, -MEIO_PATIO_ESCOLA);
        }
        lanterna(o, PILAR_GRANDE + 3, MEIO_PATIO_ESCOLA);
        lanterna(o, -PILAR_GRANDE - 3, MEIO_PATIO_ESCOLA);
        floresta(o);
        return new ArrayList<>(o.blocos.values());
    }

    /** O chão da caixa inteira: pátio, calçamento, grama e a encosta do sul com a escadaria. */
    private static void terreno(Obra o) {
        for (int dx = -BORDA; dx <= BORDA; dx++) {
            for (int dz = -BORDA; dz <= BORDA; dz++) {
                int topo = chao(dz);
                // Sob o pátio e a calçada são três camadas de pedra (para o Azul e o Roxo); sob a grama e a
                // encosta, uma só: menos blocos para montar e iluminar.
                boolean calcado = Math.abs(dx) <= CALCADA && dz >= -CALCADA && dz < ESCADA_EM;
                int fundo = calcado ? -CAMADAS : topo - 1;
                for (int dy = fundo; dy < topo; dy++) o.por(dx, dy, dz, Material.PEDRA);
                o.por(dx, topo, dz, piso(dx, dz));
            }
        }
    }

    private static Material piso(int dx, int dz) {
        int ax = Math.abs(dx);
        if (dz >= ESCADA_EM) {
            if (ax > MEIA_ESCADA) return Material.GRAMA;
            return fileiraDeDegrau(dz) ? Material.ESCADA_PEDRA : Material.TIJOLO_PEDRA;
        }
        int longe = Math.max(ax, Math.abs(dz));
        if (longe <= MEIO_PATIO_ESCOLA) {
            // A faixa central de 5 e a moldura do pátio são de andesito polido.
            return ax <= 2 || longe == MEIO_PATIO_ESCOLA ? Material.ANDESITO_POLIDO : Material.TIJOLO_PEDRA;
        }
        // O caminho segue até a escada do salão.
        if (ax <= 2 && dz < 0 && dz >= SALAO.z1()) return Material.ANDESITO_POLIDO;
        return longe <= CALCADA ? Material.TIJOLO_PEDRA : Material.GRAMA;
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

    /** Lanterna de pedra: um poste de dois blocos com a luz em cima. */
    private static void lanterna(Obra o, int dx, int dz) {
        o.por(dx, 0, dz, Material.MURO);
        o.por(dx, 1, dz, Material.MURO);
        o.por(dx, 2, dz, Material.LANTERNA);
    }

    /**
     * Um prédio de templo: base de pedra, assoalho, paredes brancas com pilares e viga escuros, porta de
     * 5 × 4, oco por dentro, e telhado de beiral largo (dois telhados no salão).
     */
    private static void predio(Obra o, Predio p) {
        int b = Predio.BEIRAL;
        for (int dx = p.x0() - b; dx <= p.x1() + b; dx++) {
            for (int dz = p.z0() - b; dz <= p.z1() + b; dz++) {
                for (int dy = 0; dy < p.podio(); dy++) {
                    boolean dentro = dx > p.x0() && dx < p.x1() && dz > p.z0() && dz < p.z1();
                    o.por(dx, dy, dz, dy == p.podio() - 1 && dentro ? Material.ASSOALHO : Material.TIJOLO_PEDRA);
                }
            }
        }
        int topo = p.podio() + p.parede() - 1;
        paredes(o, p.x0(), p.x1(), p.z0(), p.z1(), p.podio(), topo);
        // A porta, no meio do lado que dá para o pátio.
        int mx = (p.x0() + p.x1()) / 2, mz = (p.z0() + p.z1()) / 2;
        for (int i = -2; i <= 2; i++) {
            for (int dy = p.podio(); dy < p.podio() + 4; dy++) {
                if (p.porta() == 'S') o.tirar(mx + i, dy, p.z1());
                else o.tirar(p.porta() == 'O' ? p.x0() : p.x1(), dy, mz + i);
            }
        }
        if (p.porta() == 'S') {
            // Escada de frente, subindo para o norte: um degrau por bloco da base.
            for (int j = 0; j < p.podio(); j++) {
                int dz = p.z1() + b + p.podio() - j;
                for (int dx = mx - 3; dx <= mx + 3; dx++) {
                    for (int dy = 0; dy < j; dy++) o.por(dx, dy, dz, Material.TIJOLO_PEDRA);
                    o.por(dx, j, dz, Material.ESCADA_PEDRA);
                }
            }
        } else {
            // Base de um bloco: meia laje de degrau diante da porta.
            int dx = p.porta() == 'O' ? p.x0() - b - 1 : p.x1() + b + 1;
            for (int dz = mz - 2; dz <= mz + 2; dz++) o.por(dx, 0, dz, Material.LAJE_PEDRA);
        }
        // Luz por dentro, nos quatro cantos.
        for (int dx : new int[] {p.x0() + 1, p.x1() - 1}) {
            for (int dz : new int[] {p.z0() + 1, p.z1() - 1}) o.por(dx, p.podio(), dz, Material.LANTERNA);
        }
        int beiral = topo + 1;
        if (!p.doisNiveis()) {
            telhado(o, p.x0(), p.x1(), p.z0(), p.z1(), beiral, Integer.MAX_VALUE);
            return;
        }
        // Telhado de baixo com quatro faixas, o corpo de cima recuado, e o telhado de cima até fechar.
        telhado(o, p.x0(), p.x1(), p.z0(), p.z1(), beiral, 4);
        int r = 2;
        paredes(o, p.x0() + r, p.x1() - r, p.z0() + r, p.z1() - r, beiral + 2, beiral + 4);
        telhado(o, p.x0() + r, p.x1() - r, p.z0() + r, p.z1() - r, beiral + 5, Integer.MAX_VALUE);
    }

    /**
     * O salão por dentro, como um templo: a antessala logo depois da porta, separada da nave por painéis de
     * papel; a nave com tatame e pilares; o altar ao fundo, com a estante embutida na parede de trás.
     */
    private static void interiorSalao(Obra o) {
        Predio p = SALAO;
        int piso = p.podio() - 1, pe = p.podio();
        // Piso da nave: faixas de tatame separadas por uma linha de mosaico de bambu.
        for (int dx = p.x0() + 1; dx < p.x1(); dx++) {
            for (int dz = p.z0() + 1; dz <= -31; dz++) {
                o.por(dx, piso, dz, (dz - p.z0()) % 3 == 0 ? Material.BAMBU_PISO : Material.TATAME);
            }
        }
        // Os painéis de papel, com a passagem de 3 no meio.
        for (int dx = p.x0() + 1; dx < p.x1(); dx++) {
            if (Math.abs(dx) <= 1) continue;
            for (int dy = pe; dy < pe + 3; dy++) o.por(dx, dy, -30, Material.SHOJI);
        }
        // Os pilares da nave, nas duas laterais.
        for (int dx : new int[] {-9, 9}) {
            for (int dz : new int[] {-36, -33}) {
                for (int dy = pe; dy < pe + p.parede() - 1; dy++) o.por(dx, dy, dz, Material.PILAR);
            }
        }
        // O altar: a estante embutida na parede do fundo, a mesa com velas e o degrau na frente.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = pe; dy < pe + 3; dy++) o.por(dx, dy, p.z0(), Material.ESTANTE);
            o.por(dx, pe, p.z0() + 1, Material.MADEIRA_ESCURA);
            o.por(dx, pe, p.z0() + 2, Material.LAJE_MADEIRA);
        }
        for (int dx : new int[] {-2, 0, 2}) o.por(dx, pe + 1, p.z0() + 1, Material.VELA);
        for (int dx : new int[] {-4, 4}) {
            o.por(dx, pe, p.z0() + 1, Material.CERCA_ESCURA);
            o.por(dx, pe + 1, p.z0() + 1, Material.LANTERNA);
        }
        // A antessala: mesas baixas com almofadas dos lados.
        for (int lado : new int[] {-1, 1}) {
            o.por(8 * lado, pe, -29, Material.LAJE_MADEIRA);
            o.por(7 * lado, pe, -29, Material.TAPETE);
            o.por(9 * lado, pe, -29, Material.TAPETE);
        }
    }

    /** As quatro paredes de um retângulo: pilar escuro nos cantos e de 4 em 4, branco entre eles, viga escura em cima. */
    private static void paredes(Obra o, int x0, int x1, int z0, int z1, int de, int ate) {
        for (int dx = x0; dx <= x1; dx++) {
            for (int dz = z0; dz <= z1; dz++) {
                boolean emX = dx == x0 || dx == x1, emZ = dz == z0 || dz == z1;
                if (!emX && !emZ) continue;
                boolean pilar = (emX && emZ) || (emZ && (dx - x0) % 4 == 0) || (emX && (dz - z0) % 4 == 0);
                for (int dy = de; dy <= ate; dy++) {
                    o.por(dx, dy, dz, dy == ate ? Material.MADEIRA_ESCURA : pilar ? Material.PILAR : Material.PAREDE_BRANCA);
                }
            }
        }
    }

    /**
     * Telhado de quatro águas sobre um retângulo, com beiral: faixas de dois blocos que recuam um bloco e
     * sobem meio bloco cada (laje, depois bloco inteiro), até {@code faixas} ou até fechar. Os cantos do
     * beiral sobem meio bloco, como nos telhados japoneses.
     */
    private static void telhado(Obra o, int x0, int x1, int z0, int z1, int base, int faixas) {
        int b = Predio.BEIRAL;
        for (int j = 0; j < faixas; j++) {
            int ax0 = x0 - b + j, ax1 = x1 + b - j, az0 = z0 - b + j, az1 = z1 + b - j;
            if (ax0 > ax1 || az0 > az1) break;
            boolean fecha = ax1 - ax0 < 4 || az1 - az0 < 4;
            int dy = base + j / 2;
            Material m = j % 2 == 0 ? Material.LAJE_TELHA : Material.TELHA;
            for (int dx = ax0; dx <= ax1; dx++) {
                for (int dz = az0; dz <= az1; dz++) {
                    boolean faixa = dx - ax0 < 2 || ax1 - dx < 2 || dz - az0 < 2 || az1 - dz < 2;
                    if ((faixa || fecha) && !o.tem(dx, dy, dz)) o.por(dx, dy, dz, m);
                }
            }
            if (j == 0) {
                for (int dx : new int[] {ax0, ax1}) {
                    for (int dz : new int[] {az0, az1}) o.por(dx, dy, dz, Material.TELHA);
                }
            }
            if (fecha) break;
        }
    }

    /** A floresta: pinheiros altos numa grade com desvio fixo (toda luta acontece no mesmo cenário). */
    private static void floresta(Obra o) {
        for (int gx = -BORDA + 2; gx <= BORDA - 2; gx += PASSO_DA_FLORESTA) {
            for (int gz = -BORDA + 2; gz <= BORDA - 2; gz += PASSO_DA_FLORESTA) {
                int h = (gx * 73856093) ^ (gz * 19349663);
                h ^= h >>> 13;
                int x = gx + Math.floorMod(h, 3) - 1, z = gz + Math.floorMod(h >> 4, 3) - 1;
                if (!livreParaArvore(x, z)) continue;
                arvore(o, x, z, 8 + Math.floorMod(h >> 8, 5));
            }
        }
    }

    /** Longe do pátio, da calçada, dos prédios e da escadaria, e dentro da caixa. */
    public static boolean livreParaArvore(int x, int z) {
        int folga = 4;
        if (Math.abs(x) > BORDA - 1 || Math.abs(z) > BORDA - 1) return false;
        if (Math.abs(x) <= CALCADA + folga && z >= -CALCADA - folga && z < ESCADA_EM + folga) return false;
        if (Math.abs(x) <= PILAR_DA_ESCADA + 2 + folga && z >= ESCADA_EM) return false;
        for (Predio p : PREDIOS) {
            if (p.perto(x, z, folga)) return false;
        }
        return true;
    }

    /** Pinheiro: tronco de {@code altura} blocos a partir do chão e copa em andares que afinam para cima. */
    private static void arvore(Obra o, int x, int z, int altura) {
        int pe = chao(z) + 1;
        for (int dy = 3; dy <= altura + 1; dy++) {
            int doTopo = altura + 1 - dy;
            int raio = doTopo == 0 ? 0 : Math.min(3, (doTopo + 1) / 2 + doTopo % 2);
            for (int dx = -raio; dx <= raio; dx++) {
                for (int dz = -raio; dz <= raio; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > raio + 1) continue;
                    o.por(x + dx, pe + dy, z + dz, Material.FOLHA);
                }
            }
        }
        for (int dy = 0; dy < altura; dy++) o.por(x, pe + dy, z, Material.TRONCO);
    }
}
