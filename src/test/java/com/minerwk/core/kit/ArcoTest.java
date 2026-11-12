package com.minerwk.core.kit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** O arco de um corte horizontal: quem está à frente, dentro do alcance, do ângulo e da altura. */
class ArcoTest {
    private static final Vetor FRENTE = new Vetor(1, 0, 0);

    private static boolean dentro(double x, double y, double z) {
        return Arco.contem(Vetor.ZERO, FRENTE, new Vetor(x, y, z), 12, 120, 3);
    }

    @Test
    void pegaQuemEstaAFrenteAteOAlcance() {
        assertTrue(dentro(1, 0, 0));
        assertTrue(dentro(12, 0, 0));
        assertFalse(dentro(12.1, 0, 0));
        assertFalse(dentro(-1, 0, 0), "atrás não");
    }

    @Test
    void oArcoDeCentoEVinteGrausAbreSessentaParaCadaLado() {
        double a = Math.toRadians(59), b = Math.toRadians(61);
        assertTrue(dentro(5 * Math.cos(a), 0, 5 * Math.sin(a)));
        assertTrue(dentro(5 * Math.cos(a), 0, -5 * Math.sin(a)));
        assertFalse(dentro(5 * Math.cos(b), 0, 5 * Math.sin(b)));
        assertFalse(dentro(0, 0, 5), "de lado não");
    }

    @Test
    void oCorteEHorizontalAAlturaTemLimite() {
        assertTrue(dentro(5, 3, 0));
        assertTrue(dentro(5, -3, 0));
        assertFalse(dentro(5, 3.1, 0));
        // A distância e o ângulo são medidos no chão: a altura não encurta o alcance.
        assertTrue(dentro(12, 2, 0));
    }

    @Test
    void olharParaCimaNaoMudaOArcoEQuemEstaColadoConta() {
        Vetor inclinado = new Vetor(1, 5, 0);
        assertTrue(Arco.contem(Vetor.ZERO, inclinado, new Vetor(8, 0, 0), 12, 120, 3));
        assertFalse(Arco.contem(Vetor.ZERO, inclinado, new Vetor(-8, 0, 0), 12, 120, 3));
        assertTrue(Arco.contem(Vetor.ZERO, FRENTE, new Vetor(0, 1, 0), 12, 120, 3), "em cima dele: sem ângulo para medir, conta");
        // Olhando reto para cima não há frente: vale a volta toda.
        assertTrue(Arco.contem(Vetor.ZERO, new Vetor(0, 1, 0), new Vetor(-4, 0, 0), 12, 120, 3));
    }
}
