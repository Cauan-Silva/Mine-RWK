package com.minerwk.core.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.maestria.Arvore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Formas (transformações): outro conjunto de golpes nas mesmas teclas, com recargas e trancas por golpe. */
class FormasTest {
    private static final EstadoDoJogador ESTADO = new EstadoDoJogador(false, false, false);
    private static final String BANKAI = "bankai";

    /** Golpe que anota o que aconteceu com ele. */
    private static final class Golpe implements Habilidade {
        final String id;
        final Numeros numeros;
        final List<String> chamadas = new ArrayList<>();

        Golpe(String id, Numeros numeros) {
            this.id = id;
            this.numeros = numeros;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Numeros numeros() {
            return numeros;
        }

        @Override
        public void aoDisparar(Contexto contexto, float forca) {
            chamadas.add("disparou");
        }

        @Override
        public void aoInterromper(Contexto contexto) {
            chamadas.add("interrompeu");
        }
    }

    /** Recurso que anota a forma que o kit lhe passou. */
    private static final class RecursoComForma extends KitDoJogadorTest.RecursoFixo {
        final List<String> formas = new ArrayList<>();

        RecursoComForma() {
            super(100);
        }

        @Override
        public void forma(String forma) {
            formas.add(forma);
        }
    }

    private static Golpe instantaneo(String id) {
        // carga 4, disparo 2, fim 2, custo 10, recarga 40
        return new Golpe(id, Numeros.instantanea(4, 2, 2, 10, 40));
    }

    private final Golpe zBase = instantaneo("z_base");
    private final Golpe vBase = instantaneo("v_base");
    private final Golpe zBankai = instantaneo("z_bankai");
    private final Golpe vBankai = instantaneo("v_bankai");
    private final RecursoComForma recurso = new RecursoComForma();

    private Personagem personagem(Trancas trancas) {
        return new Personagem("p", "P", "Obra", () -> recurso, Map.of(Tecla.Z, zBase, Tecla.V, vBase), List.of(),
                Arvore.VAZIA, trancas, List.of(new Forma(BANKAI, Map.of(Tecla.Z, zBankai, Tecla.V, vBankai))));
    }

    private KitDoJogador kit() {
        return new KitDoJogador(personagem(Trancas.NENHUMA));
    }

    private static void correr(KitDoJogador k, long de, int ticks) {
        for (int i = 0; i < ticks; i++) k.tick(de + i, ESTADO, Contexto.NULO);
    }

    @Test
    void comecaNaFormaBaseComOsGolpesDela() {
        KitDoJogador k = kit();
        assertEquals(Forma.BASE, k.forma());
        assertEquals(zBase, k.habilidade(Tecla.Z).orElseThrow());
        assertTrue(k.habilidade(Tecla.X).isEmpty());
    }

    @Test
    void mudarDeFormaTrocaOsGolpesDasTeclasEAvisaORecurso() {
        KitDoJogador k = kit();
        assertTrue(k.mudarForma(BANKAI, Contexto.NULO));
        assertEquals(BANKAI, k.forma());
        assertEquals(zBankai, k.habilidade(Tecla.Z).orElseThrow());
        assertEquals(List.of(BANKAI), recurso.formas);
        assertTrue(k.apertar(Tecla.Z, 0));
        assertEquals(zBankai, k.execucao().orElseThrow().habilidade());
        assertTrue(k.mudarForma(Forma.BASE, Contexto.NULO));
        assertEquals(List.of(BANKAI, Forma.BASE), recurso.formas);
    }

    @Test
    void formaDesconhecidaOuIgualNaoMuda() {
        KitDoJogador k = kit();
        assertFalse(k.mudarForma("nao_existe", Contexto.NULO));
        assertFalse(k.mudarForma(Forma.BASE, Contexto.NULO), "já está na base");
        assertEquals(Forma.BASE, k.forma());
        assertTrue(recurso.formas.isEmpty());
    }

    @Test
    void recargaEDeCadaGolpeENaoDaTecla() {
        KitDoJogador k = kit();
        assertTrue(k.apertar(Tecla.Z, 0));
        correr(k, 0, 10);
        assertEquals(List.of("disparou"), zBase.chamadas);
        assertFalse(k.apertar(Tecla.Z, 10), "o Z da base está em recarga");
        k.mudarForma(BANKAI, Contexto.NULO);
        assertTrue(k.apertar(Tecla.Z, 10), "o Z do Bankai é outro golpe: não herda a recarga");
        correr(k, 10, 10);
        // Sair e voltar não zera a recarga do golpe da base.
        k.mudarForma(Forma.BASE, Contexto.NULO);
        assertFalse(k.apertar(Tecla.Z, 20));
        assertTrue(k.apertar(Tecla.Z, 45), "passados os 40 ticks ele volta");
    }

    @Test
    void trancaValePorGolpeENaoPorTecla() {
        KitDoJogador k = new KitDoJogador(personagem(new Trancas(Set.of("v_bankai"), false)));
        assertFalse(k.trancada(Tecla.V), "o V da base é livre");
        assertTrue(k.apertar(Tecla.V, 0));
        k.descartar();
        k.mudarForma(BANKAI, Contexto.NULO);
        assertTrue(k.trancada(Tecla.V), "o V do Bankai exige o despertar");
        assertFalse(k.trancada(Tecla.Z));
        assertFalse(k.apertar(Tecla.V, 100));
        k.definirDesperto(true);
        assertFalse(k.trancada(Tecla.V));
        assertTrue(k.apertar(Tecla.V, 100));
    }

    @Test
    void trancasDasTeclasViramIdsDosGolpes() {
        Trancas t = Trancas.dasTeclas(Map.of(Tecla.Z, zBase, Tecla.V, vBase), Set.of(Tecla.V, Tecla.R), true);
        assertEquals(Set.of("v_base"), t.habilidades(), "tecla sem golpe é ignorada");
        assertTrue(t.passivas());
    }

    @Test
    void mudarDeFormaInterrompeOGolpeEmCargaSemPunir() {
        KitDoJogador k = kit();
        assertTrue(k.apertar(Tecla.Z, 0));
        correr(k, 0, 2);
        assertTrue(k.mudarForma(BANKAI, Contexto.NULO));
        assertTrue(k.execucao().isEmpty());
        assertEquals(List.of("interrompeu"), zBase.chamadas);
        k.mudarForma(Forma.BASE, Contexto.NULO);
        assertTrue(k.apertar(Tecla.Z, 3), "sem recarga de punição");
    }

    @Test
    void golpeQueJaDisparouNaoEInterrompidoPelaTroca() {
        // É o caso do próprio G: ele troca a forma de dentro do disparo e termina normalmente.
        KitDoJogador k = kit();
        assertTrue(k.apertar(Tecla.Z, 0));
        correr(k, 0, 5);
        assertEquals(List.of("disparou"), zBase.chamadas);
        assertTrue(k.mudarForma(BANKAI, Contexto.NULO));
        assertTrue(k.execucao().isPresent(), "o golpe em disparo segue até o fim");
        assertEquals(List.of("disparou"), zBase.chamadas);
    }

    @Test
    void soltarTeclaDeGolpeQueFicouNaOutraFormaNaoPrendeNada() {
        Golpe segurado = new Golpe("segurado", new Numeros(2, 20, 2, 2, true, 0, 0));
        Personagem p = new Personagem("p", "P", "Obra", () -> recurso, Map.of(Tecla.Z, segurado), List.of(),
                Arvore.VAZIA, Trancas.NENHUMA, List.of(new Forma(BANKAI, Map.of(Tecla.X, zBankai))));
        KitDoJogador k = new KitDoJogador(p);
        assertTrue(k.apertar(Tecla.Z, 0));
        correr(k, 0, 4);
        // A troca chega com o Z ainda segurado: a carga acaba, e soltar o Z depois não acha golpe nenhum.
        assertTrue(k.mudarForma(BANKAI, Contexto.NULO));
        k.soltar(Tecla.Z);
        correr(k, 4, 30);
        assertEquals(List.of("interrompeu"), segurado.chamadas, "não dispara nem fica carregando");
        assertTrue(k.execucao().isEmpty());
        assertTrue(k.apertar(Tecla.X, 40), "a forma nova responde normalmente");
    }

    @Test
    void mudarDeDimensaoMantemAFormaMasAcabaComOGolpe() {
        KitDoJogador k = kit();
        k.mudarForma(BANKAI, Contexto.NULO);
        assertTrue(k.apertar(Tecla.Z, 0));
        k.descartarMantendoAForma(Contexto.NULO);
        assertEquals(BANKAI, k.forma());
        assertTrue(k.execucao().isEmpty());
        assertEquals(List.of(BANKAI), recurso.formas, "o recurso não é avisado de troca nenhuma");
    }

    @Test
    void personagemGuardaAsFormasERecusaIdsRepetidos() {
        Personagem p = personagem(Trancas.NENHUMA);
        assertTrue(p.temForma(Forma.BASE));
        assertTrue(p.temForma(BANKAI));
        assertFalse(p.temForma("outra"));
        assertEquals(vBankai, p.naTecla(BANKAI, Tecla.V).orElseThrow());
        assertEquals(vBase, p.naTecla(Forma.BASE, Tecla.V).orElseThrow());
        assertEquals(4, p.todasAsHabilidades().size());
        assertThrows(IllegalArgumentException.class, () -> new Forma(Forma.BASE, Map.of()));
        Forma repetida = new Forma(BANKAI, Map.of());
        assertThrows(IllegalArgumentException.class, () -> new Personagem("p", "P", "Obra", () -> recurso, Map.of(),
                List.of(), Arvore.VAZIA, Trancas.NENHUMA, List.of(repetida, repetida)));
    }

    @Test
    void descartarVoltaAFormaBase() {
        KitDoJogador k = kit();
        k.mudarForma(BANKAI, Contexto.NULO);
        k.descartar();
        assertEquals(Forma.BASE, k.forma());
        assertEquals(List.of(BANKAI, Forma.BASE), recurso.formas);
        assertEquals(zBase, k.habilidade(Tecla.Z).orElseThrow());
    }

    @Test
    void personagemSemTransformacaoSoTemABase() {
        Personagem p = new Personagem("p", "P", "Obra", () -> recurso, Map.of(Tecla.Z, zBase));
        assertTrue(p.transformacoes().isEmpty());
        KitDoJogador k = new KitDoJogador(p);
        assertFalse(k.mudarForma(BANKAI, Contexto.NULO));
        assertEquals(Forma.BASE, k.forma());
    }
}
