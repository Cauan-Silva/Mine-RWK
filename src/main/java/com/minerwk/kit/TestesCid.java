package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.personagens.cid.AtomicFinal;
import com.minerwk.core.personagens.cid.Cid;
import com.minerwk.registro.Efeitos;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Wolf;
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

    /** O Cid que já venceu o Ragnarok: a Espada Atômica e o Recovery Atomic estão liberados. */
    private static ServerPlayer cidDesperto(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        ServicoKits.definirDespertou(jogador, true);
        return jogador;
    }

    @GameTest(template = "vazio")
    public static void espadaAtomicaSoDepoisDoDespertar(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        ServicoKits.usarTecla(jogador, Tecla.V, true);
        helper.assertTrue(kit.execucao().isEmpty(), "sem despertar o V não começa");
        jogador.setShiftKeyDown(true);
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        helper.assertTrue(kit.execucao().isEmpty(), "nem o Recovery Atomic");
        jogador.setShiftKeyDown(false);
        ServicoKits.definirDespertou(jogador, true);
        ServicoKits.usarTecla(jogador, Tecla.V, true);
        helper.assertTrue(kit.execucao().isPresent(), "desperto, o V começa");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void espadaAtomicaCortaOArcoAFrente(GameTestHelper helper) {
        ServerPlayer jogador = cidDesperto(helper);
        Zombie frente = zumbi(helper, 7, 1);
        Zombie diagonal = zumbi(helper, 4, 3);
        Zombie lado = zumbi(helper, 1, 5);
        ServicoKits.usarTecla(jogador, Tecla.V, true);
        ticks(jogador, 11);
        helper.assertTrue(perto(frente.getHealth(), 20), "12 ticks de carga, vida " + frente.getHealth());
        ticks(jogador, 4);
        helper.assertTrue(perto(frente.getHealth(), 4), "o da frente leva 16, vida " + frente.getHealth());
        helper.assertTrue(perto(diagonal.getHealth(), 4), "o da diagonal está no arco de 120 graus, vida " + diagonal.getHealth());
        helper.assertTrue(perto(lado.getHealth(), 20), "o do lado não, vida " + lado.getHealth());
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        long faltam = kit.recargas().restante(Cid.ESPADA_ATOMICA, jogador.level().getGameTime());
        helper.assertTrue(faltam > 250 && faltam <= 280, "recarga de 14 s, faltam " + faltam);
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void recoveryAtomicCuraOCidEOsAliadosENaoOsInimigos(GameTestHelper helper) {
        ServerPlayer jogador = cidDesperto(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        jogador.setHealth(5);
        // O outro jogador do teste não está no mundo (não tem conexão): quem prova a cura dos aliados é o lobo dele.
        ServerPlayer aliado = TestesGojo.gojo(helper);
        Wolf lobo = helper.spawn(EntityType.WOLF, new BlockPos(3, 2, 1));
        lobo.tame(aliado);
        lobo.setHealth(2);
        lobo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
        lobo.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600));
        Wolf solto = helper.spawn(EntityType.WOLF, new BlockPos(1, 2, 4));
        solto.setHealth(2);
        Zombie zumbi = zumbi(helper, 5, 1);
        zumbi.setHealth(6);
        zumbi.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
        jogador.setShiftKeyDown(true);
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        ticks(jogador, 30);
        helper.assertTrue(ContextoServidor.firme(jogador), "cravou a espada: preso ao chão");
        helper.assertTrue(jogador.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "não anda");
        helper.assertTrue(perto(jogador.getHealth(), 5), "3 s de carga: ainda não curou");
        ticks(jogador, 32);
        helper.assertTrue(perto(jogador.getHealth(), jogador.getMaxHealth()), "cura o Cid por completo, vida " + jogador.getHealth());
        helper.assertTrue(perto(lobo.getHealth(), lobo.getMaxHealth()), "e o lobo de outro jogador, vida " + lobo.getHealth());
        helper.assertTrue(!lobo.hasEffect(MobEffects.WEAKNESS), "tira o efeito ruim");
        helper.assertTrue(lobo.hasEffect(MobEffects.DAMAGE_RESISTANCE), "e deixa o bom");
        helper.assertTrue(solto.getHealth() <= 2, "lobo sem dono não é aliado, vida " + solto.getHealth());
        helper.assertTrue(zumbi.getHealth() <= 6 && zumbi.hasEffect(MobEffects.WEAKNESS), "o inimigo não é curado, vida " + zumbi.getHealth());
        helper.assertTrue(!ContextoServidor.firme(jogador) && jogador.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0, "solto de novo");
        long agora = jogador.level().getGameTime();
        helper.assertTrue(kit.recargas().restante(Cid.RECOVERY, agora) > 1700, "recarga de 90 s");
        helper.assertTrue(kit.recargas().restante(Cid.ATOMIC, agora) > 1700, "que o R divide");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void golpeForteArrancaAEspadaDoChaoNoRecovery(GameTestHelper helper) {
        ServerPlayer jogador = cidDesperto(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        jogador.setShiftKeyDown(true);
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        ticks(jogador, 10);
        jogador.hurt(jogador.damageSources().cactus(), 6);
        helper.assertTrue(kit.execucao().isPresent(), "golpe fraco não cancela");
        // O traje de slime corta o primeiro golpe forte pela metade: 10 vira 5 e a carga aguenta.
        jogador.invulnerableTime = 0;
        jogador.hurt(jogador.damageSources().cactus(), 10);
        helper.assertTrue(kit.execucao().isPresent(), "o traje segurou o golpe de 10");
        jogador.invulnerableTime = 0;
        jogador.hurt(jogador.damageSources().cactus(), 8);
        helper.assertTrue(kit.execucao().isEmpty(), "o golpe de 8 cancela");
        helper.assertTrue(!ContextoServidor.firme(jogador), "e solta o Cid");
        float vida = jogador.getHealth();
        ticks(jogador, 60);
        helper.assertTrue(perto(jogador.getHealth(), vida), "cancelado não cura, vida " + jogador.getHealth());
        helper.assertTrue(kit.recargas().pronta(Cid.ATOMIC, jogador.level().getGameTime()), "nem gasta a recarga do R");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void morrerCarregandoORecoveryNaoDeixaOCidPreso(GameTestHelper helper) {
        ServerPlayer jogador = cidDesperto(helper);
        jogador.setShiftKeyDown(true);
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        ticks(jogador, 10);
        helper.assertTrue(ContextoServidor.firme(jogador), "preso na carga");
        jogador.setHealth(0);
        ticks(jogador, 1);
        helper.assertTrue(!ContextoServidor.firme(jogador), "morto, o modificador sai");
        helper.succeed();
    }

    // --- O R: I Am Atomic final ---

    /**
     * O alvo do R, a 3 blocos à frente. Os testes ficam colados uns nos outros, e um zumbi de um teste vizinho
     * pode ter sobrado bem na linha da mira: os que estão por perto somem antes.
     */
    private static Zombie alvoSozinho(GameTestHelper helper, ServerPlayer jogador) {
        for (net.minecraft.world.entity.LivingEntity e : jogador.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                jogador.getBoundingBox().inflate(8))) {
            if (e != jogador) e.discard();
        }
        return zumbi(helper, 4, 1);
    }

    @GameTest(template = "vazio")
    public static void oAtomicSemAlvoNaoGastaNada(GameTestHelper helper) {
        ServerPlayer jogador = cidDesperto(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        double magia = kit.recurso().atual();
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        ticks(jogador, 6);
        helper.assertTrue(!EspacoServidor.emViagem(jogador), "sem alvo não há viagem");
        helper.assertTrue(kit.recurso().atual() >= magia - 1e-6, "a Magia volta, tem " + kit.recurso().atual());
        helper.assertTrue(kit.recargas().pronta(Cid.ATOMIC, jogador.level().getGameTime()), "e a recarga também");
        helper.assertTrue(kit.recargas().pronta(Cid.RECOVERY, jogador.level().getGameTime()), "a do Recovery nem começa");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void oAtomicSobeOAlvoASocosEDevolveOsDois(GameTestHelper helper) {
        ServerPlayer jogador = cidDesperto(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        Zombie zumbi = alvoSozinho(helper, jogador);
        zumbi.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        zumbi.setHealth(100);
        net.minecraft.world.phys.Vec3 cidAntes = jogador.position(), zumbiAntes = zumbi.position();
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        ticks(jogador, 4);
        helper.assertTrue(EspacoServidor.emViagem(jogador) && EspacoServidor.emViagem(zumbi), "os dois estão na viagem");
        helper.assertTrue(perto(zumbi.getHealth(), 98), "o primeiro soco tira 2, vida " + zumbi.getHealth());
        helper.assertTrue(kit.recurso().atual() < 1, "gastou os 100 de Magia, tem " + kit.recurso().atual());
        // Só o golpe fere o alvo, e nada fere o Cid.
        zumbi.invulnerableTime = 0;
        zumbi.hurt(zumbi.damageSources().cactus(), 10);
        helper.assertTrue(perto(zumbi.getHealth(), 98), "outro dano não chega ao alvo, vida " + zumbi.getHealth());
        float vida = jogador.getHealth();
        jogador.hurt(jogador.damageSources().cactus(), 10);
        helper.assertTrue(perto(jogador.getHealth(), vida), "o Cid não recebe dano");
        ticks(jogador, 40);
        helper.assertTrue(perto(zumbi.getHealth(), 92), "quatro socos no mundo, vida " + zumbi.getHealth());
        helper.assertTrue(zumbi.getY() - zumbiAntes.y > 40, "o alvo subiu, " + (zumbi.getY() - zumbiAntes.y));
        helper.assertTrue(jogador.getY() - cidAntes.y > 30 && jogador.isNoGravity(), "e o Cid surge ao lado, sem cair");
        ticks(jogador, 106);
        helper.assertTrue(perto(zumbi.getHealth(), 84), "mais quatro socos lá em cima, vida " + zumbi.getHealth());
        ticks(jogador, 80);
        helper.assertTrue(perto(zumbi.getHealth(), 40), "o Atomic tira 44, vida " + zumbi.getHealth());
        helper.assertTrue(EspacoServidor.emViagem(jogador), "o clarão ainda dura");
        ticks(jogador, AtomicFinal.TICKS_DO_CLARAO);
        helper.assertTrue(!EspacoServidor.emViagem(jogador) && !EspacoServidor.emViagem(zumbi), "a viagem acabou");
        helper.assertTrue(jogador.position().distanceTo(cidAntes) < .01 && !jogador.isNoGravity(), "o Cid volta ao ponto de partida");
        helper.assertTrue(zumbi.position().distanceTo(zumbiAntes) < .01 && !zumbi.isNoGravity() && zumbi.isNoAi(),
                "o alvo também, como era antes");
        helper.assertTrue(!ContextoServidor.firme(jogador), "e o Cid anda de novo");
        long agora = jogador.level().getGameTime();
        helper.assertTrue(kit.recargas().restante(Cid.ATOMIC, agora) > 1600 && kit.recargas().restante(Cid.RECOVERY, agora) > 1600,
                "recarga de 90 s, dividida com o Recovery");
        zumbi.invulnerableTime = 0;
        zumbi.hurt(zumbi.damageSources().cactus(), 5);
        helper.assertTrue(perto(zumbi.getHealth(), 35), "depois da viagem o alvo volta a apanhar de tudo, vida " + zumbi.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void oAlvoQueMorreNoMeioDoAtomicEncurtaAViagem(GameTestHelper helper) {
        ServerPlayer jogador = cidDesperto(helper);
        Zombie zumbi = alvoSozinho(helper, jogador);
        zumbi.setHealth(3);
        net.minecraft.world.phys.Vec3 cidAntes = jogador.position();
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        ticks(jogador, 18);
        helper.assertTrue(!zumbi.isAlive(), "o segundo soco mata");
        helper.assertTrue(EspacoServidor.emViagem(jogador), "o Cid segue para o Atomic");
        ticks(jogador, 6 + AtomicFinal.TICKS_DO_CLARAO);
        helper.assertTrue(!EspacoServidor.emViagem(jogador), "e volta sem esperar os socos que faltavam");
        helper.assertTrue(jogador.position().distanceTo(cidAntes) < .01 && !jogador.isNoGravity(), "ao ponto de partida");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void morrerNoMeioDoAtomicSoltaOAlvo(GameTestHelper helper) {
        ServerPlayer jogador = cidDesperto(helper);
        Zombie zumbi = alvoSozinho(helper, jogador);
        net.minecraft.world.phys.Vec3 zumbiAntes = zumbi.position();
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        ticks(jogador, 15);
        helper.assertTrue(zumbi.getY() - zumbiAntes.y > 10, "o alvo já subiu");
        jogador.setHealth(0);
        ticks(jogador, 1);
        helper.assertTrue(!EspacoServidor.emViagem(zumbi) && !zumbi.isNoGravity(), "o alvo é solto");
        helper.assertTrue(zumbi.position().distanceTo(zumbiAntes) < .01, "no ponto de onde saiu");
        helper.assertTrue(!jogador.isNoGravity() && !ContextoServidor.firme(jogador), "e o Cid não fica preso");
        helper.succeed();
    }
}
