package com.minerwk.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.boss.MotorBoss.Evento;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MotorBossTest {
    // perto: aviso 3, janela 2, pausa 2, recarga 20, de 0 a 4 blocos. longe: de 6 a 14.
    private static final Golpe PERTO = new Golpe("perto", "a", 3, 2, 2, 20, 0, 4, 1);
    private static final Golpe LONGE = new Golpe("longe", "b", 4, 1, 3, 30, 6, 14, 1);

    private static List<Evento> rodar(MotorBoss m, int ticks, double distancia, long inicio) {
        List<Evento> eventos = new ArrayList<>();
        for (int i = 0; i < ticks; i++) eventos.add(m.tick(distancia, inicio + i, .5));
        return eventos;
    }

    @Test
    void golpePassaPorAvisoJanelaEPausa() {
        MotorBoss m = new MotorBoss(List.of(PERTO), 5);
        List<Evento> e = rodar(m, 8, 2, 0);
        // tick 0 começa; aviso nos ticks 0 a 2; a janela abre no tick 3 e fecha no 5; a pausa vai até o 7.
        assertEquals(Evento.COMECOU, e.get(0));
        assertEquals(Evento.NADA, e.get(1));
        assertEquals(Evento.NADA, e.get(2));
        assertEquals(Evento.ABRIU_JANELA, e.get(3));
        assertEquals(Evento.NADA, e.get(4));
        assertEquals(Evento.FECHOU_JANELA, e.get(5));
        assertEquals(Evento.NADA, e.get(6));
        assertEquals(Evento.TERMINOU, e.get(7));
        assertNull(m.atual());
    }

    @Test
    void fasesDoGolpeEmAndamento() {
        MotorBoss m = new MotorBoss(List.of(PERTO), 5);
        m.tick(2, 0, .5);
        assertSame(PERTO, m.atual());
        assertTrue(m.noAviso());
        rodar(m, 3, 2, 1);
        assertTrue(m.naJanela());
        assertEquals(0, m.tempoNaJanela());
        m.tick(2, 4, .5);
        assertEquals(1, m.tempoNaJanela());
    }

    @Test
    void esperaOIntervaloEARecargaAntesDeRepetir() {
        MotorBoss m = new MotorBoss(List.of(PERTO), 5);
        rodar(m, 8, 2, 0);
        // Terminou no tick 7. Intervalo de 5 e recarga de 20 a contar do começo (tick 0): volta no tick 20.
        List<Evento> e = rodar(m, 12, 2, 8);
        assertFalse(e.contains(Evento.COMECOU), "ainda em recarga até o tick 19");
        assertEquals(Evento.COMECOU, m.tick(2, 20, .5));
    }

    @Test
    void soEscolheGolpeNoAlcance() {
        MotorBoss m = new MotorBoss(List.of(PERTO, LONGE), 5);
        assertEquals(Evento.NADA, m.tick(5, 0, .5), "a 5 blocos nenhum dos dois alcança");
        assertEquals(Evento.COMECOU, m.tick(10, 1, .5));
        assertSame(LONGE, m.atual());
    }

    @Test
    void pesoDecideEntreOsQueAlcancam() {
        Golpe raro = new Golpe("raro", "a", 1, 1, 1, 0, 0, 10, 1);
        Golpe comum = new Golpe("comum", "a", 1, 1, 1, 0, 0, 10, 3);
        MotorBoss m = new MotorBoss(List.of(raro, comum), 0);
        m.tick(2, 0, .1);
        assertSame(raro, m.atual(), "sorteio 0,1 cai no primeiro quarto");
        MotorBoss m2 = new MotorBoss(List.of(raro, comum), 0);
        m2.tick(2, 0, .9);
        assertSame(comum, m2.atual());
    }

    @Test
    void forcarInterrompeEComecaOGolpePedido() {
        MotorBoss m = new MotorBoss(List.of(PERTO, LONGE), 5);
        m.tick(2, 0, .5);
        m.forcar(LONGE, 1);
        assertSame(LONGE, m.atual());
        assertTrue(m.noAviso());
        List<Evento> e = rodar(m, 4, 99, 1);
        assertEquals(Evento.ABRIU_JANELA, e.get(3), "o golpe forçado não depende do alcance");
    }

    @Test
    void pararSoltaOGolpeEAcelerarEncurtaAsRecargas() {
        MotorBoss m = new MotorBoss(List.of(PERTO), 5);
        m.acelerar(.5);
        m.tick(2, 0, .5);
        m.parar(0);
        assertNull(m.atual());
        // Recarga de 20 vira 10, contada do começo do golpe (tick 0); o intervalo de 5 vira 3.
        List<Evento> e = rodar(m, 10, 2, 1);
        assertEquals(9, e.indexOf(Evento.COMECOU), "volta no tick 10");
    }

    @Test
    void golpeInvalidoERecusado() {
        assertThrows(IllegalArgumentException.class, () -> new Golpe("x", "a", -1, 1, 1, 0, 0, 4, 1));
        assertThrows(IllegalArgumentException.class, () -> new Golpe("x", "a", 1, 0, 1, 0, 0, 4, 1));
        assertThrows(IllegalArgumentException.class, () -> new Golpe("x", "a", 1, 1, 1, 0, 5, 4, 1));
        assertThrows(IllegalArgumentException.class, () -> new Golpe("x", "a", 1, 1, 1, 0, 0, 4, 0));
    }
}
