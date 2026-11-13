package com.minerwk.core.personagens.cid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.kit.Vetor;
import org.junit.jupiter.api.Test;

class SistemaSolarTest {
    private static void assertVetor(Vetor esperado, Vetor real, double margem) {
        assertTrue(esperado.distancia(real) < margem, "esperado " + esperado + ", veio " + real);
    }

    private static double grausAte(Vetor a, Vetor b) {
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, a.ponto(b)))));
    }

    @Test
    void aTerraComecaComoOChaoDeHoje() {
        assertVetor(new Vetor(0, -1, 0), SistemaSolar.direcao(SistemaSolar.TERRA, 0), 1e-6);
        assertEquals(Math.toRadians(62), SistemaSolar.raioAparente(SistemaSolar.TERRA, 0), Math.toRadians(.5));
        assertEquals(SistemaSolar.raioAparente(SistemaSolar.TERRA, 0),
                SistemaSolar.raioAparente(SistemaSolar.TERRA, SistemaSolar.INICIO), 1e-9);
    }

    @Test
    void aTerraViraUmaBolaInteira() {
        double r = Math.toDegrees(SistemaSolar.raioAparente(SistemaSolar.TERRA, SistemaSolar.TERRA_INTEIRA));
        assertTrue(r > 10 && r < 20, "a Terra inteira na tela: " + r);
    }

    @Test
    void noFimTodosOsAstrosCabemNoQuadro() {
        for (SistemaSolar.Astro a : SistemaSolar.ASTROS) {
            double ang = grausAte(SistemaSolar.direcao(a, SistemaSolar.SISTEMA), SistemaSolar.OLHAR);
            assertTrue(ang < 30, a.nome() + " fora do quadro: " + ang);
        }
    }

    @Test
    void noFimATerraFicaNoMeioDoQuadro() {
        assertTrue(grausAte(SistemaSolar.direcao(SistemaSolar.TERRA, SistemaSolar.SISTEMA), SistemaSolar.OLHAR) < 1);
    }

    @Test
    void aTerraNoFimEhUmPontoQueAindaSeVe() {
        assertEquals(SistemaSolar.MINIMO, SistemaSolar.raioAparente(SistemaSolar.TERRA, SistemaSolar.FIM_DO_PULSO), 1e-9);
    }

    @Test
    void oSolEhOMaiorNoFim() {
        double sol = SistemaSolar.raioAparente(SistemaSolar.SOL, SistemaSolar.SISTEMA);
        for (SistemaSolar.Astro a : SistemaSolar.ASTROS) {
            if (a != SistemaSolar.SOL) assertTrue(SistemaSolar.raioAparente(a, SistemaSolar.SISTEMA) < sol, a.nome());
        }
    }

    @Test
    void osPlanetasGiramSemSairDaOrbita() {
        SistemaSolar.Astro marte = SistemaSolar.ASTROS.stream().filter(a -> a.nome().equals("marte")).findFirst().orElseThrow();
        Vetor antes = SistemaSolar.posicao(marte, 0), depois = SistemaSolar.posicao(marte, 60);
        assertTrue(antes.distancia(depois) > .5, "Marte andou");
        Vetor sol0 = SistemaSolar.posicao(SistemaSolar.SOL, 0), sol60 = SistemaSolar.posicao(SistemaSolar.SOL, 60);
        assertEquals(antes.distancia(sol0), depois.distancia(sol60), 1e-6);
        assertEquals(marte.orbita(), antes.distancia(sol0), 1e-6);
    }

    @Test
    void aTerraFicaParadaNoCentroDaCena() {
        assertVetor(Vetor.ZERO, SistemaSolar.posicao(SistemaSolar.TERRA, 0), 1e-9);
        assertVetor(Vetor.ZERO, SistemaSolar.posicao(SistemaSolar.TERRA, 90), 1e-9);
    }

    @Test
    void aEsferaDoMundoPassaParaOCeu() {
        assertEquals(1, SistemaSolar.esferaNoMundo(SistemaSolar.INICIO), 1e-9);
        assertEquals(0, SistemaSolar.esferaNoMundo(SistemaSolar.TERRA_INTEIRA), 1e-9);
        assertEquals(0, SistemaSolar.pontoRoxo(SistemaSolar.INICIO), 1e-9);
        assertTrue(SistemaSolar.pontoRoxo(SistemaSolar.TERRA_INTEIRA) >= .75);
    }

    @Test
    void osAstrosDeHojeSomemNoComecoDoRecuo() {
        assertEquals(1, SistemaSolar.astrosAntigos(SistemaSolar.INICIO), 1e-9);
        assertEquals(0, SistemaSolar.astrosAntigos(SistemaSolar.TERRA_INTEIRA), 1e-9);
    }

    @Test
    void oPulsoFinalCobreATela() {
        assertTrue(SistemaSolar.raioDoPonto(100) < SistemaSolar.raioDoPonto(110));
        assertTrue(SistemaSolar.raioDoPonto(SistemaSolar.FIM_DO_PULSO) >= Math.toRadians(90));
    }

    @Test
    void oEstouroDura130EALuzAindaBrilhaNaVolta() {
        assertEquals(130, AtomicFinal.TICKS_DO_CLARAO);
        assertEquals(130, AtomicFinal.VOLTA - AtomicFinal.ATOMIC);
        assertTrue(AtomicFinal.luz(AtomicFinal.TICKS_DO_CLARAO + 20) > .5, "quem está no chão ainda vê a luz depois da volta");
        assertTrue(AtomicFinal.branco(SistemaSolar.FIM_DO_PULSO + 4) > .5, "o pulso final emenda no branco");
    }

    @Test
    void aEsferaDoMundoCresceNoRitmoDeAntes() {
        assertEquals(1, AtomicFinal.raioDoClarao(AtomicFinal.TICKS_DA_ESFERA), 1e-9);
        assertEquals(1, AtomicFinal.raioDoClarao(AtomicFinal.TICKS_DO_CLARAO), 1e-9);
    }
}
