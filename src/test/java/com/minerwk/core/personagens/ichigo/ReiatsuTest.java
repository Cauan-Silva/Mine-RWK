package com.minerwk.core.personagens.ichigo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Forma;
import org.junit.jupiter.api.Test;

class ReiatsuTest {
    private static final EstadoDoJogador NADA = new EstadoDoJogador(false, false, false);
    private static final EstadoDoJogador APANHOU = new EstadoDoJogador(false, true, false);

    private static Reiatsu com(double valor) {
        Reiatsu r = new Reiatsu();
        r.carregar(valor);
        return r;
    }

    @Test
    void comecaCheioEm200() {
        Reiatsu r = new Reiatsu();
        assertEquals(200, r.maximo());
        assertEquals(200, r.atual());
        assertEquals("Reiatsu", r.nome());
    }

    @Test
    void noShikaiVoltaTresPorSegundoParaQuemNaoApanha() {
        Reiatsu r = com(100);
        for (int i = 0; i < 20; i++) r.tick(NADA);
        assertEquals(103, r.atual(), 1e-6);
    }

    @Test
    void apanharCortaOGanhoPelaMetadePorQuatroSegundos() {
        Reiatsu r = com(100);
        r.tick(APANHOU);
        for (int i = 1; i < 20; i++) r.tick(NADA);
        assertEquals(101.5, r.atual(), 1e-6, "1,5 por segundo logo depois de apanhar");
        // Completa os 4 segundos sem apanhar: o ganho dobra.
        for (int i = 20; i <= Reiatsu.CALMA; i++) r.tick(NADA);
        double antes = r.atual();
        for (int i = 0; i < 20; i++) r.tick(NADA);
        assertEquals(3, r.atual() - antes, 1e-6);
    }

    @Test
    void naoPassaDoMaximo() {
        Reiatsu r = com(199.99);
        for (int i = 0; i < 40; i++) r.tick(NADA);
        assertEquals(200, r.atual());
    }

    @Test
    void noBankaiNaoVoltaEDrenaDoisPorSegundo() {
        Reiatsu r = com(100);
        r.forma(Reiatsu.BANKAI);
        for (int i = 0; i < 20; i++) r.tick(NADA);
        assertEquals(98, r.atual(), 1e-6);
        assertFalse(r.esgotado());
    }

    @Test
    void bankaiCheioDuraCemSegundosEEsgota() {
        Reiatsu r = new Reiatsu();
        r.forma(Reiatsu.BANKAI);
        for (int i = 0; i < 100 * 20 - 1; i++) r.tick(NADA);
        assertFalse(r.esgotado(), "um tick antes ainda tem reiatsu");
        r.tick(NADA);
        r.tick(NADA);
        assertEquals(0, r.atual());
        assertTrue(r.esgotado());
    }

    @Test
    void semReiatsuNoShikaiNaoContaComoEsgotado() {
        Reiatsu r = com(0);
        assertFalse(r.esgotado(), "esgotar é só do Bankai");
        r.forma(Reiatsu.BANKAI);
        assertTrue(r.esgotado());
        r.forma(Forma.BASE);
        assertFalse(r.esgotado());
        r.tick(NADA);
        assertTrue(r.atual() > 0, "de volta ao Shikai o reiatsu volta a subir");
    }

    @Test
    void barraMudaDeCorNoBankai() {
        Reiatsu r = new Reiatsu();
        int shikai = r.cor();
        r.forma(Reiatsu.BANKAI);
        assertNotEquals(shikai, r.cor());
        r.forma(Forma.BASE);
        assertEquals(shikai, r.cor());
    }

    @Test
    void gastarECarregarRespeitamOsLimites() {
        Reiatsu r = com(30);
        assertTrue(r.pode(30));
        assertFalse(r.pode(30.1));
        r.gastar(30);
        assertEquals(0, r.atual());
        r.carregar(999);
        assertEquals(200, r.atual());
        r.carregar(Double.NaN);
        assertEquals(0, r.atual());
    }

    // --- Parte 6: a máscara pausa o dreno e o Mugetsu sela o reiatsu ---

    @Test
    void sinalSemDrenoPausaODrenoDoBankaiPorUmTempo() {
        Reiatsu r = com(100);
        r.forma(Reiatsu.BANKAI);
        r.sinal(Reiatsu.SEM_DRENO, 40);
        for (int i = 0; i < 40; i++) r.tick(NADA);
        assertEquals(100, r.atual(), 1e-9);
        for (int i = 0; i < 20; i++) r.tick(NADA);
        assertEquals(98, r.atual(), 1e-6, "acabou a pausa: 2 por segundo de novo");
    }

    @Test
    void sinalSemDrenoComZeroAcabaAPausaNaHora() {
        Reiatsu r = com(100);
        r.forma(Reiatsu.BANKAI);
        r.sinal(Reiatsu.SEM_DRENO, 400);
        r.sinal(Reiatsu.SEM_DRENO, 0);
        for (int i = 0; i < 20; i++) r.tick(NADA);
        assertEquals(98, r.atual(), 1e-6);
    }

    @Test
    void sairDoBankaiAcabaComAPausaDoDreno() {
        Reiatsu r = com(100);
        r.forma(Reiatsu.BANKAI);
        r.sinal(Reiatsu.SEM_DRENO, 400);
        r.forma("base");
        r.forma(Reiatsu.BANKAI);
        for (int i = 0; i < 20; i++) r.tick(NADA);
        assertEquals(98, r.atual(), 1e-6);
    }

    @Test
    void seladoFicaEmZeroSemRegenerarAteOSeloAcabar() {
        Reiatsu r = com(150);
        r.sinal(Reiatsu.SELAR, 100);
        assertEquals(0, r.atual(), 1e-9);
        assertFalse(r.pode(1));
        for (int i = 0; i < 100; i++) r.tick(NADA);
        assertEquals(0, r.atual(), 1e-9);
        for (int i = 0; i < 20; i++) r.tick(NADA);
        assertEquals(3, r.atual(), 1e-6, "selo aberto: volta a regenerar");
    }

    @Test
    void oSeloSobreviveASairEEntrarNoJogo() {
        Reiatsu r = com(150);
        r.sinal(Reiatsu.SELAR, 100);
        for (int i = 0; i < 30; i++) r.tick(NADA);
        double salvo = r.salvar();
        Reiatsu outro = new Reiatsu();
        outro.carregar(salvo);
        assertEquals(0, outro.atual(), 1e-9);
        for (int i = 0; i < 70; i++) outro.tick(NADA);
        assertEquals(0, outro.atual(), 1e-9, "faltavam 70 ticks de selo");
        outro.tick(NADA);
        assertTrue(outro.atual() > 0);
    }

    @Test
    void valorSalvoAbsurdoNaoSelaParaSempre() {
        Reiatsu r = new Reiatsu();
        r.carregar(-1e12);
        for (int i = 0; i < Reiatsu.SELO_MAXIMO + 1; i++) r.tick(NADA);
        assertTrue(r.atual() > 0, "o selo salvo nunca passa do máximo");
        r.carregar(Double.NaN);
        assertEquals(0, r.atual(), 1e-9);
    }

    @Test
    void sinalDesconhecidoNaoFazNada() {
        Reiatsu r = com(100);
        r.sinal("outro", 50);
        assertEquals(100, r.atual(), 1e-9);
    }
}
