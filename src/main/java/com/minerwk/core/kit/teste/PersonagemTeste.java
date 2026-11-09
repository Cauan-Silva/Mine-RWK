package com.minerwk.core.kit.teste;

import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Habilidade;
import com.minerwk.core.kit.Numeros;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.Tecla;
import java.util.Map;

/** Personagem para provar o núcleo antes do Gojo: um golpe instantâneo (Z) e um de segurar (X). */
public final class PersonagemTeste {
    private PersonagemTeste() {
    }

    public static Personagem criar() {
        return new Personagem(Personagens.TESTE, "Personagem de teste", "Mine-RWK", RecursoFolego::new,
                Map.of(Tecla.Z, new Golpe(), Tecla.X, new Carga()));
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
