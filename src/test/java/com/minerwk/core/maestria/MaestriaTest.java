package com.minerwk.core.maestria;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MaestriaTest {
    @Test
    void curvaDeXpCresceDezPorNivel() {
        assertEquals(100, Maestria.xpParaProximo(1));
        assertEquals(110, Maestria.xpParaProximo(2));
        assertEquals(290, Maestria.xpParaProximo(20));
        assertEquals(0, Maestria.xpParaProximo(Maestria.NIVEL_MAX));
    }

    @Test
    void xpSomadoDosMarcosDoSpec() {
        assertEquals(0, Maestria.xpAte(1));
        assertEquals(1_260, Maestria.xpAte(10));
        assertEquals(3_610, Maestria.xpAte(20));
        assertEquals(16_660, Maestria.xpAte(50));
        assertEquals(58_410, Maestria.xpAte(100));
    }

    @Test
    void nivelSaiDoXpTotal() {
        assertEquals(1, Maestria.nivel(0));
        assertEquals(1, Maestria.nivel(99));
        assertEquals(2, Maestria.nivel(100));
        assertEquals(19, Maestria.nivel(3_609));
        assertEquals(20, Maestria.nivel(3_610));
        assertEquals(100, Maestria.nivel(58_410));
        assertEquals(100, Maestria.nivel(Long.MAX_VALUE));
        assertEquals(1, Maestria.nivel(-5));
    }

    @Test
    void progressoDentroDoNivel() {
        Maestria.Progresso p = Maestria.progresso(150);
        assertEquals(2, p.nivel());
        assertEquals(50, p.xpNoNivel());
        assertEquals(110, p.xpParaProximo());
        Maestria.Progresso topo = Maestria.progresso(999_999);
        assertEquals(100, topo.nivel());
        assertEquals(0, topo.xpNoNivel());
        assertEquals(0, topo.xpParaProximo());
    }

    @Test
    void cadaNivelGanhoDaUmPonto() {
        assertEquals(0, Maestria.pontos(1));
        assertEquals(19, Maestria.pontos(20));
        assertEquals(99, Maestria.pontos(100));
    }

    @Test
    void xpPorDanoSoEmInimigoEComTeto() {
        assertEquals(0, Maestria.xpPorDano(8, false));
        assertEquals(8, Maestria.xpPorDano(8, true));
        assertEquals(1, Maestria.xpPorDano(.4f, true));
        assertEquals(20, Maestria.xpPorDano(500, true));
        assertEquals(0, Maestria.xpPorDano(0, true));
        assertEquals(0, Maestria.xpPorDano(Float.NaN, true));
    }

    @Test
    void xpPorAbateSoEmInimigo() {
        assertEquals(25, Maestria.xpPorAbate(true));
        assertEquals(0, Maestria.xpPorAbate(false));
    }

    @Test
    void somarNaoPassaDoTetoNemVoltaAtras() {
        assertEquals(130, Maestria.somar(100, 30));
        assertEquals(100, Maestria.somar(100, -30));
        assertEquals(Maestria.XP_MAX, Maestria.somar(Maestria.XP_MAX - 5, 30));
    }
}
