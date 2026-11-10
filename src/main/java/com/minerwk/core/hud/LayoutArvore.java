package com.minerwk.core.hud;

import com.minerwk.core.hud.LayoutEscolha.Retangulo;

/**
 * Posições da tela da árvore de habilidades, sem classes do Minecraft. De cima para baixo: os nós de antes do
 * despertar, o nó Despertar sozinho no meio e os nós de depois. As células são numeradas nessa ordem; cada
 * linha fica centrada. Se não couber na altura, a tela rola (em pixels).
 */
public final class LayoutArvore {
    public static final int CELULA_L = 104;
    public static final int CELULA_A = 26;
    public static final int ESPACO = 4;
    public static final int MARGEM = 10;
    /** Altura do cabeçalho (personagem, maestria, XP e pontos). */
    public static final int TOPO = 42;
    public static final int COLUNAS_MAX = 4;

    private static final int PASSO_Y = CELULA_A + ESPACO;

    private final int largura;
    private final int antes;
    private final int depois;
    private final int colunas;
    private final int yDespertar;
    private final int yDepois;
    private final int rolagemMaxima;

    public LayoutArvore(int largura, int altura, int antes, int depois) {
        this.largura = largura;
        this.antes = Math.max(0, antes);
        this.depois = Math.max(0, depois);
        this.colunas = Math.max(1, Math.min(COLUNAS_MAX, (largura - 2 * MARGEM + ESPACO) / (CELULA_L + ESPACO)));
        this.yDespertar = TOPO + linhas(this.antes) * PASSO_Y + ESPACO;
        this.yDepois = yDespertar + CELULA_A + 2 * ESPACO;
        int fim = yDepois + Math.max(0, linhas(this.depois) * PASSO_Y - ESPACO);
        this.rolagemMaxima = Math.max(0, fim + MARGEM - altura);
    }

    private int linhas(int quantos) {
        return (quantos + colunas - 1) / colunas;
    }

    public int colunas() {
        return colunas;
    }

    /** Quantas células há ao todo: as de antes, o Despertar e as de depois. */
    public int total() {
        return antes + 1 + depois;
    }

    public int indiceDoDespertar() {
        return antes;
    }

    public int rolagemMaxima() {
        return rolagemMaxima;
    }

    /** O retângulo da célula {@code indice}, sem a rolagem. */
    public Retangulo celula(int indice) {
        if (indice == antes) return new Retangulo((largura - CELULA_L) / 2, yDespertar, CELULA_L, CELULA_A);
        boolean eAntes = indice < antes;
        int i = eAntes ? indice : indice - antes - 1;
        int quantos = eAntes ? antes : depois;
        int linha = i / colunas;
        int naLinha = Math.min(colunas, quantos - linha * colunas);
        int larguraDaLinha = naLinha * (CELULA_L + ESPACO) - ESPACO;
        int x = (largura - larguraDaLinha) / 2 + (i % colunas) * (CELULA_L + ESPACO);
        int y = (eAntes ? TOPO : yDepois) + linha * PASSO_Y;
        return new Retangulo(x, y, CELULA_L, CELULA_A);
    }

    /** Índice da célula sob o ponto, com a tela rolada {@code rolagem} pixels; −1 se não há (ou é o cabeçalho). */
    public int indiceEm(double x, double y, int rolagem) {
        if (y < TOPO) return -1;
        for (int i = 0; i < total(); i++) {
            if (celula(i).contem(x, y + rolagem)) return i;
        }
        return -1;
    }
}
