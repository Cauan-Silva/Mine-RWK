package com.minerwk.core.kit;

import com.minerwk.core.maestria.Arvore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Um personagem jogável: quem é, de que obra, o recurso dele, as habilidades por tecla e as passivas.
 * Recurso e passivas vêm de fábricas porque guardam estado de cada jogador; habilidades não guardam estado.
 * A árvore de habilidades é a das melhorias que a maestria compra (vazia se o personagem não tem).
 * As trancas dizem o que só vale depois do despertar. As transformações são outros conjuntos de habilidades
 * nas mesmas teclas; {@code habilidades} é o da forma base.
 */
public record Personagem(String id, String nome, String obra, Supplier<Recurso> recurso, Map<Tecla, Habilidade> habilidades,
                         List<Supplier<Passiva>> passivas, Arvore arvore, Trancas trancas, List<Forma> transformacoes) {
    public Personagem {
        habilidades = Map.copyOf(habilidades);
        passivas = List.copyOf(passivas);
        transformacoes = List.copyOf(transformacoes);
        if (transformacoes.stream().map(Forma::id).distinct().count() != transformacoes.size()) {
            throw new IllegalArgumentException("formas com o mesmo id em " + id);
        }
    }

    public Personagem(String id, String nome, String obra, Supplier<Recurso> recurso, Map<Tecla, Habilidade> habilidades,
                      List<Supplier<Passiva>> passivas, Arvore arvore, Trancas trancas) {
        this(id, nome, obra, recurso, habilidades, passivas, arvore, trancas, List.of());
    }

    public Personagem(String id, String nome, String obra, Supplier<Recurso> recurso, Map<Tecla, Habilidade> habilidades) {
        this(id, nome, obra, recurso, habilidades, List.of());
    }

    public Personagem(String id, String nome, String obra, Supplier<Recurso> recurso, Map<Tecla, Habilidade> habilidades,
                      List<Supplier<Passiva>> passivas) {
        this(id, nome, obra, recurso, habilidades, passivas, Arvore.VAZIA);
    }

    public Personagem(String id, String nome, String obra, Supplier<Recurso> recurso, Map<Tecla, Habilidade> habilidades,
                      List<Supplier<Passiva>> passivas, Arvore arvore) {
        this(id, nome, obra, recurso, habilidades, passivas, arvore, Trancas.NENHUMA);
    }

    /** A habilidade da tecla na forma base. */
    public Optional<Habilidade> naTecla(Tecla tecla) {
        return Optional.ofNullable(habilidades.get(tecla));
    }

    /** A habilidade da tecla numa forma ({@link Forma#BASE} ou o id de uma transformação). */
    public Optional<Habilidade> naTecla(String forma, Tecla tecla) {
        if (Forma.BASE.equals(forma)) return naTecla(tecla);
        return transformacoes.stream().filter(f -> f.id().equals(forma)).findFirst().flatMap(f -> f.naTecla(tecla));
    }

    public boolean temForma(String forma) {
        return Forma.BASE.equals(forma) || transformacoes.stream().anyMatch(f -> f.id().equals(forma));
    }

    /** As habilidades de todas as formas, a base primeiro. */
    public List<Habilidade> todasAsHabilidades() {
        List<Habilidade> todas = new java.util.ArrayList<>(habilidades.values());
        for (Forma f : transformacoes) todas.addAll(f.habilidades().values());
        // As variações de quem aperta agachado, logo depois das de pé.
        for (Habilidade h : List.copyOf(todas)) {
            if (h.agachado() != h && !todas.contains(h.agachado())) todas.add(h.agachado());
        }
        return todas;
    }
}
