package com.minerwk.client.vfx;

import com.minerwk.MineRWK;
import com.minerwk.core.personagens.gojo.RoxoMaximo;
import com.minerwk.core.vfx.ImpactFrameGojo;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * O impact frame do uppercut do R: um pós-processamento de tela (shader {@code post/impacto}) que deixa
 * tudo em preto e branco de alto contraste com um toque de roxo por {@link ImpactFrameGojo#TICKS} ticks.
 */
public final class ImpactFrame {
    private static PostChain cadeia;
    private static boolean falhou;
    private static int largura, altura;
    private static long inicio = Long.MIN_VALUE;
    /** Último tick do mundo já olhado (com o jogo pausado o tick do cliente roda e o do mundo não). */
    private static long ultimo = Long.MIN_VALUE;

    private ImpactFrame() {
    }

    /** A cada tick do cliente: vê se algum uppercut do R acabou de acertar perto da câmera. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long agora = mc.level.getGameTime();
        if (agora == ultimo) return;
        ultimo = agora;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (!v.tipo.equals("gojo_roxo_maximo")) continue;
            int a = RoxoMaximo.avanco(v.forca);
            Vec3 soco = new Vec3(v.origem.x(), v.origem.y(), v.origem.z())
                    .add(new Vec3(v.direcao.x(), v.direcao.y(), v.direcao.z()).normalize().scale(v.forca));
            if (ImpactFrameGojo.dispara((long) v.idade(agora, 0), a, cam.distanceTo(soco))) inicio = agora;
        }
    }

    public static void limpar() {
        inicio = Long.MIN_VALUE;
        ultimo = Long.MIN_VALUE;
        if (cadeia != null) {
            cadeia.close();
            cadeia = null;
        }
        falhou = false;
    }

    /** Depois do mundo desenhado, antes da GUI: aplica o preto e branco por cima da tela. */
    public static void aoRenderizar(RenderLevelStageEvent evento) {
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || inicio == Long.MIN_VALUE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        double i = mc.level.getGameTime() - inicio + evento.getPartialTick().getGameTimeDeltaPartialTick(false);
        double forca = ImpactFrameGojo.forca(i);
        if (forca <= 0) {
            if (i >= ImpactFrameGojo.TICKS) inicio = Long.MIN_VALUE;
            return;
        }
        PostChain c = cadeia();
        if (c == null) return;
        c.setUniform("Forca", (float) forca);
        c.setUniform("Inverte", ImpactFrameGojo.invertido(i) ? 1f : 0f);
        c.process(evento.getPartialTick().getGameTimeDeltaTicks());
        mc.getMainRenderTarget().bindWrite(false);
    }

    private static PostChain cadeia() {
        if (falhou) return null;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget alvo = mc.getMainRenderTarget();
        try {
            if (cadeia == null) {
                cadeia = new PostChain(mc.getTextureManager(), mc.getResourceManager(), alvo,
                        MineRWK.id("shaders/post/impacto.json"));
                largura = -1;
            }
            if (largura != alvo.width || altura != alvo.height) {
                largura = alvo.width;
                altura = alvo.height;
                cadeia.resize(largura, altura);
            }
            return cadeia;
        } catch (Exception erro) {
            // Sem o shader o golpe continua; só não tem o impact frame.
            falhou = true;
            MineRWK.LOGGER.warn("Impact frame desligado: o shader não carregou", erro);
            return null;
        }
    }
}
