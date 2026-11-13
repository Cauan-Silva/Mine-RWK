package com.minerwk.core.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A camada de poder comum aos personagens: as contas, sem o Minecraft. */
class PoderTest {

    @Test
    void oPadraoDoSpec() {
        assertEquals(new Poder(20, .5, 3, 2, 2), Poder.PADRAO);
    }

    @Test
    void oGolpeDoWardenChegaPelaMetade() {
        assertEquals(15, Poder.PADRAO.recebido(30), 1e-6);
    }

    @Test
    void osGolpesDoKitFeremTresVezes() {
        assertEquals(90, Poder.PADRAO.golpe(30), 1e-6);
    }

    @Test
    void osBossesTemODobroDeVidaEDeDano() {
        assertEquals(480, Poder.PADRAO.vidaDoBoss(240), 1e-6);
        assertEquals(20, Poder.PADRAO.danoDoBoss(10), 1e-6);
    }

    @Test
    void semCamadaNadaMuda() {
        Poder neutro = new Poder(0, 0, 1, 1, 1);
        assertEquals(7, neutro.recebido(7), 1e-6);
        assertEquals(7, neutro.golpe(7), 1e-6);
    }

    @Test
    void danoNegativoNaoViraCura() {
        assertEquals(0, Poder.PADRAO.recebido(-3), 1e-6);
        assertEquals(0, Poder.PADRAO.golpe(-3), 1e-6);
        assertEquals(0, Poder.PADRAO.danoDoBoss(-3), 1e-6);
    }
}
