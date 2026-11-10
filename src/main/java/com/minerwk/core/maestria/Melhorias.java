package com.minerwk.core.maestria;

import com.minerwk.core.kit.Numeros;
import java.util.Map;

/** Os nós que um jogador comprou, vistos como fatores que os golpes aplicam. */
public final class Melhorias {
    public static final Melhorias NENHUMA = new Melhorias(Arvore.VAZIA, Map.of());

    private final Arvore arvore;
    private final Map<String, Integer> comprados;

    public Melhorias(Arvore arvore, Map<String, Integer> comprados) {
        this.arvore = arvore;
        this.comprados = Map.copyOf(comprados);
    }

    /** Nível comprado do nó, limitado ao máximo dele; 0 se o nó não existe. */
    public int nivel(String id) {
        return arvore.no(id).map(no -> Math.max(0, Math.min(no.niveis(), comprados.getOrDefault(id, 0)))).orElse(0);
    }

    private double fracao(String id) {
        return arvore.no(id).map(no -> no.porNivel() * nivel(id)).orElse(0.0);
    }

    /** Fator de um nó que aumenta: 1 sem o nó, 1,2 com dois níveis de 10%. */
    public double mais(String id) {
        return 1 + fracao(id);
    }

    /** Fator de um nó que diminui: 1 sem o nó, 0,8 com dois níveis de 10%. */
    public double menos(String id) {
        return 1 - fracao(id);
    }

    /** Os números da habilidade com as melhorias de carga e de recarga que apontam para ela. */
    public Numeros ajustar(String habilidade, Numeros n) {
        double carga = 1, recarga = 1;
        for (No no : arvore.nos()) {
            if (!no.alvo().equals(habilidade)) continue;
            if (no.tipo() == No.Tipo.CARGA) carga *= menos(no.id());
            else if (no.tipo() == No.Tipo.RECARGA) recarga *= menos(no.id());
        }
        if (carga == 1 && recarga == 1) return n;
        return new Numeros(escala(n.cargaMin(), carga), escala(n.cargaMax(), carga), n.disparo(), n.fim(), n.segurar(),
                n.custo(), escala(n.recarga(), recarga));
    }

    /** Um tempo em ticks vezes o fator; o que era positivo não vira zero. */
    public static int escala(int ticks, double fator) {
        return ticks <= 0 ? ticks : Math.max(1, (int) Math.round(ticks * fator));
    }
}
