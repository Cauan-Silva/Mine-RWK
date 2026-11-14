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
    /** As faixas de cada cenário que tem horizonte; os outros não desenham nada. */
    private static final java.util.Map<String, ResourceLocation[]> FAIXAS = new java.util.HashMap<>();
    /** A cor do pé do morro perto na textura de cada cenário: a saia embaixo do anel continua o chão escuro. */
    private static final java.util.Map<String, int[]> CHAO = java.util.Map.of(CenarioArena.ESCOLA, new int[] {14, 18, 20},
            CenarioArena.ORIANA, new int[] {10, 8, 12}, CenarioArena.INTERIOR, new int[] {150, 168, 188},
            CenarioArena.INTERIOR_CHUVA, new int[] {70, 78, 88});
    @SuppressWarnings("unchecked")
    private static final List<AnelHorizonte.Quad>[] ANEIS = new List[RAIOS.length];
    /** A prévia liga isto para gravar o quadro do VFX reduzido sem mexer na configuração do jogador. */
    public static boolean reduzidoNaPrevia;

    static {
        for (String cenario : new String[] {CenarioArena.ESCOLA, CenarioArena.ORIANA}) {
            ResourceLocation[] faixas = new ResourceLocation[AnelHorizonte.FAIXAS];
            for (int f = 0; f < faixas.length; f++) faixas[f] = MineRWK.id("textures/ceu/horizonte_" + cenario + "_" + f + ".png");
            FAIXAS.put(cenario, faixas);
        }
        // O mundo interior usa as mesmas faixas com e sem chuva; na chuva o anel é desenhado mais escuro.
        ResourceLocation[] interior = new ResourceLocation[AnelHorizonte.FAIXAS];
        for (int f = 0; f < interior.length; f++) interior[f] = MineRWK.id("textures/ceu/horizonte_interior_" + f + ".png");
        FAIXAS.put(CenarioArena.INTERIOR, interior);
        FAIXAS.put(CenarioArena.INTERIOR_CHUVA, interior);
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
        String cenario = CenarioCliente.atual();
        ResourceLocation[] faixas = FAIXAS.get(cenario);
        if (faixas == null) return;
        Vec3 camera = evento.getCamera().getPosition();
        // A altura do anel é a do mundo: o pátio fica em Y_PATIO.
        float base = (float) (CaixaArena.Y_PATIO + AnelHorizonte.Y_BASE - camera.y);
        float topo = (float) (CaixaArena.Y_PATIO + AnelHorizonte.Y_TOPO - camera.y);
        boolean reduzido = ConfigCliente.vfxReduzido() || reduzidoNaPrevia;
        int brilho = (int) (255 * CeuInterior.brilhoDoHorizonte());
        // Neste estágio a matriz do sistema não tem a rotação da câmera: ela vai em cada vértice.
        Matrix4f m = evento.getModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        for (int camada = reduzido ? RAIOS.length - 1 : 0; camada < RAIOS.length; camada++) {
            float v0 = camada / 3f, v1 = (camada + 1) / 3f;
            for (int f = 0; f < faixas.length; f++) {
                RenderSystem.setShaderTexture(0, faixas[f]);
                BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                for (AnelHorizonte.Quad q : ANEIS[camada]) {
                    if (q.faixa() != f) continue;
                    b.addVertex(m, (float) q.x0(), base, (float) q.z0()).setUv(q.u0(), v1).setColor(brilho, brilho, brilho, 255);
                    b.addVertex(m, (float) q.x1(), base, (float) q.z1()).setUv(q.u1(), v1).setColor(brilho, brilho, brilho, 255);
                    b.addVertex(m, (float) q.x1(), topo, (float) q.z1()).setUv(q.u1(), v0).setColor(brilho, brilho, brilho, 255);
                    b.addVertex(m, (float) q.x0(), topo, (float) q.z0()).setUv(q.u0(), v0).setColor(brilho, brilho, brilho, 255);
                }
                BufferUploader.drawWithShader(b.buildOrThrow());
            }
        }
        sobNevoa(m, base, CHAO.get(cenario));
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Até onde a saia desce abaixo da base do anel. */
    private static final float SAIA = 200;

    /**
     * Embaixo do anel mais perto: uma saia escura até bem abaixo, para quem olha do alto não ver o vazio por
     * baixo, e uma faixa de névoa na cor do céu na altura do pátio, onde a borda do cenário encontra o anel.
     */
    private static void sobNevoa(Matrix4f m, float base, int[] chao) {
        float[] cor = RenderSystem.getShaderFogColor();
        int r = (int) (cor[0] * 255), g = (int) (cor[1] * 255), bl = (int) (cor[2] * 255);
        float patio = (float) (base - AnelHorizonte.Y_BASE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (AnelHorizonte.Quad q : ANEIS[RAIOS.length - 1]) {
            float x0 = (float) q.x0(), z0 = (float) q.z0(), x1 = (float) q.x1(), z1 = (float) q.z1();
            b.addVertex(m, x0, base - SAIA, z0).setColor(chao[0], chao[1], chao[2], 255);
            b.addVertex(m, x1, base - SAIA, z1).setColor(chao[0], chao[1], chao[2], 255);
            b.addVertex(m, x1, base, z1).setColor(chao[0], chao[1], chao[2], 255);
            b.addVertex(m, x0, base, z0).setColor(chao[0], chao[1], chao[2], 255);
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
