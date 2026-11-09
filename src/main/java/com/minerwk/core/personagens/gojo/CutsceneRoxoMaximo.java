package com.minerwk.core.personagens.gojo;

import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.camera.Coreografia.Quadro;
import com.minerwk.core.kit.Vetor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parte 2 da cutscene do R, do disparo até a volta ao jogador: avanço, uppercut, subida, esferas, fusão,
 * a explosão num plano aberto de longe e a volta. Montada pelo avanço (que o servidor só sabe no
 * disparo); o id leva esse número. Base: olhos do Gojo, frente na horizontal para o alvo (ou para a mira,
 * sem alvo). Ver o spec "Cutscene do Vazio Roxo máximo".
 */
public final class CutsceneRoxoMaximo {
    public static final String PREFIXO = "gojo_roxo_maximo:";
    private static final String SEM_ALVO = "sem_alvo:";
    /** Ticks do plano aberto, do começo da explosão até a volta. */
    public static final int PLANO_ABERTO = 40;
    public static final int VOLTA = 15;
    /** Centro da explosão na base, com alvo: atrás do Gojo ({@link RoxoMaximo#centroFusao}). */
    public static final Vetor CENTRO_COM_ALVO = new Vetor(0, 0, -RoxoMaximo.ATRAS);
    /** De onde o plano aberto olha, em relação ao centro: de lado, acima e na diagonal (~70 blocos). */
    private static final Vetor LONGE = new Vetor(55, 25, 35);

    private CutsceneRoxoMaximo() {
    }

    public static String id(int avanco) {
        return PREFIXO + avanco;
    }

    public static String idSemAlvo(double distancia) {
        return PREFIXO + SEM_ALVO + Math.round(distancia);
    }

    public static boolean eh(String id) {
        return id.startsWith(PREFIXO);
    }

    /** A coreografia de um id da parte 2, ou vazio se o id não for dela. */
    public static Optional<Coreografia> deId(String id) {
        if (!eh(id)) return Optional.empty();
        String resto = id.substring(PREFIXO.length());
        boolean sem = resto.startsWith(SEM_ALVO);
        if (sem) resto = resto.substring(SEM_ALVO.length());
        try {
            int n = Integer.parseInt(resto);
            if (n < 0 || n > 1000) return Optional.empty();
            return Optional.of(sem ? semAlvo(n) : coreografia(Math.min(n, RoxoMaximo.AVANCO_MAX)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Com alvo, avançando {@code avanco} ticks. */
    public static Coreografia coreografia(int avanco) {
        int a = avanco;
        List<Quadro> q = new ArrayList<>();
        // Avanço: atrás do ombro, baixa, o FOV abrindo com a velocidade.
        q.add(new Quadro(0, new Vetor(.8, .2, -2.2), new Vetor(0, 0, 6), 1, 0));
        if (a > 0) q.add(new Quadro(a, new Vetor(.9, .1, -2), new Vetor(0, 0, 5), 1.25, 0));
        // Uppercut: de baixo e de lado, perto do soco, com tranco.
        q.add(new Quadro(a + 2, new Vetor(2.2, -1.6, 2.4), new Vetor(0, .5, 1.5), .85, -8));
        q.add(new Quadro(a + 6, new Vetor(2.4, -1.2, 2.2), new Vetor(0, 1.5, 1.5), .9, -4));
        // Subida: de lado, os dois subindo juntos.
        q.add(new Quadro(RoxoMaximo.inicioEsferas(a), new Vetor(4.5, .5, .8), new Vetor(0, 0, 1), 1, 0));
        // Esferas: de frente, um pouco acima, as mãos abrindo e as esferas indo para trás dele.
        q.add(new Quadro(RoxoMaximo.inicioEsferas(a) + 2, new Vetor(.3, 1, 4), Vetor.ZERO, 1, 0));
        q.add(new Quadro(RoxoMaximo.inicioFusao(a), new Vetor(.3, 1.2, 4.6), new Vetor(0, 0, -1), 1.05, 0));
        // Fusão: close no rosto, o núcleo crescendo atrás.
        q.add(new Quadro(RoxoMaximo.inicioFusao(a) + 2, new Vetor(0, .1, 1.6), CENTRO_COM_ALVO, .85, 0));
        q.add(new Quadro(RoxoMaximo.inicioExplosao(a), new Vetor(0, .05, 1.3), CENTRO_COM_ALVO, .8, 0));
        explosaoEVolta(q, RoxoMaximo.inicioExplosao(a), CENTRO_COM_ALVO);
        return new Coreografia(q);
    }

    /** Sem alvo: as esferas vão até a mira, a {@code distancia} blocos à frente. */
    public static Coreografia semAlvo(double distancia) {
        Vetor centro = new Vetor(0, 0, distancia);
        int viagem = RoxoMaximo.TICKS_ESFERAS_SEM_ALVO;
        int explosao = viagem + RoxoMaximo.TICKS_FUSAO;
        List<Quadro> q = new ArrayList<>();
        q.add(new Quadro(0, new Vetor(.8, .3, -2.2), new Vetor(0, 0, 8), 1, 0));
        q.add(new Quadro(viagem, new Vetor(.8, .3, -2), centro, .9, 0));
        q.add(new Quadro(explosao, new Vetor(.8, .3, -2), centro, .75, 0));
        explosaoEVolta(q, explosao, centro);
        return new Coreografia(q);
    }

    /** Corte seco para o plano aberto parado olhando para {@code centro}, e a volta suave aos olhos. */
    private static void explosaoEVolta(List<Quadro> q, int explosao, Vetor centro) {
        Vetor longe = centro.mais(LONGE);
        q.add(new Quadro(explosao + 1, longe, centro, 1, 0, true));
        q.add(new Quadro(explosao + PLANO_ABERTO, longe, centro, 1, 0));
        q.add(new Quadro(explosao + PLANO_ABERTO + VOLTA, Vetor.ZERO, new Vetor(0, 0, 5), 1, 0));
    }
}
