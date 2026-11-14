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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

/**
 * O céu do mundo interior do Ichigo: azul de dia, com nuvens andando devagar de leste para oeste, por cima do pôr
 * do sol da dimensão das arenas. Quando a máscara aparece (o cenário {@code interior_chuva}), o céu passa a cinza em
 * uns 3 segundos e chove. A chuva é só deste cliente: o bioma das arenas não tem chuva, então ela é desenhada aqui
 * ({@link #desenharChuva}), com os respingos e o som; o clima do servidor não muda.
 */
public final class CeuInterior {
    private static final int[] ZENITE = {58, 123, 213}, HORIZONTE = {207, 232, 255}, BAIXO = {150, 172, 196};
    private static final int[] ZENITE_CHUVA = {78, 88, 98}, HORIZONTE_CHUVA = {138, 148, 156}, BAIXO_CHUVA = {70, 76, 82};
    private static final float R = 100;
    private static final int ANEIS = 16, GOMOS = 48, NUVENS = 30;
    /** O quanto o céu já virou chuva, de 0 a 1; anda 1/60 por tick (3 s). */
    private static float chuva;

    private CeuInterior() {
    }

    /** Se o céu do mundo interior está valendo agora (com ou sem chuva). */
    public static boolean ativa() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != ArenasServidor.DIMENSAO) return false;
        String cenario = CenarioCliente.atual();
        return CenarioArena.INTERIOR.equals(cenario) || CenarioArena.INTERIOR_CHUVA.equals(cenario);
    }

    /** O quanto chove agora, de 0 a 1 (0 fora do mundo interior). */
    public static float chuva() {
        return ativa() ? chuva : 0;
    }

    /** O tick do cliente: a chuva chega e vai aos poucos; com ela, os respingos e o som. */
    public static void aoTick(ClientTickEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (!ativa()) {
            chuva = 0;
            return;
        }
        if (mc.isPaused() || mc.player == null) return;
        float alvo = CenarioArena.INTERIOR_CHUVA.equals(CenarioCliente.atual()) ? 1 : 0;
        chuva += Mth.clamp(alvo - chuva, -1 / 60f, 1 / 60f);
        if (chuva <= .05f) return;
        ClientLevel nivel = mc.level;
        // Respingos no que estiver embaixo da chuva, perto do jogador.
        for (int k = 0; k < (int) (8 * chuva); k++) {
            double x = mc.player.getX() + nivel.random.nextGaussian() * 6, z = mc.player.getZ() + nivel.random.nextGaussian() * 6;
            int topo = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            if (topo > nivel.getMinBuildHeight()) nivel.addParticle(ParticleTypes.RAIN, x, topo + .05, z, 0, 0, 0);
        }
        if (nivel.getGameTime() % 10 == 0) {
            nivel.playLocalSound(mc.player.getX(), mc.player.getY() + 2, mc.player.getZ(), SoundEvents.WEATHER_RAIN, SoundSource.WEATHER,
                    .35f * chuva, .9f + nivel.random.nextFloat() * .2f, false);
        }
    }

    /** A cor da névoa, a do horizonte: a borda do cenário some no céu. */
    public static void aoCorDaNevoa(ViewportEvent.ComputeFogColor evento) {
        if (!ativa() || evento.getCamera().getFluidInCamera() != FogType.NONE) return;
        int[] c = misturar(HORIZONTE, HORIZONTE_CHUVA, chuva);
        evento.setRed(c[0] / 255f);
        evento.setGreen(c[1] / 255f);
        evento.setBlue(c[2] / 255f);
    }

    /** O quanto o horizonte é desenhado escuro: 1 no sol, .55 na chuva. */
    public static float brilhoDoHorizonte() {
        return 1 - .45f * chuva();
    }

    /** Depois do céu do jogo e antes do horizonte (prioridade alta): a cúpula e as nuvens cobrem o pôr do sol. */
    public static void aoRenderizar(RenderLevelStageEvent evento) {
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || !ativa()) return;
        // Neste estágio a matriz do sistema não tem a rotação da câmera: ela vai em cada vértice.
        Matrix4f m = evento.getModelViewMatrix();
        double tempo = Minecraft.getInstance().level.getGameTime() + evento.getPartialTick().getGameTimeDeltaPartialTick(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int[] zenite = misturar(ZENITE, ZENITE_CHUVA, chuva), horizonte = misturar(HORIZONTE, HORIZONTE_CHUVA, chuva),
                baixo = misturar(BAIXO, BAIXO_CHUVA, chuva);
        for (int i = 0; i < ANEIS; i++) {
            double e0 = -Math.PI / 2 + Math.PI * i / ANEIS, e1 = -Math.PI / 2 + Math.PI * (i + 1) / ANEIS;
            int[] c0 = cor(e0, zenite, horizonte, baixo), c1 = cor(e1, zenite, horizonte, baixo);
            for (int k = 0; k < GOMOS; k++) {
                double g0 = Math.PI * 2 * k / GOMOS, g1 = Math.PI * 2 * (k + 1) / GOMOS;
                vertice(b, m, e0, g0, R, c0, 255);
                vertice(b, m, e1, g0, R, c1, 255);
                vertice(b, m, e1, g1, R, c1, 255);
                vertice(b, m, e0, g1, R, c0, 255);
            }
        }
        nuvens(b, m, tempo);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static int[] misturar(int[] a, int[] c, float t) {
        return new int[] {(int) (a[0] + (c[0] - a[0]) * t), (int) (a[1] + (c[1] - a[1]) * t), (int) (a[2] + (c[2] - a[2]) * t)};
    }

    /** O degradê: do horizonte ao zênite em cima; embaixo do horizonte, o tom de baixo (os prédios na névoa). */
    private static int[] cor(double e, int[] zenite, int[] horizonte, int[] baixo) {
        if (e < 0) return misturar(horizonte, baixo, (float) Math.min(1, -e * 3));
        double t = Math.sqrt(Math.sin(e));
        return misturar(horizonte, zenite, (float) t);
    }

    /** Um vértice na direção (elevação {@code e}, giro {@code g}), a {@code raio} da câmera. */
    private static void vertice(BufferBuilder b, Matrix4f m, double e, double g, double raio, int[] c, int alfa) {
        b.addVertex(m, (float) (-Math.sin(g) * Math.cos(e) * raio), (float) (Math.sin(e) * raio), (float) (Math.cos(g) * Math.cos(e) * raio))
                .setColor(c[0], c[1], c[2], alfa);
    }

    /** As nuvens: três manchas achatadas cada, girando devagar de leste para oeste; cinza na chuva. */
    private static void nuvens(BufferBuilder b, Matrix4f m, double tempo) {
        int[] cor = misturar(new int[] {255, 255, 255}, new int[] {92, 98, 106}, chuva);
        int alfa = (int) (215 + 25 * chuva);
        double giro = Math.toRadians(tempo * .02);
        for (int k = 0; k < NUVENS; k++) {
            double e = Math.toRadians(15 + 45 * acaso(k)), g = Math.PI * 2 * acaso(k + 100) + giro;
            double tamanho = .06 + .06 * acaso(k + 200);
            for (int p = 0; p < 3; p++) {
                double de = (p - 1) * tamanho * .35 * acaso(k * 3 + p + 300), dg = (p - 1) * tamanho * 1.4;
                mancha(b, m, e + de, g + dg, tamanho * (p == 1 ? 1 : .7), cor, alfa);
            }
        }
    }

    /** Uma mancha elíptica na cúpula, mais larga que alta, com a borda esmaecida. */
    private static void mancha(BufferBuilder b, Matrix4f m, double e, double g, double tamanho, int[] cor, int alfa) {
        int lados = 12;
        for (int i = 0; i < lados; i++) {
            double a0 = Math.PI * 2 * i / lados, a1 = Math.PI * 2 * (i + 1) / lados;
            vertice(b, m, e, g, R * .99, cor, alfa);
            vertice(b, m, e + Math.sin(a0) * tamanho * .45, g + Math.cos(a0) * tamanho * 1.6, R * .99, cor, alfa / 3);
            vertice(b, m, e + Math.sin(a1) * tamanho * .45, g + Math.cos(a1) * tamanho * 1.6, R * .99, cor, alfa / 3);
            vertice(b, m, e, g, R * .99, cor, alfa);
        }
    }

    /**
     * A chuva, desenhada pelo gancho de chuva da dimensão das arenas ({@link CeuArena}): riscos finos caindo em volta
     * da câmera, só onde o céu está aberto. Posições relativas à câmera.
     */
    public static void desenharChuva(ClientLevel nivel, int ticks, float parcial, double camX, double camY, double camZ) {
        float forca = chuva();
        if (forca <= .02f) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        double tempo = ticks + parcial;
        int cx = Mth.floor(camX), cz = Mth.floor(camZ), alcance = 12;
        int alfa = (int) (120 * forca);
        for (int i = -alcance; i <= alcance; i++) {
            for (int j = -alcance; j <= alcance; j++) {
                int bx = cx + i, bz = cz + j;
                double h = acaso(bx * 31 + bz * 17);
                if (h > .2 + .6 * forca) continue;
                double x = bx + acaso(bx * 7 + bz * 13 + 1), z = bz + acaso(bx * 11 + bz * 5 + 2);
                double y = camY + 10 - ((tempo * .9 + h * 40) % 22);
                if (y < nivel.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz)) continue;
                double rx = x - camX, rz = z - camZ, d = Math.sqrt(rx * rx + rz * rz);
                if (d < .5) continue;
                float lx = (float) (-rz / d * .03), lz = (float) (rx / d * .03);
                float px = (float) rx, py = (float) (y - camY), pz = (float) rz;
                b.addVertex(px - lx, py, pz - lz).setColor(200, 212, 228, 0);
                b.addVertex(px + lx, py, pz + lz).setColor(200, 212, 228, 0);
                b.addVertex(px + lx, py - 1.4f, pz + lz).setColor(200, 212, 228, alfa);
                b.addVertex(px - lx, py - 1.4f, pz - lz).setColor(200, 212, 228, alfa);
            }
        }
        MeshDataOuNada.desenhar(b);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Desenha o que houver no construtor (sem nenhuma gota, {@code build} devolve nada). */
    private static final class MeshDataOuNada {
        static void desenhar(BufferBuilder b) {
            var malha = b.build();
            if (malha != null) BufferUploader.drawWithShader(malha);
        }
    }

    private static double acaso(int n) {
        double x = Math.sin(n * 127.1 + 311.7) * 43758.5453;
        return x - Math.floor(x);
    }
}
