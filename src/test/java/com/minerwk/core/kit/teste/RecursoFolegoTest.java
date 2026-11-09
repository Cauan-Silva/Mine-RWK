package com.minerwk.core.kit.teste;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.EstadoDoJogador;
import org.junit.jupiter.api.Test;

class RecursoFolegoTest {
    private static final EstadoDoJogador PARADO = new EstadoDoJogador(true, false, false);
    private static final EstadoDoJogador ANDANDO = new EstadoDoJogador(false, false, false);

    @Test
    void encheParadoMaisRapido() {
        RecursoFolego r = new RecursoFolego();
        r.carregar(50);
        for (int i = 0; i < 10; i++) r.tick(PARADO);
        assertEquals(55, r.atual(), 1e-9);
        r.carregar(50);
        for (int i = 0; i < 10; i++) r.tick(ANDANDO);
        assertEquals(50.5, r.atual(), 1e-9);
    }

    @Test
    void naoPassaDoMaximo() {
        RecursoFolego r = new RecursoFolego();
        for (int i = 0; i < 100; i++) r.tick(PARADO);
        assertEquals(100, r.atual());
    }

    @Test
    void carregarLimitaValor() {
        RecursoFolego r = new RecursoFolego();
        r.carregar(-5);
        assertEquals(0, r.atual());
        r.carregar(999);
        assertEquals(100, r.atual());
        r.carregar(Double.NaN);
        assertEquals(0, r.atual());
    }

    @Test
    void gastarSemTerLanca() {
        RecursoFolego r = new RecursoFolego();
        r.carregar(10);
        assertFalse(r.pode(20));
        assertThrows(IllegalStateException.class, () -> r.gastar(20));
        r.gastar(10);
        assertEquals(0, r.atual());
    }
}
