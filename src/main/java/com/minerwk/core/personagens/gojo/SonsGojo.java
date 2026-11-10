package com.minerwk.core.personagens.gojo;

import com.minerwk.core.kit.Vetor;
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
    public static final String R_ENCANTAMENTO = "gojo_r_encantamento";
    public static final String R_AVANCO = "gojo_r_avanco";
    public static final String R_UPPERCUT = "gojo_r_uppercut";
    public static final String R_SUBIDA = "gojo_r_subida";
    public static final String R_ESFERAS = "gojo_r_esferas";
    public static final String R_FUSAO = "gojo_r_fusao";
    public static final String R_EXPLOSAO = "gojo_r_explosao";

    /** O vento da subida começa esses ticks depois do soco, quando o impact frame acaba. */
    public static final int ATRASO_SUBIDA = 3;
    /** Um som marcado para o meio de um visual ainda toca se o tick atrasar menos que isso. */
    public static final int JANELA = 5;

    public static final List<String> TODOS = List.of(AZUL, VERMELHO_CARGA, VERMELHO_DISPARO, VERMELHO_EXPLOSAO,
            INFINITO_ATIVA, INFINITO_DESATIVA, INFINITO_TIC, ROXO_CARGA, ROXO_DISPARO, DOMINIO, DOMINIO_FIM,
            R_ENCANTAMENTO, R_AVANCO, R_UPPERCUT, R_SUBIDA, R_ESFERAS, R_FUSAO, R_EXPLOSAO);

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
            case "gojo_roxo_200_carga" -> R_ENCANTAMENTO;
            case "gojo_roxo_maximo" -> R_AVANCO;
            case "gojo_roxo_maximo_esferas" -> R_ESFERAS;
            case "gojo_roxo_explosao" -> R_EXPLOSAO;
            default -> null;
        });
    }

    /** Se o som do visual {@code tipo} para quando o visual é encerrado antes da hora (as cargas). */
    public static boolean cortaComOVisual(String tipo) {
        return tipo.equals("gojo_vermelho_carga") || tipo.equals("gojo_roxo_carga") || tipo.equals("gojo_roxo_200_carga");
    }

    /** Um som que toca no meio de um visual: no {@code tick} dele, em {@code onde} a partir da origem do visual. */
    public record NoMeio(String som, int tick, Vetor onde) {
        /** Janela, não tick exato: o relógio do cliente pode pular. Quem chama garante que toca uma vez só. */
        public boolean naJanela(long idade) {
            return idade >= tick && idade < tick + JANELA;
        }
    }

    /**
     * Os sons que tocam no meio do visual {@code tipo} (a {@code direcao} e a {@code forca} são as do
     * visual): o uppercut e a subida do R, no ponto onde o Gojo bate, e a fusão, quando o núcleo nasce.
     */
    public static List<NoMeio> noMeio(String tipo, Vetor direcao, float forca) {
        if (tipo.equals("gojo_roxo_maximo")) {
            // A forca do visual é a distância até o alvo.
            int a = RoxoMaximo.avanco(forca);
            return List.of(new NoMeio(R_UPPERCUT, a, RoxoMaximo.deslocamentoGojo(a, direcao, forca)),
                    new NoMeio(R_SUBIDA, a + ATRASO_SUBIDA,
                            RoxoMaximo.deslocamentoGojo(a + RoxoMaximo.TICKS_SUBIDA / 2.0, direcao, forca)));
        }
        if (tipo.equals("gojo_roxo_maximo_esferas")) {
            // A forca do visual são os ticks das esferas: a fusão começa quando elas somem.
            return List.of(new NoMeio(R_FUSAO, Math.round(forca), Vetor.ZERO));
        }
        return List.of();
    }

    /** Um som que começa {@code antes} ticks antes de o visual acabar. */
    public record NoFim(String som, int antes) {
        /**
         * Se já é hora de tocar, com o visual na {@code idade} de {@code duracao} ticks. É uma janela, não um
         * tick exato: o relógio do cliente pode pular. Quem chama garante que toca uma vez só.
         */
        public boolean naJanela(long idade, int duracao) {
            return idade >= duracao - antes && idade < duracao;
        }
    }

    /**
     * Por quantos ticks o som {@code som}, que começa junto com a carga, ainda pode ser cortado por uma
     * interrupção. Vazio para os sons que nunca são cortados (disparos e gestos).
     */
    public static Optional<Integer> cargaInterrompivel(String som) {
        return som.equals(DOMINIO) ? Optional.of(10) : Optional.empty();
    }

    /** O som de fim do visual {@code tipo}: o domínio racha e o estilhaço cai no tick em que ele some. */
    public static Optional<NoFim> antesDoFim(String tipo) {
        return tipo.equals("gojo_vazio_infinito") ? Optional.of(new NoFim(DOMINIO_FIM, 8)) : Optional.empty();
    }
}
