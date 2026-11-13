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
        // Outro personagem: a arma do kit troca pela dele (o Gojo bate com o punho).
        ServicoKits.definirPersonagem(jogador, Personagens.porId(com.minerwk.core.personagens.gojo.Gojo.ID).orElseThrow());
        ticks(jogador, 1);
        helper.assertTrue(com.minerwk.core.personagens.gojo.Gojo.PUNHO.equals(ArmaServidor.atual(jogador)), "a Zangetsu vira o punho do Gojo");
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
        helper.assertTrue(perto(terceiro.getHealth(), 20 - com.minerwk.core.personagens.gojo.Gojo.PUNHO.dano()),
                "o soco do Gojo é o punho dele, vida " + terceiro.getHealth());
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

    // --- Parte 6: máscara Hollow, Mugetsu e a perda dos poderes ---

    /** Ichigo desperto (a máscara e o Mugetsu são trancados), já no Bankai e com o reiatsu cheio. */
    private static KitDoJogador bankaiDesperto(ServerPlayer jogador) {
        ServicoKits.definirDespertou(jogador, true);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        kit.mudarForma(Reiatsu.BANKAI, new ContextoServidor(jogador));
        kit.recurso().carregar(Reiatsu.MAXIMO);
        return kit;
    }

    /** Um alvo que aguenta o golpe e tem armadura pesada: só dano que ignora armadura tira o valor cheio. */
    private static net.minecraft.world.entity.animal.IronGolem blindado(GameTestHelper helper, int x, int z) {
        var golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(x, 2, z));
        golem.setNoAi(true);
        golem.getAttribute(Attributes.ARMOR).setBaseValue(30);
        golem.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(12);
        return golem;
    }

    @GameTest(template = "vazio")
    public static void mascaraEMugetsuSaoTrancadosNoServidor(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ServicoKits.definirDespertou(jogador, false);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        kit.mudarForma(Reiatsu.BANKAI, new ContextoServidor(jogador));
        ServicoKits.usarTecla(jogador, Tecla.V, true);
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        ticks(jogador, 4);
        helper.assertTrue(kit.execucao().isEmpty(), "sem despertar, a máscara e o Mugetsu não começam");
        ServicoKits.definirDespertou(jogador, true);
        ServicoKits.usarTecla(jogador, Tecla.V, true);
        ticks(jogador, 2);
        helper.assertTrue(kit.execucao().map(e -> e.habilidade().id().equals(Ichigo.MASCARA)).orElse(false), "desperto, a máscara começa");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void mascaraAumentaODanoDoGetsugaNegroEDoAtaqueBasico(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        bankaiDesperto(jogador);
        var alvo = blindado(helper, 5, 1);
        alvo.getAttribute(Attributes.ARMOR).setBaseValue(0);
        alvo.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        ServicoKits.usarTecla(jogador, Tecla.V, true);
        ticks(jogador, 20);
        helper.assertTrue(perto(ArmaServidor.atual(jogador).dano(), 6 * 1.3f), "a Tensa com a máscara fere 7,8, fere "
                + ArmaServidor.atual(jogador).dano());
        ServicoKits.usarTecla(jogador, Tecla.Z, true);
        ticks(jogador, 12);
        helper.assertTrue(perto(alvo.getHealth(), 100 - 13), "Getsuga negro de 10 vira 13: sobrou " + alvo.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void mugetsuIgnoraArmaduraETiraOsPoderes(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        KitDoJogador kit = bankaiDesperto(jogador);
        var alvo = blindado(helper, 6, 1);
        // A mesma armadura segura quase todo o Getsuga negro: é a prova de que o Mugetsu não a conta.
        ServicoKits.usarTecla(jogador, Tecla.Z, true);
        ticks(jogador, 12);
        float comArmadura = 100 - alvo.getHealth();
        helper.assertTrue(comArmadura > 0 && comArmadura < 5, "o Getsuga negro (10) deveria ser quase todo absorvido, tirou " + comArmadura);
        alvo.setHealth(100);
        alvo.invulnerableTime = 0;
        kit.recurso().carregar(Reiatsu.MAXIMO);
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        ticks(jogador, 125);
        helper.assertTrue(perto(alvo.getHealth(), 60), "o Mugetsu tira 40 mesmo com armadura: sobrou " + alvo.getHealth());
        helper.assertTrue(Forma.BASE.equals(kit.forma()), "depois do Mugetsu ele volta ao Shikai");
        helper.assertTrue(kit.recurso().atual() == 0, "sem reiatsu, tem " + kit.recurso().atual());
        long agora = jogador.level().getGameTime();
        for (Tecla t : java.util.List.of(Tecla.Z, Tecla.X, Tecla.C, Tecla.V, Tecla.G)) {
            long falta = kit.recargas().restante(kit.habilidade(t).orElseThrow().id(), agora);
            helper.assertTrue(falta > 1100 && falta <= 1200, "o golpe da tecla " + t + " fica 60 s parado, faltam " + falta);
        }
        helper.assertTrue(kit.recargas().restante(Ichigo.MUGETSU, agora) > 3500, "e o Mugetsu, 180 s");
        ticks(jogador, 30);
        helper.assertTrue(Ichigo.ZANGETSU.equals(ArmaServidor.atual(jogador)), "sobra o ataque básico da Zangetsu");
        helper.assertTrue(kit.recurso().atual() == 0, "selado: não regenera");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void semPoderesSobreviveASalvarECarregar(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        KitDoJogador kit = bankaiDesperto(jogador);
        ServicoKits.usarTecla(jogador, Tecla.R, true);
        ticks(jogador, 125);
        helper.assertTrue(kit.recurso().atual() == 0, "o Mugetsu saiu");
        // Sair e entrar no jogo: o kit é remontado a partir do que foi salvo.
        double salvo = kit.recurso().salvar();
        helper.assertTrue(salvo < 0, "o selo vai para o disco (valor salvo " + salvo + ")");
        Reiatsu outro = new Reiatsu();
        outro.carregar(salvo);
        helper.assertTrue(outro.atual() == 0 && !outro.pode(1), "ao voltar ele continua sem reiatsu");
        helper.succeed();
    }

    // --- A forma Hollow completa ---

    private static boolean hollow(KitDoJogador kit) {
        return Reiatsu.HOLLOW.equals(kit.forma());
    }

    @GameTest(template = "vazio")
    public static void golpeFatalNoBankaiViraAFormaHollow(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        KitDoJogador kit = bankaiDesperto(jogador);
        jogador.setHealth(5);
        jogador.hurt(jogador.damageSources().generic(), 50);
        helper.assertTrue(jogador.isAlive() && hollow(kit), "em vez de morrer, vira o Hollow");
        helper.assertTrue(perto(jogador.getHealth(), 11), "1 de vida mais 10 de cura: " + jogador.getHealth());
        helper.assertFalse(kit.recargas().pronta(Ichigo.PASSIVA_HOLLOW, jogador.level().getGameTime()), "a passiva entra em recarga");
        // Na forma: metade do dano, e nada o mata.
        jogador.invulnerableTime = 0;
        jogador.hurt(jogador.damageSources().generic(), 4);
        helper.assertTrue(perto(jogador.getHealth(), 9), "4 de dano viram 2: " + jogador.getHealth());
        jogador.invulnerableTime = 0;
        jogador.hurt(jogador.damageSources().generic(), 500);
        helper.assertTrue(jogador.isAlive() && perto(jogador.getHealth(), 1), "o que mataria o deixa com 1");
        // Um golpe que a armadura segura não gasta a forma: conta o dano que chegaria à vida.
        ServerPlayer blindadoDeBankai = ichigo(helper);
        KitDoJogador outro = bankaiDesperto(blindadoDeBankai);
        blindadoDeBankai.getAttribute(Attributes.ARMOR).setBaseValue(20);
        blindadoDeBankai.setHealth(10);
        blindadoDeBankai.hurt(blindadoDeBankai.damageSources().cactus(), 12);
        helper.assertTrue(!hollow(outro) && blindadoDeBankai.getHealth() < 10 && blindadoDeBankai.getHealth() > 1,
                "12 de dano com armadura não mata quem tem 10: " + blindadoDeBankai.getHealth());
        ServicoKits.usarTecla(jogador, Tecla.Z, true);
        ticks(jogador, 3);
        helper.assertTrue(kit.execucao().isEmpty(), "as teclas não respondem");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void formaHollowSoValeNoBankaiDespertoEUmaVezPorRecarga(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ServicoKits.definirDespertou(jogador, false);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        kit.mudarForma(Reiatsu.BANKAI, new ContextoServidor(jogador));
        jogador.setHealth(5);
        helper.assertFalse(Ichigo.podeVirarHollow(kit.forma(), ServicoKits.dados(jogador).maestriaAtual().despertou, true, false),
                "sem vencer o Zangetsu não há forma Hollow");
        ServicoKits.definirDespertou(jogador, true);
        kit.recargas().iniciar(Ichigo.PASSIVA_HOLLOW, jogador.level().getGameTime(), 100);
        // O jogador de teste não pode morrer de verdade (não tem conexão): confere-se a regra com o estado real.
        boolean pronta = kit.recargas().pronta(Ichigo.PASSIVA_HOLLOW, jogador.level().getGameTime());
        helper.assertFalse(Ichigo.podeVirarHollow(kit.forma(), true, pronta, false), "com a passiva em recarga o golpe mata");
        // Fora do Bankai também: um golpe que não mata passa inteiro e não vira nada.
        kit.recargas().limpar();
        kit.mudarForma(Forma.BASE, new ContextoServidor(jogador));
        jogador.hurt(jogador.damageSources().generic(), 2);
        helper.assertTrue(!hollow(kit) && perto(jogador.getHealth(), 3), "no Shikai o dano é o normal: " + jogador.getHealth());
        helper.succeed();
    }

    @GameTest(template = "vazio", timeoutTicks = 100)
    public static void formaHollowAtacaSozinhaEVoltaAoShikaiComSeisDeVida(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        KitDoJogador kit = bankaiDesperto(jogador);
        var alvo = blindado(helper, 3, 1);
        alvo.getAttribute(Attributes.ARMOR).setBaseValue(0);
        alvo.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        // A forma só vai atrás de inimigo: monstro, ou quem está atrás do Ichigo.
        var vaca = net.minecraft.world.entity.EntityType.COW.create(helper.getLevel());
        // Atrás dele, do lado oposto ao alvo: fora do caminho do Cero, que pega quem estiver na linha.
        var paraTras = jogador.position().subtract(alvo.position()).multiply(1, 0, 1).normalize().scale(1.5);
        vaca.moveTo(jogador.getX() + paraTras.x, jogador.getY(), jogador.getZ() + paraTras.z, 0, 0);
        vaca.setNoAi(true);
        helper.getLevel().addFreshEntity(vaca);
        // (Pode haver um monstro de outro teste à vista: o que importa é não ser nenhum destes dois.)
        Object manso = new ContextoServidor(jogador).inimigoProximo(24);
        helper.assertTrue(!alvo.getUUID().equals(manso) && !vaca.getUUID().equals(manso), "golem manso e vaca não são alvo");
        alvo.setTarget(jogador);
        jogador.setHealth(5);
        jogador.hurt(jogador.damageSources().generic(), 50);
        helper.assertTrue(hollow(kit), "virou o Hollow");
        ticks(jogador, 20);
        helper.assertTrue(perto(alvo.getHealth(), 100 - 8), "a garra fere 8 sem tecla nenhuma: sobrou " + alvo.getHealth());
        // Até o fim: mais garras e o Cero de 18.
        for (int i = 0; i < 190; i++) {
            alvo.invulnerableTime = 0;
            ticks(jogador, 1);
        }
        helper.assertTrue(alvo.getHealth() <= 100 - 8 - 18, "as garras e o Cero continuam: sobrou " + alvo.getHealth());
        helper.assertTrue(vaca.getHealth() == vaca.getMaxHealth(), "a vaca ao lado sai inteira");
        vaca.discard();
        helper.assertTrue(Forma.BASE.equals(kit.forma()), "ao acabar volta ao Shikai");
        helper.assertTrue(perto(jogador.getHealth(), 6), "com 6 de vida: " + jogador.getHealth());
        helper.assertFalse(kit.recargas().pronta(Ichigo.ENTRAR_NO_BANKAI, jogador.level().getGameTime()), "e o G em recarga");
        helper.succeed();
    }
}
