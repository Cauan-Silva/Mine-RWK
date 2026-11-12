package com.minerwk.core.boss;

import com.minerwk.core.vfx.FormasIchigo;
import com.minerwk.core.vfx.GetsugaIchigo;
import java.util.List;

/**
 * Os números e as regras do Zangetsu interior (o Ichigo branco, Nêmesis do Ichigo), sem classes do
 * Minecraft. O que cada golpe faz no mundo fica na entidade; aqui ficam os tempos, os danos e as decisões.
 */
public final class ZangetsuBoss {
    public static final String ID = "zangetsu";
    /** 13 barras de coração. */
    public static final float VIDA = 260;
    public static final int XP_VITORIA = 500;

    /** Corte de perto, num setor à frente; a animação "corte" dura 20 ticks. */
    public static final Golpe CORTE = new Golpe("corte", "corte", 10, 2, 8, 30, 0, 3.5, 4);
    /** Getsuga branco de bordas vermelhas, pela linha marcada no chão; "getsuga" dura 38 ticks. */
    public static final Golpe GETSUGA = new Golpe("getsuga", "getsuga", 16, 12, 10, 70, 5, 26, 3);
    /** Com a máscara: três em sequência, cada um mirado de novo; "getsuga_3" dura 62 ticks. */
    public static final Golpe GETSUGA_3 = new Golpe("getsuga_3", "getsuga_3", 12, 36, 14, 100, 5, 26, 3);
    /** Shunpo para trás do alvo e corte; "shunpo" dura 26 ticks. */
    public static final Golpe SHUNPO = new Golpe("shunpo", "shunpo", 14, 2, 10, 110, 0, 20, 2);
    /** A espada girada pela corrente e arremessada: vai e volta; "arremesso" dura 38 ticks. */
    public static final Golpe ARREMESSO = new Golpe("arremesso", "arremesso", 14, 16, 8, 120, 3, 11, 2);
    /** Cero carmesim pela mão esquerda; "cero" dura 42 ticks. */
    public static final Golpe CERO = new Golpe("cero", "cero", 24, 4, 14, 150, 6, 22, 2);

    public static final List<Golpe> GOLPES = List.of(CORTE, GETSUGA, SHUNPO, ARREMESSO, CERO);
    public static final List<Golpe> GOLPES_DA_MASCARA = List.of(CORTE, GETSUGA_3, SHUNPO, ARREMESSO, CERO);

    /** Os golpes de cada fase: na 2 (com a máscara) o Getsuga sai em sequência de três. */
    public static List<Golpe> golpes(int fase) {
        return fase <= 1 ? GOLPES : GOLPES_DA_MASCARA;
    }

    // --- Fases ---
    /** Abaixo de 40% de vida ele veste a máscara. */
    public static final double VIDA_DA_MASCARA = .4;
    /** Com a máscara as recargas e a folga ficam com 65% do tempo, e ele anda mais rápido. */
    public static final double RITMO_DA_MASCARA = .65;
    public static final double PASSO_NORMAL = 1.15, PASSO_DA_MASCARA = 1.45;
    /** Ticks da animação de vestir a máscara, em que ele não ataca, e quando ela aparece no rosto. */
    public static final int TROCA_DE_FASE = 40, MASCARA_APARECE = 12;
    /** Ticks da entrada, antes do primeiro golpe. */
    public static final int ENTRADA = 40;
    /** Ticks que ele espera antes da entrada: o jogador acabou de trocar de dimensão e ainda está carregando o cenário. */
    public static final int CHEGADA = 40;
    /** Folga entre dois golpes. */
    public static final int INTERVALO = 18;

    public static int fase(double fracaoDeVida) {
        return fracaoDeVida <= VIDA_DA_MASCARA ? 2 : 1;
    }

    public static double ritmo(int fase) {
        return fase <= 1 ? 1 : RITMO_DA_MASCARA;
    }

    // --- Corte ---
    public static final float DANO_CORTE = 6;
    public static final double RAIO_CORTE = 3.2, ANGULO_CORTE = 130;

    // --- Getsuga ---
    /** A meia-lua anda como o Getsuga negro do jogador: é o mesmo visual, em branco. */
    public static final double PASSO_GETSUGA = GetsugaIchigo.PASSO_BANKAI, ALCANCE_GETSUGA = GetsugaIchigo.ALCANCE_BANKAI;
    public static final double RAIO_GETSUGA = 1.5;
    public static final float DANO_GETSUGA = 9;
    /** Ticks entre um Getsuga e o próximo na sequência de três, e quanto antes de cada um a linha reaparece. */
    public static final int ENTRE_GETSUGAS = 12, MIRA_DO_GETSUGA = 6;

    public static int getsugas(Golpe golpe) {
        return golpe == GETSUGA_3 ? 3 : 1;
    }

    /** Se uma meia-lua sai neste tick da janela. */
    public static boolean getsugaSai(Golpe golpe, int tickNaJanela) {
        if (tickNaJanela < 0 || tickNaJanela % ENTRE_GETSUGAS != 0) return false;
        return tickNaJanela / ENTRE_GETSUGAS < getsugas(golpe);
    }

    /** Se neste tick da janela ele mira de novo e a linha do próximo Getsuga aparece (só na sequência). */
    public static boolean getsugaMira(Golpe golpe, int tickNaJanela) {
        if (golpe != GETSUGA_3 || tickNaJanela < 0) return false;
        return tickNaJanela % ENTRE_GETSUGAS == ENTRE_GETSUGAS - MIRA_DO_GETSUGA
                && tickNaJanela / ENTRE_GETSUGAS + 1 < getsugas(golpe);
    }

    // --- Shunpo ---
    /** Tick do golpe em que ele surge atrás do alvo; dali até a janela, a sombra no chão avisa. */
    public static final int TICK_DE_SURGIR = 6;
    public static final double ATRAS_DO_ALVO = 1.6;
    public static final float DANO_SHUNPO = 8;
    public static final double RAIO_SHUNPO = 2.6;

    // --- Arremesso ---
    public static final double ALCANCE_ARREMESSO = FormasIchigo.ARREMESSO_ALCANCE, RAIO_ARREMESSO = 1.3;
    public static final float DANO_ARREMESSO = 7;

    /** A que distância da saída a espada está neste tick da janela, indo até {@code alcance} e voltando. */
    public static double espadaEm(int tickNaJanela, double alcance) {
        return FormasIchigo.arremesso(tickNaJanela + 1, alcance);
    }

    // --- Cero ---
    public static final double ALCANCE_CERO = 22, RAIO_CERO = 1.0;
    public static final float DANO_CERO = 12;
    /** Nos últimos ticks do aviso a mira trava: dá para sair da linha. */
    public static final int CERO_TRAVA = 8;
    /** Quanto o raio fica na tela. */
    public static final int CERO_NA_TELA = 10;

    // --- O despertar de quem invocou ---
    /** Ticks que o jogador fica caído antes de se levantar de máscara. */
    public static final int QUEDA = 60;
    /** A luta acaba sozinha aos 10 minutos. */
    public static final int TEMPO_LIMITE = 10 * 60 * 20;
    public static final double ALCANCE_DE_VISAO = 96;

    private ZangetsuBoss() {
    }

    /** Se um golpe com esse dano mataria quem tem essa vida: em vez de morrer, o jogador veste a máscara. */
    public static boolean golpeMortal(float vida, float dano) {
        return dano >= vida;
    }

    /** @param invocadorPresente vivo, conectado e na mesma dimensão */
    public static boolean acabou(boolean invocadorPresente, int ticksDeLuta) {
        return !invocadorPresente || ticksDeLuta >= TEMPO_LIMITE;
    }
}
