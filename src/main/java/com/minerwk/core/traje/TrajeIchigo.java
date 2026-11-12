package com.minerwk.core.traje;

import com.minerwk.core.vfx.MugetsuIchigo;
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
    /** A forma final do Mugetsu: cabelo longo, faixas e o corpo coberto de reiatsu preto. Sem espada. */
    public static final String MUGETSU = "ichigo_mugetsu";
    /** A forma Hollow completa (o Vasto Lorde): pele branca, a máscara de chifres, a gola vermelha e a Tensa Zangetsu na mão. */
    public static final String HOLLOW = "ichigo_hollow";
    /** A máscara Hollow leva esses ticks para se fechar sobre o rosto (o gesto do golpe). */
    public static final int MASCARA_SE_FORMA = 6;
    /** A espada inteira: some enquanto ela está fora da mão (o arremesso). */
    public static final String OSSO_ARMA = "arma";
    /** As mangas largas do shihakushō: uma peça só de cada lado. */
    public static final List<String> MANGAS_DO_SHIKAI = List.of("manto_manga_esq", "manto_manga_dir");

    private TrajeIchigo() {
    }

    public static boolean eDoIchigo(String traje) {
        return SHIKAI.equals(traje) || BANKAI.equals(traje) || MUGETSU.equals(traje) || HOLLOW.equals(traje);
    }

    /**
     * Se o visual {@code tipo}, com {@code idade} ticks, põe o dono na forma final: da transformação, no
     * meio da carga do Mugetsu, até o véu se desfazer depois do corte.
     */
    public static boolean formaFinal(String tipo, double idade) {
        if ("ichigo_mugetsu_carga".equals(tipo)) return idade >= MugetsuIchigo.TRANSFORMA;
        return "ichigo_mugetsu".equals(tipo) && idade < MugetsuIchigo.TICKS + MugetsuIchigo.RESTO;
    }

    /** Se o visual {@code tipo} põe a máscara Hollow no rosto do dono: do fim do gesto até ela quebrar. */
    public static boolean mascaraNoRosto(String tipo, double idade, int duracao) {
        return "ichigo_mascara".equals(tipo) && idade >= MASCARA_SE_FORMA && idade < duracao;
    }
}
