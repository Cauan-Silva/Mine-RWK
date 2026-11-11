package com.minerwk.client.vfx;

import com.minerwk.core.kit.Vetor;
import com.minerwk.core.traje.TrajeIchigo;
import com.minerwk.core.vfx.FormasIchigo;
import com.minerwk.core.vfx.GetsugaIchigo;
import com.minerwk.core.vfx.PerfilVfxGojo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Os visuais do Ichigo (direção de arte do Minezin): o reiatsu do Shikai é azul-claro e soma luz; o do
 * Bankai é tinta preta com a borda vermelha. Cor chapada com luz por vértice, sem textura. As formas vêm de
 * {@code core/vfx}; o servidor manda cada visual como {@code MostrarVisual} com o tipo começando em
 * {@code ichigo_}. Tudo relativo à câmera.
 */
final class RenderVisuaisIchigo {
    private static final int AZUL = 0x5FC4FF, BRANCO = 0xEAF8FF, TINTA = 0x0E0A12, RUBRO = 0xD32A50;
    private static final int NEGRO = 0x241A2A, MARFIM = 0xEDE8DE, POEIRA = 0xB9B0A0, PEDRA = 0x6A6258, FIO = 0xF6F4EB;
    private static final Vec3 CIMA = new Vec3(0, 1, 0);
    private static final double ALCANCE = 160;

    private static BufferBuilder buffer;

    private RenderVisuaisIchigo() {
    }

    static boolean eDoIchigo(String tipo) {
        return tipo.startsWith("ichigo_");
    }

    /** Desenha {@code v}; {@code t} é a idade em ticks. */
    static void desenhar(Vec3 camera, float parcial, VisuaisAtivos.Visual v, double t) {
        Vec3 o = new Vec3(v.origem.x() - camera.x, v.origem.y() - camera.y, v.origem.z() - camera.z);
        if (o.lengthSqr() > ALCANCE * ALCANCE) return;
        // Visual encerrado antes da hora (carga solta, lâmina que acabou): some em poucos ticks.
        float sai = v.fimEm < 0 ? 1 : (float) Math.max(0, 1 - (t - (v.fimEm - v.inicio)) / VisuaisAtivos.SAIDA);
        try {
            switch (v.tipo) {
                case "ichigo_getsuga" -> getsuga(camera, v, o, t, false);
                case "ichigo_getsuga_negro" -> getsuga(camera, v, o, t, true);
                case "ichigo_corte" -> corte(v, o, t);
                case "ichigo_carga" -> carga(camera, parcial, v, t, sai, false);
                case "ichigo_bankai_carga" -> carga(camera, parcial, v, t, sai, true);
                case "ichigo_shunpo" -> shunpo(v, o, t, false);
                case "ichigo_shunpo_bankai" -> shunpo(v, o, t, true);
                case "ichigo_onda" -> onda(o, t);
                case "ichigo_arremesso" -> arremesso(camera, parcial, v, o, t);
                case "ichigo_bankai" -> bankai(o, t);
                case "ichigo_lamina" -> lamina(camera, parcial, v, t, sai);
                case "ichigo_sair" -> sair(camera, parcial, v, o, t);
                default -> {
                }
            }
        } finally {
            if (buffer != null) {
                // Um desenho falhou no meio do lote: joga fora o que estava pela metade.
                MeshData resto = buffer.build();
                if (resto != null) resto.close();
                buffer = null;
                Pincel.restaurar();
            }
        }
    }

    // ---------------------------------------------------------------- lotes e primitivas

    /** Começa um lote: {@code aditivo} soma luz; {@code profundidade} grava a distância (corpos opacos). */
    private static void abrir(boolean aditivo, boolean profundidade) {
        RenderSystem.enableBlend();
        if (aditivo) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(profundidade);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void fechar() {
        MeshData malha = buffer.build();
        buffer = null;
        if (malha != null) BufferUploader.drawWithShader(malha);
        Pincel.restaurar();
    }

    private static void ponto(Vec3 p, int cor, float alpha) {
        buffer.addVertex((float) p.x, (float) p.y, (float) p.z)
                .setColor((cor >> 16) & 255, (cor >> 8) & 255, cor & 255, (int) (Mth.clamp(alpha, 0, 1) * 255));
    }

    private static void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int cor, float alpha) {
        ponto(a, cor, alpha);
        ponto(b, cor, alpha);
        ponto(c, cor, alpha);
        ponto(d, cor, alpha);
    }

    /** A cor escurecida ou clareada pela luz {@code l} (1 = a própria cor). */
    private static int luz(int cor, double l) {
        int r = (int) Math.min(255, ((cor >> 16) & 255) * l), g = (int) Math.min(255, ((cor >> 8) & 255) * l);
        int b = (int) Math.min(255, (cor & 255) * l);
        return r << 16 | g << 8 | b;
    }

    /** Fita de {@code a} a {@code b} virada para a câmera (que está na origem). */
    private static void fita(Vec3 a, Vec3 b, double largura, int cor, float alphaA, float alphaB) {
        Vec3 l = b.subtract(a).cross(a.add(b).scale(.5));
        if (l.lengthSqr() < 1e-12) return;
        l = l.normalize().scale(largura / 2);
        ponto(a.subtract(l), cor, alphaA);
        ponto(a.add(l), cor, alphaA);
        ponto(b.add(l), cor, alphaB);
        ponto(b.subtract(l), cor, alphaB);
    }

    /** Brilho redondo virado para a câmera: cheio no centro, zero na borda. */
    private static void halo(Vec3 c, double raio, int cor, float alpha) {
        if (alpha <= .004f || c.lengthSqr() < 1e-8) return;
        Vec3 ver = c.normalize();
        Vec3 x = ver.cross(CIMA);
        x = x.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : x.normalize();
        Vec3 y = x.cross(ver);
        leque(c, x.scale(raio), y.scale(raio), cor, alpha, 0);
    }

    /** Disco deitado no chão: {@code alpha} no centro, {@code borda} na beira. */
    private static void disco(Vec3 c, double raio, int cor, float alpha, float borda) {
        leque(c, new Vec3(raio, 0, 0), new Vec3(0, 0, raio), cor, alpha, borda);
    }

    private static void leque(Vec3 c, Vec3 x, Vec3 y, int cor, float alpha, float borda) {
        int n = 16;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            ponto(c, cor, alpha);
            ponto(c.add(x.scale(Math.cos(a0))).add(y.scale(Math.sin(a0))), cor, borda);
            ponto(c.add(x.scale(Math.cos(a1))).add(y.scale(Math.sin(a1))), cor, borda);
            ponto(c, cor, alpha);
        }
    }

    /** Anel deitado no chão, de {@code raio} e largura dados. */
    private static void anel(Vec3 c, double raio, double largura, int cor, float alpha) {
        int n = 36;
        double r0 = Math.max(0, raio - largura / 2), r1 = raio + largura / 2;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            Vec3 u0 = new Vec3(Math.cos(a0), 0, Math.sin(a0)), u1 = new Vec3(Math.cos(a1), 0, Math.sin(a1));
            quad(c.add(u0.scale(r0)), c.add(u0.scale(r1)), c.add(u1.scale(r1)), c.add(u1.scale(r0)), cor, alpha);
        }
    }

    /** Caixa alinhada ao mundo, com cada face num tom (parece sólida sem textura). */
    private static void caixa(Vec3 c, double hx, double hy, double hz, int cor, float alpha) {
        Vec3 a = c.add(-hx, -hy, -hz), b = c.add(hx, -hy, -hz), d = c.add(hx, -hy, hz), e = c.add(-hx, -hy, hz);
        Vec3 f = c.add(-hx, hy, -hz), g = c.add(hx, hy, -hz), h = c.add(hx, hy, hz), i = c.add(-hx, hy, hz);
        quad(f, g, h, i, cor, alpha);
        quad(a, b, d, e, luz(cor, .5), alpha);
        quad(a, b, g, f, luz(cor, .8), alpha);
        quad(e, d, h, i, luz(cor, .8), alpha);
        quad(a, e, i, f, luz(cor, .65), alpha);
        quad(b, d, h, g, luz(cor, .65), alpha);
    }

    private static double acaso(double n) {
        double x = Math.sin(n * 127.1 + 311.7) * 43758.5453;
        return x - Math.floor(x);
    }

    private static Vec3 vec(Vetor v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    private static Entity dono(VisuaisAtivos.Visual v) {
        return Minecraft.getInstance().level.getEntity(v.dono);
    }

    /** Lateral (direita), vertical e frente do golpe. */
    private record Base(Vec3 lado, Vec3 cima, Vec3 frente) {
        static Base de(Vec3 d) {
            Vec3 f = d.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : d.normalize();
            Vec3 l = f.cross(CIMA);
            l = l.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : l.normalize();
            return new Base(l, l.cross(f).normalize(), f);
        }

        Vec3 em(GetsugaIchigo.P p, double escala) {
            return lado.scale(p.x() * escala).add(cima.scale(p.y() * escala)).add(frente.scale(p.z() * escala));
        }
    }

    /** Para onde o corpo do dono está virado e o lado direito dele. */
    private static Base corpo(Entity e, float parcial) {
        float giro = e instanceof LivingEntity vivo ? Mth.lerp(parcial, vivo.yBodyRotO, vivo.yBodyRot) : e.getYRot();
        double rad = Math.toRadians(giro);
        return Base.de(new Vec3(-Math.sin(rad), 0, Math.cos(rad)));
    }

    /** Línguas de reiatsu subindo em volta do corpo, a partir dos pés. */
    private static void linguas(Vec3 pes, double t, double altura, double largura, int cor, float alpha) {
        int passos = 6;
        for (int j = 0; j < GetsugaIchigo.LINGUAS; j++) {
            Vec3 antes = null;
            for (int s = 0; s <= passos; s++) {
                double u = s / (double) passos;
                GetsugaIchigo.P q = GetsugaIchigo.lingua(j, u, t, altura);
                Vec3 p = pes.add(q.x(), q.y(), q.z());
                if (antes != null) {
                    double u0 = (s - 1) / (double) passos;
                    fita(antes, p, largura * (1 - u0 * .8), cor, alpha * (float) (1 - u0), alpha * (float) (1 - u));
                }
                antes = p;
            }
        }
    }

    // ---------------------------------------------------------------- Getsuga Tenshō

    private static void getsuga(Vec3 camera, VisuaisAtivos.Visual v, Vec3 o, double t, boolean bankai) {
        Base b = Base.de(vec(v.direcao));
        // Parou onde o servidor avisou (bloco) ou no alcance máximo; depois se parte em lascas.
        double fim = GetsugaIchigo.voo(bankai);
        if (v.fimEm >= 0) {
            double andado = v.pontoFim == null ? GetsugaIchigo.alcance(bankai) : v.origem.distancia(v.pontoFim);
            fim = GetsugaIchigo.parada(v.fimEm - v.inicio, andado, bankai);
        }
        double dis = GetsugaIchigo.dissolve(t, fim);
        float a = (float) (Math.min(1, (t + .5) / 1.5) * (1 - dis));
        double e = GetsugaIchigo.escala(t) * (bankai ? 1 : .85 + .3 * v.forca);
        double andou = GetsugaIchigo.distancia(Math.min(t, fim), bankai);
        Vec3 c = o.add(b.frente.scale(andou));
        sulco(camera, v, b, andou, t, bankai);
        int n = GetsugaIchigo.PONTOS, m = GetsugaIchigo.SECAO;
        if (dis < .6 && a > .01f) {
            float ac = a * (float) (1 - dis / .6);
            Vec3[][] vol = new Vec3[n][m + 1];
            double[][] lum = new double[n][m + 1], rim = new double[n][m + 1];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j <= m; j++) {
                    vol[i][j] = c.add(b.em(GetsugaIchigo.volume(i, j % m, t, bankai), e));
                    GetsugaIchigo.P no = GetsugaIchigo.normal(i, j % m, t, bankai);
                    Vec3 ver = vol[i][j].scale(-1);
                    double vx = ver.dot(b.lado), vy = ver.dot(b.cima), vz = ver.dot(b.frente);
                    lum[i][j] = GetsugaIchigo.luz(no, vx, vy, vz);
                    rim[i][j] = GetsugaIchigo.borda(no, vx, vy, vz);
                }
            }
            // Pós-imagens atrás da meia-lua.
            abrir(!bankai, false);
            for (int k = 1; k <= GetsugaIchigo.ECOS; k++) {
                double tk = t - k * 1.5;
                if (tk <= 0) continue;
                Vec3 recuo = b.frente.scale(GetsugaIchigo.distancia(Math.min(tk, fim), bankai) - andou);
                malha(vol, null, null, recuo, bankai ? TINTA : AZUL, (bankai ? (k == 1 ? .6f : .32f) : (k == 1 ? .34f : .15f)) * ac);
            }
            fechar();
            // O corpo: opaco e com luz no Bankai, quase branco no Shikai.
            abrir(false, ac > .9f);
            malha(vol, lum, null, Vec3.ZERO, bankai ? NEGRO : BRANCO, bankai ? ac : ac * .92f);
            fechar();
            abrir(true, false);
            // A borda de raspão dá o contorno de qualquer ângulo.
            malha(vol, null, rim, Vec3.ZERO, bankai ? RUBRO : AZUL, ac);
            // O fio da frente (e o contorno vermelho do dorso, no Bankai).
            for (int i = 0; i < n - 1; i++) {
                Vec3 f0 = c.add(b.em(GetsugaIchigo.frente(i, bankai), e)), f1 = c.add(b.em(GetsugaIchigo.frente(i + 1, bankai), e));
                fita(f0, f1, bankai ? .06 : .08, bankai ? RUBRO : BRANCO, ac, ac);
                fita(f0, f1, .32, bankai ? RUBRO : AZUL, ac * .25f, ac * .25f);
                if (bankai) {
                    fita(c.add(b.em(GetsugaIchigo.tras(i, t, true), e)), c.add(b.em(GetsugaIchigo.tras(i + 1, t, true), e)),
                            .045, RUBRO, ac * .8f, ac * .8f);
                }
            }
            halo(c.subtract(b.frente.scale(.4 * e)), 2.6 * e, bankai ? RUBRO : AZUL, ac * (bankai ? .16f : .3f));
            fechar();
            // Chamas saindo do dorso: azuis somando luz, ou de tinta com um fio vermelho.
            abrir(!bankai, false);
            chamas(c, b, e, t, bankai, bankai ? .15 : .11, bankai ? TINTA : AZUL, ac);
            fechar();
            if (bankai) {
                abrir(true, false);
                chamas(c, b, e, t, true, .04, RUBRO, ac * .6f);
                fechar();
            }
        }
        if (dis > 0 && a > .01f) {
            double tam = .14 * (1 - dis * .6);
            abrir(false, false);
            for (int k = 0; k < GetsugaIchigo.LASCAS; k++) {
                caixa(c.add(b.em(GetsugaIchigo.lasca(k, t, fim, bankai), e)), tam, tam * .6, tam, bankai ? NEGRO : BRANCO, a);
            }
            fechar();
            abrir(true, false);
            for (int k = 0; k < GetsugaIchigo.LASCAS; k++) {
                Vec3 q = c.add(b.em(GetsugaIchigo.lasca(k, t, fim, bankai), e));
                caixa(q, tam * 1.4, tam * .9, tam * 1.4, bankai ? RUBRO : AZUL, a * .35f);
            }
            halo(c, 2.8, bankai ? RUBRO : BRANCO, a * .45f);
            fechar();
        }
    }

    /** O rasgo que a meia-lua deixa no chão, só onde ela passa rente ao terreno. */
    private static void sulco(Vec3 camera, VisuaisAtivos.Visual v, Base b, double andou, double t, boolean bankai) {
        Vec3 h = new Vec3(b.frente.x, 0, b.frente.z);
        if (h.lengthSqr() < 1e-6) return;
        h = h.normalize();
        Vec3 lado = new Vec3(-h.z, 0, h.x), saida = vec(v.origem);
        var mundo = Minecraft.getInstance().level;
        java.util.List<Vec3[]> trechos = new java.util.ArrayList<>();
        java.util.List<Float> alphas = new java.util.ArrayList<>();
        for (double z = 1; z <= andou; z += 1) {
            float a = (float) GetsugaIchigo.sulco(z, t, bankai);
            if (a < .02f) continue;
            Vec3 q = saida.add(b.frente.scale(z));
            HitResult chao = mundo.clip(new ClipContext(q, q.add(0, -3, 0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (chao.getType() == HitResult.Type.MISS) continue;
            Vec3 p = chao.getLocation().subtract(camera).add(0, .03, 0);
            Vec3 l = lado.scale(GetsugaIchigo.larguraDoSulco(z, bankai));
            trechos.add(new Vec3[] {p.subtract(l), p.add(l), p.add(h.scale(1.02)).add(l), p.add(h.scale(1.02)).subtract(l)});
            alphas.add(a);
        }
        if (trechos.isEmpty()) return;
        abrir(false, false);
        for (int i = 0; i < trechos.size(); i++) {
            Vec3[] q = trechos.get(i);
            quad(q[0], q[1], q[2], q[3], bankai ? TINTA : 0x16222E, alphas.get(i) * .8f);
        }
        fechar();
        abrir(true, false);
        for (int i = 0; i < trechos.size(); i++) {
            Vec3[] q = trechos.get(i);
            float a = alphas.get(i) * .7f;
            Vec3 sobe = new Vec3(0, .02, 0);
            Vec3 borda = q[1].subtract(q[0]).scale(.12);
            quad(q[0].add(sobe), q[0].add(borda).add(sobe), q[3].add(borda).add(sobe), q[3].add(sobe), bankai ? RUBRO : AZUL, a);
            quad(q[1].add(sobe), q[1].subtract(borda).add(sobe), q[2].subtract(borda).add(sobe), q[2].add(sobe), bankai ? RUBRO : AZUL, a);
        }
        fechar();
    }

    /** O rastro do clique da espada: um arco na diagonal na frente do corpo, que a lâmina varre e que some. */
    private static void corte(VisuaisAtivos.Visual v, Vec3 o, double t) {
        int forma = Math.round(v.forca);
        boolean bankai = forma >= 1;
        Base b = Base.de(vec(v.direcao));
        // Um clique desce da direita, o seguinte da esquerda.
        double inclina = Math.toRadians(GetsugaIchigo.INCLINACAO) * ((v.inicio & 1) == 0 ? 1 : -1);
        Vec3 eixo = b.lado.scale(Math.sin(inclina)).add(b.cima.scale(Math.cos(inclina)));
        Vec3 c = o.add(0, -.3, 0);
        double raio = FormasIchigo.raioDoCorte(forma), abertura = FormasIchigo.CORTE_ABERTURA;
        int n = 22;
        Vec3[] fora = new Vec3[n + 1], dentro = new Vec3[n + 1];
        float[] alpha = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            // De cima para baixo: o arco começa no alto, do lado da espada.
            double a = -abertura + 2 * abertura * i / n, miolo = Math.pow(Math.cos(a / abertura * Math.PI / 2), .7);
            Vec3 rumo = b.frente.scale(Math.cos(a)).subtract(eixo.scale(Math.sin(a)));
            fora[i] = c.add(rumo.scale(raio));
            dentro[i] = c.add(rumo.scale(raio * (1 - .3 * miolo)));
            alpha[i] = (float) (FormasIchigo.corte(a, t) * miolo);
        }
        if (bankai) {
            abrir(false, false);
            for (int i = 0; i < n; i++) {
                ponto(dentro[i], TINTA, 0);
                ponto(fora[i], TINTA, alpha[i] * .9f);
                ponto(fora[i + 1], TINTA, alpha[i + 1] * .9f);
                ponto(dentro[i + 1], TINTA, 0);
            }
            fechar();
        }
        abrir(true, false);
        for (int i = 0; i < n; i++) {
            if (!bankai) {
                ponto(dentro[i], AZUL, 0);
                ponto(fora[i], BRANCO, alpha[i] * .8f);
                ponto(fora[i + 1], BRANCO, alpha[i + 1] * .8f);
                ponto(dentro[i + 1], AZUL, 0);
            }
            fita(fora[i], fora[i + 1], bankai ? .05 : .07, bankai ? RUBRO : BRANCO, alpha[i], alpha[i + 1]);
            if (forma >= 2) fita(fora[i], fora[i + 1], .3, RUBRO, alpha[i] * .35f, alpha[i + 1] * .35f);
        }
        fechar();
    }

    /**
     * A malha do volume. Com {@code lum} a cor recebe a luz de cada vértice; com {@code rim} a transparência
     * segue a borda de raspão; sem os dois, cor e transparência lisas.
     */
    private static void malha(Vec3[][] vol, double[][] lum, double[][] rim, Vec3 desvio, int cor, float alpha) {
        int[][] cantos = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int i = 0; i < vol.length - 1; i++) {
            for (int j = 0; j < vol[i].length - 1; j++) {
                for (int[] q : cantos) {
                    int x = i + q[0], y = j + q[1];
                    ponto(vol[x][y].add(desvio), lum == null ? cor : luz(cor, lum[x][y]),
                            rim == null ? alpha : alpha * (float) rim[x][y]);
                }
            }
        }
    }

    private static void chamas(Vec3 c, Base b, double e, double t, boolean bankai, double largura, int cor, float alpha) {
        int passos = 6;
        for (int j = 0; j < GetsugaIchigo.CHAMAS; j++) {
            Vec3 antes = null;
            for (int s = 0; s <= passos; s++) {
                Vec3 p = c.add(b.em(GetsugaIchigo.chama(j, s / (double) passos, t, bankai), e));
                if (antes != null) {
                    float a0 = alpha * (1 - (s - 1) / (passos + 1f)), a1 = alpha * (1 - s / (passos + 1f));
                    fita(antes, p, largura * (1 - (s - 1) / (passos + 1.0)), cor, a0, a1);
                }
                antes = p;
            }
        }
    }

    // ---------------------------------------------------------------- carga (Getsuga e Bankai)

    /** O reiatsu sobe pelo corpo enquanto o golpe carrega; segue o dono. */
    private static void carga(Vec3 camera, float parcial, VisuaisAtivos.Visual v, double t, float sai, boolean bankai) {
        Entity e = dono(v);
        if (e == null) return;
        Vec3 pes = e.getPosition(parcial).subtract(camera);
        double forca = PerfilVfxGojo.suave(t / (bankai ? 26.0 : 8.0));
        double altura = bankai ? 1.5 + 2.4 * forca : 1.2 + 1.3 * forca;
        float a = sai * (float) ((.35 + .65 * forca) * Math.min(1, t / 2));
        if (a <= .01f) return;
        // No Bankai o azul do Shikai escurece: a tinta sobe por dentro e o vermelho toma a borda.
        float escuro = bankai ? (float) PerfilVfxGojo.suave((t - 10) / 14) : 0;
        if (escuro > 0) {
            abrir(false, false);
            linguas(pes, t + 2, altura * .9, .22, TINTA, a * escuro * .85f);
            fechar();
        }
        abrir(true, false);
        linguas(pes, t, altura, .13, AZUL, a * (1 - escuro * .7f));
        if (escuro > 0) linguas(pes, t + 5, altura, .06, RUBRO, a * escuro);
        disco(pes.add(0, .05, 0), 1.3 + forca, bankai && escuro > .5f ? RUBRO : AZUL, a * .35f, 0);
        if (bankai) {
            // Anéis que correm do chão em volta para os pés: o reiatsu sendo puxado.
            for (int k = 0; k < 3; k++) {
                double f = ((t + k * 3.3) % 10) / 10;
                anel(pes.add(0, .06, 0), 3.4 * (1 - f) + .4, .1, escuro > .5f ? RUBRO : BRANCO, a * (float) f * .7f);
            }
        }
        halo(pes.add(0, 1, 0), 1.6 + forca, BRANCO, a * .12f);
        fechar();
    }

    // ---------------------------------------------------------------- Shunpo

    /** O passo: riscos de velocidade pelo caminho, vultos que ficam para trás e poeira na partida e na chegada. */
    private static void shunpo(VisuaisAtivos.Visual v, Vec3 o, double t, boolean bankai) {
        Vec3 d = vec(v.direcao);
        double comprimento = d.length();
        if (comprimento < .1) return;
        Base b = Base.de(d);
        float a = (float) Math.max(0, 1 - t / FormasIchigo.SHUNPO);
        double cabeca = Math.min(1, t / 2), cauda = Mth.clamp((t - 2) / 6, 0, 1);
        int riscos = 14;
        if (bankai) {
            abrir(false, false);
            riscos(o, d, b, riscos, cabeca, cauda, .12, TINTA, a * .8f);
            fechar();
        }
        abrir(true, false);
        riscos(o, d, b, riscos, cabeca, cauda, bankai ? .04 : .06, bankai ? RUBRO : BRANCO, a * .8f);
        fechar();
        abrir(false, false);
        double[] onde = {0, .35, .7};
        for (int k = 0; k < onde.length; k++) {
            float av = (float) FormasIchigo.posImagem(k, t - k);
            if (av <= .01f) continue;
            Vec3 meio = o.add(d.scale(onde[k])).add(0, -.82, 0);
            caixa(meio, .25, .7, .14, bankai ? TINTA : 0x1C2A3A, av);
            caixa(meio.add(0, .92, 0), .2, .2, .2, bankai ? TINTA : 0x1C2A3A, av);
        }
        // Poeira: um anel de pedrinhas abrindo nos pés, na partida e na chegada.
        for (int ponta = 0; ponta < 2; ponta++) {
            Vec3 pes = o.add(d.scale(ponta)).add(0, -1.55, 0);
            for (int k = 0; k < 9; k++) {
                double ang = Math.PI * 2 * k / 9 + acaso(k + ponta * 13) * .5, r = .3 + t * (.14 + .08 * acaso(k + 3));
                double lado = .09 * a;
                caixa(pes.add(Math.cos(ang) * r, .08 + t * .05 * acaso(k + 7), Math.sin(ang) * r), lado, lado, lado, POEIRA, a * .75f);
            }
        }
        fechar();
    }

    private static void riscos(Vec3 o, Vec3 d, Base b, int n, double cabeca, double cauda, double largura, int cor, float alpha) {
        for (int k = 0; k < n; k++) {
            double x = (acaso(k) - .5) * 1.1, y = -1.5 + acaso(k + 20) * 1.8;
            double s0 = Math.min(1, cauda + .15 * acaso(k + 40)), s1 = Math.max(s0, cabeca - .1 * acaso(k + 50));
            Vec3 base = o.add(b.lado.scale(x)).add(0, y, 0);
            fita(base.add(d.scale(s0)), base.add(d.scale(s1)), largura, cor, 0, alpha);
        }
    }

    // ---------------------------------------------------------------- Getsuga no chão

    /** A onda: uma parede de reiatsu que abre em volta, rachaduras no chão, pedras e um clarão. */
    private static void onda(Vec3 o, double t) {
        double r = FormasIchigo.raioDaOnda(t), h = FormasIchigo.alturaDaOnda(t);
        float a = (float) (1 - PerfilVfxGojo.suave((t - 8) / 8));
        if (a <= .01f) return;
        int n = 40;
        double troca = Math.floor(t / 2) * 7;
        abrir(true, false);
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            double h0 = h * (.55 + .9 * acaso(i + troca)), h1 = h * (.55 + .9 * acaso((i + 1) % n + troca));
            Vec3 u0 = new Vec3(Math.cos(a0), 0, Math.sin(a0)), u1 = new Vec3(Math.cos(a1), 0, Math.sin(a1));
            parede(o.add(u0.scale(r)), o.add(u1.scale(r)), h0, h1, AZUL, a * .8f);
            parede(o.add(u0.scale(r * .88)), o.add(u1.scale(r * .88)), h0 * .6, h1 * .6, BRANCO, a * .55f);
        }
        anel(o.add(0, .06, 0), r, .2, BRANCO, a);
        disco(o.add(0, .04, 0), r, AZUL, a * .3f, a * .1f);
        for (int k = 0; k < 12; k++) {
            double ang = Math.PI * 2 * k / 12 + acaso(k) * .4;
            Vec3 u = new Vec3(Math.cos(ang), 0, Math.sin(ang)), w = new Vec3(-u.z, 0, u.x).scale(.045);
            Vec3 de = o.add(u.scale(r * .15)).add(0, .05, 0), ate = o.add(u.scale(r * (.6 + .4 * acaso(k + 5)))).add(0, .05, 0);
            quad(de.subtract(w), de.add(w), ate.add(w), ate.subtract(w), BRANCO, a * .75f);
        }
        if (t < 5) halo(o.add(0, .7, 0), 2.6, BRANCO, (float) (1 - t / 5) * .45f);
        fechar();
        abrir(false, false);
        for (int k = 0; k < 14; k++) {
            double ang = Math.PI * 2 * acaso(k + 60), rr = r * (.45 + .55 * acaso(k + 70));
            double y = Math.max(0, (.3 + .25 * acaso(k + 80)) * t - .035 * t * t);
            double lado = .07 + .06 * acaso(k + 90);
            caixa(o.add(Math.cos(ang) * rr, .1 + y, Math.sin(ang) * rr), lado, lado, lado, PEDRA, a);
        }
        fechar();
    }

    /** Um pano da parede: cheio embaixo, transparente no alto. */
    private static void parede(Vec3 p0, Vec3 p1, double h0, double h1, int cor, float alpha) {
        ponto(p0, cor, alpha);
        ponto(p1, cor, alpha);
        ponto(p1.add(0, h1, 0), cor, 0);
        ponto(p0.add(0, h0, 0), cor, 0);
    }

    // ---------------------------------------------------------------- Arremesso

    /** A Zangetsu girando no ar, presa à mão pelo pano: vai até o alcance (ou o bloco) e volta. */
    private static void arremesso(Vec3 camera, float parcial, VisuaisAtivos.Visual v, Vec3 o, double t) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = dono(v);
        Base b = Base.de(vec(v.direcao));
        Vec3 saida = vec(v.origem);
        HitResult batida = mc.level.clip(new ClipContext(saida, saida.add(b.frente.scale(FormasIchigo.ARREMESSO_ALCANCE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        double bloqueio = batida.getType() == HitResult.Type.MISS ? FormasIchigo.ARREMESSO_ALCANCE
                : batida.getLocation().distanceTo(saida);
        double alcance = FormasIchigo.alcanceDoArremesso(bloqueio);
        double fracao = alcance < 1e-6 ? 0 : FormasIchigo.arremesso(t, alcance) / alcance;
        // A mão estendida do dono: é dela que a espada sai e para ela que volta.
        Vec3 mao = o.add(b.lado.scale(.38)).add(0, -.3, 0);
        if (e != null) {
            Base c = corpo(e, parcial);
            mao = e.getPosition(parcial).subtract(camera).add(0, 1.3, 0).add(c.lado.scale(.38)).add(c.frente.scale(.7));
        }
        Vec3 p = mao.lerp(o.add(b.frente.scale(alcance)), fracao);
        // A roda do giro fica inclinada: aparece para quem vê de trás e de lado.
        Vec3 u1 = b.lado, u2 = b.cima.scale(.8).add(b.frente.scale(.6)).normalize(), fora = u1.cross(u2);
        double g = FormasIchigo.giro(t);
        Vec3 eixo = u1.scale(Math.cos(g)).add(u2.scale(Math.sin(g)));
        Vec3 fio = u2.scale(Math.cos(g)).subtract(u1.scale(Math.sin(g)));
        abrir(false, true);
        // Pano do cabo até a mão, com barriga.
        Vec3 cabo = p.subtract(eixo.scale(.45));
        Vec3 antes = mao;
        int passos = 10;
        for (int s = 1; s <= passos; s++) {
            double u = s / (double) passos;
            Vec3 q = mao.lerp(cabo, u).add(0, -.35 * Math.sin(Math.PI * u) * Math.min(1, fracao * 3)
                    + .05 * Math.sin(u * 9 + t * 1.3), 0);
            fita(antes, q, .05, MARFIM, .95f, .95f);
            antes = q;
        }
        espada(p, eixo, fio, fora);
        fechar();
        abrir(true, false);
        // O rastro da ponta: um arco claro atrás do giro.
        Vec3 ponta = null;
        for (int s = 0; s <= 7; s++) {
            double gs = g - s * .22;
            Vec3 q = p.add(u1.scale(Math.cos(gs) * 1.4)).add(u2.scale(Math.sin(gs) * 1.4));
            if (ponta != null) fita(ponta, q, .16, BRANCO, .55f * (1 - (s - 1) / 7f), .55f * (1 - s / 7f));
            ponta = q;
        }
        fechar();
    }

    /** A Zangetsu em cor chapada: cabo claro, lâmina escura de dorso reto e o fio branco. */
    private static void espada(Vec3 p, Vec3 eixo, Vec3 fio, Vec3 fora) {
        tira(p, eixo, fio, -.45, -.05, -.05, .05, MARFIM);
        tira(p, eixo, fio, -.05, 1.2, -.17, .05, 0x1A1529);
        tira(p, eixo, fio, -.05, 1.2, .05, .17, FIO);
        // A ponta: o dorso segue reto e o fio sobe até ele.
        quad(em(p, eixo, fio, 1.2, -.17), em(p, eixo, fio, 1.45, -.17), em(p, eixo, fio, 1.45, -.15), em(p, eixo, fio, 1.2, .05),
                0x1A1529, 1);
        quad(em(p, eixo, fio, 1.2, .05), em(p, eixo, fio, 1.45, -.15), em(p, eixo, fio, 1.45, -.13), em(p, eixo, fio, 1.2, .17),
                FIO, 1);
        // A espessura, para a lâmina não sumir vista de perfil.
        tira(p, eixo, fora, -.45, 1.4, -.03, .03, 0x2A2438);
    }

    private static Vec3 em(Vec3 p, Vec3 eixo, Vec3 fio, double ao, double af) {
        return p.add(eixo.scale(ao)).add(fio.scale(af));
    }

    private static void tira(Vec3 p, Vec3 eixo, Vec3 fio, double de, double ate, double f0, double f1, int cor) {
        quad(em(p, eixo, fio, de, f0), em(p, eixo, fio, ate, f0), em(p, eixo, fio, ate, f1), em(p, eixo, fio, de, f1), cor, 1);
    }

    // ---------------------------------------------------------------- Bankai

    /** A liberação: um pilar de tinta com veios vermelhos, um anel de poeira pelo chão e chamas negras. */
    private static void bankai(Vec3 o, double t) {
        double r = FormasIchigo.raioDoPilar(t), h = FormasIchigo.alturaDoPilar(t);
        float a = (float) (1 - PerfilVfxGojo.suave((t - 18) / 14));
        double anel = FormasIchigo.anelDoBankai(t);
        float aa = (float) (1 - PerfilVfxGojo.suave((t - 4) / 10));
        if (r > .02) {
            abrir(false, false);
            int n = 24;
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                Vec3 p0 = o.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), p1 = o.add(Math.cos(a1) * r, 0, Math.sin(a1) * r);
                int cor = luz(NEGRO, .6 + .5 * Math.abs(Math.cos(a0 * 2)));
                quad(p0, p1, p1.add(0, h * .6, 0), p0.add(0, h * .6, 0), cor, a * .94f);
                ponto(p0.add(0, h * .6, 0), cor, a * .94f);
                ponto(p1.add(0, h * .6, 0), cor, a * .94f);
                ponto(p1.add(0, h, 0), cor, 0);
                ponto(p0.add(0, h, 0), cor, 0);
            }
            fechar();
        }
        abrir(true, false);
        if (r > .02) {
            // Veios vermelhos subindo em espiral pela superfície do pilar.
            for (int k = 0; k < 8; k++) {
                Vec3 antes = null;
                for (int s = 0; s <= 16; s++) {
                    double u = s / 16.0, ang = k * Math.PI / 4 + u * 5 + t * .5;
                    Vec3 p = o.add(Math.cos(ang) * r * 1.04, u * h, Math.sin(ang) * r * 1.04);
                    if (antes != null) fita(antes, p, .13, RUBRO, a * (float) (1 - (s - 1) / 16.0), a * (float) (1 - u));
                    antes = p;
                }
            }
            disco(o.add(0, .05, 0), r * 2.4, RUBRO, a * .4f, 0);
        }
        if (aa > .01f) {
            anel(o.add(0, .08, 0), anel, .4, RUBRO, aa * .8f);
            anel(o.add(0, .1, 0), anel * .93, .12, BRANCO, aa * .6f);
        }
        if (t < 4) halo(o.add(0, 1, 0), 5.5, BRANCO, (float) (1 - t / 4) * .7f);
        fechar();
        abrir(false, false);
        if (aa > .01f) {
            for (int k = 0; k < 18; k++) {
                double ang = Math.PI * 2 * k / 18 + acaso(k) * .3, rr = anel * (.8 + .3 * acaso(k + 9));
                double lado = .14 * Math.max(0, 1 - t / 20);
                caixa(o.add(Math.cos(ang) * rr, .15 + t * .07 * acaso(k + 4), Math.sin(ang) * rr), lado, lado, lado, POEIRA, aa);
            }
        }
        // Depois do pilar ficam as chamas negras em volta do corpo.
        float chama = a * (float) PerfilVfxGojo.suave((t - 5) / 6);
        linguas(o, t, 2.7, .22, TINTA, chama * .85f);
        fechar();
        abrir(true, false);
        linguas(o, t + 3, 2.3, .06, RUBRO, chama * .55f);
        fechar();
    }

    /** O Getsuga na lâmina: chamas de tinta e vermelho ao longo da Tensa Zangetsu e fiapos pelo corpo. */
    private static void lamina(Vec3 camera, float parcial, VisuaisAtivos.Visual v, double t, float sai) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = dono(v);
        if (e == null || !e.isAlive()) return;
        // Quem morreu ou trocou de personagem com a lâmina acesa não avisa o fim: sem o Bankai, ela apaga.
        if (e instanceof net.minecraft.client.player.AbstractClientPlayer p
                && !TrajeIchigo.BANKAI.equals(com.minerwk.client.traje.CamadaTraje.trajeDe(p))) return;
        float a = sai * (float) (Math.min(1, t / 4) * PerfilVfxGojo.suave((v.duracao - t) / 8));
        if (a <= .01f) return;
        Vec3 pes = e.getPosition(parcial).subtract(camera);
        Base c = corpo(e, parcial);
        // Em primeira pessoa a espada é desenhada na mão da tela: lá só os fiapos do corpo aparecem.
        boolean eu = e == mc.player && mc.options.getCameraType().isFirstPerson();
        Vec3 mao = pes.add(0, .8, 0).add(c.lado.scale(.38)).add(c.frente.scale(.25));
        Vec3 ponta = mao.add(c.frente.scale(1.5)).add(0, .15, 0);
        abrir(false, false);
        if (!eu) labaredas(mao, ponta, c, t, .17, TINTA, a * .85f);
        linguas(pes, t, 1.6, .08, TINTA, a * .45f);
        fechar();
        abrir(true, false);
        if (!eu) {
            labaredas(mao, ponta, c, t + 4, .05, RUBRO, a * .7f);
            fita(mao, ponta, .1, RUBRO, a * .5f, a * .5f);
            halo(mao.lerp(ponta, .5), .9, RUBRO, a * .2f);
        }
        linguas(pes, t + 5, 1.3, .04, RUBRO, a * .25f);
        fechar();
    }

    private static void labaredas(Vec3 mao, Vec3 ponta, Base c, double t, double largura, int cor, float alpha) {
        int n = 9;
        for (int k = 0; k < n; k++) {
            Vec3 base = mao.lerp(ponta, (k + .5) / n);
            double alto = .3 + .3 * acaso(k + Math.floor(t / 2) * 5);
            Vec3 meio = base.add(0, alto * .5, 0).subtract(c.frente.scale(.06)).add(c.lado.scale(.05 * Math.sin(t * .9 + k)));
            Vec3 topo = base.add(0, alto, 0).subtract(c.frente.scale(.18));
            fita(base, meio, largura, cor, alpha, alpha * .6f);
            fita(meio, topo, largura * .6, cor, alpha * .6f, 0);
        }
    }

    /** O Bankai se desfaz: lascas de tinta sobem do corpo e o azul do Shikai volta num brilho. */
    private static void sair(Vec3 camera, float parcial, VisuaisAtivos.Visual v, Vec3 o, double t) {
        Entity e = dono(v);
        Vec3 pes = e == null ? o.add(0, -1.62, 0) : e.getPosition(parcial).subtract(camera);
        float a = (float) Math.max(0, 1 - t / FormasIchigo.SAIR);
        abrir(false, false);
        for (int k = 0; k < 16; k++) {
            double ang = Math.PI * 2 * acaso(k), r = .45 + .4 * acaso(k + 11) + t * .03;
            double lado = .09 * a;
            caixa(pes.add(Math.cos(ang) * r, .3 + acaso(k + 22) * 1.4 + t * .09, Math.sin(ang) * r), lado, lado * 1.6, lado, TINTA, a);
        }
        fechar();
        abrir(true, false);
        halo(pes.add(0, 1, 0), 1.7, AZUL, (float) Math.max(0, 1 - t / 8) * .3f);
        fechar();
    }
}
