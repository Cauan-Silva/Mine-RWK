package com.minerwk.core.boss;

import java.util.List;

/**
 * Os números e as regras do Toji como boss (Nêmesis do Gojo), sem classes do Minecraft. O que cada golpe
 * faz no mundo fica na entidade; aqui ficam os tempos, os danos e as decisões da luta.
 */
public final class TojiBoss {
    public static final String ID = "toji";
    /** 12 barras de coração. */
    public static final float VIDA = 240;
    public static final int XP_VITORIA = 500;

    /** Avança 12 blocos em linha reta; a animação "investida" dura 34 ticks. */
    public static final Golpe INVESTIDA = new Golpe("investida", "investida", 18, 4, 12, 90, 4, 14, 2);
    /** Estocada de 6 blocos que atravessa o Infinito; a animação "estocada" dura 28 ticks. */
    public static final Golpe LANCA = new Golpe("lanca", "estocada", 14, 2, 12, 80, 0, 6, 2);
    /** Puxa o alvo de até 14 blocos e emenda um soco; a animação "arremesso" dura 34 ticks. */
    public static final Golpe CORRENTE = new Golpe("corrente", "arremesso", 12, 8, 14, 100, 6, 14, 1);
    /** Nuvem Brincalhona: 3 golpes do bastão num setor à frente; a animação "giro" dura 40 ticks. */
    public static final Golpe NUVEM = new Golpe("nuvem", "giro", 16, 10, 10, 70, 0, 4.5, 3);
    /** A mesma, com 5 golpes, da fase 2 em diante. */
    public static final Golpe NUVEM_5 = new Golpe("nuvem_5", "giro", 16, 14, 10, 70, 0, 4.5, 3);
    /** Desaparecer: some, surge atrás do alvo e golpeia; a animação "sumir" dura 42 ticks. */
    public static final Golpe DESAPARECER = new Golpe("desaparecer", "sumir", 30, 2, 10, 160, 0, 20, 2);
    /** Execução do Assassino (fase 3): marca o alvo e faz 4 investidas de ângulos diferentes; "execucao" dura 90 ticks. */
    public static final Golpe EXECUCAO = new Golpe("execucao", "execucao", 20, 56, 14, 400, 0, 24, 2);
    public static final List<Golpe> GOLPES = List.of(INVESTIDA, LANCA, CORRENTE, NUVEM);

    /** Os golpes de cada fase: a 1 é contra quem só tem o Azul e o Infinito; a 3 ganha a Execução. */
    public static List<Golpe> golpes(int fase) {
        if (fase <= 1) return GOLPES;
        if (fase == 2) return List.of(INVESTIDA, LANCA, CORRENTE, NUVEM_5, DESAPARECER);
        return List.of(INVESTIDA, LANCA, CORRENTE, NUVEM_5, DESAPARECER, EXECUCAO);
    }

    // --- Nuvem Brincalhona ---
    public static final float DANO_NUVEM = 3;
    public static final double RAIO_NUVEM = 3.5;
    public static final double ANGULO_NUVEM = 160;

    /** Quantos golpes do bastão a Nuvem tem. */
    public static int golpesDaNuvem(Golpe golpe) {
        return golpe == NUVEM_5 ? 5 : 3;
    }

    /** Se o bastão bate neste tick da janela: os golpes se espalham por ela, o primeiro no tick 0. */
    public static boolean nuvemBate(Golpe golpe, int tickNaJanela) {
        int n = golpesDaNuvem(golpe);
        if (tickNaJanela < 0 || tickNaJanela >= golpe.janela()) return false;
        return tickNaJanela == 0 || tickNaJanela * n / golpe.janela() != (tickNaJanela - 1) * n / golpe.janela();
    }

    /**
     * Se o ponto a ({@code dx}, {@code dz}) de quem golpeia está no setor de {@code graus} aberto para a
     * direção ({@code dirX}, {@code dirZ}), até {@code raio}.
     */
    public static boolean noSetor(double dx, double dz, double dirX, double dirZ, double raio, double graus) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > raio) return false;
        if (d < 1e-6) return true;
        double dir = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (dir < 1e-9) return true;
        double cos = (dx * dirX + dz * dirZ) / (d * dir);
        return cos >= Math.cos(Math.toRadians(graus / 2)) - 1e-9;
    }

    // --- Desaparecer ---
    /** Tick do golpe em que ele surge atrás do alvo; dali até a janela, a sombra no chão avisa. */
    public static final int TICK_DE_SURGIR = 20;
    public static final double ATRAS_DO_ALVO = 1.6;
    public static final float DANO_SURGIR = 8;
    public static final double RAIO_SURGIR = 2.5;

    // --- Execução do Assassino ---
    public static final int INVESTIDAS_DA_EXECUCAO = 4;
    /** Ticks de cada investida: 10 de aviso pela linha, 3 de avanço e 1 de folga. */
    public static final int PASSO_DA_EXECUCAO = 14;
    public static final int AVISO_DA_EXECUCAO = 10;
    public static final int AVANCO_DA_EXECUCAO = 3;
    public static final double ALCANCE_DA_EXECUCAO = 12;
    /** A que distância do alvo cada investida começa. */
    public static final double RECUO_DA_EXECUCAO = 6;
    /** As quatro juntas tiram 20: quem fica parado sem armadura não sobrevive. */
    public static final float DANO_EXECUCAO = 5;
    /** De onde vem cada investida, em graus em volta do alvo. */
    private static final double[] ANGULOS_DA_EXECUCAO = {0, 90, 45, 135};

    /** Qual das investidas está em andamento neste tick da janela (0 a 3), ou -1 depois da última. */
    public static int investidaDaExecucao(int tickNaJanela) {
        int i = tickNaJanela / PASSO_DA_EXECUCAO;
        return tickNaJanela < 0 || i >= INVESTIDAS_DA_EXECUCAO ? -1 : i;
    }

    /** O tick dentro da investida em andamento (0 a 13). */
    public static int tickDaInvestida(int tickNaJanela) {
        return tickNaJanela % PASSO_DA_EXECUCAO;
    }

    public static boolean avancaNaExecucao(int tickNaJanela) {
        int t = tickDaInvestida(tickNaJanela);
        return investidaDaExecucao(tickNaJanela) >= 0 && t >= AVISO_DA_EXECUCAO && t < AVISO_DA_EXECUCAO + AVANCO_DA_EXECUCAO;
    }

    public static double anguloDaExecucao(int investida) {
        return ANGULOS_DA_EXECUCAO[investida % ANGULOS_DA_EXECUCAO.length];
    }

    // --- Fases e entrada ---
    /** Abaixo de 35% de vida, com quem invocou já desperto, começa a fase 3. */
    public static final double VIDA_DA_FASE_3 = .35;
    public static final double RITMO_FASE_3 = .55;
    /** Ticks da animação de troca de fase, em que ele não ataca. */
    public static final int TROCA_DE_FASE = 40;
    /** Ticks da entrada (a animação "entrada"), antes do primeiro golpe. */
    public static final int ENTRADA = 40;

    /** A fase da luta: 1 até quem invocou despertar, 2 depois, 3 com o Toji abaixo de 35% de vida. */
    public static int fase(boolean despertou, double fracaoDeVida) {
        if (!despertou) return 1;
        return fracaoDeVida <= VIDA_DA_FASE_3 ? 3 : 2;
    }

    public static double ritmo(int fase) {
        return fase <= 1 ? 1 : fase == 2 ? RITMO_FASE_2 : RITMO_FASE_3;
    }

    public static final float DANO_INVESTIDA = 8;
    public static final float DANO_LANCA = 10;
    public static final float DANO_SOCO = 6;
    public static final double ALCANCE_INVESTIDA = 12;
    public static final double ALCANCE_LANCA = 6;
    public static final double ALCANCE_CORRENTE = 14;
    /** Distância em que o soco da corrente acerta, depois do puxão. */
    public static final double ALCANCE_SOCO = 3.5;

    /** Folga entre dois golpes, em ticks. */
    public static final int INTERVALO = 20;
    /** Na fase 2 as recargas e a folga ficam com 70% do tempo. */
    public static final double RITMO_FASE_2 = .7;

    /** O despertar dispara quando o Toji perde 20% da vida... */
    public static final double VIDA_DO_DESPERTAR = .8;
    /** ...ou aos 45 segundos de luta. */
    public static final int TEMPO_DO_DESPERTAR = 45 * 20;
    /** Ticks que o jogador fica caído antes de se levantar desperto. */
    public static final int QUEDA = 60;

    /** A luta acaba sozinha aos 10 minutos. */
    public static final int TEMPO_LIMITE = 10 * 60 * 20;
    /** Quem invocou não pode se afastar mais do que isto. */
    public static final double DISTANCIA_MAXIMA = 64;
    /** A que distância de quem invocou o Toji aparece. */
    public static final double DISTANCIA_DA_INVOCACAO = 8;

    private TojiBoss() {
    }

    /** O que o Contrato responde a quem tenta invocar. */
    public enum Invocacao { OK, OUTRO_PERSONAGEM, SEM_NO, JA_EXISTE }

    /**
     * @param personagem  id do personagem de quem usa o item ("" se não escolheu)
     * @param exigido     id do personagem de quem o Toji é o Nêmesis
     * @param temNo       se o nó Despertar da árvore foi comprado
     * @param temTojiVivo se esse jogador já tem um Toji no mundo
     */
    public static Invocacao podeInvocar(String personagem, String exigido, boolean temNo, boolean temTojiVivo) {
        if (!exigido.equals(personagem)) return Invocacao.OUTRO_PERSONAGEM;
        if (!temNo) return Invocacao.SEM_NO;
        if (temTojiVivo) return Invocacao.JA_EXISTE;
        return Invocacao.OK;
    }

    /** Se já é hora de o Toji derrubar quem invocou (pela vida perdida ou pelo tempo de luta). */
    public static boolean horaDeDespertar(double fracaoDeVida, int ticksDeLuta) {
        return fracaoDeVida <= VIDA_DO_DESPERTAR || ticksDeLuta >= TEMPO_DO_DESPERTAR;
    }

    /** Se um golpe com esse dano mataria quem tem essa vida: em vez de morrer, o jogador desperta. */
    public static boolean golpeMortal(float vida, float dano) {
        return dano >= vida;
    }

    /** Por que a luta acaba sem vitória. */
    public enum Fim { CONTINUA, INVOCADOR_FORA, LONGE, TEMPO }

    /**
     * @param invocadorPresente vivo, conectado e na mesma dimensão
     * @param distancia         de quem invocou até o Toji
     */
    public static Fim fim(boolean invocadorPresente, double distancia, int ticksDeLuta) {
        if (!invocadorPresente) return Fim.INVOCADOR_FORA;
        if (distancia > DISTANCIA_MAXIMA) return Fim.LONGE;
        if (ticksDeLuta >= TEMPO_LIMITE) return Fim.TEMPO;
        return Fim.CONTINUA;
    }
}
