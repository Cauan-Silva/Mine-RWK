package com.minerwk.core.vfx;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Vetor;
import org.junit.jupiter.api.Test;

class TrajetoriaGojoTest {
    private static final Vetor DIR = new Vetor(1, 0, 0);

    @Test
    void esferaVermelhaAnda1PorTick() {
        Vetor inicio = new Vetor(1, 0, 0);
        assertEquals(inicio, TrajetoriaGojo.vermelho(inicio, DIR, 0));
        assertEquals(new Vetor(31, 0, 0), TrajetoriaGojo.vermelho(inicio, DIR, 30));
    }

    @Test
    void roxoDaOPrimeiroPassoNoTickZero() {
        assertEquals(new Vetor(2, 0, 0), TrajetoriaGojo.roxo(Vetor.ZERO, DIR, 2, 0));
        assertEquals(new Vetor(40, 0, 0), TrajetoriaGojo.roxo(Vetor.ZERO, DIR, 2, 19));
        assertEquals(new Vetor(120, 0, 0), TrajetoriaGojo.roxo(Vetor.ZERO, DIR, 4, 29));
    }

    @Test
    void roxoEntreTicksInterpola() {
        assertEquals(3, TrajetoriaGojo.roxo(Vetor.ZERO, DIR, 2, 0.5).x(), 1e-9);
    }
}
