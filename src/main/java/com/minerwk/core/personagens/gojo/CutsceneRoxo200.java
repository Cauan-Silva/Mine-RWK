package com.minerwk.core.personagens.gojo;

import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.camera.Coreografia.Quadro;
import com.minerwk.core.kit.Vetor;
import java.util.List;

/**
 * Parte 1 da cutscene do R (cap. 223): 80 ticks de encantamento e gestos. No disparo, o servidor manda a
 * parte 2 ({@link CutsceneRoxoMaximo}), que emenda sem corte; até ela chegar, o último plano fica parado.
 * Posições na base do Gojo a partir dos olhos: (lado, cima, frente). Ver o spec "Cutscene do Vazio Roxo máximo".
 */
public final class CutsceneRoxo200 {
    public static final String ID = "gojo_roxo_200";
    /** Tick do disparo. */
    public static final int FIM_ENCANTAMENTO = 80;
    /** Quanto o último plano espera a parte 2 (a rede e o tick do disparo). */
    public static final int ESPERA = 20;

    /** Uma palavra do encantamento na tela, de {@code inicio} a {@code fim}; o texto vem do idioma ({@code chave}). */
    public record Palavra(int inicio, int fim, String chave) {
    }

    public static final Coreografia COREOGRAFIA = new Coreografia(List.of(
            // Nove Cordas: dá a volta no Gojo, meio corpo.
            new Quadro(0, new Vetor(2.6, -.2, 2.2), new Vetor(0, -.3, 0), 1, 0),
            new Quadro(20, new Vetor(-2.6, -.1, 2.2), new Vetor(0, -.3, 0), .95, 0),
            // Luz Polarizada: close nas mãos.
            new Quadro(23, new Vetor(.7, -.5, 1.3), new Vetor(0, -.65, .45), .8, 0),
            new Quadro(40, new Vetor(.3, -.45, 1.05), new Vetor(0, -.65, .45), .72, 4),
            // Corvo e Declaração: de baixo para cima, o céu escurecendo.
            new Quadro(43, new Vetor(.2, -1.55, 2.6), new Vetor(0, .5, 0), 1.1, -3),
            new Quadro(60, new Vetor(.6, -1.35, 3.1), new Vetor(0, .8, 0), 1.15, 0),
            // Entre a Frente e o Verso: de frente para o rosto, aproximando.
            new Quadro(63, new Vetor(0, 0, 3.6), Vetor.ZERO, 1, 0),
            new Quadro(FIM_ENCANTAMENTO, new Vetor(0, 0, 1.4), Vetor.ZERO, .82, 0),
            new Quadro(FIM_ENCANTAMENTO + ESPERA, new Vetor(0, 0, 1.4), Vetor.ZERO, .82, 0)));

    public static final List<Palavra> PALAVRAS = List.of(
            new Palavra(2, 20, "cutscene.minerwk.gojo_roxo_200.1"),
            new Palavra(22, 40, "cutscene.minerwk.gojo_roxo_200.2"),
            new Palavra(42, 60, "cutscene.minerwk.gojo_roxo_200.3"),
            new Palavra(62, 80, "cutscene.minerwk.gojo_roxo_200.4"));

    /** O nome do golpe, no começo da parte 2. */
    public static final List<Palavra> PALAVRAS_DISPARO = List.of(new Palavra(1, 18, "cutscene.minerwk.gojo_roxo_200.5"));

    private CutsceneRoxo200() {
    }
}
