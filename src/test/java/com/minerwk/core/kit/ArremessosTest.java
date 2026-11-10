package com.minerwk.core.kit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ArremessosTest {

    @Test
    void congelaDepoisLancaGiraEAcaba() {
        var e = new Arremessos.Estado(new Vetor(2, .5, 0), 3);
        for (int i = 0; i < 3; i++) assertEquals(Arremessos.Passo.CONGELAR, e.tick(true));
        assertEquals(Arremessos.Passo.LANCAR, e.tick(true));
        // Logo depois do lançamento ainda pode estar no chão: gira mesmo assim.
        assertEquals(Arremessos.Passo.GIRAR, e.tick(true));
        assertEquals(Arremessos.Passo.GIRAR, e.tick(false));
        assertEquals(Arremessos.Passo.GIRAR, e.tick(false));
        assertEquals(Arremessos.Passo.FIM, e.tick(true), "tocou o chão depois de voar");
    }

    @Test
    void giroParaDepoisDe20Ticks() {
        var e = new Arremessos.Estado(Vetor.ZERO, 0);
        assertEquals(Arremessos.Passo.LANCAR, e.tick(true));
        for (int i = 0; i < Arremessos.GIRO_MAX; i++) assertEquals(Arremessos.Passo.GIRAR, e.tick(false));
        assertEquals(Arremessos.Passo.FIM, e.tick(false));
    }

    @Test
    void velocidadeParaLongeDoCentroComForcaEUmPoucoParaCima() {
        Vetor v = Arremessos.velocidade(Vetor.ZERO, new Vetor(3, 0, 0), 4, null);
        assertEquals(4, v.x(), 1e-9);
        assertTrue(v.y() > 0 && v.y() <= 1);
        assertEquals(0, v.z(), 1e-9);
    }

    @Test
    void comEixoSaiParaOLado() {
        // Alvo à frente e um pouco ao lado do eixo X: sai pelo lado (z), não para a frente.
        Vetor v = Arremessos.velocidade(Vetor.ZERO, new Vetor(1, 0, .5), 2, new Vetor(1, 0, 0));
        assertEquals(0, v.x(), 1e-9);
        assertEquals(2, v.z(), 1e-9);
    }

    @Test
    void alvoNoCentroVaiParaCima() {
        Vetor v = Arremessos.velocidade(Vetor.ZERO, Vetor.ZERO, 2, null);
        assertTrue(v.y() > 0);
        assertTrue(Double.isFinite(v.x()) && Double.isFinite(v.z()));
    }
}
