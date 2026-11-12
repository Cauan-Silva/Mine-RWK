package com.minerwk.core.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.maestria.Arvore;
import com.minerwk.core.kit.teste.RecursoFolego;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** A variação de uma tecla para quem está agachado, e o fim antecipado de um efeito. */
class AgachadoEEncerrarTest {
    private static final EstadoDoJogador PARADO = new EstadoDoJogador(true, false, false);

    private record Simples(String id, Numeros numeros, Habilidade variacao, List<String> disparos) implements Habilidade {
        @Override
        public Habilidade agachado() {
            return variacao == null ? this : variacao;
        }

        @Override
        public void aoDisparar(Contexto contexto, float forca) {
            disparos.add(id);
        }
    }

    private final List<String> disparos = new ArrayList<>();
    private final Simples baixo = new Simples("baixo", new Numeros(2, 10, 1, 1, true, 0, 40), null, disparos);
    private final Simples normal = new Simples("normal", Numeros.instantanea(1, 1, 1, 0, 40), baixo, disparos);

    private KitDoJogador kit(Trancas trancas) {
        return new KitDoJogador(new Personagem("p", "P", "obra", RecursoFolego::new, Map.of(Tecla.R, normal), List.of(),
                Arvore.VAZIA, trancas));
    }

    @Test
    void semVariacaoAgachadoEAHabilidadeDeSempre() {
        assertSame(baixo, baixo.agachado());
        KitDoJogador kit = new KitDoJogador(new Personagem("p", "P", "obra", RecursoFolego::new, Map.of(Tecla.R, baixo)));
        assertTrue(kit.apertar(Tecla.R, 0, true));
        assertSame(baixo, kit.execucao().orElseThrow().habilidade());
    }

    @Test
    void agachadoATeclaUsaAVariacaoComRecargaPropria() {
        KitDoJogador kit = kit(Trancas.NENHUMA);
        assertTrue(kit.apertar(Tecla.R, 0, true));
        for (int i = 0; i < 20; i++) kit.tick(i, PARADO, Contexto.NULO);
        assertEquals(List.of("baixo"), disparos);
        assertFalse(kit.recargas().pronta("baixo", 20));
        assertTrue(kit.recargas().pronta("normal", 20), "a habilidade de pé não entrou em recarga");
        assertTrue(kit.apertar(Tecla.R, 20));
        for (int i = 20; i < 30; i++) kit.tick(i, PARADO, Contexto.NULO);
        assertEquals(List.of("baixo", "normal"), disparos);
    }

    @Test
    void soltarATeclaSoltaTambemAVariacao() {
        KitDoJogador kit = kit(Trancas.NENHUMA);
        assertTrue(kit.apertar(Tecla.R, 0, true));
        kit.tick(0, PARADO, Contexto.NULO);
        kit.tick(1, PARADO, Contexto.NULO);
        kit.soltar(Tecla.R);
        kit.tick(2, PARADO, Contexto.NULO);
        assertEquals(List.of("baixo"), disparos, "é de segurar: disparou ao soltar, antes da carga máxima");
    }

    @Test
    void aTrancaValePorVariacao() {
        KitDoJogador kit = kit(new Trancas(Set.of("baixo"), false));
        assertFalse(kit.apertar(Tecla.R, 0, true), "a variação está trancada");
        assertTrue(kit.apertar(Tecla.R, 0, false));
    }

    @Test
    void asVariacoesEntramNaListaDeHabilidades() {
        List<String> ids = kit(Trancas.NENHUMA).personagem().todasAsHabilidades().stream().map(Habilidade::id).toList();
        assertEquals(List.of("normal", "baixo"), ids);
    }

    @Test
    void encerrarEfeitosAcabaSoOsDoTipoERodaOAoAcabar() {
        List<String> fins = new ArrayList<>();
        final class Longo implements Efeito {
            public int duracao() { return 100; }
            public void tick(Contexto c, int t) { }
            public void aoAcabar(Contexto c) { fins.add("longo"); }
        }
        final class Outro implements Efeito {
            public int duracao() { return 100; }
            public void tick(Contexto c, int t) { }
            public void aoAcabar(Contexto c) { fins.add("outro"); }
        }
        KitDoJogador kit = kit(Trancas.NENHUMA);
        kit.adicionarEfeito(new Longo());
        kit.adicionarEfeito(new Outro());
        assertTrue(kit.encerrarEfeitos(Longo.class, Contexto.NULO));
        assertEquals(List.of("longo"), fins);
        assertEquals(1, kit.efeitos().size());
        assertFalse(kit.encerrarEfeitos(Longo.class, Contexto.NULO));
    }
}
