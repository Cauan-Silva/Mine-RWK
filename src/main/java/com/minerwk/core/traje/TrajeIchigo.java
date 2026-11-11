package com.minerwk.core.traje;

import java.util.List;

/**
 * Nomes dos trajes do Ichigo e dos ossos que o jogo mexe neles, sem classes do Minecraft. Os modelos saem
 * de {@code ferramentas/gerar_traje_ichigo.py}: a espada faz parte do traje, presa ao braço direito.
 */
public final class TrajeIchigo {
    /** O shihakushō com a Zangetsu. */
    public static final String SHIKAI = "ichigo";
    /** O casaco longo com a Tensa Zangetsu. */
    public static final String BANKAI = "ichigo_bankai";
    /** A espada inteira: some enquanto ela está fora da mão (o arremesso). */
    public static final String OSSO_ARMA = "arma";
    /** As mangas largas do shihakushō: uma peça só de cada lado. */
    public static final List<String> MANGAS_DO_SHIKAI = List.of("manto_manga_esq", "manto_manga_dir");

    private TrajeIchigo() {
    }

    public static boolean eDoIchigo(String traje) {
        return SHIKAI.equals(traje) || BANKAI.equals(traje);
    }
}
