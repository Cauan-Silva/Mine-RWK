package com.minerwk.core.personagens.gojo;

import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Efeito;
import com.minerwk.core.kit.Habilidade;
import com.minerwk.core.kit.Numeros;
import com.minerwk.core.maestria.Melhorias;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.kit.Trancas;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.vfx.TrajetoriaGojo;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Satoru Gojo (Jujutsu Kaisen). Kit aprovado em vault/Personagens/Gojo.md; números em ticks.
 * Apresentação provisória: animações gojo_* e som de teste até a parte visual.
 */
public final class Gojo {
    public static final String ID = "gojo";
    /** Animações do R depois do avanço: o uppercut e o resto, ou só as mãos abrindo quando não há alvo. */
    public static final String ANIM_R_UPPERCUT = "gojo_roxo_maximo";
    public static final String ANIM_R_SEM_ALVO = "gojo_roxo_maximo_sem_alvo";
    /** Pose da descida lenta depois do R, em loop até pousar. */
    public static final String ANIM_R_DESCIDA = "gojo_roxo_maximo_descida";
    /** Altura dos olhos de um jogador em pé, para achar os pés a partir dos olhos. */
    static final double ALTURA_OLHOS = 1.62;

    private Gojo() {
    }

    public static Personagem criar() {
        return new Personagem(ID, "Satoru Gojo", "Jujutsu Kaisen", EnergiaAmaldicoada::new,
                Map.of(Tecla.Z, new Azul(), Tecla.X, new Vermelho(), Tecla.C, new Infinito(),
                        Tecla.V, new Roxo(), Tecla.G, new VazioInfinito(), Tecla.R, new Roxo200()),
                List.of(TecnicaReversa::new), ArvoreGojo.ARVORE,
                // Como na obra: antes do Toji ele só tem o Azul e o Infinito.
                new Trancas(java.util.Set.of(Tecla.X, Tecla.V, Tecla.G, Tecla.R), true));
    }

    /** Base das habilidades do Gojo: id e números fixos. */
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

        /** A animação do golpe (mesmo id) começa com a carga e cobre o golpe inteiro. */
        @Override
        public void aoIniciarCarga(Contexto ctx) {
            ctx.apresentacao(id, "", somDaCarga());
        }

        /** Som que começa junto com a carga (vazio = nenhum); para se a carga for interrompida. */
        protected String somDaCarga() {
            return "";
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            ctx.pararAnimacao();
        }
    }

    /** Z: ponto de atração a 6 blocos (ou no primeiro bloco), por 3 s; arranca blocos leves do chão. */
    public static final class Azul extends Golpe {
        Azul() {
            super("gojo_azul", Numeros.instantanea(4, 2, 4, 20, 160));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            Vetor centro = ctx.alvoAte(6);
            ctx.visual("gojo_azul", centro, ctx.olhar(), 1f, CampoAzul.DURACAO);
            ctx.criarEfeito(new CampoAzul(centro, CampoAzul.RAIO * ctx.melhorias().mais(ArvoreGojo.AZUL_ALCANCE)));
        }
    }

    static final class CampoAzul implements Efeito {
        private final Vetor centro;
        private final double raio;

        CampoAzul(Vetor centro) {
            this(centro, RAIO);
        }

        CampoAzul(Vetor centro, double raio) {
            this.centro = centro;
            this.raio = raio;
        }

        static final int DURACAO = 60;
        static final double RAIO = 6;
        /** Blocos arrancados por tick e no campo inteiro. */
        static final int ARRANCA_POR_TICK = 2, ARRANCA_MAX = 40;
        static final float DUREZA_MAX = .6f;
        private int arrancados;

        @Override
        public int duracao() {
            return DURACAO;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            ctx.puxar(centro, raio, 0.25);
            if (t % 15 == 0) ctx.danoEm(centro, 2, 2f);
            int vez = Math.min(ARRANCA_POR_TICK, ARRANCA_MAX - arrancados);
            if (vez > 0) arrancados += Math.max(0, ctx.arrancarBlocos(centro, raio, vez, DUREZA_MAX));
        }
    }

    /** X: segura para carregar; a esfera explode ao colidir ou depois de 30 blocos. */
    public static final class Vermelho extends Golpe {
        Vermelho() {
            super("gojo_vermelho", new Numeros(5, 30, 2, 6, true, 30, 200));
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            super.aoIniciarCarga(ctx);
            ctx.visual("gojo_vermelho_carga", ctx.olhos(), ctx.olhar(), 0f,
                    ctx.melhorias().ajustar(id(), numeros()).cargaMax());
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            super.aoInterromper(ctx);
            ctx.visualFim("gojo_vermelho_carga", ctx.olhos());
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            // A carga tem tamanho variável: o disparo sempre pula para a pose do coice.
            ctx.apresentacao("gojo_vermelho_disparo", "", SonsGojo.VERMELHO_DISPARO);
            Vetor dir = ctx.olhar();
            Vetor inicio = ctx.olhos().mais(dir);
            ctx.visualFim("gojo_vermelho_carga", ctx.olhos());
            ctx.visual("gojo_vermelho", inicio, dir, forca, EsferaVermelha.DURACAO);
            ctx.criarEfeito(new EsferaVermelha(inicio, dir, forca, (float) ctx.melhorias().mais(ArvoreGojo.VERMELHO_FORCA)));
        }
    }

    static final class EsferaVermelha implements Efeito {
        static final int DURACAO = 30; // 30 blocos a 1 por tick
        private final Vetor inicio;
        private final Vetor direcao;
        private final float forca;
        private Vetor posicao;
        private boolean colidiu;

        private final float fatorDano;

        EsferaVermelha(Vetor inicio, Vetor direcao, float forca) {
            this(inicio, direcao, forca, 1f);
        }

        EsferaVermelha(Vetor inicio, Vetor direcao, float forca, float fatorDano) {
            this.fatorDano = fatorDano;
            this.inicio = inicio;
            this.posicao = inicio;
            this.direcao = direcao;
            this.forca = forca;
        }

        @Override
        public int duracao() {
            return DURACAO;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            Vetor proxima = TrajetoriaGojo.vermelho(inicio, direcao, t + 1);
            colidiu = ctx.colide(posicao, proxima);
            posicao = proxima;
        }

        @Override
        public boolean encerrado() {
            return colidiu;
        }

        @Override
        public void aoAcabar(Contexto ctx) {
            int feridos = ctx.danoUmaVez(posicao, 3, (6 + 8 * forca) * fatorDano, new HashSet<>());
            ctx.arremessar(posicao, 4, 2.5 + 1.5 * forca, 3, null);
            if (feridos > 0) ctx.acertou(.5f + .5f * forca);
            ctx.tremor(0.3f + 0.5f * forca, 12, 16);
            ctx.visualFim("gojo_vermelho", posicao);
        }
    }

    /** C: Infinito por até 6 s, gastando 1,5 de energia por tick. */
    public static final class Infinito extends Golpe {
        Infinito() {
            super("gojo_infinito", Numeros.instantanea(2, 1, 2, 40, 500));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.apresentacao("", "", SonsGojo.INFINITO_ATIVA);
            int duracao = Melhorias.escala(InfinitoAtivo.DURACAO, ctx.melhorias().mais(ArvoreGojo.INFINITO_DURACAO));
            ctx.visual("gojo_infinito", ctx.olhos(), ctx.olhar(), 0f, duracao);
            ctx.criarEfeito(new InfinitoAtivo(duracao));
        }
    }

    static final class InfinitoAtivo implements Efeito {
        static final double DRENO = 1.5;
        static final int DURACAO = 120;
        private final int duracao;
        private boolean semEnergia;

        InfinitoAtivo() {
            this(DURACAO);
        }

        InfinitoAtivo(int duracao) {
            this.duracao = duracao;
        }

        @Override
        public int duracao() {
            return duracao;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            if (!ctx.gastarRecurso(DRENO)) {
                if (!semEnergia) ctx.visualFim("gojo_infinito", ctx.olhos());
                semEnergia = true;
                return;
            }
            // Renova por 2 ticks: se o efeito acabar, o Infinito cai logo em seguida.
            ctx.infinito(2);
            ctx.empurrar(ctx.olhos(), 2, 0.3);
        }

        @Override
        public boolean encerrado() {
            return semEnergia;
        }

        @Override
        public boolean anulavel() {
            return true;
        }

        @Override
        public void aoAcabar(Contexto ctx) {
            ctx.apresentacao("", "", SonsGojo.INFINITO_DESATIVA);
        }
    }

    /** V: Roxo, 1,5 s de carga; avança 40 blocos em 2 s apagando o caminho. */
    public static final class Roxo extends Golpe {
        Roxo() {
            super("gojo_roxo", Numeros.instantanea(30, 2, 8, 90, 900));
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            super.aoIniciarCarga(ctx);
            ctx.visual("gojo_roxo_carga", ctx.olhos(), ctx.olhar(), 0f, numeros().cargaMax());
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            super.aoInterromper(ctx);
            ctx.visualFim("gojo_roxo_carga", ctx.olhos());
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.apresentacao("", "", SonsGojo.ROXO_DISPARO);
            ctx.tremor(0.6f, 15, 24);
            ctx.visual("gojo_roxo", ctx.olhos(), ctx.olhar(), 1f, 40);
            ctx.criarEfeito(new FeixeRoxo(ctx.olhos(), ctx.olhar(), TrajetoriaGojo.PASSO_ROXO, 40, 1.5,
                    30f * (float) ctx.melhorias().mais(ArvoreGojo.ROXO_FORCA), 2, 4));
        }
    }

    /** R: Roxo 200%, 4 s de encantamento; cai com golpe de 6 ou mais na carga. */
    public static final class Roxo200 extends Golpe {
        /**
         * Ticks de voo depois da carga: a sequência mais longa, com folga; no fim dela o voo vira a
         * descida ({@link Contexto#descida}), que dura até ele pousar (no máximo 30 s: a explosão pode abrir
         * uma cratera bem abaixo de onde o Gojo começou).
         */
        static final int VOO_DEPOIS = RoxoMaximo.fim(RoxoMaximo.AVANCO_MAX) + 20;
        /** Teto da descida lenta; ela acaba antes, quando o Gojo pousa. */
        static final int DESCIDA_MAX = 600;

        Roxo200() {
            super("gojo_roxo_200", Numeros.instantanea(80, 2, 10, 200, 6000));
        }

        @Override
        public float danoQueInterrompe() {
            return 6f;
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            super.aoIniciarCarga(ctx);
            // Fica no chão no encantamento; o voo cobre a sequência (o cliente move) e a descida lenta.
            ctx.voo(numeros().cargaMax() + VOO_DEPOIS);
            ctx.visual("gojo_roxo_200_carga", ctx.olhos(), ctx.olhar(), 0f, numeros().cargaMax());
            ctx.cutscene(CutsceneRoxo200.ID);
        }

        @Override
        public void aoInterromper(Contexto ctx) {
            super.aoInterromper(ctx);
            ctx.visualFim("gojo_roxo_200_carga", ctx.olhos());
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.tremor(1.2f, 30, 64);
            ctx.criarEfeito(new ComboRoxoMaximo(ctx));
        }
    }

    /**
     * O R depois do encantamento ({@link RoxoMaximo}): com alvo na mira, avança, dá o uppercut, os dois
     * sobem, o Azul e o Vermelho se fundem atrás do Gojo e o Roxo explode engolindo os dois. Sem alvo,
     * as esferas vão até a mira e explodem lá.
     */
    static final class ComboRoxoMaximo implements Efeito {
        private final Object alvo;
        private final Vetor olhos;
        private final Vetor direcao;
        private final double distancia;
        private final int avanco;
        private final java.util.Set<Object> atingidos = new HashSet<>();
        private final Vetor baseAlvo;
        private Vetor centro;
        /** Id da parte 2 da cutscene e a base dela (frente na horizontal para o alvo, ou para a mira). */
        private final String cutscene;
        private final Vetor baseCutscene;

        ComboRoxoMaximo(Contexto ctx) {
            olhos = ctx.olhos();
            Object marcado = ctx.marcarAlvo(RoxoMaximo.ALCANCE, RoxoMaximo.LARGURA);
            Vetor onde = marcado == null ? null : ctx.posicaoDe(marcado);
            alvo = onde == null ? null : marcado;
            if (alvo != null) {
                Vetor ate = onde.menos(olhos);
                distancia = ate.comprimento();
                direcao = distancia > 1e-6 ? ate.vezes(1 / distancia) : ctx.olhar();
                avanco = RoxoMaximo.avanco(distancia);
                baseAlvo = onde;
                cutscene = CutsceneRoxoMaximo.id(avanco);
                baseCutscene = new Vetor(direcao.x(), 0, direcao.z());
            } else {
                direcao = ctx.olhar();
                distancia = 0;
                avanco = 0;
                centro = ctx.alvoAte(RoxoMaximo.ALCANCE);
                baseAlvo = null;
                Vetor ate = centro.menos(olhos);
                cutscene = CutsceneRoxoMaximo.idSemAlvo(ate.comprimento());
                baseCutscene = ate.comprimento() > 1e-6 ? ate : direcao;
            }
        }

        @Override
        public int duracao() {
            return alvo != null ? RoxoMaximo.fim(avanco)
                    : RoxoMaximo.TICKS_ESFERAS_SEM_ALVO + RoxoMaximo.TICKS_FUSAO + RoxoMaximo.TICKS_EXPLOSAO;
        }

        @Override
        public void aoAcabar(Contexto ctx) {
            ctx.descida(ANIM_R_DESCIDA, Roxo200.DESCIDA_MAX);
        }

        @Override
        public void tick(Contexto ctx, int t) {
            if (t == 0) ctx.cutscene(cutscene, olhos, baseCutscene);
            if (alvo == null) {
                if (t == 0) {
                    ctx.apresentacao(ANIM_R_SEM_ALVO, "", "");
                    ctx.visual("gojo_roxo_maximo_esferas", centro, direcao, RoxoMaximo.TICKS_ESFERAS_SEM_ALVO,
                            RoxoMaximo.TICKS_ESFERAS_SEM_ALVO + RoxoMaximo.TICKS_FUSAO);
                }
                if (t == RoxoMaximo.TICKS_ESFERAS_SEM_ALVO + RoxoMaximo.TICKS_FUSAO) explodir(ctx);
                return;
            }
            // O cliente move o Gojo pela mesma conta (avanço e subida) a partir deste visual.
            if (t == 0) ctx.visual("gojo_roxo_maximo", olhos, direcao, (float) distancia, RoxoMaximo.fim(avanco));
            if (t == avanco) {
                // O alvo é segurado onde o Gojo bate (a posição da mira), mesmo se correu no avanço.
                ctx.apresentacao(ANIM_R_UPPERCUT, "", "");
                ctx.cutsceneParaAlvo(alvo, cutscene, olhos, baseCutscene, avanco);
                ctx.ferir(alvo, RoxoMaximo.DANO_UPPERCUT);
                ctx.acertou(1.5f);
                ctx.tremor(1f, 10, 32);
            }
            if (t >= avanco && t < RoxoMaximo.inicioExplosao(avanco)) {
                ctx.suspender(alvo, baseAlvo.mais(new Vetor(0, RoxoMaximo.alturaAlvo(t - avanco + 1), 0)), 2);
            }
            if (t == RoxoMaximo.inicioEsferas(avanco)) {
                // Quem move o Gojo é o cliente, e o servidor pode estar atrasado: o centro sai da mesma conta
                // do movimento (onde ele está no fim da subida), não da posição que o servidor tem agora.
                Vetor noAlto = olhos.mais(RoxoMaximo.deslocamentoGojo(RoxoMaximo.inicioEsferas(avanco), direcao, distancia));
                centro = RoxoMaximo.centroFusao(noAlto, direcao);
                ctx.visual("gojo_roxo_maximo_esferas", centro, direcao, RoxoMaximo.TICKS_ESFERAS,
                        RoxoMaximo.TICKS_ESFERAS + RoxoMaximo.TICKS_FUSAO);
            }
            if (t == RoxoMaximo.inicioExplosao(avanco)) {
                // O alvo marcado sempre leva o golpe, mesmo se tiver escapado do raio.
                if (atingidos.add(alvo)) ctx.ferir(alvo, RoxoMaximo.DANO);
                explodir(ctx);
            }
        }

        private void explodir(Contexto ctx) {
            ctx.criarEfeito(new ExplosaoRoxa(centro, direcao, RoxoMaximo.DANO, atingidos,
                    RoxoMaximo.RAIO_EXPLOSAO, RoxoMaximo.TICKS_EXPLOSAO));
        }
    }

    /**
     * A massa do Roxo: a cada tick avança {@code passo} blocos, apaga e fere em volta do ponto. Cada alvo
     * leva o dano uma vez só; quem sobrevive é jogado para fora do caminho.
     */
    static final class FeixeRoxo implements Efeito {
        private final Vetor origem;
        private final Vetor direcao;
        private final double passo;
        private final int passos;
        private final double raio;
        private final float dano;
        private final double arremesso;
        private final int congelar;
        private final java.util.Set<Object> atingidos = new HashSet<>();
        private boolean avisouAcerto;

        FeixeRoxo(Vetor origem, Vetor direcao, double passo, int passos, double raio, float dano,
                  double arremesso, int congelar) {
            this.origem = origem;
            this.direcao = direcao;
            this.passo = passo;
            this.passos = passos;
            this.raio = raio;
            this.dano = dano;
            this.arremesso = arremesso;
            this.congelar = congelar;
        }

        @Override
        public int duracao() {
            return passos;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            Vetor ponto = TrajetoriaGojo.roxo(origem, direcao, passo, t);
            ctx.apagarBlocos(ponto, raio);
            if (ctx.danoUmaVez(ponto, raio, dano, atingidos) > 0) {
                ctx.arremessar(ponto, raio, arremesso, congelar, direcao);
                if (!avisouAcerto) ctx.acertou(dano >= 60 ? 1.5f : 1f);
                avisouAcerto = true;
            }
        }
    }

    /** Fim do R: a explosão cresce casca por casca até {@code raio} em {@code ticks}, ferindo cada alvo uma vez. */
    static final class ExplosaoRoxa implements Efeito {
        private final double raio;
        private final int ticks;
        private final Vetor centro;
        private final Vetor direcao;
        private final float dano;
        /** Os mesmos alvos da massa: quem já levou o golpe não leva de novo. */
        private final java.util.Set<Object> atingidos;

        ExplosaoRoxa(Vetor centro, Vetor direcao, float dano, java.util.Set<Object> atingidos, double raio, int ticks) {
            this.raio = raio;
            this.ticks = ticks;
            this.centro = centro;
            this.direcao = direcao;
            this.dano = dano;
            this.atingidos = atingidos;
        }

        @Override
        public int duracao() {
            return ticks;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            if (t == 0) {
                // A forca do visual leva o raio para o cliente.
                ctx.visual("gojo_roxo_explosao", centro, direcao, (float) raio, ticks + RoxoMaximo.TICKS_RESTO);
                ctx.tremor(1.8f, ticks + 10, 96);
            }
            double de = raio * RoxoMaximo.fracaoExplosao((double) t / ticks);
            double ate = raio * RoxoMaximo.fracaoExplosao((double) (t + 1) / ticks);
            ctx.apagarCasca(centro, de, ate);
            if (ctx.danoUmaVez(centro, ate, dano, atingidos) > 0) ctx.arremessar(centro, ate, 6, 4, null);
        }
    }

    /** G: Vazio Infinito, domínio fixo de raio 12 por 8 s. */
    public static final class VazioInfinito extends Golpe {
        VazioInfinito() {
            super("gojo_vazio_infinito", Numeros.instantanea(10, 2, 10, 200, 3600));
        }

        @Override
        protected String somDaCarga() {
            return SonsGojo.DOMINIO;
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.tremor(0.5f, 20, 24);
            int duracao = Melhorias.escala(Dominio.DURACAO, ctx.melhorias().mais(ArvoreGojo.DOMINIO_DURACAO));
            ctx.visual("gojo_vazio_infinito", ctx.olhos(), ctx.olhar(), 0f, duracao);
            // O domínio é fechado e tem chão próprio, no nível dos pés: ninguém sai, entra ou cai.
            ctx.barreira(ctx.olhos(), Dominio.RAIO, ctx.olhos().y() - ALTURA_OLHOS, duracao);
            ctx.criarEfeito(new Dominio(ctx.olhos(), duracao));
        }
    }

    static final class Dominio implements Efeito {
        static final double RAIO = 12;
        static final int DURACAO = 160;
        private final Vetor centro;
        private final int duracao;

        Dominio(Vetor centro) {
            this(centro, DURACAO);
        }

        Dominio(Vetor centro, int duracao) {
            this.centro = centro;
            this.duracao = duracao;
        }

        @Override
        public int duracao() {
            return duracao;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            ctx.paralisar(centro, RAIO, 5);
        }
    }
}
