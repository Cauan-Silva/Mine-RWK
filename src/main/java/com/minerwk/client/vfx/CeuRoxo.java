package com.minerwk.client.vfx;

import com.minerwk.core.vfx.MugetsuIchigo;
import com.minerwk.core.personagens.gojo.RoxoMaximo;
import com.minerwk.core.vfx.EncantamentoGojo;
import com.minerwk.core.vfx.FusaoRoxoGojo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * O céu escurece durante o R: aos poucos no encantamento, cheio na fusão e na explosão, e volta devagar.
 * É um véu escuro desenhado logo depois do céu e antes do terreno, então só o céu, o sol e as nuvens
 * distantes escurecem; o roxo dos golpes continua claro por cima.
 */
public final class CeuRoxo {
    /** As esferas e a explosão ficam no alto e são vistas de longe: o alcance delas é três vezes maior. */
    private static final double LONGE = 3;
    private static double escuro;
    private static long ultimo = Long.MIN_VALUE;

    private CeuRoxo() {
    }

    /** Quanto o céu está escuro agora, de 0 a 1 (a neblina acompanha). */
    public static double escuro() {
        return escuro;
    }

    public static void limpar() {
        escuro = 0;
        ultimo = Long.MIN_VALUE;
    }

    /** A cada tick do cliente: vê o que pede céu escuro perto da câmera e anda um passo até lá. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long agora = mc.level.getGameTime();
        if (agora == ultimo) return;
        ultimo = agora;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double alvo = 0;
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (v.fimEm >= 0) continue;
            double d = cam.distanceTo(new Vec3(v.origem.x(), v.origem.y(), v.origem.z()));
            double t = v.idade(agora, 0);
            double pede = switch (v.tipo) {
                case "gojo_roxo_200_carga" -> EncantamentoGojo.ceuCarga(t, v.duracao > 0 ? v.duracao : 80)
                        * EncantamentoGojo.perto(d);
                case "gojo_roxo_maximo" -> EncantamentoGojo.CEU_CARGA * EncantamentoGojo.perto(d / LONGE);
                case "gojo_roxo_maximo_esferas" -> (t < v.forca ? .8 : 1) * EncantamentoGojo.perto(d / LONGE);
                case "gojo_roxo_explosao" -> t < v.duracao - RoxoMaximo.TICKS_RESTO ? EncantamentoGojo.perto(d / LONGE) : 0;
                // O Mugetsu do Ichigo: meio escuro desde a transformação, todo escuro no corte.
                case "ichigo_mugetsu_carga" -> MugetsuIchigo.ceuDaCarga(t, MugetsuIchigo.TRANSFORMA) * EncantamentoGojo.perto(d);
                case "ichigo_mugetsu" -> MugetsuIchigo.ceu(t) * EncantamentoGojo.perto(d / LONGE);
                default -> 0;
            };
            alvo = Math.max(alvo, pede);
        }
        escuro = FusaoRoxoGojo.ceu(escuro, alvo);
        if (escuro < .002) escuro = 0;
    }

    public static void aoRenderizar(RenderLevelStageEvent evento) {
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || escuro <= 0) return;
        // Só onde há céu: no Nether e no End o véu pintaria o fundo.
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimensionType().hasSkyLight()) return;
        // Um cubo de cor única em volta da câmera cobre o céu inteiro, qualquer que seja a rotação.
        float l = 60;
        int a = (int) (Math.min(1, escuro) * .88 * 255);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float[][] faces = {
                {-l, -l, -l, l, -l, -l, l, l, -l, -l, l, -l}, {-l, -l, l, l, -l, l, l, l, l, -l, l, l},
                {-l, -l, -l, -l, -l, l, -l, l, l, -l, l, -l}, {l, -l, -l, l, -l, l, l, l, l, l, l, -l},
                {-l, -l, -l, l, -l, -l, l, -l, l, -l, -l, l}, {-l, l, -l, l, l, -l, l, l, l, -l, l, l}};
        for (float[] f : faces) {
            for (int k = 0; k < 12; k += 3) b.addVertex(f[k], f[k + 1], f[k + 2]).setColor(10, 2, 26, a);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        Pincel.restaurar();
    }
}
