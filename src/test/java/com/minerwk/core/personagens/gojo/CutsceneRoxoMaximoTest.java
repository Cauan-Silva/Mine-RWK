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
            assertTrue(fusao.posicao().comprimento() < 2.5, "close no Gojo");
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
