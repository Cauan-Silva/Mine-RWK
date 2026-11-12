package com.minerwk.core.hud;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProntoTest {

    @Test
    void naoPiscaNoLogin() {
        Pronto p = new Pronto();
        p.ver("a", false, 100);
        assertEquals(-1, p.prontaEm("a"));
    }

    @Test
    void piscaNaTransicao() {
        Pronto p = new Pronto();
        p.ver("a", true, 100);
        p.ver("a", false, 140);
        assertEquals(140, p.prontaEm("a"));
        p.ver("a", false, 150);
        assertEquals(140, p.prontaEm("a"));
    }

    @Test
    void limpar() {
        Pronto p = new Pronto();
        p.ver("a", true, 100);
        p.ver("a", false, 140);
        p.limpar();
        assertEquals(-1, p.prontaEm("a"));
    }
}
