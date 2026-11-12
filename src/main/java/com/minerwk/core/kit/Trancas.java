package com.minerwk.core.kit;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * O que um personagem só pode usar depois de despertar. Vale por habilidade e não por tecla: a mesma tecla
 * pode ser livre numa forma e trancada em outra.
 *
 * @param habilidades os ids das habilidades trancadas
 * @param passivas    se as passivas também só rodam depois do despertar
 */
public record Trancas(Set<String> habilidades, boolean passivas) {
    public static final Trancas NENHUMA = new Trancas(Set.of(), false);

    public Trancas {
        habilidades = Set.copyOf(habilidades);
    }

    /** Tranca as habilidades que estão nessas teclas de um conjunto; tecla sem habilidade é ignorada. */
    public static Trancas dasTeclas(Map<Tecla, Habilidade> kit, Set<Tecla> teclas, boolean passivas) {
        Set<String> ids = new HashSet<>();
        for (Tecla t : teclas) {
            Habilidade h = kit.get(t);
            if (h != null) ids.add(h.id());
        }
        return new Trancas(ids, passivas);
    }
}
