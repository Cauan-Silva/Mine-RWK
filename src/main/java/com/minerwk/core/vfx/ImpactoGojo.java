package com.minerwk.core.vfx;

import java.util.Map;
import java.util.Optional;

/**
 * Quanto cada momento forte do Gojo mexe na câmera de quem vê: tremor, clarão e soco de FOV.
 * Tudo cai linearmente com a distância até zero no raio.
 */
public final class ImpactoGojo {
    /**
     * Reação cheia (em cima do ponto). {@code fov} é a fração somada ao FOV (0,06 = +6%; negativo fecha).
     */
    public record Reacao(float tremor, int ticksTremor, int corClarao, float alphaClarao, int ticksClarao,
                         double fov, int ticksFov, double raio) {
    }

    private static final Map<String, Reacao> TABELA = Map.of(
            "gojo_azul", new Reacao(.25f, 10, 0x3CC8FF, .15f, 3, -.05, 10, 12),
            "gojo_vermelho_explosao", new Reacao(.8f, 8, 0xFFB0A0, .35f, 4, .06, 6, 24),
            "gojo_roxo", new Reacao(1f, 12, 0xB060FF, .45f, 5, .08, 8, 30),
            "gojo_roxo_200", new Reacao(1.6f, 20, 0xFFFFFF, .7f, 6, .12, 12, 48),
            "gojo_vazio_infinito", new Reacao(.5f, 10, 0xDDEEFF, .8f, 3, -.08, 20, 16),
            "gojo_roxo_explosao", new Reacao(2f, 30, 0xC890FF, .85f, 8, .15, 16, 64));

    /** Raio em que o tremor da carga do Roxo 200% ainda chega. */
    public static final double RAIO_CARGA_200 = 16;

    private ImpactoGojo() {
    }

    public static Optional<Reacao> de(String momento) {
        return Optional.ofNullable(TABELA.get(momento));
    }

    /** 1 em cima do ponto, 0 no raio e além. */
    public static double queda(double distancia, double raio) {
        return raio <= 0 ? 0 : Math.max(0, 1 - distancia / raio);
    }

    public static float alphaClarao(Reacao r, double idade) {
        if (idade < 0 || r.ticksClarao() <= 0) return 0;
        double k = 1 - idade / r.ticksClarao();
        return (float) Math.max(0, Math.min(1, r.alphaClarao() * k * k));
    }

    /** Multiplicador do FOV: começa em 1 + fov e volta suave a 1. */
    public static double fov(Reacao r, double idade) {
        if (idade < 0 || idade >= r.ticksFov()) return 1;
        return 1 + r.fov() * (1 - PerfilVfxGojo.suave(idade / r.ticksFov()));
    }

    /** Tremor contínuo da carga do Roxo 200%: de 0,1 a 0,6 ao longo da carga. */
    public static double tremorCarga200(double t, int carga) {
        double k = carga <= 0 ? 1 : Math.max(0, Math.min(1, t / carga));
        return .1 + .5 * k;
    }
}
