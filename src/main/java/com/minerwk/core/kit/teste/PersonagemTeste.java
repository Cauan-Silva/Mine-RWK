package com.minerwk.core.kit.teste;

import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Forma;
import com.minerwk.core.kit.Habilidade;
import com.minerwk.core.kit.Numeros;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.Tecla;
import java.util.Map;

/**
 * Personagem para provar o núcleo antes do Gojo: um golpe instantâneo (Z) e um de segurar (X). O G entra e
 * sai de uma transformação em que o Z é outro golpe.
 */
public final class PersonagemTeste {
    private PersonagemTeste() {
    }

    public static Personagem criar() {
        Transformar transformar = new Transformar();
        return new Personagem(Personagens.TESTE, "Personagem de teste", "Mine-RWK", RecursoFolego::new,
                Map.of(Tecla.Z, new Golpe(), Tecla.X, new Carga(), Tecla.G, transformar), java.util.List.of(),
                com.minerwk.core.maestria.Arvore.VAZIA, com.minerwk.core.kit.Trancas.NENHUMA,
                java.util.List.of(new Forma(FORMA, Map.of(Tecla.Z, new GolpeForte(), Tecla.G, transformar))));
    }

    /** Id da transformação de teste. */
    public static final String FORMA = "teste_forma";

    /** Entra na transformação e sai dela. */
    static final class Transformar implements Habilidade {
        private static final Numeros NUMEROS = Numeros.instantanea(2, 1, 2, 0, 20);

        @Override
        public String id() {
            return "teste_transformar";
        }

        @Override
        public Numeros numeros() {
            return NUMEROS;
        }

        @Override
        public void aoDisparar(Contexto contexto, float forca) {
            contexto.mudarForma(Forma.BASE.equals(contexto.forma()) ? FORMA : Forma.BASE);
        }
    }

    /** O Z da transformação: mais dano, outra recarga. */
    static final class GolpeForte implements Habilidade {
        private static final Numeros NUMEROS = Numeros.instantanea(3, 2, 5, 20, 20);

        @Override
        public String id() {
            return "teste_golpe_forte";
        }

        @Override
        public Numeros numeros() {
            return NUMEROS;
        }

        @Override
        public void aoDisparar(Contexto contexto, float forca) {
            contexto.danoEmArea(4, 8);
            contexto.apresentacao("gojo_azul", "", "teste");
        }
    }

    /** Golpe instantâneo em área curta. */
    static final class Golpe implements Habilidade {
        private static final Numeros NUMEROS = Numeros.instantanea(5, 2, 5, 20, 40);

        @Override
        public String id() {
            return "teste_golpe";
        }

        @Override
        public Numeros numeros() {
            return NUMEROS;
        }

        @Override
        public void aoDisparar(Contexto contexto, float forca) {
            contexto.danoEmArea(3, 4);
            contexto.apresentacao("gojo_azul", "", "teste");
        }
    }

    /** Segura para carregar; o dano cresce com a força. */
    static final class Carga implements Habilidade {
        private static final Numeros NUMEROS = new Numeros(10, 40, 2, 10, true, 30, 100);

        @Override
        public String id() {
            return "teste_carga";
        }

        @Override
        public Numeros numeros() {
            return NUMEROS;
        }

        @Override
        public void aoDisparar(Contexto contexto, float forca) {
            contexto.danoEmArea(5, 4 + 8 * forca);
            contexto.apresentacao("gojo_roxo", "", "teste");
            contexto.tremor(0.3f + 0.7f * forca, 15, 12);
        }
    }
}
