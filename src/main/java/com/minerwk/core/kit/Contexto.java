package com.minerwk.core.kit;

/** O que uma habilidade pode pedir ao jogo. A camada do Minecraft implementa; cresce conforme os personagens pedirem. */
public interface Contexto {
    /** Contexto que não faz nada, para quando não há jogo por perto (testes, interrupção sem contexto). */
    Contexto NULO = new Contexto() {
    };

    /** Causa {@code dano} a todas as criaturas num raio em volta do jogador, menos ele. */
    default void danoEmArea(double raio, float dano) {
    }

    /** Para a animação do kit no jogador (a carga foi cortada). */
    default void pararAnimacao() {
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
     * Barreira esférica por {@code ticks} (regra de {@link Barreira}): quem está dentro não sai nem desce
     * abaixo de {@code chao}, quem está fora não entra. O próprio jogador conta como quem está dentro.
     */
    default void barreira(Vetor centro, double raio, double chao, int ticks) {
    }

    /**
     * Marca o alvo de um golpe: a criatura mais perto da linha da mira, a até {@code alcance} blocos e a
     * até {@code largura} da linha. Devolve uma chave opaca (a mesma que {@link #danoUmaVez} guarda em
     * {@code atingidos}) ou null.
     */
    default Object marcarAlvo(double alcance, double largura) {
        return null;
    }

    /** Centro do alvo marcado, ou null se ele não existe mais (morreu, saiu, mudou de dimensão). */
    default Vetor posicaoDe(Object alvo) {
        return null;
    }

    /** Segura o alvo marcado parado em {@code ponto} (centro) por {@code ticks}, sem gravidade nem IA. */
    default void suspender(Object alvo, Vetor ponto, int ticks) {
    }

    /** Fere só o alvo marcado. */
    default void ferir(Object alvo, float dano) {
    }

    /** O jogador vai voar por {@code ticks} (o cliente o move): sem dano de queda e sem ser chutado por voo. */
    default void voo(int ticks) {
    }

    /**
     * Começa a cutscene {@code id} só para o jogador (a partir dos olhos e do olhar de agora). Ela acaba
     * sozinha ou quando o visual de carga do golpe é encerrado.
     */
    default void cutscene(String id) {
    }

    /** Como {@link #cutscene(String)}, com a base dada (a parte 2 do R vira para o alvo, não para o olhar). */
    default void cutscene(String id, Vetor origem, Vetor direcao) {
    }

    /**
     * Mostra a cutscene {@code id} do jogador também para o alvo marcado, se for um jogador, como se ela
     * tivesse começado {@code passados} ticks atrás (ele entra no meio, junto com o Gojo).
     */
    default void cutsceneParaAlvo(Object alvo, String id, Vetor origem, Vetor direcao, int passados) {
    }

    /** Acaba na hora a cutscene do jogador (e a de quem a assiste). */
    default void encerrarCutscene() {
    }

    /**
     * O jogador desce do voo: se está no ar, toca a pose {@code animacao} e o voo (sem dano de queda)
     * passa a acabar quando ele pousar (ou cair na água, subir escada, planar...), ou em {@code ticksMax};
     * aí a pose para, se ainda for ela. Já no chão, o voo acaba na hora, sem pose.
     */
    default void descida(String animacao, int ticksMax) {
    }

    /** O golpe do jogador acertou alguém: a tela dele trava um instante (mais forte com {@code forca}). */
    default void acertou(float forca) {
    }

    /** Se há bloco sólido ou criatura no segmento. */
    default boolean colide(Vetor de, Vetor ate) {
        return false;
    }

    /** Apaga os blocos da esfera, se a regra do servidor deixar. */
    /**
     * Apaga os blocos entre as esferas de raio {@code de} (exclusive) e {@code ate} (inclusive): explosões
     * grandes crescem casca por casca. Pode ficar para os ticks seguintes se for muito bloco.
     */
    default void apagarCasca(Vetor centro, double de, double ate) {
    }

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
