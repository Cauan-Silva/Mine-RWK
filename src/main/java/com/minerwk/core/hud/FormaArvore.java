package com.minerwk.core.hud;

import com.minerwk.core.maestria.No;
import java.util.List;
import java.util.Map;

/**
 * Como a árvore de habilidades de um personagem é desenhada: os galhos, cada um com o ícone do golpe que ele
 * melhora e os nós em fila. Só a forma; as regras de compra continuam na {@code Arvore}. O Despertar fica no
 * tronco e não entra em galho nenhum.
 */
public record FormaArvore(List<Galho> galhos) {
    /** O selo no canto do ícone do nó, que diz o que ele melhora. */
    public enum Selo { FORCA, RECARGA, CARGA, DURACAO, RECURSO }

    /**
     * Um galho: {@code icone} é o nome de um PNG de {@code textures/gui/icones}; {@code selos} troca o selo dos nós
     * cujo selo não é o do tipo (duração, recurso).
     */
    public record Galho(String id, String icone, boolean depoisDoDespertar, List<String> nos, Map<String, Selo> selos) {
        public static Galho antes(String id, String icone, List<String> nos, Map<String, Selo> selos) {
            return new Galho(id, icone, false, nos, selos);
        }

        public static Galho depois(String id, String icone, List<String> nos, Map<String, Selo> selos) {
            return new Galho(id, icone, true, nos, selos);
        }
    }

    public List<Galho> antes() {
        return galhos.stream().filter(g -> !g.depoisDoDespertar()).toList();
    }

    public List<Galho> depois() {
        return galhos.stream().filter(Galho::depoisDoDespertar).toList();
    }

    /** O selo do nó: o do mapa do galho dele ou, sem ele, o do tipo. */
    public Selo selo(No no) {
        for (Galho g : galhos) {
            Selo s = g.selos().get(no.id());
            if (s != null) return s;
        }
        return switch (no.tipo()) {
            case RECARGA -> Selo.RECARGA;
            case CARGA -> Selo.CARGA;
            default -> Selo.FORCA;
        };
    }
}
