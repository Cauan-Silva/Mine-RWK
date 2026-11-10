package com.minerwk.core.maestria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.kit.Numeros;
import com.minerwk.core.maestria.Arvore.Resultado;
import com.minerwk.core.personagens.gojo.ArvoreGojo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArvoreTest {
    private static final Arvore ARVORE = new Arvore(List.of(
            No.melhoria("alcance", false, .10),
            No.recarga("recarga", false, "golpe", .08),
            No.carga("carga", true, "golpe", .10),
            No.despertar("despertar", 5, 20)));

    @Test
    void cadaNivelDeUmNoCustaUmDoisETres() {
        No no = ARVORE.no("alcance").orElseThrow();
        assertEquals(1, no.custoDoProximo(0));
        assertEquals(2, no.custoDoProximo(1));
        assertEquals(3, no.custoDoProximo(2));
        assertEquals(0, no.custoDoProximo(3));
        assertEquals(5, ARVORE.no("despertar").orElseThrow().custoDoProximo(0));
    }

    @Test
    void gastoSomaOsNiveisComprados() {
        assertEquals(0, ARVORE.gasto(Map.of()));
        assertEquals(1 + 2 + 1, ARVORE.gasto(Map.of("alcance", 2, "recarga", 1)));
        assertEquals(6 + 5, ARVORE.gasto(Map.of("alcance", 3, "despertar", 1, "de_outra_versao", 3)));
    }

    @Test
    void compraDescontaPontosERespeitaOLimite() {
        Map<String, Integer> nos = new HashMap<>();
        // Nível 4 de maestria: 3 pontos.
        assertEquals(Resultado.OK, ARVORE.comprar(nos, "alcance", 4, false));
        assertEquals(Resultado.OK, ARVORE.comprar(nos, "alcance", 4, false));
        assertEquals(2, nos.get("alcance"));
        assertEquals(Resultado.SEM_PONTOS, ARVORE.comprar(nos, "alcance", 4, false));
        assertEquals(2, nos.get("alcance"));
        assertEquals(Resultado.OK, ARVORE.comprar(nos, "alcance", 100, false));
        assertEquals(Resultado.NIVEL_MAXIMO, ARVORE.comprar(nos, "alcance", 100, false));
        assertEquals(Resultado.NO_DESCONHECIDO, ARVORE.comprar(nos, "nao_existe", 100, false));
    }

    @Test
    void despertarExigeMaestriaENosDeDepoisExigemTerDespertado() {
        Map<String, Integer> nos = new HashMap<>();
        assertEquals(Resultado.MAESTRIA_BAIXA, ARVORE.comprar(nos, "despertar", 19, false));
        assertEquals(Resultado.OK, ARVORE.comprar(nos, "despertar", 20, false));
        assertEquals(Resultado.NAO_DESPERTOU, ARVORE.comprar(nos, "carga", 100, false));
        assertEquals(Resultado.OK, ARVORE.comprar(nos, "carga", 100, true));
    }

    @Test
    void pontosLivresNaoFicamNegativos() {
        assertEquals(19 - 5, ARVORE.pontosLivres(Map.of("despertar", 1), 20));
        assertEquals(0, ARVORE.pontosLivres(Map.of("alcance", 3), 2));
    }

    @Test
    void melhoriasViramFatores() {
        Melhorias m = new Melhorias(ARVORE, Map.of("alcance", 2, "recarga", 3, "carga", 1));
        assertEquals(1.2, m.mais("alcance"), 1e-9);
        assertEquals(.8, m.menos("alcance"), 1e-9);
        assertEquals(1, m.mais("nao_existe"), 1e-9);
        assertEquals(2, m.nivel("alcance"));
        Numeros n = m.ajustar("golpe", new Numeros(10, 30, 2, 6, true, 30, 200));
        assertEquals(9, n.cargaMin());
        assertEquals(27, n.cargaMax());
        assertEquals(152, n.recarga());
        assertEquals(30, n.custo(), 1e-9);
        Numeros outro = new Numeros(10, 30, 2, 6, true, 30, 200);
        assertEquals(outro, m.ajustar("outro_golpe", outro));
        assertEquals(outro, Melhorias.NENHUMA.ajustar("golpe", outro));
    }

    @Test
    void nivelSalvoAcimaDoMaximoNaoPassaDoLimite() {
        Melhorias m = new Melhorias(ARVORE, Map.of("alcance", 99));
        assertEquals(1.3, m.mais("alcance"), 1e-9);
    }

    @Test
    void arvoreNaoAceitaNoRepetido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Arvore(List.of(No.melhoria("a", false, .1), No.melhoria("a", false, .1))));
    }

    @Test
    void arvoreDoGojoBateComOSpec() {
        Arvore gojo = ArvoreGojo.ARVORE;
        assertEquals(14, gojo.nos().size());
        Map<String, Integer> tudo = new HashMap<>();
        for (No no : gojo.nos()) tudo.put(no.id(), no.niveis());
        assertEquals(83, gojo.gasto(tudo));
        No despertar = gojo.no(ArvoreGojo.DESPERTAR).orElseThrow();
        assertEquals(20, despertar.nivelMinimo());
        assertEquals(5, despertar.custoDoProximo(0));
        assertEquals(6, gojo.nos().stream().filter(n -> !n.depoisDoDespertar() && n != despertar).count());
        assertEquals(7, gojo.nos().stream().filter(No::depoisDoDespertar).count());
        assertTrue(gojo.no(ArvoreGojo.AZUL_ALCANCE).isPresent());
        assertFalse(gojo.no(ArvoreGojo.VERMELHO_FORCA).orElseThrow().niveis() != 3);
    }
}
