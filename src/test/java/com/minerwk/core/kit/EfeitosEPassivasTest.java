package com.minerwk.core.kit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EfeitosEPassivasTest {
    private static final EstadoDoJogador ESTADO = new EstadoDoJogador(false, false, false);
    private static final Contexto CTX = new Contexto() {};

    /** Efeito falso que conta ticks e anota quando acabou. */
    static final class Contador implements Efeito {
        final int duracao;
        final List<Integer> ticks = new ArrayList<>();
        boolean acabou;
        boolean encerrar;
        Contador(int duracao) { this.duracao = duracao; }
        public int duracao() { return duracao; }
        public void tick(Contexto c, int t) { ticks.add(t); }
        public boolean encerrado() { return encerrar; }
        public void aoAcabar(Contexto c) { acabou = true; }
    }

    private static KitDoJogador kit(Habilidade z, Habilidade x, List<java.util.function.Supplier<Passiva>> passivas) {
        var r = new KitDoJogadorTest.RecursoFixo(100);
        return new KitDoJogador(new Personagem("p", "P", "Obra", () -> r, Map.of(Tecla.Z, z, Tecla.X, x), passivas));
    }

    @Test
    void efeitoDuraOTempoCertoEAcabaUmaVez() {
        var golpe = new KitDoJogadorTest.Falsa("z", Numeros.instantanea(1, 0, 0, 0, 0));
        var k = kit(golpe, golpe, List.of());
        var efeito = new Contador(5);
        k.adicionarEfeito(efeito);
        for (int i = 0; i < 8; i++) k.tick(i, ESTADO, CTX);
        assertEquals(List.of(0, 1, 2, 3, 4), efeito.ticks);
        assertTrue(efeito.acabou);
        assertEquals(0, k.efeitos().size());
    }

    @Test
    void efeitoNaoTravaOutraHabilidade() {
        var z = new KitDoJogadorTest.Falsa("z", Numeros.instantanea(1, 0, 0, 0, 0));
        var x = new KitDoJogadorTest.Falsa("x", Numeros.instantanea(1, 0, 0, 0, 0));
        var k = kit(z, x, List.of());
        var efeito = new Contador(20);
        k.adicionarEfeito(efeito);
        k.tick(0, ESTADO, CTX);
        assertTrue(k.apertar(Tecla.X, 1));
        for (int i = 1; i < 5; i++) k.tick(i, ESTADO, CTX);
        assertTrue(x.chamadas.contains("disparo"));
        assertEquals(5, efeito.ticks.size());
    }

    @Test
    void efeitoEncerradoSaiAntes() {
        var golpe = new KitDoJogadorTest.Falsa("z", Numeros.instantanea(1, 0, 0, 0, 0));
        var k = kit(golpe, golpe, List.of());
        var efeito = new Contador(50);
        k.adicionarEfeito(efeito);
        k.tick(0, ESTADO, CTX);
        efeito.encerrar = true;
        k.tick(1, ESTADO, CTX);
        assertTrue(efeito.acabou);
        assertEquals(0, k.efeitos().size());
    }

    @Test
    void descartarEncerraEfeitos() {
        var golpe = new KitDoJogadorTest.Falsa("z", Numeros.instantanea(1, 0, 0, 0, 0));
        var k = kit(golpe, golpe, List.of());
        var efeito = new Contador(50);
        k.adicionarEfeito(efeito);
        k.descartar();
        assertEquals(0, k.efeitos().size());
        assertFalse(efeito.acabou, "descartar não roda aoAcabar (jogador morreu ou saiu)");
    }

    @Test
    void passivaRodaTodoTick() {
        List<EstadoDoJogador> vistos = new ArrayList<>();
        Passiva p = (c, e) -> vistos.add(e);
        var golpe = new KitDoJogadorTest.Falsa("z", Numeros.instantanea(1, 0, 0, 0, 0));
        var k = kit(golpe, golpe, List.of(() -> p));
        for (int i = 0; i < 3; i++) k.tick(i, ESTADO, CTX);
        assertEquals(3, vistos.size());
    }

    /** Habilidade de carga longa que só cai com dano 6 ou mais. */
    static final class Resistente extends KitDoJogadorTest.Falsa {
        Resistente() { super("r", Numeros.instantanea(80, 0, 0, 0, 400)); }
        public float danoQueInterrompe() { return 6f; }
    }

    @Test
    void danoForteNaCargaInterrompeEFracoNao() {
        var r = new Resistente();
        var k = kit(r, r, List.of());
        assertTrue(k.apertar(Tecla.Z, 0));
        k.tick(0, ESTADO, CTX);
        k.levouDano(5f, 1);
        assertTrue(k.execucao().isPresent());
        k.levouDano(6f, 2);
        assertTrue(k.execucao().isEmpty());
        assertEquals(100, k.recargas().restante("r", 2));
    }

    @Test
    void semLimiteDanoNaoInterrompe() {
        var golpe = new KitDoJogadorTest.Falsa("z", Numeros.instantanea(40, 0, 0, 0, 0));
        var k = kit(golpe, golpe, List.of());
        k.apertar(Tecla.Z, 0);
        k.tick(0, ESTADO, CTX);
        k.levouDano(1000f, 1);
        assertTrue(k.execucao().isPresent());
    }

    @Test
    void vetorBasico() {
        Vetor a = new Vetor(1, 2, 2);
        assertEquals(3, a.comprimento(), 1e-9);
        assertEquals(1, a.normalizado().comprimento(), 1e-9);
        assertEquals(new Vetor(3, 6, 6), a.vezes(3));
        assertEquals(new Vetor(2, 4, 4), a.mais(a));
        assertEquals(3, a.distancia(Vetor.ZERO), 1e-9);
        assertEquals(Vetor.ZERO, Vetor.ZERO.normalizado());
    }
}
