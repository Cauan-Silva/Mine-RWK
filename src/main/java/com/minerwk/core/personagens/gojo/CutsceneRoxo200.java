package com.minerwk.core.personagens.gojo;

import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.camera.Coreografia.Quadro;
import com.minerwk.core.kit.Vetor;
import java.util.List;

/**
 * Cutscene do Roxo 200% (cap. 223): 80 ticks de encantamento e gestos, o disparo e a volta ao jogador.
 * Posições na base do Gojo a partir dos olhos: (lado, cima, frente). Ver o spec "Roxo 200 cutscene".
 */
public final class CutsceneRoxo200 {
    public static final String ID = "gojo_roxo_200";

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
            new Quadro(80, new Vetor(0, 0, 1.4), Vetor.ZERO, .82, 0),
            // Disparo: atrás do ombro, seguindo a massa.
            new Quadro(82, new Vetor(.75, .3, -1.9), new Vetor(0, 0, 8), 1.1, 0),
            new Quadro(95, new Vetor(.75, .3, -1.9), new Vetor(0, 0, 30), 1.15, 0),
            // Volta para os olhos do jogador.
            new Quadro(110, Vetor.ZERO, new Vetor(0, 0, 5), 1, 0)));

    public static final List<Palavra> PALAVRAS = List.of(
            new Palavra(2, 20, "cutscene.minerwk.gojo_roxo_200.1"),
            new Palavra(22, 40, "cutscene.minerwk.gojo_roxo_200.2"),
            new Palavra(42, 60, "cutscene.minerwk.gojo_roxo_200.3"),
            new Palavra(62, 80, "cutscene.minerwk.gojo_roxo_200.4"),
            new Palavra(81, 98, "cutscene.minerwk.gojo_roxo_200.5"));

    private CutsceneRoxo200() {
    }
}
