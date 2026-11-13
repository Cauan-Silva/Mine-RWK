package com.minerwk.kit;

import com.minerwk.ConfigServidor;
import com.minerwk.MineRWK;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.kit.Poder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A camada de poder do spec "Balanceamento": vida, redução de dano e o multiplicador dos golpes. */
@GameTestHolder(MineRWK.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestesPoder {
    private TestesPoder() {
    }

    private static Poder poder() {
        return ConfigServidor.poder();
    }

    /** O Gojo dos testes de mecânica fica sem a camada; aqui ela é o que se testa. */
    private static ServerPlayer comCamada(ServerPlayer jogador) {
        PoderServidor.comCamada(jogador);
        return jogador;
    }

    private static boolean perto(double a, double b) {
        return Math.abs(a - b) < 1e-3;
    }

    /** O Gojo dos testes, com a vida cheia no máximo novo e sem o Infinito (que bloquearia o golpe do Warden). */
    private static ServerPlayer gojo(GameTestHelper helper) {
        // Com conexão de mentira: levar dano manda pacotes ao jogador.
        ServerPlayer jogador = comCamada(com.minerwk.arena.TestesArena.jogador(helper));
        PoderServidor.aplicarVida(jogador);
        jogador.setHealth(jogador.getMaxHealth());
        return jogador;
    }

    @GameTest(template = "vazio")
    public static void golpeDoKitTiraTresVezes(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        zumbi.setNoAi(true);
        zumbi.getAttribute(Attributes.ARMOR).setBaseValue(0);
        zumbi.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
        zumbi.setHealth(40);
        var c = zumbi.getBoundingBox().getCenter();
        new ContextoServidor(jogador).danoEm(new Vetor(c.x, c.y, c.z), 1, 4);
        helper.assertTrue(perto(zumbi.getHealth(), 40 - poder().golpe(4)),
                "o golpe de 4 deveria tirar " + poder().golpe(4) + ", sobrou " + zumbi.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void personagemTemVidaExtraESemPersonagemVolta(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        helper.assertTrue(perto(jogador.getMaxHealth(), 20 + poder().vidaExtra()), "com personagem: " + jogador.getMaxHealth());
        ServicoKits.dados(jogador).personagem = "";
        PoderServidor.aplicarVida(jogador);
        helper.assertTrue(perto(jogador.getMaxHealth(), 20), "sem personagem: " + jogador.getMaxHealth());
        helper.assertTrue(jogador.getHealth() <= 20, "e a vida atual cabe no máximo novo: " + jogador.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void renascidoMantemAVidaExtra(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        ServerPlayer novo = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), jogador.getGameProfile(),
                ClientInformation.createDefault());
        novo.restoreFrom(jogador, false);
        PoderServidor.aoRenascer(novo);
        helper.assertTrue(perto(novo.getMaxHealth(), 20 + poder().vidaExtra()), "renascido: " + novo.getMaxHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void golpeDoWardenChegaPelaMetade(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Warden warden = helper.spawn(EntityType.WARDEN, new BlockPos(6, 2, 1));
        warden.setNoAi(true);
        float antes = jogador.getHealth();
        jogador.hurt(warden.damageSources().mobAttack(warden), 30);
        helper.assertTrue(perto(antes - jogador.getHealth(), poder().recebido(30)),
                "o golpe de 30 deveria tirar " + poder().recebido(30) + ", tirou " + (antes - jogador.getHealth()));
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void semPersonagemApanhaNormal(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        ServicoKits.dados(jogador).personagem = "";
        PoderServidor.aplicarVida(jogador);
        jogador.setHealth(20);
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        zumbi.setNoAi(true);
        jogador.hurt(zumbi.damageSources().mobAttack(zumbi), 6);
        helper.assertTrue(perto(jogador.getHealth(), 14), "sem personagem o golpe chega inteiro: " + jogador.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void killIgnoraARedução(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        jogador.kill();
        helper.assertTrue(jogador.isDeadOrDying(), "/kill continua matando");
        helper.succeed();
    }
}
