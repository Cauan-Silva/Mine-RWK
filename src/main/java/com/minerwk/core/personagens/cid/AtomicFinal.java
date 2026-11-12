package com.minerwk.core.personagens.cid;

import com.minerwk.core.kit.Vetor;

/**
 * A sequência do R do Cid, o I Am Atomic final: quatro socos que levam o alvo para cima, a passagem para o
 * espaço no quinto, mais quatro socos lá, as três palavras e o Atomic. Os tempos são em ticks desde o
 * disparo; o servidor, o cliente e os sons usam as mesmas contas. Sem classes do Minecraft.
 */
public final class AtomicFinal {
    /** O alvo tem de estar à frente, a até 6 blocos, e a até 1,5 da linha da mira. */
    public static final double ALCANCE = 6, LARGURA = 1.5;
    /** Quatro socos no mundo e quatro no espaço, um a cada 6 ticks, 2 de dano cada. */
    public static final int SOCOS = 4, ENTRE_SOCOS = 6;
    public static final float DANO_DO_SOCO = 2;
    /** A cada soco no mundo o alvo sobe 12 blocos, em {@link #TICKS_DA_SUBIDA} ticks. */
    public static final double SUBIDA = 12;
    public static final int TICKS_DA_SUBIDA = 5;
    /** O Cid surge ao lado do alvo estes ticks antes do soco seguinte, a esta distância dele. */
    public static final int ANTES_DO_SOCO = 2;
    public static final double AO_LADO = 1.6;
    /** O quinto soco: os dois saem do mundo. */
    public static final int PASSAGEM = SOCOS * ENTRE_SOCOS;
    /** No espaço, cada soco empurra o alvo estes blocos para a frente. */
    public static final double EMPURRAO = 2.5;
    /** O primeiro soco no espaço, um intervalo depois da chegada. */
    public static final int PRIMEIRO_NO_ESPACO = PASSAGEM + ENTRE_SOCOS;
    /** As três palavras: 40 ticks parados, começando um intervalo depois do último soco. */
    public static final int FALA = PRIMEIRO_NO_ESPACO + SOCOS * ENTRE_SOCOS, TICKS_DA_FALA = 40;
    public static final int ATOMIC = FALA + TICKS_DA_FALA;
    public static final float DANO = 44;
    /** O clarão roxo no espaço: cresce, toma a tela e some; só então os dois voltam. */
    public static final int TICKS_DO_CLARAO = 40;
    public static final int VOLTA = ATOMIC + TICKS_DO_CLARAO;
    /** No mundo de onde saíram: quem está a até 96 blocos vê a luz no céu por 3 s. */
    public static final double RAIO_DA_LUZ = 96;
    public static final int TICKS_DA_LUZ = 60;
    /** O risco roxo que quem ficou embaixo vê quando os dois somem para cima. */
    public static final int TICKS_DO_RISCO = 14;
    /** Depois da volta o golpe ainda dura estes ticks (a luz no céu vista do chão). */
    public static final int DEPOIS_DA_VOLTA = TICKS_DA_LUZ - TICKS_DO_CLARAO;
    public static final int FIM = VOLTA + DEPOIS_DA_VOLTA;

    private AtomicFinal() {
    }

    private static double suave(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    /** Se há soco no tick {@code t}: os quatro do mundo e os quatro do espaço (a passagem não fere). */
    public static boolean soco(int t) {
        if (t < 0 || t % ENTRE_SOCOS != 0) return false;
        return t < PASSAGEM || (t >= PRIMEIRO_NO_ESPACO && t < FALA);
    }

    /** Quanto o alvo já subiu no tick {@code t} da subida: 12 blocos por soco, em 5 ticks cada. */
    public static double alturaDoAlvo(double t) {
        if (t <= 0) return 0;
        t = Math.min(t, PASSAGEM);
        int k = (int) Math.min(SOCOS - 1, Math.floor(t / ENTRE_SOCOS));
        return SUBIDA * (k + suave((t - k * ENTRE_SOCOS) / TICKS_DA_SUBIDA));
    }

    /** A altura do Cid: ele não voa, surge ao lado do alvo pouco antes de cada soco. */
    public static double alturaDoCid(int t) {
        if (t < 0) return 0;
        int k = Math.min(SOCOS, (t + ANTES_DO_SOCO) / ENTRE_SOCOS);
        return SUBIDA * k;
    }

    /** No espaço: quanto o alvo já foi empurrado para a frente no tick {@code t} (desde o disparo). */
    public static double empurraoDoAlvo(double t) {
        double e = t - PRIMEIRO_NO_ESPACO;
        if (e <= 0) return 0;
        e = Math.min(e, SOCOS * ENTRE_SOCOS);
        int k = (int) Math.min(SOCOS - 1, Math.floor(e / ENTRE_SOCOS));
        return EMPURRAO * (k + suave((e - k * ENTRE_SOCOS) / TICKS_DA_SUBIDA));
    }

    /** No espaço: quanto o Cid já avançou atrás do alvo. */
    public static double avancoDoCid(int t) {
        int e = t - PRIMEIRO_NO_ESPACO;
        if (e < 0) return 0;
        return EMPURRAO * Math.min(SOCOS, (e + ANTES_DO_SOCO) / ENTRE_SOCOS);
    }

    /** O centro do alvo durante a subida, a partir de onde ele estava. */
    public static Vetor alvoNaSubida(Vetor centro, double t) {
        return centro.mais(new Vetor(0, alturaDoAlvo(t), 0));
    }

    /** Os pés do Cid durante a subida: no chão até o primeiro soco, depois ao lado do alvo, do lado de onde veio. */
    public static Vetor cidNaSubida(Vetor pes, Vetor centroDoAlvo, Vetor rumo, int t) {
        double h = alturaDoCid(t);
        if (h <= 0) return pes;
        return centroDoAlvo.menos(rumo.vezes(AO_LADO)).mais(new Vetor(0, h - .9, 0));
    }

    /** No espaço: o centro do alvo, à frente e um pouco acima dos pés do Cid na chegada. */
    public static Vetor alvoNoEspaco(Vetor chegada, Vetor rumo, double t) {
        return chegada.mais(rumo.vezes(AO_LADO + empurraoDoAlvo(t))).mais(new Vetor(0, .9, 0));
    }

    public static Vetor cidNoEspaco(Vetor chegada, Vetor rumo, int t) {
        return chegada.mais(rumo.vezes(avancoDoCid(t)));
    }

    /** Qual das três palavras está na tela no tick {@code t} (0 a 2), ou -1. */
    public static int palavra(double t) {
        if (t < FALA || t >= ATOMIC) return -1;
        return (int) Math.min(2, (t - FALA) * 3 / TICKS_DA_FALA);
    }

    /** O tamanho da aura roxa durante as palavras, de 0 a 1. */
    public static double aura(double t) {
        return suave((t - FALA) / TICKS_DA_FALA);
    }

    /** O raio do clarão roxo, em fração do máximo, {@code t} ticks depois do Atomic: estoura e cresce devagar. */
    public static double raioDoClarao(double t) {
        double s = Math.max(0, Math.min(1, t / TICKS_DO_CLARAO));
        return 1 - (1 - s) * (1 - s);
    }

    /** A opacidade do clarão roxo: estoura de uma vez e some antes de a tela clarear. */
    public static double clarao(double t) {
        if (t < 0 || t >= SOME_O_CLARAO) return 0;
        return Math.min(1, t / 2) * Math.min(1, (SOME_O_CLARAO - t) / 10);
    }

    /** Quando o clarão roxo já sumiu, e até quando a tela fica branca (a volta acontece por baixo dela). */
    public static final int SOME_O_CLARAO = TICKS_DO_CLARAO - 6, FIM_DO_BRANCO = TICKS_DO_CLARAO + 16;

    /** O branco na tela, {@code t} ticks depois do Atomic: só quando o clarão some; cobre a volta e abre no chão. */
    public static double branco(double t) {
        if (t < SOME_O_CLARAO - 8 || t >= FIM_DO_BRANCO) return 0;
        return Math.min(1, (t - (SOME_O_CLARAO - 8)) / 8) * Math.min(1, (FIM_DO_BRANCO - t) / 12);
    }

    /** O lampejo que esconde a passagem para o espaço, {@code t} ticks depois dela. */
    public static double lampejo(double t) {
        return t < 0 || t >= TICKS_DO_LAMPEJO ? 0 : 1 - t / TICKS_DO_LAMPEJO;
    }

    public static final int TICKS_DO_LAMPEJO = 8;

    /** A força da luz no céu do mundo, {@code t} ticks depois do Atomic. */
    public static double luz(double t) {
        if (t < 0 || t >= TICKS_DA_LUZ) return 0;
        return Math.min(1, t / 4) * Math.min(1, (TICKS_DA_LUZ - t) / 25);
    }
}
