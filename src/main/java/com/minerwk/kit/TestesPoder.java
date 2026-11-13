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

    // --- Os bosses ---

    private static void vidaDobrada(GameTestHelper helper, net.minecraft.world.entity.Mob boss, ServerPlayer dono,
                                    java.util.function.Consumer<ServerPlayer> comecar, float vida) {
        boss.moveTo(dono.getX() + 6, dono.getY(), dono.getZ());
        helper.getLevel().addFreshEntity(boss);
        comecar.accept(dono);
        helper.assertTrue(perto(boss.getMaxHealth(), poder().vidaDoBoss(vida)),
                boss.getName().getString() + ": vida " + boss.getMaxHealth() + " em vez de " + poder().vidaDoBoss(vida));
        helper.assertTrue(perto(boss.getHealth(), boss.getMaxHealth()), "e começa com ela cheia");
        // Começar a luta de novo (o boss salvo no disco é carregado com a vida já multiplicada) não multiplica outra vez.
        comecar.accept(dono);
        helper.assertTrue(perto(boss.getMaxHealth(), poder().vidaDoBoss(vida)), "uma vez só");
        boss.discard();
    }

    @GameTest(template = "vazio")
    public static void bossesNascemComAVidaMultiplicada(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        var toji = com.minerwk.registro.Entidades.TOJI.get().create(helper.getLevel());
        vidaDobrada(helper, toji, jogador, toji::comecarLuta, com.minerwk.core.boss.TojiBoss.VIDA);
        var zangetsu = com.minerwk.registro.Entidades.ZANGETSU.get().create(helper.getLevel());
        vidaDobrada(helper, zangetsu, jogador, zangetsu::comecarLuta, com.minerwk.core.boss.ZangetsuBoss.VIDA);
        var ragnarok = com.minerwk.registro.Entidades.RAGNAROK.get().create(helper.getLevel());
        vidaDobrada(helper, ragnarok, jogador, ragnarok::comecarLuta, com.minerwk.core.boss.RagnarokBoss.VIDA);
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void golpeDoBossChegaMultiplicadoEReduzido(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        var toji = com.minerwk.registro.Entidades.TOJI.get().create(helper.getLevel());
        toji.moveTo(jogador.getX() + 2, jogador.getY(), jogador.getZ());
        helper.getLevel().addFreshEntity(toji);
        toji.comecarLuta(jogador);
        float antes = jogador.getHealth();
        toji.ferirParaTeste(jogador, 6);
        float esperado = poder().recebido(poder().danoDoBoss(6));
        helper.assertTrue(perto(antes - jogador.getHealth(), esperado),
                "o soco de 6 deveria tirar " + esperado + ", tirou " + (antes - jogador.getHealth()));
        toji.discard();
        helper.succeed();
    }

    // --- Achados da revisão final ---

    @GameTest(template = "vazio")
    public static void sairEEntrarMantemAVida(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        jogador.setHealth(38);
        // Sair e entrar no mundo: o jogador é salvo e carregado de novo.
        net.minecraft.nbt.CompoundTag salvo = new net.minecraft.nbt.CompoundTag();
        jogador.saveWithoutId(salvo);
        ServerPlayer carregado = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), jogador.getGameProfile(),
                ClientInformation.createDefault());
        carregado.load(salvo);
        PoderServidor.aplicarVida(carregado);
        helper.assertTrue(perto(carregado.getMaxHealth(), 20 + poder().vidaExtra()), "máximo depois de entrar: " + carregado.getMaxHealth());
        helper.assertTrue(perto(carregado.getHealth(), 38), "a vida de quando saiu: " + carregado.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void socoDoTojiNaoDerrubaQuemSobrevive(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        ServicoKits.definirDespertou(jogador, false);
        var toji = com.minerwk.registro.Entidades.TOJI.get().create(helper.getLevel());
        toji.moveTo(jogador.getX() + 2, jogador.getY(), jogador.getZ());
        helper.getLevel().addFreshEntity(toji);
        toji.comecarLuta(jogador);
        float chega = poder().recebido(poder().danoDoBoss(6));
        jogador.setHealth(chega + 2);
        toji.ferirParaTeste(jogador, 6);
        helper.assertFalse(com.minerwk.boss.LutasToji.caido(jogador), "o soco não mataria: não é hora do despertar");
        helper.assertTrue(perto(jogador.getHealth(), 2), "o soco chega reduzido, vida " + jogador.getHealth());
        toji.discard();
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void trajeDoCidContinuaComOGolpeForteDeAntes(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        ServicoKits.definirPersonagem(jogador, com.minerwk.core.kit.Personagens.porId(com.minerwk.core.personagens.cid.Cid.ID).orElseThrow());
        PoderServidor.aplicarVida(jogador);
        jogador.setHealth(jogador.getMaxHealth());
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        zumbi.setNoAi(true);
        float antes = jogador.getHealth();
        // Um golpe de 10 era forte antes da camada (8 ou mais): o traje continua cortando.
        jogador.hurt(zumbi.damageSources().mobAttack(zumbi), 10);
        var kit = ServicoKits.kit(jogador).orElseThrow();
        helper.assertFalse(kit.recargas().pronta(com.minerwk.core.personagens.cid.Cid.TRAJE, helper.getLevel().getGameTime()),
                "o traje de slime deveria ter cortado o golpe de 10");
        helper.assertTrue(perto(antes - jogador.getHealth(), poder().recebido(5)), "metade do traje e a redução: tirou " + (antes - jogador.getHealth()));
        helper.succeed();
    }
}
