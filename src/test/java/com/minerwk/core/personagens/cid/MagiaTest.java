package com.minerwk.core.personagens.cid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.kit.EstadoDoJogador;
import org.junit.jupiter.api.Test;

/** A Magia do Cid: volta sempre, mais rápido para quem não apanha, e para no Overdrive. */
class MagiaTest {
    private static final EstadoDoJogador CALMO = new EstadoDoJogador(true, false, false);
    private static final EstadoDoJogador APANHANDO = new EstadoDoJogador(true, true, false);

    private static void ticks(Magia m, EstadoDoJogador e, int n) {
        for (int i = 0; i < n; i++) m.tick(e);
    }

    @Test
    void comecaCheiaEmCem() {
        Magia m = new Magia();
        assertEquals(100, m.maximo());
        assertEquals(100, m.atual());
        assertEquals("Magia", m.nome());
    }

    @Test
    void gastaENaoDeixaGastarOQueNaoTem() {
        Magia m = new Magia();
        m.gastar(30);
        assertEquals(70, m.atual(), 1e-9);
        assertFalse(m.pode(71));
        assertTrue(m.pode(70));
        assertThrows(IllegalStateException.class, () -> m.gastar(71));
    }

    @Test
    void apanhandoVoltaQuatroPorSegundo() {
        Magia m = new Magia();
        m.carregar(0);
        ticks(m, APANHANDO, 20);
        assertEquals(4, m.atual(), 1e-6);
    }

    @Test
    void depoisDeQuatroSegundosSemApanharVoltaOitoPorSegundo() {
        Magia m = new Magia();
        m.carregar(0);
        m.tick(APANHANDO);
        double antes = m.atual();
        ticks(m, CALMO, Magia.CALMA);
        assertEquals(antes + 4.0 * Magia.CALMA / 20, m.atual(), 0.5, "os 4 s de espera ainda são no ganho normal");
        double calmo = m.atual();
        ticks(m, CALMO, 20);
        assertEquals(calmo + 8, m.atual(), 1e-6);
    }

    @Test
    void naoPassaDoMaximo() {
        Magia m = new Magia();
        ticks(m, CALMO, 100);
        assertEquals(100, m.atual());
    }

    @Test
    void noOverdriveNaoVoltaEDepoisVoltaSozinha() {
        Magia m = new Magia();
        m.carregar(10);
        m.sinal(Magia.SEM_GANHO, 40);
        ticks(m, CALMO, 40);
        assertEquals(10, m.atual(), 1e-9);
        ticks(m, CALMO, 20);
        assertEquals(18, m.atual(), 1e-6);
    }

    @Test
    void sinalZeroAcabaAPausaESinalDesconhecidoNaoFazNada() {
        Magia m = new Magia();
        m.carregar(10);
        m.sinal(Magia.SEM_GANHO, 40);
        m.sinal(Magia.SEM_GANHO, 0);
        m.sinal("outro", 99);
        ticks(m, CALMO, 20);
        assertEquals(18, m.atual(), 1e-6);
    }

    @Test
    void salvaECarregaComLimites() {
        Magia m = new Magia();
        m.gastar(25);
        Magia outra = new Magia();
        outra.carregar(m.salvar());
        assertEquals(75, outra.atual(), 1e-9);
        outra.carregar(500);
        assertEquals(100, outra.atual());
        outra.carregar(-3);
        assertEquals(0, outra.atual());
        outra.carregar(Double.NaN);
        assertEquals(0, outra.atual());
    }
}
