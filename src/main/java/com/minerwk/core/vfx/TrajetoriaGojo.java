package com.minerwk.core.vfx;

import com.minerwk.core.kit.Vetor;

/**
 * Caminho das esferas do Gojo. O servidor (dano) e o cliente (desenho) usam as mesmas contas,
 * então o visual passa exatamente por onde o golpe acerta. {@code t} pode ser fracionário (entre ticks).
 */
public final class TrajetoriaGojo {
    /** Blocos por tick da esfera do Vermelho. */
    public static final double VELOCIDADE_VERMELHO = 1.5;

    private TrajetoriaGojo() {
    }

    /** Posição da esfera do Vermelho {@code t} ticks depois de sair de {@code inicio}. */
    public static Vetor vermelho(Vetor inicio, Vetor direcao, double t) {
        return inicio.mais(direcao.vezes(VELOCIDADE_VERMELHO * t));
    }

    /** Ponto da massa do Roxo no tick {@code t}: o primeiro passo já acontece no tick 0. */
    public static Vetor roxo(Vetor origem, Vetor direcao, double passo, double t) {
        return origem.mais(direcao.vezes(passo * (t + 1)));
    }
}
