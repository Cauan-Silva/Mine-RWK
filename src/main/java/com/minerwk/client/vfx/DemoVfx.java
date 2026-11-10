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
    /** {@code r}: o R de verdade num alvo invocado, com a cutscene inteira, longe do spawn. */
    private static final boolean R_COMPLETO = MODO.equals("r");
    private static final int LONGE_DO_SPAWN = 300;
    /** {@code traje}: o jogador como Gojo de frente, de costas, num golpe, com armadura vestida e em 1ª pessoa. */
    private static final boolean TRAJE = MODO.equals("traje");
    private static boolean vestiuPeitoral;
    /** {@code arvore}: a tela da árvore do Gojo em três momentos (sem pontos, com compras e desperto). */
    private static final boolean ARVORE = MODO.equals("arvore");
    private static final int CARGA = 100;
    private static final Path PASTA = Path.of("demo");
    private static final int ESFERAS = RoxoMaximo.TICKS_ESFERAS;
    private static final int ESTOURO = ESFERAS + RoxoMaximo.TICKS_FUSAO;
    private static final int FIM_PREVIA = ARVORE ? 100 : TRAJE ? 260 : R_COMPLETO ? 360
            : ENCANTAMENTO ? CARGA + 90 : ESTOURO + RoxoMaximo.TICKS_EXPLOSAO + 110;
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
            jogador.connection.sendCommand(ENCANTAMENTO || R_COMPLETO || TRAJE ? "time set noon" : "time set midnight");
            if (ARVORE) jogador.connection.sendCommand("rwk trocar @s gojo");
            if (TRAJE) {
                jogador.connection.sendCommand("rwk trocar @s gojo");
                // Terreno limpo, longe dos blocos soltos do spawn; volta no fim.
                jogador.connection.sendCommand("tp @s ~150 ~12 ~");
            }
            // A explosão apaga blocos: a demo do R acontece longe do spawn e o jogador volta no fim.
            if (R_COMPLETO) jogador.connection.sendCommand("tp @s ~" + LONGE_DO_SPAWN + " ~12 ~"); // do alto: o chão de lá pode ser mais alto
            jogador.connection.sendCommand("weather clear");
            jogador.connection.sendCommand("gamemode creative");
            jogador.setYRot(0);
            jogador.setXRot(ENCANTAMENTO ? 6 : R_COMPLETO ? 4 : -8);
            if (ENCANTAMENTO) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
        if (R_COMPLETO && ticks == 120) {
            jogador.setYRot(0);
            jogador.setXRot(4);
            jogador.connection.sendCommand("rwk trocar @s gojo");
            jogador.connection.sendCommand("kill @e[type=minecraft:husk,distance=..60]");
            jogador.connection.sendCommand("execute at @s run summon minecraft:husk ~ ~ ~14 {NoAI:1b}");
        }
        if (R_COMPLETO && ticks == 190) jogador.connection.sendCommand("rwk recarregar");
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
        if (ARVORE) {
            mc.gui.getChat().clearMessages(false);
            if (t == 0) jogador.connection.sendCommand("rwk maestria @s zerar");
            if (t == 2) jogador.connection.sendCommand("rwk despertar @s nao");
            if (t == 5) com.minerwk.client.kit.TelaArvore.abrir();
            if (t == 30) jogador.connection.sendCommand("rwk maestria @s nivel 24");
            if (t == 32) {
                for (String no : new String[] {"gojo_azul_alcance", "gojo_azul_alcance", "gojo_infinito_duracao",
                        "gojo_energia_reserva", "gojo_energia_reserva", "gojo_energia_reserva", "gojo_despertar"}) {
                    jogador.connection.sendCommand("rwk maestria @s comprar " + no);
                }
            }
            if (t == 60) jogador.connection.sendCommand("rwk despertar @s sim");
            if (t == 62) jogador.connection.sendCommand("rwk maestria @s comprar gojo_vermelho_forca");
            // Não deixa o mundo de testes com a maestria da demonstração.
            if (t == FIM_PREVIA - 4) jogador.connection.sendCommand("rwk maestria @s zerar");
            if (t == FIM_PREVIA - 3) jogador.connection.sendCommand("rwk despertar @s nao");
            if (t >= FIM_PREVIA) previa = -1;
            return;
        }
        if (TRAJE) {
            mc.gui.getChat().clearMessages(false);
            var tipo = net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
            if (t >= 50 && t < 100) tipo = net.minecraft.client.CameraType.THIRD_PERSON_BACK;
            if (t >= 150 && t < 190) tipo = net.minecraft.client.CameraType.FIRST_PERSON;
            mc.options.setCameraType(tipo);
            jogador.setXRot(t >= 150 && t < 190 ? 20 : 8);
            if (t == 2 || t == 150 || t == 188) jogador.connection.sendCommand("rwk recarregar");
            if (t == 1) {
                // Mão vazia, para o braço aparecer em primeira pessoa.
                for (int espaco = 0; espaco < 9; espaco++) {
                    if (jogador.getInventory().getItem(espaco).isEmpty()) {
                        jogador.getInventory().selected = espaco;
                        jogador.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(espaco));
                        break;
                    }
                }
            }
            // Um Vermelho de frente e outro em primeira pessoa (não arranca blocos, que tapariam a câmera):
            // o traje tem de seguir os braços, e a manga tem de aparecer na mão parada antes do golpe.
            if (t == 20 || t == 168) tecla(com.minerwk.core.kit.Tecla.X, true);
            if (t == 34 || t == 180) tecla(com.minerwk.core.kit.Tecla.X, false);
            // Uma corrida vista de costas: a barra do casaco balança.
            if (t >= 60 && t < 95) {
                Vec3 frente = new Vec3(jogador.getLookAngle().x, 0, jogador.getLookAngle().z).normalize();
                jogador.setDeltaMovement(frente.scale(.28).add(0, jogador.getDeltaMovement().y, 0));
            }
            // Armadura normal vestida não pode aparecer por cima (só testa se o espaço do peito estiver vazio).
            if (t == 100 && jogador.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty()) {
                vestiuPeitoral = true;
                jogador.connection.sendCommand("item replace entity @s armor.chest with minecraft:diamond_chestplate");
            }
            if (t == 148 && vestiuPeitoral) jogador.connection.sendCommand("item replace entity @s armor.chest with minecraft:air");
            // O domínio: a venda sai e os olhos brilham.
            if (t == 192) tecla(com.minerwk.core.kit.Tecla.G, true);
            if (t == 194) tecla(com.minerwk.core.kit.Tecla.G, false);
            if (t == FIM_PREVIA - 3) jogador.connection.sendCommand("tp @s ~-150 ~ ~-10");
            if (t >= FIM_PREVIA) {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                previa = -1;
            }
            return;
        }
        if (R_COMPLETO) {
            mc.gui.getChat().clearMessages(false); // sem as respostas dos comandos na gravação
            // O R de verdade: aperta e solta a tecla; o servidor faz o resto (cutscene, alvo, explosão).
            if (t == 0) net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.minerwk.rede.UsarTecla(com.minerwk.core.kit.Tecla.R, true));
            if (t == 3) net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.minerwk.rede.UsarTecla(com.minerwk.core.kit.Tecla.R, false));
            if (t == FIM_PREVIA - 3) jogador.connection.sendCommand("tp @s ~-" + LONGE_DO_SPAWN + " ~ ~");
            if (t >= FIM_PREVIA) previa = -1;
            return;
        }
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

    private static void tecla(com.minerwk.core.kit.Tecla tecla, boolean apertou) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.minerwk.rede.UsarTecla(tecla, apertou));
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
