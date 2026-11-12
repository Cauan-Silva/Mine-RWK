package com.minerwk.core.hud;

import com.minerwk.core.hud.TemaUi.Moldura;
import java.util.Map;

/**
 * Os temas de interface de cada personagem. Um personagem pode ter um tema por forma (o Ichigo no Hollow); forma
 * sem tema próprio usa o da base, e quem não tem tema usa o {@link #GENERICO}. As cores são as do spec
 * [[2026-11-12 UI dos personagens]].
 */
public final class Temas {
    public static final TemaUi GENERICO = new TemaUi(0xFFAAAAAA, 0xFF555555, 0xAA000000, 0xFF555555, Moldura.LISA, "generico");

    private static final TemaUi GOJO = new TemaUi(0xFF7FD8FF, 0xFFFFFFFF, 0xC0101828, 0xFFB48CFF, Moldura.LISA, "gojo");
    private static final TemaUi ICHIGO = new TemaUi(0xFFE03030, 0xFF1A1A1A, 0xC0100808, 0xFF802020, Moldura.CORTADA, "ichigo");
    private static final TemaUi ICHIGO_HOLLOW =
            new TemaUi(0xFFF0EDE0, 0xFFE03030, 0xC0180808, 0xFFB02020, Moldura.CORTADA, "ichigo_hollow");
    private static final TemaUi CID = new TemaUi(0xFF9A6BFF, 0xFFE8B84A, 0xC00C0818, 0xFF5A3A9A, Moldura.ORNADA, "cid");

    /** Tema da base de cada personagem. */
    private static final Map<String, TemaUi> BASE = Map.of("gojo", GOJO, "ichigo", ICHIGO, "cid", CID);
    /** Temas de forma, com a chave {@code personagem:forma}. */
    private static final Map<String, TemaUi> FORMAS = Map.of("ichigo:hollow", ICHIGO_HOLLOW);

    private Temas() {
    }

    public static TemaUi tema(String personagem, String forma) {
        TemaUi daForma = FORMAS.get(personagem + ":" + forma);
        if (daForma != null) return daForma;
        return BASE.getOrDefault(personagem, GENERICO);
    }

    public static boolean temTema(String personagem) {
        return BASE.containsKey(personagem);
    }
}
