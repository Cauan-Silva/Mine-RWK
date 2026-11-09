package com.minerwk.core.kit.teste;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.Tecla;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PersonagemTesteTest {

    @AfterEach
    void limpar() {
        com.minerwk.core.kit.PersonagensTestes.limpar();
    }

    @Test
    void personagemTesteTemZeX() {
        Personagem p = PersonagemTeste.criar();
        assertEquals("teste", p.id());
        assertEquals("teste_golpe", p.naTecla(Tecla.Z).orElseThrow().id());
        assertEquals(20, p.naTecla(Tecla.Z).orElseThrow().numeros().custo());
        assertTrue(p.naTecla(Tecla.X).orElseThrow().numeros().segurar());
        assertTrue(p.naTecla(Tecla.R).isEmpty());
        assertEquals(100, p.recurso().get().atual());
    }

    @Test
    void escolhiveisEscondeTesteQuandoHaOutro() {
        Personagens.registrar(PersonagemTeste.criar());
        assertEquals(List.of("teste"), Personagens.escolhiveis().stream().map(Personagem::id).toList());
        Personagens.registrar(new Personagem("gojo", "Gojo", "Jujutsu Kaisen", RecursoFolego::new, Map.of()));
        assertEquals(List.of("gojo"), Personagens.escolhiveis().stream().map(Personagem::id).toList());
        assertEquals(2, Personagens.todos().size());
    }

    @Test
    void idDesconhecidoDevolveVazio() {
        Personagens.registrar(PersonagemTeste.criar());
        assertTrue(Personagens.porId("nao_existe").isEmpty());
        assertTrue(Personagens.porId("teste").isPresent());
    }

    @Test
    void idRepetidoLancaExcecao() {
        Personagens.registrar(PersonagemTeste.criar());
        assertThrows(IllegalArgumentException.class, () -> Personagens.registrar(PersonagemTeste.criar()));
    }
}
