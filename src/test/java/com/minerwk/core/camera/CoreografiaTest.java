package com.minerwk.core.camera;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoreografiaTest {
    private static final Coreografia DOIS = new Coreografia(List.of(
            new Coreografia.Quadro(0, new Vetor(0, 0, 0), new Vetor(0, 0, 1), 1, 0),
            new Coreografia.Quadro(10, new Vetor(10, 0, 0), new Vetor(10, 0, 1), 2, 10)));

    @Test
    void emCimaDoQuadroDevolveOQuadro() {
        assertEquals(new Vetor(0, 0, 0), DOIS.quadro(0).posicao());
        assertEquals(new Vetor(10, 0, 0), DOIS.quadro(10).posicao());
        assertEquals(2, DOIS.quadro(10).fov(), 1e-9);
    }

    @Test
    void entreQuadrosFicaNoMeioESuave() {
        assertEquals(5, DOIS.quadro(5).posicao().x(), 1e-9);
        double cedo = DOIS.quadro(1).posicao().x();
        assertTrue(cedo > 0 && cedo < 1, "começa devagar: " + cedo);
        for (double t = 0; t < 10; t += .5) {
            assertTrue(DOIS.quadro(t + .5).posicao().x() >= DOIS.quadro(t).posicao().x(), "sem voltar");
        }
    }

    @Test
    void foraDoIntervaloFicaNasPontas() {
        assertEquals(new Vetor(0, 0, 0), DOIS.quadro(-5).posicao());
        assertEquals(new Vetor(10, 0, 0), DOIS.quadro(50).posicao());
        assertEquals(10, DOIS.duracao());
    }

    @Test
    void quadrosForaDeOrdemOuVaziosNaoValem() {
        assertThrows(IllegalArgumentException.class, () -> new Coreografia(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Coreografia(List.of(
                new Coreografia.Quadro(5, Vetor.ZERO, Vetor.ZERO, 1, 0),
                new Coreografia.Quadro(5, Vetor.ZERO, Vetor.ZERO, 1, 0))));
    }

    @Test
    void naBaseGiraParaADirecao() {
        // Olhando para +X: frente vira +X, cima continua +Y.
        Vetor origem = new Vetor(100, 64, 100);
        Vetor dir = new Vetor(1, 0, 0);
        Vetor frente = Coreografia.naBase(new Vetor(0, 0, 2), origem, dir);
        assertEquals(102, frente.x(), 1e-9);
        assertEquals(64, frente.y(), 1e-9);
        Vetor cima = Coreografia.naBase(new Vetor(0, 3, 0), origem, dir);
        assertEquals(67, cima.y(), 1e-9);
        Vetor lado = Coreografia.naBase(new Vetor(1, 0, 0), origem, dir);
        assertEquals(1, lado.distancia(origem), 1e-9);
        assertEquals(100, lado.x(), 1e-9, "o lado é perpendicular à frente");
    }

    @Test
    void corteSecoSeguraOQuadroAnteriorEPulaNoTick() {
        Coreografia c = new Coreografia(List.of(
                new Coreografia.Quadro(0, Vetor.ZERO, new Vetor(0, 0, 1), 1, 0),
                new Coreografia.Quadro(10, new Vetor(50, 0, 0), new Vetor(0, 0, 1), 1, 0, true)));
        assertEquals(Vetor.ZERO, c.quadro(9.9).posicao(), "até o corte, nada se mexe");
        assertEquals(new Vetor(50, 0, 0), c.quadro(10).posicao());
    }

    @Test
    void encantamentoDoRTerminaNoDisparoESeguraAteAParte2Chegar() {
        Coreografia c = CutsceneRoxo200.COREOGRAFIA;
        assertEquals(CutsceneRoxo200.FIM_ENCANTAMENTO + CutsceneRoxo200.ESPERA, c.duracao());
        Coreografia.Quadro disparo = c.quadro(CutsceneRoxo200.FIM_ENCANTAMENTO);
        Coreografia.Quadro fim = c.quadro(c.duracao());
        assertEquals(disparo.posicao(), fim.posicao(), "segura o último plano");
        assertEquals(disparo.alvo(), fim.alvo());
        assertEquals(disparo.fov(), fim.fov(), 1e-9);
        assertEquals(4, CutsceneRoxo200.PALAVRAS.size(), "as quatro palavras; o nome do golpe vai para a parte 2");
        for (int i = 1; i < CutsceneRoxo200.PALAVRAS.size(); i++) {
            assertTrue(CutsceneRoxo200.PALAVRAS.get(i).inicio() >= CutsceneRoxo200.PALAVRAS.get(i - 1).fim(), "uma de cada vez");
        }
    }
}
