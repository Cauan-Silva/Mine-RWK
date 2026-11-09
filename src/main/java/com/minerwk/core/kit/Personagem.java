package com.minerwk.core.kit;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Um personagem jogável: quem é, de que obra, o recurso dele e as habilidades por tecla. */
public record Personagem(String id, String nome, String obra, Supplier<Recurso> recurso, Map<Tecla, Habilidade> habilidades) {
    public Personagem {
        habilidades = Map.copyOf(habilidades);
    }

    public Optional<Habilidade> naTecla(Tecla tecla) {
        return Optional.ofNullable(habilidades.get(tecla));
    }
}
