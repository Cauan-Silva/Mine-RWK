package com.minerwk.core.hud;

import com.minerwk.core.hud.LayoutEscolha.Retangulo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Posições da árvore de habilidades com galhos (spec [[2026-11-12 UI dos personagens]]), sem classes do
 * Minecraft. O tronco sobe pelo meio: a raiz (o emblema) embaixo, os galhos de antes do Despertar, o Despertar e
 * os galhos de depois em cima. Os galhos de cada grupo se alternam à esquerda e à direita, dois por altura; cada um
 * tem a ponta (o ícone do galho) perto do tronco e os nós seguindo para fora.
 *
 * <p>Os galhos são numerados de 0 em diante, os de antes primeiro. As posições são do conteúdo; a tela rola em y
 * (y na tela = y do conteúdo − rolagem) e começa rolada até o fim, mostrando a raiz.
 */
public final class LayoutArvore {
    public static final int NO = 24;
    public static final int PASSO_X = 30;
    public static final int PASSO_Y = 36;
    /** Altura do cabeçalho (personagem, maestria, XP e pontos). */
    public static final int TOPO = 42;
    public static final int MARGEM = 10;
    public static final int PAINEL_L = 120;
    /** O {@code galho} de um {@link Alvo} que é o Despertar. */
    public static final int DESPERTAR = -2;

    /** Uma ligação: do tronco à ponta ({@code no} = −1) ou até o nó {@code no} do galho; {@code galho} −1 é o tronco. */
    public record Linha(int x1, int y1, int x2, int y2, int galho, int no) {
    }

    /** O que está sob o mouse: o nó {@code no} do galho, a ponta ({@code no} = −1) ou o Despertar. */
    public record Alvo(int galho, int no) {
    }

    private final List<Integer> nos = new ArrayList<>();
    private final int quantosAntes;
    private final int cx;
    private final int[] yDoGalho;
    private final int yDespertar;
    private final int yRaiz;
    private final int rolagemMaxima;
    private final Retangulo painel;
    private final List<Linha> linhas = new ArrayList<>();

    public LayoutArvore(int largura, int altura, List<Integer> nosAntes, List<Integer> nosDepois) {
        nos.addAll(nosAntes);
        nos.addAll(nosDepois);
        quantosAntes = nosAntes.size();
        int maior = nos.stream().mapToInt(Integer::intValue).max().orElse(0);
        int alcance = PASSO_X * (1 + maior);
        boolean cabePainel = largura >= 2 * alcance + NO + PAINEL_L + 3 * MARGEM;
        int fimDaArvore = cabePainel ? largura - PAINEL_L - 2 * MARGEM : largura - MARGEM;
        cx = (MARGEM + fimDaArvore) / 2;

        // De cima para baixo: os galhos de depois (o mais alto primeiro), o Despertar, os de antes e a raiz.
        yDoGalho = new int[nos.size()];
        int y = TOPO;
        int linhasDepois = (nosDepois.size() + 1) / 2;
        for (int linha = linhasDepois - 1; linha >= 0; linha--, y += PASSO_Y) {
            for (int g = 0; g < nosDepois.size(); g++) if (g / 2 == linha) yDoGalho[quantosAntes + g] = y;
        }
        yDespertar = y;
        y += PASSO_Y;
        int linhasAntes = (quantosAntes + 1) / 2;
        for (int linha = linhasAntes - 1; linha >= 0; linha--, y += PASSO_Y) {
            for (int g = 0; g < quantosAntes; g++) if (g / 2 == linha) yDoGalho[g] = y;
        }
        yRaiz = y;
        rolagemMaxima = Math.max(0, yRaiz + NO + MARGEM - altura);
        painel = cabePainel ? new Retangulo(largura - MARGEM - PAINEL_L, TOPO, PAINEL_L, Math.max(80, altura - TOPO - MARGEM)) : null;

        int meio = NO / 2;
        int topoDoTronco = nosDepois.isEmpty() ? yDespertar : yDoGalho[quantosAntes + nosDepois.size() - 1];
        for (int g = quantosAntes; g < nos.size(); g++) topoDoTronco = Math.min(topoDoTronco, yDoGalho[g]);
        linhas.add(new Linha(cx, yRaiz + meio, cx, yDespertar + meio, -1, -1));
        linhas.add(new Linha(cx, yDespertar + meio, cx, topoDoTronco + meio, -1, -1));
        for (int g = 0; g < nos.size(); g++) {
            int yl = yDoGalho[g] + meio;
            Retangulo ponta = ponta(g);
            linhas.add(new Linha(cx, yl, ponta.x() + meio, yl, g, -1));
            int xAntes = ponta.x() + meio;
            for (int i = 0; i < nos.get(g); i++) {
                int xNo = no(g, i).x() + meio;
                linhas.add(new Linha(xAntes, yl, xNo, yl, g, i));
                xAntes = xNo;
            }
        }
    }

    /** −1 à esquerda, 1 à direita: os galhos de cada grupo se alternam. */
    private int lado(int galho) {
        int noGrupo = galho < quantosAntes ? galho : galho - quantosAntes;
        return noGrupo % 2 == 0 ? -1 : 1;
    }

    private Retangulo celula(int centroX, int y) {
        return new Retangulo(centroX - NO / 2, y, NO, NO);
    }

    public Retangulo raiz() {
        return celula(cx, yRaiz);
    }

    public Retangulo despertar() {
        return celula(cx, yDespertar);
    }

    public Retangulo ponta(int galho) {
        return celula(cx + lado(galho) * PASSO_X, yDoGalho[galho]);
    }

    public Retangulo no(int galho, int i) {
        return celula(cx + lado(galho) * PASSO_X * (i + 2), yDoGalho[galho]);
    }

    public List<Linha> linhas() {
        return linhas;
    }

    /** O painel de detalhe ao lado da árvore, se couber; sem ele, a tela mostra o detalhe junto ao mouse. */
    public Optional<Retangulo> painel() {
        return Optional.ofNullable(painel);
    }

    public int rolagemMaxima() {
        return rolagemMaxima;
    }

    /** O que está sob o ponto da tela, com a tela rolada {@code rolagem} pixels; null se nada (ou o cabeçalho). */
    public Alvo alvoEm(double x, double y, int rolagem) {
        if (y < TOPO) return null;
        double yc = y + rolagem;
        if (despertar().contem(x, yc)) return new Alvo(DESPERTAR, -1);
        for (int g = 0; g < nos.size(); g++) {
            if (ponta(g).contem(x, yc)) return new Alvo(g, -1);
            for (int i = 0; i < nos.get(g); i++) if (no(g, i).contem(x, yc)) return new Alvo(g, i);
        }
        return null;
    }
}
