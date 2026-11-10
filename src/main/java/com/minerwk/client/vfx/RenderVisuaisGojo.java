package com.minerwk.client.vfx;

import com.minerwk.client.ConfigCliente;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200;
import com.minerwk.core.vfx.BolhaInfinitoGojo;
import com.minerwk.core.vfx.CoroaRoxoGojo;
import com.minerwk.core.vfx.DescargaVermelhoGojo;
import com.minerwk.core.vfx.FluxoAzulGojo;
import com.minerwk.core.vfx.FusaoRoxoGojo;
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
    private static final com.mojang.blaze3d.vertex.ByteBufferBuilder BUFFER_TEXTO = new com.mojang.blaze3d.vertex.ByteBufferBuilder(1536);

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
            // Rastros calculam a própria transparência (começam cheios, sem a entrada suave).
            boolean rastro = v.tipo.endsWith("_rastro") || v.tipo.equals("gojo_azul_marca");
            if (op <= .004f && !rastro) continue;
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
                case "gojo_infinito_toque" -> toqueInfinito(q, v, t, op);
                case "gojo_roxo_carga" -> roxoCarga(q, v, t, op, TrajetoriaGojo.PASSO_ROXO, 30, 1);
                case "gojo_roxo_200_carga" -> {
                    encantamento(q, v, t, op);
                    palavrasNoMundo(q, v, t);
                }
                case "gojo_roxo_maximo" -> roxoMaximo(q, v, t, op);
                case "gojo_roxo_maximo_esferas" -> esferasRoxoMaximo(q, v, t, op);
                case "gojo_roxo" -> roxo(q, v, t, op, TrajetoriaGojo.PASSO_ROXO, 2.1);
                case "gojo_roxo_200" -> roxo(q, v, t, op, TrajetoriaGojo.PASSO_ROXO_200, 9.8);
                case "gojo_roxo_explosao" -> explosaoRoxa(q, v, t);
                case "gojo_roxo_rastro" -> tunelRoxo(q, v.origem, v.direcao, v.forca, 1.5,
                        .7f * (float) Math.pow(Math.max(0, 1 - t / v.duracao), 1.5));
                case "gojo_roxo_200_rastro" -> tunelRoxo(q, v.origem, v.direcao, v.forca, 7,
                        .7f * (float) Math.pow(Math.max(0, 1 - t / v.duracao), 1.5));
                case "gojo_azul_marca" -> marcaAzul(q, v, (float) (PerfilVfxGojo.suave(t / 4)
                        * Math.min(1, Math.max(0, (v.duracao - t) / PerfilVfxGojo.MARCA_AZUL))));
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
        return mao(e, parcial, 1);
    }

    /** Ponto na frente da mão do dono: {@code lado} 1 é a direita, -1 a esquerda. */
    private static Vec3 mao(Entity e, float parcial, int lado) {
        Vec3 olho = e.getEyePosition(parcial);
        Vec3 olhar = e.getViewVector(parcial);
        Vec3 l = olhar.cross(new Vec3(0, 1, 0));
        l = l.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : l.normalize();
        return olho.add(olhar.scale(.7)).add(l.scale(.35 * lado)).add(0, -.25, 0);
    }

    private static void esfera(Quadro q, float modo, boolean somarLuz, Vec3 c, double raio, int cor, float alpha) {
        if (q.longe(c) || alpha <= .004f) return;
        Vec3 olhar = c.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : c.scale(-1).normalize();
        Pincel p = Pincel.novo().comecar(modo, somarLuz, q.tempo, olhar);
        p.esfera(c, raio, cor, alpha, q.segmentos(24));
        p.terminar();
    }

    /** Brilho aditivo em volta de uma esfera (metade no VFX reduzido). */
    private static void brilho(Quadro q, Vec3 c, double raio, int cor, float alpha) {
        if (q.longe(c)) return;
        if (q.detalhe.faixas() < 64) alpha *= .5f;
        if (alpha <= .004f) return;
        Pincel p = Pincel.novo().comecar(Pincel.BRILHO, true, q.tempo, new Vec3(0, 0, 1));
        p.brilho(c, raio, cor, alpha);
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
        double ra = FluxoAzulGojo.raio(t) * 1.3;
        esfera(q, Pincel.AZUL, false, c, ra, 0x0A1830, op);
        esfera(q, Pincel.AZUL, true, c, ra * 1.6, CIANO, op * .35f);
        brilho(q, c, ra * 3.5, CIANO, op * .55f);
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
        // Espirais de vento entrando no núcleo, apertando a cada volta.
        int ns = passos * 2;
        for (int f = 0; f < 4; f++) {
            List<Vec3> pts = new ArrayList<>(ns + 1);
            float[] a = new float[ns + 1];
            for (int k = 0; k <= ns; k++) {
                double s = (double) k / ns;
                pts.add(c.add(Base.direto(FluxoAzulGojo.espiral(f, s, t))));
                a[k] = op * .55f * (float) Math.sin(Math.PI * s);
            }
            p.fita(pts, .07, f % 2 == 0 ? 0xFFFFFF : BRANCO_AZULADO, a);
        }
        // Anéis de sucção: nascem largos e se fecham no núcleo.
        int na = q.segmentos(32);
        for (int k = 0; k < 3; k++) {
            double r = FluxoAzulGojo.anelSuccao(k, t);
            float a = op * .5f * (float) FluxoAzulGojo.alphaAnel(k, t);
            if (a <= .004f) continue;
            double incl = .35 + k * .9;
            List<Vec3> pts = new ArrayList<>(na + 1);
            for (int i = 0; i <= na; i++) {
                double ang = i * Math.PI * 2 / na;
                double y = Math.sin(ang) * r;
                pts.add(c.add(Math.cos(ang) * r, y * Math.sin(incl) * .5, y * Math.cos(incl)));
            }
            p.fita(pts, .06, CIANO, alphas(pts.size(), a));
        }
        // Espaço dobrando: um anel de frente para a câmera que ondula em volta do núcleo.
        Vec3 f = c.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : c.normalize();
        Vec3 u = f.cross(new Vec3(0, 1, 0));
        u = u.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : u.normalize();
        Vec3 w = u.cross(f).normalize();
        List<Vec3> dobra = new ArrayList<>(na + 1);
        for (int i = 0; i <= na; i++) {
            double ang = i * Math.PI * 2 / na;
            double r = ra * 2.4 * (1 + .09 * Math.sin(ang * 6 + t * .45));
            dobra.add(c.add(u.scale(Math.cos(ang) * r)).add(w.scale(Math.sin(ang) * r)));
        }
        p.fita(dobra, .18, 0x1A3A70, alphas(dobra.size(), op * .35f));
        p.terminar();
        // O núcleo "engole a luz": centro escuro dentro da borda ciano, pulsando.
        esfera(q, Pincel.AZUL, false, c, ra * (.5 + .06 * Math.sin(t * .6)), 0x000000, op);
    }

    // ---------------------------------------------------------------- Vermelho

    private static void vermelhoCarga(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Entity e = dono(v);
        if (e == null) return;
        Vec3 c = q.rel(mao(e, q.parcial));
        double r = DescargaVermelhoGojo.raioCarga(t) * 1.4;
        esfera(q, Pincel.VERMELHO, false, c, r, CARMESIM, op);
        esfera(q, Pincel.VERMELHO, true, c, r * 2.2, CARMESIM, op * .3f);
        brilho(q, c, r * 4, CARMESIM, op * .6f);
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
        double r = .55 + .25 * v.forca;
        esfera(q, Pincel.VERMELHO, false, c, r, CARMESIM, op);
        esfera(q, Pincel.VERMELHO, true, c, r * 2, CARMESIM, op * .35f);
        brilho(q, c, r * 4, CARMESIM, op * .7f);
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
        brilho(q, c, 6 + i * .5, 0xFF5040, (float) Math.max(0, 1 - i / 8) * .9f);
        float alpha = (float) DescargaVermelhoGojo.alphaOnda(i);
        Base b = Base.de(v.direcao);
        int passos = q.detalhe.passos() * 3;
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        for (int f = 0; f < 3; f++) {
            List<Vec3> pts = new ArrayList<>(passos + 1);
            for (int k = 0; k <= passos; k++) pts.add(c.add(b.aplicar(DescargaVermelhoGojo.onda(f, (double) k / passos, i)).scale(1.5)));
            p.fita(pts, .25, CARMESIM, alphas(pts.size(), alpha * (1 - f * .25f)));
        }
        p.terminar();
    }

    /**
     * Fim do R em camadas: núcleo branco ofuscante, casca roxa crescendo até o raio (a forca do visual) no
     * tempo do servidor (a duração menos os 28 ticks de sumir), fitas de energia em raios e duas ondas de
     * choque (uma correndo pelo chão até 60 blocos, outra no ar). A distorção e as partículas vêm do Photon.
     */
    /**
     * A explosão do Roxo em camadas: estoura rápido e cresce devagar até o raio, com o vazio escuro no
     * miolo, a casca violeta com relâmpagos correndo nela, levas de fitas em raios, uma coluna de luz,
     * ondas de choque em sequência e, no fim, o que sobra encolhendo e sumindo.
     */
    private static void explosaoRoxa(Quadro q, VisuaisAtivos.Visual v, double i) {
        Vec3 c = q.rel(v.origem);
        if (c.lengthSqr() > 400 * 400) return;
        double raio = v.forca > 1 ? v.forca : 24;
        double resto = Math.min(com.minerwk.core.personagens.gojo.RoxoMaximo.TICKS_RESTO, Math.max(1, v.duracao - 1));
        double cresceEm = Math.max(1, v.duracao - resto);
        double cresce = com.minerwk.core.personagens.gojo.RoxoMaximo.fracaoExplosao(i / cresceEm);
        float some = (float) Math.max(0, 1 - Math.max(0, i - cresceEm) / resto);
        some *= some;
        // No resto, a casca encolhe um pouco enquanto some.
        double r = (2 + (raio - 2) * cresce) * (1 - .2 * (1 - Math.sqrt(some)));
        float tremula = (float) (.85 + .15 * Math.sin(i * 1.7));
        boolean reduzido = q.detalhe.faixas() < 64;
        // O vazio no miolo, a camada violeta forte e a casca clara por fora.
        esfera(q, Pincel.ROXO, false, c, r * .5, 0x14041F, .75f * some);
        esfera(q, Pincel.ROXO, true, c, r * .78, 0x7A2CFF, .55f * some * tremula);
        esfera(q, Pincel.ROXO, true, c, r, 0xC890FF, .75f * some);
        esfera(q, Pincel.ROXO, true, c, 1 + raio * .45 * cresce, 0xFFFFFF, (float) Math.exp(-i / 8));
        brilho(q, c, raio * 1.25 + raio * .8 * cresce, 0x9A4DFF, .8f * some);
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        // Coluna de luz atravessando o centro no estouro.
        float coluna = (float) Math.exp(-i / 14);
        if (coluna > .02f) {
            List<Vec3> pts = List.of(c.add(0, -raio, 0), c, c.add(0, raio * 1.8, 0));
            p.fita(pts, 2 + raio * .12 * coluna, 0xE0C8FF, new float[] {0, coluna, 0});
            p.fita(pts, .8 + raio * .04 * coluna, 0xFFFFFF, new float[] {0, coluna, 0});
        }
        // Levas de fitas em raios: uma nova a cada 12 ticks enquanto cresce.
        if (i < cresceEm) {
            int leva = (int) (i / 12);
            float fitas = (float) (1 - (i - leva * 12) / 12);
            java.util.Random rnd = new java.util.Random(v.inicio * 31 + leva);
            for (int k = 0; k < (reduzido ? 8 : 16); k++) {
                Vec3 dir = new Vec3(rnd.nextGaussian(), rnd.nextGaussian() * .6, rnd.nextGaussian()).normalize();
                double ate = r * (1.1 + rnd.nextDouble() * .5);
                List<Vec3> pts = List.of(c.add(dir.scale(r * .2)), c.add(dir.scale(ate * .6)), c.add(dir.scale(ate)));
                p.fita(pts, .6 + raio * .02, k % 3 == 0 ? 0xFFFFFF : 0xC890FF, new float[] {fitas, fitas * .7f, 0});
            }
        }
        // Relâmpagos correndo na casca: mudam de lugar a cada 2 ticks.
        if (some > .05f) {
            java.util.Random rnd = new java.util.Random(v.inicio * 17 + (long) (i / 2));
            for (int k = 0; k < (reduzido ? 5 : 12); k++) {
                Vec3 a = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize();
                Vec3 passo = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).scale(.22);
                List<Vec3> pts = new ArrayList<>(6);
                for (int j = 0; j < 6; j++) {
                    pts.add(c.add(a.scale(r * 1.01)));
                    a = a.add(passo).add(rnd.nextGaussian() * .08, rnd.nextGaussian() * .08, rnd.nextGaussian() * .08).normalize();
                }
                float al = .9f * some;
                p.fita(pts, .25 + raio * .012, k % 2 == 0 ? 0xFFFFFF : 0xE0C8FF, new float[] {0, al, al, al, al, 0});
            }
        }
        // Ondas de choque no chão, três em sequência, e uma no ar, inclinada.
        int n = q.segmentos(64);
        for (int anel = 0; anel < 4; anel++) {
            double atraso = anel == 3 ? 0 : anel * 10;
            double rc = com.minerwk.core.vfx.FusaoRoxoGojo.raioChoque(i - atraso);
            float onda = (float) Math.max(0, 1 - (i - atraso) / (cresceEm + 10));
            if (i < atraso || onda <= .01f || rc <= .5) continue;
            boolean noAr = anel == 3;
            double ra = noAr ? rc * .8 : rc;
            double y = noAr ? 0 : -Math.min(raio * .5, 20);
            double incl = noAr ? .35 : 0;
            List<Vec3> pts = new ArrayList<>(n + 1);
            for (int k = 0; k <= n; k++) {
                double ang = k * Math.PI * 2 / n;
                pts.add(c.add(Math.cos(ang) * ra, y + Math.sin(ang) * ra * incl, Math.sin(ang) * ra));
            }
            float forca = noAr ? .5f : .7f / (1 + anel * .6f);
            p.fita(pts, noAr ? 1.2 : 2.5, noAr ? 0xFFFFFF : 0xE0C8FF, alphas(pts.size(), onda * forca));
        }
        p.terminar();
    }

    // ---------------------------------------------------------------- R (Vazio Roxo máximo)

    /**
     * No encantamento do R, sem esferas (elas só nascem lá no céu): uma aura de fitas azuis e vermelhas
     * subindo em espiral em volta do Gojo, mais forte a cada palavra, os olhos brilhando e rachaduras de
     * luz abrindo no chão a partir dos pés.
     */
    private static void encantamento(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Entity e = dono(v);
        if (e == null) return;
        double k = Math.min(1, t / Math.max(1, v.duracao));
        Vec3 pe = q.rel(e.getPosition(q.parcial));
        if (q.longe(pe)) return;
        double tempo = q.tempo;
        double altura = 1.2 + 1.3 * k;
        int fitas = q.detalhe.faixas() < 64 ? 4 : 6, n = 16;
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        for (int f = 0; f < fitas; f++) {
            int cor = f % 2 == 0 ? CIANO : CARMESIM;
            double fase = 2 * Math.PI * f / fitas;
            List<Vec3> pts = new ArrayList<>(n + 1);
            float[] al = new float[n + 1];
            for (int j = 0; j <= n; j++) {
                double u = (double) j / n;
                // O giro andando com o tempo faz a espiral parecer subir.
                double ang = fase + u * 2.4 - tempo * .22;
                double raio = .75 + .15 * Math.sin(tempo * .3 + f) - .25 * u;
                pts.add(pe.add(Math.cos(ang) * raio, u * altura, Math.sin(ang) * raio));
                al[j] = (float) (op * (.25 + .5 * k) * Math.sin(Math.PI * u));
            }
            p.fita(pts, .1 + .12 * k, cor, al);
        }
        // Rachaduras de luz abrindo no chão a partir dos pés, cada vez mais compridas.
        for (int r = 0; r < com.minerwk.core.vfx.EncantamentoGojo.RACHADURAS; r++) {
            List<Vetor> linha = com.minerwk.core.vfx.EncantamentoGojo.rachadura(v.inicio, r, k);
            if (linha.size() < 2) continue;
            List<Vec3> pts = new ArrayList<>(linha.size());
            float[] al = new float[linha.size()];
            for (int j = 0; j < linha.size(); j++) {
                pts.add(pe.add(linha.get(j).x(), .04, linha.get(j).z()));
                // Mais forte perto dos pés, sumindo na ponta, pulsando de leve.
                al[j] = (float) (op * (.9 - .7 * j / (linha.size() - 1)) * (.8 + .2 * Math.sin(tempo * .6 + r)));
            }
            p.fita(pts, .16, VIOLETA, al);
            p.fita(pts, .05, 0xFFFFFF, al);
        }
        p.terminar();
        // Olhos brilhando: dois pontos de luz azul-claro um pouco à frente do rosto.
        Vec3 olho = e.getEyePosition(q.parcial);
        Vec3 olhar = e.getViewVector(q.parcial);
        Vec3 l = olhar.cross(new Vec3(0, 1, 0));
        l = l.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : l.normalize();
        for (int lado = -1; lado <= 1; lado += 2) {
            Vec3 c = q.rel(olho.add(olhar.scale(.3)).add(l.scale(.11 * lado)));
            brilho(q, c, .25 + .2 * k, 0x9FE8FF, op * (float) (.45 + .55 * k));
        }
    }

    /** Avanço até o alvo (rastro), clarão do uppercut e o rastro do alvo subindo. */
    private static void roxoMaximo(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        double dist = v.forca;
        int a = com.minerwk.core.personagens.gojo.RoxoMaximo.avanco(dist);
        Vec3 dir = vec(v.direcao).normalize();
        Vec3 inicio = q.rel(v.origem);
        if (q.longe(inicio) && q.longe(inicio.add(dir.scale(dist)))) return;
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        // Rastro do avanço: vai até onde o Gojo está e some logo depois do soco.
        float rastro = (float) Math.max(0, 1 - Math.max(0, t - a) / 8) * op;
        if (rastro > .004f && dist > 1.5) {
            double feito = Math.min(1, a == 0 ? 1 : t / a) * (dist - 1.5);
            int n = 12;
            List<Vec3> pts = new ArrayList<>(n + 1);
            float[] al = new float[n + 1];
            for (int k = 0; k <= n; k++) {
                pts.add(inicio.add(dir.scale(feito * k / n)).add(0, -.6, 0));
                al[k] = rastro * .8f * k / n;
            }
            p.fita(pts, .7, VIOLETA, al);
            p.fita(pts, .25, 0xFFFFFF, al);
        }
        // Rastro do alvo subindo, do ponto do soco até onde ele está.
        Vec3 soco = inicio.add(dir.scale(dist));
        if (t >= a) {
            double sub = t - a;
            double alto = com.minerwk.core.personagens.gojo.RoxoMaximo.alturaAlvo(sub);
            float a2 = (float) Math.max(0, 1 - Math.max(0, sub - 15) / 10) * op;
            if (a2 > .004f && alto > .1) {
                List<Vec3> pts = List.of(soco, soco.add(0, alto * .5, 0), soco.add(0, alto, 0));
                p.fita(pts, .9, 0xE0C8FF, new float[] {0, a2 * .5f, a2 * .9f});
            }
        }
        p.terminar();
        // Clarão do uppercut.
        if (t >= a && t < a + 12) {
            float clarao = (float) Math.exp(-(t - a) / 2.5) * op;
            esfera(q, Pincel.ROXO, true, soco, .6 + (t - a) * .35, 0xFFFFFF, clarao);
            brilho(q, soco, 4 + (t - a) * .8, 0xC890FF, clarao);
        }
    }

    /**
     * As esferas do R em quatro tempos ({@code forca} = ticks das esferas): o Azul (mão esquerda) e o
     * Vermelho (direita) viajam em arcos até a órbita atrás do Gojo, dançam numa espiral que fecha
     * deixando rastros trançados, esticam em gota com um filete entre elas, encostam e soltam um anel
     * branco, afundam uma na outra e o núcleo roxo nasce e carrega até o silêncio antes do estouro.
     */
    private static void esferasRoxoMaximo(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Vec3 centro = q.rel(v.origem);
        Vetor frente = v.direcao;
        Entity e = dono(v);
        // Com alvo (o Gojo no ar, no meio do combo): a fusão fica sempre atrás dele de verdade, na
        // horizontal, e as esferas nascem nas mãos abertas para os lados, como na animação.
        boolean atras = e != null && noCombo(v);
        Vec3[] maos = null;
        if (atras) {
            Vec3 olho = e.getEyePosition(q.parcial);
            frente = new Vetor(v.direcao.x(), 0, v.direcao.z());
            centro = q.rel(com.minerwk.core.personagens.gojo.RoxoMaximo.centroFusao(
                    new Vetor(olho.x, olho.y, olho.z), v.direcao));
            Base aberta = Base.de(frente);
            maos = new Vec3[] {q.rel(olho.add(aberta.lado.scale(-.9)).add(0, -.35, 0)),
                    q.rel(olho.add(aberta.lado.scale(.9)).add(0, -.35, 0))};
        } else if (e != null) {
            maos = new Vec3[] {q.rel(mao(e, q.parcial, -1)), q.rel(mao(e, q.parcial, 1))};
        }
        if (q.longe(centro)) return;
        int danca = com.minerwk.core.personagens.gojo.RoxoMaximo.TICKS_DANCA;
        int conjurar = com.minerwk.core.personagens.gojo.RoxoMaximo.TICKS_CONJURAR;
        double esferas = Math.max(danca + conjurar + 1, v.forca);
        double viagem = esferas - danca - conjurar;
        Base b = Base.de(frente);
        if (t < conjurar) {
            // Lá no céu: o Azul nasce na mão esquerda e o Vermelho na direita, crescendo.
            if (maos != null) {
                double r = FusaoRoxoGojo.raioConjurando(t);
                bolaDaFusao(q, -1, maos[0], null, 1, op, r);
                bolaDaFusao(q, 1, maos[1], null, 1, op, r);
            }
            return;
        }
        if (t < conjurar + viagem) {
            if (maos != null) viagemEsferas(q, centro, b, maos, (t - conjurar) / viagem, op);
            return;
        }
        if (t < esferas) {
            dancaEsferas(q, centro, b, t - conjurar - viagem, op);
            return;
        }
        nucleoRoxoMaximo(q, centro, t - esferas, op);
    }

    /** Se o dono deste visual está no meio do combo do R com alvo (avanço, uppercut e subida). */
    private static boolean noCombo(VisuaisAtivos.Visual v) {
        for (VisuaisAtivos.Visual o : VisuaisAtivos.ativos()) {
            if (o.dono == v.dono && o.tipo.equals("gojo_roxo_maximo")) return true;
        }
        return false;
    }

    private static void viagemEsferas(Quadro q, Vec3 centro, Base b, Vec3[] maos, double fracao, float op) {
        double s = PerfilVfxGojo.suave(fracao);
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        Vec3[] bolas = new Vec3[2];
        for (int lado = -1; lado <= 1; lado += 2) {
            Vec3 de = maos[(lado + 1) / 2];
            Vec3 ate = centro.add(b.aplicar(FusaoRoxoGojo.posicao(lado, 0)));
            double curva = 1 + .35 * de.distanceTo(ate);
            int cor = lado < 0 ? CIANO : CARMESIM;
            int n = 10;
            List<Vec3> pts = new ArrayList<>(n + 1);
            float[] al = new float[n + 1];
            for (int k = 0; k <= n; k++) {
                pts.add(arco(de, ate, b, lado, curva, Math.max(0, s - .35 * (n - k) / n)));
                al[k] = op * .7f * k / n;
            }
            p.fita(pts, .35, cor, al);
            bolas[(lado + 1) / 2] = arco(de, ate, b, lado, curva, s);
        }
        p.terminar();
        bolaDaFusao(q, -1, bolas[0], null, 1, op);
        bolaDaFusao(q, 1, bolas[1], null, 1, op);
    }

    private static void dancaEsferas(Quadro q, Vec3 centro, Base b, double d, float op) {
        double sep = FusaoRoxoGojo.separacao(d);
        double estica = FusaoRoxoGojo.alongamento(sep);
        int encosta = FusaoRoxoGojo.encosta();
        // Afundando: as duas encolhem juntas até sumir no ponto.
        double some = d <= encosta + FusaoRoxoGojo.TOQUE ? 1
                : Math.max(0, 1 - (d - encosta - FusaoRoxoGojo.TOQUE) / FusaoRoxoGojo.AFUNDA);
        Vec3 azul = centro.add(b.aplicar(FusaoRoxoGojo.posicao(-1, d)));
        Vec3 verm = centro.add(b.aplicar(FusaoRoxoGojo.posicao(1, d)));
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        // Rastros trançados: cada esfera arrasta o caminho dos últimos ticks da espiral (hélice dupla).
        int n = 16;
        for (int lado = -1; lado <= 1; lado += 2) {
            List<Vec3> pts = new ArrayList<>(n + 1);
            float[] al = new float[n + 1];
            for (int k = 0; k <= n; k++) {
                double dk = Math.max(0, d - 5.0 * (n - k) / n);
                pts.add(centro.add(b.aplicar(FusaoRoxoGojo.posicao(lado, dk))));
                al[k] = op * .75f * k / n * (float) some;
            }
            p.fita(pts, .3, lado < 0 ? CIANO : CARMESIM, al);
        }
        // Filete entre as duas, do ciano ao carmesim passando pelo violeta, ondulando.
        float filete = (float) Math.min(1, (estica - 1) / (FusaoRoxoGojo.GOTA_MAX - 1) * 1.5) * op * (float) some;
        if (filete > .01f) {
            int partes = 6;
            Vec3 lateral = verm.subtract(azul).cross(b.frente);
            lateral = lateral.lengthSqr() < 1e-6 ? b.cima : lateral.normalize();
            for (int k = 0; k < partes; k++) {
                List<Vec3> seg = new ArrayList<>(3);
                for (int j = 0; j <= 2; j++) {
                    double u = (k + j / 2.0) / partes;
                    double onda = Math.sin(u * Math.PI * 3 + q.tempo * 18) * .12 * Math.sin(u * Math.PI);
                    seg.add(azul.lerp(verm, u).add(lateral.scale(onda)));
                }
                p.fita(seg, .14, FusaoRoxoGojo.corFilete((k + .5) / partes), alphas(seg.size(), filete));
            }
        }
        // Toque: anel fino de luz branca no plano da órbita, abrindo.
        if (d >= encosta) {
            double i = d - encosta;
            float anel = (float) Math.exp(-i / 2.5) * op;
            double ra = .6 + i * .9;
            int m = q.segmentos(40);
            List<Vec3> pts = new ArrayList<>(m + 1);
            double incl = Math.toRadians(35);
            Vec3 eixoB = b.cima.scale(Math.sin(incl)).add(b.frente.scale(Math.cos(incl)));
            for (int k = 0; k <= m; k++) {
                double ang = k * Math.PI * 2 / m;
                pts.add(centro.add(b.lado.scale(Math.cos(ang) * ra)).add(eixoB.scale(Math.sin(ang) * ra)));
            }
            p.fita(pts, .1, 0xFFFFFF, alphas(pts.size(), anel));
        }
        p.terminar();
        bolaDaFusao(q, -1, azul, verm, estica, op * (float) some);
        bolaDaFusao(q, 1, verm, azul, estica, op * (float) some);
        // Bordas misturando: um brilho violeta no ponto de contato quando estão perto.
        if (estica > 1.3) {
            brilho(q, azul.lerp(verm, .5), .8 + sep * .3, VIOLETA, op * (float) Math.min(1, (estica - 1.3) / .5) * .7f);
        }
    }

    /**
     * Uma esfera da fusão: o núcleo e a borda; perto da outra ({@code outra}), estica em gota na direção
     * dela (duas esferas menores puxadas para a frente fazem a ponta).
     */
    private static void bolaDaFusao(Quadro q, int lado, Vec3 c, Vec3 outra, double estica, float op) {
        bolaDaFusao(q, lado, c, outra, estica, op, FusaoRoxoGojo.RAIO_ESFERA);
    }

    private static void bolaDaFusao(Quadro q, int lado, Vec3 c, Vec3 outra, double estica, float op, double r) {
        if (op <= .004f || r <= .005) return;
        int cor = lado < 0 ? CIANO : CARMESIM;
        int miolo = lado < 0 ? 0x0A1830 : CARMESIM;
        float modo = lado < 0 ? Pincel.AZUL : Pincel.VERMELHO;
        esfera(q, modo, false, c, r, miolo, op);
        esfera(q, modo, true, c, r * 1.8, cor, op * .42f);
        if (outra != null && estica > 1.02) {
            Vec3 dir = outra.subtract(c);
            if (dir.lengthSqr() > 1e-6) {
                dir = dir.normalize();
                double alonga = (estica - 1) * r;
                esfera(q, modo, false, c.add(dir.scale(alonga * .9)), r * .78, miolo, op);
                esfera(q, modo, true, c.add(dir.scale(alonga * 1.6)), r * .5, cor, op * .8f);
            }
        }
        brilho(q, c, 2.2 * r / FusaoRoxoGojo.RAIO_ESFERA, cor, op * .6f);
    }

    /**
     * O núcleo roxo da fusão ({@code f}: ticks desde o nascimento): clarão curto no nascimento, núcleo
     * pequeno e escuro com borda violeta que cresce pulsando cada vez mais rápido, anéis de choque e
     * relâmpagos curtos; no silêncio encolhe, para de pulsar e clareia para branco.
     */
    private static void nucleoRoxoMaximo(Quadro q, Vec3 centro, double f, float op) {
        double silencio = FusaoRoxoGojo.silencio(f);
        double r = FusaoRoxoGojo.raioNucleo(f) * FusaoRoxoGojo.pulso(f);
        double carga = Math.min(1, f / com.minerwk.core.personagens.gojo.RoxoMaximo.TICKS_FUSAO);
        esfera(q, Pincel.ROXO, false, centro, r, VIOLETA, op);
        esfera(q, Pincel.ROXO, true, centro, r * 1.25, 0xC890FF, op * (.25f + .3f * (float) carga));
        brilho(q, centro, r * (3 + 2 * carga), 0x9A4DFF, op * (.5f + .4f * (float) carga));
        // Nascimento: clarão curto.
        esfera(q, Pincel.ROXO, true, centro, .5 + f * .5, 0xFFFFFF, (float) Math.exp(-f / 1.5) * op);
        // Silêncio: a borda vira branca e o núcleo fica ofuscante antes do estouro.
        if (silencio > 0) {
            esfera(q, Pincel.ROXO, true, centro, r * 1.1, 0xFFFFFF, op * (float) silencio);
            brilho(q, centro, r * 6, 0xFFFFFF, op * (float) silencio * .8f);
            return;
        }
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        int n = q.segmentos(32);
        // Anéis girando em volta do núcleo, mais rápido com a carga.
        for (int anel = 0; anel < 3; anel++) {
            double giro = q.tempo * (2 + anel + 3 * carga) + anel * 2.1;
            Vec3 eixoA = new Vec3(Math.cos(giro), Math.sin(anel * 1.1), Math.sin(giro)).normalize();
            Vec3 eixoB = eixoA.cross(new Vec3(0, 1, 0));
            eixoB = eixoB.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : eixoB.normalize();
            Vec3 eixoC = eixoA.cross(eixoB).normalize();
            double ra = r * (1.8 + .3 * anel);
            List<Vec3> pts = new ArrayList<>(n + 1);
            for (int i = 0; i <= n; i++) {
                double ang = i * Math.PI * 2 / n;
                pts.add(centro.add(eixoB.scale(Math.cos(ang) * ra)).add(eixoC.scale(Math.sin(ang) * ra)));
            }
            p.fita(pts, .12, anel == 1 ? 0xFFFFFF : 0xC890FF, alphas(pts.size(), op * .7f));
        }
        // Anéis de choque pequenos, cada vez mais seguidos.
        double periodo = Math.max(3, 10 - 7 * carga);
        double fase = (f % periodo) / periodo;
        double rc = r * (1.2 + 2.5 * fase);
        List<Vec3> choque = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) {
            double ang = i * Math.PI * 2 / n;
            choque.add(centro.add(Math.cos(ang) * rc, 0, Math.sin(ang) * rc));
        }
        p.fita(choque, .18, 0xE0C8FF, alphas(choque.size(), op * (float) (1 - fase) * .8f));
        // Relâmpagos roxos curtos: trocam a cada 2 ticks, mais numerosos com a carga.
        java.util.Random rnd = new java.util.Random((long) (f / 2) * 7919L);
        int raios = 1 + (int) (carga * 4);
        for (int k = 0; k < raios; k++) {
            Vec3 dir = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize();
            List<Vec3> zig = new ArrayList<>(6);
            Vec3 ponto = centro.add(dir.scale(r * .9));
            for (int j = 0; j < 6; j++) {
                zig.add(ponto);
                ponto = ponto.add(dir.scale(.5 + rnd.nextDouble() * .5))
                        .add(rnd.nextGaussian() * .35, rnd.nextGaussian() * .35, rnd.nextGaussian() * .35);
            }
            p.fita(zig, .07, k % 2 == 0 ? 0xE8D0FF : VIOLETA, alphas(zig.size(), op * .9f));
        }
        p.terminar();
    }

    /** Arco de {@code de} até {@code ate}: abre para o {@code lado} e sobe um pouco no meio. */
    private static Vec3 arco(Vec3 de, Vec3 ate, Base b, int lado, double curva, double s) {
        double bojo = Math.sin(Math.PI * s);
        return de.lerp(ate, s).add(b.lado.scale(lado * curva * bojo)).add(b.cima.scale(curva * .4 * bojo));
    }

    // ---------------------------------------------------------------- Infinito

    private static void infinito(Quadro q, VisuaisAtivos.Visual v, float op) {
        Entity e = dono(v);
        if (e == null) return;
        Vec3 c = q.rel(e.getPosition(q.parcial).add(0, e.getBbHeight() / 2, 0));
        double raio = BolhaInfinitoGojo.RAIO;
        esfera(q, Pincel.INFINITO, true, c, raio, 0x8CCCFF, op * .9f);
        if (q.longe(c)) return;
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        int n = q.segmentos(32);
        // Brilho correndo pela bolha: duas faixas em círculos máximos que giram.
        for (int b = 0; b < 2; b++) {
            double g = q.tempo * (1.1 + b * .4) + b * 1.7;
            Vec3 eixo = new Vec3(Math.cos(g), .6 - b * 1.2, Math.sin(g)).normalize();
            Vec3 u = perpendicular(eixo);
            Vec3 w = eixo.cross(u).normalize();
            List<Vec3> pts = new ArrayList<>(n / 2 + 1);
            float[] a = new float[n / 2 + 1];
            for (int i = 0; i <= n / 2; i++) {
                double ang = i * Math.PI / (n / 2) + q.tempo * 2.5;
                pts.add(c.add(u.scale(Math.cos(ang) * raio)).add(w.scale(Math.sin(ang) * raio)));
                a[i] = op * .35f * (float) Math.sin(Math.PI * i / (n / 2));
            }
            p.fita(pts, .05, 0xFFFFFF, a);
        }
        // Hexágonos sutis que acendem e apagam pela superfície.
        double giro = q.tempo * .2;
        for (int i = 0; i < BolhaInfinitoGojo.HEXAGONOS; i++) {
            float a = op * .3f * (float) BolhaInfinitoGojo.brilhoHexagono(i, q.tempo);
            if (a <= .01f) continue;
            Vetor d0 = BolhaInfinitoGojo.direcaoHexagono(i);
            Vec3 d = new Vec3(d0.x() * Math.cos(giro) - d0.z() * Math.sin(giro), d0.y(),
                    d0.x() * Math.sin(giro) + d0.z() * Math.cos(giro));
            Vec3 centro = c.add(d.scale(raio));
            Vec3 u = perpendicular(d);
            Vec3 w = d.cross(u).normalize();
            List<Vec3> hex = new ArrayList<>(7);
            for (int k = 0; k <= 6; k++) {
                double ang = k * Math.PI / 3;
                hex.add(centro.add(u.scale(Math.cos(ang) * .22)).add(w.scale(Math.sin(ang) * .22)));
            }
            p.fita(hex, .025, 0xBFE8FF, alphas(hex.size(), a));
        }
        p.terminar();
    }

    /** O Infinito parou algo: ondas pela bolha a partir do ponto e, se foi um projétil, um halo tremendo nele. */
    private static void toqueInfinito(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Entity e = dono(v);
        if (e != null) {
            Vec3 c = q.rel(e.getPosition(q.parcial).add(0, e.getBbHeight() / 2, 0));
            if (!q.longe(c)) {
                Vec3 d = vec(v.direcao).lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : vec(v.direcao).normalize();
                Vec3 u = perpendicular(d);
                Vec3 w = d.cross(u).normalize();
                double raio = BolhaInfinitoGojo.RAIO;
                int n = q.segmentos(24);
                Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
                for (int k = 0; k < BolhaInfinitoGojo.ONDAS; k++) {
                    float a = (float) BolhaInfinitoGojo.alphaOnda(k, t) * .8f;
                    if (a <= .004f) continue;
                    double th = Math.max(.05, BolhaInfinitoGojo.angulo(k, t));
                    List<Vec3> pts = new ArrayList<>(n + 1);
                    for (int i = 0; i <= n; i++) {
                        double ang = i * Math.PI * 2 / n;
                        Vec3 lado = u.scale(Math.cos(ang)).add(w.scale(Math.sin(ang)));
                        pts.add(c.add(d.scale(Math.cos(th) * raio)).add(lado.scale(Math.sin(th) * raio)));
                    }
                    p.fita(pts, .06, 0xBFE8FF, alphas(pts.size(), a));
                }
                p.terminar();
                brilho(q, c.add(d.scale(raio)), .9, CIANO, (float) Math.exp(-t / 3) * .8f);
            }
        }
        if (v.forca > 1) {
            // Projétil parado: treme no ar com um halo azul até cair.
            Vec3 h = q.rel(v.origem).add(Math.sin(t * 9) * .04, Math.cos(t * 11) * .04, Math.sin(t * 13) * .04);
            esfera(q, Pincel.INFINITO, true, h, .28, 0x8CCCFF, op * .8f);
            brilho(q, h, .9, CIANO, op * .6f);
        }
    }

    private static Vec3 perpendicular(Vec3 d) {
        Vec3 u = d.cross(new Vec3(0, 1, 0));
        return (u.lengthSqr() < 1e-6 ? d.cross(new Vec3(1, 0, 0)) : u).normalize();
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
        brilho(q, azul, r * 3.5, CIANO, op * .5f);
        brilho(q, verm, r * 3.5, CARMESIM, op * .5f);
        double forma = CoroaRoxoGojo.formacao(t, carga);
        if (forma > 0) {
            esfera(q, Pincel.ROXO, false, centro, .9 * escala * forma, VIOLETA, op * (float) forma);
            brilho(q, centro, 3.5 * escala * forma, VIOLETA, op * (float) forma * .7f);
        }
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
        Vetor ponta = TrajetoriaGojo.roxo(v.origem, v.direcao, passo, t - 1);
        // O túnel já aberto brilha atrás da massa (depois, o rastro continua sozinho).
        tunelRoxo(q, v.origem, v.direcao, (float) Math.max(0, ponta.distancia(v.origem)), raio / 1.4,
                .7f * (float) PerfilVfxGojo.suave(t / 4));
        Vec3 c = q.rel(ponta);
        if (q.longe(c)) return;
        esfera(q, Pincel.ROXO, false, c, raio, VIOLETA, op);
        esfera(q, Pincel.ROXO, true, c, raio * 1.35, VIOLETA, op * .3f);
        float pulso = (float) (.8 + .2 * Math.sin(t * .9));
        brilho(q, c, raio * 3.5 * pulso, VIOLETA, op * .75f);
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

    /** Túnel do Roxo: faixa violeta ao longo do caminho, com brilhos espaçados. */
    private static void tunelRoxo(Quadro q, Vetor origem, Vetor direcao, float comprimento, double raio, float alpha) {
        if (alpha <= .004f || comprimento < 1) return;
        Vec3 o = q.rel(origem);
        Vec3 d = vec(direcao);
        int n = Math.max(2, (int) (comprimento / 2));
        List<Vec3> pts = new ArrayList<>(n + 1);
        float[] a = new float[n + 1];
        for (int k = 0; k <= n; k++) {
            double s = (double) k / n;
            pts.add(o.add(d.scale(s * comprimento)));
            // Entra suave perto do Gojo, para não cobrir a tela de quem disparou.
            a[k] = alpha * (float) PerfilVfxGojo.suave(s * comprimento / 3);
        }
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        p.fita(pts, raio * .8, VIOLETA, a);
        p.terminar();
        int espaco = q.detalhe.faixas() < 64 ? 12 : 6;
        for (double s = 4; s < comprimento; s += espaco) {
            brilho(q, o.add(d.scale(s)), raio * 1.5, VIOLETA, alpha * .45f);
        }
    }

    /** Marca escura com borda ciano que o Azul deixa no chão. */
    private static void marcaAzul(Quadro q, VisuaisAtivos.Visual v, float a) {
        if (a <= .004f) return;
        Vec3 c = q.rel(v.origem);
        if (q.longe(c)) return;
        double raio = 2.5;
        Pincel chao = Pincel.novo().comecar(Pincel.CHAO, false, q.tempo, new Vec3(0, 1, 0));
        chao.disco(c, raio, 0xFFFFFF, a * .7f, q.segmentos(32));
        chao.terminar();
        int n = q.segmentos(32);
        List<Vec3> anel = new ArrayList<>(n + 1);
        for (int k = 0; k <= n; k++) {
            double ang = k * Math.PI * 2 / n;
            anel.add(c.add(Math.cos(ang) * raio, .03, Math.sin(ang) * raio));
        }
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        p.fita(anel, .12, CIANO, alphas(anel.size(), a * .8f));
        p.terminar();
    }

    /**
     * Para quem vê o Gojo de fora: as palavras do encantamento girando em volta dele. Quem está na
     * cutscene já as vê na tela.
     */
    private static void palavrasNoMundo(Quadro q, VisuaisAtivos.Visual v, double t) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && v.dono == mc.player.getId()) return;
        Entity e = dono(v);
        if (e == null) return;
        Vec3 olhos = e.getEyePosition(q.parcial);
        if (q.longe(q.rel(olhos)) || q.rel(olhos).lengthSqr() > 48 * 48) return;
        net.minecraft.client.gui.Font fonte = mc.font;
        // Buffer próprio: descarregar o principal aqui adiantaria o que outros renders deixaram pendente.
        var buffers = net.minecraft.client.renderer.MultiBufferSource.immediate(BUFFER_TEXTO);
        var camera = mc.gameRenderer.getMainCamera();
        for (CutsceneRoxo200.Palavra p : CutsceneRoxo200.PALAVRAS) {
            if (t < p.inicio() || t > p.fim()) continue;
            double alpha = PerfilVfxGojo.suave((t - p.inicio()) / 4) * PerfilVfxGojo.suave((p.fim() - t) / 4);
            if (alpha < .03) continue;
            double ang = t * .06 + p.inicio();
            Vec3 pos = q.rel(olhos.add(Math.cos(ang) * 1.8, .7, Math.sin(ang) * 1.8));
            var pose = new com.mojang.blaze3d.vertex.PoseStack();
            pose.translate(pos.x, pos.y, pos.z);
            pose.mulPose(camera.rotation());
            pose.scale(.035f, -.035f, .035f);
            net.minecraft.network.chat.Component texto = net.minecraft.network.chat.Component.translatable(p.chave() + ".jp");
            int cor = ((int) (alpha * 255) << 24) | 0xE8D8FF;
            fonte.drawInBatch(texto, -fonte.width(texto) / 2f, 0, cor, false, pose.last().pose(), buffers,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
        buffers.endBatch();
    }

    // ---------------------------------------------------------------- Vazio Infinito

    private static void dominio(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Vec3 c = q.rel(v.origem);
        if (q.longe(c)) return;
        double raio = PerfilVfxGojo.raioDominio(t);
        if (raio < .2) return;
        // Nos últimos ticks o domo racha e se desfaz em vez de só sumir.
        double racha = PerfilVfxGojo.suave((t - (v.duracao - PerfilVfxGojo.DOMINIO_RACHA)) / PerfilVfxGojo.DOMINIO_RACHA);
        float opDomo = op * (float) (1 - racha);
        if (racha > 0) rachaduras(q, c, raio, racha);
        // O domo escuro é desenhado depois do terreno: de dentro, esconde o mundo além do raio;
        // de fora, é uma bolha escura. O teste de profundidade mantém o que está mais perto.
        Pincel p = Pincel.novo().comecar(Pincel.CEU, false, q.tempo, new Vec3(0, 0, 1));
        p.esfera(c, raio, 0xFFFFFF, opDomo, q.segmentos(48));
        p.terminar();
        // O chão desenhado fica no topo do bloco invisível que o servidor põe (pés arredondados para baixo).
        double yPiso = Math.floor(v.origem.y() - 1.62 + 1e-4) + .02;
        Vec3 pes = new Vec3(c.x, yPiso - q.camera.y, c.z);
        Pincel chao = Pincel.novo().comecar(Pincel.CHAO, false, q.tempo, new Vec3(0, 1, 0));
        chao.disco(pes, raio, 0xFFFFFF, opDomo * .85f, q.segmentos(48));
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

    /** Rachaduras de luz que correm pelo domo enquanto ele se desfaz ({@code racha} de 0 a 1). */
    private static void rachaduras(Quadro q, Vec3 c, double raio, double racha) {
        int n = q.detalhe.faixas() < 64 ? 6 : 12;
        float alpha = (float) Math.sin(racha * Math.PI);
        int passos = 10;
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        for (int f = 0; f < n; f++) {
            double teta = f * 2.399963;
            double fi = Math.acos(1 - 2 * (f + .5) / n);
            List<Vec3> pts = new ArrayList<>(passos + 1);
            for (int k = 0; k <= passos; k++) {
                double a = teta + k * .09 * (.4 + racha * 2);
                double b = fi + Math.sin(k * 1.7 + f) * .07;
                pts.add(c.add(Math.sin(b) * Math.cos(a) * raio, Math.cos(b) * raio, Math.sin(b) * Math.sin(a) * raio));
            }
            p.fita(pts, .18, 0xCFF6FF, alphas(pts.size(), alpha));
        }
        p.terminar();
    }

    /** Neblina escura dentro do domínio (só a cor; água e lava ficam como estão). */
    public static void aoCorDaNeblina(ViewportEvent.ComputeFogColor evento) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || evento.getCamera().getFluidInCamera() != net.minecraft.world.level.material.FogType.NONE) return;
        double presenca = Math.max(CeuRoxo.escuro() * .85,
                presencaDominio(evento.getCamera().getPosition(), (float) evento.getPartialTick()));
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
        // O céu escurece em volta do Gojo na segunda metade do encantamento do Roxo 200%.
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (!v.tipo.equals("gojo_roxo_200_carga") || v.fimEm >= 0) continue;
            double d = camera.distanceTo(vec(v.origem));
            double k = PerfilVfxGojo.suave((v.idade(agora, parcial) - 40) / 20) * Math.max(0, 1 - d / 32) * .85;
            maior = Math.max(maior, k);
        }
        return maior;
    }
}
