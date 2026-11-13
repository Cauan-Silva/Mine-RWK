package com.minerwk.client.vfx;

import com.minerwk.MineRWK;
import com.minerwk.client.camera.CutsceneCliente;
import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.cid.AtomicFinal;
import com.minerwk.core.personagens.cid.CutsceneAtomic;
import com.minerwk.core.personagens.cid.SistemaSolar;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    /** Ticks desde o Atomic, ou {@code NaN} se não houve. */
    static double tempoDoAtomic(float parcial) {
        ClientLevel nivel = Minecraft.getInstance().level;
        return nivel == null || atomicEm == Long.MIN_VALUE ? Double.NaN : nivel.getGameTime() - atomicEm + parcial;
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
        /** Multiplica a opacidade de tudo o que se desenha (os astros de sempre somem no recuo). */
        private double alfaGeral = 1;

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
                // Quem vê a cutscene do estouro vê a câmera recuar até o sistema solar; os outros, o céu de sempre.
                double t = tempoDoAtomic(parcial);
                Vetor rumo = CutsceneCliente.emCurso(CutsceneAtomic.CLARAO) ? CutsceneCliente.direcao() : null;
                boolean recuo = rumo != null && t >= 0;
                abrir();
                fundo();
                estrelas(ticks + parcial);
                if (recuo) {
                    alfaGeral = SistemaSolar.astrosAntigos(t);
                    if (alfaGeral > .004) astros();
                    alfaGeral = 1;
                    sistema(t, rumo);
                } else {
                    astros();
                    planeta();
                }
                fechar();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                abrir();
                if (recuo) {
                    luzesDoRecuo(t, rumo);
                } else {
                    luzes();
                }
                fechar();
            } finally {
                alfaGeral = 1;
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
                    .setColor((cor >> 16) & 255, (cor >> 8) & 255, cor & 255, (int) (Math.max(0, Math.min(1, alpha * alfaGeral)) * 255));
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

        // ------------------------------------------------------------ o recuo até o sistema solar

        /** Uma direção da cena (na base da cutscene) no céu, na casca. */
        private static Vec3 noCeu(Vetor base, Vetor rumo, double raio) {
            Vetor v = Coreografia.naBase(base, Vetor.ZERO, rumo).normalizado();
            return new Vec3(v.x(), v.y(), v.z()).scale(raio);
        }

        /** O raio, na casca, de um meio ângulo (o pulso final chega a cobrir a tela). */
        private static double naCasca(double meioAngulo) {
            return R * Math.tan(Math.min(meioAngulo, Math.toRadians(85)));
        }

        /** Dois eixos perpendiculares a {@code e}; com {@code e} para baixo, são os eixos x e z do mundo, como os do planeta de sempre. */
        private static Vec3[] eixos(Vec3 e) {
            Vec3 u = new Vec3(1, 0, 0).subtract(e.scale(e.x));
            if (u.lengthSqr() < 1e-6) u = new Vec3(0, 0, 1).subtract(e.scale(e.z));
            u = u.normalize();
            return new Vec3[] {u, e.cross(u).normalize()};
        }

        /** O ponto da casca a {@code a} radianos de {@code e}, no giro {@code g}. */
        private static Vec3 naCalota(Vec3 e, Vec3[] uv, double a, double g, double raio) {
            return e.scale(Math.cos(a)).add(uv[0].scale(Math.sin(a) * Math.cos(g))).add(uv[1].scale(Math.sin(a) * Math.sin(g))).scale(raio);
        }

        /** Os astros, do mais longe ao mais perto (a Terra, enorme no começo, cobre o que está atrás dela), e as órbitas. */
        private void sistema(double t, Vetor rumo) {
            double novos = 1 - SistemaSolar.astrosAntigos(t);
            orbitas(t, rumo, novos);
            Vetor camera = SistemaSolar.camera(t);
            List<SistemaSolar.Astro> ordem = new ArrayList<>(SistemaSolar.ASTROS);
            ordem.sort(Comparator.comparingDouble((SistemaSolar.Astro a) -> -SistemaSolar.posicao(a, t).distancia(camera)));
            for (SistemaSolar.Astro a : ordem) {
                if (a == SistemaSolar.TERRA) {
                    terra(t, rumo);
                } else if (novos > .004) {
                    astro(a, t, rumo, novos);
                }
            }
        }

        /** As órbitas: anéis finos e apagados, que aparecem quando a Terra já é uma bola. */
        private void orbitas(double t, Vetor rumo, double novos) {
            double u = Math.max(0, Math.min(1, (t - SistemaSolar.TERRA_INTEIRA) / 30.0));
            double alfa = .28 * novos * u * u * (3 - 2 * u);
            if (alfa < .004) return;
            Vetor camera = SistemaSolar.camera(t), sol = SistemaSolar.posicao(SistemaSolar.SOL, t);
            int n = 120;
            for (SistemaSolar.Astro a : SistemaSolar.ASTROS) {
                if (a == SistemaSolar.SOL) continue;
                Vec3 antes = null;
                for (int k = 0; k <= n; k++) {
                    double g = Math.PI * 2 * k / n;
                    Vetor p = sol.mais(new Vetor(Math.cos(g), 0, Math.sin(g)).vezes(a.orbita()));
                    if (p.distancia(camera) < 3) {
                        antes = null;
                        continue;
                    }
                    Vec3 agora = noCeu(SistemaSolar.direcao(p, t), rumo, R * .995);
                    if (antes != null) {
                        Vec3 largura = agora.subtract(antes).cross(agora).normalize().scale(.09);
                        ponto(antes.subtract(largura), 0x8FA6D8, alfa);
                        ponto(agora.subtract(largura), 0x8FA6D8, alfa);
                        ponto(agora.add(largura), 0x8FA6D8, alfa);
                        ponto(antes.add(largura), 0x8FA6D8, alfa);
                    }
                    antes = agora;
                }
            }
        }

        /** Um astro: o disco com a face de trás na sombra (longe do Sol); o Sol com o halo; Saturno com o anel. */
        private void astro(SistemaSolar.Astro a, double t, Vetor rumo, double alfa) {
            Vec3 c = noCeu(SistemaSolar.direcao(a, t), rumo, R * .997);
            double raio = naCasca(SistemaSolar.raioAparente(a, t));
            if (a == SistemaSolar.SOL) {
                disco(c, raio * 4, 0xFFC56A, .25 * alfa, 0);
                disco(c, raio * 1.8, 0xFFE2A0, .6 * alfa, 0);
                disco(c, raio, a.cor(), alfa, alfa);
                return;
            }
            if (a.anel()) anel(c, raio, alfa);
            disco(c, raio, a.cor(), alfa, alfa);
            Vec3 paraOSol = noCeu(SistemaSolar.direcao(SistemaSolar.SOL, t), rumo, R).subtract(c);
            Vec3 ver = c.normalize();
            Vec3 lado = paraOSol.subtract(ver.scale(paraOSol.dot(ver)));
            if (lado.lengthSqr() > 1e-9) {
                disco(c.subtract(lado.normalize().scale(raio * .55)).scale(.999), raio * .9, 0x020108, .75 * alfa, .4 * alfa);
            }
        }

        /** O anel de Saturno: uma faixa fina e inclinada atravessando o disco. */
        private void anel(Vec3 c, double raio, double alfa) {
            Vec3 v = c.normalize(), l = v.cross(new Vec3(0, 1, 0)).normalize(), cc = l.cross(v);
            Vec3 eixo = l.scale(raio * 2.1).add(cc.scale(raio * .5)), fino = cc.scale(raio * .14).subtract(l.scale(raio * .03));
            ponto(c.subtract(eixo).subtract(fino), 0xE8D7B0, .85 * alfa);
            ponto(c.add(eixo).subtract(fino), 0xE8D7B0, .85 * alfa);
            ponto(c.add(eixo).add(fino), 0xE8D7B0, .85 * alfa);
            ponto(c.subtract(eixo).add(fino), 0xE8D7B0, .85 * alfa);
        }

        /**
         * A Terra: a calota do planeta de sempre em volta da direção dela, com o mesmo desenho reescalado. No
         * começo do recuo é igual ao planeta de sempre; depois encolhe até virar uma bola e um ponto.
         */
        private void terra(double t, Vetor rumo) {
            Vec3 e = noCeu(SistemaSolar.direcao(SistemaSolar.TERRA, t), rumo, 1);
            double raio = SistemaSolar.raioAparente(SistemaSolar.TERRA, t);
            if (raio <= SistemaSolar.MINIMO * 1.01) {
                disco(e.scale(R * .99), naCasca(raio), 0x4F8FE8, 1, 1);
                return;
            }
            Vec3[] uv = eixos(e);
            int aneis = 14, gomos = 72;
            for (int i = 0; i < aneis; i++) {
                double a0 = raio * i / aneis, a1 = raio * (i + 1) / aneis;
                double c0 = BORDA * i / aneis, c1 = BORDA * (i + 1) / aneis;
                for (int k = 0; k < gomos; k++) {
                    double g0 = Math.PI * 2 * k / gomos, g1 = Math.PI * 2 * (k + 1) / gomos;
                    ponto(naCalota(e, uv, a0, g0, R * .99), corDoPlaneta(c0, g0), 1);
                    ponto(naCalota(e, uv, a1, g0, R * .99), corDoPlaneta(c1, g0), 1);
                    ponto(naCalota(e, uv, a1, g1, R * .99), corDoPlaneta(c1, g1), 1);
                    ponto(naCalota(e, uv, a0, g1, R * .99), corDoPlaneta(c0, g1), 1);
                }
            }
        }

        /**
         * As luzes do recuo, que somam: a atmosfera em volta da Terra (do tamanho dela), o sol de sempre nascendo
         * atrás dela enquanto ela é o chão, o banho roxo sobre a Terra e o ponto roxo do clarão, que pulsa e se
         * abre no fim até tomar a tela.
         */
        private void luzesDoRecuo(double t, Vetor rumo) {
            Vec3 e = noCeu(SistemaSolar.direcao(SistemaSolar.TERRA, t), rumo, 1);
            double raio = SistemaSolar.raioAparente(SistemaSolar.TERRA, t), escala = raio / BORDA;
            double antigos = SistemaSolar.astrosAntigos(t);
            Vec3[] uv = eixos(e);
            int gomos = 72;
            double fora = Math.toRadians(5) * escala, dentro = Math.toRadians(9) * escala;
            for (int k = 0; k < gomos; k++) {
                double g0 = Math.PI * 2 * k / gomos, g1 = Math.PI * 2 * (k + 1) / gomos;
                double d0 = .3 + .7 * Math.pow(.5 + .5 * Math.cos(g0 - SOL), 2), d1 = .3 + .7 * Math.pow(.5 + .5 * Math.cos(g1 - SOL), 2);
                ponto(naCalota(e, uv, raio, g0, R * .98), 0x4FA3FF, .9 * d0);
                ponto(naCalota(e, uv, raio + fora, g0, R * .98), 0x2A5BD8, 0);
                ponto(naCalota(e, uv, raio + fora, g1, R * .98), 0x2A5BD8, 0);
                ponto(naCalota(e, uv, raio, g1, R * .98), 0x4FA3FF, .9 * d1);
                ponto(naCalota(e, uv, raio, g0, R * .98), 0x7FC2FF, .7 * d0);
                ponto(naCalota(e, uv, raio - dentro, g0, R * .98), 0x2A6FE0, 0);
                ponto(naCalota(e, uv, raio - dentro, g1, R * .98), 0x2A6FE0, 0);
                ponto(naCalota(e, uv, raio, g1, R * .98), 0x7FC2FF, .7 * d1);
            }
            if (antigos > .004) {
                Vec3 sol = naCalota(e, uv, raio + Math.toRadians(2.5) * escala, SOL, R * .97);
                disco(sol, 34 * escala, 0x6FA8FF, .35 * antigos, 0);
                disco(sol, 14 * escala, 0xCFE2FF, .7 * antigos, 0);
                disco(sol, 4.5 * escala, 0xFFFFFF, antigos, 0);
            }
            double roxo = Math.min(1, SistemaSolar.pontoRoxo(t));
            if (roxo <= .004) return;
            disco(e.scale(R * .985), naCasca(raio), 0x9A7CFF, .45 * roxo, .2 * roxo);
            Vec3 c = noCeu(SistemaSolar.direcao(SistemaSolar.PONTO, t), rumo, R * .96);
            double tamanho = naCasca(SistemaSolar.raioDoPonto(t));
            double pulso = SistemaSolar.pontoRoxo(t);
            disco(c, tamanho * 5 * pulso, 0x7A4CFF, .45 * roxo, 0);
            disco(c, tamanho * 2 * pulso, 0xB89CFF, .9 * roxo, 0);
            disco(c, tamanho * .8, 0xFFFFFF, roxo, 0);
            if (t >= SistemaSolar.TERRA_INTEIRA) {
                Vec3 ver = c.normalize();
                for (Vec3 eixo : eixos(ver)) {
                    Vec3 comprido = eixo.scale(tamanho * 9 * pulso), fino = eixo.cross(ver).normalize().scale(tamanho * .12);
                    ponto(c.subtract(comprido), 0xD8C8FF, 0);
                    ponto(c.subtract(fino), 0xD8C8FF, .8 * roxo);
                    ponto(c.add(comprido), 0xD8C8FF, 0);
                    ponto(c.add(fino), 0xD8C8FF, .8 * roxo);
                }
            }
        }
    }
}
