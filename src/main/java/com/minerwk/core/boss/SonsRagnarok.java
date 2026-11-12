package com.minerwk.core.boss;

import java.util.List;

/**
 * Ids dos sons do Ragnarok, gerados por {@code ferramentas/gerar_sfx_ragnarok.py}: graves e pesados (rugido,
 * terra, fogo, o ar das asas). O registro fica em {@code registro/Sons}; quem os toca é o servidor.
 */
public final class SonsRagnarok {
    /** O rugido da chegada, saindo do portal. */
    public static final String ENTRADA = "ragnarok_entrada";
    /** O murro: o golpe, a terra rachando e as pedras caindo. */
    public static final String SOCO = "ragnarok_soco";
    /** As asas abrindo de uma vez e a magia saindo em leque. */
    public static final String RAJADA = "ragnarok_rajada";
    /** As asas batendo na subida da investida. */
    public static final String ASAS = "ragnarok_asas";
    /** O rasante: o ar rasgado e o fogo passando. */
    public static final String INVESTIDA = "ragnarok_investida";
    /** Os 2 s em que ele enche o peito: o fogo sendo puxado para a boca. */
    public static final String SOPRO_CARGA = "ragnarok_sopro_carga";
    /** O feixe: o rugido de fogo por 2 s. */
    public static final String SOPRO = "ragnarok_sopro";
    /** O pisão e a onda correndo pelo chão. */
    public static final String PISAO = "ragnarok_pisao";
    /** O corte do Shadow na asa. */
    public static final String CORTE = "ragnarok_corte";
    /** O urro de dor e de raiva depois do corte. */
    public static final String RUGIDO = "ragnarok_rugido";
    public static final String MORTE = "ragnarok_morte";

    public static final List<String> TODOS = List.of(ENTRADA, SOCO, RAJADA, ASAS, INVESTIDA, SOPRO_CARGA, SOPRO, PISAO,
            CORTE, RUGIDO, MORTE);

    private SonsRagnarok() {
    }
}
