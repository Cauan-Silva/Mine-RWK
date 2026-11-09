package com.minerwk.core.kit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExecucaoTest {

    private static Habilidade hab(Numeros n) {
        return new Habilidade() {
            public String id() { return "h"; }
            public Numeros numeros() { return n; }
        };
    }

    private static float forca(Execucao.Evento e) {
        return assertInstanceOf(Execucao.Disparou.class, e).forca();
    }

    @Test
    void instantaneaDisparaNoTickDaCargaComForca1() {
        Execucao ex = new Execucao(hab(Numeros.instantanea(3, 2, 2, 0, 0)));
        assertSame(Execucao.Evento.NADA, ex.tick());
        assertSame(Execucao.Evento.NADA, ex.tick());
        assertEquals(1f, forca(ex.tick()));
        assertEquals(Execucao.Fase.DISPARO, ex.fase());
    }

    @Test
    void instantaneaTerminaDepoisDeDisparoEFim() {
        Execucao ex = new Execucao(hab(Numeros.instantanea(3, 2, 2, 0, 0)));
        for (int i = 0; i < 3; i++) ex.tick();
        for (int i = 0; i < 3; i++) assertSame(Execucao.Evento.NADA, ex.tick());
        assertSame(Execucao.Evento.TERMINOU, ex.tick());
        assertEquals(Execucao.Fase.ACABOU, ex.fase());
    }

    @Test
    void segurarSoltandoNoMeioDaForcaProporcional() {
        Execucao ex = new Execucao(hab(new Numeros(10, 30, 0, 0, true, 0, 0)));
        for (int i = 0; i < 20; i++) assertSame(Execucao.Evento.NADA, ex.tick());
        ex.soltar();
        assertEquals(0.5f, forca(ex.tick()), 0.06f);
    }

    @Test
    void segurarChegandoNaCargaMaximaDisparaSozinho() {
        Execucao ex = new Execucao(hab(new Numeros(10, 30, 0, 0, true, 0, 0)));
        for (int i = 0; i < 29; i++) assertSame(Execucao.Evento.NADA, ex.tick());
        assertEquals(1f, forca(ex.tick()));
    }

    @Test
    void soltarAntesDaCargaMinimaEsperaAMinima() {
        Execucao ex = new Execucao(hab(new Numeros(10, 30, 0, 0, true, 0, 0)));
        ex.tick();
        ex.tick();
        ex.soltar();
        for (int i = 3; i <= 9; i++) assertSame(Execucao.Evento.NADA, ex.tick());
        assertEquals(0f, forca(ex.tick()));
    }

    @Test
    void numerosInvalidosLancamExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new Numeros(10, 5, 0, 0, true, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Numeros(0, 0, 0, 0, false, -1, 0));
    }
}
