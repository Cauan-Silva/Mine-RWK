package com.minerwk.core.kit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BarreiraTest {
    private static final Vetor C = new Vetor(0, 64, 0);
    private static final double R = 12;
    private static final double CHAO = 62.38;

    @Test
    void quemEstaDentroNaoPassaDaBordaEPerdeAVelocidadeParaFora() {
        var c = Barreira.corrigir(C, R, CHAO, true, new Vetor(12, 64, 0), new Vetor(.5, 0, .3));
        assertEquals(R - Barreira.MARGEM, c.posicao().distancia(C), 1e-9);
        assertEquals(0, c.velocidade().x(), 1e-9, "a parte que ia para fora some");
        assertEquals(.3, c.velocidade().z(), 1e-9, "a parte ao longo da borda fica");
    }

    @Test
    void quemEstaDentroEVoltandoParaOCentroNaoEMexido() {
        var v = new Vetor(-.4, 0, 0);
        var c = Barreira.corrigir(C, R, CHAO, true, new Vetor(5, 64, 0), v);
        assertEquals(new Vetor(5, 64, 0), c.posicao());
        assertEquals(v, c.velocidade());
        assertFalse(c.noChao());
    }

    @Test
    void oChaoSeguraELevantaQuemEstaDentro() {
        var c = Barreira.corrigir(C, R, CHAO, true, new Vetor(3, 61, 2), new Vetor(.1, -.8, 0));
        assertEquals(CHAO, c.posicao().y(), 1e-9);
        assertEquals(0, c.velocidade().y(), 1e-9);
        assertEquals(.1, c.velocidade().x(), 1e-9);
        assertTrue(c.noChao());
        var subindo = Barreira.corrigir(C, R, CHAO, true, new Vetor(3, CHAO, 2), new Vetor(0, .42, 0));
        assertEquals(.42, subindo.velocidade().y(), 1e-9, "pular continua valendo");
    }

    @Test
    void quemEstaForaNaoEntra() {
        var c = Barreira.corrigir(C, R, CHAO, false, new Vetor(0, 64, 12), new Vetor(0, 0, -.6));
        assertEquals(R + Barreira.MARGEM, c.posicao().distancia(C), 1e-9);
        assertEquals(0, c.velocidade().z(), 1e-9);
        assertFalse(c.noChao(), "o chão é só para quem está dentro");
    }

    @Test
    void quemEstaForaELongeNaoEMexido() {
        var p = new Vetor(0, 40, 30);
        var c = Barreira.corrigir(C, R, CHAO, false, p, new Vetor(0, -.08, 0));
        assertEquals(p, c.posicao());
    }

    @Test
    void dentroNoComecoDecideOLado() {
        assertTrue(Barreira.dentro(C, R, new Vetor(11, 64, 0)));
        assertFalse(Barreira.dentro(C, R, new Vetor(12.5, 64, 0)));
    }

    @Test
    void subidaDoRoxo200SobeSuaveAte20EPara() {
        assertEquals(0, Barreira.subidaRoxo200(0), 1e-9);
        assertTrue(Barreira.subidaRoxo200(30) > 5 && Barreira.subidaRoxo200(30) < 15);
        assertEquals(20, Barreira.subidaRoxo200(60), 1e-9);
        assertEquals(20, Barreira.subidaRoxo200(80), 1e-9);
        for (int t = 1; t <= 60; t++) assertTrue(Barreira.subidaRoxo200(t) >= Barreira.subidaRoxo200(t - 1));
    }
}
