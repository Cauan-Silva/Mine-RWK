package com.minerwk.core.personagens.ichigo;

import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.camera.Coreografia.Quadro;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200.Palavra;
import com.minerwk.core.vfx.MugetsuIchigo;
import java.util.List;

/**
 * A cutscene do Mugetsu, uma parte só: os 6 s da carga (o reiatsu preto subindo, a forma final, o silêncio)
 * e o véu avançando depois do corte. Posições na base do Ichigo a partir dos olhos: (lado, cima, frente).
 * Os cortes de câmera caem nos tempos de {@link MugetsuIchigo}, os mesmos do som e dos visuais.
 */
public final class CutsceneMugetsu {
    public static final String ID = Ichigo.MUGETSU;
    /** O plano aberto do véu entra esses ticks depois do corte da lâmina. */
    public static final int PLANO_DO_VEU = MugetsuIchigo.CARGA + 3;
    private static final int T = MugetsuIchigo.TRANSFORMA, S = MugetsuIchigo.SILENCIO, C = MugetsuIchigo.CARGA;

    public static final Coreografia COREOGRAFIA = new Coreografia(List.of(
            // O reiatsu preto sobe: a câmera dá a volta nele, de corpo inteiro.
            new Quadro(0, new Vetor(2.4, .3, 2.8), new Vetor(0, -.4, 0), 1, 0),
            new Quadro(T - 2, new Vetor(-2.0, .1, 2.5), new Vetor(0, -.4, 0), .95, 0),
            // A forma final aparece: corte seco para baixo, olhando para cima, e sobe devagar.
            new Quadro(T, new Vetor(.7, -1.45, 2.3), new Vetor(0, .35, 0), 1.1, -3, true),
            new Quadro(60, new Vetor(.35, -1.2, 1.8), new Vetor(0, .45, 0), 1.05, 0),
            // O rosto, fechando até tudo se calar.
            new Quadro(63, new Vetor(0, .05, 1.7), Vetor.ZERO, .82, 0, true),
            new Quadro(S, new Vetor(0, .05, 1.2), Vetor.ZERO, .72, 0),
            // No silêncio: atrás do ombro, vendo para onde o corte vai sair.
            new Quadro(S + 3, new Vetor(-.95, .35, -2.3), new Vetor(0, 0, 6), 1, 0, true),
            new Quadro(C, new Vetor(-.75, .3, -1.9), new Vetor(0, .1, 8), .95, 0),
            // O corte: plano aberto, de lado e do alto, fora da faixa do véu (de dentro dele não se vê nada).
            new Quadro(PLANO_DO_VEU, new Vetor(10, 5, -5), new Vetor(0, 1, 16), 1.15, 0, true),
            new Quadro(C + MugetsuIchigo.TICKS + 6, new Vetor(12, 6.5, -3), new Vetor(0, 1, 26), 1.15, 0),
            // De volta ao Ichigo, já sem poderes.
            new Quadro(C + MugetsuIchigo.TICKS + 22, new Vetor(0, .3, -3.4), new Vetor(0, 0, 4), 1, 0)));

    /** O anúncio e o nome do golpe; o texto vem do idioma ({@code chave} e {@code chave.jp}). */
    public static final List<Palavra> PALAVRAS = List.of(
            new Palavra(64, S - 2, "cutscene.minerwk.ichigo_mugetsu.1"),
            new Palavra(S + 4, C + 4, "cutscene.minerwk.ichigo_mugetsu.2"));

    private CutsceneMugetsu() {
    }
}
