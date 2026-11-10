package com.minerwk.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.hud.LayoutEscolha.Retangulo;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LayoutArvoreTest {
    private static List<Retangulo> todos(LayoutArvore l) {
        List<Retangulo> r = new ArrayList<>();
        for (int i = 0; i < l.total(); i++) r.add(l.celula(i));
        return r;
    }

    @Test
    void colunasSeguemALarguraDaTela() {
        assertEquals(4, new LayoutArvore(480, 270, 6, 7).colunas());
        assertEquals(3, new LayoutArvore(427, 240, 6, 7).colunas());
        assertEquals(1, new LayoutArvore(150, 240, 6, 7).colunas());
        assertEquals(4, new LayoutArvore(1920, 1080, 6, 7).colunas(), "não passa de quatro");
    }

    @Test
    void celulasNaoSeSobrepoemEFicamDentroDaTela() {
        for (int[] tela : new int[][] {{480, 270}, {427, 240}, {320, 240}, {150, 200}}) {
            LayoutArvore l = new LayoutArvore(tela[0], tela[1], 6, 7);
            List<Retangulo> r = todos(l);
            assertEquals(14, r.size());
            for (int a = 0; a < r.size(); a++) {
                Retangulo ra = r.get(a);
                assertTrue(ra.x() >= 0 && ra.x() + ra.largura() <= tela[0], "célula " + a + " fora na largura " + tela[0]);
                for (int b = a + 1; b < r.size(); b++) {
                    Retangulo rb = r.get(b);
                    boolean separados = ra.x() + ra.largura() <= rb.x() || rb.x() + rb.largura() <= ra.x()
                            || ra.y() + ra.altura() <= rb.y() || rb.y() + rb.altura() <= ra.y();
                    assertTrue(separados, "células " + a + " e " + b + " se sobrepõem em " + tela[0]);
                }
            }
        }
    }

    @Test
    void ordemEAntesDespertarDepoisDeCimaParaBaixo() {
        LayoutArvore l = new LayoutArvore(480, 270, 6, 7);
        assertEquals(6, l.indiceDoDespertar());
        int fimAntes = l.celula(5).y() + l.celula(5).altura();
        assertTrue(l.celula(6).y() >= fimAntes);
        assertTrue(l.celula(7).y() >= l.celula(6).y() + l.celula(6).altura());
        assertTrue(l.celula(0).y() >= LayoutArvore.TOPO);
    }

    @Test
    void linhaIncompletaFicaCentrada() {
        LayoutArvore l = new LayoutArvore(480, 270, 6, 7);
        // Segunda linha de "antes": duas células de quatro.
        Retangulo a = l.celula(4), b = l.celula(5);
        assertEquals(480 - (b.x() + b.largura()), a.x(), 1);
        Retangulo d = l.celula(6);
        assertEquals(480 - (d.x() + d.largura()), d.x(), 1);
    }

    @Test
    void acharONoSobOMouseContaComARolagem() {
        LayoutArvore l = new LayoutArvore(480, 270, 6, 7);
        Retangulo r = l.celula(9);
        assertEquals(9, l.indiceEm(r.x() + 3, r.y() + 3, 0));
        assertEquals(-1, l.indiceEm(r.x() - 2, r.y() + 3, 0), "no espaço entre células");
        assertEquals(9, l.indiceEm(r.x() + 3, r.y() + 3 - 10, 10), "com a tela rolada 10 para baixo");
        assertEquals(-1, l.indiceEm(5, LayoutArvore.TOPO - 4, 0), "o cabeçalho não tem nó");
    }

    @Test
    void rolagemSoExisteQuandoNaoCabe() {
        assertEquals(0, new LayoutArvore(480, 270, 6, 7).rolagemMaxima());
        assertEquals(0, new LayoutArvore(427, 240, 6, 7).rolagemMaxima(), "a árvore do Gojo cabe na janela padrão");
        LayoutArvore baixo = new LayoutArvore(150, 200, 6, 7);
        assertTrue(baixo.rolagemMaxima() > 0);
        Retangulo ultimo = baixo.celula(13);
        assertTrue(ultimo.y() + ultimo.altura() - baixo.rolagemMaxima() <= 200, "rolando tudo, o último nó aparece");
        assertFalse(new LayoutArvore(480, 270, 0, 0).total() != 1, "árvore só com o despertar");
    }
}
