package com.minerwk.core.hud;

/**
 * Posições da tela de escolha, sem classes do Minecraft: grade de emblemas e painel de detalhes.
 * Em tela larga o painel fica à direita da grade; abaixo de {@link #LARGURA_ESTREITA} ele vai para baixo.
 * A rolagem é contada em linhas da grade.
 */
public final class LayoutEscolha {
    public static final int EMBLEMA_L = 32;
    public static final int EMBLEMA_A = 43;
    public static final int ESPACO = 6;
    public static final int MARGEM = 10;
    public static final int TOPO = 30;
    public static final int LARGURA_ESTREITA = 360;

    private static final int PASSO_X = EMBLEMA_L + ESPACO;
    private static final int PASSO_Y = EMBLEMA_A + ESPACO;

    public record Retangulo(int x, int y, int largura, int altura) {
        public boolean contem(double px, double py) {
            return px >= x && py >= y && px < x + largura && py < y + altura;
        }
    }

    private final int total;
    private final int colunas;
    private final int linhasVisiveis;
    private final int rolagemMaxima;
    private final Retangulo grade;
    private final Retangulo painel;

    public LayoutEscolha(int largura, int altura, int total) {
        this.total = Math.max(0, total);
        boolean estreita = largura < LARGURA_ESTREITA;
        int gradeL = estreita ? largura - 2 * MARGEM : largura / 2 - MARGEM;
        this.colunas = Math.max(1, (gradeL + ESPACO) / PASSO_X);
        int linhasTotais = Math.max(1, (this.total + colunas - 1) / colunas);
        int alturaUtil = altura - TOPO - MARGEM;
        int disponivel = estreita ? alturaUtil / 2 : alturaUtil;
        int cabem = Math.max(1, (disponivel + ESPACO) / PASSO_Y);
        this.linhasVisiveis = estreita ? Math.min(linhasTotais, cabem) : cabem;
        this.rolagemMaxima = Math.max(0, linhasTotais - linhasVisiveis);
        int gradeA = linhasVisiveis * PASSO_Y - ESPACO;
        this.grade = new Retangulo(MARGEM, TOPO, gradeL, gradeA);
        if (estreita) {
            int py = TOPO + gradeA + ESPACO;
            this.painel = new Retangulo(MARGEM, py, gradeL, Math.max(0, altura - py - MARGEM));
        } else {
            int px = largura / 2 + ESPACO;
            this.painel = new Retangulo(px, TOPO, Math.max(0, largura - px - MARGEM), alturaUtil);
        }
    }

    public int colunas() { return colunas; }
    public int linhasVisiveis() { return linhasVisiveis; }
    public int rolagemMaxima() { return rolagemMaxima; }
    public Retangulo grade() { return grade; }
    public Retangulo painel() { return painel; }

    /** Canto superior esquerdo da célula na posição visível {@code (linha, coluna)}. */
    public int celulaX(int coluna) { return grade.x() + coluna * PASSO_X; }
    public int celulaY(int linha) { return grade.y() + linha * PASSO_Y; }

    /** Índice do personagem sob o ponto, ou −1 (fora da grade, no espaço entre células ou além do total). */
    public int indiceEm(double x, double y, int rolagem) {
        if (!grade.contem(x, y)) return -1;
        int rx = (int) (x - grade.x());
        int ry = (int) (y - grade.y());
        if (rx % PASSO_X >= EMBLEMA_L || ry % PASSO_Y >= EMBLEMA_A) return -1;
        int coluna = rx / PASSO_X;
        int linha = ry / PASSO_Y;
        if (coluna >= colunas || linha >= linhasVisiveis) return -1;
        int indice = (linha + rolagem) * colunas + coluna;
        return indice < total ? indice : -1;
    }
}
