package com.minerwk.core.kit;

/**
 * O ataque básico (o clique) de um personagem que luta com arma própria: vale no lugar do item que o
 * jogador segura.
 *
 * @param dano       dano de um golpe com a recarga do ataque cheia
 * @param velocidade ataques por segundo (a mão vazia do Minecraft faz 4; uma espada, 1,6)
 * @param alcance    blocos que o golpe alcança; 0 = o alcance normal do jogador
 */
public record Arma(float dano, float velocidade, float alcance) {
    /** Sem a arma na mão: o soco do Minecraft. */
    public static final Arma MAO = new Arma(1, 4, 0);
}
