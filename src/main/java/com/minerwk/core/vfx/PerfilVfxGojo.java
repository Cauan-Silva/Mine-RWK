package com.minerwk.core.vfx;

/**
 * Tempos, alcances e nível de detalhe do visual do Gojo (portado do Minezin, com os números do Mine-RWK).
 * Só aparência: nada aqui muda dano, alcance ou duração dos golpes.
 */
public final class PerfilVfxGojo {
    public static final double DOMINIO_RAIO = 12;
    public static final int DOMINIO_DURACAO = 160;
    /** Ticks até o domo atingir o raio cheio. */
    public static final int DOMINIO_ABERTURA = 20;
    /** Ticks que a explosão do Vermelho fica na tela depois do impacto (só cliente). */
    public static final int EXPLOSAO_VERMELHO = 40;
    /** Ticks que o túnel do Roxo e a marca do Azul ficam no mundo depois do golpe. */
    public static final int RASTRO_ROXO = 60, MARCA_AZUL = 40;
    /** Ticks finais em que o domo do domínio racha e se desfaz. */
    public static final int DOMINIO_RACHA = 20;

    /** Quantos elementos desenhar: faixas e passos das curvas, detritos do Azul, estrelas do domínio. */
    public record Detalhe(int faixas, int passos, int detritos, int estrelas) {
    }

    private static final Detalhe COMPLETO = new Detalhe(64, 14, 20, 48);
    private static final Detalhe REDUZIDO = new Detalhe(24, 8, 8, 16);

    private PerfilVfxGojo() {
    }

    public static Detalhe detalhe(boolean reduzido) {
        return reduzido ? REDUZIDO : COMPLETO;
    }

    /** Curva suave de 0 a 1 (smoothstep), com a entrada limitada a [0, 1]. */
    public static double suave(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Entra em 4 ticks e sai nos últimos 8; zero fora de [0, duracao]. */
    public static double opacidade(double t, int duracao) {
        if (duracao <= 0 || t < 0) return 0;
        return suave(t / 4) * suave((duracao - t) / 8);
    }

    public static double raioDominio(double t) {
        return DOMINIO_RAIO * suave(t / DOMINIO_ABERTURA);
    }

    /** Quanto do ambiente do domínio aparece para uma câmera a {@code distancia} do centro. */
    public static double presencaDominio(double t, double distancia) {
        return suave((raioDominio(t) - distancia) / 2) * suave((DOMINIO_DURACAO - t) / 18) * suave((t - 8) / 10);
    }
}
