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
    public static final List<Golpe> GOLPES = List.of(INVESTIDA, LANCA, CORRENTE);

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
