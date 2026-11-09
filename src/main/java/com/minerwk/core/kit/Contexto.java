package com.minerwk.core.kit;

/** O que uma habilidade pode pedir ao jogo. A camada do Minecraft implementa; cresce conforme os personagens pedirem. */
public interface Contexto {
    /** Causa {@code dano} a todas as criaturas num raio em volta do jogador, menos ele. */
    default void danoEmArea(double raio, float dano) {
    }

    /**
     * Toca animação, efeito e som no jogador, para ele e para quem está perto.
     * Os ids são do mod ({@code gojo_azul} vira {@code minerwk:gojo_azul}); vazio = nada.
     */
    default void apresentacao(String animacao, String efeito, String som) {
    }
}
