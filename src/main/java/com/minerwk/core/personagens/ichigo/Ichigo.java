package com.minerwk.core.personagens.ichigo;

import com.minerwk.core.kit.Arma;
import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Efeito;
import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Forma;
import com.minerwk.core.kit.Habilidade;
import com.minerwk.core.kit.Numeros;
import com.minerwk.core.kit.Passiva;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.kit.Trancas;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.vfx.FormasIchigo;
import com.minerwk.core.vfx.GetsugaIchigo;
import com.minerwk.core.maestria.Arvore;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ichigo Kurosaki (Bleach). A forma base é o Shikai; o G leva ao Bankai, que troca os golpes e gasta o
 * reiatsu até acabar. Cada golpe toca a animação de mesmo id e manda os visuais {@code ichigo_*}, que o
 * cliente desenha a partir de {@code core/vfx}. O som de cada golpe sai do visual dele ({@link SonsIchigo}).
 */
public final class Ichigo {
    public static final String ID = "ichigo";
    /** Id do G do Shikai: é ele que entra em recarga quando o Bankai acaba. */
    public static final String ENTRAR_NO_BANKAI = "ichigo_bankai";
    /** Ids da máscara Hollow e do Mugetsu: os dois golpes que só o despertar libera. */
    public static final String MASCARA = "ichigo_mascara", MUGETSU = "ichigo_mugetsu";
    /** Depois do Mugetsu: 60 s sem golpes e sem reiatsu. */
    public static final int SEM_PODERES = 1200;
    /** Com a máscara, todo dano causado é 30% maior. */
    static final float BONUS_DA_MASCARA = 1.3f;
    /** Recarga do Bankai para quem saiu por vontade (5 s) e para quem ficou sem reiatsu (10 s). */
    static final int RECARGA_AO_SAIR = 100, RECARGA_ESGOTADO = 200;
    /** O Shunpo do Bankai com alvo: a quantos blocos atrás dele o Ichigo surge. */
    static final double ATRAS_DO_ALVO = 1.5;
    /** Dos olhos do Ichigo aos pés, menos do meio do alvo aos pés dele: para os dois ficarem no mesmo chão. */
    private static final double OLHOS_SOBRE_O_MEIO = 0.72;

    /** O ataque básico em cada forma, e com o Getsuga na lâmina. */
    public static final Arma ZANGETSU = new Arma(7, 1.4f, 0);
    public static final Arma TENSA_ZANGETSU = new Arma(6, 2.2f, 0);
    public static final Arma TENSA_CARREGADA = new Arma(12, 2.2f, 5);
    /** As duas com a máscara Hollow no rosto. */
    static final Arma TENSA_MASCARADA = reforcada(TENSA_ZANGETSU), TENSA_CARREGADA_MASCARADA = reforcada(TENSA_CARREGADA);

    private static Arma reforcada(Arma a) {
        return new Arma(a.dano() * BONUS_DA_MASCARA, a.velocidade(), a.alcance());
    }

    /** Dos olhos aos pés: os visuais de chão nascem nos pés do Ichigo. */
    private static final Vetor ATE_OS_PES = new Vetor(0, -1.62, 0);

    private Ichigo() {
    }

    public static Personagem criar() {
        Map<Tecla, Habilidade> shikai = Map.of(Tecla.Z, new GetsugaTensho(), Tecla.X, new Shunpo(),
                Tecla.C, new GetsugaNoChao(), Tecla.V, new Arremesso(), Tecla.G, new Bankai());
        Forma bankai = new Forma(Reiatsu.BANKAI, Map.of(Tecla.Z, new GetsugaNegro(), Tecla.X, new ShunpoDoBankai(),
                Tecla.C, new GetsugaNaLamina(), Tecla.V, new MascaraHollow(), Tecla.G, new SairDoBankai(),
                Tecla.R, new Mugetsu()));
        return new Personagem(ID, "Ichigo Kurosaki", "Bleach", Reiatsu::new, shikai, List.of(Zangetsu::new),
                Arvore.VAZIA, new Trancas(Set.of(MASCARA, MUGETSU), false), List.of(bankai));
    }

    private static boolean noBankai(Contexto ctx) {
        return Reiatsu.BANKAI.equals(ctx.forma());
    }

    private static boolean mascarado(Contexto ctx) {
        return ctx.efeitoAtivo(MascaraAtiva.class);
    }

    /** O dano de um golpe, já com o bônus da máscara se ela está no rosto. */
    private static float dano(Contexto ctx, float base) {
        return mascarado(ctx) ? base * BONUS_DA_MASCARA : base;
    }

    /** De onde o Getsuga sai: 1 bloco à frente dos olhos, ou dos próprios olhos se há parede colada. */
    private static Vetor saida(Contexto ctx) {
        Vetor frente = ctx.olhos().mais(ctx.olhar());
        return ctx.colideBloco(ctx.olhos(), frente) ? ctx.olhos() : frente;
    }

    private static Vetor pes(Contexto ctx) {
        return ctx.olhos().mais(ATE_OS_PES);
    }

    /** O Bankai se desfazendo em volta do corpo (ao sair por vontade ou sem reiatsu). */
    private static void desfazer(Contexto ctx) {
        ctx.visual("ichigo_sair", ctx.olhos(), ctx.olhar(), 0f, FormasIchigo.SAIR);
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

        protected String animacao() {
            return id;
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            ctx.apresentacao(animacao(), "", "");
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            ctx.pararAnimacao();
        }
    }

    /**
     * A espada na mão: o ataque básico é o da forma. É também quem encerra o Bankai quando o reiatsu acaba.
     */
    static final class Zangetsu implements Passiva {
        @Override
        public void tick(Contexto ctx, EstadoDoJogador estado) {
            // O Mugetsu gasta tudo e ele mesmo tira do Bankai: no meio dele, zero de reiatsu não encerra a forma.
            if (noBankai(ctx) && ctx.recurso() <= 1e-6 && !MUGETSU.equals(ctx.golpeEmAndamento())) {
                ctx.mudarForma(Forma.BASE);
                ctx.recarga(ENTRAR_NO_BANKAI, RECARGA_ESGOTADO);
                desfazer(ctx);
            }
            ctx.arma(!noBankai(ctx) ? ZANGETSU : mascarado(ctx) ? TENSA_MASCARADA : TENSA_ZANGETSU);
        }
    }

    /** Z do Shikai: segura para o corte sair mais forte; a meia-lua atravessa os inimigos. */
    static final class GetsugaTensho extends Golpe {
        static final String VISUAL = "ichigo_getsuga";

        GetsugaTensho() {
            super(VISUAL, new Numeros(2, 10, 2, 6, true, 20, 80));
        }

        /** O reiatsu sobe pelo corpo enquanto a tecla está segurada. */
        @Override
        public void aoIniciarCarga(Contexto ctx) {
            super.aoIniciarCarga(ctx);
            ctx.visual("ichigo_carga", ctx.olhos(), ctx.olhar(), 0f, numeros().cargaMax() + 6);
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            super.aoInterromper(ctx);
            ctx.visualFim("ichigo_carga", ctx.olhos());
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor de = saida(ctx);
            ctx.visualFim("ichigo_carga", ctx.olhos());
            ctx.apresentacao("ichigo_getsuga_disparo", "", "");
            ctx.visual(VISUAL, de, ctx.olhar(), forca, GetsugaIchigo.duracao(false));
            ctx.criarEfeito(new MeiaLua(de, ctx.olhar(), GetsugaIchigo.PASSO_SHIKAI, GetsugaIchigo.ALCANCE_SHIKAI,
                    8 + 4 * forca, VISUAL));
        }
    }

    /** Z do Bankai: sai na hora, mais rápido e mais longe. */
    static final class GetsugaNegro extends Golpe {
        static final String VISUAL = "ichigo_getsuga_negro";
        static final float LARGURA_COM_MASCARA = 1.5f;

        GetsugaNegro() {
            super(VISUAL, Numeros.instantanea(2, 2, 4, 15, 30));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor de = saida(ctx);
            // Com a máscara o corte é 50% mais largo; a força do visual leva a largura ao cliente.
            float largura = mascarado(ctx) ? LARGURA_COM_MASCARA : 1f;
            ctx.visual(VISUAL, de, ctx.olhar(), largura, GetsugaIchigo.duracao(true));
            ctx.criarEfeito(new MeiaLua(de, ctx.olhar(), GetsugaIchigo.PASSO_BANKAI, GetsugaIchigo.ALCANCE_BANKAI,
                    dano(ctx, 10), VISUAL, MeiaLua.RAIO * largura));
        }
    }

    /** A meia-lua do Getsuga: voa reto, fere cada inimigo uma vez e só para em bloco sólido. */
    static final class MeiaLua implements Efeito {
        /** Meia largura do corte (3 blocos de ponta a ponta). */
        static final double RAIO = 1.5;
        private final Vetor direcao;
        private final double passo, alcance, raio;
        private final float dano;
        /** O visual que acompanha esta meia-lua: é encerrado se ela bater num bloco. */
        private final String visual;
        private final Set<Object> atingidos = new HashSet<>();
        private Vetor ponto;
        private double andou;
        private boolean parou;

        MeiaLua(Vetor origem, Vetor direcao, double passo, double alcance, float dano, String visual) {
            this(origem, direcao, passo, alcance, dano, visual, RAIO);
        }

        MeiaLua(Vetor origem, Vetor direcao, double passo, double alcance, float dano, String visual, double raio) {
            this.raio = raio;
            this.ponto = origem;
            this.direcao = direcao.normalizado();
            this.passo = passo;
            this.alcance = alcance;
            this.dano = dano;
            this.visual = visual;
        }

        @Override
        public int duracao() {
            return (int) Math.ceil(alcance / passo) + 1;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            double p = Math.min(passo, alcance - andou);
            Vetor proximo = ponto.mais(direcao.vezes(p));
            if (ctx.colideBloco(ponto, proximo)) {
                parou = true;
                ctx.visualFim(visual, ponto);
                return;
            }
            // Dois pontos por tick: o passo é maior que o corte, e ninguém pode ficar no vão.
            int feridos = ctx.danoUmaVez(ponto.mais(direcao.vezes(p / 2)), raio, dano, atingidos)
                    + ctx.danoUmaVez(proximo, raio, dano, atingidos);
            if (feridos > 0) ctx.acertou(.5f);
            ponto = proximo;
            andou += p;
        }

        @Override
        public boolean encerrado() {
            return parou || andou >= alcance - 1e-9;
        }
    }

    /** X do Shikai: o passo rápido, em linha reta até 8 blocos. */
    static final class Shunpo extends Golpe {
        Shunpo() {
            super("ichigo_shunpo", Numeros.instantanea(1, 1, 3, 15, 140));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor de = ctx.olhos();
            ctx.deslocar(ctx.olhar(), 8, null);
            ctx.visual("ichigo_shunpo", de, ctx.olhos().menos(de), 0f, FormasIchigo.SHUNPO);
        }
    }

    /** X do Bankai: mais longe e quase sem recarga; com um inimigo na mira, surge atrás dele. */
    static final class ShunpoDoBankai extends Golpe {
        static final double ALCANCE = 14, LARGURA_DA_MIRA = 1.5;

        ShunpoDoBankai() {
            super("ichigo_shunpo_bankai", Numeros.instantanea(1, 1, 2, 10, 40));
        }

        @Override
        protected String animacao() {
            return "ichigo_shunpo";
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor olhos = ctx.olhos();
            Object alvo = ctx.marcarAlvo(ALCANCE, LARGURA_DA_MIRA);
            Vetor onde = alvo == null ? null : ctx.posicaoDe(alvo);
            if (onde == null) {
                ctx.deslocar(ctx.olhar(), ALCANCE, null);
            } else {
                Vetor ate = onde.menos(olhos);
                Vetor reto = new Vetor(ate.x(), 0, ate.z()).normalizado();
                if (reto.comprimento() < .5) reto = new Vetor(ctx.olhar().x(), 0, ctx.olhar().z()).normalizado();
                Vetor caminho = new Vetor(ate.x(), ate.y() + OLHOS_SOBRE_O_MEIO, ate.z()).mais(reto.vezes(ATRAS_DO_ALVO));
                ctx.deslocar(caminho.normalizado(), caminho.comprimento(), onde);
            }
            ctx.visual("ichigo_shunpo_bankai", olhos, ctx.olhos().menos(olhos), 1f, FormasIchigo.SHUNPO);
        }
    }

    /** C do Shikai: crava o Getsuga no chão; a onda fere, afasta e desfaz projéteis em volta. */
    static final class GetsugaNoChao extends Golpe {
        static final double RAIO = FormasIchigo.ONDA_RAIO;

        GetsugaNoChao() {
            super("ichigo_getsuga_chao", Numeros.instantanea(8, 2, 6, 30, 240));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor centro = ctx.olhos();
            ctx.danoEm(centro, RAIO, 6);
            ctx.empurrar(centro, RAIO, 1.2);
            ctx.desfazerProjeteis(centro, RAIO);
            ctx.tremor(.4f, 8, 10);
            ctx.visual("ichigo_onda", pes(ctx), ctx.olhar(), 1f, FormasIchigo.ONDA);
        }
    }

    /** V do Shikai: gira a Zangetsu pelo pano e a lança; enquanto ela não volta, o Ichigo está sem espada. */
    static final class Arremesso extends Golpe {
        Arremesso() {
            super("ichigo_arremesso", Numeros.instantanea(6, Lancada.IDA, Lancada.IDA, 20, 200));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.arma(Arma.MAO);
            ctx.visual("ichigo_arremesso", ctx.olhos(), ctx.olhar(), 1f, FormasIchigo.ARREMESSO);
            ctx.criarEfeito(new Lancada(ctx.olhos(), ctx.olhar()));
        }
    }

    /** A Zangetsu fora da mão: vai até 10 blocos ferindo quem estiver no caminho e volta pelo pano. */
    static final class Lancada implements Efeito {
        static final int IDA = FormasIchigo.ARREMESSO_IDA;
        static final double PASSO = FormasIchigo.ARREMESSO_PASSO, RAIO = 1.2;
        private final Vetor origem, direcao;
        private final Set<Object> atingidos = new HashSet<>();
        private double andou;
        private boolean bateu;

        Lancada(Vetor origem, Vetor direcao) {
            this.origem = origem;
            this.direcao = direcao.normalizado();
        }

        @Override
        public int duracao() {
            return IDA * 2;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            ctx.arma(Arma.MAO);
            if (t < IDA && !bateu) {
                Vetor de = origem.mais(direcao.vezes(andou));
                Vetor ate = origem.mais(direcao.vezes(andou + PASSO));
                if (ctx.colideBloco(de, ate)) {
                    bateu = true;
                } else {
                    andou += PASSO;
                    if (ctx.danoUmaVez(ate, RAIO, 10, atingidos) > 0) ctx.acertou(.5f);
                }
            } else if (t >= IDA) {
                // A volta: a espada vem de onde parou até a mão, sem ferir.
                andou = Math.max(0, andou - andou / (duracao() - t));
            }
        }
    }

    /** G do Shikai: a liberação. Afasta quem está perto, sem ferir, e passa para o Bankai. */
    static final class Bankai extends Golpe {
        Bankai() {
            super(ENTRAR_NO_BANKAI, Numeros.instantanea(30, 4, 6, 50, 0));
        }

        /** O reiatsu é puxado para o corpo durante a carga; some se a liberação for cortada. */
        @Override
        public void aoIniciarCarga(Contexto ctx) {
            super.aoIniciarCarga(ctx);
            ctx.visual("ichigo_bankai_carga", ctx.olhos(), ctx.olhar(), 0f, numeros().cargaMax() + 6);
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            super.aoInterromper(ctx);
            ctx.visualFim("ichigo_bankai_carga", ctx.olhos());
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor centro = ctx.olhos();
            ctx.empurrar(centro, 6, 1.5);
            ctx.tremor(.6f, 12, 14);
            ctx.visualFim("ichigo_bankai_carga", centro);
            ctx.visual("ichigo_bankai", pes(ctx), ctx.olhar(), 1f, FormasIchigo.BANKAI);
            ctx.mudarForma(Reiatsu.BANKAI);
        }
    }

    /** G do Bankai: volta ao Shikai por vontade; o Bankai fica um tempo sem poder voltar. */
    static final class SairDoBankai extends Golpe {
        SairDoBankai() {
            super("ichigo_sair_bankai", Numeros.instantanea(4, 1, 4, 0, 20));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            if (!ctx.mudarForma(Forma.BASE)) return;
            ctx.recarga(ENTRAR_NO_BANKAI, RECARGA_AO_SAIR);
            desfazer(ctx);
        }
    }

    /** C do Bankai: o Getsuga fica na lâmina, e por 6 s o ataque básico alcança mais e fere mais. */
    static final class GetsugaNaLamina extends Golpe {
        GetsugaNaLamina() {
            super("ichigo_getsuga_lamina", Numeros.instantanea(4, 2, 4, 30, 280));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.arma(mascarado(ctx) ? TENSA_CARREGADA_MASCARADA : TENSA_CARREGADA);
            ctx.visual(LaminaCarregada.VISUAL, ctx.olhos(), ctx.olhar(), 1f, LaminaCarregada.DURACAO);
            ctx.criarEfeito(new LaminaCarregada());
        }
    }

    /** O Getsuga na lâmina; acaba antes se o Ichigo sair do Bankai. */
    static final class LaminaCarregada implements Efeito {
        static final int DURACAO = 120;
        static final String VISUAL = "ichigo_lamina";
        private boolean acabou;

        @Override
        public int duracao() {
            return DURACAO;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            if (!noBankai(ctx)) {
                acabou = true;
                ctx.visualFim(VISUAL, ctx.olhos());
                return;
            }
            ctx.arma(mascarado(ctx) ? TENSA_CARREGADA_MASCARADA : TENSA_CARREGADA);
        }

        @Override
        public boolean encerrado() {
            return acabou;
        }
    }

    /**
     * V do Bankai (trancado): veste a máscara Hollow. Por 11 s todo dano causado é 30% maior, o Getsuga negro
     * fica mais largo e o Bankai para de drenar.
     */
    static final class MascaraHollow extends Golpe {
        /** A mão passa pelo rosto: a máscara só vale no fim do gesto. */
        static final int GESTO = 8;

        MascaraHollow() {
            super(MASCARA, Numeros.instantanea(GESTO, 2, 6, 40, 800));
        }

        /** A animação do gesto entra com a apresentação da máscara; por enquanto ele não toca nenhuma. */
        @Override
        protected String animacao() {
            return "";
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.sinalAoRecurso(Reiatsu.SEM_DRENO, MascaraAtiva.DURACAO);
            ctx.visual(MascaraAtiva.VISUAL, ctx.olhos(), ctx.olhar(), 1f, MascaraAtiva.DURACAO);
            ctx.criarEfeito(new MascaraAtiva());
        }
    }

    /** A máscara no rosto. Quem pergunta por ela são os golpes e o ataque básico; acaba antes se o Bankai acabar. */
    static final class MascaraAtiva implements Efeito {
        static final int DURACAO = 220;
        static final String VISUAL = "ichigo_mascara";
        private boolean acabou;

        @Override
        public int duracao() {
            return DURACAO;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            if (noBankai(ctx)) return;
            acabou = true;
            ctx.visualFim(VISUAL, ctx.olhos());
        }

        @Override
        public boolean encerrado() {
            return acabou;
        }
    }

    /**
     * R do Bankai (trancado): o Getsuga Tensho final. A carga é a cutscene; no corte ele gasta todo o reiatsu,
     * solta o véu e perde os poderes por 60 s.
     */
    static final class Mugetsu extends Golpe {
        /** A cutscene: 6 segundos da transformação ao corte. */
        static final int CARGA = 120;
        /** Dentro da carga: quando a forma final aparece e quando tudo se cala antes do corte. */
        static final int TRANSFORMA = 30, SILENCIO = 84;
        static final String VISUAL_DA_CARGA = "ichigo_mugetsu_carga", VISUAL = "ichigo_mugetsu";
        /** Os golpes do Shikai, que ficam em recarga enquanto ele está sem poderes. */
        private static final List<String> DO_SHIKAI = List.of("ichigo_getsuga", "ichigo_shunpo", "ichigo_getsuga_chao",
                "ichigo_arremesso", ENTRAR_NO_BANKAI);

        Mugetsu() {
            super(MUGETSU, Numeros.instantanea(CARGA, 2, 20, 100, 3600));
        }

        /** As animações da forma final entram com a cutscene; por enquanto ele não toca nenhuma. */
        @Override
        protected String animacao() {
            return "";
        }

        /** Um golpe forte durante a cutscene corta o Mugetsu, como no R do Gojo. */
        @Override
        public float danoQueInterrompe() {
            return 6f;
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            super.aoIniciarCarga(ctx);
            // A carga dura 6 s: sem a pausa, o Bankai drenaria o reiatsu de que o corte precisa.
            ctx.sinalAoRecurso(Reiatsu.SEM_DRENO, CARGA + 2);
            ctx.visual(VISUAL_DA_CARGA, ctx.olhos(), ctx.olhar(), 0f, CARGA + 6);
            ctx.cutscene(MUGETSU);
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            super.aoInterromper(ctx);
            ctx.sinalAoRecurso(Reiatsu.SEM_DRENO, 0);
            ctx.visualFim(VISUAL_DA_CARGA, ctx.olhos());
            ctx.encerrarCutscene();
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            // Reto, no nível do chão: o véu sobe dele.
            Vetor rumo = new Vetor(ctx.olhar().x(), 0, ctx.olhar().z());
            rumo = rumo.comprimento() < 1e-6 ? new Vetor(0, 0, 1) : rumo.normalizado();
            ctx.visualFim(VISUAL_DA_CARGA, ctx.olhos());
            ctx.visual(VISUAL, pes(ctx), rumo, 1f, Veu.TICKS + Veu.RESTO);
            ctx.tremor(.8f, 20, 24);
            // O dano é medido antes de os poderes irem embora (a máscara ainda conta).
            ctx.criarEfeito(new Veu(ctx.olhos(), rumo, dano(ctx, Veu.DANO)));
            perderOsPoderes(ctx);
        }

        /** O custo da obra: de volta ao Shikai, sem reiatsu e com todos os golpes parados por 60 s. */
        private static void perderOsPoderes(Contexto ctx) {
            ctx.gastarRecurso(ctx.recurso());
            ctx.mudarForma(Forma.BASE);
            ctx.sinalAoRecurso(Reiatsu.SELAR, SEM_PODERES);
            for (String golpe : DO_SHIKAI) ctx.recarga(golpe, SEM_PODERES);
            ctx.criarEfeito(new SemPoderes());
        }
    }

    /**
     * Os 60 s sem poderes, só para avisar: mostra o reiatsu indo embora logo depois do corte e voltando no
     * fim, onde o Ichigo estiver. Quem tira e devolve os poderes de fato são as recargas e o selo do reiatsu,
     * que são salvos; este efeito não é, então quem sai e entra no jogo perde só o aviso.
     */
    static final class SemPoderes implements Efeito {
        /** Ticks depois do corte em que o reiatsu vai embora (o véu já passou), e quanto cada aviso dura. */
        static final int AVISO = 30, TICKS_DO_AVISO = 30;

        @Override
        public int duracao() {
            return SEM_PODERES;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            if (t == AVISO) ctx.visual("ichigo_poderes_perdidos", ctx.olhos(), ctx.olhar(), 1f, TICKS_DO_AVISO);
        }

        @Override
        public void aoAcabar(Contexto ctx) {
            ctx.visual("ichigo_poderes_voltam", ctx.olhos(), ctx.olhar(), 1f, TICKS_DO_AVISO);
        }
    }

    /**
     * O véu do Mugetsu: uma parede de escuridão de 9 blocos de largura que avança 40 blocos em linha reta.
     * Atravessa tudo (não para em bloco nem quebra nada) e fere cada inimigo uma vez, sem contar a armadura.
     */
    static final class Veu implements Efeito {
        static final float DANO = 40;
        static final double ALCANCE = 40, MEIA_LARGURA = 4.5, PASSO = 2;
        /** Ticks do avanço, e os do céu voltando ao normal depois (só no visual). */
        static final int TICKS = (int) (ALCANCE / PASSO), RESTO = 30;
        private final Vetor origem, rumo;
        private final float dano;
        private final Set<Object> atingidos = new HashSet<>();

        Veu(Vetor origem, Vetor rumo, float dano) {
            this.origem = origem;
            this.rumo = rumo;
            this.dano = dano;
        }

        @Override
        public int duracao() {
            return TICKS;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            // Dois pontos por tick: o passo é de 2 blocos e a faixa tem de ficar sem vãos.
            double ate = (t + 1) * PASSO;
            int feridos = ctx.danoDiretoUmaVez(origem.mais(rumo.vezes(ate - PASSO / 2)), MEIA_LARGURA, dano, atingidos)
                    + ctx.danoDiretoUmaVez(origem.mais(rumo.vezes(ate)), MEIA_LARGURA, dano, atingidos);
            if (feridos > 0) ctx.acertou(1f);
        }
    }
}
