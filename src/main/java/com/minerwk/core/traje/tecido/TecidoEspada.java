package com.minerwk.core.traje.tecido;

/**
 * Os enfeites da espada como pano: a faixa da Zangetsu ({@code arma_cloth}, {@code arma_cloth_1..3}) e a
 * corrente da Tensa Zangetsu ({@code arma_chain}, {@code arma_chain_1..3}). Vem do Minezin (mesmo autor).
 * Uma espada na mão se mexe mais com o braço e com a mira do que com a caminhada, então a mola é puxada
 * pelo giro e pela inclinação da cabeça, pelo balanço do braço e pelos golpes, com ganho forte, e uma
 * ondulação contínua desce pela tira. Um tick por tick do jogo (20 Hz).
 */
public final class TecidoEspada {
    private static final MolaManto.Pose ZERO = new MolaManto.Pose(0, 0, 0);
    private static final String FAIXA = "arma_cloth", CORRENTE = "arma_chain";
    private final MolaManto mola = new MolaManto(1.8);
    private double tempo, agitacao;

    /** O segmento (0 a 3) de um osso de enfeite, ou -1 se o osso não é um. */
    public static int segmento(String osso) {
        for (String base : new String[] {FAIXA, CORRENTE}) {
            if (osso.equals(base)) return 0;
            if (osso.startsWith(base + "_")) {
                try {
                    return Math.min(3, Integer.parseInt(osso.substring(base.length() + 1)));
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public void reset() {
        mola.reset();
        tempo = agitacao = 0;
    }

    /**
     * Um tick. {@code frente}, {@code lado} e {@code vertical} são o deslocamento do jogador no referencial
     * do corpo (blocos); {@code giro} e {@code arfagem}, a mudança da mira (radianos); {@code braco}, a
     * mudança do balanço do braço; {@code golpe}, de 0 a 1 no meio de um ataque.
     */
    public void tick(double frente, double lado, double vertical, double giro, double arfagem, double braco, double golpe,
                     boolean noChao) {
        if (!Double.isFinite(frente + lado + vertical + giro + arfagem + braco + golpe)) {
            reset();
            return;
        }
        tempo++;
        double alvo = Math.min(2, .45 + Math.hypot(frente, lado) * 4 + Math.abs(giro) * 3 + Math.abs(arfagem) * 3 + golpe);
        agitacao += (alvo - agitacao) * .3;
        mola.tick(new MolaManto.Entrada((frente + braco * .5 + arfagem * 1.1) * 3.2, (lado + giro * .9) * 3.2,
                vertical * 3 + arfagem * .8, giro * 2.4 + golpe * 1.5, noChao));
    }

    /** Quanto o osso gira além do repouso (radianos); amostrar não avança a física. */
    public MolaManto.Pose pose(String osso, double parcial) {
        int k = segmento(osso);
        if (k < 0) return ZERO;
        MolaManto.Pose a = mola.amostrar(k, parcial);
        MolaManto.Pose b = k == 0 ? ZERO : mola.amostrar(k - 1, parcial);
        // Os elos da corrente pesam mais que a faixa: menos chicote e menos tremor na ponta.
        boolean corrente = osso.startsWith(CORRENTE);
        double g = (corrente ? 3.2 : 4.6) * (k == 0 ? .8 : 1 + .35 * k), t = tempo + parcial, u = (k + 1) / 4.0;
        // Nunca parada: um pêndulo que desce pela tira mesmo em repouso, mais um tremor rápido que cresce
        // com a agitação. A corrente balança mais devagar e mais largo; a faixa treme mais.
        double bal = (corrente ? .14 : .12) * (.6 + agitacao) * u * Math.sin(t * (corrente ? .32 : .45) - k * .8);
        double tremor = agitacao * (corrente ? .1 : .22) * u * (Math.sin(t * 1.1 - k * 1.2) + .6 * Math.sin(t * 2.7 - k * 2.1));
        double lateral = (corrente ? .1 : .08) * (.5 + agitacao) * u * Math.sin(t * (corrente ? .27 : .38) + 1.3 - k * .9);
        return new MolaManto.Pose(lim((a.x() - b.x()) * g + tremor + bal, 1.3), lim((a.y() - b.y()) * g * .6 + tremor * .3, .6),
                lim((a.z() - b.z()) * g + tremor * .7 + lateral, 1.3));
    }

    private static double lim(double v, double m) {
        return Math.max(-m, Math.min(m, v));
    }
}
