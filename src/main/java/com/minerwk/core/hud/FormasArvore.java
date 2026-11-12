package com.minerwk.core.hud;

import com.minerwk.core.personagens.cid.ArvoreCid;
import com.minerwk.core.personagens.gojo.ArvoreGojo;
import com.minerwk.core.personagens.ichigo.ArvoreIchigo;
import java.util.Map;
import java.util.Optional;

/** A forma da árvore de cada personagem que tem uma. */
public final class FormasArvore {
    private static final Map<String, FormaArvore> FORMAS =
            Map.of("gojo", ArvoreGojo.FORMA, "ichigo", ArvoreIchigo.FORMA, "cid", ArvoreCid.FORMA);

    private FormasArvore() {
    }

    public static Optional<FormaArvore> de(String personagem) {
        return Optional.ofNullable(FORMAS.get(personagem));
    }
}
