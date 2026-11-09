package com.minerwk.client.camera;

import com.minerwk.MineRWK;
import com.minerwk.client.ConfigCliente;
import com.minerwk.core.camera.Coreografia;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200;
import com.minerwk.core.personagens.gojo.CutsceneRoxoMaximo;
import com.minerwk.rede.Cutscene;
import com.minerwk.rede.EncerrarVisual;
import java.util.List;
import java.util.Map;
import net.minecraft.client.CameraType;
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
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Cutscene de ultimate no cliente de quem usou o golpe (e, na parte 2 do R, do alvo, que assiste com a
 * câmera na base do Gojo). A câmera vira uma entidade só do cliente que segue a {@link Coreografia};
 * barras de cinema, HUD e mão escondidas, andar bloqueado e o olhar do jogador preso (o golpe sai para
 * onde ele olhava). Uma cutscene do mesmo dono que chega com outra ativa emenda sem corte. Acaba
 * sozinha, se a carga for cortada, se o dono morrer ou se o servidor mandar {@link Cutscene#FIM}.
 */
public final class CutsceneCliente {
    /**
     * O que cada cutscene tem: a câmera, as palavras na tela, o visual de carga que a corta, se quem não
     * é o dono (o alvo) também assiste, se outra parte vem depois (as barras não fecham esperando) e o
     * tick a partir do qual a base para de seguir o dono (plano aberto parado; -1: sempre segue).
     */
    record Roteiro(Coreografia coreografia, List<CutsceneRoxo200.Palavra> palavras, String cargaQueCorta,
                   boolean espectador, boolean continua, int congelarDesde) {
    }

    private static final Map<String, Roteiro> ROTEIROS = Map.of(
            CutsceneRoxo200.ID, new Roteiro(CutsceneRoxo200.COREOGRAFIA, CutsceneRoxo200.PALAVRAS,
                    "gojo_roxo_200_carga", false, true, -1));

    private static Roteiro roteiroDe(String id) {
        Roteiro r = ROTEIROS.get(id);
        if (r != null) return r;
        return CutsceneRoxoMaximo.deId(id)
                .map(c -> new Roteiro(c, CutsceneRoxo200.PALAVRAS_DISPARO, "", true, false,
                        c.duracao() - CutsceneRoxoMaximo.PLANO_ABERTO - CutsceneRoxoMaximo.VOLTA))
                .orElse(null);
    }
    public static final ResourceLocation CAMADA = MineRWK.id("cutscene");

    private static Roteiro roteiro;
    private static Cutscene atual;
    private static ClientLevel mundo;
    /** Com a câmera de cinema (falso: config desligada, só as palavras). */
    private static boolean comCamera;
    private static Entity camera;
    private static float yawJogador, pitchJogador;
    private static Coreografia.Quadro quadro;
    /** Tipo de câmera de antes (F5): a cutscene força a primeira pessoa e devolve no fim. */
    private static CameraType cameraAntes;
    /** Emendou numa cutscene anterior: as barras já estão na tela. */
    private static boolean emendada;
    /** Base parada do plano aberto (o dono desce devagar; a explosão não). */
    private static Vetor origemCongelada;

    private CutsceneCliente() {
    }

    public static boolean ativa() {
        return atual != null;
    }

    public static void receber(Cutscene pacote) {
        Minecraft mc = Minecraft.getInstance();
        if (pacote.id().equals(Cutscene.FIM)) {
            if (atual != null && atual.dono() == pacote.dono()) encerrar();
            return;
        }
        Roteiro r = roteiroDe(pacote.id());
        if (mc.level == null || mc.player == null || r == null) return;
        if (pacote.dono() != mc.player.getId() && !r.espectador()) return;
        if (atual != null && atual.dono() == pacote.dono() && (!comCamera || camera != null)) {
            // Continuação (a parte 2 do R): troca o roteiro sem soltar a câmera.
            roteiro = r;
            atual = pacote;
            emendada = true;
            origemCongelada = null;
            return;
        }
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
                cameraAntes = mc.options.getCameraType();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                posicionar(0, 0);
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
        if (cameraAntes != null) mc.options.setCameraType(cameraAntes);
        cameraAntes = null;
        atual = null;
        roteiro = null;
        camera = null;
        quadro = null;
        comCamera = false;
        emendada = false;
        origemCongelada = null;
        mundo = null;
    }

    private static double idade(float parcial) {
        return Minecraft.getInstance().level.getGameTime() - atual.inicio() + parcial;
    }

    /** Antes do tick do jogador: o olhar enviado ao servidor continua o do começo (o golpe sai para lá). */
    public static void aoTickAntes(ClientTickEvent.Pre evento) {
        if (atual != null) prenderOlhar();
    }

    private static void prenderOlhar() {
        var jogador = Minecraft.getInstance().player;
        if (jogador == null) return;
        jogador.setYRot(yawJogador);
        jogador.setXRot(pitchJogador);
        jogador.yRotO = yawJogador;
        jogador.xRotO = pitchJogador;
        jogador.setYHeadRot(yawJogador);
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        if (atual == null) return;
        prenderOlhar();
        Minecraft mc = Minecraft.getInstance();
        Entity dono = mc.level == null ? null : mc.level.getEntity(atual.dono());
        if (mc.level != mundo || mc.player == null || !mc.player.isAlive() || (dono != null && !dono.isAlive())
                || idade(0) > roteiro.coreografia().duracao()) encerrar();
    }

    /** Antes de cada quadro: põe a câmera no lugar e segura o olhar do jogador. */
    public static void aoComecarQuadro(RenderFrameEvent.Pre evento) {
        if (atual == null || Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) return;
        float parcial = evento.getPartialTick().getGameTimeDeltaPartialTick(false);
        prenderOlhar();
        if (comCamera) {
            // F5 durante a cutscene tiraria a câmera da coreografia.
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON) mc.options.setCameraType(CameraType.FIRST_PERSON);
            posicionar(idade(parcial), parcial);
        }
    }

    /**
     * A câmera segue os olhos do dono do golpe (no R ele avança e sobe), virada para a direção do
     * pacote. Longe demais para o cliente ver o dono, usa os olhos de quando a cutscene começou.
     */
    private static void posicionar(double t, float parcial) {
        quadro = roteiro.coreografia().quadro(t);
        Entity dono = Minecraft.getInstance().level.getEntity(atual.dono());
        Vetor origem = atual.origem();
        if (dono != null) {
            var olhos = dono.getEyePosition(parcial);
            origem = new Vetor(olhos.x, olhos.y, olhos.z);
        }
        if (roteiro.congelarDesde() >= 0 && t >= roteiro.congelarDesde()) {
            if (origemCongelada == null) origemCongelada = origem;
            origem = origemCongelada;
        }
        Vetor pos = Coreografia.naBase(quadro.posicao(), origem, atual.direcao());
        Vetor alvo = Coreografia.naBase(quadro.alvo(), origem, atual.direcao());
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

    /** Com a câmera de cinema, só a camada da cutscene e o chat aparecem. */
    public static void aoCamadaGui(RenderGuiLayerEvent.Pre evento) {
        if (atual != null && comCamera && !evento.getName().equals(CAMADA)
                && !evento.getName().equals(VanillaGuiLayers.CHAT)) evento.setCanceled(true);
    }

    /** Atacar, usar e escolher bloco ficam desligados: a mira seguiria a câmera de cinema. */
    public static void aoInteragir(InputEvent.InteractionKeyMappingTriggered evento) {
        if (atual == null) return;
        evento.setCanceled(true);
        evento.setSwingHand(false);
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
            double fecha = roteiro.continua() ? 1 : suave((fim - t) / 12);
            int barra = (int) (altura * .12 * (emendada ? 1 : suave(t / 10)) * fecha);
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
