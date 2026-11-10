package com.minerwk.core.vfx;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Vetor;
import java.util.List;
import org.junit.jupiter.api.Test;

class EncantamentoGojoTest {
    private static double comprimento(List<Vetor> pts) {
        double c = 0;
        for (int i = 1; i < pts.size(); i++) c += pts.get(i).distancia(pts.get(i - 1));
        return c;
    }

    @Test
    void ceuEscureceAosPoucosNaCargaSemChegarAoEscuroDaExplosao() {
        assertEquals(0, EncantamentoGojo.ceuCarga(0, 80), 1e-9);
        assertEquals(EncantamentoGojo.CEU_CARGA, EncantamentoGojo.ceuCarga(80, 80), 1e-9);
        assertEquals(EncantamentoGojo.CEU_CARGA, EncantamentoGojo.ceuCarga(200, 80), 1e-9);
        assertTrue(EncantamentoGojo.CEU_CARGA < 1, "o escuro cheio fica para a fusão e a explosão");
        for (int t = 1; t <= 80; t++) {
            assertTrue(EncantamentoGojo.ceuCarga(t, 80) > EncantamentoGojo.ceuCarga(t - 1, 80));
        }
    }

    @Test
    void ceuSoEscureceParaQuemEstaPerto() {
        assertEquals(1, EncantamentoGojo.perto(0), 1e-9);
        assertEquals(0, EncantamentoGojo.perto(EncantamentoGojo.ALCANCE_CEU), 1e-9);
        assertEquals(0, EncantamentoGojo.perto(500), 1e-9);
        assertTrue(EncantamentoGojo.perto(10) > EncantamentoGojo.perto(40));
    }

    @Test
    void rachadurasCrescemDosPesSemSairDoChao() {
        for (int i = 0; i < EncantamentoGojo.RACHADURAS; i++) {
            assertEquals(List.of(Vetor.ZERO), EncantamentoGojo.rachadura(7, i, 0), "no começo não há rachadura");
            double antes = 0;
            for (double k = .1; k <= 1.0001; k += .1) {
                List<Vetor> pts = EncantamentoGojo.rachadura(7, i, k);
                assertEquals(Vetor.ZERO, pts.get(0), "saem dos pés");
                double c = comprimento(pts);
                assertTrue(c > antes, "só cresce");
                antes = c;
                for (Vetor p : pts) {
                    assertEquals(0, p.y(), 1e-9);
                    assertTrue(p.comprimento() <= EncantamentoGojo.ALCANCE_RACHADURA + 1e-9);
                }
            }
            assertTrue(antes > 2, "no fim da carga a rachadura é comprida");
        }
    }

    @Test
    void rachaduraNaoMudaDeLugarEnquantoCresce() {
        List<Vetor> meio = EncantamentoGojo.rachadura(42, 3, .5), cheia = EncantamentoGojo.rachadura(42, 3, 1);
        assertEquals(cheia, EncantamentoGojo.rachadura(42, 3, 1), "mesma semente, mesmo desenho");
        for (int j = 0; j < meio.size() - 1; j++) assertEquals(cheia.get(j), meio.get(j), "a parte já aberta fica onde está");
        assertNotEquals(cheia, EncantamentoGojo.rachadura(43, 3, 1), "cada uso racha diferente");
    }
}
