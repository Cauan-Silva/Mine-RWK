package com.minerwk.core.vfx;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FormasIchigoTest {
    @Test
    void aMeiaLuaAndaComOGolpeEParaNoAlcance() {
        assertEquals(0, GetsugaIchigo.distancia(0, false));
        assertEquals(1.6, GetsugaIchigo.distancia(1, false), 1e-9);
        assertEquals(24, GetsugaIchigo.distancia(GetsugaIchigo.voo(false), false), 1e-9);
        assertEquals(24, GetsugaIchigo.distancia(99, false), 1e-9);
        assertEquals(28, GetsugaIchigo.distancia(GetsugaIchigo.voo(true), true), 1e-9);
        assertEquals(GetsugaIchigo.voo(true) + GetsugaIchigo.DISSIPA, GetsugaIchigo.duracao(true));
    }

    @Test
    void aMeiaLuaQueBateNumBlocoParaNoPontoDoServidor() {
        // O servidor avisa um tick depois do último passo dado: a meia-lua não pode andar esse passo a mais.
        assertEquals(3, GetsugaIchigo.parada(4, 4.8, false), 1e-9);
        assertEquals(4.8, GetsugaIchigo.distancia(GetsugaIchigo.parada(4, 4.8, false), false), 1e-9);
        assertEquals(0, GetsugaIchigo.parada(1, 0, true), 1e-9);
        // Um aviso que chega cedo vale como está.
        assertEquals(2, GetsugaIchigo.parada(2, 24, false), 1e-9);
    }

    @Test
    void aMeiaLuaTemOTamanhoDoDano() {
        for (boolean bankai : new boolean[] {false, true}) {
            double alto = 0;
            for (int i = 0; i < GetsugaIchigo.PONTOS; i++) {
                GetsugaIchigo.P p = GetsugaIchigo.frente(i, bankai);
                alto = Math.max(alto, Math.hypot(p.x(), p.y()));
            }
            assertEquals(1.5, alto, .25, "a meia altura do corte fica perto do raio do dano");
        }
    }

    @Test
    void aMeiaLuaEGrossaNoMeioEFinaNasPontas() {
        int meio = GetsugaIchigo.PONTOS / 2;
        assertTrue(largura(meio) > .5);
        assertTrue(largura(0) < .15);
        assertTrue(largura(GetsugaIchigo.PONTOS - 1) < .15);
    }

    private static double largura(int i) {
        GetsugaIchigo.P f = GetsugaIchigo.frente(i, false), t = GetsugaIchigo.tras(i, 3, false);
        return Math.sqrt(Math.pow(f.x() - t.x(), 2) + Math.pow(f.y() - t.y(), 2) + Math.pow(f.z() - t.z(), 2));
    }

    @Test
    void aLuzDoVolumeFicaNosLimites() {
        for (int i = 0; i < GetsugaIchigo.PONTOS; i++) {
            for (int j = 0; j < GetsugaIchigo.SECAO; j++) {
                GetsugaIchigo.P n = GetsugaIchigo.normal(i, j, 5, true);
                assertEquals(1, Math.sqrt(n.x() * n.x() + n.y() * n.y() + n.z() * n.z()), 1e-6);
                double luz = GetsugaIchigo.luz(n, .3, .2, -1);
                assertTrue(luz >= .28 && luz <= 1.25, "luz " + luz);
            }
        }
    }

    @Test
    void aMeiaLuaCresceESeDesfaz() {
        assertEquals(.35, GetsugaIchigo.escala(0), 1e-9);
        assertEquals(1, GetsugaIchigo.escala(GetsugaIchigo.CRESCE), 1e-9);
        assertEquals(0, GetsugaIchigo.dissolve(10, 10), 1e-9);
        assertEquals(1, GetsugaIchigo.dissolve(10 + GetsugaIchigo.DISSIPA, 10), 1e-9);
        // As lascas saem do fio e só se afastam.
        GetsugaIchigo.P antes = GetsugaIchigo.lasca(0, 10, 10, false), depois = GetsugaIchigo.lasca(0, 14, 10, false);
        assertTrue(Math.hypot(depois.x(), depois.y()) > Math.hypot(antes.x(), antes.y()));
    }

    @Test
    void aZangetsuVaiParaEVolta() {
        assertEquals(0, FormasIchigo.arremesso(0, 10));
        assertEquals(5, FormasIchigo.arremesso(4, 10), 1e-9);
        assertEquals(10, FormasIchigo.arremesso(FormasIchigo.ARREMESSO_IDA, 10), 1e-9);
        assertEquals(5, FormasIchigo.arremesso(12, 10), 1e-9);
        assertEquals(0, FormasIchigo.arremesso(FormasIchigo.ARREMESSO, 10), 1e-9);
        // Com parede no caminho ela para antes e espera a hora de voltar.
        assertEquals(2.5, FormasIchigo.arremesso(6, 2.5), 1e-9);
    }

    @Test
    void aZangetsuParaEmPassosInteirosAntesDoBloco() {
        assertEquals(10, FormasIchigo.alcanceDoArremesso(100), 1e-9);
        assertEquals(2.5, FormasIchigo.alcanceDoArremesso(3), 1e-9);
        assertEquals(0, FormasIchigo.alcanceDoArremesso(.4), 1e-9);
    }

    @Test
    void aOndaAbreAteORaioDoGolpe() {
        assertEquals(0, FormasIchigo.raioDaOnda(0), 1e-9);
        assertEquals(FormasIchigo.ONDA_RAIO, FormasIchigo.raioDaOnda(FormasIchigo.ONDA), 1e-9);
        double antes = 0;
        for (int t = 1; t <= FormasIchigo.ONDA; t++) {
            assertTrue(FormasIchigo.raioDaOnda(t) >= antes);
            antes = FormasIchigo.raioDaOnda(t);
        }
        assertEquals(0, FormasIchigo.alturaDaOnda(FormasIchigo.ONDA), 1e-9);
    }

    @Test
    void oPilarDoBankaiSobeESome() {
        assertEquals(0, FormasIchigo.raioDoPilar(0), 1e-9);
        assertTrue(FormasIchigo.raioDoPilar(4) > 1.5);
        assertEquals(0, FormasIchigo.raioDoPilar(FormasIchigo.BANKAI), 1e-9);
        assertTrue(FormasIchigo.alturaDoPilar(6) > 10);
        assertTrue(FormasIchigo.posImagem(0, 1) > FormasIchigo.posImagem(0, 3));
        assertEquals(0, FormasIchigo.posImagem(0, 5), 1e-9);
    }
}
