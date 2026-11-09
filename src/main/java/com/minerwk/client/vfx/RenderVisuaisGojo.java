package com.minerwk.client.vfx;

import com.minerwk.client.ConfigCliente;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.vfx.CoroaRoxoGojo;
import com.minerwk.core.vfx.DescargaVermelhoGojo;
import com.minerwk.core.vfx.FluxoAzulGojo;
import com.minerwk.core.vfx.PerfilVfxGojo;
import com.minerwk.core.vfx.TrajetoriaGojo;
import com.minerwk.core.vfx.VazioInfinitoGojo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Desenha os visuais do Gojo (direção de arte do Minezin, adaptada à mecânica do Mine-RWK).
 * Tudo relativo à câmera, depois das partículas. As formas vêm de {@code core/vfx}.
 */
public final class RenderVisuaisGojo {
    private static final int CIANO = 0x3CC8FF;
    private static final int CARMESIM = 0xFF2A28;
    private static final int VIOLETA = 0xB060FF;
    private static final int BRANCO_AZULADO = 0x8CCCFF;
    /** Visuais mais longe que isso da câmera não são desenhados. */
    private static final double ALCANCE = 160;

    private RenderVisuaisGojo() {
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        VisuaisAtivos.tick();
    }

    public static void aoRenderizar(RenderLevelStageEvent evento) {
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || VisuaisAtivos.ativos().isEmpty()) return;
        float parcial = evento.getPartialTick().getGameTimeDeltaPartialTick(false);
        long agora = mc.level.getGameTime();
        Quadro q = new Quadro(evento.getCamera().getPosition(), parcial, agora,
                (float) ((agora % 24000) + parcial) / 20f, PerfilVfxGojo.detalhe(ConfigCliente.vfxReduzido()));
        for (VisuaisAtivos.Visual v : List.copyOf(VisuaisAtivos.ativos())) {
            float op = v.opacidade(agora, parcial);
            if (op <= .004f) continue;
            double t = v.idade(agora, parcial);
            try {
                desenhar(q, v, t, op);
            } catch (RuntimeException erro) {
                // Um visual quebrado não derruba o jogo: some, e o estado de render volta ao padrão.
                Pincel.restaurar();
                VisuaisAtivos.ativos().remove(v);
                com.minerwk.MineRWK.LOGGER.warn("Visual {} falhou e foi removido", v.tipo, erro);
            }
        }
    }

    private static void desenhar(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        {
            switch (v.tipo) {
                case "gojo_azul" -> azul(q, v, t, op);
                case "gojo_vermelho_carga" -> vermelhoCarga(q, v, t, op);
                case "gojo_vermelho" -> vermelho(q, v, t, op);
                case "gojo_vermelho_explosao" -> explosao(q, v, t);
                case "gojo_infinito" -> infinito(q, v, op);
                case "gojo_roxo_carga" -> roxoCarga(q, v, t, op, 2, 30, 1);
                case "gojo_roxo_200_carga" -> roxoCarga(q, v, t, op, 4, 80, 1.7);
                case "gojo_roxo" -> roxo(q, v, t, op, 2, 1.9);
                case "gojo_roxo_200" -> roxo(q, v, t, op, 4, 3.2);
                case "gojo_vazio_infinito" -> dominio(q, v, t, op);
                default -> {
                }
            }
        }
    }

    /** O que vale para todos os visuais neste quadro. */
    private record Quadro(Vec3 camera, float parcial, long agora, float tempo, PerfilVfxGojo.Detalhe detalhe) {
        Vec3 rel(Vetor p) {
            return new Vec3(p.x() - camera.x, p.y() - camera.y, p.z() - camera.z);
        }

        Vec3 rel(Vec3 p) {
            return p.subtract(camera);
        }

        boolean longe(Vec3 relativo) {
            return relativo.lengthSqr() > ALCANCE * ALCANCE;
        }

        int segmentos(int completo) {
            return detalhe.faixas() < 64 ? Math.max(8, completo / 2) : completo;
        }
    }

    /** Base ortonormal a partir de uma direção: lateral, vertical e frente. */
    private record Base(Vec3 lado, Vec3 cima, Vec3 frente) {
        static Base de(Vetor d) {
            Vec3 f = new Vec3(d.x(), d.y(), d.z());
            f = f.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : f.normalize();
            Vec3 l = f.cross(new Vec3(0, 1, 0));
            if (l.lengthSqr() < 1e-6) l = new Vec3(1, 0, 0);
            l = l.normalize();
            return new Base(l, l.cross(f).normalize(), f);
        }

        Vec3 aplicar(Vetor p) {
            return lado.scale(p.x()).add(cima.scale(p.y())).add(frente.scale(p.z()));
        }

        /** Para formas em (x, y, z) do mundo relativas ao centro (Azul), sem girar. */
        static Vec3 direto(Vetor p) {
            return new Vec3(p.x(), p.y(), p.z());
        }
    }

    private static Vec3 vec(Vetor v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    private static Entity dono(VisuaisAtivos.Visual v) {
        return Minecraft.getInstance().level.getEntity(v.dono);
    }

    /** Ponto na frente da mão direita do dono (o visual segue o jogador enquanto carrega). */
    private static Vec3 mao(Entity e, float parcial) {
        Vec3 olho = e.getEyePosition(parcial);
        Vec3 olhar = e.getViewVector(parcial);
        Vec3 lado = olhar.cross(new Vec3(0, 1, 0));
        lado = lado.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : lado.normalize();
        return olho.add(olhar.scale(.7)).add(lado.scale(.35)).add(0, -.25, 0);
    }

    private static void esfera(Quadro q, float modo, boolean somarLuz, Vec3 c, double raio, int cor, float alpha) {
        if (q.longe(c) || alpha <= .004f) return;
        Vec3 olhar = c.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : c.scale(-1).normalize();
        Pincel p = Pincel.novo().comecar(modo, somarLuz, q.tempo, olhar);
        p.esfera(c, raio, cor, alpha, q.segmentos(24));
        p.terminar();
    }

    private static float[] alphas(int n, float alpha) {
        float[] a = new float[n];
        java.util.Arrays.fill(a, alpha);
        return a;
    }

    // ---------------------------------------------------------------- Azul

    private static void azul(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Vec3 c = q.rel(v.origem);
        if (q.longe(c)) return;
        esfera(q, Pincel.AZUL, false, c, FluxoAzulGojo.raio(t), 0x0A1830, op);
        esfera(q, Pincel.AZUL, true, c, FluxoAzulGojo.raio(t) * 1.6, CIANO, op * .35f);
        int faixas = Math.max(6, q.detalhe.faixas() / 4);
        int passos = q.detalhe.passos();
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        for (int f = 0; f < faixas; f++) {
            List<Vec3> pts = new ArrayList<>(passos + 1);
            float[] a = new float[passos + 1];
            for (int k = 0; k <= passos; k++) {
                double s = (double) k / passos;
                pts.add(c.add(Base.direto(FluxoAzulGojo.fluxo(f, s, t))));
                a[k] = (float) (op * FluxoAzulGojo.pulso(f, s, t));
            }
            p.fita(pts, .09, CIANO, a);
        }
        for (int f = 0; f < 3; f++) {
            List<Vec3> pts = new ArrayList<>(passos * 2 + 1);
            for (int k = 0; k <= passos * 2; k++) pts.add(c.add(Base.direto(FluxoAzulGojo.arco(f, (double) k / (passos * 2), t))));
            p.fita(pts, .05, BRANCO_AZULADO, alphas(pts.size(), op * .6f));
        }
        p.terminar();
    }

    // ---------------------------------------------------------------- Vermelho

    private static void vermelhoCarga(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Entity e = dono(v);
        if (e == null) return;
        Vec3 c = q.rel(mao(e, q.parcial));
        double r = DescargaVermelhoGojo.raioCarga(t);
        esfera(q, Pincel.VERMELHO, false, c, r, CARMESIM, op);
        esfera(q, Pincel.VERMELHO, true, c, r * 2.2, CARMESIM, op * .3f);
        Base b = Base.de(new Vetor(e.getViewVector(q.parcial).x, e.getViewVector(q.parcial).y, e.getViewVector(q.parcial).z));
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        int passos = q.detalhe.passos();
        for (int f = 0; f < 4; f++) {
            List<Vec3> pts = new ArrayList<>(passos + 1);
            for (int k = 0; k <= passos; k++) pts.add(c.add(b.aplicar(DescargaVermelhoGojo.carga(f, (double) k / passos, t))));
            p.fita(pts, .035, 0xFF8070, alphas(pts.size(), op * .8f));
        }
        p.terminar();
    }

    private static void vermelho(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Vec3 c = q.rel(TrajetoriaGojo.vermelho(v.origem, v.direcao, t));
        double r = .35 + .15 * v.forca;
        esfera(q, Pincel.VERMELHO, false, c, r, CARMESIM, op);
        esfera(q, Pincel.VERMELHO, true, c, r * 2, CARMESIM, op * .35f);
        // Rastro curto atrás da esfera.
        Vec3 atras = vec(v.direcao).scale(-1);
        List<Vec3> pts = new ArrayList<>();
        int n = 8;
        float[] a = new float[n + 1];
        for (int k = 0; k <= n; k++) {
            pts.add(c.add(atras.scale(k * .4)));
            a[k] = op * .7f * (1 - (float) k / n);
        }
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        p.fita(pts, r * 1.4, CARMESIM, a);
        p.terminar();
    }

    private static void explosao(Quadro q, VisuaisAtivos.Visual v, double i) {
        Vec3 c = q.rel(v.origem);
        if (q.longe(c)) return;
        float clarao = (float) Math.exp(-i / 3);
        esfera(q, Pincel.VERMELHO, true, c, .8 + i * .25, 0xFFD0C0, clarao);
        float alpha = (float) DescargaVermelhoGojo.alphaOnda(i);
        Base b = Base.de(v.direcao);
        int passos = q.detalhe.passos() * 3;
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        for (int f = 0; f < 3; f++) {
            List<Vec3> pts = new ArrayList<>(passos + 1);
            for (int k = 0; k <= passos; k++) pts.add(c.add(b.aplicar(DescargaVermelhoGojo.onda(f, (double) k / passos, i))));
            p.fita(pts, .25, CARMESIM, alphas(pts.size(), alpha * (1 - f * .25f)));
        }
        p.terminar();
    }

    // ---------------------------------------------------------------- Infinito

    private static void infinito(Quadro q, VisuaisAtivos.Visual v, float op) {
        Entity e = dono(v);
        if (e == null) return;
        Vec3 c = q.rel(e.getPosition(q.parcial).add(0, e.getBbHeight() / 2, 0));
        esfera(q, Pincel.INFINITO, true, c, 1.4, 0x8CCCFF, op * .9f);
    }

    // ---------------------------------------------------------------- Roxo

    private static void roxoCarga(Quadro q, VisuaisAtivos.Visual v, double t, float op, double passo, int carga, double escala) {
        Entity e = dono(v);
        if (e == null) return;
        Vec3 olho = e.getEyePosition(q.parcial);
        Vec3 olhar = e.getViewVector(q.parcial);
        Vetor dir = new Vetor(olhar.x, olhar.y, olhar.z);
        Vec3 centro = q.rel(vec(CoroaRoxoGojo.centroFusao(new Vetor(olho.x, olho.y, olho.z), dir, passo)));
        Base b = Base.de(dir);
        double sep = CoroaRoxoGojo.separacao(t, carga) / 2;
        Vec3 azul = centro.add(b.lado.scale(-sep));
        Vec3 verm = centro.add(b.lado.scale(sep));
        double r = .35 * escala;
        esfera(q, Pincel.AZUL, false, azul, r, 0x0A1830, op);
        esfera(q, Pincel.AZUL, true, azul, r * 1.7, CIANO, op * .4f);
        esfera(q, Pincel.VERMELHO, false, verm, r, CARMESIM, op);
        esfera(q, Pincel.VERMELHO, true, verm, r * 1.7, CARMESIM, op * .35f);
        double forma = CoroaRoxoGojo.formacao(t, carga);
        if (forma > 0) esfera(q, Pincel.ROXO, false, centro, .9 * escala * forma, VIOLETA, op * (float) forma);
        int passos = q.detalhe.passos();
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        for (int f = 0; f < 3; f++) {
            List<Vec3> pts = new ArrayList<>(passos + 1);
            for (int k = 0; k <= passos; k++) pts.add(centro.add(b.aplicar(CoroaRoxoGojo.ponte(f, (double) k / passos, t, carga))));
            p.fita(pts, .06 * escala, VIOLETA, alphas(pts.size(), op * .7f));
        }
        p.terminar();
    }

    private static void roxo(Quadro q, VisuaisAtivos.Visual v, double t, float op, double passo, double raio) {
        // O feixe do servidor começa a andar um tick depois do disparo.
        Vec3 c = q.rel(TrajetoriaGojo.roxo(v.origem, v.direcao, passo, t - 1));
        if (q.longe(c)) return;
        esfera(q, Pincel.ROXO, false, c, raio, VIOLETA, op);
        esfera(q, Pincel.ROXO, true, c, raio * 1.35, VIOLETA, op * .3f);
        Base b = Base.de(v.direcao);
        int passos = q.detalhe.passos() * 2;
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        for (int f = 0; f < 3; f++) {
            List<Vec3> pts = new ArrayList<>(passos + 1);
            for (int k = 0; k <= passos; k++) pts.add(c.add(b.aplicar(CoroaRoxoGojo.ponto(f, (double) k / passos, t, raio))));
            p.fita(pts, .12 * raio, 0xE0B0FF, alphas(pts.size(), op * .7f));
        }
        int rastros = Math.max(2, q.detalhe.faixas() / 16);
        for (int f = 0; f < rastros; f++) {
            List<Vec3> pts = new ArrayList<>(passos + 1);
            float[] a = new float[passos + 1];
            for (int k = 0; k <= passos; k++) {
                double s = (double) k / passos;
                pts.add(c.add(b.aplicar(CoroaRoxoGojo.rastro(f, rastros, s, t, raio))));
                a[k] = op * .6f * (float) (1 - s);
            }
            p.fita(pts, .1 * raio, VIOLETA, a);
        }
        p.terminar();
    }

    // ---------------------------------------------------------------- Vazio Infinito

    private static void dominio(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Vec3 c = q.rel(v.origem);
        if (q.longe(c)) return;
        double raio = PerfilVfxGojo.raioDominio(t);
        if (raio < .2) return;
        // O domo escuro é desenhado depois do terreno: de dentro, esconde o mundo além do raio;
        // de fora, é uma bolha escura. O teste de profundidade mantém o que está mais perto.
        Pincel p = Pincel.novo().comecar(Pincel.CEU, false, q.tempo, new Vec3(0, 0, 1));
        p.esfera(c, raio, 0xFFFFFF, op, q.segmentos(48));
        p.terminar();
        Vec3 pes = c.add(0, -1.62 + .02, 0);
        Pincel chao = Pincel.novo().comecar(Pincel.CHAO, false, q.tempo, new Vec3(0, 1, 0));
        chao.disco(pes, raio, 0xFFFFFF, op * .85f, q.segmentos(48));
        chao.terminar();
        int faixas = Math.max(8, q.detalhe.faixas() / 3);
        int passos = q.detalhe.passos();
        Pincel linhas = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        for (int f = 0; f < faixas; f++) {
            List<Vec3> pts = new ArrayList<>(passos + 1);
            float[] a = new float[passos + 1];
            for (int k = 0; k <= passos; k++) {
                double s = (double) k / passos;
                pts.add(pes.add(Base.direto(VazioInfinitoGojo.informacao(f, s, t))));
                a[k] = op * (float) VazioInfinitoGojo.pulso(f, s, t);
            }
            linhas.fita(pts, .05, VazioInfinitoGojo.cor(f, .5, t), a);
        }
        linhas.terminar();
    }

    /** Neblina escura dentro do domínio (só a cor; água e lava ficam como estão). */
    public static void aoCorDaNeblina(ViewportEvent.ComputeFogColor evento) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || evento.getCamera().getFluidInCamera() != net.minecraft.world.level.material.FogType.NONE) return;
        double presenca = presencaDominio(evento.getCamera().getPosition(), (float) evento.getPartialTick());
        if (presenca <= 0) return;
        float k = (float) (1 - presenca);
        evento.setRed(evento.getRed() * k + .004f * (1 - k));
        evento.setGreen(evento.getGreen() * k + .006f * (1 - k));
        evento.setBlue(evento.getBlue() * k + .02f * (1 - k));
    }

    private static double presencaDominio(Vec3 camera, float parcial) {
        long agora = Minecraft.getInstance().level.getGameTime();
        double maior = 0;
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (!v.tipo.equals("gojo_vazio_infinito") || v.fimEm >= 0) continue;
            double d = camera.distanceTo(vec(v.origem));
            maior = Math.max(maior, PerfilVfxGojo.presencaDominio(v.idade(agora, parcial), d));
        }
        return maior;
    }
}
