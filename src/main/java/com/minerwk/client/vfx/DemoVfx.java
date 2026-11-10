package com.minerwk.client.vfx;

import com.minerwk.MineRWK;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Demonstração automática para gravar prévias (run {@code clientDemo}): com {@code -Dminerwk.demo=true},
 * toca os protótipos em sequência depois de entrar no mundo, grava um quadro por tick em {@code run/demo/}
 * e fecha o jogo no fim. O vídeo é montado depois com o ffmpeg.
 */
public final class DemoVfx {
    private DemoVfx() {
    }

    private static final boolean LIGADA = Boolean.getBoolean("minerwk.demo");
    private static final Path PASTA = Path.of("demo");
    private static final int INICIO_GRAVACAO = 80;
    private static final int FIM = 560;
    private static int ticks;
    private static int quadro;

    public static void aoTick(ClientTickEvent.Post e) {
        if (!LIGADA) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        LocalPlayer jogador = mc.player;
        if (jogador == null || mc.isPaused()) return;
        ticks++;
        if (ticks == 40) {
            jogador.connection.sendCommand("time set midnight");
            jogador.connection.sendCommand("gamemode creative");
            jogador.setYRot(0);
            jogador.setXRot(5);
        }
        if (ticks > 45 && jogador.getAbilities().mayfly) jogador.getAbilities().flying = true;
        BlockPos frente = BlockPos.containing(jogador.position().add(new Vec3(0, 0, 9)));
        switch (ticks) {
            case 100, 160 -> PrototipoPhoton.fogos(jogador.level(), frente);
            case 260 -> PrototipoPhoton.halo(jogador.level(), frente);
            case 400 -> PrototipoPhoton.casca(jogador.level(), frente);
            case FIM -> mc.stop();
            default -> {
            }
        }
        if (ticks >= INICIO_GRAVACAO && ticks < FIM) gravarQuadro(mc);
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
