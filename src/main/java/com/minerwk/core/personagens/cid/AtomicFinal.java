package com.minerwk.core.personagens.cid;

import com.minerwk.core.kit.Vetor;

/**
 * A sequência do R do Cid, o I Am Atomic final: quatro socos que levam o alvo para cima, a passagem para o
 * espaço no quinto, mais quatro socos lá (o último arremessa o alvo para longe), as duas primeiras palavras,
 * a carga em silêncio, a terceira palavra e o Atomic. Os tempos são em ticks desde o disparo; o servidor, o
 * cliente e os sons usam as mesmas contas. Sem classes do Minecraft.
 */
public final class AtomicFinal {
    /** O alvo tem de estar à frente, a até 6 blocos, e a até 1,5 da linha da mira. */
    public static final double ALCANCE = 6, LARGURA = 1.5;
    /** Quatro socos no mundo e quatro no espaço, um a cada 12 ticks, 2 de dano cada. */
    public static final int SOCOS = 4, ENTRE_SOCOS = 12;
    public static final float DANO_DO_SOCO = 2;
    /**
     * O peso do soco: o alvo fica {@link #PARADA} ticks parado no ponto do golpe (a parada de impacto) e só
     * então é arremessado, 12 blocos para cima em {@link #TICKS_DA_SUBIDA} ticks, rápido no começo.
     */
    public static final double SUBIDA = 12;
    public static final int PARADA = 3, TICKS_DA_SUBIDA = 7;
    /** O Cid surge ao lado do alvo estes ticks antes do soco seguinte, a esta distância dele. */
    public static final int ANTES_DO_SOCO = 3;
    public static final double AO_LADO = 1.6;
    /** O quinto soco: os dois saem do mundo. */
    public static final int PASSAGEM = SOCOS * ENTRE_SOCOS;
    /** A chegada: o espaço aparece inteiro antes do primeiro soco de lá. */
    public static final int TICKS_DA_CHEGADA = 16;
    public static final int PRIMEIRO_NO_ESPACO = PASSAGEM + TICKS_DA_CHEGADA;
    /** No espaço, cada soco empurra o alvo estes blocos para a frente; o último o arremessa para longe. */
    public static final double EMPURRAO = 2.5, ARREMESSO = 9;
    public static final int TICKS_DO_ARREMESSO = 9;
    public static final int ULTIMO_SOCO = PRIMEIRO_NO_ESPACO + (SOCOS - 1) * ENTRE_SOCOS;
    /**
     * As palavras, começando um intervalo depois do último soco: "I" e "am", cada uma {@link #TICKS_DA_PALAVRA}
     * ticks na tela com um silêncio entre elas; a carga (a magia é sugada para a espada e o som some); e
     * "atomic", dita no silêncio, até o estouro.
     */
    public static final int FALA = ULTIMO_SOCO + ENTRE_SOCOS;
    public static final int TICKS_DA_PALAVRA = 24, ENTRE_PALAVRAS = 28;
    public static final int CARGA = FALA + 2 * ENTRE_PALAVRAS, TICKS_DA_CARGA = 30;
    public static final int TERCEIRA = CARGA + TICKS_DA_CARGA;
    public static final int ATOMIC = TERCEIRA + TICKS_DA_PALAVRA;
    public static final int TICKS_DA_FALA = ATOMIC - FALA;
    public static final float DANO = 44;
    /** A esfera de luz roxa no espaço: nasce pequena, cresce devagar até tomar a tela e some; só então os dois voltam. */
    public static final int TICKS_DO_CLARAO = 70;
    public static final int VOLTA = ATOMIC + TICKS_DO_CLARAO;
    /** No mundo de onde saíram: quem está a até 96 blocos vê a luz no céu por 6 s. */
    public static final double RAIO_DA_LUZ = 96;
    public static final int TICKS_DA_LUZ = 120;
    /** O risco roxo que quem ficou embaixo vê quando os dois somem para cima. */
    public static final int TICKS_DO_RISCO = 14;
    /** A onda de choque na volta: empurra quem está a até 10 blocos do ponto de partida, sem ferir nem quebrar nada. */
    public static final double RAIO_DA_ONDA = 10, FORCA_DA_ONDA = 1.1;
    public static final int TICKS_DA_ONDA = 24;
    /** Ela chega ao chão estes ticks depois da volta, quando o branco da tela já abriu. */
    public static final int ATRASO_DA_ONDA = 16;
    /** Depois da volta o golpe ainda dura estes ticks (a luz no céu vista do chão). */
    public static final int DEPOIS_DA_VOLTA = TICKS_DA_LUZ - TICKS_DO_CLARAO;
    public static final int FIM = VOLTA + DEPOIS_DA_VOLTA;

    private AtomicFinal() {
    }

    private static double suave(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    /** O arremesso: sai de uma vez e vai freando. */
    private static double arremesso(double x) {
        x = Math.max(0, Math.min(1, x));
        return 1 - (1 - x) * (1 - x) * (1 - x);
    }

    /** Se há soco no tick {@code t}: os quatro do mundo e os quatro do espaço (a passagem não fere). */
    public static boolean soco(int t) {
        if (t < 0) return false;
        if (t < PASSAGEM) return t % ENTRE_SOCOS == 0;
        return t >= PRIMEIRO_NO_ESPACO && t <= ULTIMO_SOCO && (t - PRIMEIRO_NO_ESPACO) % ENTRE_SOCOS == 0;
    }

    /** Quanto o alvo já subiu no tick {@code t} da subida: 12 blocos por soco, depois da parada de impacto. */
    public static double alturaDoAlvo(double t) {
        if (t <= 0) return 0;
        t = Math.min(t, PASSAGEM);
        int k = (int) Math.min(SOCOS - 1, Math.floor(t / ENTRE_SOCOS));
        return SUBIDA * (k + arremesso((t - k * ENTRE_SOCOS - PARADA) / TICKS_DA_SUBIDA));
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
        int k = (int) Math.min(SOCOS - 1, Math.floor(e / ENTRE_SOCOS));
        double desde = e - k * ENTRE_SOCOS - PARADA;
        if (k < SOCOS - 1) return EMPURRAO * (k + arremesso(desde / TICKS_DA_SUBIDA));
        return EMPURRAO * k + ARREMESSO * arremesso(desde / TICKS_DO_ARREMESSO);
    }

    /** No espaço: quanto o Cid já avançou atrás do alvo. Depois do último soco ele fica: o alvo é que vai embora. */
    public static double avancoDoCid(int t) {
        int e = t - PRIMEIRO_NO_ESPACO;
        if (e < 0) return 0;
        return EMPURRAO * Math.min(SOCOS - 1, (e + ANTES_DO_SOCO) / ENTRE_SOCOS);
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

    /** Qual das três palavras está na tela no tick {@code t} (0 a 2), ou -1 (antes, depois e nos silêncios). */
    public static int palavra(double t) {
        if (t < FALA || t >= ATOMIC) return -1;
        if (t >= TERCEIRA) return 2;
        double u = t - FALA;
        int i = (int) (u / ENTRE_PALAVRAS);
        return i < 2 && u - i * ENTRE_PALAVRAS < TICKS_DA_PALAVRA ? i : -1;
    }

    /** O tamanho da aura roxa enquanto as duas primeiras palavras são ditas, de 0 a 1. */
    public static double aura(double t) {
        return suave((t - FALA) / (CARGA - FALA));
    }

    /** A carga, de 0 a 1: a aura inteira é sugada para a espada. */
    public static double carga(double t) {
        return suave((t - CARGA) / TICKS_DA_CARGA);
    }

    /** O raio da esfera, em fração do máximo, {@code t} ticks depois do Atomic: nasce pequena e cresce cada vez mais depressa. */
    public static double raioDoClarao(double t) {
        double s = Math.max(0, Math.min(1, t / TICKS_DO_CLARAO));
        return Math.pow(s, 1.6);
    }

    /** A opacidade da esfera roxa: estoura de uma vez e some antes de a tela clarear. */
    public static double clarao(double t) {
        if (t < 0 || t >= SOME_O_CLARAO) return 0;
        return Math.min(1, t / 2) * Math.min(1, (SOME_O_CLARAO - t) / 10);
    }

    /** Quando a esfera já sumiu, e até quando a tela fica branca (a volta acontece por baixo dela). */
    public static final int SOME_O_CLARAO = TICKS_DO_CLARAO - 6, FIM_DO_BRANCO = TICKS_DO_CLARAO + 20;

    /** O branco na tela, {@code t} ticks depois do Atomic: só quando a esfera some; cobre a volta e abre no chão. */
    public static double branco(double t) {
        if (t < SOME_O_CLARAO - 8 || t >= FIM_DO_BRANCO) return 0;
        return Math.min(1, (t - (SOME_O_CLARAO - 8)) / 8) * Math.min(1, (FIM_DO_BRANCO - t) / 14);
    }

    /** O lampejo que esconde a passagem para o espaço, {@code t} ticks depois dela. */
    public static double lampejo(double t) {
        return t < 0 || t >= TICKS_DO_LAMPEJO ? 0 : 1 - t / TICKS_DO_LAMPEJO;
    }

    public static final int TICKS_DO_LAMPEJO = 10;

    /** A força da luz no céu do mundo, {@code t} ticks depois do Atomic. */
    public static double luz(double t) {
        if (t < 0 || t >= TICKS_DA_LUZ) return 0;
        return Math.min(1, t / 4) * Math.min(1, (TICKS_DA_LUZ - t) / 40);
    }

    /** A onda de choque no chão, {@code t} ticks depois da volta: o raio (em fração do máximo) e a força que resta. */
    public static double raioDaOnda(double t) {
        return arremesso(t / TICKS_DA_ONDA);
    }

    public static double onda(double t) {
        return t < 0 || t >= TICKS_DA_ONDA ? 0 : 1 - t / TICKS_DA_ONDA;
    }
}
