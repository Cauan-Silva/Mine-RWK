package com.minerwk.arena;

import com.minerwk.MineRWK;
import com.minerwk.boss.LutasToji;
import com.minerwk.boss.TojiEntity;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.kit.ServicoKits;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.arena.PlantaEscola;
import com.minerwk.core.kit.Vetor;
import com.minerwk.kit.Anexos;
import com.minerwk.kit.TestesGojo;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    public static ServerPlayer jogador(GameTestHelper helper) {
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

    @GameTest(template = "vazio", batch = "arena_5", timeoutTicks = 200)
    public static void montarPoePlataformaEApagarLimpa(GameTestHelper helper) {
        ServerLevel arena = garantirDimensao(helper.getLevel().getServer());
        CaixaArena caixa = CaixaArena.daVaga(40);
        BlockPos centro = new BlockPos(caixa.centroX(), CaixaArena.Y_PATIO, caixa.centroZ());
        BlockPos sujeira = centro.offset(35, 20, -35);
        arena.setBlock(sujeira, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        boolean[] montou = {false};
        boolean[] apagou = {false};
        long comeco = arena.getGameTime();
        // O retorno roda dentro do tick do servidor: os erros são guardados e cobrados depois.
        String[] erro = {null};
        ObrasArena.montar(arena, caixa, PlantaEscola.plataforma(), () -> {
            montou[0] = true;
            long levou = arena.getGameTime() - comeco;
            if (levou > 40) erro[0] = "a montagem deveria levar até 40 ticks, levou " + levou;
            else if (!arena.getBlockState(centro.below()).is(Blocks.STONE_BRICKS)) erro[0] = "tijolo de pedra sob o centro";
            else if (!arena.getBlockState(centro.below(4).offset(30, 0, -30)).is(Blocks.STONE)) erro[0] = "pedra no canto de baixo";
            else if (!arena.getBlockState(centro).isAir()) erro[0] = "ar na altura dos pés";
            else if (!arena.getBlockState(sujeira).isAir()) erro[0] = "o que havia na caixa é limpo antes de montar";
            arena.addFreshEntity(new ItemEntity(arena, centro.getX() + .5, centro.getY() + 1, centro.getZ() + .5,
                    new ItemStack(Items.DIAMOND)));
            ObrasArena.apagar(arena, caixa, () -> apagou[0] = true);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(montou[0] && apagou[0], "montar e apagar deveriam terminar");
            helper.assertTrue(erro[0] == null, String.valueOf(erro[0]));
            helper.assertTrue(arena.getBlockState(centro.below()).isAir() && arena.getBlockState(centro.below(4)).isAir(),
                    "a caixa deveria ficar só com ar");
            AABB tudo = new AABB(centro).inflate(41, 64, 41);
            helper.assertTrue(arena.getEntitiesOfClass(ItemEntity.class, tudo).isEmpty(), "sem itens caídos na caixa");
            helper.assertTrue(ObrasArena.ocioso(), "sem obra pendente");
        });
    }

    // --- A luta do Toji na arena ---

    private static boolean perto(ServerPlayer jogador, Vec3 ponto) {
        return jogador.position().distanceTo(ponto) < .05;
    }

    private static Vec3 vec(Vetor v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    @GameTest(template = "vazio", batch = "arena_6", timeoutTicks = 300)
    public static void invocarLevaParaAArenaComOToji(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        helper.assertTrue(LutasToji.invocar(jogador, false) == TojiBoss.Invocacao.OK, "a invocação deveria começar");
        helper.assertFalse(ArenasServidor.naArena(jogador), "durante a montagem o jogador ainda está no mundo dele");
        helper.assertTrue(LutasToji.invocar(jogador, false) == TojiBoss.Invocacao.NA_ARENA, "com a arena reservada não dá para invocar de novo");
        helper.succeedWhen(() -> {
            CaixaArena caixa = ArenasServidor.de(jogador.getUUID()).orElseThrow();
            helper.assertTrue(ArenasServidor.naArena(jogador) && perto(jogador, vec(caixa.entradaJogador())),
                    "o jogador deveria estar na entrada da vaga, está em " + jogador.position());
            TojiEntity toji = LutasToji.de(jogador.getUUID()).orElse(null);
            helper.assertTrue(toji != null && ArenasServidor.naArena(toji), "o Toji deveria estar na arena");
            helper.assertTrue(Math.abs(toji.distanceTo(jogador) - 16) < .5, "a 16 blocos, está a " + toji.distanceTo(jogador));
            helper.assertTrue(LutasToji.invocar(jogador, false) == TojiBoss.Invocacao.NA_ARENA, "de dentro da arena não dá para invocar");
            toji.discard();
        });
    }

    @GameTest(template = "vazio", batch = "arena_7", timeoutTicks = 400)
    public static void vitoriaTrazDeVoltaEm100Ticks(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        ServicoKits.definirDespertou(jogador, false);
        Level origem = jogador.level();
        Vec3 antes = jogador.position();
        LutasToji.invocar(jogador, false);
        long[] morte = {-1};
        helper.succeedWhen(() -> {
            long agora = helper.getLevel().getGameTime();
            if (morte[0] < 0) {
                TojiEntity toji = LutasToji.de(jogador.getUUID()).orElse(null);
                helper.assertTrue(toji != null && ArenasServidor.naArena(jogador), "esperando a luta começar");
                toji.kill();
                morte[0] = agora;
            }
            if (agora - morte[0] <= 90) {
                helper.assertTrue(ArenasServidor.naArena(jogador), "antes dos 5 s o jogador ainda está na arena");
            }
            helper.assertTrue(agora - morte[0] >= 110, "esperando os 5 s");
            helper.assertTrue(jogador.level() == origem && perto(jogador, antes), "deveria estar de volta, está em " + jogador.position());
            helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().despertou, "a vitória dá o despertar");
            helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "o ponto de retorno é apagado");
            helper.assertTrue(ArenasServidor.de(jogador.getUUID()).isEmpty(), "a arena dele acabou");
        });
    }

    @GameTest(template = "vazio", batch = "arena_8", timeoutTicks = 300)
    public static void tojiSomeTrazDeVoltaNaHora(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        ServicoKits.definirDespertou(jogador, false);
        Level origem = jogador.level();
        Vec3 antes = jogador.position();
        LutasToji.invocar(jogador, false);
        boolean[] sumiu = {false};
        helper.succeedWhen(() -> {
            if (!sumiu[0]) {
                TojiEntity toji = LutasToji.de(jogador.getUUID()).orElse(null);
                helper.assertTrue(toji != null && ArenasServidor.naArena(jogador), "esperando a luta começar");
                toji.discard();
                sumiu[0] = true;
            }
            helper.assertTrue(jogador.level() == origem && perto(jogador, antes), "deveria voltar na hora, está em " + jogador.position());
            helper.assertFalse(ServicoKits.dados(jogador).maestriaAtual().despertou, "sem vitória não há despertar");
            helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "o ponto de retorno é apagado");
        });
    }

    @GameTest(template = "vazio", batch = "arena_9", timeoutTicks = 300)
    public static void sairDuranteAMontagemLiberaAVaga(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        int ocupadasAntes = ArenasServidor.vagasOcupadas();
        Vec3 antes = jogador.position();
        LutasToji.invocar(jogador, false);
        helper.assertTrue(ArenasServidor.vagasOcupadas() == ocupadasAntes + 1, "a vaga é reservada ao invocar");
        helper.runAfterDelay(1, () -> ArenasServidor.aoSair(jogador));
        helper.runAfterDelay(2, () -> helper.succeedWhen(() -> {
            helper.assertTrue(ArenasServidor.vagasOcupadas() == ocupadasAntes, "a vaga deveria ser liberada depois da limpeza");
            helper.assertTrue(ArenasServidor.de(jogador.getUUID()).isEmpty(), "sem arena para ele");
            helper.assertTrue(!ArenasServidor.naArena(jogador) && perto(jogador, antes), "ele nunca foi levado");
            helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "sem ponto de retorno guardado");
            helper.assertTrue(LutasToji.de(jogador.getUUID()).isEmpty(), "e sem Toji");
        }));
    }

    @GameTest(template = "vazio", batch = "arena_10", timeoutTicks = 300)
    public static void duasLutasFicamEmVagasDiferentes(GameTestHelper helper) {
        ServerPlayer um = jogador(helper);
        ServerPlayer dois = jogador(helper);
        LutasToji.invocar(um, false);
        LutasToji.invocar(dois, false);
        helper.succeedWhen(() -> {
            CaixaArena a = ArenasServidor.de(um.getUUID()).orElseThrow();
            CaixaArena b = ArenasServidor.de(dois.getUUID()).orElseThrow();
            helper.assertFalse(a.equals(b), "cada luta na sua vaga");
            TojiEntity ta = LutasToji.de(um.getUUID()).orElse(null);
            TojiEntity tb = LutasToji.de(dois.getUUID()).orElse(null);
            helper.assertTrue(ta != null && tb != null, "esperando os dois Tojis");
            helper.assertTrue(perto(um, vec(a.entradaJogador())) && perto(dois, vec(b.entradaJogador())), "cada um na sua entrada");
            ta.discard();
            tb.discard();
        });
    }

    @GameTest(template = "vazio", batch = "arena_11")
    public static void entrarNoJogoNaArenaSemLutaVolta(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        Level origem = jogador.level();
        Vec3 antes = jogador.position();
        ArenasServidor.levar(jogador, CaixaArena.daVaga(50));
        ArenasServidor.aoEntrar(jogador);
        helper.assertTrue(jogador.level() == origem && perto(jogador, antes), "quem entra no jogo na arena sem luta volta ao ponto de retorno");

        ArenasServidor.levar(jogador, CaixaArena.daVaga(50));
        jogador.setData(Anexos.RETORNO_ARENA, new PontoRetorno(
                ResourceLocation.fromNamespaceAndPath(MineRWK.MOD_ID, "nao_existe"), 0, 80, 0, 0, 0));
        ArenasServidor.aoEntrar(jogador);
        helper.assertFalse(ArenasServidor.naArena(jogador), "com retorno inválido vai para o renascimento normal");

        ArenasServidor.levar(jogador, CaixaArena.daVaga(50));
        jogador.removeData(Anexos.RETORNO_ARENA);
        ArenasServidor.aoEntrar(jogador);
        helper.assertFalse(ArenasServidor.naArena(jogador), "sem ponto de retorno também");
        jogador.remove(Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }
}
