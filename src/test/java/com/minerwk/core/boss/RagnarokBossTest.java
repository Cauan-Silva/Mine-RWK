package com.minerwk.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagnarokBossTest {
    @Test
    void aFaseDoisComecaNaMetadeDaVidaETrocaAInvestidaPeloPisao() {
        assertEquals(1, RagnarokBoss.fase(.51));
        assertEquals(2, RagnarokBoss.fase(.5));
        assertTrue(RagnarokBoss.golpes(1).contains(RagnarokBoss.INVESTIDA));
        assertFalse(RagnarokBoss.golpes(1).contains(RagnarokBoss.PISAO));
        assertFalse(RagnarokBoss.golpes(2).contains(RagnarokBoss.INVESTIDA), "sem a asa ele não voa");
        assertTrue(RagnarokBoss.golpes(2).contains(RagnarokBoss.PISAO));
        assertEquals(5, RagnarokBoss.golpes(1).size());
        assertEquals(5, RagnarokBoss.golpes(2).size());
        assertTrue(RagnarokBoss.ritmo(2) < RagnarokBoss.ritmo(1), "bate mais rápido");
    }

    @Test
    void oCalorCresceNaFaseDois() {
        double beirada = RagnarokBoss.LARGURA / 2;
        assertTrue(RagnarokBoss.noCalor(beirada + 2, 0, 1));
        assertFalse(RagnarokBoss.noCalor(beirada + 2.1, 0, 1));
        assertTrue(RagnarokBoss.noCalor(beirada + 3, 0, 2));
        assertFalse(RagnarokBoss.noCalor(beirada + 1, 12, 1), "bem acima da cabeça dele não queima");
        assertEquals(1f, RagnarokBoss.danoDoCalor(1));
        assertEquals(2f, RagnarokBoss.danoDoCalor(2));
    }

    @Test
    void oSocoCaiNoAlvoAteOndeOBracoAlcanca() {
        assertEquals(5, RagnarokBoss.pontoDoSoco(5));
        assertEquals(RagnarokBoss.ALCANCE_DO_BRACO, RagnarokBoss.pontoDoSoco(20));
        assertTrue(RagnarokBoss.pontoDoSoco(0) > RagnarokBoss.LARGURA / 2, "nunca dentro do próprio corpo");
    }

    @Test
    void aBarragemTemQuatroSocosCadaUmMiradoAntes() {
        List<Integer> socos = new ArrayList<>(), miras = new ArrayList<>();
        for (int t = 0; t < RagnarokBoss.BARRAGEM.janela(); t++) {
            if (RagnarokBoss.socoDaBarragem(t)) socos.add(t);
            if (RagnarokBoss.miraDaBarragem(t)) miras.add(t);
        }
        assertEquals(List.of(0, 10, 20, 30), socos);
        assertEquals(List.of(4, 14, 24), miras, "os três seguintes são mirados 6 ticks antes");
        assertTrue(RagnarokBoss.BARRAGEM.janela() > 30);
    }

    @Test
    void aRajadaAbreEmLeque() {
        assertEquals(-30, RagnarokBoss.desvioDaRajada(0), 1e-9);
        assertEquals(0, RagnarokBoss.desvioDaRajada(2), 1e-9);
        assertEquals(30, RagnarokBoss.desvioDaRajada(4), 1e-9);
        assertTrue(RagnarokBoss.PASSO_RAJADA * RagnarokBoss.RAJADA.janela() < RagnarokBoss.ALCANCE_RAJADA,
                "os projéteis vivem além do golpe");
    }

    @Test
    void aInvestidaSobeAtravessaEPousa() {
        Golpe g = RagnarokBoss.INVESTIDA;
        assertEquals(0, RagnarokBoss.alturaNaInvestida(0));
        assertEquals(RagnarokBoss.ALTURA_DO_VOO, RagnarokBoss.alturaNaInvestida(g.aviso() - 1), 1e-9, "no fim do aviso está no alto");
        assertEquals(0, RagnarokBoss.alturaNaInvestida(g.aviso() + g.janela() - 1), 1e-9, "passa rasante");
        assertEquals(0, RagnarokBoss.alturaNaInvestida(g.duracao()));
        assertEquals(1, RagnarokBoss.avancoDaInvestida(g.janela() - 1), 1e-9, "atravessa a linha inteira");
        for (int t = 1; t < g.janela(); t++) {
            assertTrue(RagnarokBoss.avancoDaInvestida(t) >= RagnarokBoss.avancoDaInvestida(t - 1));
        }
    }

    @Test
    void aInvestidaParaAntesDaBeiradaDaArena() {
        assertEquals(20, RagnarokBoss.ateABeirada(10, 0, 1, 0, 30), 1e-9);
        assertEquals(40, RagnarokBoss.ateABeirada(10, 0, -1, 0, 30), 1e-9);
        double d = Math.sqrt(.5);
        assertEquals(10 / d, RagnarokBoss.ateABeirada(0, 20, d, d, 30), 1e-9, "a beirada mais próxima manda");
        assertEquals(0, RagnarokBoss.ateABeirada(35, 0, 1, 0, 30), "já fora, não anda");
    }

    @Test
    void oSoproVarreDeUmLadoAoOutro() {
        int fim = RagnarokBoss.SOPRO.janela() - 1;
        assertEquals(-25, RagnarokBoss.desvioDoSopro(0, 1), 1e-9);
        assertEquals(25, RagnarokBoss.desvioDoSopro(fim, 1), 1e-9);
        assertEquals(25, RagnarokBoss.desvioDoSopro(0, -1), 1e-9, "para o outro lado");
        assertEquals(40, RagnarokBoss.SOPRO.aviso(), "2 s de carga na boca");
    }

    @Test
    void aOndaDoPisaoEPulavel() {
        int fim = RagnarokBoss.PISAO.janela() - 1;
        assertEquals(RagnarokBoss.RAIO_PISAO, RagnarokBoss.raioDaOnda(fim), 1e-9);
        double r = RagnarokBoss.raioDaOnda(5);
        assertTrue(RagnarokBoss.naOnda(5, r, 0));
        assertFalse(RagnarokBoss.naOnda(5, r, 1), "quem pulou passa por cima");
        assertFalse(RagnarokBoss.naOnda(5, r + 3, 0), "a onda é um aro, não um disco");
    }

    @Test
    void aLutaAcabaSemOInvocadorOuNoTempoLimite() {
        assertTrue(RagnarokBoss.acabou(false, 10));
        assertFalse(RagnarokBoss.acabou(true, 10));
        assertTrue(RagnarokBoss.acabou(true, RagnarokBoss.TEMPO_LIMITE));
        assertEquals(320, RagnarokBoss.VIDA);
        assertEquals(500, RagnarokBoss.XP_VITORIA);
    }
}
