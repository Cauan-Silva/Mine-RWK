package com.minerwk.core.kit;

/**
 * O recurso que as habilidades de um personagem gastam (energia amaldiçoada, reiatsu, fôlego...).
 * Cada personagem tem a sua implementação, com regras próprias de ganho em {@link #tick}.
 */
public interface Recurso {
    double atual();

    double maximo();

    boolean pode(double custo);

    /** @throws IllegalStateException se não houver o suficiente */
    void gastar(double custo);

    /** Um tick de jogo: aqui o recurso enche, esvazia ou reage ao que o jogador fez. */
    void tick(EstadoDoJogador estado);

    /** Valor para salvar nos dados do jogador. */
    double salvar();

    /** Restaura um valor salvo, limitado a [0, máximo]. */
    void carregar(double valor);
}
