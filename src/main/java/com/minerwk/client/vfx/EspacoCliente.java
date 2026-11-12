package com.minerwk.client.vfx;

import com.minerwk.MineRWK;
import com.minerwk.core.personagens.cid.AtomicFinal;
import com.minerwk.kit.EspacoServidor;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

/**
 * O lado do cliente da dimensão do espaço (o R do Cid): o céu dela (preto, estrelas, um planeta enorme embaixo
 * com a borda azul, o sol nascendo atrás dele, uma lua e dois planetas), sem neblina, e os dois clarões que
 * cobrem a tela nas trocas de dimensão: o lampejo da passagem e o branco do fim. Os clarões são contados pelo
 * tempo do jogo, que é o mesmo nas duas dimensões, e por isso atravessam a troca.
 */
public final class EspacoCliente {
    public static final ResourceLocation CAMADA = MineRWK.id("clarao_atomic");
    private static final int BRANCO = 0xF4F0FF, LAMPEJO = 0xCBBEFF;
    /** Tempo do jogo em que cada clarão começou; o menor valor = nunca. */
    private static long atomicEm = Long.MIN_VALUE, passagemEm = Long.MIN_VALUE;
    /** O relógio do computador nos mesmos dois momentos: logo depois da troca o mundo novo ainda não sabe as horas. */
    private static long atomicNoRelogio, passagemNoRelogio;

    private EspacoCliente() {
    }

    public static void registrar(RegisterDimensionSpecialEffectsEvent evento) {
        evento.register(EspacoServidor.DIMENSAO.location(), new Ceu());
    }

    public static boolean noEspaco() {
        ClientLevel nivel = Minecraft.getInstance().level;
        return nivel != null && nivel.dimension() == EspacoServidor.DIMENSAO;
    }

    public static void limpar() {
        atomicEm = Long.MIN_VALUE;
        passagemEm = Long.MIN_VALUE;
    }

    /** Um visual chegou: o Atomic acende o branco do fim; o risco da passagem, só para quem sobe, o lampejo. */
    static void aoVisual(VisuaisAtivos.Visual v) {
        Minecraft mc = Minecraft.getInstance();
        boolean meu = mc.player != null && mc.player.getId() == v.dono;
        boolean noEspaco = mc.level != null && mc.level.dimension().location().toString().equals("minerwk:espaco");
        // O branco é de quem está no golpe: quem só assiste de fora (na arena, por exemplo) vê o clarão no mundo.
        if (v.tipo.equals("cid_atomic") && (meu || noEspaco)) {
            atomicEm = v.inicio;
            atomicNoRelogio = System.currentTimeMillis();
        } else if (v.tipo.equals("cid_atomic_risco") && meu) {
            passagemEm = v.inicio;
            passagemNoRelogio = System.currentTimeMillis();
        }
    }

    /** Quanto o clarão do Atomic ilumina o céu do espaço agora, de 0 a 1. */
    static double forcaDoAtomic() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || atomicEm == Long.MIN_VALUE) return 0;
        double t = mc.level.getGameTime() - atomicEm + mc.getTimer().getGameTimeDeltaPartialTick(false);
        return AtomicFinal.clarao(t) * Math.min(1, .25 + AtomicFinal.raioDoClarao(t) * 2);
    }

    private static double branco(float parcial) {
        ClientLevel nivel = Minecraft.getInstance().level;
        return nivel == null || atomicEm == Long.MIN_VALUE ? 0 : AtomicFinal.branco(nivel.getGameTime() - atomicEm + parcial);
    }

    private static double lampejo(float parcial) {
        ClientLevel nivel = Minecraft.getInstance().level;
        return nivel == null || passagemEm == Long.MIN_VALUE ? 0 : AtomicFinal.lampejo(nivel.getGameTime() - passagemEm + parcial);
    }

    /** O clarão cobre a tela inteira, por baixo das barras da cutscene. */
    public static void desenhar(GuiGraphics g, DeltaTracker tempo) {
        float parcial = tempo.getGameTimeDeltaPartialTick(false);
        double b = branco(parcial), l = lampejo(parcial);
        if (l > .004) g.fill(0, 0, g.guiWidth(), g.guiHeight(), (int) (Math.min(1, l) * 255) << 24 | LAMPEJO);
        if (b > .004) g.fill(0, 0, g.guiWidth(), g.guiHeight(), (int) (Math.min(1, b) * 255) << 24 | BRANCO);
    }

    /** A tela "carregando terreno" não aparece na ida nem na volta: um dos clarões já cobre a troca. */
    public static void aoAbrirTela(ScreenEvent.Opening evento) {
        if (!(evento.getNewScreen() instanceof ReceivingLevelScreen)) return;
        long agora = System.currentTimeMillis();
        boolean ida = agora - passagemNoRelogio < 2000;
        boolean volta = agora - atomicNoRelogio < AtomicFinal.FIM_DO_BRANCO * 50L + 1500;
        if (ida || volta) evento.setCanceled(true);
    }

    /** No espaço não há neblina: a câmera se afasta cem blocos e os dois ainda aparecem. */
    public static void aoNeblina(ViewportEvent.RenderFog evento) {
        if (!noEspaco()) return;
        evento.setNearPlaneDistance(1e5f);
        evento.setFarPlaneDistance(2e5f);
        evento.setCanceled(true);
    }

    // ---------------------------------------------------------------- o céu

    /** O céu do espaço. Tudo é desenhado numa casca a {@link #R} blocos da câmera, que gira com o mundo. */
    private static final class Ceu extends DimensionSpecialEffects {
        private static final float R = 100;
        /** Até onde o planeta sobe, medido do ponto bem embaixo: 62 graus (a borda fica 28 graus abaixo do horizonte). */
        private static final double BORDA = Math.toRadians(62);
        /** De que lado o sol nasce (um ângulo em volta do eixo vertical). */
        private static final double SOL = Math.toRadians(35);
        private static final int ESTRELAS = 900;

        private BufferBuilder b;
        private Matrix4f m;

        Ceu() {
            super(Float.NaN, false, SkyType.NONE, true, true);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 cor, float brilho) {
            return Vec3.ZERO;
        }

        @Override
        public boolean isFoggyAt(int x, int z) {
            return false;
        }

        @Override
        public boolean renderSky(ClientLevel nivel, int ticks, float parcial, Matrix4f modelo, Camera camera, Matrix4f projecao,
                                 boolean neblina, Runnable prepararNeblina) {
            m = modelo;
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            try {
                RenderSystem.defaultBlendFunc();
                abrir();
                fundo();
                estrelas(ticks + parcial);
                astros();
                planeta();
                fechar();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                abrir();
                luzes();
                fechar();
            } finally {
                if (b != null) {
                    MeshData resto = b.build();
                    if (resto != null) resto.close();
                    b = null;
                }
                Pincel.restaurar();
                RenderSystem.enableDepthTest();
            }
            return true;
        }

        private void abrir() {
            b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }

        private void fechar() {
            MeshData malha = b.build();
            b = null;
            if (malha != null) BufferUploader.drawWithShader(malha);
        }

        private void ponto(Vec3 p, int cor, double alpha) {
            b.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
                    .setColor((cor >> 16) & 255, (cor >> 8) & 255, cor & 255, (int) (Math.max(0, Math.min(1, alpha)) * 255));
        }

        private static double acaso(double n) {
            double x = Math.sin(n * 127.1 + 311.7) * 43758.5453;
            return x - Math.floor(x);
        }

        private static int misturar(int a, int c, double q) {
            q = Math.max(0, Math.min(1, q));
            int r = (int) (((a >> 16) & 255) * (1 - q) + ((c >> 16) & 255) * q);
            int g = (int) (((a >> 8) & 255) * (1 - q) + ((c >> 8) & 255) * q);
            int az = (int) ((a & 255) * (1 - q) + (c & 255) * q);
            return r << 16 | g << 8 | az;
        }

        /** A direção com {@code elevacao} acima do horizonte e {@code giro} em volta do eixo vertical. */
        private static Vec3 direcao(double elevacao, double giro) {
            double h = Math.cos(elevacao);
            return new Vec3(h * Math.cos(giro), Math.sin(elevacao), h * Math.sin(giro));
        }

        /** Disco virado para a câmera, no ponto {@code c} da casca: cheio no centro, {@code borda} na beirada. */
        private void disco(Vec3 c, double raio, int cor, double centro, double borda) {
            Vec3 ver = c.normalize();
            Vec3 x = ver.cross(new Vec3(0, 1, 0));
            x = (x.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : x.normalize()).scale(raio);
            Vec3 y = x.cross(ver);
            int n = 24;
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                ponto(c, cor, centro);
                ponto(c.add(x.scale(Math.cos(a0))).add(y.scale(Math.sin(a0))), cor, borda);
                ponto(c.add(x.scale(Math.cos(a1))).add(y.scale(Math.sin(a1))), cor, borda);
                ponto(c, cor, centro);
            }
        }

        /** Um cubo preto em volta da câmera. */
        private void fundo() {
            float l = R * 1.5f;
            float[][] faces = {
                    {-l, -l, -l, l, -l, -l, l, l, -l, -l, l, -l}, {-l, -l, l, l, -l, l, l, l, l, -l, l, l},
                    {-l, -l, -l, -l, -l, l, -l, l, l, -l, l, -l}, {l, -l, -l, l, -l, l, l, l, l, l, l, -l},
                    {-l, -l, -l, l, -l, -l, l, -l, l, -l, -l, l}, {-l, l, -l, l, l, -l, l, l, l, -l, l, l}};
            for (float[] f : faces) {
                for (int k = 0; k < 12; k += 3) ponto(new Vec3(f[k], f[k + 1], f[k + 2]), 0x020108, 1);
            }
        }

        /** Estrelas espalhadas pela casca toda (as de trás do planeta ele cobre); algumas piscam devagar. */
        private void estrelas(double t) {
            for (int k = 0; k < ESTRELAS; k++) {
                double y = 2 * acaso(k) - 1, giro = Math.PI * 2 * acaso(k + 1000), h = Math.sqrt(1 - y * y);
                Vec3 c = new Vec3(h * Math.cos(giro), y, h * Math.sin(giro)).scale(R);
                double tamanho = .1 + .32 * Math.pow(acaso(k + 2000), 3);
                double brilho = .35 + .65 * acaso(k + 3000);
                if (k % 7 == 0) brilho *= .7 + .3 * Math.sin(t * .08 + k);
                int cor = k % 5 == 0 ? 0xBFD0FF : k % 11 == 0 ? 0xFFE2C4 : 0xFFFFFF;
                Vec3 ver = c.normalize();
                Vec3 x = ver.cross(new Vec3(0, 1, 0));
                x = (x.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : x.normalize()).scale(tamanho);
                Vec3 yy = x.cross(ver);
                ponto(c.subtract(x).subtract(yy), cor, brilho);
                ponto(c.add(x).subtract(yy), cor, brilho);
                ponto(c.add(x).add(yy), cor, brilho);
                ponto(c.subtract(x).add(yy), cor, brilho);
            }
        }

        /** A lua, cinza e com a face meio na sombra, e dois planetas pequenos, um deles com anel. */
        private void astros() {
            Vec3 lua = direcao(Math.toRadians(34), SOL + Math.toRadians(150)).scale(R);
            disco(lua, 5.2, 0xC9C9D2, 1, 1);
            Vec3 ver = lua.normalize(), lado = ver.cross(new Vec3(0, 1, 0)).normalize(), cima = lado.cross(ver);
            disco(lua.add(lado.scale(1.4)).add(cima.scale(.9)), .9, 0x9A9AA6, 1, 1);
            disco(lua.add(lado.scale(-1.6)).add(cima.scale(-1.2)), 1.2, 0xA3A3AE, 1, 1);
            disco(lua.add(lado.scale(.3)).add(cima.scale(-2.3)), .7, 0x9A9AA6, 1, 1);
            // A sombra: um disco escuro deslocado para o lado contrário ao sol.
            disco(lua.add(lado.scale(-2.6)).scale(.999), 5.0, 0x020108, .8, .55);
            Vec3 laranja = direcao(Math.toRadians(52), SOL + Math.toRadians(-70)).scale(R);
            disco(laranja, 1.7, 0xD9925A, 1, 1);
            Vec3 v2 = laranja.normalize(), l2 = v2.cross(new Vec3(0, 1, 0)).normalize(), c2 = l2.cross(v2);
            Vec3 eixo = l2.scale(3.4).add(c2.scale(.9)), fino = c2.scale(.16).subtract(l2.scale(.04));
            ponto(laranja.subtract(eixo).subtract(fino), 0xE8C9A0, .85);
            ponto(laranja.add(eixo).subtract(fino), 0xE8C9A0, .85);
            ponto(laranja.add(eixo).add(fino), 0xE8C9A0, .85);
            ponto(laranja.subtract(eixo).add(fino), 0xE8C9A0, .85);
            disco(direcao(Math.toRadians(18), SOL + Math.toRadians(95)).scale(R), .9, 0xC8635A, 1, 1);
        }

        /** A cor do planeta num ponto: mar azul-escuro, manchas de terra e de nuvem, mais claro para a borda e para o sol. */
        private static int corDoPlaneta(double a, double giro) {
            double u = a / BORDA;
            double terra = Math.sin(giro * 3 + 1.3 * Math.sin(u * 7 + giro)) * Math.cos(u * 5.5 + .8 * Math.sin(giro * 2));
            double nuvem = Math.sin(giro * 5 + u * 9) * Math.sin(u * 13 - giro * 2 + 1.7);
            int cor = misturar(0x0A1C46, 0x1E55B8, u * u);
            if (terra > .35) cor = misturar(cor, 0x2F5A3A, Math.min(1, (terra - .35) * 3) * .8);
            if (nuvem > .45) cor = misturar(cor, 0xDCE8FF, Math.min(1, (nuvem - .45) * 3) * .6);
            // O lado do sol é dia; o outro, noite.
            double dia = .5 + .5 * Math.cos(giro - SOL);
            double luz = .22 + .78 * dia * (.35 + .65 * u);
            return misturar(0x01030A, cor, luz);
        }

        /** O planeta: uma calota da casca, do ponto bem embaixo até a borda. */
        private void planeta() {
            int aneis = 14, gomos = 72;
            for (int i = 0; i < aneis; i++) {
                double a0 = BORDA * i / aneis, a1 = BORDA * (i + 1) / aneis;
                for (int k = 0; k < gomos; k++) {
                    double g0 = Math.PI * 2 * k / gomos, g1 = Math.PI * 2 * (k + 1) / gomos;
                    ponto(direcao(a0 - Math.PI / 2, g0).scale(R * .99), corDoPlaneta(a0, g0), 1);
                    ponto(direcao(a1 - Math.PI / 2, g0).scale(R * .99), corDoPlaneta(a1, g0), 1);
                    ponto(direcao(a1 - Math.PI / 2, g1).scale(R * .99), corDoPlaneta(a1, g1), 1);
                    ponto(direcao(a0 - Math.PI / 2, g1).scale(R * .99), corDoPlaneta(a0, g1), 1);
                }
            }
        }

        /** As luzes, que somam: a atmosfera azul na borda do planeta e o sol nascendo atrás dela. */
        private void luzes() {
            int gomos = 72;
            double borda = BORDA - Math.PI / 2;
            // O Atomic acende o planeta: um banho roxo sobre a calota inteira, do tamanho do clarão.
            double banho = forcaDoAtomic();
            if (banho > .004) {
                for (int i = 0; i < 6; i++) {
                    double a0 = BORDA * i / 6 - Math.PI / 2, a1 = BORDA * (i + 1) / 6 - Math.PI / 2;
                    for (int k = 0; k < gomos; k++) {
                        double g0 = Math.PI * 2 * k / gomos, g1 = Math.PI * 2 * (k + 1) / gomos;
                        ponto(direcao(a0, g0).scale(R * .985), 0x9A7CFF, .5 * banho);
                        ponto(direcao(a1, g0).scale(R * .985), 0x9A7CFF, .5 * banho);
                        ponto(direcao(a1, g1).scale(R * .985), 0x9A7CFF, .5 * banho);
                        ponto(direcao(a0, g1).scale(R * .985), 0x9A7CFF, .5 * banho);
                    }
                }
            }
            for (int k = 0; k < gomos; k++) {
                double g0 = Math.PI * 2 * k / gomos, g1 = Math.PI * 2 * (k + 1) / gomos;
                double d0 = .3 + .7 * Math.pow(.5 + .5 * Math.cos(g0 - SOL), 2), d1 = .3 + .7 * Math.pow(.5 + .5 * Math.cos(g1 - SOL), 2);
                // Para fora: o azul que se desfaz no preto.
                ponto(direcao(borda, g0).scale(R * .98), 0x4FA3FF, .9 * d0);
                ponto(direcao(borda + Math.toRadians(5), g0).scale(R * .98), 0x2A5BD8, 0);
                ponto(direcao(borda + Math.toRadians(5), g1).scale(R * .98), 0x2A5BD8, 0);
                ponto(direcao(borda, g1).scale(R * .98), 0x4FA3FF, .9 * d1);
                // Para dentro: a névoa azul sobre a beirada do planeta.
                ponto(direcao(borda, g0).scale(R * .98), 0x7FC2FF, .7 * d0);
                ponto(direcao(borda - Math.toRadians(9), g0).scale(R * .98), 0x2A6FE0, 0);
                ponto(direcao(borda - Math.toRadians(9), g1).scale(R * .98), 0x2A6FE0, 0);
                ponto(direcao(borda, g1).scale(R * .98), 0x7FC2FF, .7 * d1);
            }
            Vec3 sol = direcao(borda + Math.toRadians(2.5), SOL).scale(R * .97);
            disco(sol, 34, 0x6FA8FF, .35, 0);
            disco(sol, 14, 0xCFE2FF, .7, 0);
            disco(sol, 4.5, 0xFFFFFF, 1, 0);
        }
    }
}
