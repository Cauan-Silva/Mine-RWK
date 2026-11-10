package com.minerwk.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

/** A maestria de um jogador num personagem: XP total, níveis dos nós da árvore e se já despertou. */
public final class DadosMaestria {
    public static final Codec<DadosMaestria> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("xp", 0L).forGetter(d -> d.xp),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("nos", Map.of()).forGetter(d -> d.nos),
            Codec.BOOL.optionalFieldOf("despertou", false).forGetter(d -> d.despertou)
    ).apply(i, DadosMaestria::new));

    public long xp;
    public Map<String, Integer> nos;
    public boolean despertou;

    public DadosMaestria() {
        this(0, Map.of(), false);
    }

    public DadosMaestria(long xp, Map<String, Integer> nos, boolean despertou) {
        this.xp = xp;
        this.nos = new HashMap<>(nos);
        this.despertou = despertou;
    }
}
