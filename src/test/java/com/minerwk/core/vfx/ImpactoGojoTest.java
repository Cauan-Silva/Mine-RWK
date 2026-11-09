package com.minerwk.core.vfx;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ImpactoGojoTest {

    @Test
    void quedaCheiaNoPontoEZeroNoRaio() {
        assertEquals(1, ImpactoGojo.queda(0, 24), 1e-9);
        assertEquals(.5, ImpactoGojo.queda(12, 24), 1e-9);
        assertEquals(0, ImpactoGojo.queda(24, 24), 1e-9);
        assertEquals(0, ImpactoGojo.queda(100, 24), 1e-9);
    }

    @Test
    void momentosDaTabela() {
        var v = ImpactoGojo.de("gojo_vermelho_explosao").orElseThrow();
        assertEquals(.8f, v.tremor(), 1e-6);
        assertEquals(24, v.raio(), 1e-9);
        assertTrue(ImpactoGojo.de("gojo_infinito").isEmpty(), "Infinito não reage");
        assertTrue(ImpactoGojo.de("gojo_azul").orElseThrow().fov() < 0, "Azul puxa o FOV");
        float maior = ImpactoGojo.de("gojo_roxo_200").orElseThrow().tremor();
        for (String m : new String[]{"gojo_azul", "gojo_vermelho_explosao", "gojo_roxo", "gojo_vazio_infinito"}) {
            var r = ImpactoGojo.de(m).orElseThrow();
            assertTrue(r.tremor() < maior && r.raio() < 48, m);
        }
    }

    @Test
    void claraoEFovSomem() {
        var r = ImpactoGojo.de("gojo_roxo").orElseThrow();
        assertEquals(.45f, ImpactoGojo.alphaClarao(r, 0), 1e-6);
        for (double t = 0; t < 20; t += .25) {
            float a = ImpactoGojo.alphaClarao(r, t);
            assertTrue(a >= 0 && a <= 1);
        }
        assertEquals(0, ImpactoGojo.alphaClarao(r, r.ticksClarao()), 1e-6);
        assertTrue(ImpactoGojo.fov(r, 0) > 1.07);
        assertEquals(1, ImpactoGojo.fov(r, r.ticksFov()), 1e-9);
        assertEquals(1, ImpactoGojo.fov(r, 500), 1e-9);
    }

    @Test
    void tremorDaCargaDoRoxo200Sobe() {
        assertEquals(.1, ImpactoGojo.tremorCarga200(0, 80), 1e-9);
        assertEquals(.6, ImpactoGojo.tremorCarga200(80, 80), 1e-9);
        assertTrue(ImpactoGojo.tremorCarga200(40, 80) > ImpactoGojo.tremorCarga200(20, 80));
    }
}
