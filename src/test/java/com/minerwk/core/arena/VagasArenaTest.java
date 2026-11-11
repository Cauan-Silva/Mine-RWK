package com.minerwk.core.arena;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VagasArenaTest {

    @Test
    void reservaAMenorLivre() {
        VagasArena v = new VagasArena();
        assertEquals(0, v.reservar());
        assertEquals(1, v.reservar());
        assertEquals(2, v.reservar());
    }

    @Test
    void vagaLiberadaEReaproveitada() {
        VagasArena v = new VagasArena();
        v.reservar();
        v.reservar();
        v.reservar();
        v.liberar(1);
        assertFalse(v.ocupada(1));
        assertTrue(v.ocupada(0));
        assertEquals(1, v.reservar());
        assertEquals(3, v.reservar());
    }

    @Test
    void limparLiberaTudo() {
        VagasArena v = new VagasArena();
        v.reservar();
        v.reservar();
        v.limpar();
        assertFalse(v.ocupada(0));
        assertEquals(0, v.reservar());
    }

    @Test
    void liberarVagaNaoReservadaNaoLanca() {
        VagasArena v = new VagasArena();
        assertDoesNotThrow(() -> v.liberar(7));
        assertEquals(0, v.reservar());
    }

    @Test
    void contaAsOcupadas() {
        VagasArena v = new VagasArena();
        assertEquals(0, v.ocupadas());
        v.reservar();
        v.reservar();
        assertEquals(2, v.ocupadas());
        v.liberar(0);
        assertEquals(1, v.ocupadas());
    }
}
