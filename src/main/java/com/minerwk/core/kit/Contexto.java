package com.minerwk.core.kit;

/** O que uma habilidade pode pedir ao jogo. A camada do Minecraft implementa; cresce conforme os personagens pedirem. */
public interface Contexto {
    /** Contexto que não faz nada, para quando não há jogo por perto (testes, interrupção sem contexto). */
    Contexto NULO = new Contexto() {
    };

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

    /**
     * Fere quem está no raio e ainda não está em {@code atingidos} (e anota quem feriu ali).
     * Para golpes que passam vários ticks pelo mesmo alvo. Devolve quantos foram feridos agora.
     */
    default int danoUmaVez(Vetor centro, double raio, float dano, java.util.Set<Object> atingidos) {
        return 0;
    }

    /**
     * Quem está no raio para no ar por {@code congelar} ticks e depois sai voando com {@code forca}
     * (ver {@link Arremessos}). Com {@code eixo} não nulo, sai para o lado do caminho. Quem já está
     * sendo arremessado é ignorado.
     */
    default void arremessar(Vetor centro, double raio, double forca, int congelar, Vetor eixo) {
    }

    /**
     * Arranca até {@code maximo} blocos leves (dureza até {@code durezaMax}) no raio, que viram blocos
     * caindo, se a regra do servidor deixar. Devolve quantos arrancou.
     */
    default int arrancarBlocos(Vetor centro, double raio, int maximo, float durezaMax) {
        return 0;
    }

    /**
     * Chão sólido e invisível por {@code ticks}, no nível dos pés {@code pes}, num disco de {@code raio}:
     * só onde há ar. Some sozinho no fim.
     */
    default void pisoSolido(Vetor pes, double raio, int ticks) {
    }

    /** O golpe do jogador acertou alguém: a tela dele trava um instante (mais forte com {@code forca}). */
    default void acertou(float forca) {
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

    /**
     * Começa um visual de golpe para o jogador e quem o vê. O cliente desenha sozinho a partir da origem,
     * da direção e da duração (ver {@code core/vfx}).
     */
    default void visual(String tipo, Vetor origem, Vetor direcao, float forca, int duracao) {
    }

    /** Acaba um visual antes da hora, ou no ponto em que terminou (ex.: onde a esfera bateu). */
    default void visualFim(String tipo, Vetor ponto) {
    }

    /** Partículas vanilla, provisórias até os VFX; {@code tipo} é o id vanilla (ex.: "portal"). */
    default void particulas(String tipo, Vetor ponto, int quantidade) {
    }
}
