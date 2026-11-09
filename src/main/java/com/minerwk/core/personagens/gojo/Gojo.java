package com.minerwk.core.personagens.gojo;

import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Efeito;
import com.minerwk.core.kit.Habilidade;
import com.minerwk.core.kit.Numeros;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.kit.Vetor;
import java.util.List;
import java.util.Map;

/**
 * Satoru Gojo (Jujutsu Kaisen). Kit aprovado em vault/Personagens/Gojo.md; números em ticks.
 * Apresentação provisória: animações gojo_* e som de teste até a parte visual.
 */
public final class Gojo {
    public static final String ID = "gojo";
    private static final String SOM = "teste";

    private Gojo() {
    }

    public static Personagem criar() {
        return new Personagem(ID, "Satoru Gojo", "Jujutsu Kaisen", EnergiaAmaldicoada::new,
                Map.of(Tecla.Z, new Azul(), Tecla.X, new Vermelho(), Tecla.C, new Infinito(),
                        Tecla.V, new Roxo(), Tecla.G, new VazioInfinito(), Tecla.R, new Roxo200()),
                List.of(TecnicaReversa::new));
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
    }

    /** Z: ponto de atração a 6 blocos (ou no primeiro bloco), por 1,5 s. */
    public static final class Azul extends Golpe {
        Azul() {
            super("gojo_azul", Numeros.instantanea(4, 2, 4, 20, 120));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.apresentacao("gojo_azul", "", SOM);
            ctx.criarEfeito(new CampoAzul(ctx.alvoAte(6)));
        }
    }

    static final class CampoAzul implements Efeito {
        private final Vetor centro;

        CampoAzul(Vetor centro) {
            this.centro = centro;
        }

        @Override
        public int duracao() {
            return 30;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            ctx.puxar(centro, 5, 0.25);
            if (t % 10 == 0) ctx.danoEm(centro, 2, 2f);
            ctx.particulas("portal", centro, 8);
        }
    }

    /** X: segura para carregar; a esfera explode ao colidir ou depois de 30 blocos. */
    public static final class Vermelho extends Golpe {
        Vermelho() {
            super("gojo_vermelho", new Numeros(5, 30, 2, 6, true, 30, 200));
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            ctx.apresentacao("gojo_vermelho", "", "");
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.apresentacao("", "", SOM);
            Vetor dir = ctx.olhar();
            ctx.criarEfeito(new EsferaVermelha(ctx.olhos().mais(dir), dir, forca));
        }
    }

    static final class EsferaVermelha implements Efeito {
        static final double VELOCIDADE = 1.5;
        private final Vetor direcao;
        private final float forca;
        private Vetor posicao;
        private boolean colidiu;

        EsferaVermelha(Vetor inicio, Vetor direcao, float forca) {
            this.posicao = inicio;
            this.direcao = direcao;
            this.forca = forca;
        }

        @Override
        public int duracao() {
            return 20; // 30 blocos
        }

        @Override
        public void tick(Contexto ctx, int t) {
            Vetor proxima = posicao.mais(direcao.vezes(VELOCIDADE));
            colidiu = ctx.colide(posicao, proxima);
            posicao = proxima;
            ctx.particulas("flame", posicao, 6);
        }

        @Override
        public boolean encerrado() {
            return colidiu;
        }

        @Override
        public void aoAcabar(Contexto ctx) {
            ctx.danoEm(posicao, 3, 6 + 8 * forca);
            ctx.empurrar(posicao, 4, 1.5 + 1.5 * forca);
            ctx.tremor(0.3f + 0.5f * forca, 12, 16);
            ctx.particulas("explosion", posicao, 1);
        }
    }

    /** C: Infinito por até 6 s, gastando 1,5 de energia por tick. */
    public static final class Infinito extends Golpe {
        Infinito() {
            super("gojo_infinito", Numeros.instantanea(2, 1, 2, 40, 500));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.apresentacao("gojo_infinito", "", SOM);
            ctx.criarEfeito(new InfinitoAtivo());
        }
    }

    static final class InfinitoAtivo implements Efeito {
        static final double DRENO = 1.5;
        private boolean semEnergia;

        @Override
        public int duracao() {
            return 120;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            if (!ctx.gastarRecurso(DRENO)) {
                semEnergia = true;
                return;
            }
            // Renova por 2 ticks: se o efeito acabar, o Infinito cai logo em seguida.
            ctx.infinito(2);
            ctx.empurrar(ctx.olhos(), 2, 0.3);
            if (t % 5 == 0) ctx.particulas("end_rod", ctx.olhos(), 2);
        }

        @Override
        public boolean encerrado() {
            return semEnergia;
        }
    }

    /** V: Roxo, 1,5 s de carga; avança 40 blocos apagando o caminho. */
    public static final class Roxo extends Golpe {
        Roxo() {
            super("gojo_roxo", Numeros.instantanea(30, 2, 8, 90, 900));
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            ctx.apresentacao("gojo_roxo", "", "");
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.apresentacao("", "", SOM);
            ctx.tremor(0.6f, 15, 24);
            ctx.criarEfeito(new FeixeRoxo(ctx.olhos(), ctx.olhar(), 2, 20, 1.5, 30f));
        }
    }

    /** R: Roxo 200%, 4 s de encantamento; cai com golpe de 6 ou mais na carga. */
    public static final class Roxo200 extends Golpe {
        Roxo200() {
            super("gojo_roxo_200", Numeros.instantanea(80, 2, 10, 200, 6000));
        }

        @Override
        public float danoQueInterrompe() {
            return 6f;
        }

        @Override
        public void aoIniciarCarga(Contexto ctx) {
            ctx.apresentacao("gojo_roxo", "", "");
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.apresentacao("", "", SOM);
            ctx.tremor(1.2f, 30, 64);
            ctx.criarEfeito(new FeixeRoxo(ctx.olhos(), ctx.olhar(), 4, 30, 2.5, 60f));
        }
    }

    /** A massa do Roxo: a cada tick avança {@code passo} blocos, apaga e fere em volta do ponto. */
    static final class FeixeRoxo implements Efeito {
        private final Vetor origem;
        private final Vetor direcao;
        private final double passo;
        private final int passos;
        private final double raio;
        private final float dano;

        FeixeRoxo(Vetor origem, Vetor direcao, double passo, int passos, double raio, float dano) {
            this.origem = origem;
            this.direcao = direcao;
            this.passo = passo;
            this.passos = passos;
            this.raio = raio;
            this.dano = dano;
        }

        @Override
        public int duracao() {
            return passos;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            Vetor ponto = origem.mais(direcao.vezes(passo * (t + 1)));
            ctx.apagarBlocos(ponto, raio);
            ctx.danoEm(ponto, raio, dano);
            ctx.particulas("witch", ponto, 12);
        }
    }

    /** G: Vazio Infinito, domínio fixo de raio 12 por 8 s. */
    public static final class VazioInfinito extends Golpe {
        VazioInfinito() {
            super("gojo_vazio_infinito", Numeros.instantanea(10, 2, 10, 200, 3600));
        }

        @Override
        public void aoDisparar(Contexto ctx, float forca) {
            ctx.apresentacao("gojo_dominio", "", SOM);
            ctx.tremor(0.5f, 20, 24);
            ctx.criarEfeito(new Dominio(ctx.olhos()));
        }
    }

    static final class Dominio implements Efeito {
        static final double RAIO = 12;
        private final Vetor centro;

        Dominio(Vetor centro) {
            this.centro = centro;
        }

        @Override
        public int duracao() {
            return 160;
        }

        @Override
        public void tick(Contexto ctx, int t) {
            ctx.paralisar(centro, RAIO, 5);
            if (t % 10 == 0) ctx.particulas("end_rod", centro, 40);
        }
    }
}
