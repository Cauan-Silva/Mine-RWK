package com.minerwk.core.boss;

import java.util.List;

/**
 * Ids dos sons do Toji, gerados por {@code ferramentas/gerar_sfx_toji.py}: secos e físicos (impacto, metal,
 * corrente, ar cortado), sem zumbido de energia. O registro fica em {@code registro/Sons}.
 */
public final class SonsToji {
    public static final String ENTRADA = "toji_entrada";
    /** O clique de metal que avisa um golpe (não toca na fase 3). */
    public static final String AVISO = "toji_aviso";
    public static final String INVESTIDA = "toji_investida";
    public static final String LANCA = "toji_lanca";
    /** A Lança desfazendo o Infinito. */
    public static final String ANULA = "toji_anula";
    public static final String CORRENTE = "toji_corrente";
    public static final String SOCO = "toji_soco";
    public static final String NUVEM = "toji_nuvem";
    public static final String SUMIR = "toji_sumir";
    public static final String SURGIR = "toji_surgir";
    /** A marca da Execução do Assassino no alvo. */
    public static final String MARCA = "toji_marca";
    public static final String FASE = "toji_fase";
    /** Quem invocou cai: o golpe e o coração batendo pelos 3 segundos da queda. */
    public static final String QUEDA = "toji_queda";
    public static final String DESPERTAR = "toji_despertar";
    public static final String MORTE = "toji_morte";

    public static final List<String> TODOS = List.of(ENTRADA, AVISO, INVESTIDA, LANCA, ANULA, CORRENTE, SOCO, NUVEM,
            SUMIR, SURGIR, MARCA, FASE, QUEDA, DESPERTAR, MORTE);

    private SonsToji() {
    }
}
