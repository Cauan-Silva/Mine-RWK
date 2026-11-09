package com.minerwk.core.kit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RecargasTest {

    @Test
    void habilidadeNuncaUsadaEstaPronta() {
        Recargas r = new Recargas();
        assertTrue(r.pronta("z", 0));
        assertEquals(0, r.restante("z", 0));
    }

    @Test
    void usarIniciaRecargaEBloqueiaAteOFim() {
        Recargas r = new Recargas();
        assertTrue(r.usar("z", 100, 40));
        assertFalse(r.usar("z", 139, 40));
        assertEquals(1, r.restante("z", 139));
        assertTrue(r.pronta("z", 140));
        assertTrue(r.usar("z", 140, 40));
    }

    @Test
    void habilidadesSaoIndependentes() {
        Recargas r = new Recargas();
        r.iniciar("z", 0, 100);
        assertTrue(r.pronta("x", 10));
        assertFalse(r.pronta("z", 10));
    }

    @Test
    void duracaoNegativaEhRecusada() {
        assertThrows(IllegalArgumentException.class, () -> new Recargas().iniciar("z", 0, -1));
    }

    @Test
    void limparLiberaTudo() {
        Recargas r = new Recargas();
        r.iniciar("z", 0, 100);
        r.limpar();
        assertTrue(r.pronta("z", 1));
    }

    @Test
    void salvarECarregarPreservaTicks() {
        Recargas r = new Recargas();
        r.iniciar("z", 100, 40);
        Recargas outra = new Recargas();
        outra.carregar(r.salvar(), 0);
        assertEquals(10, outra.restante("z", 130));
        assertTrue(outra.pronta("x", 0));
    }

    @Test
    void carregarUsaORestanteComoDuracao() {
        Recargas r = new Recargas();
        r.iniciar("z", 100, 40);
        Recargas outra = new Recargas();
        outra.carregar(r.salvar(), 120);
        assertEquals(20, outra.duracao("z"));
        outra.carregar(r.salvar(), 500);
        assertEquals(0, outra.duracao("z"));
    }
}
