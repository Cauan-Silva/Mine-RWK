package com.minerwk.core.kit;

/** O que o jogador fez neste tick, para os recursos reagirem. */
public record EstadoDoJogador(boolean parado, boolean apanhou, boolean causouDano) {
}
