package com.minerwk.core.vfx;

import com.minerwk.core.kit.Vetor;

/** Vazio Infinito: cores da nebulosa e linhas de informação dentro do domo (raio 12). */
public final class VazioInfinitoGojo {
    private static final double OURO = 2.399963;
    private static final int[] PALETA = {0x0EE7FF, 0x2454FF, 0x8230FF, 0xE920FF, 0xFF238E, 0x7542FF};
    /** O Minezin usava raio 26; as formas foram escaladas por este fator. */
    private static final double ESCALA = PerfilVfxGojo.DOMINIO_RAIO / 26.0;

    private VazioInfinitoGojo() {
    }

    /** Cor RGB da faixa, misturando a paleta ao longo de {@code s}. */
    public static int cor(int faixa, double s, double t) {
        double fase = Math.floorMod(faixa, PALETA.length) + s * .85 + .1 * Math.sin(t * .035 + faixa);
        int base = (int) Math.floor(fase);
        double mistura = fase - base;
        int a = PALETA[Math.floorMod(base, PALETA.length)];
        int b = PALETA[Math.floorMod(base + 1, PALETA.length)];
        int resultado = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int canal = (int) Math.round(((a >> shift) & 255) * (1 - mistura) + ((b >> shift) & 255) * mistura);
            resultado |= canal << shift;
        }
        return resultado;
    }

    /** Se a câmera a {@code distancia} do centro está de fato dentro do domo. */
    public static boolean interior(double distancia, double raio) {
        return raio > .1 && distancia < raio - .5;
    }

    /** Linha de informação subindo da borda para o alto, relativa ao centro. */
    public static Vetor informacao(int faixa, double s, double t) {
        double a = faixa * OURO + t * .003;
        double r = (17 + (faixa % 5) * 1.1) * ESCALA;
        double inicio = (.3 + (faixa % 7) * 1.5) * ESCALA;
        double topo = 8 * ESCALA;
        return new Vetor(Math.cos(a) * r * (1 - s), inicio + (topo - inicio) * s + Math.sin(s * Math.PI) * ESCALA,
                Math.sin(a) * r * (1 - s));
    }

    public static double pulso(int faixa, double s, double t) {
        double onda = .5 + .5 * Math.cos((s * 2 - t * .025 - faixa * .13) * Math.PI * 2);
        double abertura = .2 + .8 * (1 - PerfilVfxGojo.suave((t - 16) / 16));
        return (.18 + .82 * Math.pow(onda, 12)) * Math.sin(s * Math.PI) * abertura;
    }
}
