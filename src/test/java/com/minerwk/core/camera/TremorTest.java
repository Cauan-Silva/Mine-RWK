package com.minerwk.core.camera;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TremorTest {

    @Test
    void decaiAteZeroERemove() {
        Tremor t = new Tremor();
        t.adicionar(1f, 10);
        assertEquals(1f, t.forcaAtual(), 1e-6);
        for (int i = 0; i < 5; i++) t.tick();
        assertEquals(0.25f, t.forcaAtual(), 1e-6);
        for (int i = 0; i < 5; i++) t.tick();
        assertEquals(0f, t.forcaAtual());
        assertEquals(0, t.ativos());
    }

    @Test
    void doisTremoresSomam() {
        Tremor t = new Tremor();
        t.adicionar(0.4f, 20);
        t.adicionar(0.3f, 20);
        assertEquals(0.7f, t.forcaAtual(), 1e-6);
    }

    @Test
    void somaLimitada() {
        Tremor t = new Tremor();
        for (int i = 0; i < 5; i++) t.adicionar(1f, 20);
        assertEquals(Tremor.LIMITE, t.forcaAtual(), 1e-6);
    }

    @Test
    void intensidadeZeroNaoMexe() {
        Tremor t = new Tremor();
        t.adicionar(1f, 20);
        Tremor.Deslocamento d = t.deslocamento(0.37f, 0f);
        assertEquals(0f, d.guinada());
        assertEquals(0f, d.inclinacao());
        assertEquals(0f, d.rotacao());
    }

    @Test
    void semTremorNaoMexeEComTremorMexe() {
        Tremor t = new Tremor();
        assertEquals(0f, t.deslocamento(0.37f, 1f).guinada());
        t.adicionar(1f, 20);
        Tremor.Deslocamento d = t.deslocamento(0.37f, 1f);
        assertNotEquals(0f, d.guinada());
        assertTrue(Math.abs(d.guinada()) <= Tremor.GRAUS * Tremor.LIMITE);
    }
}
