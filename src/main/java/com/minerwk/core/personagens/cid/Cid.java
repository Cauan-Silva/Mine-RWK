package com.minerwk.core.personagens.cid;

import com.minerwk.core.kit.Arma;
import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Efeito;
import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Habilidade;
import com.minerwk.core.kit.Numeros;
import com.minerwk.core.kit.Passiva;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.kit.Trancas;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.maestria.Arvore;
import com.minerwk.core.vfx.FormasCid;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cid Kagenou, o Shadow (The Eminence in Shadow). Uma forma só: a espada de slime e a Magia. Cada golpe toca
 * a animação de mesmo id e manda os visuais {@code cid_*}, que o cliente desenha. A Espada Atômica (V) e o
 * Recovery Atomic (agachado + R) só valem depois do despertar; o I Am Atomic final (R de pé) entra na parte 6.
 */
public final class Cid {
    public static final String ID = "cid";
    /** Id do G: a recarga dele começa quando o Overdrive acaba, não quando liga. */
    public static final String OVERDRIVE = "cid_overdrive";
    /** Os três golpes que só o despertar libera: o V, o R e o R agachado. */
    public static final String ESPADA_ATOMICA = "cid_espada_atomica", ATOMIC = "cid_atomic", RECOVERY = "cid_recovery";
    /** O R e o Recovery Atomic dividem a recarga de 90 s: usar um põe o outro em recarga também. */
    public static final int RECARGA_DO_ATOMIC = 1800;
    /** A recarga do traje de slime (a passiva não tem tecla: a recarga tem id próprio). */
    public static final String TRAJE = "cid_traje";
    /** O traje segura um golpe forte a cada 12 s. */
    public static final int RECARGA_DO_TRAJE = 240;
    /** A partir deste dano, já descontada a armadura, o golpe é forte e o traje o corta pela metade. */
    static final float GOLPE_FORTE = 8;
    /** No Overdrive todo dano causado é 30% maior. */
    static final float BONUS_DO_OVERDRIVE = 1.3f;

    /** O ataque básico: a espada de slime, que alcança mais longe que uma espada comum. */
    public static final Arma ESPADA = new Arma(5, 1.6f, 3.5f);
    static final Arma ESPADA_EM_OVERDRIVE = new Arma(ESPADA.dano() * BONUS_DO_OVERDRIVE, ESPADA.velocidade(), ESPADA.alcance());

    /** Dos olhos aos pés. */
    private static final Vetor ATE_OS_PES = new Vetor(0, -1.62, 0);

    private Cid() {
    }

    public static Personagem criar() {
        Map<Tecla, Habilidade> kit = Map.of(Tecla.Z, new Redemoinho(), Tecla.X, new PosImagem(),
                Tecla.C, new Prisao(), Tecla.G, new Overdrive(), Tecla.V, new EspadaAtomica(), Tecla.R, new Atomic());
        return new Personagem(ID, "Cid Kagenou", "The Eminence in Shadow", Magia::new, kit, List.of(EspadaDeSlime::new),
                Arvore.VAZIA, new Trancas(Set.of(ESPADA_ATOMICA, ATOMIC, RECOVERY), false));
    }

    /** O dano que passa pelo traje de slime: o golpe forte, com a passiva pronta, vem pela metade. */
    public static float amortecer(float dano, boolean pronta) {
        return gastaOTraje(dano, pronta) ? dano / 2 : dano;
    }

    /** Se este golpe é o que o traje segura (e a passiva entra em recarga). */
    public static boolean gastaOTraje(float dano, boolean pronta) {
        return pronta && dano >= GOLPE_FORTE;
    }

    private static boolean emOverdrive(Contexto ctx) {
        return ctx.efeitoAtivo(EmOverdrive.class);
    }

    /** O dano de um golpe, já com o bônus do Overdrive se ele está ligado. */
    private static float dano(Contexto ctx, float base) {
        return emOverdrive(ctx) ? base * BONUS_DO_OVERDRIVE : base;
    }

    /** De onde um projétil sai: 1 bloco à frente dos olhos, ou dos próprios olhos se há parede colada. */
    private static Vetor saida(Contexto ctx) {
        Vetor frente = ctx.olhos().mais(ctx.olhar());
        return ctx.colideBloco(ctx.olhos(), frente) ? ctx.olhos() : frente;
    }

    /** Base das habilidades: id e números fixos; a animação do golpe tem o mesmo id e começa com a carga. */
    private abstract static class Golpe implements Habilidade {
        private final String id;
        private final Numeros numeros;

        Golpe(String id, Numeros numeros) {
            this.id = id;
            this.numeros = numeros;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Numeros numeros() {
            return numeros;
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            ctx.apresentacao(id, "", "");
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            ctx.pararAnimacao();
        }
    }

    /** A espada de slime na mão: o ataque básico, mais forte no Overdrive. */
    static final class EspadaDeSlime implements Passiva {
        @Override
        public void tick(Contexto ctx, EstadoDoJogador estado) {
            ctx.arma(emOverdrive(ctx) ? ESPADA_EM_OVERDRIVE : ESPADA);
        }
    }

    /** Z: o Redemoinho de Ébano. A espada estica e dá três cortes em volta dele. */
    static final class Redemoinho extends Golpe {
        static final String VISUAL = "cid_redemoinho";
        static final double RAIO = 5;
        static final float DANO = 4;
        /** Os três cortes em 12 ticks: um a cada 5, o primeiro logo no disparo. */
        static final int DURACAO = 12, ENTRE_CORTES = 5;

        Redemoinho() {
            super(VISUAL, Numeros.instantanea(2, DURACAO, 4, 15, 80));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            // O visual dura mais que o golpe: o arco do terceiro corte ainda está sumindo quando ele acaba.
            ctx.visual(VISUAL, ctx.olhos(), ctx.olhar(), (float) RAIO, FormasCid.REDEMOINHO);
            ctx.criarEfeito(new Giro());
        }
    }

    static final class Giro implements Efeito {
        @Override
        public int duracao() {
            return Redemoinho.DURACAO;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            if (t % Redemoinho.ENTRE_CORTES != 0) return;
            // O primeiro corte respeita a proteção de quem acabou de apanhar de outra coisa; os dois seguintes
            // só passam por cima da proteção do corte anterior.
            if (t == 0) ctx.danoEmArea(Redemoinho.RAIO, dano(ctx, Redemoinho.DANO));
            else ctx.danoSeguidoEmArea(Redemoinho.RAIO, dano(ctx, Redemoinho.DANO));
        }
    }

    /** X: a Pós-imagem. Ele some para a frente e a imagem fica para trás, enganando os mobs. */
    static final class PosImagem extends Golpe {
        static final String VISUAL = "cid_pos_imagem";
        static final double ALCANCE = 9;
        /** A imagem dura 2 s. */
        static final int IMAGEM = 40;

        PosImagem() {
            super(VISUAL, Numeros.instantanea(1, 1, 3, 10, 120));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor de = ctx.olhos();
            Vetor rumo = ctx.olhar();
            ctx.deslocar(rumo, ALCANCE, null);
            ctx.chamariz(de.mais(ATE_OS_PES), IMAGEM);
            ctx.visual(VISUAL, de, rumo, 0f, IMAGEM);
        }
    }

    /** C: a Prisão de Correntes Negras. Um pedaço de slime voa; em quem acertar, as correntes fecham 1 s depois. */
    static final class Prisao extends Golpe {
        static final String VISUAL = "cid_slime", CORRENTES = "cid_correntes";
        static final double ALCANCE = 16, PASSO = 1.6, RAIO = .8;
        static final float DANO = 6;
        /** Do acerto às correntes, e quanto elas seguram. */
        static final int ESPERA = 20, PRESO = 50;

        Prisao() {
            super("cid_prisao", Numeros.instantanea(3, 2, 4, 20, 240));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor de = saida(ctx);
            ctx.visual(VISUAL, de, ctx.olhar(), 0f, (int) Math.ceil(ALCANCE / PASSO) + 1);
            ctx.criarEfeito(new Slime(de, ctx.olhar(), dano(ctx, DANO)));
        }
    }

    /** O slime da Prisão: voa reto e some no primeiro inimigo ou no primeiro bloco. */
    static final class Slime implements Efeito {
        private final Vetor direcao;
        private final float dano;
        private final Set<Object> atingidos = new HashSet<>();
        private Vetor ponto;
        private double andou;
        private boolean parou;

        Slime(Vetor origem, Vetor direcao, float dano) {
            this.ponto = origem;
            this.direcao = direcao.normalizado();
            this.dano = dano;
        }

        @Override
        public int duracao() {
            return (int) Math.ceil(Prisao.ALCANCE / Prisao.PASSO) + 1;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            double p = Math.min(Prisao.PASSO, Prisao.ALCANCE - andou);
            Vetor proximo = ponto.mais(direcao.vezes(p));
            if (ctx.colideBloco(ponto, proximo)) {
                parar(ctx, ponto);
                return;
            }
            // Dois pontos por tick, o do meio primeiro: o passo é maior que o slime, e ele para no primeiro.
            Vetor meio = ponto.mais(direcao.vezes(p / 2));
            for (Vetor v : List.of(meio, proximo)) {
                if (ctx.danoUmaVez(v, Prisao.RAIO, dano, atingidos) > 0) {
                    ctx.acertou(.4f);
                    ctx.criarEfeito(new Correntes(atingidos.iterator().next()));
                    parar(ctx, v);
                    return;
                }
            }
            ponto = proximo;
            andou += p;
        }

        private void parar(Contexto ctx, Vetor onde) {
            parou = true;
            ctx.visualFim(Prisao.VISUAL, onde);
        }

        @Override
        public boolean encerrado() {
            return parou || andou >= Prisao.ALCANCE - 1e-9;
        }
    }

    /** O alvo marcado pelo slime: depois da espera, as correntes fecham onde ele estiver. */
    static final class Correntes implements Efeito {
        private final Object alvo;

        Correntes(Object alvo) {
            this.alvo = alvo;
        }

        @Override
        public int duracao() {
            return Prisao.ESPERA + 1;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            if (t < Prisao.ESPERA) return;
            Vetor onde = ctx.posicaoDe(alvo);
            if (onde == null) return;
            ctx.paralisarAlvo(alvo, Prisao.PRESO);
            ctx.visual(Prisao.CORRENTES, onde, new Vetor(0, 1, 0), 0f, Prisao.PRESO);
        }
    }

    /** G: o Overdrive. Liga por 8 s; apertar de novo desliga sem custo. */
    static final class Overdrive extends Golpe {
        static final int DURACAO = 160, RECARGA = 500;
        static final float RECUO = 4;

        Overdrive() {
            // A recarga não é a do disparo: quem a põe é o fim do efeito.
            super(OVERDRIVE, Numeros.instantanea(2, 1, 2, 0, 0));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            if (emOverdrive(ctx)) {
                ctx.encerrarEfeito(EmOverdrive.class);
                return;
            }
            // O Overdrive acabou sozinho durante a carga deste aperto: o disparo zerou a recarga que o fim
            // tinha posto. Ela volta, e nada liga (senão dava para emendar um no outro, sem recarga).
            if (ctx.efeitoAtivo(Esfriando.class)) {
                ctx.recarga(OVERDRIVE, RECARGA);
                return;
            }
            ctx.sinalAoRecurso(Magia.SEM_GANHO, DURACAO);
            ctx.visual(OVERDRIVE, ctx.olhos(), ctx.olhar(), 0f, DURACAO);
            ctx.criarEfeito(new EmOverdrive());
        }
    }

    /** O Overdrive ligado: +30% de dano (quem lê é o kit), Velocidade II e a Magia parada. */
    static final class EmOverdrive implements Efeito {
        /** A velocidade é renovada de 5 em 5 ticks e dura 10: some sozinha quando o efeito acaba. */
        private static final int RENOVAR = 5;
        private int ultimo = -1;

        @Override
        public int duracao() {
            return Overdrive.DURACAO;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            ultimo = t;
            if (t % RENOVAR != 0) return;
            ctx.velocidade(2, RENOVAR * 2);
        }

        @Override
        public void aoAcabar(Contexto ctx) {
            // Só quem leva até o fim paga o recuo; quem desliga antes sai inteiro.
            if (ultimo >= Overdrive.DURACAO - 1) ctx.recuo(Overdrive.RECUO);
            ctx.sinalAoRecurso(Magia.SEM_GANHO, 0);
            ctx.recarga(OVERDRIVE, Overdrive.RECARGA);
            ctx.visualFim(OVERDRIVE, ctx.olhos());
            ctx.criarEfeito(new Esfriando());
        }

        /** Morreu, saiu ou trocou de dimensão no meio: sem recuo, mas a recarga corre e a Magia volta a subir. */
        @Override
        public void aoDescartar(Contexto ctx) {
            ctx.sinalAoRecurso(Magia.SEM_GANHO, 0);
            ctx.recarga(OVERDRIVE, Overdrive.RECARGA);
            ctx.visualFim(OVERDRIVE, ctx.olhos());
        }
    }

    /** V: a Espada Atômica. A magia se junta na lâmina e sai num corte horizontal largo. */
    static final class EspadaAtomica extends Golpe {
        static final String CARGA = "cid_espada_carga";
        static final double ALCANCE = FormasCid.ALCANCE_DO_CORTE, ARCO = FormasCid.ARCO_DO_CORTE;
        /** O corte é horizontal: pega quem está até 3 blocos acima ou abaixo dos olhos. */
        static final double ALTURA = 3;
        static final float DANO = 16;

        EspadaAtomica() {
            super(ESPADA_ATOMICA, Numeros.instantanea(FormasCid.CARGA_DA_ESPADA, 6, 8, 30, 280));
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            super.aoIniciarCarga(ctx);
            ctx.visual(CARGA, ctx.olhos(), ctx.olhar(), 0f, FormasCid.CARGA_DA_ESPADA + 2);
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            super.aoInterromper(ctx);
            ctx.visualFim(CARGA, ctx.olhos());
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            // O arco fica deitado: a direção é a do olhar no chão.
            Vetor rumo = noChao(ctx.olhar());
            ctx.visualFim(CARGA, ctx.olhos());
            ctx.visual(ESPADA_ATOMICA, ctx.olhos(), rumo, (float) ALCANCE, FormasCid.CORTE_ATOMICO);
            ctx.tremor(.35f, 8, 16);
            if (ctx.danoEmArco(ctx.olhos(), rumo, ALCANCE, ARCO, ALTURA, dano(ctx, DANO)) > 0) ctx.acertou(.8f);
        }
    }

    /** O olhar deitado no chão, com comprimento 1; olhando reto para cima ou para baixo, o próprio olhar. */
    private static Vetor noChao(Vetor olhar) {
        Vetor v = new Vetor(olhar.x(), 0, olhar.z());
        return v.comprimento() < 1e-6 ? olhar : v.normalizado();
    }

    /**
     * R de pé: o I Am Atomic final, que entra na parte 6. Por ora a tecla só existe para o Recovery Atomic
     * (agachado) e para a recarga que os dois dividem: o custo acima do máximo da Magia impede que comece.
     */
    static final class Atomic extends Golpe {
        private final Habilidade recovery = new RecoveryAtomic();

        Atomic() {
            super(ATOMIC, Numeros.instantanea(1, 1, 1, Magia.MAXIMO + 1, RECARGA_DO_ATOMIC));
        }

        @Override
        public Habilidade agachado() {
            return recovery;
        }
    }

    /** Agachado + R: o Recovery Atomic. Crava a espada, carrega 3 s parado e cura todos em volta. */
    static final class RecoveryAtomic extends Golpe {
        static final String CARGA = "cid_recovery_carga";
        static final double RAIO = FormasCid.RAIO_DO_RECOVERY;

        RecoveryAtomic() {
            super(RECOVERY, Numeros.instantanea(FormasCid.CARGA_DO_RECOVERY, 10, 10, 60, RECARGA_DO_ATOMIC));
        }

        /** Um golpe forte (o mesmo limite do traje de slime) arranca a espada do chão. */
        @Override
        public float danoQueInterrompe() {
            return GOLPE_FORTE;
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            super.aoIniciarCarga(ctx);
            ctx.firmar(true);
            // Os pés de verdade: ele está agachado, e descer dos olhos poria o círculo debaixo do chão.
            ctx.visual(CARGA, ctx.pes(), new Vetor(0, 1, 0), (float) RAIO, FormasCid.CARGA_DO_RECOVERY + 2);
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            super.aoInterromper(ctx);
            ctx.firmar(false);
            ctx.visualFim(CARGA, ctx.pes());
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor pes = ctx.pes();
            ctx.firmar(false);
            ctx.visualFim(CARGA, pes);
            ctx.visual(RECOVERY, pes, new Vetor(0, 1, 0), (float) RAIO, FormasCid.COLUNA);
            ctx.restaurar(RAIO);
            // Se um dia a árvore encurtar a recarga do Recovery, a do R tem de acompanhar aqui.
            ctx.recarga(ATOMIC, RECARGA_DO_ATOMIC);
        }
    }

    /** Os instantes logo depois de o Overdrive acabar, em que um G que já estava em carga não o religa. */
    static final class Esfriando implements Efeito {
        @Override
        public int duracao() {
            return 6;
        }

        @Override
        public void tick(Contexto ctx, int t) {
        }
    }
}
