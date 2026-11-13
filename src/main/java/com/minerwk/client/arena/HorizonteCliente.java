package com.minerwk.client.arena;

import com.minerwk.MineRWK;
import com.minerwk.arena.ArenasServidor;
import com.minerwk.client.ConfigCliente;
import com.minerwk.core.arena.AnelHorizonte;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.rede.CenarioArena;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * O horizonte da Escola Jujutsu: três anéis de montanhas, floresta e cidade em volta da câmera, desenhados
 * logo depois do céu. O anel anda com a câmera em x e z (nunca se chega perto dele), mas a altura fica presa
 * ao pátio, para os morros não subirem quando se pula. O terreno é desenhado depois e fica na frente.
 */
public final class HorizonteCliente {
    /** Os raios, do mais longe ao mais perto; cada um usa a sua linha da textura (0 em cima). */
    private static final double[] RAIOS = {300, 220, 160};
    private static final int SEGMENTOS = 16;
    private static final ResourceLocation[] FAIXAS = new ResourceLocation[AnelHorizonte.FAIXAS];
    @SuppressWarnings("unchecked")
    private static final List<AnelHorizonte.Quad>[] ANEIS = new List[RAIOS.length];
    /** A prévia liga isto para gravar o quadro do VFX reduzido sem mexer na configuração do jogador. */
    public static boolean reduzidoNaPrevia;

    static {
        for (int f = 0; f < FAIXAS.length; f++) FAIXAS[f] = MineRWK.id("textures/ceu/horizonte_escola_" + f + ".png");
        for (int i = 0; i < RAIOS.length; i++) {
            ANEIS[i] = AnelHorizonte.anel(RAIOS[i], AnelHorizonte.Y_BASE, AnelHorizonte.Y_TOPO, SEGMENTOS);
        }
    }

    private HorizonteCliente() {
    }

    public static void aoRenderizar(RenderLevelStageEvent evento) {
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != ArenasServidor.DIMENSAO) return;
        if (!CenarioArena.ESCOLA.equals(CenarioCliente.atual())) return;
        Vec3 camera = evento.getCamera().getPosition();
        // A altura do anel é a do mundo: o pátio fica em Y_PATIO.
        float base = (float) (CaixaArena.Y_PATIO + AnelHorizonte.Y_BASE - camera.y);
        float topo = (float) (CaixaArena.Y_PATIO + AnelHorizonte.Y_TOPO - camera.y);
        boolean reduzido = ConfigCliente.vfxReduzido() || reduzidoNaPrevia;
        // Neste estágio a matriz do sistema não tem a rotação da câmera: ela vai em cada vértice.
        Matrix4f m = evento.getModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        for (int camada = reduzido ? RAIOS.length - 1 : 0; camada < RAIOS.length; camada++) {
            float v0 = camada / 3f, v1 = (camada + 1) / 3f;
            for (int f = 0; f < FAIXAS.length; f++) {
                RenderSystem.setShaderTexture(0, FAIXAS[f]);
                BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                for (AnelHorizonte.Quad q : ANEIS[camada]) {
                    if (q.faixa() != f) continue;
                    b.addVertex(m, (float) q.x0(), base, (float) q.z0()).setUv(q.u0(), v1).setColor(255, 255, 255, 255);
                    b.addVertex(m, (float) q.x1(), base, (float) q.z1()).setUv(q.u1(), v1).setColor(255, 255, 255, 255);
                    b.addVertex(m, (float) q.x1(), topo, (float) q.z1()).setUv(q.u1(), v0).setColor(255, 255, 255, 255);
                    b.addVertex(m, (float) q.x0(), topo, (float) q.z0()).setUv(q.u0(), v0).setColor(255, 255, 255, 255);
                }
                BufferUploader.drawWithShader(b.buildOrThrow());
            }
        }
        sobNevoa(m, base);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** A cor do pé da floresta na textura: a saia embaixo do anel continua o chão escuro até sumir de vista. */
    private static final int[] CHAO = {14, 18, 20};
    /** Até onde a saia desce abaixo da base do anel. */
    private static final float SAIA = 200;

    /**
     * Embaixo do anel mais perto: uma saia escura até bem abaixo, para quem olha do alto não ver o vazio por
     * baixo, e uma faixa de névoa na cor do céu na altura do pátio, onde a borda do cenário encontra o anel.
     */
    private static void sobNevoa(Matrix4f m, float base) {
        float[] cor = RenderSystem.getShaderFogColor();
        int r = (int) (cor[0] * 255), g = (int) (cor[1] * 255), bl = (int) (cor[2] * 255);
        float patio = (float) (base - AnelHorizonte.Y_BASE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (AnelHorizonte.Quad q : ANEIS[RAIOS.length - 1]) {
            float x0 = (float) q.x0(), z0 = (float) q.z0(), x1 = (float) q.x1(), z1 = (float) q.z1();
            b.addVertex(m, x0, base - SAIA, z0).setColor(CHAO[0], CHAO[1], CHAO[2], 255);
            b.addVertex(m, x1, base - SAIA, z1).setColor(CHAO[0], CHAO[1], CHAO[2], 255);
            b.addVertex(m, x1, base, z1).setColor(CHAO[0], CHAO[1], CHAO[2], 255);
            b.addVertex(m, x0, base, z0).setColor(CHAO[0], CHAO[1], CHAO[2], 255);
            faixaDeNevoa(b, m, q, patio - 24, patio - 6, r, g, bl, 0, 110);
            faixaDeNevoa(b, m, q, patio - 6, patio + 4, r, g, bl, 110, 0);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static void faixaDeNevoa(BufferBuilder b, Matrix4f m, AnelHorizonte.Quad q, float de, float ate, int r, int g,
                                     int bl, int alfaDe, int alfaAte) {
        b.addVertex(m, (float) q.x0(), de, (float) q.z0()).setColor(r, g, bl, alfaDe);
        b.addVertex(m, (float) q.x1(), de, (float) q.z1()).setColor(r, g, bl, alfaDe);
        b.addVertex(m, (float) q.x1(), ate, (float) q.z1()).setColor(r, g, bl, alfaAte);
        b.addVertex(m, (float) q.x0(), ate, (float) q.z0()).setColor(r, g, bl, alfaAte);
    }
}
