package com.minerwk.core.arena;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Barreira;
import com.minerwk.core.kit.Vetor;
import org.junit.jupiter.api.Test;

class CaixaArenaTest {
    private static final CaixaArena VAGA0 = CaixaArena.daVaga(0);

    @Test
    void centroDeCadaVaga() {
        assertEquals(256, VAGA0.centroX());
        assertEquals(256, VAGA0.centroZ());
        assertEquals(768, CaixaArena.daVaga(1).centroX());
        assertEquals(256, CaixaArena.daVaga(1).centroZ());
        assertEquals(256, CaixaArena.daVaga(64).centroX());
        assertEquals(768, CaixaArena.daVaga(64).centroZ());
    }

    @Test
    void vagaPelaPosicao() {
        assertEquals(VAGA0, CaixaArena.daPosicao(256, 256));
        assertEquals(VAGA0, CaixaArena.daPosicao(511.9, 0));
        assertEquals(new CaixaArena(1, 0), CaixaArena.daPosicao(512, 0));
    }

    @Test
    void posicaoNegativaNaoQuebra() {
        assertEquals(new CaixaArena(-1, -1), CaixaArena.daPosicao(-0.1, -0.1));
    }

    @Test
    void blocosDaFaceContamComoDentro() {
        assertTrue(VAGA0.contemBloco(206, 44, 206));
        assertTrue(VAGA0.contemBloco(306, 119, 306));
        assertFalse(VAGA0.contemBloco(205, 64, 256));
        assertFalse(VAGA0.contemBloco(307, 64, 256));
        assertFalse(VAGA0.contemBloco(256, 43, 256));
        assertFalse(VAGA0.contemBloco(256, 120, 256));
    }

    @Test
    void tojiEntra16BlocosAoNorteDoJogador() {
        Vetor jogador = VAGA0.entradaJogador();
        Vetor toji = VAGA0.entradaToji();
        assertEquals(jogador.x(), toji.x(), 1e-9);
        assertEquals(16, jogador.z() - toji.z(), 1e-9);
        assertEquals(CaixaArena.Y_PATIO, jogador.y(), 1e-9);
        for (Vetor p : new Vetor[] {jogador, toji}) {
            assertTrue(Math.abs(p.x() - VAGA0.centro().x()) <= 20);
            assertTrue(Math.abs(p.z() - VAGA0.centro().z()) <= 20);
        }
    }

    @Test
    void paredeSeguraQuemVaiParaFora() {
        Vetor pes = new Vetor(VAGA0.centroX() + 50.9, 64, VAGA0.centroZ() + .5);
        Barreira.Correcao c = VAGA0.corrigir(pes, new Vetor(.4, 0, 0), .3, 1.8);
        assertEquals(VAGA0.centroX() + 51 - .3 - Barreira.MARGEM, c.posicao().x(), 1e-9);
        assertTrue(c.velocidade().x() <= 0);
        Vetor oeste = new Vetor(VAGA0.centroX() - 49.9, 64, VAGA0.centroZ() + .5);
        Barreira.Correcao o = VAGA0.corrigir(oeste, new Vetor(-.4, 0, 0), .3, 1.8);
        assertEquals(VAGA0.centroX() - 50 + .3 + Barreira.MARGEM, o.posicao().x(), 1e-9);
        assertTrue(o.velocidade().x() >= 0);
    }

    @Test
    void noCentroNadaMuda() {
        Vetor vel = new Vetor(.2, -.1, .3);
        Barreira.Correcao c = VAGA0.corrigir(VAGA0.centro(), vel, .3, 1.8);
        assertEquals(VAGA0.centro(), c.posicao());
        assertEquals(vel, c.velocidade());
    }

    @Test
    void tetoSeguraQuemSobe() {
        Vetor pes = new Vetor(VAGA0.centroX(), 119, VAGA0.centroZ());
        Barreira.Correcao c = VAGA0.corrigir(pes, new Vetor(0, .5, 0), .3, 1.8);
        assertEquals(120 - 1.8, c.posicao().y(), 1e-9);
        assertTrue(c.velocidade().y() <= 0);
    }

    @Test
    void fundoEAberto() {
        Vetor pes = new Vetor(VAGA0.centroX(), 40, VAGA0.centroZ());
        Barreira.Correcao c = VAGA0.corrigir(pes, new Vetor(0, -1, 0), .3, 1.8);
        assertEquals(40, c.posicao().y(), 1e-9);
        assertEquals(-1, c.velocidade().y(), 1e-9);
        assertTrue(VAGA0.abaixoDoFundo(43.9));
        assertFalse(VAGA0.abaixoDoFundo(44));
    }
}
