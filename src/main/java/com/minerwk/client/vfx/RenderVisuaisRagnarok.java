package com.minerwk.client.vfx;

import com.minerwk.core.boss.RagnarokBoss;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Os visuais dos golpes do Ragnarok: fogo e magia carmesim, que somam luz, e a terra queimada e o slime do
 * Shadow, que são tinta escura. Cor chapada por vértice, sem textura. O servidor manda cada um como
 * {@code MostrarVisual} com o tipo começando em {@code ragnarok_}; os avisos no chão continuam sendo as
 * marcas do Toji. Tudo relativo à câmera.
 */
final class RenderVisuaisRagnarok {
    private static final int FOGO = 0xFF7A1A, BRASA = 0xFFC860, MIOLO = 0xFFF2D8, CARMIM = 0xD8203A,
            QUEIMADO = 0x160C0A, TINTA = 0x0B0912, MAGIA = 0x7A5CFF;
    private static final Vec3 CIMA = new Vec3(0, 1, 0);
    private static final double FOLGA = .07;

    private static BufferBuilder buffer;

    private RenderVisuaisRagnarok() {
    }

    static boolean eDoRagnarok(String tipo) {
        return tipo.startsWith("ragnarok_");
    }

    /** Desenha {@code v}; {@code t} é a idade em ticks. */
    static void desenhar(Vec3 camera, float parcial, VisuaisAtivos.Visual v, double t) {
        Vec3 o = new Vec3(v.origem.x() - camera.x, v.origem.y() - camera.y, v.origem.z() - camera.z);
        Vec3 d = new Vec3(v.direcao.x(), v.direcao.y(), v.direcao.z());
        d = d.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : d.normalize();
        try {
            switch (v.tipo) {
                case "ragnarok_cratera" -> cratera(o, v.forca, t, v.duracao);
                case "ragnarok_projetil" -> projetil(o, d, v.forca, t);
                case "ragnarok_carga" -> carga(o, t, v.duracao);
                case "ragnarok_sopro" -> sopro(camera, o, d, v.forca, t, v.duracao);
                case "ragnarok_onda" -> onda(o, v.forca, t, v.duracao);
                case "ragnarok_corte" -> corte(o, d, t, v.duracao);
                default -> {
                }
            }
        } finally {
            if (buffer != null) {
                MeshData resto = buffer.build();
                if (resto != null) resto.close();
                buffer = null;
                Pincel.restaurar();
            }
        }
    }

    // ---------------------------------------------------------------- lotes e primitivas

    private static void abrir(boolean aditivo) {
        RenderSystem.enableBlend();
        if (aditivo) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
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
        if (alpha <= .004f || c.lengthSqr() < 1e-8 || raio <= 0) return;
        Vec3 ver = c.normalize();
        Vec3 x = ver.cross(CIMA);
        x = (x.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : x.normalize()).scale(raio);
        Vec3 y = x.cross(ver);
        int n = 14;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            ponto(c, cor, alpha);
            ponto(c.add(x.scale(Math.cos(a0))).add(y.scale(Math.sin(a0))), cor, 0);
            ponto(c.add(x.scale(Math.cos(a1))).add(y.scale(Math.sin(a1))), cor, 0);
            ponto(c, cor, alpha);
        }
    }

    /** Anel deitado no chão, de {@code dentro} a {@code fora}; o alpha cai de {@code aDentro} para {@code aFora}. */
    private static void anel(Vec3 c, double dentro, double fora, int cor, float aDentro, float aFora) {
        if (fora <= dentro) return;
        int n = 36;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            Vec3 u0 = new Vec3(Math.cos(a0), 0, Math.sin(a0)), u1 = new Vec3(Math.cos(a1), 0, Math.sin(a1));
            ponto(c.add(u0.scale(dentro)), cor, aDentro);
            ponto(c.add(u0.scale(fora)), cor, aFora);
            ponto(c.add(u1.scale(fora)), cor, aFora);
            ponto(c.add(u1.scale(dentro)), cor, aDentro);
        }
    }

    /** Um número de 0 a 1 que só depende de {@code k}: o mesmo em todo quadro. */
    private static double acaso(double k) {
        double x = Math.sin(k * 127.1 + 311.7) * 43758.5453;
        return x - Math.floor(x);
    }

    private static Vec3 girar(Vec3 v, double graus) {
        double a = Math.toRadians(graus), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    private static Vec3 plano(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : h.normalize();
    }

    // ---------------------------------------------------------------- os golpes

    /** O murro: a terra queimada abre, o aro de fogo corre para fora, rachaduras em brasa e pedras saltando. */
    private static void cratera(Vec3 o, double raio, double t, int duracao) {
        double p = Mth.clamp(t / duracao, 0, 1), sai = 1 - p;
        Vec3 c = o.add(0, FOLGA, 0);
        abrir(false);
        anel(c, 0, raio * Math.min(1, t / 2), QUEIMADO, (float) (.8 * Math.sqrt(sai)), (float) (.5 * sai));
        // As pedras: sobem, giram e caem de volta (somem ao tocar o chão).
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * acaso(k), h = .12 + .1 * acaso(k + 30), sobe = .35 + .3 * acaso(k + 60);
            double y = sobe * t - .045 * t * t;
            if (y < 0 && t > 2) continue;
            Vec3 pedra = c.add(Math.cos(a) * (raio * .4 + h * t), Math.max(0, y), Math.sin(a) * (raio * .4 + h * t));
            double tam = .18 + .2 * acaso(k + 90);
            fita(pedra, pedra.add(girar(new Vec3(tam, tam * .6, 0), t * 40 + k * 30)), tam, QUEIMADO, .95f, .95f);
        }
        fechar();
        abrir(true);
        double aro = raio * (.4 + 1.1 * (1 - Math.pow(1 - Math.min(1, t / 10), 3)));
        anel(c.add(0, .01, 0), Math.max(0, aro - .7), aro, FOGO, (float) (.15 * sai), (float) (.9 * sai));
        anel(c.add(0, .015, 0), Math.max(0, aro - .25), aro, BRASA, 0, (float) (.8 * sai));
        // As rachaduras em brasa, do centro para fora, que esfriam devagar.
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2 * (k + .3 * acaso(k + 7)) / 8;
            Vec3 u = new Vec3(Math.cos(a), 0, Math.sin(a)), ponta = u.scale(raio * (.9 + .4 * acaso(k + 3)) * Math.min(1, t / 3));
            Vec3 meio = u.scale(ponta.length() * .5).add(girar(u, 90).scale(.4 * (acaso(k + 11) - .5)));
            Vec3 b = c.add(0, .02, 0);
            fita(b, b.add(meio), .22, FOGO, (float) sai, (float) (.8 * sai));
            fita(b.add(meio), b.add(ponta), .16, FOGO, (float) (.8 * sai), 0);
        }
        halo(c.add(0, .8, 0), raio * 1.3 * (1 - p * .5), FOGO, (float) (.6 * Math.max(0, 1 - t / 6)));
        halo(c.add(0, .5, 0), raio * .6, MIOLO, (float) (.7 * Math.max(0, 1 - t / 3)));
        fechar();
    }

    /** Um projétil da rajada: a bola de magia carmesim com o rastro, e o estouro quando para. */
    private static void projetil(Vec3 o, Vec3 d, double ate, double t) {
        double andou = Math.min(ate, RagnarokBoss.PASSO_RAJADA * t);
        Vec3 p = o.add(d.scale(andou));
        abrir(true);
        if (andou < ate) {
            double rastro = Math.min(4, andou);
            fita(p.subtract(d.scale(rastro)), p, .6, CARMIM, 0, .75f);
            fita(p.subtract(d.scale(rastro * .6)), p, .22, BRASA, 0, .8f);
            double pulsa = 1 + .15 * Math.sin(t * 1.7);
            halo(p, 1.0 * pulsa, CARMIM, .75f);
            halo(p, .5 * pulsa, FOGO, .85f);
            halo(p, .22, MIOLO, 1f);
        } else {
            double e = (t - ate / RagnarokBoss.PASSO_RAJADA) / 6;
            if (e < 1) {
                halo(p, .8 + 1.8 * e, CARMIM, (float) (.8 * (1 - e)));
                halo(p, .4 + .6 * e, MIOLO, (float) (.9 * (1 - e)));
            }
        }
        fechar();
    }

    /** O fogo sendo puxado para a boca nos 2 s do Sopro: a bola cresce e as chamas correm para dentro. */
    private static void carga(Vec3 boca, double t, int duracao) {
        double p = Mth.clamp(t / duracao, 0, 1);
        abrir(true);
        for (int k = 0; k < 14; k++) {
            double fase = (t * .06 + acaso(k)) % 1;
            double a = Math.PI * 2 * acaso(k + 20), b = (acaso(k + 40) - .5) * 2.4;
            Vec3 fora = new Vec3(Math.cos(a) * Math.cos(b), Math.sin(b) * .6, Math.sin(a) * Math.cos(b)).scale(4.5 * (1 - fase) + .4);
            fita(boca.add(fora), boca.add(fora.scale(.6)), .12, k % 2 == 0 ? FOGO : CARMIM, 0, (float) (.8 * p * Math.sin(Math.PI * fase)));
        }
        double pulsa = 1 + .1 * Math.sin(t * 1.3);
        halo(boca, (.4 + 2.2 * p) * pulsa, CARMIM, (float) (.35 + .4 * p));
        halo(boca, (.25 + 1.0 * p) * pulsa, FOGO, (float) (.4 + .4 * p));
        halo(boca, .15 + .45 * p, MIOLO, (float) (.5 + .5 * p));
        fechar();
    }

    /**
     * O Sopro do Demônio: o feixe sai da boca, bate no chão à frente dele e corre rente a ele, varrendo de um
     * lado ao outro. {@code o} são os pés dele, {@code d} a mira travada e {@code sentido} para que lado varre:
     * as contas repetem as do servidor (RagnarokEntity.soprar).
     */
    private static void sopro(Vec3 camera, Vec3 o, Vec3 d, double sentido, double t, int duracao) {
        Vec3 frente = plano(d);
        double x = Mth.clamp(t / Math.max(1, duracao - 1), 0, 1);
        Vec3 rumo = girar(frente, (sentido < 0 ? -1 : 1) * RagnarokBoss.VARREDURA * (x - .5));
        Vec3 boca = o.add(0, RagnarokBoss.ALTURA * .82, 0).add(frente.scale(1.4));
        Vec3 de = o.add(0, 1, 0).add(rumo.scale(RagnarokBoss.LARGURA / 2 + 1));
        double alcance = RagnarokBoss.ALCANCE_SOPRO * Math.min(1, t / 3 + .1);
        Vec3 fim = o.add(0, 1, 0).add(rumo.scale(alcance));
        var nivel = Minecraft.getInstance().level;
        if (nivel != null) {
            Vec3 a = de.add(camera), b = fim.add(camera);
            HitResult r = nivel.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (r.getType() != HitResult.Type.MISS) fim = r.getLocation().subtract(camera);
        }
        double sai = Mth.clamp((duracao - t) / 4, 0, 1);
        double treme = 1 + .12 * Math.sin(t * 2.3) + .08 * Math.sin(t * 5.1);
        abrir(true);
        for (double[] camada : new double[][] {{2.6, .4}, {1.5, .7}, {.6, .95}}) {
            int cor = camada[0] > 2 ? CARMIM : camada[0] > 1 ? FOGO : MIOLO;
            float a = (float) (camada[1] * sai);
            fita(boca, de, camada[0] * treme * .8, cor, a, a);
            fita(de, fim, camada[0] * treme, cor, a, (float) (a * .85));
        }
        halo(boca, 1.6 * treme, FOGO, (float) (.8 * sai));
        halo(fim, 3.2 * treme, FOGO, (float) (.7 * sai));
        halo(fim, 1.4, MIOLO, (float) (.8 * sai));
        // As línguas de fogo subindo ao longo do feixe.
        double longe = fim.subtract(de).length();
        for (int k = 0; k < 12; k++) {
            double s = (acaso(k) + t * .05) % 1, sobe = (t * .25 + acaso(k + 9)) % 1;
            Vec3 p = de.add(rumo.scale(longe * s)).add(0, .3 + sobe * 2.2, 0);
            fita(p, p.add(0, .7, 0), .35, k % 3 == 0 ? CARMIM : FOGO, (float) (.7 * sai * (1 - sobe)), 0);
        }
        fechar();
    }

    /** O pisão: um aro de fogo que corre pelo chão até {@code raio}, com as chamas subindo dele. */
    private static void onda(Vec3 o, double raio, double t, int duracao) {
        double r = 1 + (raio - 1) * Mth.clamp((t + 1) / duracao, 0, 1);
        double sai = Mth.clamp((duracao + 6 - t) / 8, 0, 1);
        Vec3 c = o.add(0, FOLGA, 0);
        double larg = RagnarokBoss.LARGURA_DA_ONDA;
        abrir(false);
        anel(c, Math.max(0, r - larg * 2.5), Math.max(0, r - larg), QUEIMADO, 0, (float) (.55 * sai));
        fechar();
        abrir(true);
        anel(c.add(0, .01, 0), Math.max(0, r - larg), r, FOGO, (float) (.9 * sai), (float) (.2 * sai));
        anel(c.add(0, .015, 0), Math.max(0, r - larg * .4), r, BRASA, 0, (float) (.8 * sai));
        int n = 28;
        for (int k = 0; k < n; k++) {
            double a = Math.PI * 2 * (k + acaso(k)) / n, h = .6 + .9 * acaso(k + 50) * (1 + .3 * Math.sin(t * .8 + k));
            Vec3 b = c.add(Math.cos(a) * (r - larg * .5), 0, Math.sin(a) * (r - larg * .5));
            fita(b, b.add(0, h, 0), .5, k % 3 == 0 ? CARMIM : FOGO, (float) (.85 * sai), 0);
        }
        fechar();
    }

    /** O corte do Shadow na asa: um risco preto que atravessa a asa de uma vez, com o fio de magia e o respingo. */
    private static void corte(Vec3 o, Vec3 d, double t, int duracao) {
        double cabeca = Math.min(1, t / 2.5), cauda = Mth.clamp((t - 3) / (duracao - 3), 0, 1);
        double meio = 5.5;
        Vec3 a = o.subtract(d.scale(meio)), b = o.add(d.scale(meio));
        Vec3 de = a.add(b.subtract(a).scale(cauda)), ate = a.add(b.subtract(a).scale(cabeca));
        float some = (float) (1 - cauda);
        abrir(false);
        fita(de, ate, .55, TINTA, some, some);
        fechar();
        abrir(true);
        fita(de, ate, 1.1, MAGIA, (float) (.55 * some), (float) (.8 * some));
        fita(de, ate, .18, MIOLO, (float) (.6 * some), (float) (.9 * some));
        halo(ate, 1.2, MAGIA, (float) (.7 * some));
        // O respingo: gotas carmesim saindo do corte, caindo.
        if (t > 2) {
            for (int k = 0; k < 16; k++) {
                double s = acaso(k), tt = t - 2;
                Vec3 base = a.add(b.subtract(a).scale(s));
                Vec3 v = new Vec3(acaso(k + 20) - .5, .2 + .3 * acaso(k + 40), acaso(k + 60) - .5).scale(.35);
                Vec3 p = base.add(v.scale(tt)).add(0, -.03 * tt * tt, 0);
                fita(p, p.add(v.scale(.8)), .14, CARMIM, (float) (.9 * some), 0);
            }
        }
        fechar();
    }
}
