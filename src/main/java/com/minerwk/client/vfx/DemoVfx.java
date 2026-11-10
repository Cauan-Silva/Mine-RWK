package com.minerwk.client.vfx;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.RoxoMaximo;
import com.minerwk.rede.MostrarVisual;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Prévia do VFX do R sem precisar de alvo nem do servidor: {@code /rwk previa} toca, só no cliente, as
 * esferas e a fusão a 10 blocos na frente e depois a explosão a 70 blocos (como no plano aberto da
 * cutscene). Na run {@code clientDemo} ({@code -Dminerwk.demo=true}) a prévia começa sozinha, cada tick
 * vira um quadro em {@code run/demo/} e o jogo fecha no fim; o vídeo é montado com o ffmpeg.
 */
public final class DemoVfx {
    private DemoVfx() {
    }

    /** {@code true}: fusão e explosão; {@code encantamento}: a carga do R de dia, em terceira pessoa. */
    private static final String MODO = System.getProperty("minerwk.demo", "");
    private static final boolean DEMO = !MODO.isEmpty();
    private static final boolean ENCANTAMENTO = MODO.equals("encantamento");
    private static final int CARGA = 100;
    private static final Path PASTA = Path.of("demo");
    private static final int ESFERAS = RoxoMaximo.TICKS_ESFERAS;
    private static final int ESTOURO = ESFERAS + RoxoMaximo.TICKS_FUSAO;
    private static final int FIM_PREVIA = ENCANTAMENTO ? CARGA + 90 : ESTOURO + RoxoMaximo.TICKS_EXPLOSAO + 110;
    private static int ticks;
    private static int quadro;
    /** Tick (do contador da demo) em que a prévia começou; -1 parada. */
    private static int previa = -1;
    private static Vec3 perto, longe;

    /** Começa a prévia do R na frente do jogador. */
    public static void previa() {
        previa = ticks;
        perto = null;
    }

    public static void aoTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (DEMO) mc.options.pauseOnLostFocus = false;
        LocalPlayer jogador = mc.player;
        if (jogador == null || mc.isPaused()) return;
        ticks++;
        if (DEMO && ticks == 40) {
            jogador.connection.sendCommand(ENCANTAMENTO ? "time set noon" : "time set midnight");
            jogador.connection.sendCommand("weather clear");
            jogador.connection.sendCommand("gamemode creative");
            jogador.setYRot(0);
            jogador.setXRot(ENCANTAMENTO ? 6 : -8);
            if (ENCANTAMENTO) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
        // Espera o servidor integrado assentar depois de carregar o mundo (senão o tempo do visual trava e pula).
        if (DEMO && ticks == 200) previa();
        if (previa < 0) return;
        int t = ticks - previa;
        tocarPrevia(mc, jogador, t);
        if (DEMO) {
            gravarQuadro(mc);
            // A prévia zera a si mesma no último tick: a parada usa o t guardado antes.
            if (t >= FIM_PREVIA) mc.stop();
        }
    }

    private static void tocarPrevia(Minecraft mc, LocalPlayer jogador, int t) {
        if (ENCANTAMENTO) {
            // Só a carga: aura, olhos, rachaduras, pedras e o céu escurecendo, e depois o céu voltando.
            if (t == 0) {
                VisuaisAtivos.receber(new MostrarVisual(jogador.getId(), "gojo_roxo_200_carga",
                        vetor(jogador.getEyePosition()), new Vetor(jogador.getLookAngle().x, 0, jogador.getLookAngle().z),
                        0, CARGA, mc.level.getGameTime()));
            }
            if (t >= FIM_PREVIA) previa = -1;
            return;
        }
        if (DEMO) {
            jogador.setNoGravity(true);
            jogador.setDeltaMovement(Vec3.ZERO);
        }
        if (perto == null) {
            Vec3 frente = new Vec3(jogador.getLookAngle().x, 0, jogador.getLookAngle().z).normalize();
            perto = jogador.getEyePosition().add(frente.scale(10)).add(0, 2, 0);
            longe = jogador.getEyePosition().add(frente.scale(70)).add(0, 8, 0);
        }
        long agora = mc.level.getGameTime();
        Vetor dir = new Vetor(jogador.getLookAngle().x, 0, jogador.getLookAngle().z);
        if (t == 0) {
            VisuaisAtivos.receber(new MostrarVisual(jogador.getId(), "gojo_roxo_maximo_esferas", vetor(perto), dir,
                    ESFERAS, ESTOURO, agora));
        }
        if (t == ESTOURO + 10) {
            VisuaisAtivos.receber(new MostrarVisual(jogador.getId(), "gojo_roxo_explosao", vetor(longe), dir,
                    (float) RoxoMaximo.RAIO_EXPLOSAO, RoxoMaximo.TICKS_EXPLOSAO + RoxoMaximo.TICKS_RESTO, agora));
        }
        if (t >= FIM_PREVIA) previa = -1;
    }

    private static Vetor vetor(Vec3 v) {
        return new Vetor(v.x, v.y, v.z);
    }

    private static void gravarQuadro(Minecraft mc) {
        NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        Path arquivo = PASTA.resolve(String.format("quadro_%04d.png", quadro++));
        Util.ioPool().execute(() -> {
            try (img) {
                Files.createDirectories(PASTA);
                img.writeToFile(arquivo);
            } catch (IOException ex) {
                MineRWK.LOGGER.warn("Demo: falha ao gravar {}", arquivo, ex);
            }
        });
    }
}
