package com.minerwk.core.hud;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.hud.LayoutArvore.Alvo;
import com.minerwk.core.hud.LayoutEscolha.Retangulo;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LayoutArvoreTest {
    /** As formas do Cid e do Ichigo: quantos nós cada galho tem, antes e depois do Despertar. */
    private static final List<List<List<Integer>>> FORMAS = List.of(
            List.of(List.of(2, 1, 1, 2), List.of(2, 1, 1)),
            List.of(List.of(2, 1, 3), List.of(2, 2, 2)),
            // Um personagem futuro com um galho de 4 nós ainda cabe em 320 de largura.
            List.of(List.of(4, 1), List.of(4, 2)));
    private static final int[][] TELAS = {{320, 240}, {854, 480}};

    private static List<Retangulo> todos(LayoutArvore l, List<Integer> antes, List<Integer> depois) {
        List<Retangulo> r = new ArrayList<>();
        r.add(l.raiz());
        r.add(l.despertar());
        List<Integer> nos = new ArrayList<>(antes);
        nos.addAll(depois);
        for (int g = 0; g < nos.size(); g++) {
            r.add(l.ponta(g));
            for (int i = 0; i < nos.get(g); i++) r.add(l.no(g, i));
        }
        return r;
    }

    private static boolean sobrepoe(Retangulo a, Retangulo b) {
        return a.x() < b.x() + b.largura() && b.x() < a.x() + a.largura()
                && a.y() < b.y() + b.altura() && b.y() < a.y() + a.altura();
    }

    @Test
    void nadaSeSobrepoeETudoCabeNaLargura() {
        for (List<List<Integer>> f : FORMAS) {
            for (int[] tela : TELAS) {
                LayoutArvore l = new LayoutArvore(tela[0], tela[1], f.get(0), f.get(1));
                List<Retangulo> r = todos(l, f.get(0), f.get(1));
                for (int i = 0; i < r.size(); i++) {
                    Retangulo a = r.get(i);
                    assertTrue(a.x() >= LayoutArvore.MARGEM && a.x() + a.largura() <= tela[0] - LayoutArvore.MARGEM,
                            a + " fora da largura " + tela[0]);
                    for (int j = i + 1; j < r.size(); j++) assertFalse(sobrepoe(a, r.get(j)), a + " e " + r.get(j));
                }
            }
        }
    }

    @Test
    void umaLinhaPorLigacaoMaisOTronco() {
        LayoutArvore l = new LayoutArvore(854, 480, List.of(2, 1, 1, 2), List.of(2, 1, 1));
        int galhos = 7, nos = 2 + 1 + 1 + 2 + 2 + 1 + 1;
        assertEquals(galhos + nos + 2, l.linhas().size());
    }

    @Test
    void alvoComATelaRolada() {
        LayoutArvore l = new LayoutArvore(320, 240, List.of(2, 1, 3), List.of(2, 2, 2));
        int rolagem = 17;
        Retangulo no = l.no(1, 0);
        assertEquals(new Alvo(1, 0), l.alvoEm(no.x() + no.largura() / 2.0, no.y() + no.altura() / 2.0 - rolagem, rolagem));
        Retangulo d = l.despertar();
        Alvo desp = l.alvoEm(d.x() + d.largura() / 2.0, d.y() + d.altura() / 2.0 - rolagem, rolagem);
        assertNotNull(desp);
        assertEquals(LayoutArvore.DESPERTAR, desp.galho());
        assertNull(l.alvoEm(0, 0, 0));
    }

    @Test
    void painelSoQuandoCabe() {
        LayoutArvore pequeno = new LayoutArvore(320, 240, List.of(2, 1, 3), List.of(2, 2, 2));
        assertTrue(pequeno.painel().isEmpty());
        assertTrue(pequeno.rolagemMaxima() > 0);

        List<Integer> antes = List.of(2, 1, 1, 2), depois = List.of(2, 1, 1);
        LayoutArvore grande = new LayoutArvore(854, 480, antes, depois);
        Retangulo painel = grande.painel().orElseThrow();
        for (Retangulo r : todos(grande, antes, depois)) assertFalse(sobrepoe(painel, r), r.toString());
    }
}
