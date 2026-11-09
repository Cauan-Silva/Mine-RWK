package com.minerwk.core.kit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KitDoJogadorTest {
    private static final EstadoDoJogador ESTADO = new EstadoDoJogador(false, false, false);
    private static final Contexto CTX = new Contexto() {};

    /** Recurso falso: não regenera. */
    static final class RecursoFixo implements Recurso {
        double atual;
        RecursoFixo(double v) { atual = v; }
        public double atual() { return atual; }
        public double maximo() { return 100; }
        public boolean pode(double c) { return atual >= c; }
        public void gastar(double c) { if (!pode(c)) throw new IllegalStateException(); atual -= c; }
        public void tick(EstadoDoJogador e) { }
        public double salvar() { return atual; }
        public void carregar(double v) { atual = v; }
    }

    /** Habilidade falsa que anota os ganchos chamados. */
    static final class Falsa implements Habilidade {
        final String id;
        final Numeros n;
        final List<String> chamadas = new ArrayList<>();
        Falsa(String id, Numeros n) { this.id = id; this.n = n; }
        public String id() { return id; }
        public Numeros numeros() { return n; }
        public void aoIniciarCarga(Contexto c) { chamadas.add("carga"); }
        public void aoDisparar(Contexto c, float f) { chamadas.add("disparo"); }
        public void aoTerminar(Contexto c) { chamadas.add("fim"); }
    }

    // Z: carga 5, disparo 2, fim 2, custo 20, recarga 40. X: segurar 10..30, custo 30, recarga 100.
    final Falsa z = new Falsa("z", Numeros.instantanea(5, 2, 2, 20, 40));
    final Falsa x = new Falsa("x", new Numeros(10, 30, 0, 0, true, 30, 100));

    KitDoJogador kit(double recurso) {
        RecursoFixo r = new RecursoFixo(recurso);
        return new KitDoJogador(new Personagem("p", "P", "Obra", () -> r, Map.of(Tecla.Z, z, Tecla.X, x)));
    }

    /** Roda {@code n} ticks a partir de {@code inicio}; devolve o próximo tick. */
    static long rodar(KitDoJogador k, long inicio, int n) {
        for (int i = 0; i < n; i++) k.tick(inicio + i, ESTADO, CTX);
        return inicio + n;
    }

    @Test
    void custoCobradoSoNoDisparo() {
        KitDoJogador k = kit(100);
        assertTrue(k.apertar(Tecla.Z, 0));
        rodar(k, 0, 4);
        assertEquals(100, k.recurso().atual());
        k.tick(4, ESTADO, CTX);
        assertEquals(80, k.recurso().atual());
    }

    @Test
    void recargaComecaNoDisparo() {
        KitDoJogador k = kit(100);
        k.apertar(Tecla.Z, 0);
        rodar(k, 0, 5); // dispara no tick 4
        assertEquals(40, k.recargas().restante("z", 4));
        assertTrue(k.recargas().pronta("z", 44));
    }

    @Test
    void naoComecaEmRecargaOuSemRecurso() {
        KitDoJogador k = kit(100);
        k.apertar(Tecla.Z, 0);
        long t = rodar(k, 0, 9); // dispara e termina
        assertTrue(k.execucao().isEmpty());
        assertFalse(k.apertar(Tecla.Z, t));
        KitDoJogador pobre = kit(10);
        assertFalse(pobre.apertar(Tecla.Z, 0));
        assertTrue(pobre.execucao().isEmpty());
    }

    @Test
    void atordoarNaCargaDaPunicaoDeUmQuarto() {
        KitDoJogador k = kit(100);
        k.apertar(Tecla.Z, 0);
        rodar(k, 0, 2);
        k.atordoar(2);
        assertTrue(k.execucao().isEmpty());
        assertEquals(100, k.recurso().atual());
        assertFalse(k.recargas().pronta("z", 11));
        assertTrue(k.recargas().pronta("z", 12));
        assertFalse(z.chamadas.contains("disparo"));
    }

    @Test
    void atordoarDepoisDoDisparoNaoFazNada() {
        KitDoJogador k = kit(100);
        k.apertar(Tecla.Z, 0);
        rodar(k, 0, 6);
        k.atordoar(6);
        assertTrue(k.execucao().isPresent());
        assertEquals(40, k.recargas().restante("z", 4));
    }

    @Test
    void outraTeclaDuranteExecucaoEhIgnorada() {
        KitDoJogador k = kit(100);
        k.apertar(Tecla.Z, 0);
        assertFalse(k.apertar(Tecla.X, 1));
        assertSame(z, k.execucao().orElseThrow().habilidade());
        k.soltar(Tecla.X); // não pode afetar a execução de Z
        assertSame(z, k.execucao().orElseThrow().habilidade());
    }

    @Test
    void teclaSemHabilidadeNaoFazNada() {
        KitDoJogador k = kit(100);
        assertFalse(k.apertar(Tecla.R, 0));
        k.soltar(Tecla.R);
        assertTrue(k.execucao().isEmpty());
    }

    @Test
    void descartarNaoCobraNemPune() {
        KitDoJogador k = kit(100);
        k.apertar(Tecla.Z, 0);
        rodar(k, 0, 3);
        k.descartar();
        assertTrue(k.execucao().isEmpty());
        assertEquals(100, k.recurso().atual());
        assertTrue(k.recargas().pronta("z", 3));
    }

    @Test
    void semRecursoNoDisparoViraInterrupcao() {
        KitDoJogador k = kit(100);
        k.apertar(Tecla.Z, 0);
        rodar(k, 0, 2);
        ((RecursoFixo) k.recurso()).atual = 5; // gastou em outra coisa durante a carga
        rodar(k, 2, 3); // tick 4 seria o disparo
        assertTrue(k.execucao().isEmpty());
        assertEquals(5, k.recurso().atual());
        assertFalse(z.chamadas.contains("disparo"));
        assertTrue(k.recargas().pronta("z", 14));
        assertFalse(k.recargas().pronta("z", 13));
    }

    @Test
    void soltarDisparaHabilidadeDeSegurar() {
        KitDoJogador k = kit(100);
        k.apertar(Tecla.X, 0);
        rodar(k, 0, 15);
        k.soltar(Tecla.X);
        k.tick(15, ESTADO, CTX);
        assertEquals(70, k.recurso().atual());
    }

    @Test
    void ganchosChamadosNaOrdem() {
        KitDoJogador k = kit(100);
        k.apertar(Tecla.Z, 0);
        rodar(k, 0, 9);
        assertEquals(List.of("carga", "disparo", "fim"), z.chamadas);
        assertTrue(k.execucao().isEmpty());
    }
}
