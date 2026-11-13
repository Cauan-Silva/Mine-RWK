package com.minerwk.core.arena;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.arena.AnelHorizonte.Quad;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnelHorizonteTest {
    private static final double EPS = 1e-6;

    private static List<Quad> anel() {
        return AnelHorizonte.anel(AnelHorizonte.RAIO_MINIMO, AnelHorizonte.Y_BASE, AnelHorizonte.Y_TOPO, 8);
    }

    @Test
    void fechaOs360Graus() {
        List<Quad> q = anel();
        assertEquals(AnelHorizonte.FAIXAS * 8, q.size());
        for (int i = 0; i < q.size(); i++) {
            Quad a = q.get(i), b = q.get((i + 1) % q.size());
            assertEquals(a.x1(), b.x0(), EPS, "emenda em x no quad " + i);
            assertEquals(a.z1(), b.z0(), EPS, "emenda em z no quad " + i);
        }
        double soma = 0;
        for (Quad a : q) soma += Math.abs(Math.atan2(a.x0() * a.z1() - a.z0() * a.x1(), a.x0() * a.x1() + a.z0() * a.z1()));
        assertEquals(2 * Math.PI, soma, 1e-9);
    }

    @Test
    void faixasEmendam() {
        List<Quad> q = anel();
        for (int f = 0; f < AnelHorizonte.FAIXAS; f++) {
            Quad primeiro = q.get(f * 8), ultimo = q.get(f * 8 + 7);
            assertEquals(f, primeiro.faixa());
            assertEquals(0f, primeiro.u0(), 1e-6);
            assertEquals(1f, ultimo.u1(), 1e-6);
            for (int i = f * 8; i < f * 8 + 7; i++) assertEquals(q.get(i).u1(), q.get(i + 1).u0(), 1e-6);
        }
    }

    @Test
    void ficaForaDaCaixaEAbaixoDaEscadaria() {
        assertTrue(AnelHorizonte.RAIO_MINIMO > CaixaArena.MEIA * Math.sqrt(2), "o anel atravessaria a caixa");
        for (Quad a : anel()) {
            assertTrue(Math.hypot(a.x0(), a.z0()) >= AnelHorizonte.RAIO_MINIMO - EPS);
            assertEquals(AnelHorizonte.Y_BASE, a.yBase(), EPS);
        }
        int pe = PlantaEscola.chao(Integer.MAX_VALUE / 2);
        assertTrue(AnelHorizonte.Y_BASE < pe - 5, "a base deveria ficar abaixo do pé da escadaria (" + pe + ")");
        assertThrows(IllegalArgumentException.class, () -> AnelHorizonte.anel(40, 0, 10, 8));
    }

    @Test
    void faixaZeroOlhaParaOSulEAFaixaUmParaOOeste() {
        List<Quad> q = anel();
        Quad meio0 = q.get(4), meio1 = q.get(12);
        assertEquals(0, meio0.x0(), EPS);
        assertTrue(meio0.z0() > 0, "a faixa 0 (a cidade) fica ao sul");
        // Olhando para o sul, a faixa seguinte fica à direita, a oeste: a imagem não sai espelhada.
        assertTrue(meio1.x0() < 0 && Math.abs(meio1.z0()) < EPS, "a faixa 1 fica a oeste");
    }
}
