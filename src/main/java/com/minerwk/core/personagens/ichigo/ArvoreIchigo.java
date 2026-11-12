package com.minerwk.core.personagens.ichigo;

import com.minerwk.core.hud.FormaArvore;
import com.minerwk.core.hud.FormaArvore.Galho;
import com.minerwk.core.hud.FormaArvore.Selo;
import com.minerwk.core.maestria.Arvore;
import com.minerwk.core.maestria.No;
import java.util.List;
import java.util.Map;

/**
 * A árvore de habilidades do Ichigo. Antes do Despertar ela melhora o Shikai, o reiatsu e o fôlego do
 * Bankai; depois de vencer o Zangetsu interior, o Getsuga negro, a máscara e o Mugetsu.
 */
public final class ArvoreIchigo {
    public static final String GETSUGA_FORCA = "ichigo_getsuga_forca";
    public static final String GETSUGA_RECARGA = "ichigo_getsuga_recarga";
    public static final String SHUNPO_RECARGA = "ichigo_shunpo_recarga";
    public static final String REIATSU_RESERVA = "ichigo_reiatsu_reserva";
    public static final String REIATSU_FLUXO = "ichigo_reiatsu_fluxo";
    public static final String BANKAI_FOLEGO = "ichigo_bankai_folego";
    public static final String DESPERTAR = "ichigo_despertar";
    public static final String NEGRO_FORCA = "ichigo_negro_forca";
    public static final String LAMINA_DURACAO = "ichigo_lamina_duracao";
    public static final String MASCARA_DURACAO = "ichigo_mascara_duracao";
    public static final String MASCARA_RECARGA = "ichigo_mascara_recarga";
    public static final String MUGETSU_RECARGA = "ichigo_mugetsu_recarga";
    public static final String PODERES_VOLTA = "ichigo_poderes_volta";

    public static final Arvore ARVORE = new Arvore(List.of(
            No.melhoria(GETSUGA_FORCA, false, .08),
            No.recarga(GETSUGA_RECARGA, false, "ichigo_getsuga", .08),
            No.recarga(SHUNPO_RECARGA, false, "ichigo_shunpo", .08),
            No.melhoria(REIATSU_RESERVA, false, .10),
            No.melhoria(REIATSU_FLUXO, false, .10),
            No.melhoria(BANKAI_FOLEGO, false, .10),
            No.despertar(DESPERTAR, 5, 20),
            No.melhoria(NEGRO_FORCA, true, .08),
            No.melhoria(LAMINA_DURACAO, true, .10),
            No.melhoria(MASCARA_DURACAO, true, .10),
            No.recarga(MASCARA_RECARGA, true, Ichigo.MASCARA, .08),
            No.recarga(MUGETSU_RECARGA, true, Ichigo.MUGETSU, .08),
            No.melhoria(PODERES_VOLTA, true, .10)));

    /** Como a árvore é desenhada (spec [[2026-11-12 UI dos personagens]]). */
    public static final FormaArvore FORMA = new FormaArvore(List.of(
            Galho.antes("getsuga", "ichigo_getsuga", List.of(GETSUGA_FORCA, GETSUGA_RECARGA), Map.of()),
            Galho.antes("shunpo", "ichigo_shunpo", List.of(SHUNPO_RECARGA), Map.of()),
            Galho.antes("reiatsu", "ichigo_recurso", List.of(REIATSU_RESERVA, REIATSU_FLUXO, BANKAI_FOLEGO),
                    Map.of(REIATSU_RESERVA, Selo.RECURSO, REIATSU_FLUXO, Selo.RECURSO, BANKAI_FOLEGO, Selo.RECURSO)),
            Galho.depois("getsuga_negro", "ichigo_getsuga_negro", List.of(NEGRO_FORCA, LAMINA_DURACAO), Map.of(LAMINA_DURACAO, Selo.DURACAO)),
            Galho.depois("mascara", "ichigo_mascara", List.of(MASCARA_DURACAO, MASCARA_RECARGA), Map.of(MASCARA_DURACAO, Selo.DURACAO)),
            Galho.depois("mugetsu", "ichigo_mugetsu", List.of(MUGETSU_RECARGA, PODERES_VOLTA), Map.of())));

    private ArvoreIchigo() {
    }
}
