package com.minerwk.core.vfx;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ImpactFrameGojoTest {
    @Test
    void duraTresTicksJuntoComOSoco() {
        assertEquals(3, ImpactFrameGojo.TICKS);
        assertTrue(ImpactFrameGojo.dispara(10, 10, 5), "na idade do uppercut, perto");
        assertFalse(ImpactFrameGojo.dispara(9, 10, 5));
        assertFalse(ImpactFrameGojo.dispara(11, 10, 5), "só uma vez");
        assertFalse(ImpactFrameGojo.dispara(10, 10, ImpactFrameGojo.ALCANCE + 1), "longe não vê");
        assertTrue(ImpactFrameGojo.dispara(0, 0, 5), "soco sem corrida (alvo colado)");
    }

    @Test
    void alternaSilhuetasEApagaNoFim() {
        assertEquals(1, ImpactFrameGojo.forca(0), 1e-9);
        assertEquals(1, ImpactFrameGojo.forca(1), 1e-9);
        assertTrue(ImpactFrameGojo.forca(2.5) < 1, "o último tick some");
        assertEquals(0, ImpactFrameGojo.forca(ImpactFrameGojo.TICKS), 1e-9);
        assertEquals(0, ImpactFrameGojo.forca(-1), 1e-9);
        assertNotEquals(ImpactFrameGojo.invertido(0), ImpactFrameGojo.invertido(1), "pisca invertendo, como nos animes");
        assertEquals(ImpactFrameGojo.invertido(0), ImpactFrameGojo.invertido(2));
    }
}
