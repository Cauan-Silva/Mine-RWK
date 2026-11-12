package com.minerwk.core.hud;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Recargas;
import com.minerwk.core.kit.Tecla;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogicaHudTest {

    @Test
    void segundosSeguemARegraDoSpec() {
        assertEquals("4", LogicaHud.segundos(61));
        assertEquals("1", LogicaHud.segundos(20));
        assertEquals("0.9", LogicaHud.segundos(19));
        assertEquals("0.3", LogicaHud.segundos(7));
        assertEquals("0.1", LogicaHud.segundos(1));
        assertEquals("", LogicaHud.segundos(0));
        assertEquals("", LogicaHud.segundos(-5));
    }

    @Test
    void fracaoFicaEntreZeroEUm() {
        assertEquals(0.5f, LogicaHud.fracao(100, 40, 80), 1e-6);
        assertEquals(0f, LogicaHud.fracao(100, 40, 130));
        assertEquals(1f, LogicaHud.fracao(100, 40, 10));
        assertEquals(0f, LogicaHud.fracao(100, 0, 50));
    }

    @Test
    void punicaoUsaADuracaoDaPropriaRecarga() {
        Recargas r = new Recargas();
        r.iniciar("x", 0, 10);
        assertEquals(10, r.duracao("x"));
        assertEquals(0.5f, LogicaHud.fracao(10, r.duracao("x"), 5), 1e-6);
        assertEquals(0, r.duracao("nunca"));
    }

    @Test
    void semRecursoQuandoAtualMenorQueCusto() {
        assertTrue(LogicaHud.semRecurso(10, 20));
        assertFalse(LogicaHud.semRecurso(20, 20));
    }

    @Test
    void teclasVisiveisNaOrdemSoAsExistentes() {
        assertEquals(List.of(Tecla.Z, Tecla.X, Tecla.R), LogicaHud.teclasVisiveis(EnumSet.of(Tecla.R, Tecla.X, Tecla.Z)));
        assertEquals(List.of(), LogicaHud.teclasVisiveis(EnumSet.noneOf(Tecla.class)));
    }

    @Test
    void barraDeXpFicaEntreZeroEUmECheiaNoNivelMaximo() {
        assertEquals(.5, LogicaHud.fracaoXp(55, 110), 1e-9);
        assertEquals(0, LogicaHud.fracaoXp(-3, 110), 1e-9);
        assertEquals(1, LogicaHud.fracaoXp(500, 110), 1e-9);
        assertEquals(1, LogicaHud.fracaoXp(0, 0), 1e-9);
    }

    @Test
    void semPoderesQuandoNaoHaRecursoETodosOsGolpesEstaoParados() {
        assertEquals(1150, LogicaHud.semPoderes(0, java.util.List.of(1150L, 1150L, 1100L)), "o aviso mostra o tempo do último a voltar");
        assertEquals(0, LogicaHud.semPoderes(0, java.util.List.of(1150L, 0L, 1100L)), "um golpe pronto: ainda tem poderes");
        assertEquals(0, LogicaHud.semPoderes(5, java.util.List.of(1150L, 1150L)), "com recurso não é perda de poderes");
        assertEquals(0, LogicaHud.semPoderes(0, java.util.List.of()), "sem golpes na forma não há o que avisar");
        assertEquals(0, LogicaHud.semPoderes(0, java.util.List.of(30L, 40L)), "recargas curtas ao mesmo tempo não são o custo do Mugetsu");
    }
}
