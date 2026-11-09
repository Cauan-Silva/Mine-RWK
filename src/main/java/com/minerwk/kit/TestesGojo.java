package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.Gojo;
import com.minerwk.registro.Efeitos;
import com.minerwk.registro.Blocos;
import com.minerwk.registro.Regras;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.level.GameType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests do Gojo no servidor (rodam com {@code ./gradlew runGameTestServer}). */
@GameTestHolder(MineRWK.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestesGojo {
    private TestesGojo() {
    }

    private static ServerPlayer gojo(GameTestHelper helper) {
        // Jogador real sem conexão, no sobrevivência: o falso do GameTest é criativo e ignoraria o dano.
        ServerPlayer jogador = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "gojo_teste"), ClientInformation.createDefault());
        CompoundTag modo = new CompoundTag();
        modo.putInt("playerGameType", GameType.SURVIVAL.getId());
        jogador.loadGameTypes(modo);
        // Recém-nascido fica 60 ticks imune a dano; sem zerar, os testes de dano passariam sem testar nada.
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, jogador, 0, "spawnInvulnerableTime");
        jogador.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Gojo.ID).orElseThrow());
        return jogador;
    }

    private static Vetor vetor(Vec3 v) {
        return new Vetor(v.x, v.y, v.z);
    }

    @GameTest(template = "vazio")
    public static void gojoEscolhivelEAzulCriaEfeito(GameTestHelper helper) {
        helper.assertTrue(Personagens.escolhiveis().stream().anyMatch(p -> p.id().equals(Gojo.ID)), "Gojo deveria ser escolhível");
        ServerPlayer jogador = gojo(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        ServicoKits.usarTecla(jogador, Tecla.Z, true);
        for (int i = 0; i < 8; i++) ServicoKits.tickJogador(jogador);
        helper.assertTrue(kit.efeitos().size() == 1, "o Azul deveria deixar um efeito, há " + kit.efeitos().size());
        // 200 − 20 do Azul, mais o que encheu nos ticks depois do disparo.
        helper.assertTrue(kit.recurso().atual() >= 180 && kit.recurso().atual() < 190, "energia deveria ser 180–190, é " + kit.recurso().atual());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void azulPuxaZumbi(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1));
        Vec3 centro = helper.absoluteVec(new Vec3(1.5, 2.5, 1.5));
        new ContextoServidor(jogador).puxar(vetor(centro), 5, 0.25);
        helper.assertTrue(zumbi.getDeltaMovement().x < -0.2, "o zumbi deveria ir para o centro, vel " + zumbi.getDeltaMovement());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void roxoApagaBlocosSoComARegra(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        var regra = helper.getLevel().getGameRules().getRule(Regras.APAGAR_BLOCOS);
        BlockPos pedra = new BlockPos(3, 2, 3);
        BlockPos bedrock = new BlockPos(3, 3, 3);
        Vetor centro = vetor(helper.absoluteVec(new Vec3(3.5, 2.5, 3.5)));
        ContextoServidor ctx = new ContextoServidor(jogador);
        try {
            regra.set(false, helper.getLevel().getServer());
            helper.setBlock(pedra, Blocks.STONE);
            helper.setBlock(bedrock, Blocks.BEDROCK);
            ctx.apagarBlocos(centro, 1.5);
            helper.assertBlockPresent(Blocks.STONE, pedra);

            regra.set(true, helper.getLevel().getServer());
            ctx.apagarBlocos(centro, 1.5);
            helper.assertBlockPresent(Blocks.AIR, pedra);
            helper.assertBlockPresent(Blocks.BEDROCK, bedrock);
        } finally {
            regra.set(true, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void vermelhoCongelaEArremessaZumbi(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1));
        Vec3 inicio = zumbi.position();
        Vetor centro = vetor(helper.absoluteVec(new Vec3(2.5, 2.5, 1.5)));
        new ContextoServidor(jogador).arremessar(centro, 4, 3, 3, null);
        helper.assertTrue(ArremessosServidor.emArremesso(zumbi), "o zumbi deveria estar sendo arremessado");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(zumbi.position().distanceTo(inicio) < 0.3, "congelado não anda: " + zumbi.position());
            helper.assertFalse(zumbi.isNoAi() || zumbi.isNoGravity(), "congelar não pode gravar NoAI/NoGravity no mob");
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(zumbi.position().x - inicio.x > 2, "deveria ter voado para longe: " + zumbi.position());
            helper.assertFalse(zumbi.isNoAi(), "a IA volta depois do lançamento");
            helper.succeed();
        });
    }

    @GameTest(template = "vazio")
    public static void azulArrancaTerraMasNaoPedra(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        var regra = helper.getLevel().getGameRules().getRule(Regras.APAGAR_BLOCOS);
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) helper.setBlock(new BlockPos(x, 1, z), (x + z) % 2 == 0 ? Blocks.DIRT : Blocks.STONE);
        }
        Vetor centro = vetor(helper.absoluteVec(new Vec3(3.5, 2.5, 3.5)));
        ContextoServidor ctx = new ContextoServidor(jogador);
        try {
            regra.set(false, helper.getLevel().getServer());
            for (int i = 0; i < 40; i++) ctx.arrancarBlocos(centro, 2, 2, .6f);
            helper.assertBlockPresent(Blocks.DIRT, new BlockPos(3, 1, 3));
            regra.set(true, helper.getLevel().getServer());
            int total = 0;
            for (int i = 0; i < 40; i++) total += ctx.arrancarBlocos(centro, 2, 2, .6f);
            helper.assertTrue(total > 0, "deveria ter arrancado alguma terra");
            for (int x = 2; x <= 4; x++) {
                for (int z = 2; z <= 4; z++) {
                    if ((x + z) % 2 != 0) helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 1, z));
                }
            }
        } finally {
            regra.set(true, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void dominioPoeChaoNoArQueSomeSozinho(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        BlockPos noAr = new BlockPos(2, 4, 2);
        Vetor pes = vetor(helper.absoluteVec(new Vec3(2.5, 5, 2.5)));
        new ContextoServidor(jogador).pisoSolido(pes, 1.5, 5);
        helper.assertBlockPresent(Blocos.pisoDominio(), noAr);
        helper.runAfterDelay(8, () -> {
            helper.assertBlockPresent(Blocks.AIR, noAr);
            helper.succeed();
        });
    }

    @GameTest(template = "vazio")
    public static void dominioParalisaZumbi(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        new ContextoServidor(jogador).paralisar(vetor(jogador.position()), 12, 40);
        helper.assertTrue(zumbi.hasEffect(Efeitos.PARALISIA), "o zumbi deveria estar paralisado");
        helper.assertTrue(zumbi.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "velocidade deveria ser 0");
        helper.assertFalse(jogador.hasEffect(Efeitos.PARALISIA), "o Gojo não se paralisa");
        float vida = jogador.getHealth();
        jogador.hurt(helper.getLevel().damageSources().mobAttack(zumbi), 4);
        helper.assertTrue(jogador.getHealth() == vida, "zumbi paralisado não deveria causar dano");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void infinitoBarraFlecha(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Zombie atirador = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        Arrow flecha = new Arrow(EntityType.ARROW, helper.getLevel());
        flecha.setOwner(atirador);
        float vida = jogador.getHealth();
        new ContextoServidor(jogador).infinito(20);
        jogador.hurt(helper.getLevel().damageSources().arrow(flecha, atirador), 5);
        helper.assertTrue(jogador.getHealth() == vida, "com Infinito a flecha não deveria ferir");
        jogador.hurt(helper.getLevel().damageSources().fall(), 2);
        helper.assertTrue(jogador.getHealth() < vida, "queda continua ferindo");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void golpesNaoAtingemOLoboDoGojo(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Wolf lobo = helper.spawn(EntityType.WOLF, new BlockPos(2, 2, 1));
        lobo.tame(jogador);
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
        float vidaLobo = lobo.getHealth();
        ContextoServidor ctx = new ContextoServidor(jogador);
        ctx.danoEm(vetor(jogador.position()), 5, 4);
        ctx.paralisar(vetor(jogador.position()), 5, 20);
        helper.assertTrue(lobo.getHealth() == vidaLobo, "o lobo do Gojo não deveria tomar dano");
        helper.assertFalse(lobo.hasEffect(Efeitos.PARALISIA), "o lobo do Gojo não deveria ser paralisado");
        helper.assertTrue(zumbi.hasEffect(Efeitos.PARALISIA), "o zumbi sim");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void paralisadoAindaSeFereSozinho(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        new ContextoServidor(jogador).paralisar(vetor(jogador.position()), 12, 40);
        float vida = zumbi.getHealth();
        zumbi.hurt(helper.getLevel().damageSources().mobAttack(zumbi), 3);
        helper.assertTrue(zumbi.getHealth() < vida, "dano causado em si mesmo não deveria ser cancelado");
        helper.succeed();
    }
}
