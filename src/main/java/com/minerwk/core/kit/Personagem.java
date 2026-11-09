package com.minerwk.core.kit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Um personagem jogável: quem é, de que obra, o recurso dele, as habilidades por tecla e as passivas.
 * Recurso e passivas vêm de fábricas porque guardam estado de cada jogador; habilidades não guardam estado.
 */
public record Personagem(String id, String nome, String obra, Supplier<Recurso> recurso, Map<Tecla, Habilidade> habilidades,
                         List<Supplier<Passiva>> passivas) {
    public Personagem {
        habilidades = Map.copyOf(habilidades);
        passivas = List.copyOf(passivas);
    }

    public Personagem(String id, String nome, String obra, Supplier<Recurso> recurso, Map<Tecla, Habilidade> habilidades) {
        this(id, nome, obra, recurso, habilidades, List.of());
    }

    public Optional<Habilidade> naTecla(Tecla tecla) {
        return Optional.ofNullable(habilidades.get(tecla));
    }
}
