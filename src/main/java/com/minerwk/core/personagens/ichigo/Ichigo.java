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
import com.minerwk.core.maestria.Arvore;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ichigo Kurosaki (Bleach). A forma base é o Shikai; o G leva ao Bankai, que troca os golpes e gasta o
 * reiatsu até acabar. A apresentação aqui é provisória (partículas): modelos, animações e VFX entram depois.
 */
public final class Ichigo {
    public static final String ID = "ichigo";
    /** Id do G do Shikai: é ele que entra em recarga quando o Bankai acaba. */
    public static final String ENTRAR_NO_BANKAI = "ichigo_bankai";
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

    /** Partículas provisórias: azul-claro no Shikai, preto e vermelho no Bankai. */
    private static final String AZUL = "soul_fire_flame", PRETO = "squid_ink", VERMELHO = "flame", PASSO = "cloud",
            CORTE = "sweep_attack";

    private Ichigo() {
    }

    public static Personagem criar() {
        Map<Tecla, Habilidade> shikai = Map.of(Tecla.Z, new GetsugaTensho(), Tecla.X, new Shunpo(),
                Tecla.C, new GetsugaNoChao(), Tecla.V, new Arremesso(), Tecla.G, new Bankai());
        Forma bankai = new Forma(Reiatsu.BANKAI, Map.of(Tecla.Z, new GetsugaNegro(), Tecla.X, new ShunpoDoBankai(),
                Tecla.C, new GetsugaNaLamina(), Tecla.G, new SairDoBankai()));
        return new Personagem(ID, "Ichigo Kurosaki", "Bleach", Reiatsu::new, shikai, List.of(Zangetsu::new),
                Arvore.VAZIA, Trancas.NENHUMA, List.of(bankai));
    }

    private static boolean noBankai(Contexto ctx) {
        return Reiatsu.BANKAI.equals(ctx.forma());
    }

    /** De onde o Getsuga sai: 1 bloco à frente dos olhos, ou dos próprios olhos se há parede colada. */
    private static Vetor saida(Contexto ctx) {
        Vetor frente = ctx.olhos().mais(ctx.olhar());
        return ctx.colideBloco(ctx.olhos(), frente) ? ctx.olhos() : frente;
    }

    /** Base das habilidades: id e números fixos. */
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
    }

    /**
     * A espada na mão: o ataque básico é o da forma. É também quem encerra o Bankai quando o reiatsu acaba.
     */
    static final class Zangetsu implements Passiva {
        @Override
        public void tick(Contexto ctx, EstadoDoJogador estado) {
            if (noBankai(ctx) && ctx.recurso() <= 1e-6) {
                ctx.mudarForma(Forma.BASE);
                ctx.recarga(ENTRAR_NO_BANKAI, RECARGA_ESGOTADO);
                ctx.particulas(PRETO, ctx.olhos(), 30);
            }
            ctx.arma(noBankai(ctx) ? TENSA_ZANGETSU : ZANGETSU);
        }
    }

    /** Z do Shikai: segura para o corte sair mais forte; a meia-lua atravessa os inimigos. */
    static final class GetsugaTensho extends Golpe {
        GetsugaTensho() {
            super("ichigo_getsuga", new Numeros(2, 10, 2, 6, true, 20, 80));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.criarEfeito(new MeiaLua(saida(ctx), ctx.olhar(), 1.6, 24, 8 + 4 * forca, AZUL));
        }
    }

    /** Z do Bankai: sai na hora, mais rápido e mais longe. */
    static final class GetsugaNegro extends Golpe {
        GetsugaNegro() {
            super("ichigo_getsuga_negro", Numeros.instantanea(2, 2, 4, 15, 30));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.criarEfeito(new MeiaLua(saida(ctx), ctx.olhar(), 2.4, 28, 10, PRETO));
        }
    }

    /** A meia-lua do Getsuga: voa reto, fere cada inimigo uma vez e só para em bloco sólido. */
    static final class MeiaLua implements Efeito {
        /** Meia largura do corte (3 blocos de ponta a ponta). */
        static final double RAIO = 1.5;
        private final Vetor direcao;
        private final double passo, alcance;
        private final float dano;
        private final String particula;
        private final Set<Object> atingidos = new HashSet<>();
        private Vetor ponto;
        private double andou;
        private boolean parou;

        MeiaLua(Vetor origem, Vetor direcao, double passo, double alcance, float dano, String particula) {
            this.ponto = origem;
            this.direcao = direcao.normalizado();
            this.passo = passo;
            this.alcance = alcance;
            this.dano = dano;
            this.particula = particula;
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
                ctx.particulas(particula, ponto, 20);
                return;
            }
            // Dois pontos por tick: o passo é maior que o corte, e ninguém pode ficar no vão.
            int feridos = ctx.danoUmaVez(ponto.mais(direcao.vezes(p / 2)), RAIO, dano, atingidos)
                    + ctx.danoUmaVez(proximo, RAIO, dano, atingidos);
            if (feridos > 0) ctx.acertou(.5f);
            ponto = proximo;
            andou += p;
            ctx.particulas(particula, ponto, 8);
            ctx.particulas(PRETO.equals(particula) ? VERMELHO : CORTE, ponto, 2);
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
            ctx.particulas(PASSO, ctx.olhos(), 10);
            ctx.deslocar(ctx.olhar(), 8, null);
            ctx.particulas(PASSO, ctx.olhos(), 10);
        }
    }

    /** X do Bankai: mais longe e quase sem recarga; com um inimigo na mira, surge atrás dele. */
    static final class ShunpoDoBankai extends Golpe {
        static final double ALCANCE = 14, LARGURA_DA_MIRA = 1.5;

        ShunpoDoBankai() {
            super("ichigo_shunpo_bankai", Numeros.instantanea(1, 1, 2, 10, 40));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor olhos = ctx.olhos();
            ctx.particulas(PRETO, olhos, 10);
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
            ctx.particulas(PRETO, ctx.olhos(), 10);
        }
    }

    /** C do Shikai: crava o Getsuga no chão; a onda fere, afasta e desfaz projéteis em volta. */
    static final class GetsugaNoChao extends Golpe {
        static final double RAIO = 5;

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
            ctx.particulas(AZUL, centro, 60);
            ctx.particulas("explosion", centro, 3);
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
            ctx.criarEfeito(new Lancada(ctx.olhos(), ctx.olhar()));
        }
    }

    /** A Zangetsu fora da mão: vai até 10 blocos ferindo quem estiver no caminho e volta pelo pano. */
    static final class Lancada implements Efeito {
        static final int IDA = 8;
        static final double PASSO = 10.0 / IDA, RAIO = 1.2;
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
            ctx.particulas(CORTE, origem.mais(direcao.vezes(andou)), 1);
            ctx.particulas("crit", origem.mais(direcao.vezes(andou / 2)), 3);
        }
    }

    /** G do Shikai: a liberação. Afasta quem está perto, sem ferir, e passa para o Bankai. */
    static final class Bankai extends Golpe {
        Bankai() {
            super(ENTRAR_NO_BANKAI, Numeros.instantanea(30, 4, 6, 50, 0));
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            ctx.particulas(AZUL, ctx.olhos(), 40);
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor centro = ctx.olhos();
            ctx.empurrar(centro, 6, 1.5);
            ctx.tremor(.6f, 12, 14);
            ctx.particulas(PRETO, centro, 80);
            ctx.particulas(VERMELHO, centro, 30);
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
            ctx.particulas(PRETO, ctx.olhos(), 30);
        }
    }

    /** C do Bankai: o Getsuga fica na lâmina, e por 6 s o ataque básico alcança mais e fere mais. */
    static final class GetsugaNaLamina extends Golpe {
        GetsugaNaLamina() {
            super("ichigo_getsuga_lamina", Numeros.instantanea(4, 2, 4, 30, 280));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.arma(TENSA_CARREGADA);
            ctx.criarEfeito(new LaminaCarregada());
        }
    }

    /** O Getsuga na lâmina; acaba antes se o Ichigo sair do Bankai. */
    static final class LaminaCarregada implements Efeito {
        static final int DURACAO = 120;
        private boolean acabou;

        @Override
        public int duracao() {
            return DURACAO;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            if (!noBankai(ctx)) {
                acabou = true;
                return;
            }
            ctx.arma(TENSA_CARREGADA);
            if (t % 4 == 0) {
                ctx.particulas(PRETO, ctx.olhos().mais(ctx.olhar()), 2);
                ctx.particulas(VERMELHO, ctx.olhos().mais(ctx.olhar()), 1);
            }
        }

        @Override
        public boolean encerrado() {
            return acabou;
        }
    }
}
