package com.minerwk.core.personagens.gojo;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Vetor;
import org.junit.jupiter.api.Test;

class RoxoMaximoTest {
    private static final Vetor FRENTE = new Vetor(1, 0, 0);

    @Test
    void avancoA3BlocosPorTickParandoA1E5DoAlvo() {
        assertEquals(10, RoxoMaximo.avanco(31.5));
        assertEquals(RoxoMaximo.AVANCO_MAX, RoxoMaximo.avanco(60));
        assertEquals(0, RoxoMaximo.avanco(1), "colado no alvo: uppercut na hora");
    }

    @Test
    void fasesEmSequencia() {
        int a = 10;
        assertEquals(25, RoxoMaximo.inicioEsferas(a));
        assertEquals(35, RoxoMaximo.inicioFusao(a));
        assertEquals(45, RoxoMaximo.inicioExplosao(a));
        assertEquals(61, RoxoMaximo.fim(a));
    }

    @Test
    void gojoAvancaAteOAlvoESobeJunto() {
        double dist = 31.5;
        int a = RoxoMaximo.avanco(dist);
        assertEquals(Vetor.ZERO, RoxoMaximo.deslocamentoGojo(0, FRENTE, dist));
        assertEquals(30, RoxoMaximo.deslocamentoGojo(a, FRENTE, dist).x(), 1e-9, "para a 1,5 do alvo");
        Vetor topo = RoxoMaximo.deslocamentoGojo(a + RoxoMaximo.TICKS_SUBIDA, FRENTE, dist);
        assertEquals(30, topo.x(), 1e-9);
        assertEquals(RoxoMaximo.SUBIDA, topo.y(), 1e-9);
        assertEquals(RoxoMaximo.SUBIDA, RoxoMaximo.deslocamentoGojo(a + 40, FRENTE, dist).y(), 1e-9, "fica lá em cima");
    }

    @Test
    void alvoSobe25Em15TicksSuave() {
        assertEquals(0, RoxoMaximo.alturaAlvo(0), 1e-9);
        assertEquals(RoxoMaximo.SUBIDA, RoxoMaximo.alturaAlvo(15), 1e-9);
        for (int t = 1; t <= 15; t++) assertTrue(RoxoMaximo.alturaAlvo(t) > RoxoMaximo.alturaAlvo(t - 1));
    }

    @Test
    void fusaoQuatroBlocosAtrasDoGojoNaHorizontal() {
        Vetor olhos = new Vetor(10, 90, 5);
        Vetor c = RoxoMaximo.centroFusao(olhos, new Vetor(.6, -.8, 0));
        assertEquals(new Vetor(6, 90, 5), c);
        Vetor reto = RoxoMaximo.centroFusao(olhos, new Vetor(0, -1, 0));
        assertEquals(4, reto.distancia(olhos), 1e-9, "olhando reto para baixo: fica 4 acima");
        assertEquals(94, reto.y(), 1e-9);
    }
}
