package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.boss.LutasZangetsu;
import com.minerwk.boss.ZangetsuEntity;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.core.boss.ZangetsuBoss;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.personagens.ichigo.ArvoreIchigo;
import com.minerwk.core.personagens.ichigo.Ichigo;
import com.minerwk.core.personagens.ichigo.Reiatsu;
import com.minerwk.registro.Entidades;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da luta contra o Zangetsu interior (rodam com {@code ./gradlew runGameTestServer}). Como os do
 * Toji, cada um roda sozinho num lote só dele, e as esperas são "até acontecer". O jogador dos testes não
 * está no mundo e por isso não conta os ticks de invulnerabilidade: onde vêm dois golpes seguidos, o teste
 * zera a invulnerabilidade a cada tick.
 */
@GameTestHolder(MineRWK.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestesZangetsu {
    private static final int CHAO = 0;
    /** Onde o jogador fica, no molde; o Zangetsu fica em x maior. */
    private static final double X = 4.5, Z = 4.5;

    private TestesZangetsu() {
    }

    /** Um Ichigo que ainda não despertou, no chão, com uma conexão de mentira (dano de verdade manda pacotes). */
    private static ServerPlayer ichigo(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Ichigo.ID).orElseThrow());
        ServicoKits.definirDespertou(jogador, false);
        if (jogador.connection == null) {
            Connection conexao = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(conexao);
            jogador.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), conexao, jogador,
                    CommonListenerCookie.createInitial(jogador.getGameProfile(), false));
        }
        // Longe do bloco que o molde "vazio" tem no canto: uma meia-lua pararia nele antes de chegar ao jogador.
        jogador.setPos(helper.absoluteVec(new Vec3(X, CHAO, Z)));
        helper.assertTrue(helper.getLevel().noCollision(jogador.getBoundingBox().deflate(1e-3)), "o jogador precisa de um lugar livre");
        // Olhando para onde o Zangetsu fica (x crescente).
        jogador.setYRot(-90);
        jogador.setXRot(0);
        ChunkPos centro = new ChunkPos(jogador.blockPosition());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) helper.getLevel().setChunkForced(centro.x + dx, centro.z + dz, true);
        }
        return jogador;
    }

    /** Um Zangetsu a {@code distancia} blocos do jogador, os dois no chão, já em luta. */
    private static ZangetsuEntity zangetsu(GameTestHelper helper, ServerPlayer jogador, double distancia) {
        ZangetsuEntity z = Entidades.ZANGETSU.get().create(helper.getLevel());
        Vec3 p = helper.absoluteVec(new Vec3(X + distancia, CHAO, Z));
        z.moveTo(p.x, p.y, p.z, 90, 0);
        z.comecarLuta(jogador);
        helper.getLevel().addFreshEntity(z);
        return z;
    }

    @GameTest(template = "vazio", batch = "zangetsu_1", timeoutTicks = 300)
    public static void mascaraRachadaExigeSerIchigoEONoDespertar(GameTestHelper helper) {
        ServerPlayer jogador = com.minerwk.arena.TestesArena.jogador(helper);
        helper.assertTrue(LutasZangetsu.invocar(jogador, true) == TojiBoss.Invocacao.OUTRO_PERSONAGEM, "o Gojo não entra");
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Ichigo.ID).orElseThrow());
        helper.assertTrue(LutasZangetsu.invocar(jogador, true) == TojiBoss.Invocacao.SEM_NO, "sem o nó a máscara não funciona");
        ServicoKits.dados(jogador).maestriaAtual().nos.put(ArvoreIchigo.DESPERTAR, 1);
        helper.assertTrue(LutasZangetsu.invocar(jogador, true) == TojiBoss.Invocacao.OK, "com o nó a entrada começa");
        helper.assertTrue(LutasZangetsu.invocar(jogador, true) == TojiBoss.Invocacao.NA_ARENA, "uma luta por jogador");
        boolean[] sumiu = {false};
        helper.succeedWhen(() -> {
            if (!sumiu[0]) {
                ZangetsuEntity z = LutasZangetsu.de(jogador.getUUID()).orElse(null);
                helper.assertTrue(z != null, "esperando o Zangetsu entrar na arena");
                double distancia = z.distanceTo(jogador);
                helper.assertTrue(distancia > 15 && distancia < 17, "a 16 blocos, está a " + distancia);
                // O mundo interior: os dois sobre a fachada de vidro e concreto.
                helper.assertTrue(z.level().getBlockState(z.blockPosition().below()).blocksMotion(), "há chão sob ele");
                helper.assertTrue(jogador.level().getBlockState(jogador.blockPosition().below()).blocksMotion(), "e sob o jogador");
                z.discard();
                sumiu[0] = true;
            }
            helper.assertTrue(LutasZangetsu.de(jogador.getUUID()).isEmpty(), "sem ele a luta acabou");
        });
    }

    @GameTest(template = "vazio", batch = "zangetsu_2")
    public static void entradaSeguraOPrimeiroGolpe(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 3);
        helper.startSequence()
                .thenIdle(ZangetsuBoss.CHEGADA + ZangetsuBoss.ENTRADA - 10)
                .thenExecute(() -> {
                    helper.assertTrue(z.golpe() == null, "na entrada ele ainda não ataca");
                    helper.assertTrue(jogador.getHealth() == 20, "nem fere");
                })
                .thenWaitUntil(() -> helper.assertTrue(z.golpe() != null, "depois da entrada vem o primeiro golpe"))
                .thenExecute(z::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "zangetsu_3")
    public static void corteDePertoFereSeis(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 2);
        z.forcar(ZangetsuBoss.CORTE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - ZangetsuBoss.DANO_CORTE, "vida " + jogador.getHealth()))
                .thenExecute(z::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "zangetsu_4")
    public static void getsugaBrancoFereNaLinha(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 10);
        z.forcar(ZangetsuBoss.GETSUGA);
        helper.startSequence()
                .thenIdle(ZangetsuBoss.GETSUGA.aviso() - 2)
                .thenExecute(() -> helper.assertTrue(jogador.getHealth() == 20, "no aviso ainda não fere"))
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - ZangetsuBoss.DANO_GETSUGA, "vida " + jogador.getHealth()))
                .thenExecute(z::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "zangetsu_5", timeoutTicks = 200)
    public static void sequenciaDeTresAcertaTresVezesEATerceiraDerruba(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 10);
        helper.onEachTick(() -> jogador.invulnerableTime = 0);
        z.forcar(ZangetsuBoss.GETSUGA_3);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 11, "o primeiro: vida " + jogador.getHealth()))
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 2, "o segundo: vida " + jogador.getHealth()))
                .thenWaitUntil(() -> helper.assertTrue(LutasZangetsu.caido(jogador), "o terceiro mataria: ele cai"))
                .thenExecute(z::discard)
                .thenExecute(() -> {
                    helper.assertFalse(LutasZangetsu.caido(jogador), "sem o Zangetsu ele é solto");
                    helper.assertFalse(ServicoKits.desperto(jogador), "e sem vitória não há despertar");
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "zangetsu_6", timeoutTicks = 300)
    public static void golpeMortalViraAQuedaEEleSeLevantaDeMascara(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 2);
        jogador.setHealth(4);
        z.forcar(ZangetsuBoss.CORTE);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(LutasZangetsu.caido(jogador), "o corte mataria: ele cai"))
                .thenExecute(() -> {
                    helper.assertTrue(jogador.isAlive() && jogador.getHealth() == 1, "vivo, com 1 de vida");
                    helper.assertTrue(z.golpe() == null, "e o Zangetsu espera");
                    helper.assertTrue(ProtecoesServidor.paralisado(jogador), "caído não age");
                })
                .thenWaitUntil(() -> helper.assertFalse(LutasZangetsu.caido(jogador), "esperando ele se levantar"))
                .thenExecute(() -> {
                    helper.assertTrue(jogador.getHealth() == jogador.getMaxHealth(), "vida cheia");
                    helper.assertTrue(ServicoKits.desperto(jogador), "os golpes trancados abrem nesta luta");
                    helper.assertFalse(ServicoKits.dados(jogador).maestriaAtual().despertou, "mas só nesta luta");
                    helper.assertTrue(Reiatsu.BANKAI.equals(kit.forma()), "ele se levanta no Bankai");
                    helper.assertTrue(kit.efeitos().stream().anyMatch(e -> e.getClass().getSimpleName().equals("MascaraAtiva")),
                            "com a máscara no rosto");
                    jogador.setHealth(3);
                    z.forcar(ZangetsuBoss.CORTE);
                })
                .thenWaitUntil(() -> helper.assertTrue(!jogador.isAlive() || jogador.getHealth() <= 0, "a segunda vez mata de verdade"))
                .thenWaitUntil(() -> helper.assertTrue(z.isRemoved(), "e ele vai embora"))
                .thenExecute(() -> helper.assertFalse(ServicoKits.desperto(jogador), "os golpes voltam a trancar"))
                .thenSucceed();
    }

    /** Um empurrão para o vão entre os prédios fere sem ser golpe dele: também não pode matar antes da máscara. */
    @GameTest(template = "vazio", batch = "zangetsu_12")
    public static void qualquerDanoMortalNaLutaViraAQueda(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 6);
        jogador.setHealth(3);
        jogador.hurt(jogador.damageSources().generic(), 4);
        helper.assertTrue(LutasZangetsu.caido(jogador), "a queda no vazio mataria: ele cai");
        helper.assertTrue(jogador.isAlive(), "e continua vivo");
        z.discard();
        helper.assertFalse(LutasZangetsu.caido(jogador), "sem o Zangetsu ele é solto");
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "zangetsu_7")
    public static void shunpoSurgeAtrasECorta(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 8);
        z.forcar(ZangetsuBoss.SHUNPO);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - ZangetsuBoss.DANO_SHUNPO, "vida " + jogador.getHealth()))
                .thenExecute(() -> {
                    helper.assertTrue(z.distanceTo(jogador) < 3, "ele está colado: " + z.distanceTo(jogador));
                    // O jogador olha para x crescente: atrás dele é x menor.
                    helper.assertTrue(z.getX() < jogador.getX(), "e atrás do jogador");
                    z.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "zangetsu_8", timeoutTicks = 200)
    public static void espadaArremessadaFereUmaVezEVolta(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 6);
        helper.onEachTick(() -> jogador.invulnerableTime = 0);
        z.forcar(ZangetsuBoss.ARREMESSO);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(z.semEspada(), "a espada sai da mão"))
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - ZangetsuBoss.DANO_ARREMESSO, "vida " + jogador.getHealth()))
                .thenWaitUntil(() -> helper.assertFalse(z.semEspada(), "esperando a espada voltar"))
                .thenExecute(() -> {
                    helper.assertTrue(jogador.getHealth() == 20 - ZangetsuBoss.DANO_ARREMESSO, "na volta não fere de novo");
                    z.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "zangetsu_9")
    public static void ceroFereDozeNaLinha(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 12);
        z.forcar(ZangetsuBoss.CERO);
        helper.startSequence()
                .thenIdle(ZangetsuBoss.CERO.aviso() - 2)
                .thenExecute(() -> helper.assertTrue(jogador.getHealth() == 20, "na carga ainda não fere"))
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - ZangetsuBoss.DANO_CERO, "vida " + jogador.getHealth()))
                .thenExecute(z::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "zangetsu_10", timeoutTicks = 200)
    public static void abaixoDeQuarentaPorCentoEleVesteAMascara(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 6);
        helper.assertTrue(z.fase() == 1 && !z.mascarado(), "começa sem máscara");
        z.setHealth(ZangetsuBoss.VIDA * .39f);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(z.fase() == 2, "esperando a segunda fase"))
                .thenExecute(() -> helper.assertTrue(z.golpe() == null, "enquanto veste a máscara ele não ataca"))
                .thenWaitUntil(() -> helper.assertTrue(z.mascarado(), "a máscara aparece no rosto"))
                .thenExecute(() -> helper.assertTrue(com.minerwk.rede.CenarioArena.INTERIOR_CHUVA.equals(
                        com.minerwk.arena.ArenasServidor.cenarioEnviado(jogador.getUUID())), "a fase 2 manda a chuva"))
                .thenExecute(z::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "zangetsu_11", timeoutTicks = 200)
    public static void vencerLiberaODespertarEDaQuinhentosDeXp(GameTestHelper helper) {
        ServerPlayer jogador = ichigo(helper);
        ZangetsuEntity z = zangetsu(helper, jogador, 4);
        long antes = ServicoKits.dados(jogador).maestriaAtual().xp;
        z.hurt(z.damageSources().playerAttack(jogador), 10000);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(z.isRemoved(), "esperando ele cair"))
                .thenExecute(() -> {
                    helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().despertou, "o despertar é de vez");
                    helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().xp == antes + ZangetsuBoss.XP_VITORIA, "500 de XP");
                    KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
                    helper.assertFalse(kit.trancada(com.minerwk.core.kit.Tecla.G), "o Bankai nunca foi trancado");
                })
                .thenSucceed();
    }
}
