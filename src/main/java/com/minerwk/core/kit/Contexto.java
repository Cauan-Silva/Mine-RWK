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

    /** Treme a tela dos jogadores a até {@code raio} blocos; mais longe, mais fraco. */
    default void tremor(float forca, int duracao, double raio) {
    }

    // Os métodos abaixo agem em volta de um ponto e nunca afetam o próprio jogador.

    /** Posição dos olhos do jogador. */
    default Vetor olhos() {
        return Vetor.ZERO;
    }

    /** Para onde o jogador olha, com comprimento 1. */
    default Vetor olhar() {
        return new Vetor(0, 0, 1);
    }

    /** O ponto a {@code distancia} blocos na direção do olhar, ou antes, no primeiro bloco sólido. */
    default Vetor alvoAte(double distancia) {
        return olhos().mais(olhar().vezes(distancia));
    }

    /** Puxa criaturas, itens e projéteis para o centro; {@code forca} em blocos por tick. */
    default void puxar(Vetor centro, double raio, double forca) {
    }

    /** Empurra para longe do centro; {@code forca} em blocos por tick. */
    default void empurrar(Vetor centro, double raio, double forca) {
    }

    default void danoEm(Vetor centro, double raio, float dano) {
    }

    /** Se há bloco sólido ou criatura no segmento. */
    default boolean colide(Vetor de, Vetor ate) {
        return false;
    }

    /** Apaga os blocos da esfera, se a regra do servidor deixar. */
    default void apagarBlocos(Vetor centro, double raio) {
    }

    /** Paralisa quem está no raio por {@code ticks}: não anda, não ataca, não usa golpes. */
    default void paralisar(Vetor centro, double raio, int ticks) {
    }

    /** Liga o Infinito do jogador por {@code ticks}: ataques corpo a corpo e projéteis não chegam. */
    default void infinito(int ticks) {
    }

    default void curar(float vida) {
    }

    /** Gasta do recurso do jogador; devolve falso (e não gasta) se não houver o bastante. */
    default boolean gastarRecurso(double quantidade) {
        return true;
    }

    /** Começa um efeito solto no kit do jogador. */
    default void criarEfeito(Efeito efeito) {
    }

    /** Partículas vanilla, provisórias até os VFX; {@code tipo} é o id vanilla (ex.: "portal"). */
    default void particulas(String tipo, Vetor ponto, int quantidade) {
    }
}
