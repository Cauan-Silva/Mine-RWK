package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.Forma;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.personagens.ichigo.Ichigo;
import com.minerwk.core.personagens.ichigo.Reiatsu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests do Ichigo no servidor (rodam com {@code ./gradlew runGameTestServer}). */
@GameTestHolder(MineRWK.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestesIchigo {
    private TestesIchigo() {
    }

    /** Jogador de sobrevivência com o kit do Ichigo, olhando para +x. */
    private static ServerPlayer ichigo(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Ichigo.ID).orElseThrow());
        jogador.setYRot(-90);
        jogador.setXRot(0);
        return jogador;
    }

    /** Zumbi parado e sem armadura, para o dano sair exato. */
    private static Zombie zumbi(GameTestHelper helper, int x, int z) {
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(x, 2, z));
        zumbi.setNoAi(true);
        zumbi.getAttribute(Attributes.ARMOR).setBaseValue(0);
        return zumbi;
    }

    private static void ticks(ServerPlayer jogador, int n) {
        for (int i = 0; i < n; i++) ServicoKits.tickJogador(jogador);
    }

    private static boolean perto(double a, double b) {
        return Math.abs(a - b) < 1e-3;
    }

    @GameTest(template = "vazio")
    public static void ichigoEntraNoBankaiEOReiatsuAcabaComEle(GameTestHelper helper) {
        helper.assertTrue(Personagens.escolhiveis().stream().anyMatch(p -> p.id().equals(Ichigo.ID)), "Ichigo deveria ser escolhível");
        ServerPlayer jogador = ichigo(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        ticks(jogador, 1);
        helper.assertTrue(Ichigo.ZANGETSU.equals(ArmaServidor.atual(jogador)), "no Shikai a arma é a Zangetsu");
        double velocidade = jogador.getAttributeValue(Attributes.ATTACK_SPEED);
        helper.assertTrue(perto(velocidade, Ichigo.ZANGETSU.velocidade()), "velocidade de ataque " + velocidade);
        ServicoKits.usarTecla(jogador, Tecla.G, true);
        ticks(jogador, 45);
        helper.assertTrue(Reiatsu.BANKAI.equals(kit.forma()), "o G deveria levar ao Bankai, forma " + kit.forma());
        helper.assertTrue(Ichigo.TENSA_ZANGETSU.equals(ArmaServidor.atual(jogador)), "no Bankai a arma é a Tensa Zangetsu");
        velocidade = jogador.getAttributeValue(Attributes.ATTACK_SPEED);
        helper.assertTrue(perto(velocidade, Ichigo.TENSA_ZANGETSU.velocidade()), "velocidade de ataque no Bankai " + velocidade);
        kit.recurso().carregar(0.05);
        ticks(jogador, 3);
        helper.assertTrue(Forma.BASE.equals(kit.forma()), "sem reiatsu o Bankai acaba");
        long faltam = kit.recargas().restante(Ichigo.ENTRAR_NO_BANKAI, jogador.level().getGameTime());
        helper.assertTrue(faltam > 150, "o G deveria ficar em recarga, faltam " + faltam);
        // Outro personagem: a arma do kit some.
        ServicoKits.definirPersonagem(jogador, Personagens.porId(com.minerwk.core.personagens.gojo.Gojo.ID).orElseThrow());
        ticks(jogador, 1);
        helper.assertTrue(ArmaServidor.atual(jogador) == null, "o Gojo não tem arma do kit");
        helper.assertTrue(perto(jogador.getAttributeValue(Attributes.ATTACK_SPEED), 4), "a velocidade de ataque volta ao normal");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void getsugaAtravessaDoisZumbis(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        Zombie perto = zumbi(helper, 4, 1);
        Zombie longe = zumbi(helper, 7, 1);
        ServicoKits.usarTecla(jogador, Tecla.Z, true);
        ticks(jogador, 30);
        // Carga cheia: 12 de dano em cada um. O ataque básico (7) não se mete no dano do golpe.
        helper.assertTrue(perto(perto.getHealth(), 8), "o zumbi de perto deveria ficar com 8, ficou com " + perto.getHealth());
        helper.assertTrue(perto(longe.getHealth(), 8), "o de trás também é atingido, ficou com " + longe.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void ataqueBasicoUsaODanoDaZangetsu(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        Zombie alvo = zumbi(helper, 3, 1);
        ticks(jogador, 1);
        // Um soco de mão vazia com a recarga cheia chega valendo 1 (o ataque do jogador): vira o da espada.
        alvo.hurt(jogador.damageSources().playerAttack(jogador), 1f);
        helper.assertTrue(perto(alvo.getHealth(), 20 - Ichigo.ZANGETSU.dano()), "deveria tirar 7, vida " + alvo.getHealth());
        Zombie outro = zumbi(helper, 3, 3);
        outro.hurt(jogador.damageSources().playerAttack(jogador), .5f);
        helper.assertTrue(perto(outro.getHealth(), 20 - Ichigo.ZANGETSU.dano() / 2), "golpe apressado vale a metade, vida " + outro.getHealth());
        ServerPlayer gojo = TestesGojo.gojo(helper);
        Zombie terceiro = zumbi(helper, 5, 3);
        ServicoKits.tickJogador(gojo);
        terceiro.hurt(gojo.damageSources().playerAttack(gojo), 1f);
        helper.assertTrue(perto(terceiro.getHealth(), 19), "o soco do Gojo não muda, vida " + terceiro.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void getsugaNoChaoDesfazFlechaDosOutros(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        Zombie atirador = zumbi(helper, 4, 1);
        Arrow flecha = new Arrow(EntityType.ARROW, helper.getLevel());
        flecha.setOwner(atirador);
        flecha.setPos(jogador.position().add(2, 1, 0));
        flecha.setNoGravity(true);
        helper.getLevel().addFreshEntity(flecha);
        Arrow minha = new Arrow(EntityType.ARROW, helper.getLevel());
        minha.setOwner(jogador);
        minha.setPos(jogador.position().add(1, 1, 1));
        minha.setNoGravity(true);
        helper.getLevel().addFreshEntity(minha);
        ServicoKits.usarTecla(jogador, Tecla.C, true);
        ticks(jogador, 10);
        helper.assertTrue(flecha.isRemoved(), "a flecha do zumbi deveria sumir");
        helper.assertTrue(!minha.isRemoved(), "a flecha do próprio Ichigo fica");
        helper.assertTrue(perto(atirador.getHealth(), 14), "o zumbi leva 6, vida " + atirador.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void shunpoParaAntesDaParede(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        // O molde tem barreira na altura 2: em cima dela o jogador começa livre, como num chão de verdade.
        jogador.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 3, 1.5)));
        for (int y = 3; y <= 6; y++) {
            for (int z = 0; z <= 3; z++) helper.setBlock(new BlockPos(6, y, z), Blocks.STONE);
        }
        helper.assertTrue(helper.getLevel().noCollision(jogador, jogador.getBoundingBox()), "o jogador deveria começar em lugar livre");
        double x0 = jogador.getX();
        double y0 = jogador.getY();
        double parede = helper.absolutePos(new BlockPos(6, 2, 1)).getX();
        ServicoKits.usarTecla(jogador, Tecla.X, true);
        ticks(jogador, 3);
        double andou = jogador.getX() - x0;
        helper.assertTrue(andou > 3 && jogador.getX() < parede - .3 + 1e-6, "deveria parar encostado na parede, andou " + andou);
        helper.assertTrue(perto(jogador.getY(), y0), "sem subir nem descer: " + y0 + " -> " + jogador.getY());
        helper.assertTrue(perto(jogador.getYRot(), -90), "continua olhando para a frente");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void shunpoDoBankaiSurgeAtrasDoZumbiOlhandoParaEle(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        Zombie alvo = zumbi(helper, 5, 1);
        kit.mudarForma(Reiatsu.BANKAI, new ContextoServidor(jogador));
        ServicoKits.usarTecla(jogador, Tecla.X, true);
        ticks(jogador, 3);
        helper.assertTrue(jogador.getX() > alvo.getX() + 1, "deveria passar do zumbi: " + jogador.getX() + " x " + alvo.getX());
        // Olhando de volta para ele (−x): giro perto de 90.
        float giro = net.minecraft.util.Mth.wrapDegrees(jogador.getYRot());
        helper.assertTrue(Math.abs(giro - 90) < 15, "deveria encarar o zumbi, giro " + giro);
        helper.succeed();
    }
}
