package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.boss.LutasToji;
import com.minerwk.boss.TojiEntity;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.personagens.gojo.ArvoreGojo;
import com.minerwk.registro.Efeitos;
import com.minerwk.registro.Entidades;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da luta contra o Toji (rodam com {@code ./gradlew runGameTestServer}).
 *
 * <p>Cada teste roda sozinho, num lote só dele: os testes ficam a 6 blocos um do outro, os golpes dos vizinhos
 * acertariam o Toji, e quando um teste termina o Minecraft apaga as entidades que estão na área dele. As
 * esperas são "até acontecer", não um número fixo de ticks: no começo do lote o Toji demora um ou dois ticks
 * para começar a agir.
 */
@GameTestHolder(MineRWK.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestesToji {
    private static final int CHAO = 0;

    private TestesToji() {
    }

    /** Um Toji a {@code distancia} blocos do jogador, os dois no chão, já em luta. */
    private static TojiEntity toji(GameTestHelper helper, ServerPlayer jogador, double distancia) {
        // Dano de verdade num jogador manda pacotes: ele ganha uma conexão de mentira, que não vai a lugar nenhum.
        if (jogador.connection == null) {
            Connection conexao = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(conexao);
            jogador.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), conexao, jogador,
                    CommonListenerCookie.createInitial(jogador.getGameProfile(), false));
        }
        // No chão do mundo de testes (o jogador dos testes do Gojo flutua 2 blocos acima dele).
        jogador.setPos(helper.absoluteVec(new Vec3(1.5, CHAO, 1.5)));
        // Só o chunk do teste processa entidades; os vizinhos, por onde o Toji anda, precisam ser forçados.
        ChunkPos centro = new ChunkPos(jogador.blockPosition());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) helper.getLevel().setChunkForced(centro.x + dx, centro.z + dz, true);
        }
        TojiEntity toji = Entidades.TOJI.get().create(helper.getLevel());
        Vec3 p = helper.absoluteVec(new Vec3(1.5 + distancia, CHAO, 1.5));
        toji.moveTo(p.x, p.y, p.z, 90, 0);
        toji.comecarLuta(jogador);
        helper.getLevel().addFreshEntity(toji);
        return toji;
    }

    /** Uma parede atrás do jogador: a investida não sai da área do teste. */
    private static void parede(GameTestHelper helper) {
        for (int y = CHAO; y <= CHAO + 2; y++) {
            for (int z = 0; z <= 2; z++) helper.setBlock(new BlockPos(-2, y, z), Blocks.STONE);
        }
    }

    @GameTest(template = "vazio", batch = "toji_1")
    public static void lancaAtravessaEDesligaOInfinito(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        // O Infinito de verdade: a tecla C, com o efeito no kit.
        kit.apertar(Tecla.C, helper.getLevel().getGameTime());
        for (int i = 0; i < 6; i++) ServicoKits.tickJogador(jogador);
        helper.assertTrue(kit.efeitos().size() == 1, "o Infinito deveria estar ligado");
        ProtecoesServidor.ligarInfinito(jogador, 200);
        TojiEntity toji = toji(helper, jogador, 3);
        toji.forcar(TojiBoss.LANCA);
        helper.runAfterDelay(TojiBoss.LANCA.aviso() - 4, () ->
                helper.assertTrue(ProtecoesServidor.infinitoAtivo(jogador) && jogador.getHealth() == 20, "o aviso não fere"));
        helper.succeedWhen(() -> {
            helper.assertFalse(ProtecoesServidor.infinitoAtivo(jogador), "a Lança desliga o Infinito");
            helper.assertTrue(kit.efeitos().isEmpty(), "e o efeito dele acaba");
            helper.assertTrue(jogador.getHealth() == 20 - TojiBoss.DANO_LANCA,
                    "a Lança deveria tirar 10 de vida, sobrou " + jogador.getHealth());
        });
    }

    @GameTest(template = "vazio", batch = "toji_2")
    public static void infinitoParaAInvestida(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        ProtecoesServidor.ligarInfinito(jogador, 200);
        parede(helper);
        TojiEntity toji = toji(helper, jogador, 5);
        toji.forcar(TojiBoss.INVESTIDA);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(toji.distanceTo(jogador) < 4, "o Toji deveria avançar"))
                // A investida inteira passa pelo jogador; ainda falta muito para o Toji escolher outro golpe.
                .thenIdle(TojiBoss.INVESTIDA.janela() + 2)
                .thenExecute(() -> helper.assertTrue(jogador.getHealth() == 20,
                        "o Infinito para a investida, sobrou " + jogador.getHealth()))
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "toji_3")
    public static void investidaFereSemInfinito(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        parede(helper);
        TojiEntity toji = toji(helper, jogador, 5);
        toji.forcar(TojiBoss.INVESTIDA);
        helper.succeedWhen(() -> helper.assertTrue(jogador.getHealth() == 20 - TojiBoss.DANO_INVESTIDA,
                "a investida deveria tirar 8 de vida, sobrou " + jogador.getHealth()));
    }

    @GameTest(template = "vazio", batch = "toji_4", timeoutTicks = 240)
    public static void despertarNoMeioDaLutaEVitoria(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        ServicoKits.definirDespertou(jogador, false);
        long xpAntes = ServicoKits.dados(jogador).maestriaAtual().xp;
        TojiEntity toji = toji(helper, jogador, 3);
        helper.assertTrue(toji.fase() == 1, "quem não despertou começa na fase 1");
        // 20% da vida perdida: o Toji arma a Lança que derruba.
        toji.setHealth(TojiBoss.VIDA * .79f);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(LutasToji.caido(jogador), "a Lança derruba quem invocou"))
                .thenExecute(() -> {
                    helper.assertTrue(jogador.getHealth() == 1, "caído com 1 de vida, está com " + jogador.getHealth());
                    helper.assertTrue(ProtecoesServidor.paralisado(jogador), "caído não age");
                    jogador.hurt(jogador.damageSources().generic(), 50);
                    helper.assertTrue(jogador.getHealth() == 1, "caído não morre");
                    helper.assertFalse(ServicoKits.desperto(jogador), "ainda não despertou");
                })
                .thenIdle(TojiBoss.QUEDA - 10)
                .thenExecute(() -> helper.assertTrue(LutasToji.caido(jogador), "a queda dura 3 segundos"))
                .thenWaitUntil(() -> helper.assertFalse(LutasToji.caido(jogador), "levanta"))
                .thenExecute(() -> {
                    helper.assertTrue(jogador.getHealth() == jogador.getMaxHealth(), "com a vida cheia");
                    helper.assertTrue(ServicoKits.desperto(jogador), "desperto na luta");
                    helper.assertFalse(ServicoKits.dados(jogador).maestriaAtual().despertou, "mas ainda não de vez");
                    helper.assertTrue(toji.fase() == 2, "o Toji passa para a fase 2");
                    KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
                    helper.assertTrue(kit.apertar(Tecla.X, helper.getLevel().getGameTime()), "o Vermelho funciona na luta");
                    kit.descartar();
                    // Vitória: o despertar fica.
                    toji.hurt(jogador.damageSources().playerAttack(jogador), 1000);
                    helper.assertTrue(toji.isDeadOrDying(), "o Toji deveria ter morrido");
                    helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().despertou, "a vitória dá o despertar de vez");
                    helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().xp >= xpAntes + TojiBoss.XP_VITORIA,
                            "e 500 de XP");
                })
                .thenWaitUntil(() -> helper.assertTrue(toji.isRemoved(), "o corpo some depois da animação de morte"))
                .thenExecute(() -> {
                    helper.assertTrue(LutasToji.de(jogador.getUUID()).isEmpty(), "e a luta acaba");
                    helper.assertTrue(ServicoKits.desperto(jogador), "continua desperto");
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "toji_5")
    public static void golpeMortalViraODespertar(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        ServicoKits.definirDespertou(jogador, false);
        jogador.setHealth(4);
        TojiEntity toji = toji(helper, jogador, 3);
        toji.forcar(TojiBoss.LANCA);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(LutasToji.caido(jogador), "o golpe mortal derruba"))
                .thenExecute(() -> {
                    helper.assertTrue(jogador.isAlive(), "em vez de matar quem ainda não despertou");
                    toji.discard();
                    helper.assertFalse(LutasToji.caido(jogador), "sem o Toji ninguém fica caído");
                    helper.assertFalse(ProtecoesServidor.paralisado(jogador), "nem paralisado");
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "toji_6")
    public static void tojiSomeQuandoQuemInvocouMorre(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        ServicoKits.definirDespertou(jogador, false);
        TojiEntity toji = toji(helper, jogador, 6);
        ServicoKits.definirDespertoNaLuta(jogador, true);
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertFalse(toji.isRemoved(), "com quem invocou vivo a luta segue");
                    jogador.setHealth(0);
                })
                .thenWaitUntil(() -> helper.assertTrue(toji.isRemoved(), "o Toji some quando quem invocou morre"))
                .thenExecute(() -> {
                    helper.assertFalse(ServicoKits.desperto(jogador), "e os golpes voltam a trancar");
                    helper.assertFalse(ServicoKits.dados(jogador).maestriaAtual().despertou, "sem vitória não há despertar");
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "toji_7")
    public static void contratoExigeONoDespertar(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        ServicoKits.definirDespertou(jogador, false);
        helper.assertTrue(LutasToji.invocar(jogador, true) == TojiBoss.Invocacao.SEM_NO, "sem o nó o Contrato não funciona");
        ServicoKits.dados(jogador).maestriaAtual().nos.put(ArvoreGojo.DESPERTAR, 1);
        helper.assertTrue(LutasToji.invocar(jogador, true) == TojiBoss.Invocacao.OK, "com o nó o Toji aparece");
        TojiEntity toji = LutasToji.de(jogador.getUUID()).orElseThrow();
        double distancia = toji.distanceTo(jogador);
        helper.assertTrue(distancia > 6 && distancia < 12, "a uns 8 blocos, está a " + distancia);
        helper.assertTrue(LutasToji.invocar(jogador, true) == TojiBoss.Invocacao.JA_EXISTE, "um Toji por jogador");
        toji.discard();
        helper.assertTrue(LutasToji.de(jogador.getUUID()).isEmpty(), "sem o Toji dá para invocar de novo");
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "toji_8")
    public static void vazioInfinitoNaoParalisaOToji(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        TojiEntity toji = toji(helper, jogador, 4);
        helper.assertTrue(toji.fase() == 2, "contra quem já despertou a luta começa na fase 2");
        toji.addEffect(new MobEffectInstance(Efeitos.PARALISIA, 100));
        helper.assertFalse(toji.hasEffect(Efeitos.PARALISIA), "o Toji não tem energia amaldiçoada para paralisar");
        toji.discard();
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "toji_9")
    public static void entradaSeguraOPrimeiroGolpe(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        TojiEntity toji = toji(helper, jogador, 3);
        helper.startSequence()
                .thenIdle(TojiBoss.ENTRADA - 10)
                .thenExecute(() -> {
                    helper.assertTrue(toji.golpe() == null, "na entrada ele ainda não ataca");
                    helper.assertTrue(jogador.getHealth() == 20, "nem fere");
                })
                .thenWaitUntil(() -> helper.assertTrue(toji.golpe() != null, "depois da entrada vem o primeiro golpe"))
                .thenExecute(toji::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "toji_10")
    public static void nuvemBateTresVezes(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        TojiEntity toji = toji(helper, jogador, 2);
        toji.forcar(TojiBoss.NUVEM);
        float esperado = 20 - 3 * TojiBoss.DANO_NUVEM;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == esperado,
                        "3 golpes de 3 de dano, sobrou " + jogador.getHealth()))
                .thenIdle(TojiBoss.NUVEM.pausa() - 2)
                .thenExecute(() -> {
                    helper.assertTrue(jogador.getHealth() == esperado, "e nenhum a mais, sobrou " + jogador.getHealth());
                    toji.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "toji_11")
    public static void nuvemNaoPegaQuemEstaForaDoRaio(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        TojiEntity toji = toji(helper, jogador, 5);
        toji.forcar(TojiBoss.NUVEM);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(toji.golpe() == null, "o golpe termina"))
                .thenExecute(() -> {
                    helper.assertTrue(jogador.getHealth() == 20, "a 5 blocos o bastão não alcança, sobrou " + jogador.getHealth());
                    toji.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "toji_12")
    public static void desaparecerSurgeAoLadoEFere(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        TojiEntity toji = toji(helper, jogador, 7);
        toji.forcar(TojiBoss.DESAPARECER);
        helper.startSequence()
                .thenExecute(() -> helper.assertTrue(toji.isInvisible(), "ele some"))
                .thenWaitUntil(() -> helper.assertFalse(toji.isInvisible(), "e volta a aparecer"))
                .thenExecute(() -> {
                    helper.assertTrue(toji.distanceTo(jogador) < 2.5, "colado no alvo, está a " + toji.distanceTo(jogador));
                    helper.assertTrue(jogador.getHealth() == 20, "a sombra avisa antes do golpe");
                })
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() == 20 - TojiBoss.DANO_SURGIR,
                        "o golpe tira 8, sobrou " + jogador.getHealth()))
                .thenExecute(toji::discard)
                .thenSucceed();
    }

    @GameTest(template = "vazio", batch = "toji_13", timeoutTicks = 300)
    public static void fase3ComecaComAExecucao(GameTestHelper helper) {
        ServerPlayer jogador = TestesGojo.gojo(helper);
        TojiEntity toji = toji(helper, jogador, 4);
        helper.assertTrue(toji.fase() == 2, "contra quem já despertou a luta começa na fase 2");
        toji.setHealth(TojiBoss.VIDA * .3f);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(toji.fase() == 3, "abaixo de 35% de vida vem a fase 3"))
                .thenExecute(() -> helper.assertTrue(toji.golpe() == null, "ele para um instante na troca de fase"))
                .thenWaitUntil(() -> helper.assertTrue(toji.golpe() == TojiBoss.EXECUCAO, "e emenda a Execução do Assassino"))
                .thenWaitUntil(() -> helper.assertTrue(jogador.getHealth() <= 20 - TojiBoss.DANO_EXECUCAO,
                        "a investida acerta quem fica parado, sobrou " + jogador.getHealth()))
                .thenExecute(toji::discard)
                .thenSucceed();
    }
}
