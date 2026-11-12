package com.minerwk.core.traje;

import com.minerwk.core.personagens.ichigo.Reiatsu;
import java.util.Optional;

/** Que traje (modelo em {@code geo/traje/<nome>}) cada personagem veste, em cada forma. */
public final class Trajes {
    private Trajes() {
    }

    /** O traje do {@code personagem} na {@code forma}, se ele tiver um. */
    public static Optional<String> de(String personagem, String forma) {
        if ("gojo".equals(personagem)) return Optional.of("gojo");
        if ("ichigo".equals(personagem)) {
            if (Reiatsu.HOLLOW.equals(forma)) return Optional.of(TrajeIchigo.HOLLOW);
            return Optional.of(Reiatsu.BANKAI.equals(forma) ? TrajeIchigo.BANKAI : TrajeIchigo.SHIKAI);
        }
        return Optional.empty();
    }
}
