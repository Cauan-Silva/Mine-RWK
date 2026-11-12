package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.personagens.cid.Cid;
import com.minerwk.registro.Efeitos;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests do Cid no servidor (rodam com {@code ./gradlew runGameTestServer}). */
@GameTestHolder(MineRWK.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestesCid {
    private TestesCid() {
    }

    /** Jogador de sobrevivência com o kit do Cid, olhando para +x. */
    private static ServerPlayer cid(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Cid.ID).orElseThrow());
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
    public static void cidEEscolhivelETemAEspadaDeSlime(GameTestHelper helper) {
        helper.assertTrue(Personagens.escolhiveis().stream().anyMatch(p -> p.id().equals(Cid.ID)), "Cid deveria ser escolhível");
        ServerPlayer jogador = cid(helper);
        ticks(jogador, 1);
        helper.assertTrue(Cid.ESPADA.equals(ArmaServidor.atual(jogador)), "a arma é a espada de slime");
        Zombie alvo = zumbi(helper, 3, 1);
        alvo.hurt(jogador.damageSources().playerAttack(jogador), 1f);
        helper.assertTrue(perto(alvo.getHealth(), 15), "o ataque básico tira 5, vida " + alvo.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void redemoinhoAcertaOsTresCortesEmQuemEstaEmVolta(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        Zombie frente = zumbi(helper, 4, 1);
        Zombie atras = zumbi(helper, 1, 4);
        Zombie longe = zumbi(helper, 8, 1);
        ServicoKits.usarTecla(jogador, Tecla.Z, true);
        ticks(jogador, 30);
        // Três cortes de 4 em 12 ticks: o corte anterior não pode proteger do seguinte.
        helper.assertTrue(perto(frente.getHealth(), 8), "o da frente leva os três cortes, vida " + frente.getHealth());
        helper.assertTrue(perto(atras.getHealth(), 8), "o de trás também, vida " + atras.getHealth());
        helper.assertTrue(perto(longe.getHealth(), 20), "fora do raio de 5 não leva nada, vida " + longe.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void posImagemLevaParaAFrenteParaNaParedeEEnganaOZumbi(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 4));
        zumbi.setTarget(jogador);
        for (int y = 2; y <= 4; y++) helper.setBlock(new BlockPos(6, y, 1), Blocks.STONE);
        double antes = jogador.getX();
        ServicoKits.usarTecla(jogador, Tecla.X, true);
        ticks(jogador, 6);
        double andou = jogador.getX() - antes;
        helper.assertTrue(andou > 2 && andou < 5, "deveria parar antes da parede, andou " + andou);
        helper.assertTrue(ChamarizServidor.enganados(jogador) == 1, "o zumbi que o mirava deveria ser enganado");
        helper.assertTrue(zumbi.getTarget() == null, "o zumbi larga o Cid");
        // O zumbi volta a mirar e o chamariz o engana de novo enquanto dura.
        zumbi.setTarget(jogador);
        ChamarizServidor.tick(helper.getLevel().getServer());
        helper.assertTrue(zumbi.getTarget() == null, "durante a imagem ele não volta ao Cid");
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(ChamarizServidor.enganados(jogador) == 0, "depois de 2 s a imagem some");
            helper.succeed();
        });
    }

    @GameTest(template = "vazio")
    public static void prisaoFereOPrimeiroEOPrendeUmSegundoDepois(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        Zombie primeiro = zumbi(helper, 5, 1);
        Zombie segundo = zumbi(helper, 7, 1);
        ServicoKits.usarTecla(jogador, Tecla.C, true);
        ticks(jogador, 12);
        helper.assertTrue(perto(primeiro.getHealth(), 14), "o primeiro leva 6, vida " + primeiro.getHealth());
        helper.assertTrue(perto(segundo.getHealth(), 20), "o slime não atravessa, vida " + segundo.getHealth());
        helper.assertTrue(!primeiro.hasEffect(Efeitos.PARALISIA), "as correntes ainda não fecharam");
        ticks(jogador, 20);
        helper.assertTrue(primeiro.hasEffect(Efeitos.PARALISIA), "1 s depois o alvo fica preso");
        helper.assertTrue(!segundo.hasEffect(Efeitos.PARALISIA), "só o alvo acertado é preso");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void correntesNaoPrendemQuemEstaColadoNoAlvo(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        Zombie alvo = zumbi(helper, 5, 1);
        ServicoKits.usarTecla(jogador, Tecla.C, true);
        ticks(jogador, 12);
        // Depois do acerto, outro zumbi nasce no mesmo bloco do alvo.
        Zombie colado = zumbi(helper, 5, 1);
        ticks(jogador, 20);
        helper.assertTrue(alvo.hasEffect(Efeitos.PARALISIA), "o alvo fica preso");
        helper.assertTrue(!colado.hasEffect(Efeitos.PARALISIA), "quem está colado nele não");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void chamarizNaoEnganaBoss(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        var wither = helper.spawn(EntityType.WITHER, new BlockPos(1, 2, 5));
        wither.setNoAi(true);
        helper.assertTrue(!ChamarizServidor.enganavel(wither), "o Wither não cai na imagem");
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        helper.assertTrue(ChamarizServidor.enganavel(zumbi), "um zumbi cai");
        wither.discard();
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void overdriveDaVelocidadeMaisDanoERecuoNoFim(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        ServicoKits.usarTecla(jogador, Tecla.G, true);
        ticks(jogador, 5);
        // A Velocidade II fica para o teste no jogo: o jogador do GameTest não tem conexão para receber o efeito.
        Zombie alvo = zumbi(helper, 3, 1);
        alvo.hurt(jogador.damageSources().playerAttack(jogador), 1f);
        helper.assertTrue(perto(alvo.getHealth(), 13.5), "o ataque básico tira 6,5 no Overdrive, vida " + alvo.getHealth());
        double magia = kit.recurso().atual();
        ticks(jogador, 100);
        helper.assertTrue(perto(kit.recurso().atual(), magia), "a Magia não volta no Overdrive");
        ticks(jogador, 70);
        helper.assertTrue(perto(jogador.getHealth(), 16), "levou até o fim: 4 de recuo, vida " + jogador.getHealth());
        long faltam = kit.recargas().restante(Cid.OVERDRIVE, jogador.level().getGameTime());
        helper.assertTrue(faltam > 400, "o G entra em recarga quando acaba, faltam " + faltam);
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void overdriveNaoMataComORecuoEDesligaSemCusto(GameTestHelper helper) {
        ServerPlayer fraco = cid(helper);
        fraco.setHealth(3);
        ServicoKits.usarTecla(fraco, Tecla.G, true);
        ticks(fraco, 170);
        helper.assertTrue(perto(fraco.getHealth(), 1), "o recuo deixa com 1, vida " + fraco.getHealth());
        ServerPlayer outro = cid(helper);
        ServicoKits.usarTecla(outro, Tecla.G, true);
        ticks(outro, 20);
        ServicoKits.usarTecla(outro, Tecla.G, false);
        ServicoKits.usarTecla(outro, Tecla.G, true);
        ticks(outro, 10);
        helper.assertTrue(perto(outro.getHealth(), 20), "quem desliga antes não paga recuo, vida " + outro.getHealth());
        ticks(outro, 200);
        helper.assertTrue(perto(outro.getHealth(), 20), "e o recuo não vem depois, vida " + outro.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void trajeDeSlimeCortaOGolpeForteUmaVezACadaDozeSegundos(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        ticks(jogador, 1);
        jogador.hurt(jogador.damageSources().cactus(), 6);
        helper.assertTrue(perto(jogador.getHealth(), 14), "golpe fraco passa inteiro, vida " + jogador.getHealth());
        helper.assertTrue(kit.recargas().pronta(Cid.TRAJE, jogador.level().getGameTime()), "e não gasta o traje");
        jogador.invulnerableTime = 0;
        jogador.hurt(jogador.damageSources().cactus(), 10);
        helper.assertTrue(perto(jogador.getHealth(), 9), "o golpe de 10 vem como 5, vida " + jogador.getHealth());
        jogador.invulnerableTime = 0;
        jogador.hurt(jogador.damageSources().cactus(), 8);
        helper.assertTrue(perto(jogador.getHealth(), 1), "o seguinte vem inteiro: o traje está em recarga, vida " + jogador.getHealth());
        long faltam = kit.recargas().restante(Cid.TRAJE, jogador.level().getGameTime());
        helper.assertTrue(faltam > 200 && faltam <= Cid.RECARGA_DO_TRAJE, "recarga de 12 s, faltam " + faltam);
        // Outro personagem não tem o traje.
        ServerPlayer gojo = TestesGojo.gojo(helper);
        gojo.hurt(gojo.damageSources().cactus(), 10);
        helper.assertTrue(perto(gojo.getHealth(), 10), "o Gojo leva o golpe inteiro, vida " + gojo.getHealth());
        helper.succeed();
    }
}
