package com.minerwk.client.vfx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.minerwk.core.boss.TojiBoss;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * As marcas dos golpes do Toji: secas, de cor chapada (branco, vermelho escuro, preto, aço), sem o brilho
 * de energia amaldiçoada dos visuais do Gojo. O servidor manda cada uma como {@code MostrarVisual} com o
 * tipo começando em {@code toji_}; são elas que avisam os golpes, e na fase 3 são o único aviso.
 */
final class RenderVisuaisToji {
    private static final int BRANCO = 0xF2F2F2, VERMELHO = 0xB01818, PRETO = 0x0A0A0A, ACO = 0x9AA0A8;
    /** Altura acima do chão, para a marca não brigar com o bloco. */
    private static final double FOLGA = .06;
    private static final double BORDA = .07;

    private static BufferBuilder buffer;

    private RenderVisuaisToji() {
    }

    /** Desenha {@code v} se for do Toji; {@code t} é a idade em ticks. */
    static void desenhar(Vec3 camera, float parcial, VisuaisAtivos.Visual v, double t) {
        if (!v.tipo.startsWith("toji_")) return;
        Vec3 o = new Vec3(v.origem.x() - camera.x, v.origem.y() - camera.y, v.origem.z() - camera.z);
        Vec3 d = new Vec3(v.direcao.x(), v.direcao.y(), v.direcao.z());
        double p = v.duracao <= 0 ? 1 : Mth.clamp(t / v.duracao, 0, 1);
        // Quanto do aviso já passou: enche a marca até o golpe sair (os últimos ticks são o golpe).
        double carga = v.duracao <= 4 ? 1 : Mth.clamp(t / (v.duracao - 4), 0, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        try {
            switch (v.tipo) {
                case "toji_linha" -> linha(o, d, v.forca, carga);
                case "toji_setor" -> setor(o, d, v.forca, carga);
                case "toji_sombra" -> sombra(o, v.forca, carga);
                case "toji_marca" -> marca(camera, parcial, v, o, t);
                case "toji_estocada" -> {
                    fita(o, d, v.forca, .3, BRANCO, (float) (.3 * (1 - p)), 0);
                    fita(o, d, v.forca, .08, BRANCO, (float) (1 - p), 0);
                }
                case "toji_corrente" -> fita(o, d, v.forca, .07, ACO, (float) (.55 + .4 * p), .32);
                default -> {
                }
            }
        } finally {
            MeshData malha = buffer.build();
            buffer = null;
            if (malha != null) BufferUploader.drawWithShader(malha);
            Pincel.restaurar();
        }
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

    private static Vec3 plano(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : h.normalize();
    }

    /** O corredor da investida no chão: enche de vermelho da origem até a ponta enquanto o aviso corre. */
    private static void linha(Vec3 o, Vec3 rumo, double comprimento, double carga) {
        Vec3 d = plano(rumo);
        Vec3 lado = new Vec3(-d.z, 0, d.x);
        Vec3 base = o.add(0, FOLGA, 0);
        faixa(base, d, lado, 0, comprimento, 1, VERMELHO, .16f);
        faixa(base.add(0, .005, 0), d, lado, 0, comprimento * carga, 1, VERMELHO, .34f);
        Vec3 cima = base.add(0, .01, 0);
        faixa(cima.add(lado.scale(1 - BORDA / 2)), d, lado, 0, comprimento, BORDA / 2, BRANCO, .85f);
        faixa(cima.subtract(lado.scale(1 - BORDA / 2)), d, lado, 0, comprimento, BORDA / 2, BRANCO, .85f);
        faixa(cima, d, lado, comprimento - BORDA, comprimento, 1, BRANCO, .85f);
    }

    /** Retângulo no chão ao longo de {@code d}, de {@code de} a {@code ate}, com meia largura {@code meia}. */
    private static void faixa(Vec3 base, Vec3 d, Vec3 lado, double de, double ate, double meia, int cor, float alpha) {
        if (ate <= de) return;
        Vec3 a = base.add(d.scale(de)), b = base.add(d.scale(ate)), l = lado.scale(meia);
        quad(a.subtract(l), b.subtract(l), b.add(l), a.add(l), cor, alpha);
    }

    /** O setor da Nuvem Brincalhona: um leque que enche do centro para a borda. */
    private static void setor(Vec3 o, Vec3 rumo, double raio, double carga) {
        Vec3 d = plano(rumo);
        double meio = Math.atan2(d.z, d.x);
        double abertura = Math.toRadians(TojiBoss.ANGULO_NUVEM);
        int n = 20;
        Vec3 base = o.add(0, FOLGA, 0);
        for (int i = 0; i < n; i++) {
            double a0 = meio - abertura / 2 + abertura * i / n;
            double a1 = meio - abertura / 2 + abertura * (i + 1) / n;
            Vec3 u0 = new Vec3(Math.cos(a0), 0, Math.sin(a0)), u1 = new Vec3(Math.cos(a1), 0, Math.sin(a1));
            quad(base, base.add(u0.scale(raio)), base.add(u1.scale(raio)), base, VERMELHO, .16f);
            Vec3 c = base.add(0, .005, 0);
            quad(c, c.add(u0.scale(raio * carga)), c.add(u1.scale(raio * carga)), c, VERMELHO, .34f);
            Vec3 b = base.add(0, .01, 0);
            quad(b.add(u0.scale(raio - BORDA)), b.add(u0.scale(raio)), b.add(u1.scale(raio)), b.add(u1.scale(raio - BORDA)),
                    BRANCO, .85f);
        }
        for (int s = -1; s <= 1; s += 2) {
            double a = meio + s * abertura / 2;
            Vec3 u = new Vec3(Math.cos(a), 0, Math.sin(a));
            faixa(base.add(0, .01, 0), u, new Vec3(-u.z, 0, u.x), 0, raio, BORDA / 2, BRANCO, .85f);
        }
    }

    /** A sombra do Desaparecer: cresce no chão dentro do círculo onde o golpe vai cair. */
    private static void sombra(Vec3 o, double raio, double carga) {
        Vec3 base = o.add(0, FOLGA, 0);
        anel(base, 0, raio * (.35 + .65 * carga), PRETO, .55f);
        anel(base.add(0, .01, 0), raio - BORDA, raio, VERMELHO, .9f);
    }

    private static void anel(Vec3 c, double dentro, double fora, int cor, float alpha) {
        int n = 28;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            Vec3 u0 = new Vec3(Math.cos(a0), 0, Math.sin(a0)), u1 = new Vec3(Math.cos(a1), 0, Math.sin(a1));
            quad(c.add(u0.scale(dentro)), c.add(u0.scale(fora)), c.add(u1.scale(fora)), c.add(u1.scale(dentro)), cor, alpha);
        }
    }

    /** A marca da Execução do Assassino: um anel que fecha nos pés do alvo e o segue, com quatro riscos girando. */
    private static void marca(Vec3 camera, float parcial, VisuaisAtivos.Visual v, Vec3 origem, double t) {
        Vec3 c = origem;
        Entity alvo = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(v.dono);
        if (alvo != null) c = alvo.getPosition(parcial).subtract(camera);
        c = c.add(0, FOLGA, 0);
        double raio = v.forca * (1 + 1.5 * Math.max(0, 1 - t / 20));
        anel(c, raio - BORDA * 1.5, raio, VERMELHO, .95f);
        for (int i = 0; i < 4; i++) {
            double a = t * .12 + Math.PI / 2 * i;
            Vec3 u = new Vec3(Math.cos(a), 0, Math.sin(a));
            faixa(c.add(0, .005, 0), u, new Vec3(-u.z, 0, u.x), raio - .1, raio + .45, BORDA, VERMELHO, .95f);
        }
    }

    /**
     * Fita virada para a câmera (que está na origem), de {@code o} por {@code comprimento} na direção
     * {@code rumo}. Com {@code elo} maior que zero ela sai tracejada, como os elos de uma corrente.
     */
    private static void fita(Vec3 o, Vec3 rumo, double comprimento, double largura, int cor, float alpha, double elo) {
        if (rumo.lengthSqr() < 1e-8 || comprimento <= 0) return;
        Vec3 d = rumo.normalize();
        Vec3 lado = d.cross(o.add(d.scale(comprimento / 2)));
        if (lado.lengthSqr() < 1e-10) lado = d.cross(new Vec3(0, 1, 0));
        if (lado.lengthSqr() < 1e-10) return;
        lado = lado.normalize().scale(largura / 2);
        double passo = elo > 0 ? elo : comprimento;
        for (double de = 0; de < comprimento; de += passo * (elo > 0 ? 1.45 : 1)) {
            Vec3 a = o.add(d.scale(de)), b = o.add(d.scale(Math.min(comprimento, de + passo)));
            quad(a.subtract(lado), b.subtract(lado), b.add(lado), a.add(lado), cor, alpha);
        }
    }
}
