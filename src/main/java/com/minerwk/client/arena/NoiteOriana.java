package com.minerwk.client.arena;

import com.minerwk.arena.ArenasServidor;
import com.minerwk.rede.CenarioArena;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

/**
 * A noite de Oriana sob ataque. A dimensão das arenas tem uma hora só, a do pôr do sol; em Oriana o cliente cobre
 * esse céu com uma cúpula noturna avermelhada, com algumas estrelas e o brilho laranja do incêndio baixo no norte,
 * onde fica a capital, e pinta a névoa no mesmo tom. Os blocos continuam com a luz do pôr do sol.
 */
public final class NoiteOriana {
    /** O zênite, o horizonte e o chão da cúpula. */
    private static final int[] ZENITE = {11, 7, 18}, HORIZONTE = {58, 20, 16}, CHAO = {10, 6, 8};
    /** O brilho do incêndio, somado perto do horizonte norte. */
    private static final int[] INCENDIO = {150, 60, 20};
    private static final float R = 100;
    private static final int ANEIS = 16, GOMOS = 48, ESTRELAS = 300;

    private NoiteOriana() {
    }

    /** Se o céu de Oriana está valendo agora. */
    public static boolean ativa() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension() == ArenasServidor.DIMENSAO && CenarioArena.ORIANA.equals(CenarioCliente.atual());
    }

    /** A cor da névoa, a do horizonte: a borda do cenário some na noite. */
    public static void aoCorDaNevoa(ViewportEvent.ComputeFogColor evento) {
        if (!ativa()) return;
        evento.setRed(HORIZONTE[0] / 255f);
        evento.setGreen(HORIZONTE[1] / 255f);
        evento.setBlue(HORIZONTE[2] / 255f);
    }

    /** Depois do céu do jogo e antes do horizonte (registrado antes dele): a cúpula cobre o pôr do sol. */
    public static void aoRenderizar(RenderLevelStageEvent evento) {
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || !ativa()) return;
        // Neste estágio a matriz do sistema não tem a rotação da câmera: ela vai em cada vértice.
        Matrix4f m = evento.getModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < ANEIS; i++) {
            double e0 = -Math.PI / 2 + Math.PI * i / ANEIS, e1 = -Math.PI / 2 + Math.PI * (i + 1) / ANEIS;
            for (int k = 0; k < GOMOS; k++) {
                double g0 = Math.PI * 2 * k / GOMOS, g1 = Math.PI * 2 * (k + 1) / GOMOS;
                ponto(b, m, e0, g0);
                ponto(b, m, e1, g0);
                ponto(b, m, e1, g1);
                ponto(b, m, e0, g1);
            }
        }
        estrelas(b, m);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Um vértice da cúpula na elevação {@code e} e no giro {@code g} (0 é o sul, π é o norte). */
    private static void ponto(BufferBuilder b, Matrix4f m, double e, double g) {
        float x = (float) (-Math.sin(g) * Math.cos(e) * R), y = (float) (Math.sin(e) * R), z = (float) (Math.cos(g) * Math.cos(e) * R);
        int[] c = cor(e, g);
        b.addVertex(m, x, y, z).setColor(c[0], c[1], c[2], 255);
    }

    /** O degradê do zênite ao horizonte, escuro embaixo dele, com o incêndio somado baixo e ao norte. */
    static int[] cor(double e, double g) {
        int[] c = new int[3];
        double alto = Math.max(0, Math.sin(e));
        double norte = Math.max(0, -Math.cos(g));
        double incendio = Math.pow(norte, 3) * Math.exp(-alto * 7) * (e < -.05 ? 0 : 1);
        for (int i = 0; i < 3; i++) {
            double base = e < 0 ? CHAO[i] + (HORIZONTE[i] - CHAO[i]) * Math.max(0, 1 + e * 6)
                    : HORIZONTE[i] + (ZENITE[i] - HORIZONTE[i]) * Math.pow(alto, .55);
            c[i] = (int) Math.min(255, base + INCENDIO[i] * incendio);
        }
        return c;
    }

    /** Estrelas tênues, sempre nas mesmas direções, só no alto (a fumaça e o brilho apagam as baixas). */
    private static void estrelas(BufferBuilder b, Matrix4f m) {
        for (int k = 0; k < ESTRELAS; k++) {
            double e = Math.asin(.3 + .7 * acaso(k)), g = Math.PI * 2 * acaso(k + 500);
            float x = (float) (-Math.sin(g) * Math.cos(e) * R * .98), y = (float) (Math.sin(e) * R * .98), z = (float) (Math.cos(g) * Math.cos(e) * R * .98);
            float t = (float) (.12 + .2 * acaso(k + 1000));
            // Um quadradinho virado para o centro: dois eixos perpendiculares à direção.
            float hx = (float) Math.cos(g) * t, hz = (float) Math.sin(g) * t;
            float vx = (float) (Math.sin(g) * Math.sin(e)) * t, vy = (float) Math.cos(e) * t, vz = (float) (-Math.cos(g) * Math.sin(e)) * t;
            int a = (int) (90 + 120 * acaso(k + 2000));
            b.addVertex(m, x - hx - vx, y - vy, z - hz - vz).setColor(255, 236, 220, a);
            b.addVertex(m, x + hx - vx, y - vy, z + hz - vz).setColor(255, 236, 220, a);
            b.addVertex(m, x + hx + vx, y + vy, z + hz + vz).setColor(255, 236, 220, a);
            b.addVertex(m, x - hx + vx, y + vy, z - hz + vz).setColor(255, 236, 220, a);
        }
    }

    private static double acaso(int n) {
        double x = Math.sin(n * 127.1 + 311.7) * 43758.5453;
        return x - Math.floor(x);
    }
}
