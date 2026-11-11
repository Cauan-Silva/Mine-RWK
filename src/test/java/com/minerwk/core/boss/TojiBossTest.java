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
    void golpesNovosCabemNasAnimacoes() {
        // "giro" dura 2 s, "sumir" 2,1 s e "execucao" 4,5 s.
        assertTrue(TojiBoss.NUVEM.duracao() <= 40);
        assertEquals(40, TojiBoss.NUVEM_5.duracao());
        assertEquals(42, TojiBoss.DESAPARECER.duracao());
        assertEquals(90, TojiBoss.EXECUCAO.duracao());
        assertEquals(TojiBoss.INVESTIDAS_DA_EXECUCAO * TojiBoss.PASSO_DA_EXECUCAO, TojiBoss.EXECUCAO.janela());
        assertTrue(TojiBoss.TICK_DE_SURGIR < TojiBoss.DESAPARECER.aviso(), "surge antes de golpear, para a sombra avisar");
    }

    @Test
    void cadaFaseTemOsSeusGolpes() {
        assertFalse(TojiBoss.golpes(1).contains(TojiBoss.DESAPARECER), "a fase 1 é contra quem só tem Azul e Infinito");
        assertTrue(TojiBoss.golpes(1).contains(TojiBoss.NUVEM));
        assertTrue(TojiBoss.golpes(2).contains(TojiBoss.DESAPARECER));
        assertTrue(TojiBoss.golpes(2).contains(TojiBoss.NUVEM_5));
        assertFalse(TojiBoss.golpes(2).contains(TojiBoss.EXECUCAO));
        assertTrue(TojiBoss.golpes(3).contains(TojiBoss.EXECUCAO));
    }

    @Test
    void faseDependeDoDespertarEDaVida() {
        assertEquals(1, TojiBoss.fase(false, 1));
        assertEquals(1, TojiBoss.fase(false, .1), "sem despertar a luta não passa da fase 1");
        assertEquals(2, TojiBoss.fase(true, .36));
        assertEquals(3, TojiBoss.fase(true, .35));
        assertTrue(TojiBoss.ritmo(3) < TojiBoss.ritmo(2) && TojiBoss.ritmo(2) < TojiBoss.ritmo(1));
    }

    @Test
    void nuvemBateTresOuCincoVezesNaJanela() {
        int tres = 0, cinco = 0;
        for (int t = 0; t < 20; t++) {
            if (TojiBoss.nuvemBate(TojiBoss.NUVEM, t)) tres++;
            if (TojiBoss.nuvemBate(TojiBoss.NUVEM_5, t)) cinco++;
        }
        assertEquals(3, tres);
        assertEquals(5, cinco);
        assertTrue(TojiBoss.nuvemBate(TojiBoss.NUVEM, 0), "o primeiro golpe é quando a janela abre");
        assertFalse(TojiBoss.nuvemBate(TojiBoss.NUVEM, 10), "fora da janela não bate");
    }

    @Test
    void setorDaNuvemPegaAFrenteENaoAsCostas() {
        // Olhando para +Z, setor de 160 graus e raio 3,5.
        assertTrue(TojiBoss.noSetor(0, 3, 0, 1, 3.5, 160));
        assertTrue(TojiBoss.noSetor(2.9, .6, 0, 1, 3.5, 160), "quase de lado ainda pega (78 graus)");
        assertFalse(TojiBoss.noSetor(3, -.6, 0, 1, 3.5, 160), "um pouco para trás não");
        assertFalse(TojiBoss.noSetor(0, -2, 0, 1, 3.5, 160));
        assertFalse(TojiBoss.noSetor(0, 3.6, 0, 1, 3.5, 160), "fora do raio");
        assertTrue(TojiBoss.noSetor(0, 0, 0, 1, 3.5, 160), "em cima dele");
    }

    @Test
    void execucaoSaoQuatroInvestidasComAvisoEAvanco() {
        assertEquals(0, TojiBoss.investidaDaExecucao(0));
        assertEquals(3, TojiBoss.investidaDaExecucao(55));
        assertEquals(-1, TojiBoss.investidaDaExecucao(56));
        assertFalse(TojiBoss.avancaNaExecucao(9), "os 10 primeiros ticks de cada investida são aviso");
        assertTrue(TojiBoss.avancaNaExecucao(10));
        assertTrue(TojiBoss.avancaNaExecucao(12));
        assertFalse(TojiBoss.avancaNaExecucao(13));
        assertTrue(TojiBoss.avancaNaExecucao(14 + 11));
        java.util.Set<Double> angulos = new java.util.HashSet<>();
        for (int i = 0; i < TojiBoss.INVESTIDAS_DA_EXECUCAO; i++) angulos.add(TojiBoss.anguloDaExecucao(i));
        assertEquals(4, angulos.size(), "cada investida vem de um ângulo");
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
    void lutaAcabaSemOInvocadorOuAosDezMinutos() {
        // A distância não conta mais: a barreira da arena segura os dois.
        assertEquals(Fim.CONTINUA, TojiBoss.fim(true, 100));
        assertEquals(Fim.CONTINUA, TojiBoss.fim(true, 11999));
        assertEquals(Fim.INVOCADOR_FORA, TojiBoss.fim(false, 0));
        assertEquals(Fim.TEMPO, TojiBoss.fim(true, 12000));
    }
}
