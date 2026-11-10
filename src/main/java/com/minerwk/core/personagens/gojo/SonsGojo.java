package com.minerwk.core.personagens.gojo;

import java.util.List;
import java.util.Optional;

/**
 * Ids dos sons do Gojo (gerados por {@code ferramentas/gerar_sfx_gojo.py}) e qual som acompanha cada
 * visual. Sem classes do Minecraft; o registro fica em {@code registro/Sons}.
 */
public final class SonsGojo {
    public static final String AZUL = "gojo_azul";
    public static final String VERMELHO_CARGA = "gojo_vermelho_carga";
    public static final String VERMELHO_DISPARO = "gojo_vermelho_disparo";
    public static final String VERMELHO_EXPLOSAO = "gojo_vermelho_explosao";
    public static final String INFINITO_ATIVA = "gojo_infinito_ativa";
    public static final String INFINITO_DESATIVA = "gojo_infinito_desativa";
    public static final String INFINITO_TIC = "gojo_infinito_tic";
    public static final String ROXO_CARGA = "gojo_roxo_carga";
    public static final String ROXO_DISPARO = "gojo_roxo_disparo";
    public static final String DOMINIO = "gojo_dominio";
    public static final String DOMINIO_FIM = "gojo_dominio_fim";

    public static final List<String> TODOS = List.of(AZUL, VERMELHO_CARGA, VERMELHO_DISPARO, VERMELHO_EXPLOSAO,
            INFINITO_ATIVA, INFINITO_DESATIVA, INFINITO_TIC, ROXO_CARGA, ROXO_DISPARO, DOMINIO, DOMINIO_FIM);

    private SonsGojo() {
    }

    /**
     * O som que toca quando o visual {@code tipo} começa, no ponto do visual. Assim o som sai sempre junto
     * com o que se vê e do lugar certo (a explosão longe soa de longe).
     */
    public static Optional<String> doVisual(String tipo) {
        return Optional.ofNullable(switch (tipo) {
            case "gojo_azul" -> AZUL;
            case "gojo_vermelho_carga" -> VERMELHO_CARGA;
            case "gojo_roxo_carga" -> ROXO_CARGA;
            case "gojo_vermelho_explosao" -> VERMELHO_EXPLOSAO;
            case "gojo_infinito_toque" -> INFINITO_TIC;
            default -> null;
        });
    }

    /** Se o som do visual {@code tipo} para quando o visual é encerrado antes da hora (as cargas). */
    public static boolean cortaComOVisual(String tipo) {
        return tipo.equals("gojo_vermelho_carga") || tipo.equals("gojo_roxo_carga");
    }

    /** Um som que começa {@code antes} ticks antes de o visual acabar. */
    public record NoFim(String som, int antes) {
    }

    /** O som de fim do visual {@code tipo}: o domínio racha e o estilhaço cai no tick em que ele some. */
    public static Optional<NoFim> antesDoFim(String tipo) {
        return tipo.equals("gojo_vazio_infinito") ? Optional.of(new NoFim(DOMINIO_FIM, 8)) : Optional.empty();
    }
}
