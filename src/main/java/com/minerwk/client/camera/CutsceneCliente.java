package com.minerwk.client.camera;

import com.minerwk.MineRWK;
import com.minerwk.client.ConfigCliente;
import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200;
import com.minerwk.rede.Cutscene;
import com.minerwk.rede.EncerrarVisual;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Cutscene de ultimate no cliente de quem usou o golpe. A câmera vira uma entidade só do cliente que
 * segue a {@link Coreografia}; barras de cinema, HUD e mão escondidas, andar bloqueado e o olhar do
 * jogador preso (o golpe sai para onde ele olhava). Acaba sozinha, ou na hora se a carga for cortada.
 */
public final class CutsceneCliente {
    /** O que cada cutscene tem: a câmera, as palavras na tela e o visual de carga que a corta. */
    record Roteiro(Coreografia coreografia, List<CutsceneRoxo200.Palavra> palavras, String cargaQueCorta) {
    }

    private static final Map<String, Roteiro> ROTEIROS = Map.of(
            CutsceneRoxo200.ID, new Roteiro(CutsceneRoxo200.COREOGRAFIA, CutsceneRoxo200.PALAVRAS, "gojo_roxo_200_carga"));
    public static final ResourceLocation CAMADA = MineRWK.id("cutscene");

    private static Roteiro roteiro;
    private static Cutscene atual;
    private static ClientLevel mundo;
    /** Com a câmera de cinema (falso: config desligada, só as palavras). */
    private static boolean comCamera;
    private static Entity camera;
    private static float yawJogador, pitchJogador;
    private static Coreografia.Quadro quadro;

    private CutsceneCliente() {
    }

    public static boolean ativa() {
        return atual != null;
    }

    public static void receber(Cutscene pacote) {
        Minecraft mc = Minecraft.getInstance();
        Roteiro r = ROTEIROS.get(pacote.id());
        if (mc.level == null || mc.player == null || r == null || pacote.dono() != mc.player.getId()) return;
        encerrar();
        roteiro = r;
        atual = pacote;
        mundo = mc.level;
        yawJogador = mc.player.getYRot();
        pitchJogador = mc.player.getXRot();
        comCamera = ConfigCliente.cutscenes();
        if (comCamera) {
            camera = EntityType.MARKER.create(mc.level);
            if (camera == null) {
                comCamera = false;
            } else {
                posicionar(0);
                mc.setCameraEntity(camera);
            }
        }
    }

    /** O visual de carga do golpe acabou antes da hora (golpe forte, morte): corta a cutscene. */
    public static void aoEncerrarVisual(EncerrarVisual pacote) {
        if (atual != null && pacote.dono() == atual.dono() && pacote.tipo().equals(roteiro.cargaQueCorta())) encerrar();
    }

    public static void encerrar() {
        Minecraft mc = Minecraft.getInstance();
        if (comCamera && mc.player != null) mc.setCameraEntity(mc.player);
        atual = null;
        roteiro = null;
        camera = null;
        quadro = null;
        comCamera = false;
        mundo = null;
    }

    private static double idade(float parcial) {
        return Minecraft.getInstance().level.getGameTime() - atual.inicio() + parcial;
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        if (atual == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != mundo || mc.player == null || !mc.player.isAlive()
                || idade(0) > roteiro.coreografia().duracao()) encerrar();
    }

    /** Antes de cada quadro: põe a câmera no lugar e segura o olhar do jogador. */
    public static void aoComecarQuadro(RenderFrameEvent.Pre evento) {
        if (atual == null || Minecraft.getInstance().level == null) return;
        float parcial = evento.getPartialTick().getGameTimeDeltaPartialTick(false);
        var jogador = Minecraft.getInstance().player;
        if (jogador != null) {
            jogador.setYRot(yawJogador);
            jogador.setXRot(pitchJogador);
            jogador.yRotO = yawJogador;
            jogador.xRotO = pitchJogador;
        }
        if (comCamera) posicionar(idade(parcial));
    }

    private static void posicionar(double t) {
        quadro = roteiro.coreografia().quadro(t);
        Vetor pos = Coreografia.naBase(quadro.posicao(), atual.origem(), atual.direcao());
        Vetor alvo = Coreografia.naBase(quadro.alvo(), atual.origem(), atual.direcao());
        Vetor d = alvo.menos(pos);
        double horizontal = Math.sqrt(d.x() * d.x() + d.z() * d.z());
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x(), d.z()));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y(), horizontal));
        camera.setPos(pos.x(), pos.y(), pos.z());
        camera.xo = pos.x();
        camera.yo = pos.y();
        camera.zo = pos.z();
        camera.xOld = pos.x();
        camera.yOld = pos.y();
        camera.zOld = pos.z();
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
    }

    public static void aoCalcularAngulos(ViewportEvent.ComputeCameraAngles evento) {
        if (atual != null && comCamera && quadro != null) evento.setRoll((float) quadro.inclinacao());
    }

    public static void aoCalcularFov(ViewportEvent.ComputeFov evento) {
        if (atual != null && comCamera && quadro != null && evento.usedConfiguredFov()) {
            evento.setFOV(evento.getFOV() * quadro.fov());
        }
    }

    public static void aoEntrada(MovementInputUpdateEvent evento) {
        if (atual == null) return;
        var e = evento.getInput();
        e.forwardImpulse = 0;
        e.leftImpulse = 0;
        e.up = e.down = e.left = e.right = false;
        e.jumping = false;
        e.shiftKeyDown = false;
    }

    /** Com a câmera de cinema, só a camada da cutscene aparece. */
    public static void aoCamadaGui(RenderGuiLayerEvent.Pre evento) {
        if (atual != null && comCamera && !evento.getName().equals(CAMADA)) evento.setCanceled(true);
    }

    public static void aoMao(RenderHandEvent evento) {
        if (atual != null && comCamera) evento.setCanceled(true);
    }

    private static double suave(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Barras de cinema e as palavras do encantamento. */
    public static void desenhar(GuiGraphics g, DeltaTracker tempo) {
        if (atual == null || Minecraft.getInstance().level == null) return;
        double t = idade(tempo.getGameTimeDeltaPartialTick(false));
        int largura = g.guiWidth();
        int altura = g.guiHeight();
        if (comCamera) {
            int fim = roteiro.coreografia().duracao();
            int barra = (int) (altura * .12 * suave(t / 10) * suave((fim - t) / 12));
            g.fill(0, 0, largura, barra, 0xFF000000);
            g.fill(0, altura - barra, largura, altura, 0xFF000000);
        }
        Font fonte = Minecraft.getInstance().font;
        for (CutsceneRoxo200.Palavra p : roteiro.palavras()) {
            if (t < p.inicio() || t > p.fim()) continue;
            double alpha = suave((t - p.inicio()) / 4) * suave((p.fim() - t) / 4);
            if (alpha < .03) continue;
            int a = (int) (alpha * 255) << 24;
            g.pose().pushPose();
            g.pose().translate(largura / 2f, altura * .66f, 0);
            g.pose().scale(3, 3, 1);
            g.drawCenteredString(fonte, Component.translatable(p.chave() + ".jp"), 0, 0, a | 0xF2E6FF);
            g.pose().popPose();
            g.pose().pushPose();
            g.pose().translate(largura / 2f, altura * .66f + 34, 0);
            g.pose().scale(1.5f, 1.5f, 1);
            g.drawCenteredString(fonte, Component.translatable(p.chave()), 0, 0, a | 0xC8A8FF);
            g.pose().popPose();
        }
    }
}
