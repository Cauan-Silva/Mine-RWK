package com.minerwk.arena;

import com.minerwk.MineRWK;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.kit.Vetor;
import com.minerwk.kit.Anexos;
import com.minerwk.kit.TestesGojo;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests da arena. Em lotes separados: os testes mexem na mesma vaga da dimensão. */
@GameTestHolder(MineRWK.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestesArena {
    private TestesArena() {
    }

    /**
     * O servidor de GameTest monta o mundo com uma lista fixa de dimensões e ignora as de datapack. Aqui a
     * dimensão da arena é criada na mão, com o mesmo tipo (do JSON do mod) e o mesmo gerador vazio.
     */
    public static ServerLevel garantirDimensao(MinecraftServer servidor) {
        ServerLevel pronta = ArenasServidor.nivel(servidor);
        if (pronta != null) return pronta;
        var registros = servidor.registryAccess();
        Holder<DimensionType> tipo = registros.registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE, MineRWK.id("arena")));
        Holder<Biome> bioma = registros.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.THE_VOID);
        LevelStem stem = new LevelStem(tipo,
                new FlatLevelSource(new FlatLevelGeneratorSettings(Optional.empty(), bioma, List.of())));
        ChunkProgressListener mudo = new ChunkProgressListener() {
            @Override
            public void updateSpawnPos(ChunkPos centro) {
            }

            @Override
            public void onStatusChange(ChunkPos chunk, ChunkStatus status) {
            }

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }
        };
        ServerLevel nivel = new ServerLevel(servidor, servidor.executor, servidor.storageSource,
                new DerivedLevelData(servidor.getWorldData(), servidor.getWorldData().overworldData()),
                ArenasServidor.DIMENSAO, stem, mudo, false, 0L, List.of(), false, null);
        servidor.forgeGetWorldMap().put(ArenasServidor.DIMENSAO, nivel);
        servidor.markWorldsDirty();
        return nivel;
    }

    /** O jogador dos testes do Gojo, com uma conexão de mentira (trocar de dimensão manda pacotes). */
    private static ServerPlayer jogador(GameTestHelper helper) {
        garantirDimensao(helper.getLevel().getServer());
        ServerPlayer jogador = TestesGojo.gojo(helper);
        Connection conexao = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(conexao);
        jogador.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), conexao, jogador,
                CommonListenerCookie.createInitial(jogador.getGameProfile(), false));
        return jogador;
    }

    @GameTest(template = "vazio", batch = "arena_1")
    public static void dimensaoExiste(GameTestHelper helper) {
        ServerLevel nivel = garantirDimensao(helper.getLevel().getServer());
        helper.assertTrue(nivel != null, "a dimensão minerwk:arena deveria existir");
        helper.assertTrue(nivel.dimensionType().fixedTime().orElse(-1) == 11500, "hora fixa no fim de tarde (11500)");
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_2")
    public static void levarETrazerVoltaAoLugarExato(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        jogador.setYRot(37);
        jogador.setXRot(-12);
        Level origem = jogador.level();
        double x = jogador.getX(), y = jogador.getY(), z = jogador.getZ();
        CaixaArena caixa = CaixaArena.daVaga(0);

        ArenasServidor.levar(jogador, caixa);
        Vetor entrada = caixa.entradaJogador();
        helper.assertTrue(ArenasServidor.naArena(jogador), "deveria estar na dimensão da arena");
        helper.assertTrue(jogador.position().distanceTo(new net.minecraft.world.phys.Vec3(entrada.x(), entrada.y(), entrada.z())) < .01,
                "deveria estar na entrada do jogador, está em " + jogador.position());
        helper.assertTrue(Math.abs(jogador.getYRot() - 180) < .01, "deveria olhar para o norte");
        helper.assertTrue(jogador.hasData(Anexos.RETORNO_ARENA), "o ponto de retorno deveria estar salvo");

        helper.assertTrue(ArenasServidor.trazer(jogador), "trazer deveria devolver true");
        helper.assertTrue(jogador.level() == origem, "deveria voltar à dimensão de origem");
        helper.assertTrue(Math.abs(jogador.getX() - x) < .01 && Math.abs(jogador.getY() - y) < .01
                && Math.abs(jogador.getZ() - z) < .01, "deveria voltar ao lugar exato, está em " + jogador.position());
        helper.assertTrue(Math.abs(jogador.getYRot() - 37) < .01 && Math.abs(jogador.getXRot() + 12) < .01,
                "deveria voltar olhando para onde olhava");
        helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "o ponto de retorno deveria ser apagado");
        jogador.remove(Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_3")
    public static void trazerSemRetornoNaoFazNada(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        var antes = jogador.position();
        helper.assertFalse(ArenasServidor.trazer(jogador), "sem ponto de retorno, trazer devolve false");
        helper.assertTrue(jogador.position().equals(antes) && !ArenasServidor.naArena(jogador), "o jogador não se move");
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_4")
    public static void retornoInvalidoVaiParaORenascimento(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        ArenasServidor.levar(jogador, CaixaArena.daVaga(0));
        jogador.setData(Anexos.RETORNO_ARENA, new PontoRetorno(
                ResourceLocation.fromNamespaceAndPath(MineRWK.MOD_ID, "nao_existe"), 0, 80, 0, 0, 0));
        helper.assertFalse(ArenasServidor.trazer(jogador), "retorno inválido devolve false");
        helper.assertFalse(ArenasServidor.naArena(jogador), "mas o jogador sai da arena, para o renascimento normal");
        helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "e o ponto de retorno é apagado");
        jogador.remove(Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }
}
