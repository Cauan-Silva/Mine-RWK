package com.minerwk.client.vfx;

import com.minerwk.client.ConfigCliente;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200;
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
        p.terminar();
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
     * Fim do R: a esfera roxa cresce até o raio (a forca do visual) no tempo do servidor (a duração menos
     * os 28 ticks de sumir), com clarão branco, e se apaga.
     */
    private static void explosaoRoxa(Quadro q, VisuaisAtivos.Visual v, double i) {
        Vec3 c = q.rel(v.origem);
        if (c.lengthSqr() > 400 * 400) return;
        double raio = v.forca > 1 ? v.forca : 24;
        double cresceEm = Math.max(1, v.duracao - 28);
        double cresce = PerfilVfxGojo.suave(i / cresceEm);
        float some = (float) Math.max(0, 1 - Math.max(0, i - cresceEm) / (v.duracao - cresceEm));
        esfera(q, Pincel.ROXO, true, c, 2 + (raio - 2) * cresce, 0xC890FF, .85f * some);
        esfera(q, Pincel.ROXO, true, c, 1 + raio * .5 * cresce, 0xFFFFFF, (float) Math.exp(-i / 4));
        brilho(q, c, raio * 1.25 + raio * .8 * cresce, 0x9A4DFF, .8f * some);
    }

    // ---------------------------------------------------------------- R (Vazio Roxo máximo)

    /** No encantamento do R: o Azul na mão esquerda e o Vermelho na direita, crescendo. */
    private static void encantamento(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Entity e = dono(v);
        if (e == null) return;
        double r = .12 + .3 * Math.min(1, t / Math.max(1, v.duracao));
        Vec3 azul = q.rel(mao(e, q.parcial, -1));
        Vec3 verm = q.rel(mao(e, q.parcial, 1));
        esfera(q, Pincel.AZUL, false, azul, r, 0x0A1830, op);
        esfera(q, Pincel.AZUL, true, azul, r * 1.8, CIANO, op * .4f);
        brilho(q, azul, r * 4, CIANO, op * .5f);
        esfera(q, Pincel.VERMELHO, false, verm, r, CARMESIM, op);
        esfera(q, Pincel.VERMELHO, true, verm, r * 1.8, CARMESIM, op * .35f);
        brilho(q, verm, r * 4, CARMESIM, op * .5f);
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
     * O Azul (mão esquerda) e o Vermelho (direita) saem em arcos e se encontram no centro (a origem) em
     * {@code forca} ticks; depois o núcleo roxo pulsa e cresce até a explosão.
     */
    private static void esferasRoxoMaximo(Quadro q, VisuaisAtivos.Visual v, double t, float op) {
        Vec3 centro = q.rel(v.origem);
        if (q.longe(centro)) return;
        double viagem = Math.max(1, v.forca);
        Entity e = dono(v);
        if (t < viagem && e != null) {
            double s = PerfilVfxGojo.suave(t / viagem);
            Base b = Base.de(v.direcao);
            Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
            for (int lado = -1; lado <= 1; lado += 2) {
                Vec3 de = q.rel(mao(e, q.parcial, lado));
                double curva = 1 + .35 * de.distanceTo(centro);
                int cor = lado < 0 ? CIANO : CARMESIM;
                List<Vec3> pts = new ArrayList<>();
                int n = 10;
                float[] al = new float[n + 1];
                for (int k = 0; k <= n; k++) {
                    double sk = Math.max(0, s - .35 * (n - k) / n);
                    pts.add(arco(de, centro, b, lado, curva, sk));
                    al[k] = op * .7f * k / n;
                }
                p.fita(pts, .35, cor, al);
                Vec3 bola = arco(de, centro, b, lado, curva, s);
                if (lado < 0) {
                    esfera(q, Pincel.AZUL, false, bola, .45, 0x0A1830, op);
                    esfera(q, Pincel.AZUL, true, bola, .8, CIANO, op * .45f);
                } else {
                    esfera(q, Pincel.VERMELHO, false, bola, .45, CARMESIM, op);
                    esfera(q, Pincel.VERMELHO, true, bola, .8, CARMESIM, op * .4f);
                }
                brilho(q, bola, 2.2, cor, op * .6f);
            }
            p.terminar();
            return;
        }
        // Fusão: clarão no encontro, núcleo roxo pulsando e anéis girando.
        double f = t - viagem;
        double k = Math.min(1, f / Math.max(1, v.duracao - viagem));
        double pulso = 1 + .18 * Math.sin(f * 1.3);
        double r = (1 + 2.5 * k) * pulso;
        esfera(q, Pincel.ROXO, false, centro, r, VIOLETA, op);
        esfera(q, Pincel.ROXO, true, centro, r * 1.6, 0xC890FF, op * .45f);
        brilho(q, centro, r * 4, 0x9A4DFF, op * .8f);
        float clarao = (float) Math.exp(-f / 2) * op;
        esfera(q, Pincel.ROXO, true, centro, 1.5 + f * .4, 0xFFFFFF, clarao);
        Pincel p = Pincel.novo().comecar(Pincel.FITA, true, q.tempo, new Vec3(0, 0, 1));
        int n = q.segmentos(32);
        for (int anel = 0; anel < 3; anel++) {
            double giro = q.tempo * (2 + anel) + anel * 2.1;
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
