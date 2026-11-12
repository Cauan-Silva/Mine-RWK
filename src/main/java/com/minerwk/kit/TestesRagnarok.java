package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.boss.LutasRagnarok;
import com.minerwk.boss.RagnarokEntity;
import com.minerwk.core.arena.PlantaOriana;
import com.minerwk.core.boss.RagnarokBoss;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.personagens.cid.ArvoreCid;
import com.minerwk.core.personagens.cid.Cid;
import com.minerwk.registro.Efeitos;
import com.minerwk.registro.Entidades;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da luta contra o Ragnarok (rodam com {@code ./gradlew runGameTestServer}). Como os do Zangetsu,
 * cada um roda sozinho num lote só dele, e as esperas são "até acontecer". O jogador dos testes não está no
 * mundo: não anda com o empurrão nem conta os ticks de invulnerabilidade.
 */
@GameTestHolder(MineRWK.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestesRagnarok {
    private static final int CHAO = 0;
    /** Onde o jogador fica, no molde; o Ragnarok fica em x maior. */
    private static final double X = 4.5, Z = 4.5;

    private TestesRagnarok() {
    }

    /** Um Cid que ainda não despertou, no chão, com uma conexão de mentira (dano de verdade manda pacotes). */
    private static ServerPlayer cid(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Cid.ID).orElseThrow());
        ServicoKits.definirDespertou(jogador, false);
        if (jogador.connection == null) {
            Connection conexao = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(conexao);
            jogador.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), conexao, jogador,
                    CommonListenerCookie.createInitial(jogador.getGameProfile(), false));
        }
        jogador.setPos(helper.absoluteVec(new Vec3(X, CHAO, Z)));
        helper.assertTrue(helper.getLevel().noCollision(jogador.getBoundingBox().deflate(1e-3)), "o jogador precisa de um lugar livre");
        jogador.setYRot(-90);
        jogador.setXRot(0);
        ChunkPos centro = new ChunkPos(jogador.blockPosition());
        for (int dx = -1; dx <= 3; dx++) {
            for (int dz = -1; dz <= 1; dz++) helper.getLevel().setChunkForced(centro.x + dx, centro.z + dz, true);
        }
        return jogador;
    }

    /** O primeiro golpe forte de cada luta chega pela metade: o traje de slime do Cid o segura. */
    private static float comTraje(float dano) {
        return Cid.amortecer(dano, true);
    }

    /** Um Ragnarok a {@code distancia} blocos do jogador (de centro a centro), já em luta e virado para ele. */
    private static RagnarokEntity ragnarok(GameTestHelper helper, ServerPlayer jogador, double distancia) {
        RagnarokEntity r = Entidades.RAGNAROK.get().create(helper.getLevel());
        Vec3 p = helper.absoluteVec(new Vec3(X + distancia, CHAO, Z));
        r.moveTo(p.x, p.y, p.z, 90, 0);
        r.comecarLuta(jogador);
        helper.getLevel().addFreshEntity(r);
        return r;
    }

    @GameTest(template = "vazio", batch = "ragnarok_1", timeoutTicks = 300)
    public static void anelDaSucessaoExigeSerOCidEONoDespertar(GameTestHelper helper) {
        ServerPlayer jogador = com.minerwk.arena.TestesArena.jogador(helper);
        helper.assertTrue(LutasRagnarok.invocar(jogador, true) == TojiBoss.Invocacao.OUTRO_PERSONAGEM, "o Gojo não abre a Rosa Negra");
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Cid.ID).orElseThrow());
        helper.assertTrue(LutasRagnarok.invocar(jogador, true) == TojiBoss.Invocacao.SEM_NO, "sem o nó o anel não funciona");
        ServicoKits.dados(jogador).maestriaAtual().nos.put(ArvoreCid.DESPERTAR, 1);
        helper.assertTrue(LutasRagnarok.invocar(jogador, true) == TojiBoss.Invocacao.OK, "com o nó a entrada começa");
        helper.assertTrue(LutasRagnarok.invocar(jogador, true) == TojiBoss.Invocacao.NA_ARENA, "uma luta por jogador");
        boolean[] sumiu = {false};
        helper.succeedWhen(() -> {
            if (!sumiu[0]) {
                RagnarokEntity r = LutasRagnarok.de(jogador.getUUID()).orElse(null);
                helper.assertTrue(r != null, "esperando o Ragnarok entrar na arena");
                double distancia = r.distanceTo(jogador), esperada = 16 - PlantaOriana.ENTRADA_DO_BOSS;
                helper.assertTrue(Math.abs(distancia - esperada) < 1.5, "a " + esperada + " blocos, está a " + distancia);
                helper.assertTrue(r.level().getBlockState(r.blockPosition().below()).blocksMotion(), "há chão sob ele");
                helper.assertTrue(r.level().noCollision(r.getBoundingBox().deflate(1e-3)), "e ele cabe onde entrou");
                helper.assertTrue(jogador.level().getBlockState(jogador.blockPosition().below()).blocksMotion(), "e sob o jogador");
                r.discard();
                sumiu[0] = true;
            }
            helper.assertTrue(LutasRagnarok.de(jogador.getUUID()).isEmpty(), "sem ele a luta acabou");
            helper.assertFalse(com.minerwk.arena.ArenasServidor.naArena(jogador), "esperando ele voltar da arena");
            // De volta, ele passou a existir no mundo dos testes; a conexão de mentira não aguenta os pacotes
            // dos bosses dos outros testes, então ele sai do mundo aqui.
            jogador.serverLevel().removePlayerImmediately(jogador, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        });
    }

    @GameTest(template = "vazio", batch = "ragnarok_2", timeoutTicks = 200)
    public static void naEntradaSoOCalorFereEDepoisVemOPrimeiroGolpe(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        // A 3 blocos do centro dele: 1,4 da beirada do corpo, dentro do calor.
        RagnarokEntity r = ragnarok(helper, jogador, 3);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - RagnarokBoss.danoDoCalor(1), "vida " + jogador.getHealth()))
                .thenExecute(() -> helper.assertTrue(r.golpe() == null, "na entrada ele ainda não ataca: foi o calor"))
                .thenWaitUntil(() -> helper.assertTrue(r.golpe() != null, "depois da entrada vem o primeiro golpe"))
                .thenExecute(r::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "ragnarok_3")
    public static void socoEmChamasFereDezNoCirculo(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        RagnarokEntity r = ragnarok(helper, jogador, 6);
        r.forcar(RagnarokBoss.SOCO);
        helper.startSequence()
                .thenIdle(RagnarokBoss.SOCO.aviso() - 2)
                .thenExecute(() -> helper.assertTrue(jogador.getHealth() == 20, "no aviso ainda não fere"))
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - comTraje(RagnarokBoss.DANO_SOCO), "vida " + jogador.getHealth()))
                .thenExecute(r::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "ragnarok_4", timeoutTicks = 200)
    public static void barragemAcertaUmSocoDepoisDoOutro(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        RagnarokEntity r = ragnarok(helper, jogador, 6);
        r.forcar(RagnarokBoss.BARRAGEM);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 13, "o primeiro: vida " + jogador.getHealth()))
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 6, "o segundo: vida " + jogador.getHealth()))
                .thenExecute(r::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "ragnarok_5", timeoutTicks = 200)
    public static void rajadaDasAsasAcertaComOProjetilDoMeio(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        RagnarokEntity r = ragnarok(helper, jogador, 14);
        r.forcar(RagnarokBoss.RAJADA);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - RagnarokBoss.DANO_RAJADA, "vida " + jogador.getHealth()))
                .thenIdle(30)
                .thenExecute(() -> {
                    helper.assertTrue(jogador.getHealth() == 20 - RagnarokBoss.DANO_RAJADA, "só um dos cinco passa por ele");
                    r.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "ragnarok_6", timeoutTicks = 200)
    public static void investidaAtravessaALinhaEFereDoze(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        RagnarokEntity r = ragnarok(helper, jogador, 8);
        r.forcar(RagnarokBoss.INVESTIDA);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(r.voando() && r.getY() > jogador.getY() + 3, "ele sobe antes de vir"))
                .thenExecute(() -> {
                    helper.assertTrue(jogador.getHealth() == 20, "subindo ainda não fere");
                    // A linha inteira tem 34 blocos e entra no terreno dos testes vizinhos, que podem cortá-la:
                    // aqui basta ela passar do jogador, que está a 8.
                    helper.assertTrue(r.comprimentoDaInvestida() >= 11, "a linha passa do jogador: " + r.comprimentoDaInvestida());
                })
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - comTraje(RagnarokBoss.DANO_INVESTIDA), "vida " + jogador.getHealth()))
                .thenWaitUntil(() -> helper.assertFalse(r.voando(), "esperando ele pousar"))
                .thenExecute(() -> {
                    helper.assertTrue(r.getX() < jogador.getX(), "do outro lado do jogador");
                    helper.assertTrue(jogador.getHealth() == 20 - comTraje(RagnarokBoss.DANO_INVESTIDA), "e feriu uma vez só");
                    r.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "ragnarok_7", timeoutTicks = 200)
    public static void soproDoDemonioCarregaDoisSegundosEVarre(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        RagnarokEntity r = ragnarok(helper, jogador, 14);
        r.forcar(RagnarokBoss.SOPRO);
        helper.startSequence()
                .thenIdle(RagnarokBoss.SOPRO.aviso() - 2)
                .thenExecute(() -> helper.assertTrue(jogador.getHealth() == 20, "na carga ainda não fere"))
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - comTraje(RagnarokBoss.DANO_SOPRO), "vida " + jogador.getHealth()))
                .thenWaitUntil(() -> helper.assertTrue(r.golpe() == null, "esperando o sopro acabar"))
                .thenExecute(() -> {
                    helper.assertTrue(jogador.getHealth() == 20 - comTraje(RagnarokBoss.DANO_SOPRO), "o feixe fere cada um uma vez");
                    r.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "ragnarok_8", timeoutTicks = 200)
    public static void pisaoMandaUmaOndaQueFereOito(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        RagnarokEntity r = ragnarok(helper, jogador, 6);
        r.forcar(RagnarokBoss.PISAO);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - comTraje(RagnarokBoss.DANO_PISAO), "vida " + jogador.getHealth()))
                .thenExecute(r::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "ragnarok_9", timeoutTicks = 300)
    public static void abaixoDaMetadeDaVidaEleePerdeAAsaENaoVoaMais(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        RagnarokEntity r = ragnarok(helper, jogador, 20);
        helper.assertTrue(r.fase() == 1 && !r.semAsa(), "começa com as duas asas");
        r.setHealth(RagnarokBoss.VIDA * .49f);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(r.fase() == 2, "esperando a segunda fase"))
                .thenExecute(() -> helper.assertTrue(r.golpe() == null, "na cena da asa ele não ataca"))
                .thenWaitUntil(() -> helper.assertTrue(r.semAsa(), "a asa some do corpo"))
                .thenExecute(() -> {
                    helper.assertFalse(r.addEffect(new MobEffectInstance(Efeitos.PARALISIA, 40)), "as correntes não o seguram");
                    r.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "ragnarok_10", timeoutTicks = 200)
    public static void vencerLiberaODespertarEDaQuinhentosDeXp(GameTestHelper helper) {
        ServerPlayer jogador = cid(helper);
        RagnarokEntity r = ragnarok(helper, jogador, 12);
        long antes = ServicoKits.dados(jogador).maestriaAtual().xp;
        r.hurt(r.damageSources().playerAttack(jogador), 10000);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(r.isRemoved(), "esperando ele cair"))
                .thenExecute(() -> {
                    helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().despertou, "o despertar é de vez");
                    helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().xp == antes + RagnarokBoss.XP_VITORIA, "500 de XP");
                    helper.assertTrue(LutasRagnarok.de(jogador.getUUID()).isEmpty(), "e a luta acabou");
                })
                .thenSucceed();
    }
}
