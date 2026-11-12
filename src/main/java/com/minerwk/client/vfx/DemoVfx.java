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
    /** {@code ichigo_traje}: o Ichigo no Shikai e no Bankai, de frente, correndo de costas e em 1ª pessoa. */
    private static final boolean TRAJE_ICHIGO = MODO.equals("ichigo_traje");
    /** {@code ichigo}: os golpes do Shikai (Z, X, C, V), a entrada do Bankai e os golpes dele, num alvo parado. */
    private static final boolean ICHIGO = MODO.equals("ichigo");
    /** {@code ichigo_mugetsu}: o Bankai, a máscara Hollow de frente, e o Mugetsu inteiro com a perda dos poderes. */
    private static final boolean MUGETSU = MODO.equals("ichigo_mugetsu");
    /** {@code arvore}: a tela da árvore do Gojo em três momentos (sem pontos, com compras e desperto). */
    private static final boolean ARVORE = MODO.equals("arvore");
    /** {@code toji}: a luta contra o Toji: a entrada, a Nuvem, a investida, a queda com a cutscene, o Desaparecer e a fase 3. */
    private static final boolean TOJI = MODO.equals("toji");
    /** {@code zangetsu}: a luta contra o Zangetsu interior, do mundo interior à vitória, com a queda e a máscara. */
    private static final boolean ZANGETSU = MODO.equals("zangetsu");
    /** O tick da prévia em que o Zangetsu apareceu (a arena leva um tempo para ficar pronta). */
    private static int zangetsuEm = -1;
    private static boolean fimDaDemo;
    private static final int CARGA = 100;
    private static final Path PASTA = Path.of("demo");
    private static final int ESFERAS = RoxoMaximo.TICKS_ESFERAS;
    private static final int ESTOURO = ESFERAS + RoxoMaximo.TICKS_FUSAO;
    private static final int FIM_PREVIA = ZANGETSU ? 2400 : TOJI ? 520 : ARVORE ? 100 : TRAJE ? 260 : TRAJE_ICHIGO ? 300 : ICHIGO ? 350 : MUGETSU ? 360 : R_COMPLETO ? 360
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
        // Uma gravação anterior pode ter acabado com o jogador morto: o mundo de testes guarda isso.
        if (DEMO && ticks == 10 && jogador.isDeadOrDying()) {
            jogador.respawn();
            mc.setScreen(null);
        }
        if (DEMO && ticks == 40) {
            jogador.connection.sendCommand(ENCANTAMENTO || R_COMPLETO || TRAJE || TRAJE_ICHIGO || ICHIGO || MUGETSU || TOJI || ZANGETSU ? "time set noon" : "time set midnight");
            if (ARVORE) jogador.connection.sendCommand("rwk trocar @s gojo");
            if (TOJI) {
                jogador.connection.sendCommand("rwk trocar @s gojo");
                jogador.connection.sendCommand("rwk despertar @s nao");
                jogador.connection.sendCommand("tp @s ~150 ~12 ~");
            }
            if (ZANGETSU) jogador.connection.sendCommand("rwk despertar @s nao");
            if (TRAJE_ICHIGO || ICHIGO || MUGETSU || ZANGETSU) {
                jogador.connection.sendCommand("rwk trocar @s ichigo");
                jogador.connection.sendCommand("tp @s ~150 ~12 ~");
            }
            if (TRAJE) {
                jogador.connection.sendCommand("rwk trocar @s gojo");
                jogador.connection.sendCommand("rwk despertar @s sim");
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
            jogador.connection.sendCommand("rwk despertar @s sim");
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
            if (t >= FIM_PREVIA || fimDaDemo) mc.stop();
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
        if (ZANGETSU) {
            previaDoZangetsu(mc, jogador, t);
            return;
        }
        if (TOJI) {
            // No sobrevivência, para os golpes do Toji valerem; o comando de teste dispensa o Contrato.
            if (t == 0) {
                jogador.connection.sendCommand("gamerule sendCommandFeedback false");
                jogador.connection.sendCommand("gamemode survival");
            }
            if (t <= 2) mc.gui.getChat().clearMessages(false);
            // A entrada: ele chega e fala antes do primeiro golpe.
            if (t == 2) jogador.connection.sendCommand("rwk toji @s");
            if (t == 70) golpeDoToji(com.minerwk.core.boss.TojiBoss.NUVEM);
            if (t == 115) golpeDoToji(com.minerwk.core.boss.TojiBoss.INVESTIDA);
            // Tira mais de 20% da vida dele: o Toji arma a Lança que derruba, e a cutscene mostra o despertar.
            if (t == 160) jogador.connection.sendCommand("damage @e[type=minerwk:toji,limit=1,sort=nearest] 60");
            // A gravação não pode acabar numa tela de morte: depois do despertar o jogador aguenta mais.
            if (t >= 262 && t % 10 == 0) curarJogador();
            if (t == 285) golpeDoToji(com.minerwk.core.boss.TojiBoss.DESAPARECER);
            // Abaixo de 35% de vida: a fase 3 e a Execução do Assassino.
            if (t == 335) jogador.connection.sendCommand("damage @e[type=minerwk:toji,limit=1,sort=nearest] 110");
            // De fora nos golpes que se leem pelo chão; a cutscene cuida da própria câmera.
            boolean deFora = (t >= 60 && t < 160) || t >= 330;
            if (!com.minerwk.client.camera.CutsceneCliente.ativa()) {
                mc.options.setCameraType(deFora ? net.minecraft.client.CameraType.THIRD_PERSON_BACK
                        : net.minecraft.client.CameraType.FIRST_PERSON);
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof com.minerwk.boss.TojiEntity && !e.isInvisible()) {
                        jogador.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, e.position().add(0, 1.2, 0));
                    }
                }
            }
            // Longe demais o Toji vai embora sem vitória: o mundo de testes não fica com o despertar da demonstração.
            if (t == FIM_PREVIA - 6) {
                jogador.connection.sendCommand("gamerule sendCommandFeedback true");
                jogador.connection.sendCommand("gamemode creative");
                jogador.connection.sendCommand("tp @s ~-150 ~ ~-10");
            }
            if (t >= FIM_PREVIA) {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                previa = -1;
            }
            return;
        }
        if (ICHIGO) {
            mc.gui.getChat().clearMessages(false);
            // De lado, por uma câmera fixa (de costas o golpe some atrás do corpo): Getsuga carregado, Shunpo,
            // a onda no chão e o arremesso. De frente: a entrada do Bankai. De lado de novo: dois Getsugas
            // negros e o Shunpo; de frente: o Getsuga na lâmina; e a saída.
            boolean frente = t >= 136 && t < 212 || t >= 278 && t < 312;
            net.minecraft.world.entity.Entity camera = jogador;
            if (!frente) {
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof net.minecraft.world.entity.decoration.ArmorStand && e.distanceToSqr(jogador) < 60 * 60) camera = e;
                }
            }
            if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            mc.options.setCameraType(frente ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT
                    : net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            jogador.setXRot(frente ? 6 : 2);
            if (t == 0) {
                jogador.setYRot(0);
                jogador.connection.sendCommand("kill @e[type=minecraft:armor_stand,distance=..60]");
                jogador.connection.sendCommand("execute at @s run summon minecraft:armor_stand ~-3.5 ~0.6 ~7 "
                        + "{Invisible:1b,NoGravity:1b,Marker:0b,Rotation:[-90f,6f]}");
                jogador.connection.sendCommand("kill @e[type=minecraft:husk,distance=..60]");
                jogador.connection.sendCommand("execute at @s run summon minecraft:husk ~ ~ ~15 {NoAI:1b}");
            }
            if (t == 2 || t == 100 || t == 136 || t == 213 || t == 270 || t == 312) jogador.connection.sendCommand("rwk recarregar");
            int[][] teclas = {{10, 22, 0}, {52, 54, 1}, {76, 78, 2}, {104, 106, 3}, {140, 142, 4},
                    {220, 222, 0}, {238, 240, 0}, {256, 258, 1}, {276, 278, 2}, {318, 320, 4}};
            com.minerwk.core.kit.Tecla[] qual = {com.minerwk.core.kit.Tecla.Z, com.minerwk.core.kit.Tecla.X,
                    com.minerwk.core.kit.Tecla.C, com.minerwk.core.kit.Tecla.V, com.minerwk.core.kit.Tecla.G};
            for (int[] k : teclas) {
                if (t == k[0]) tecla(qual[k[2]], true);
                if (t == k[1]) tecla(qual[k[2]], false);
            }
            // Cliques da espada: dois no Shikai, um no Bankai e dois com o Getsuga na lâmina.
            if (t == 36 || t == 44 || t == 248 || t == 292 || t == 302) jogador.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (t == FIM_PREVIA - 3) {
                jogador.connection.sendCommand("kill @e[type=minecraft:husk,distance=..60]");
                jogador.connection.sendCommand("kill @e[type=minecraft:armor_stand,distance=..60]");
                jogador.connection.sendCommand("tp @s ~-150 ~ ~-30");
            }
            if (t >= FIM_PREVIA) {
                mc.setCameraEntity(jogador);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                previa = -1;
            }
            return;
        }
        if (MUGETSU) {
            mc.gui.getChat().clearMessages(false);
            // De lado, por uma câmera fixa: a entrada do Bankai. De frente: a máscara se formando. De lado de
            // novo: a carga do Mugetsu (a cutscene toma a câmera quando existe), o corte, o véu e os poderes indo.
            boolean frente = t >= 52 && t < 118;
            net.minecraft.world.entity.Entity camera = jogador;
            if (!frente) {
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof net.minecraft.world.entity.decoration.ArmorStand && e.distanceToSqr(jogador) < 60 * 60) camera = e;
                }
            }
            // Durante uma cutscene a câmera é dela.
            if (!com.minerwk.client.camera.CutsceneCliente.ativa()) {
                if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
                mc.options.setCameraType(frente ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT
                        : net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            }
            jogador.setXRot(frente ? 6 : 2);
            if (t == 0) {
                jogador.setYRot(0);
                jogador.connection.sendCommand("rwk despertar @s sim");
                jogador.connection.sendCommand("kill @e[type=minecraft:armor_stand,distance=..60]");
                jogador.connection.sendCommand("execute at @s run summon minecraft:armor_stand ~-5 ~0.8 ~9 "
                        + "{Invisible:1b,NoGravity:1b,Marker:0b,Rotation:[-100f,4f]}");
                jogador.connection.sendCommand("kill @e[type=minecraft:husk,distance=..80]");
                jogador.connection.sendCommand("execute at @s run summon minecraft:husk ~ ~ ~18 {NoAI:1b}");
            }
            if (t == 2 || t == 48 || t == 122) jogador.connection.sendCommand("rwk recarregar");
            // G: Bankai. V: a máscara. R: o Mugetsu.
            if (t == 6) tecla(com.minerwk.core.kit.Tecla.G, true);
            if (t == 8) tecla(com.minerwk.core.kit.Tecla.G, false);
            if (t == 56) tecla(com.minerwk.core.kit.Tecla.V, true);
            if (t == 58) tecla(com.minerwk.core.kit.Tecla.V, false);
            if (t == 128) tecla(com.minerwk.core.kit.Tecla.R, true);
            if (t == 130) tecla(com.minerwk.core.kit.Tecla.R, false);
            curarJogador();
            if (t == FIM_PREVIA - 3) {
                jogador.connection.sendCommand("kill @e[type=minecraft:husk,distance=..80]");
                jogador.connection.sendCommand("kill @e[type=minecraft:armor_stand,distance=..60]");
                jogador.connection.sendCommand("tp @s ~-150 ~ ~-30");
            }
            if (t >= FIM_PREVIA) {
                mc.setCameraEntity(jogador);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                previa = -1;
            }
            return;
        }
        if (TRAJE_ICHIGO) {
            mc.gui.getChat().clearMessages(false);
            // Shikai de frente (um Getsuga), corrida de costas, 1ª pessoa com um ataque; depois o Bankai: a
            // entrada de frente, corrida de costas (o casaco), de lado girando, e 1ª pessoa.
            var tipo = net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
            if (t >= 45 && t < 85 || t >= 170 && t < 240) tipo = net.minecraft.client.CameraType.THIRD_PERSON_BACK;
            boolean primeira = t >= 85 && t < 115 || t >= 260 && t < 290;
            if (primeira) tipo = net.minecraft.client.CameraType.FIRST_PERSON;
            mc.options.setCameraType(tipo);
            jogador.setXRot(primeira ? 10 : 8);
            if (t == 2 || t == 118) jogador.connection.sendCommand("rwk recarregar");
            if (t == 1) {
                for (int espaco = 0; espaco < 9; espaco++) {
                    if (jogador.getInventory().getItem(espaco).isEmpty()) {
                        jogador.getInventory().selected = espaco;
                        jogador.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(espaco));
                        break;
                    }
                }
            }
            if (t == 15) tecla(com.minerwk.core.kit.Tecla.Z, true);
            if (t == 27) tecla(com.minerwk.core.kit.Tecla.Z, false);
            if (t == 95 || t == 270) jogador.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (t == 120) tecla(com.minerwk.core.kit.Tecla.G, true);
            if (t == 122) tecla(com.minerwk.core.kit.Tecla.G, false);
            boolean corre = t >= 50 && t < 82 || t >= 175 && t < 215;
            if (corre) {
                Vec3 frente = new Vec3(jogador.getLookAngle().x, 0, jogador.getLookAngle().z).normalize();
                jogador.setDeltaMovement(frente.scale(.28).add(0, jogador.getDeltaMovement().y, 0));
            }
            // Um giro parado, visto de costas e depois de frente: o casaco e a corrente acompanham.
            if (t >= 220 && t < 256) jogador.setYRot(jogador.getYRot() + 9);
            if (t == FIM_PREVIA - 3) jogador.connection.sendCommand("tp @s ~-150 ~ ~-10");
            if (t >= FIM_PREVIA) {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                previa = -1;
            }
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

    /**
     * A luta inteira contra o Zangetsu interior. Os tempos contam de quando ele aparece, porque a arena do
     * mundo interior leva um tempo para ficar pronta.
     */
    private static void previaDoZangetsu(Minecraft mc, LocalPlayer jogador, int t) {
        if (t == 0) {
            jogador.connection.sendCommand("gamerule sendCommandFeedback false");
            jogador.connection.sendCommand("gamemode survival");
        }
        if (t <= 2) mc.gui.getChat().clearMessages(false);
        if (t == 2) jogador.connection.sendCommand("rwk zangetsu @s");
        net.minecraft.world.entity.Entity zangetsu = null;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof com.minerwk.boss.ZangetsuEntity) zangetsu = e;
        }
        if (zangetsuEm < 0) {
            if (zangetsu == null) return;
            zangetsuEm = t;
        }
        int z = t - zangetsuEm;
        // Até a queda, o jogador não pode cair por um golpe fora do roteiro.
        if (z < 250 && z % 10 == 0) vidaDoJogador(20);
        if (z == 60) golpeDoZangetsu(com.minerwk.core.boss.ZangetsuBoss.GETSUGA);
        if (z == 110) golpeDoZangetsu(com.minerwk.core.boss.ZangetsuBoss.ARREMESSO);
        if (z == 160) golpeDoZangetsu(com.minerwk.core.boss.ZangetsuBoss.CERO);
        if (z == 215) golpeDoZangetsu(com.minerwk.core.boss.ZangetsuBoss.SHUNPO);
        // O golpe que mataria: o jogador cai e se levanta de máscara.
        if (z == 262) golpeDoZangetsu(com.minerwk.core.boss.ZangetsuBoss.GETSUGA);
        if (z >= 270 && z < 300 && z % 2 == 0 && !com.minerwk.client.camera.CutsceneCliente.ativa()) vidaDoJogador(1);
        if (z >= 380 && z % 10 == 0) vidaDoJogador(20);
        // Abaixo de 40% da vida ele veste a máscara e o Getsuga vem em três.
        if (z == 400) jogador.connection.sendCommand("damage @e[type=minerwk:zangetsu,limit=1,sort=nearest] 170");
        if (z == 460) golpeDoZangetsu(com.minerwk.core.boss.ZangetsuBoss.GETSUGA_3);
        if (z == 540) jogador.connection.sendCommand("kill @e[type=minerwk:zangetsu]");
        boolean deFora = (z >= 55 && z < 262) || z >= 440;
        if (!com.minerwk.client.camera.CutsceneCliente.ativa()) {
            mc.options.setCameraType(deFora ? net.minecraft.client.CameraType.THIRD_PERSON_BACK
                    : net.minecraft.client.CameraType.FIRST_PERSON);
            if (zangetsu != null && !zangetsu.isInvisible()) {
                jogador.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, zangetsu.position().add(0, 1.2, 0));
                // De fora, um pouco de lado: senão o jogador tapa o Zangetsu.
                if (deFora) jogador.setYRot(jogador.getYRot() + 28);
            }
        }
        // A vitória devolve o jogador ao mundo; depois o mundo de testes volta a ficar sem o despertar da demonstração.
        int volta = 540 + com.minerwk.arena.ArenasServidor.TICKS_DEPOIS_DA_VITORIA + 30;
        if (z == volta) {
            jogador.connection.sendCommand("rwk despertar @s nao");
            jogador.connection.sendCommand("rwk maestria @s zerar");
            jogador.connection.sendCommand("gamerule sendCommandFeedback true");
            jogador.connection.sendCommand("gamemode creative");
        }
        if (z >= volta + 10) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            fimDaDemo = true;
        }
    }

    private static void golpeDoZangetsu(com.minerwk.core.boss.Golpe golpe) {
        net.minecraft.server.MinecraftServer servidor = Minecraft.getInstance().getSingleplayerServer();
        if (servidor == null) return;
        servidor.execute(() -> {
            for (net.minecraft.server.level.ServerLevel nivel : servidor.getAllLevels()) {
                for (net.minecraft.world.entity.Entity e : nivel.getAllEntities()) {
                    if (e instanceof com.minerwk.boss.ZangetsuEntity zangetsu) zangetsu.forcar(golpe);
                }
            }
        });
    }

    private static void vidaDoJogador(float vida) {
        net.minecraft.server.MinecraftServer servidor = Minecraft.getInstance().getSingleplayerServer();
        if (servidor == null) return;
        servidor.execute(() -> servidor.getPlayerList().getPlayers().forEach(p -> {
            if (p.isAlive()) p.setHealth(Math.min(vida, p.getMaxHealth()));
        }));
    }

    /** Manda o Toji da demonstração começar um golpe (o servidor é o do próprio jogo). */
    private static void golpeDoToji(com.minerwk.core.boss.Golpe golpe) {
        net.minecraft.server.MinecraftServer servidor = Minecraft.getInstance().getSingleplayerServer();
        if (servidor == null) return;
        servidor.execute(() -> {
            for (net.minecraft.server.level.ServerLevel nivel : servidor.getAllLevels()) {
                for (net.minecraft.world.entity.Entity e : nivel.getAllEntities()) {
                    if (e instanceof com.minerwk.boss.TojiEntity toji) toji.forcar(golpe);
                }
            }
        });
    }

    /** Enche a vida de quem joga a demonstração, para a gravação não acabar numa tela de morte. */
    private static void curarJogador() {
        net.minecraft.server.MinecraftServer servidor = Minecraft.getInstance().getSingleplayerServer();
        if (servidor == null) return;
        servidor.execute(() -> servidor.getPlayerList().getPlayers().forEach(p -> {
            if (p.isAlive()) p.setHealth(p.getMaxHealth());
        }));
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
