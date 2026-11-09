package com.minerwk.client.vfx;

import com.minerwk.MineRWK;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;

/**
 * Desenha a geometria dos visuais com o shader {@code gojo_energy} (portado do Minezin). Coordenadas
 * relativas à câmera; a rotação da câmera já está no ModelViewMat. Sem o shader (falhou ao carregar),
 * desenha a mesma geometria só com cor, para o jogo nunca fechar por causa do visual.
 */
public final class Pincel {
    /** Modos do shader {@code gojo_energy.fsh}. */
    public static final float AZUL = 0, VERMELHO = 1, ROXO = 2, INFINITO = 3, FITA = 5, CEU = 40, CHAO = 41;

    private static ShaderInstance base;
    private static ShaderInstance aditivo;
    private static boolean avisou;

    private BufferBuilder buffer;
    private boolean comShader;
    private final Matrix4f matriz = new Matrix4f();

    private Pincel() {
    }

    public static void registrarShaders(RegisterShadersEvent evento) {
        // Os dois ou nenhum: sem um deles o desenho todo cai para cor simples.
        base = null;
        aditivo = null;
        try {
            ShaderInstance b = new ShaderInstance(evento.getResourceProvider(), MineRWK.id("gojo_energy"),
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            ShaderInstance a;
            try {
                a = new ShaderInstance(evento.getResourceProvider(), MineRWK.id("gojo_energy_additive"),
                        DefaultVertexFormat.POSITION_TEX_COLOR);
            } catch (IOException | RuntimeException e) {
                b.close();
                throw e;
            }
            evento.registerShader(b, s -> base = s);
            evento.registerShader(a, s -> aditivo = s);
        } catch (IOException | RuntimeException e) {
            MineRWK.LOGGER.warn("Shader gojo_energy não carregou; o visual do Gojo usa cor simples", e);
        }
    }

    /** Um pincel por quadro; {@code tempo} anima o material (segundos de jogo). */
    public static Pincel novo() {
        return new Pincel();
    }

    /**
     * Começa um lote de quadriláteros num modo do shader. {@code aditivo} soma luz (halos, fitas);
     * senão mistura por alfa (corpos). {@code olhar} é a direção da câmera, usada para o brilho de borda.
     */
    public Pincel comecar(float modo, boolean somarLuz, float tempo, Vec3 olhar) {
        ShaderInstance s = somarLuz ? aditivo : base;
        comShader = s != null;
        RenderSystem.enableBlend();
        if (somarLuz) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        if (comShader) {
            RenderSystem.setShader(() -> s);
            s.safeGetUniform("EffectMode").set(modo);
            s.safeGetUniform("EffectTime").set(tempo);
            s.safeGetUniform("Viewer").set((float) olhar.x, (float) olhar.y, (float) olhar.z);
            buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        } else {
            if (!avisou) {
                avisou = true;
                MineRWK.LOGGER.warn("Desenhando o visual do Gojo sem shader");
            }
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }
        return this;
    }

    public void terminar() {
        try {
            MeshData malha = buffer.build();
            if (malha != null) BufferUploader.drawWithShader(malha);
        } finally {
            buffer = null;
            restaurar();
        }
    }

    /** Volta o estado de render ao padrão (também usado se um desenho falhar no meio). */
    static void restaurar() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void vertice(double x, double y, double z, int cor, float alpha, float u, float v) {
        int r = (cor >> 16) & 255;
        int g = (cor >> 8) & 255;
        int b = cor & 255;
        int a = (int) (Math.max(0, Math.min(1, alpha)) * 255);
        var vx = buffer.addVertex(matriz, (float) x, (float) y, (float) z).setColor(r, g, b, a);
        if (comShader) vx.setUv(u, v);
    }

    /** Esfera em volta de {@code c} (relativo à câmera); UV como o shader espera: u = ângulo, v = latitude. */
    public void esfera(Vec3 c, double raio, int cor, float alpha, int segmentos) {
        int n = Math.max(6, segmentos);
        int m = n / 2;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                pontoEsfera(c, raio, i, j, n, m, cor, alpha);
                pontoEsfera(c, raio, i + 1, j, n, m, cor, alpha);
                pontoEsfera(c, raio, i + 1, j + 1, n, m, cor, alpha);
                pontoEsfera(c, raio, i, j + 1, n, m, cor, alpha);
            }
        }
    }

    private void pontoEsfera(Vec3 c, double raio, int i, int j, int n, int m, int cor, float alpha) {
        float u = (float) i / n;
        float v = (float) j / m;
        double a = u * Math.PI * 2;
        double lat = (v - .5) * Math.PI;
        vertice(c.x + Math.cos(a) * Math.cos(lat) * raio, c.y + Math.sin(lat) * raio, c.z + Math.sin(a) * Math.cos(lat) * raio,
                cor, alpha, u, v);
    }

    /**
     * Fita virada para a câmera passando pelos pontos (relativos à câmera). {@code alphas} por ponto
     * (mesmo tamanho de {@code pontos}) permite brilho correndo pela fita.
     */
    public void fita(List<Vec3> pontos, double largura, int cor, float[] alphas) {
        int n = pontos.size();
        if (n < 2) return;
        for (int k = 0; k < n - 1; k++) {
            Vec3 a = pontos.get(k);
            Vec3 b = pontos.get(k + 1);
            Vec3 lado = lado(a, b, largura);
            float ua = (float) k / (n - 1);
            float ub = (float) (k + 1) / (n - 1);
            vertice(a.x - lado.x, a.y - lado.y, a.z - lado.z, cor, alphas[k], ua, 0);
            vertice(b.x - lado.x, b.y - lado.y, b.z - lado.z, cor, alphas[k + 1], ub, 0);
            vertice(b.x + lado.x, b.y + lado.y, b.z + lado.z, cor, alphas[k + 1], ub, 1);
            vertice(a.x + lado.x, a.y + lado.y, a.z + lado.z, cor, alphas[k], ua, 1);
        }
    }

    /** Metade da largura, perpendicular ao segmento e à linha de visão (a câmera está na origem). */
    private static Vec3 lado(Vec3 a, Vec3 b, double largura) {
        Vec3 tangente = b.subtract(a);
        Vec3 meio = a.add(b).scale(.5);
        Vec3 l = tangente.cross(meio);
        if (l.lengthSqr() < 1e-10) l = tangente.cross(new Vec3(0, 1, 0));
        if (l.lengthSqr() < 1e-10) l = new Vec3(1, 0, 0);
        return l.normalize().scale(largura / 2);
    }

    /** Disco horizontal (chão do domínio): u = ângulo, v = 0,5 no centro até 1 na borda. */
    public void disco(Vec3 c, double raio, int cor, float alpha, int segmentos) {
        int n = Math.max(8, segmentos);
        int aneis = 6;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < aneis; j++) {
                pontoDisco(c, raio, i, j, n, aneis, cor, alpha);
                pontoDisco(c, raio, i + 1, j, n, aneis, cor, alpha);
                pontoDisco(c, raio, i + 1, j + 1, n, aneis, cor, alpha);
                pontoDisco(c, raio, i, j + 1, n, aneis, cor, alpha);
            }
        }
    }

    private void pontoDisco(Vec3 c, double raio, int i, int j, int n, int aneis, int cor, float alpha) {
        double a = (double) i / n * Math.PI * 2;
        double r = (double) j / aneis * raio;
        vertice(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, cor, alpha, (float) i / n, .5f + .5f * j / aneis);
    }
}
