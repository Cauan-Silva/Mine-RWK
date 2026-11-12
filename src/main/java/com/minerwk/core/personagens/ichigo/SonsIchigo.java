package com.minerwk.core.personagens.ichigo;

import com.minerwk.core.personagens.gojo.SonsGojo;
import java.util.List;
import java.util.Optional;

/**
 * Ids dos sons do Ichigo (gerados por {@code ferramentas/gerar_sfx_ichigo.py}) e qual som acompanha cada
 * visual. Todo som do Ichigo sai de um visual (do começo dele ou, na máscara quebrando, do fim): assim ele soa junto com o que se vê, do lugar certo, e para
 * com o visual quando é o caso. Sem classes do Minecraft; o registro fica em {@code registro/Sons}.
 */
public final class SonsIchigo {
    public static final String GETSUGA_CARGA = "ichigo_getsuga_carga";
    public static final String GETSUGA = "ichigo_getsuga";
    public static final String GETSUGA_NEGRO = "ichigo_getsuga_negro";
    public static final String SHUNPO = "ichigo_shunpo";
    public static final String ONDA = "ichigo_onda";
    public static final String ARREMESSO = "ichigo_arremesso";
    public static final String BANKAI_CARGA = "ichigo_bankai_carga";
    public static final String BANKAI = "ichigo_bankai";
    public static final String LAMINA = "ichigo_lamina";
    public static final String SAIR = "ichigo_sair";
    public static final String CORTE = "ichigo_corte";
    public static final String MASCARA_VESTE = "ichigo_mascara_veste";
    public static final String MASCARA_QUEBRA = "ichigo_mascara_quebra";
    public static final String MUGETSU_CARGA = "ichigo_mugetsu_carga";
    public static final String MUGETSU = "ichigo_mugetsu";
    public static final String PODERES_PERDIDOS = "ichigo_poderes_perdidos";
    public static final String PODERES_VOLTAM = "ichigo_poderes_voltam";
    public static final String HOLLOW = "ichigo_hollow";
    public static final String HOLLOW_GARRA = "ichigo_hollow_garra";
    public static final String CERO_CARGA = "ichigo_cero_carga";
    public static final String CERO = "ichigo_cero";

    public static final List<String> TODOS = List.of(GETSUGA_CARGA, GETSUGA, GETSUGA_NEGRO, SHUNPO, ONDA, ARREMESSO,
            BANKAI_CARGA, BANKAI, LAMINA, SAIR, CORTE, MASCARA_VESTE, MASCARA_QUEBRA, MUGETSU_CARGA, MUGETSU,
            PODERES_PERDIDOS, PODERES_VOLTAM, HOLLOW, HOLLOW_GARRA, CERO_CARGA, CERO);

    private SonsIchigo() {
    }

    /** O som que toca quando o visual {@code tipo} começa, no ponto do visual. */
    public static Optional<String> doVisual(String tipo) {
        return Optional.ofNullable(switch (tipo) {
            case "ichigo_carga" -> GETSUGA_CARGA;
            case "ichigo_getsuga" -> GETSUGA;
            // Os golpes do Zangetsu interior são os do Ichigo, em branco: soam igual.
            case "ichigo_getsuga_negro", "ichigo_getsuga_branco" -> GETSUGA_NEGRO;
            // O Sonído da forma Hollow é o mesmo passo.
            case "ichigo_shunpo", "ichigo_shunpo_bankai", "ichigo_hollow_sonido" -> SHUNPO;
            case "ichigo_onda" -> ONDA;
            case "ichigo_arremesso", "ichigo_arremesso_branco" -> ARREMESSO;
            case "ichigo_bankai_carga" -> BANKAI_CARGA;
            // O Cero é o mesmo no Zangetsu interior (carmesim) e na forma Hollow (violeta).
            case "ichigo_cero_carga", "ichigo_hollow_cero_carga" -> CERO_CARGA;
            case "ichigo_cero", "ichigo_hollow_cero" -> CERO;
            case "ichigo_hollow" -> HOLLOW;
            case "ichigo_hollow_garra" -> HOLLOW_GARRA;
            case "ichigo_bankai" -> BANKAI;
            case "ichigo_lamina" -> LAMINA;
            case "ichigo_sair" -> SAIR;
            case "ichigo_corte" -> CORTE;
            case "ichigo_mascara" -> MASCARA_VESTE;
            case "ichigo_mugetsu_carga" -> MUGETSU_CARGA;
            case "ichigo_mugetsu" -> MUGETSU;
            case "ichigo_poderes_perdidos" -> PODERES_PERDIDOS;
            case "ichigo_poderes_voltam" -> PODERES_VOLTAM;
            default -> null;
        });
    }

    /**
     * Se o som do visual {@code tipo} para quando o visual é encerrado antes da hora: as duas cargas (soltou
     * cedo ou foi interrompido) e a lâmina carregada (saiu do Bankai).
     */
    public static boolean cortaComOVisual(String tipo) {
        return tipo.equals("ichigo_carga") || tipo.equals("ichigo_bankai_carga") || tipo.equals("ichigo_lamina")
                || tipo.equals("ichigo_mugetsu_carga") || tipo.equals("ichigo_cero_carga")
                || tipo.equals("ichigo_hollow_cero_carga");
    }

    /** A máscara começa a quebrar esses ticks antes de sumir: o último estalo cai quando ela some. */
    static final int QUEBRA_DA_MASCARA = 8;

    /** O som de fim do visual {@code tipo}: a máscara Hollow quebrando quando os 11 s acabam. */
    public static Optional<SonsGojo.NoFim> antesDoFim(String tipo) {
        return tipo.equals("ichigo_mascara") ? Optional.of(new SonsGojo.NoFim(MASCARA_QUEBRA, QUEBRA_DA_MASCARA)) : Optional.empty();
    }
}
