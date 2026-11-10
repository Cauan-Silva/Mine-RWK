package com.minerwk.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.boss.TojiBoss.Fim;
import com.minerwk.core.boss.TojiBoss.Invocacao;
import org.junit.jupiter.api.Test;

class TojiBossTest {
    @Test
    void golpesTemOsAvisosDoSpecECabemNasAnimacoes() {
        assertEquals(18, TojiBoss.INVESTIDA.aviso());
        assertEquals(14, TojiBoss.LANCA.aviso());
        assertEquals(12, TojiBoss.CORRENTE.aviso());
        // As animações do modelo duram 1,7 s, 1,4 s e 1,7 s.
        assertEquals(34, TojiBoss.INVESTIDA.duracao());
        assertEquals(28, TojiBoss.LANCA.duracao());
        assertEquals(34, TojiBoss.CORRENTE.duracao());
    }

    @Test
    void sempreHaGolpeEntreZeroECatorzeBlocos() {
        for (double d = 0; d <= 14; d += .5) {
            double distancia = d;
            assertTrue(TojiBoss.GOLPES.stream().anyMatch(g -> g.alcanca(distancia)), "sem golpe a " + d);
        }
    }

    @Test
    void contratoExigeGojoNoDespertarENenhumTojiVivo() {
        assertEquals(Invocacao.OUTRO_PERSONAGEM, TojiBoss.podeInvocar("", "gojo", true, false));
        assertEquals(Invocacao.OUTRO_PERSONAGEM, TojiBoss.podeInvocar("teste", "gojo", true, false));
        assertEquals(Invocacao.SEM_NO, TojiBoss.podeInvocar("gojo", "gojo", false, false));
        assertEquals(Invocacao.JA_EXISTE, TojiBoss.podeInvocar("gojo", "gojo", true, true));
        assertEquals(Invocacao.OK, TojiBoss.podeInvocar("gojo", "gojo", true, false));
    }

    @Test
    void despertarDisparaComVintePorCentoDeVidaPerdidaOuAosQuarentaECincoSegundos() {
        assertFalse(TojiBoss.horaDeDespertar(1, 0));
        assertFalse(TojiBoss.horaDeDespertar(.81, 899));
        assertTrue(TojiBoss.horaDeDespertar(.8, 0));
        assertTrue(TojiBoss.horaDeDespertar(1, 900));
    }

    @Test
    void golpeMortalEOQueZeraAVida() {
        assertFalse(TojiBoss.golpeMortal(10.5f, 10));
        assertTrue(TojiBoss.golpeMortal(10, 10));
        assertTrue(TojiBoss.golpeMortal(4, 8));
    }

    @Test
    void lutaAcabaSemOInvocadorLongeDemaisOuAosDezMinutos() {
        assertEquals(Fim.CONTINUA, TojiBoss.fim(true, 64, 11999));
        assertEquals(Fim.INVOCADOR_FORA, TojiBoss.fim(false, 2, 0));
        assertEquals(Fim.LONGE, TojiBoss.fim(true, 64.5, 0));
        assertEquals(Fim.TEMPO, TojiBoss.fim(true, 2, 12000));
    }
}
