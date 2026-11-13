package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.Gojo;
import com.minerwk.registro.Efeitos;
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

    public static ServerPlayer gojo(GameTestHelper helper) {
        // Jogador real sem conexão, no sobrevivência: o falso do GameTest é criativo e ignoraria o dano.
        ServerPlayer jogador = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "gojo_teste"), ClientInformation.createDefault());
        CompoundTag modo = new CompoundTag();
        modo.putInt("playerGameType", GameType.SURVIVAL.getId());
        jogador.loadGameTypes(modo);
        // Recém-nascido fica 60 ticks imune a dano; sem zerar, os testes de dano passariam sem testar nada.
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, jogador, 0, "spawnInvulnerableTime");
        jogador.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
        // Os testes de mecânica conferem os números de cada golpe: sem a camada de poder (ver TestesPoder).
        PoderServidor.semCamada(jogador);
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Gojo.ID).orElseThrow());
        // Os testes dos golpes não são sobre o trancamento: o Gojo deles já despertou.
        ServicoKits.definirDespertou(jogador, true);
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
    public static void zumbiNaMiraDoRLevaUppercutESobe25(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        jogador.setYRot(-90); // olhando para +x
        jogador.setXRot(0);
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 2, 1));
        ContextoServidor ctx = new ContextoServidor(jogador);
        Object alvo = ctx.marcarAlvo(60, 4);
        helper.assertTrue(zumbi.getUUID().equals(alvo), "o zumbi na mira deveria ser o alvo, veio " + alvo);
        float vida = zumbi.getHealth();
        ctx.ferir(alvo, 10);
        helper.assertTrue(zumbi.getHealth() < vida, "o uppercut deveria ferir");
        Vetor base = ctx.posicaoDe(alvo);
        Vetor topo = base.mais(new Vetor(0, 25, 0));
        ctx.suspender(alvo, topo, 6);
        helper.runAfterDelay(3, () -> {
            Vetor agora = ctx.posicaoDe(alvo);
            helper.assertTrue(agora.distancia(topo) < 0.5, "segurado 25 blocos acima: " + agora + " ≠ " + topo);
            helper.succeed();
        });
    }

    @GameTest(template = "vazio")
    public static void descidaDoRAcabaQuandoOGojoPousa(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        jogador.setOnGround(false);
        new ContextoServidor(jogador).descida("gojo_roxo_maximo_descida", 600);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(BarreirasServidor.voando(jogador), "no ar, ainda voando");
            jogador.setOnGround(true);
        });
        helper.runAfterDelay(4, () -> {
            helper.assertFalse(BarreirasServidor.voando(jogador), "pousou: o voo acabou");
            helper.succeed();
        });
    }

    @GameTest(template = "vazio")
    public static void descidaNoChaoNaoVoa(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        jogador.setOnGround(true);
        new ContextoServidor(jogador).descida("gojo_roxo_maximo_descida", 600);
        helper.assertFalse(BarreirasServidor.voando(jogador), "já no chão: nem voo nem pose de descida");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void vooNovoNaDescidaNaoAcabaAoPousar(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        jogador.setOnGround(false);
        ContextoServidor ctx = new ContextoServidor(jogador);
        ctx.descida("gojo_roxo_maximo_descida", 600);
        ctx.voo(100); // outro R antes de pousar
        jogador.setOnGround(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(BarreirasServidor.voando(jogador), "o voo novo não é descida: não acaba ao pousar");
            helper.succeed();
        });
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
    public static void dominioSeguraZumbiNoArENaoDeixaSair(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Vec3 centro = helper.absoluteVec(new Vec3(4.5, 7, 4.5));
        double chao = centro.y - 1;
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 6, 4));
        zumbi.setPos(centro.x, chao, centro.z);
        new ContextoServidor(jogador).barreira(vetor(centro), 3, chao, 40);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(Math.abs(zumbi.getY() - chao) < 0.2, "o zumbi deveria ficar em pé no ar, y " + (zumbi.getY() - chao));
            zumbi.setDeltaMovement(1.5, 0, 0);
        });
        helper.runAfterDelay(30, () -> {
            double d = zumbi.position().distanceTo(centro);
            helper.assertTrue(d < 3, "o zumbi deveria continuar dentro, distância " + d);
            helper.succeed();
        });
    }

    @GameTest(template = "vazio")
    public static void dominioNaoDeixaZumbiDeForaEntrar(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Vec3 centro = helper.absoluteVec(new Vec3(4.5, 3, 4.5));
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, 2, 4));
        new ContextoServidor(jogador).barreira(vetor(centro), 3, centro.y - 1, 20);
        helper.runAtTickTime(helper.getTick() + 1, () -> zumbi.setDeltaMovement(-1.5, 0, 0));
        helper.runAfterDelay(10, () -> {
            double d = zumbi.position().distanceTo(centro);
            helper.assertTrue(d >= 3, "o zumbi de fora não deveria entrar, distância " + d);
            helper.succeed();
        });
    }

    @GameTest(template = "vazio")
    public static void explosaoRoxaApagaAsCascas(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        for (int x = 2; x <= 6; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 2; z <= 6; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            }
        }
        Vec3 centro = helper.absoluteVec(new Vec3(4.5, 4.5, 4.5));
        ContextoServidor ctx = new ContextoServidor(jogador);
        ctx.apagarCasca(vetor(centro), 0, 1.5);
        ctx.apagarCasca(vetor(centro), 1.5, 3);
        helper.runAfterDelay(3, () -> {
            // Cada bloco com o centro a até 3 do centro some; os de fora ficam (as duas cascas sem buraco).
            for (int x = 2; x <= 6; x++) {
                for (int y = 2; y <= 6; y++) {
                    for (int z = 2; z <= 6; z++) {
                        BlockPos b = new BlockPos(x, y, z);
                        boolean dentro = helper.absoluteVec(Vec3.atCenterOf(b)).distanceTo(centro) <= 3;
                        helper.assertBlockPresent(dentro ? Blocks.AIR : Blocks.STONE, b);
                    }
                }
            }
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

    @GameTest(template = "vazio")
    public static void soGolpeDoKitEmInimigoDaXpDeMaestria(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        Zombie zumbi = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        Wolf lobo = helper.spawn(EntityType.WOLF, new BlockPos(1, 2, 3));
        DadosMaestria m = ServicoKits.dados(jogador).maestriaAtual();
        zumbi.hurt(helper.getLevel().damageSources().playerAttack(jogador), 3);
        helper.assertTrue(m.xp == 0, "soco não é golpe do kit: sem XP");
        ContextoServidor ctx = new ContextoServidor(jogador);
        ctx.ferir(lobo.getUUID(), 3);
        helper.assertTrue(m.xp == 0, "lobo não é inimigo: sem XP");
        zumbi.invulnerableTime = 0;
        ctx.ferir(zumbi.getUUID(), 4);
        helper.assertTrue(m.xp == 4, "4 de dano num zumbi deveria dar 4 de XP, deu " + m.xp);
        zumbi.invulnerableTime = 0;
        ctx.ferir(zumbi.getUUID(), 1000);
        helper.assertTrue(m.xp == 4 + 20 + 25, "o golpe que mata dá o teto do dano e o abate, deu " + m.xp);
        ServicoKits.ganharXp(jogador, 100);
        helper.assertTrue(com.minerwk.core.maestria.Maestria.nivel(m.xp) == 2, "149 de XP é nível 2");
        // Trocar de personagem e voltar não apaga a maestria.
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Personagens.TESTE).orElseThrow());
        helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().xp == 0, "o outro personagem começa do zero");
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Gojo.ID).orElseThrow());
        helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().xp == 149, "a maestria do Gojo volta");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void compraDeNoValeNoServidorEChegaNoKit(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        ServicoKits.definirDespertou(jogador, false);
        var ok = com.minerwk.core.maestria.Arvore.Resultado.OK;
        String reserva = com.minerwk.core.personagens.gojo.ArvoreGojo.ENERGIA_RESERVA;
        String despertar = com.minerwk.core.personagens.gojo.ArvoreGojo.DESPERTAR;
        String vermelho = com.minerwk.core.personagens.gojo.ArvoreGojo.VERMELHO_FORCA;
        helper.assertTrue(ServicoKits.comprarNo(jogador, reserva) == com.minerwk.core.maestria.Arvore.Resultado.SEM_PONTOS,
                "no nível 1 não há pontos");
        ServicoKits.definirXp(jogador, com.minerwk.core.maestria.Maestria.xpAte(20));
        helper.assertTrue(ServicoKits.comprarNo(jogador, reserva) == ok, "com 19 pontos compra a reserva");
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        helper.assertTrue(kit.recurso().maximo() == 220, "a reserva deveria subir o máximo para 220, está " + kit.recurso().maximo());
        helper.assertTrue(new ContextoServidor(jogador).melhorias().nivel(reserva) == 1, "o contexto enxerga o nó");
        helper.assertTrue(ServicoKits.comprarNo(jogador, despertar) == ok, "no nível 20 compra o Despertar");
        helper.assertTrue(ServicoKits.comprarNo(jogador, vermelho) == com.minerwk.core.maestria.Arvore.Resultado.NAO_DESPERTOU,
                "nó de depois do despertar fica trancado");
        helper.assertTrue(ServicoKits.comprarNo(jogador, "nao_existe") == com.minerwk.core.maestria.Arvore.Resultado.NO_DESCONHECIDO,
                "id inventado é recusado");
        // Trocar de personagem e voltar: os nós continuam valendo.
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Personagens.TESTE).orElseThrow());
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Gojo.ID).orElseThrow());
        kit = ServicoKits.kit(jogador).orElseThrow();
        helper.assertTrue(kit.recurso().maximo() == 220 && kit.recurso().atual() == 220, "o kit novo nasce com a reserva cheia");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void golpesTrancadosAteDespertar(GameTestHelper helper) {
        ServerPlayer jogador = gojo(helper);
        ServicoKits.definirDespertou(jogador, false);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        long agora = helper.getLevel().getGameTime();
        helper.assertFalse(kit.apertar(com.minerwk.core.kit.Tecla.X, agora), "sem despertar o Vermelho não começa");
        helper.assertFalse(kit.apertar(com.minerwk.core.kit.Tecla.R, agora), "nem o R");
        // Desperto só na luta: vale agora e some quando a luta acaba.
        ServicoKits.definirDespertoNaLuta(jogador, true);
        helper.assertTrue(ServicoKits.desperto(jogador), "desperto na luta");
        helper.assertFalse(ServicoKits.dados(jogador).maestriaAtual().despertou, "mas não de vez");
        helper.assertTrue(kit.apertar(com.minerwk.core.kit.Tecla.X, agora), "na luta o Vermelho funciona");
        kit.descartar();
        ServicoKits.definirDespertoNaLuta(jogador, false);
        helper.assertFalse(kit.apertar(com.minerwk.core.kit.Tecla.X, agora), "acabou a luta, tranca de novo");
        helper.assertTrue(kit.apertar(com.minerwk.core.kit.Tecla.Z, agora), "o Azul nunca tranca");
        kit.descartar();
        // Trocar de personagem durante a luta não carrega o despertar junto.
        ServicoKits.definirDespertoNaLuta(jogador, true);
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Gojo.ID).orElseThrow());
        helper.assertFalse(ServicoKits.desperto(jogador), "kit novo começa trancado");
        helper.succeed();
    }
}
