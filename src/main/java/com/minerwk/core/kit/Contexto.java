package com.minerwk.core.kit;

/** O que uma habilidade pode pedir ao jogo. A camada do Minecraft implementa; cresce conforme os personagens pedirem. */
public interface Contexto {
    /** Contexto que não faz nada, para quando não há jogo por perto (testes, interrupção sem contexto). */
    Contexto NULO = new Contexto() {
    };

    /** As melhorias da árvore que o jogador comprou; os golpes leem daqui o nível de cada nó. */
    default com.minerwk.core.maestria.Melhorias melhorias() {
        return com.minerwk.core.maestria.Melhorias.NENHUMA;
    }

    /** Causa {@code dano} a todas as criaturas num raio em volta do jogador, menos ele. */
    default void danoEmArea(double raio, float dano) {
    }

    /**
     * Como {@link #danoEmArea}, para cortes em sequência: o corte anterior não protege o alvo deste (no
     * Minecraft, quem acabou de apanhar fica meio segundo sem levar dano igual ou menor).
     */
    default void danoSeguidoEmArea(double raio, float dano) {
        danoEmArea(raio, dano);
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

    /** Os pés do jogador. Não é o mesmo que descer dos olhos: agachado, os olhos ficam mais baixos. */
    default Vetor pes() {
        return olhos().mais(new Vetor(0, -1.62, 0));
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

    /** Paralisa só o alvo marcado por {@code ticks} (as correntes do Cid); nada acontece se ele não existe mais. */
    default void paralisarAlvo(Object alvo, int ticks) {
    }

    /** Liga o Infinito do jogador por {@code ticks}: ataques corpo a corpo e projéteis não chegam. */
    default void infinito(int ticks) {
    }

    default void curar(float vida) {
    }

    /** Põe a vida do jogador em {@code vida} (até o máximo dele). */
    default void vida(float vida) {
    }

    /**
     * O inimigo vivo mais perto do jogador, a até {@code raio} blocos e à vista: monstros, quem o ataca e
     * outros jogadores, nunca animais nem aldeões (é para golpes que escolhem o alvo sozinhos). Devolve a mesma chave opaca
     * de {@link #marcarAlvo}, ou null.
     */
    default Object inimigoProximo(double raio) {
        return null;
    }

    /** Gasta do recurso do jogador; devolve falso (e não gasta) se não houver o bastante. */
    default boolean gastarRecurso(double quantidade) {
        return true;
    }

    /** Manda um sinal ao recurso do jogador (ver {@link Recurso#sinal}). */
    default void sinalAoRecurso(String sinal, int ticks) {
    }

    /** Se o jogador tem um efeito desse tipo em andamento. */
    default boolean efeitoAtivo(Class<? extends Efeito> tipo) {
        return false;
    }

    /** O id do golpe que o jogador está executando (da carga ao fim), ou vazio. */
    default String golpeEmAndamento() {
        return "";
    }

    /**
     * Como {@link #danoUmaVez}, mas o dano ignora a armadura do alvo. Devolve quantos foram feridos.
     */
    default int danoDiretoUmaVez(Vetor centro, double raio, float dano, java.util.Set<Object> atingidos) {
        return danoUmaVez(centro, raio, dano, atingidos);
    }

    /** A forma em que o jogador está. */
    default String forma() {
        return Forma.BASE;
    }

    /** Passa o jogador para outra forma do personagem. Devolve se mudou. */
    default boolean mudarForma(String forma) {
        return false;
    }

    /** Quanto o jogador tem do recurso agora. */
    default double recurso() {
        return Double.MAX_VALUE;
    }

    /** Põe uma habilidade do jogador em recarga por {@code ticks}, a partir de agora. */
    default void recarga(String habilidade, int ticks) {
    }

    /**
     * O ataque básico do jogador neste tick. Vale enquanto for pedido: quem tem arma própria pede todo tick
     * (a passiva), e o último pedido do tick ganha (um efeito pode trocar a arma por um tempo).
     */
    default void arma(Arma arma) {
    }

    /**
     * Leva o jogador em linha reta por até {@code distancia} blocos, parando antes de parede; com
     * {@code encarar}, ele chega olhando para esse ponto. Devolve onde os pés ficaram.
     */
    default Vetor deslocar(Vetor direcao, double distancia, Vetor encarar) {
        return Vetor.ZERO;
    }

    /** Se há bloco sólido entre os dois pontos (inimigos não contam: é para o que os atravessa). */
    default boolean colideBloco(Vetor de, Vetor ate) {
        return false;
    }

    /** Desfaz os projéteis dos outros dentro da esfera; devolve quantos. */
    default int desfazerProjeteis(Vetor centro, double raio) {
        return 0;
    }

    /**
     * Deixa um chamariz em {@code posicao} (os pés da imagem) por {@code ticks}: os mobs que miravam o
     * jogador largam dele e vão até lá. Não engana outros jogadores.
     */
    default void chamariz(Vetor posicao, int ticks) {
    }

    /** O jogador fica com Velocidade {@code nivel} (1 = I) por {@code ticks}; renova se já tinha. */
    default void velocidade(int nivel, int ticks) {
    }

    /** O custo de um golpe no próprio corpo: tira {@code dano} de vida sem armadura no meio e sem matar (para em 1). */
    default void recuo(float dano) {
    }

    /**
     * Um corte horizontal: fere quem está no arco de {@code graus} à frente de {@code direcao}, a até
     * {@code alcance} blocos e a até {@code altura} acima ou abaixo de {@code origem} (ver {@link Arco}).
     * Devolve quantos feriu.
     */
    default int danoEmArco(Vetor origem, Vetor direcao, double alcance, double graus, double altura, float dano) {
        return 0;
    }

    /** Prende o jogador ao chão (não anda nem pula) ou solta. Quem prende é quem solta. */
    default void firmar(boolean preso) {
    }

    /**
     * Cura por completo e tira os efeitos ruins do jogador e dos aliados a até {@code raio} blocos: os outros
     * jogadores e os animais domados. Não fere ninguém. Devolve quantos curou, contando o jogador.
     */
    default int restaurar(double raio) {
        return 0;
    }

    /** Acaba na hora os efeitos desse tipo do jogador, com o {@code aoAcabar} deles. */
    default void encerrarEfeito(Class<? extends Efeito> tipo) {
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
