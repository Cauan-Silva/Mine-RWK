package com.minerwk.core.personagens.ichigo;

import java.util.List;
import java.util.Optional;

/**
 * Ids dos sons do Ichigo (gerados por {@code ferramentas/gerar_sfx_ichigo.py}) e qual som acompanha cada
 * visual. Todo som do Ichigo sai de um visual: assim ele soa junto com o que se vê, do lugar certo, e para
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

    public static final List<String> TODOS = List.of(GETSUGA_CARGA, GETSUGA, GETSUGA_NEGRO, SHUNPO, ONDA, ARREMESSO,
            BANKAI_CARGA, BANKAI, LAMINA, SAIR, CORTE);

    private SonsIchigo() {
    }

    /** O som que toca quando o visual {@code tipo} começa, no ponto do visual. */
    public static Optional<String> doVisual(String tipo) {
        return Optional.ofNullable(switch (tipo) {
            case "ichigo_carga" -> GETSUGA_CARGA;
            case "ichigo_getsuga" -> GETSUGA;
            case "ichigo_getsuga_negro" -> GETSUGA_NEGRO;
            case "ichigo_shunpo", "ichigo_shunpo_bankai" -> SHUNPO;
            case "ichigo_onda" -> ONDA;
            case "ichigo_arremesso" -> ARREMESSO;
            case "ichigo_bankai_carga" -> BANKAI_CARGA;
            case "ichigo_bankai" -> BANKAI;
            case "ichigo_lamina" -> LAMINA;
            case "ichigo_sair" -> SAIR;
            case "ichigo_corte" -> CORTE;
            default -> null;
        });
    }

    /**
     * Se o som do visual {@code tipo} para quando o visual é encerrado antes da hora: as duas cargas (soltou
     * cedo ou foi interrompido) e a lâmina carregada (saiu do Bankai).
     */
    public static boolean cortaComOVisual(String tipo) {
        return tipo.equals("ichigo_carga") || tipo.equals("ichigo_bankai_carga") || tipo.equals("ichigo_lamina");
    }
}
