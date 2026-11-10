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
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("recargas", Map.of()).forGetter(d -> d.recargas),
            Codec.unboundedMap(Codec.STRING, DadosMaestria.CODEC).optionalFieldOf("maestria", Map.of()).forGetter(d -> d.maestria)
    ).apply(i, DadosKit::new));

    /** Id do personagem; vazio enquanto o jogador não escolheu. */
    public String personagem;
    public double recurso;
    public Map<String, Long> recargas;
    /** A maestria de cada personagem que o jogador já usou, pelo id; trocar de personagem não apaga. */
    public Map<String, DadosMaestria> maestria;

    public DadosKit() {
        this("", 0, Map.of(), Map.of());
    }

    public DadosKit(String personagem, double recurso, Map<String, Long> recargas, Map<String, DadosMaestria> maestria) {
        this.personagem = personagem;
        this.recurso = recurso;
        this.recargas = new HashMap<>(recargas);
        this.maestria = new HashMap<>(maestria);
    }

    /** A maestria no personagem atual (criada zerada na primeira vez). */
    public DadosMaestria maestriaAtual() {
        return maestria.computeIfAbsent(personagem, id -> new DadosMaestria());
    }

    public boolean temPersonagem() {
        return !personagem.isEmpty();
    }
}
