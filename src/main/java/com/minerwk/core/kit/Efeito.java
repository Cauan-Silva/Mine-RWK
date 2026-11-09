package com.minerwk.core.kit;

/**
 * Algo que uma habilidade deixa no mundo e que dura sozinho (campo do Azul, Infinito, domínio...).
 * Enquanto dura, o jogador pode usar outras habilidades. O {@link KitDoJogador} roda um tick por vez.
 */
public interface Efeito {
    /** Ticks que o efeito dura, no máximo. */
    int duracao();

    /** @param t tick do efeito, começando em 0 */
    void tick(Contexto contexto, int t);

    /** Verdadeiro para acabar antes da duração (ex.: o Vermelho colidiu). */
    default boolean encerrado() {
        return false;
    }

    /** Roda uma vez quando acaba (pela duração ou por {@link #encerrado()}); não roda se o jogador morreu ou saiu. */
    default void aoAcabar(Contexto contexto) {
    }
}
