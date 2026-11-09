package com.minerwk.core.kit;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lista dos personagens do mod, preenchida no início do jogo. */
public final class Personagens {
    /** Id do personagem de teste, escondido da escolha quando há personagens reais. */
    public static final String TESTE = "teste";

    private static final Map<String, Personagem> PERSONAGENS = new LinkedHashMap<>();

    private Personagens() {
    }

    public static synchronized void registrar(Personagem personagem) {
        if (PERSONAGENS.putIfAbsent(personagem.id(), personagem) != null) {
            throw new IllegalArgumentException("personagem repetido: " + personagem.id());
        }
    }

    public static synchronized Optional<Personagem> porId(String id) {
        return Optional.ofNullable(PERSONAGENS.get(id));
    }

    /** Todos, na ordem de registro. */
    public static synchronized Collection<Personagem> todos() {
        return List.copyOf(PERSONAGENS.values());
    }

    /** Os que aparecem na tela de escolha: o de teste só aparece se não houver outro. */
    public static synchronized Collection<Personagem> escolhiveis() {
        List<Personagem> reais = PERSONAGENS.values().stream().filter(p -> !p.id().equals(TESTE)).toList();
        return reais.isEmpty() ? List.copyOf(PERSONAGENS.values()) : reais;
    }

    static synchronized void limparParaTestes() {
        PERSONAGENS.clear();
    }
}
