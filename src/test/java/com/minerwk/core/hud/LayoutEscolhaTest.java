package com.minerwk.core.hud;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LayoutEscolhaTest {
    private static final int PASSO_X = LayoutEscolha.EMBLEMA_L + LayoutEscolha.ESPACO;
    private static final int PASSO_Y = LayoutEscolha.EMBLEMA_A + LayoutEscolha.ESPACO;

    @Test
    void colunasCabemNaLargura() {
        LayoutEscolha l = new LayoutEscolha(400, 240, 15);
        assertTrue(l.colunas() >= 1);
        assertTrue(l.colunas() * PASSO_X - LayoutEscolha.ESPACO <= l.grade().largura());
        assertTrue(l.painel().x() >= l.grade().x() + l.grade().largura(), "painel à direita da grade");
    }

    @Test
    void rolagemSoQuandoNaoCabe() {
        assertEquals(0, new LayoutEscolha(400, 240, 3).rolagemMaxima());
        assertTrue(new LayoutEscolha(400, 240, 40).rolagemMaxima() > 0);
    }

    @Test
    void indiceEmAcertaCelula() {
        LayoutEscolha l = new LayoutEscolha(400, 240, 40);
        int x0 = l.grade().x();
        int y0 = l.grade().y();
        int meio = LayoutEscolha.EMBLEMA_L / 2;
        assertEquals(0, l.indiceEm(x0 + meio, y0 + 5, 0));
        assertEquals(1, l.indiceEm(x0 + PASSO_X + meio, y0 + 5, 0));
        assertEquals(-1, l.indiceEm(x0 + LayoutEscolha.EMBLEMA_L + 2, y0 + 5, 0), "espaço entre células");
        assertEquals(l.colunas(), l.indiceEm(x0 + meio, y0 + 5, 1), "com uma linha de rolagem");
        assertEquals(l.colunas() + 1, l.indiceEm(x0 + PASSO_X + meio, y0 + PASSO_Y + 5, 0));
        assertEquals(-1, l.indiceEm(x0 - 3, y0 + 5, 0), "fora da grade");
    }

    @Test
    void indiceAlemDoTotalEhMenosUm() {
        LayoutEscolha l = new LayoutEscolha(400, 240, 1);
        assertEquals(-1, l.indiceEm(l.grade().x() + PASSO_X + 5, l.grade().y() + 5, 0));
    }

    @Test
    void painelVaiParaBaixoEmTelaEstreita() {
        LayoutEscolha l = new LayoutEscolha(300, 300, 15);
        assertTrue(l.painel().y() >= l.grade().y() + l.grade().altura(), "painel abaixo da grade");
        assertTrue(l.colunas() * PASSO_X - LayoutEscolha.ESPACO <= 300);
    }
}
