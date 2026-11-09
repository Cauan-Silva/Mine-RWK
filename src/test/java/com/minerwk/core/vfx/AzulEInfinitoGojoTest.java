package com.minerwk.core.vfx;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Vetor;
import org.junit.jupiter.api.Test;

class AzulEInfinitoGojoTest {

    @Test
    void espiralDoAzulApertaAteONucleo() {
        for (int f = 0; f < 4; f++) {
            double antes = Double.MAX_VALUE;
            for (int k = 0; k <= 20; k++) {
                double r = FluxoAzulGojo.espiral(f, k / 20.0, 7).comprimento();
                assertTrue(r < antes + 1e-9, "a espiral fecha a cada passo");
                antes = r;
            }
            assertEquals(FluxoAzulGojo.raio(7), FluxoAzulGojo.espiral(f, 1, 7).comprimento(), .05, "termina no núcleo");
        }
    }

    @Test
    void anelDeSuccaoNasceLargoESeFechaVariasVezesPorSegundo() {
        double largo = FluxoAzulGojo.anelSuccao(0, 0);
        double meio = FluxoAzulGojo.anelSuccao(0, FluxoAzulGojo.CICLO_ANEL / 2.0);
        assertTrue(largo > meio && meio > FluxoAzulGojo.raio(0));
        int ciclo = FluxoAzulGojo.CICLO_ANEL;
        assertEquals(largo - FluxoAzulGojo.raio(0), FluxoAzulGojo.anelSuccao(0, ciclo) - FluxoAzulGojo.raio(ciclo), 1e-9, "repete");
        assertTrue(FluxoAzulGojo.CICLO_ANEL <= 10, "várias vezes por segundo");
    }

    @Test
    void ondaDoToqueSaiDoPontoEAbrePelaBolha() {
        assertEquals(0, BolhaInfinitoGojo.angulo(0, 0), 1e-9);
        assertTrue(BolhaInfinitoGojo.angulo(0, 8) > BolhaInfinitoGojo.angulo(0, 4));
        assertTrue(BolhaInfinitoGojo.angulo(1, 8) < BolhaInfinitoGojo.angulo(0, 8), "a segunda onda vem atrás");
        assertTrue(BolhaInfinitoGojo.angulo(0, 1000) <= Math.PI / 2, "não passa do meio da bolha");
        assertEquals(0, BolhaInfinitoGojo.alphaOnda(0, BolhaInfinitoGojo.TICKS_ONDA), 1e-9, "some no fim");
    }

    @Test
    void hexagonosEspalhadosNaBolha() {
        int n = BolhaInfinitoGojo.HEXAGONOS;
        for (int i = 0; i < n; i++) {
            Vetor d = BolhaInfinitoGojo.direcaoHexagono(i);
            assertEquals(1, d.comprimento(), 1e-9);
            for (int j = 0; j < i; j++) assertTrue(d.distancia(BolhaInfinitoGojo.direcaoHexagono(j)) > .3, "não se amontoam");
            for (double t = 0; t < 10; t += .5) {
                double a = BolhaInfinitoGojo.brilhoHexagono(i, t);
                assertTrue(a >= 0 && a <= 1);
            }
        }
    }
}
