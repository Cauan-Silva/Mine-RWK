package com.minerwk.core.vfx;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** As formas do Mugetsu: o véu que avança, o céu que escurece e a fumaça da transformação. */
class MugetsuIchigoTest {

    @Test
    void oVeuAvancaCentoECinquentaBlocosEmUmSegundo() {
        assertEquals(0, MugetsuIchigo.frente(0), 1e-9);
        assertEquals(75, MugetsuIchigo.frente(10), 1e-9);
        assertEquals(3.75, MugetsuIchigo.frente(.5), 1e-9, "anda liso entre os ticks");
        assertEquals(150, MugetsuIchigo.frente(20), 1e-9);
        assertEquals(150, MugetsuIchigo.frente(35), 1e-9, "para no alcance");
        assertEquals(20, MugetsuIchigo.TICKS, "o som do Mugetsu foi feito para esse tempo");
        assertEquals(0, MugetsuIchigo.frente(-3), 1e-9);
        assertEquals(MugetsuIchigo.ALCANCE / MugetsuIchigo.PASSO, MugetsuIchigo.TICKS);
        assertEquals(3, 2 * MugetsuIchigo.MEIA_LARGURA, "fino: 3 blocos de largura");
        assertEquals(40, MugetsuIchigo.ALTURA, "e 40 de altura");
    }

    @Test
    void oVeuSobeDoChaoAtrasDaFrenteENaoExisteAdianteDela() {
        double t = 10; // a frente está a 75 blocos
        assertEquals(0, MugetsuIchigo.altura(0, 75.5, t), 1e-9, "adiante da frente não há véu");
        assertEquals(0, MugetsuIchigo.altura(0, 100, t), 1e-9);
        assertTrue(MugetsuIchigo.altura(0, 73, t) < MugetsuIchigo.altura(0, 50, t), "logo atrás da frente ele ainda está subindo");
        assertTrue(MugetsuIchigo.altura(0, 20, t) > 25, "no meio, já alto: " + MugetsuIchigo.altura(0, 20, t));
        assertTrue(MugetsuIchigo.altura(0, 10, t) <= MugetsuIchigo.ALTURA * 1.01);
    }

    @Test
    void oVeuEMaisBaixoNasBordas() {
        double t = 20;
        double meio = 0, borda = 0;
        // A média ao longo do caminho: o topo é irregular, mas as bordas ficam mais baixas que o meio.
        for (int z = 2; z <= 30; z++) {
            meio += MugetsuIchigo.altura(0, z, t);
            borda += MugetsuIchigo.altura(1, z, t) + MugetsuIchigo.altura(-1, z, t);
        }
        assertTrue(borda / 2 < meio * .85, "bordas " + borda / 2 + " x meio " + meio);
        assertTrue(borda > 0, "mas ainda há véu na borda");
    }

    @Test
    void oVeuFicaCheioEnquantoAvancaEDepoisSeDesfaz() {
        assertEquals(1, MugetsuIchigo.opacidade(0), 1e-9);
        assertEquals(1, MugetsuIchigo.opacidade(MugetsuIchigo.TICKS), 1e-9);
        double meio = MugetsuIchigo.opacidade(MugetsuIchigo.TICKS + MugetsuIchigo.RESTO / 2.0);
        assertTrue(meio > .2 && meio < .8, "no meio do resto: " + meio);
        assertEquals(0, MugetsuIchigo.opacidade(MugetsuIchigo.TICKS + MugetsuIchigo.RESTO), 1e-9);
        assertEquals(0, MugetsuIchigo.opacidade(500), 1e-9);
    }

    @Test
    void oCeuEscureceNoCorteEVoltaComOResto() {
        assertEquals(0, MugetsuIchigo.ceu(0), 1e-9);
        assertEquals(1, MugetsuIchigo.ceu(MugetsuIchigo.CEU_ESCURECE), 1e-9, "escuro em poucos ticks");
        assertEquals(1, MugetsuIchigo.ceu(MugetsuIchigo.TICKS), 1e-9);
        assertTrue(MugetsuIchigo.ceu(MugetsuIchigo.TICKS + 10) < 1);
        assertEquals(0, MugetsuIchigo.ceu(MugetsuIchigo.TICKS + MugetsuIchigo.RESTO), 1e-9);
    }

    @Test
    void aFumacaIncheNaTransformacaoEMurchaAteOSilencio() {
        int transforma = 30, silencio = 84, corte = 120;
        assertEquals(0, MugetsuIchigo.fumaca(0, transforma, silencio), 1e-9);
        assertTrue(MugetsuIchigo.fumaca(15, transforma, silencio) > 0);
        assertTrue(MugetsuIchigo.fumaca(15, transforma, silencio) < MugetsuIchigo.fumaca(29, transforma, silencio));
        assertEquals(1, MugetsuIchigo.fumaca(transforma, transforma, silencio), 1e-9, "cheia quando a forma final aparece");
        assertTrue(MugetsuIchigo.fumaca(60, transforma, silencio) < 1);
        assertEquals(MugetsuIchigo.FUMACA_NO_SILENCIO, MugetsuIchigo.fumaca(silencio, transforma, silencio), 1e-9);
        assertEquals(MugetsuIchigo.FUMACA_NO_SILENCIO, MugetsuIchigo.fumaca(corte, transforma, silencio), 1e-9,
                "no silêncio sobra só um fio de reiatsu preto, parado");
    }

    @Test
    void oCeuJaEscureceUmPoucoDuranteACarga() {
        int transforma = 30;
        assertEquals(0, MugetsuIchigo.ceuDaCarga(0, transforma), 1e-9);
        assertEquals(MugetsuIchigo.CEU_DA_CARGA, MugetsuIchigo.ceuDaCarga(transforma, transforma), 1e-9);
        assertEquals(MugetsuIchigo.CEU_DA_CARGA, MugetsuIchigo.ceuDaCarga(100, transforma), 1e-9);
        assertTrue(MugetsuIchigo.CEU_DA_CARGA < 1, "o escuro cheio fica para o corte");
    }
}
