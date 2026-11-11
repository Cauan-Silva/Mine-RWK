package com.minerwk.core.kit;

import java.util.Map;
import java.util.Optional;

/**
 * Uma transformação de um personagem (o Bankai do Ichigo): outro conjunto de habilidades nas mesmas teclas.
 * A forma base não é uma {@code Forma}: são as habilidades do próprio {@link Personagem}, com o id {@link #BASE}.
 */
public record Forma(String id, Map<Tecla, Habilidade> habilidades) {
    /** Id da forma base, a que o jogador tem ao escolher o personagem e ao entrar no jogo. */
    public static final String BASE = "base";

    public Forma {
        if (id == null || id.isBlank() || BASE.equals(id)) throw new IllegalArgumentException("id de forma inválido: " + id);
        habilidades = Map.copyOf(habilidades);
    }

    public Optional<Habilidade> naTecla(Tecla tecla) {
        return Optional.ofNullable(habilidades.get(tecla));
    }
}
