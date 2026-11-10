package com.minerwk.core.traje;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Regras do traje do Gojo, sem classes do Minecraft: quem o veste, quando a venda sai e o balanço da
 * barra do casaco. O modelo e a textura saem de {@code ferramentas/gerar_traje_gojo.py}.
 */
public final class TrajeGojo {
    public static final String OSSO_VENDA = "venda";
    public static final String OSSO_OLHOS = "olhos";
    /** O quanto um segmento da barra dobra, no máximo (radianos). */
    public static final double BALANCO_MAX = 1.0;

    /** Visuais do Gojo que tiram a venda: o Vazio Infinito e o R inteiro. */
    private static final Set<String> SEM_VENDA = Set.of("gojo_vazio_infinito", "gojo_roxo_200_carga",
            "gojo_roxo_maximo", "gojo_roxo_maximo_esferas", "gojo_roxo_explosao");

    private TrajeGojo() {
    }

    /** O traje que o {@code personagem} veste, se tiver um. */
    public static Optional<String> de(String personagem) {
        return "gojo".equals(personagem) ? Optional.of("gojo") : Optional.empty();
    }

    /** Se a venda está fora do rosto, pelos visuais em andamento do jogador. */
    public static boolean vendaFora(Collection<String> visuaisAtivos) {
        for (String tipo : visuaisAtivos) {
            if (SEM_VENDA.contains(tipo)) return true;
        }
        return false;
    }

    /** Os quatro lados da barra do casaco, com quantas colunas de segmentos cada um tem. */
    public enum Lado {
        // No modelo (vindo do Minezin) "left" e "right" são de quem olha de frente: o osso "left" fica do
        // lado direito do corpo (x negativo), e o braço esquerdo fica em x positivo.
        TRAS("back", 3), FRENTE("front", 3), ESQUERDA("right", 2), DIREITA("left", 2);

        private final String nome;
        private final int colunas;

        Lado(String nome, int colunas) {
            this.nome = nome;
            this.colunas = colunas;
        }

        public int colunas() {
            return colunas;
        }
    }

    /** Nome do osso de um segmento da barra no modelo ({@code linha} 0 em cima, 1 embaixo). */
    public static String ossoDaBarra(Lado lado, int coluna, int linha) {
        return "gojo_hem_" + lado.nome + "_" + coluna + "_" + linha;
    }

    /**
     * Quanto o segmento da barra abre para fora, em radianos (negativo: cola no corpo). A velocidade é a do
     * jogador em blocos por tick, no referencial do corpo: {@code frente} positivo andando para a frente,
     * {@code lateral} positivo andando para a direita e {@code vertical} negativo caindo.
     */
    public static double balanco(Lado lado, int linha, double frente, double lateral, double vertical, double tempo) {
        // O "vento" vem contra o movimento: empurra para fora o lado de trás e cola o lado da frente.
        double vento = switch (lado) {
            case TRAS -> frente;
            case FRENTE -> -frente;
            case ESQUERDA -> lateral;
            case DIREITA -> -lateral;
        };
        double sobe = Math.max(0, -vertical) * 1.1 - Math.max(0, vertical) * .3;
        double respira = .04 * Math.sin(tempo * .12 + lado.ordinal() * 1.7 + linha);
        double abre = vento * 2.2 + sobe + respira;
        // O segmento de baixo dobra um pouco mais, por cima do de cima.
        if (linha > 0) abre *= .6;
        return Math.max(-.15, Math.min(BALANCO_MAX, abre));
    }
}
