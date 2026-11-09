package com.minerwk.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

/** Dados do kit salvos no jogador (Data Attachment). A habilidade em uso não é salva. */
public final class DadosKit {
    public static final Codec<DadosKit> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("personagem", "").forGetter(d -> d.personagem),
            Codec.DOUBLE.optionalFieldOf("recurso", 0.0).forGetter(d -> d.recurso),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("recargas", Map.of()).forGetter(d -> d.recargas)
    ).apply(i, DadosKit::new));

    /** Id do personagem; vazio enquanto o jogador não escolheu. */
    public String personagem;
    public double recurso;
    public Map<String, Long> recargas;

    public DadosKit() {
        this("", 0, Map.of());
    }

    public DadosKit(String personagem, double recurso, Map<String, Long> recargas) {
        this.personagem = personagem;
        this.recurso = recurso;
        this.recargas = new HashMap<>(recargas);
    }

    public boolean temPersonagem() {
        return !personagem.isEmpty();
    }
}
