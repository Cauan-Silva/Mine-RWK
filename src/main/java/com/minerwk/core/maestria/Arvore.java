package com.minerwk.core.maestria;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A árvore de habilidades de um personagem: os nós e as regras de compra. Sem classes do Minecraft. */
public final class Arvore {
    public static final Arvore VAZIA = new Arvore(List.of());

    /** O que aconteceu numa tentativa de compra. */
    public enum Resultado { OK, NO_DESCONHECIDO, NIVEL_MAXIMO, SEM_PONTOS, MAESTRIA_BAIXA, NAO_DESPERTOU }

    private final Map<String, No> nos = new LinkedHashMap<>();

    public Arvore(List<No> nos) {
        for (No no : nos) {
            if (this.nos.putIfAbsent(no.id(), no) != null) throw new IllegalArgumentException("nó repetido: " + no.id());
        }
    }

    /** Os nós, na ordem em que foram declarados. */
    public List<No> nos() {
        return List.copyOf(nos.values());
    }

    public Optional<No> no(String id) {
        return Optional.ofNullable(nos.get(id));
    }

    /** Pontos já gastos; ids que não são desta árvore (de outra versão do mod) não contam. */
    public int gasto(Map<String, Integer> comprados) {
        int soma = 0;
        for (No no : nos.values()) soma += no.custoAte(comprados.getOrDefault(no.id(), 0));
        return soma;
    }

    public int pontosLivres(Map<String, Integer> comprados, int nivelDeMaestria) {
        return Math.max(0, Maestria.pontos(nivelDeMaestria) - gasto(comprados));
    }

    /** Se dá para subir um nível do nó, sem mudar nada. */
    public Resultado podeComprar(Map<String, Integer> comprados, String id, int nivelDeMaestria, boolean despertou) {
        No no = nos.get(id);
        if (no == null) return Resultado.NO_DESCONHECIDO;
        int atual = comprados.getOrDefault(id, 0);
        if (atual >= no.niveis()) return Resultado.NIVEL_MAXIMO;
        if (no.depoisDoDespertar() && !despertou) return Resultado.NAO_DESPERTOU;
        if (nivelDeMaestria < no.nivelMinimo()) return Resultado.MAESTRIA_BAIXA;
        if (no.custoDoProximo(atual) > pontosLivres(comprados, nivelDeMaestria)) return Resultado.SEM_PONTOS;
        return Resultado.OK;
    }

    /** Sobe um nível do nó em {@code comprados}, se as regras deixarem. */
    public Resultado comprar(Map<String, Integer> comprados, String id, int nivelDeMaestria, boolean despertou) {
        Resultado r = podeComprar(comprados, id, nivelDeMaestria, despertou);
        if (r == Resultado.OK) comprados.merge(id, 1, Integer::sum);
        return r;
    }
}
