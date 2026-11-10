package com.minerwk.core.personagens.gojo;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.kit.Vetor;
import org.junit.jupiter.api.Test;

class CutsceneRoxoMaximoTest {

    @Test
    void idLevaOAvancoEVolta() {
        for (int a : new int[] {0, 10, 20}) {
            String id = CutsceneRoxoMaximo.id(a);
            assertTrue(CutsceneRoxoMaximo.eh(id));
            assertEquals(CutsceneRoxoMaximo.coreografia(a).duracao(), CutsceneRoxoMaximo.deId(id).orElseThrow().duracao());
        }
        String sem = CutsceneRoxoMaximo.idSemAlvo(42.4);
        assertEquals(CutsceneRoxoMaximo.semAlvo(42).duracao(), CutsceneRoxoMaximo.deId(sem).orElseThrow().duracao());
        assertTrue(CutsceneRoxoMaximo.deId("gojo_roxo_200").isEmpty());
        assertTrue(CutsceneRoxoMaximo.deId(CutsceneRoxoMaximo.PREFIXO + "lixo").isEmpty());
    }

    @Test
    void comAlvoOsPlanosSeguemAsFasesEAExplosaoEVistaDeLonge() {
        for (int a : new int[] {0, 10, 20}) {
            Coreografia c = CutsceneRoxoMaximo.coreografia(a);
            int e = RoxoMaximo.inicioExplosao(a);
            assertEquals(e + CutsceneRoxoMaximo.PLANO_ABERTO + CutsceneRoxoMaximo.VOLTA, c.duracao());
            assertPlanoAberto(c, e, CutsceneRoxoMaximo.CENTRO_COM_ALVO);
            // Na fusão, close no rosto olhando para o núcleo atrás.
            Coreografia.Quadro fusao = c.quadro(RoxoMaximo.inicioFusao(a) + 5);
            assertTrue(fusao.posicao().comprimento() < 3, "close no Gojo");
            assertTrue(fusao.posicao().z() > 0 && fusao.posicao().x() < 0, "três quartos pela esquerda, vendo o rosto");
            assertTrue(fusao.alvo().z() < 0, "olha para trás dele");
            assertVolta(c);
        }
    }

    @Test
    void semAlvoSegueAsEsferasEMostraAExplosaoNaMiraDeLonge() {
        Coreografia c = CutsceneRoxoMaximo.semAlvo(42);
        int e = RoxoMaximo.TICKS_ESFERAS_SEM_ALVO + RoxoMaximo.TICKS_FUSAO;
        assertEquals(e + CutsceneRoxoMaximo.PLANO_ABERTO + CutsceneRoxoMaximo.VOLTA, c.duracao());
        assertEquals(new Vetor(0, 0, 42), c.quadro(RoxoMaximo.TICKS_ESFERAS_SEM_ALVO).alvo(), "olha para onde as esferas vão");
        assertPlanoAberto(c, e, new Vetor(0, 0, 42));
        assertVolta(c);
    }

    @Test
    void planoAbertoMostraAExplosaoInteiraEUmPoucoDoQueSobra() {
        assertTrue(CutsceneRoxoMaximo.PLANO_ABERTO >= RoxoMaximo.TICKS_EXPLOSAO + 20);
    }

    @Test
    void corridaBaixaAtrasECorteSecoDeLadoNoUppercut() {
        for (int a : new int[] {0, 10, 20}) {
            Coreografia c = CutsceneRoxoMaximo.coreografia(a);
            if (a > 0) {
                for (double t = 0; t < a; t += .5) {
                    Coreografia.Quadro q = c.quadro(t);
                    assertTrue(q.posicao().z() < 0, "atrás do Gojo correndo");
                    assertTrue(q.posicao().y() < -.9, "câmera baixa, perto do chão: " + q.posicao().y());
                }
            }
            Coreografia.Quadro soco = c.quadro(a + 1);
            assertTrue(Math.abs(soco.posicao().x()) > 3, "de lado no soco");
            assertTrue(soco.posicao().y() < -1, "de baixo");
            if (a > 0) {
                assertTrue(c.quadro(a + .5).posicao().distancia(soco.posicao()) > 3, "corte seco, não deslize");
            }
        }
    }

    @Test
    void depoisDoUppercutACameraNuncaEntraNoAlvo() {
        for (int a : new int[] {0, 10, 20}) {
            Coreografia c = CutsceneRoxoMaximo.coreografia(a);
            for (double t = a; t <= RoxoMaximo.inicioExplosao(a); t += .25) {
                Vetor p = c.quadro(t).posicao();
                assertTrue(distanciaDoAlvo(p) >= 2, "tick " + t + ": a " + distanciaDoAlvo(p) + " do alvo");
            }
        }
    }

    /** O alvo fica suspenso na frente do Gojo: uma caixa em volta de (0, 0, 1,5) na base dos olhos dele. */
    private static double distanciaDoAlvo(Vetor p) {
        double dx = Math.max(0, Math.abs(p.x()) - .5);
        double dy = Math.max(0, Math.max(-1.5 - p.y(), p.y() - 1.5));
        double dz = Math.max(0, Math.max(1 - p.z(), p.z() - 2));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Do começo da explosão até o fim do plano aberto: a pelo menos 60 do centro, parado e olhando para ele. */
    private static void assertPlanoAberto(Coreografia c, int e, Vetor centro) {
        Coreografia.Quadro antes = c.quadro(e - .5);
        assertTrue(antes.posicao().comprimento() < 5, "antes da explosão a câmera está perto do Gojo");
        Coreografia.Quadro q = c.quadro(e + 1);
        for (double t = e + 1; t <= e + CutsceneRoxoMaximo.PLANO_ABERTO; t += .5) {
            Coreografia.Quadro k = c.quadro(t);
            assertTrue(k.posicao().distancia(centro) >= 60, "longe: " + k.posicao().distancia(centro));
            assertEquals(q.posicao(), k.posicao(), "parada");
            assertEquals(centro, k.alvo(), "olhando a explosão");
        }
    }

    private static void assertVolta(Coreografia c) {
        assertEquals(Vetor.ZERO, c.quadro(c.duracao()).posicao(), "termina nos olhos");
        assertEquals(1, c.quadro(c.duracao()).fov(), 1e-9);
    }
}
