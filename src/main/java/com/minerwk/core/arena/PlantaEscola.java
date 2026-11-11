package com.minerwk.core.arena;

import java.util.ArrayList;
import java.util.List;

/**
 * A planta da arena: qual material vai em cada posição, relativa ao bloco do centro. {@code dy} 0 é a
 * altura dos pés no pátio, então o piso fica em {@code dy} −1. Separada do código que põe os blocos no
 * mundo para poder ser testada e ajustada sem abrir o jogo.
 */
public final class PlantaEscola {
    /** Meio lado do pátio: de −30 a +30 são 61 blocos. */
    public static final int MEIO_PATIO = 30;
    /** Camadas sob os pés: o piso e mais três de pedra. */
    public static final int CAMADAS = 4;

    public enum Material {
        PEDRA, TIJOLO_PEDRA, LAJE_PEDRA, ANDESITO_POLIDO, ESCADA_PEDRA, MADEIRA_ESCURA, MADEIRA_VERMELHA, TRONCO,
        FOLHA, LANTERNA, MURO
    }

    public record Bloco(int dx, int dy, int dz, Material material) {
    }

    private PlantaEscola() {
    }

    /** Pátio liso de 61 × 61: piso de tijolo de pedra e três camadas de pedra por baixo. */
    public static List<Bloco> plataforma() {
        List<Bloco> blocos = new ArrayList<>((2 * MEIO_PATIO + 1) * (2 * MEIO_PATIO + 1) * CAMADAS);
        for (int dy = -CAMADAS; dy <= -1; dy++) {
            Material m = dy == -1 ? Material.TIJOLO_PEDRA : Material.PEDRA;
            for (int dx = -MEIO_PATIO; dx <= MEIO_PATIO; dx++) {
                for (int dz = -MEIO_PATIO; dz <= MEIO_PATIO; dz++) blocos.add(new Bloco(dx, dy, dz, m));
            }
        }
        return blocos;
    }
}
