package com.minerwk.core.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CurvaEscurecerTest {
    private static double em(double tick) {
        return CurvaEscurecer.opacidade(tick, 10, 20, 15);
    }

    @Test
    void comecaClaraEscureceFicaPretaEClareia() {
        assertEquals(0, em(0), 1e-9);
        assertEquals(1, em(10), 1e-9);
        assertEquals(1, em(29), 1e-9);
        assertEquals(1, em(30), 1e-9);
        assertEquals(0, em(45), 1e-9);
    }

    @Test
    void asRampasSaoRetas() {
        assertEquals(.5, em(5), 1e-9);
        assertEquals(.25, em(2.5), 1e-9);
        assertEquals(1 - 7.5 / 15, em(37.5), 1e-9);
    }

    @Test
    void foraDoTempoNaoEscurece() {
        assertEquals(0, em(-1), 1e-9);
        assertEquals(0, em(46), 1e-9);
        assertEquals(0, em(1000), 1e-9);
    }

    @Test
    void semRampaJaComecaPreta() {
        assertEquals(1, CurvaEscurecer.opacidade(0, 0, 5, 15), 1e-9);
        assertEquals(1, CurvaEscurecer.opacidade(5, 0, 5, 15), 1e-9);
        assertEquals(0, CurvaEscurecer.opacidade(20, 0, 5, 0), 1e-9);
        assertEquals(1, CurvaEscurecer.opacidade(3, 0, 5, 0), 1e-9);
    }
}
